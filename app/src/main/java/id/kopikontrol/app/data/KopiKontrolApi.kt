package id.kopikontrol.app.data

import id.kopikontrol.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiException(message: String, val statusCode: Int = 0) : Exception(message)

class KopiKontrolApi(private val sessionStore: SessionStore) {
    suspend fun restoreSession(): Account? = withContext(Dispatchers.IO) {
        if (sessionStore.cookieHeader().isBlank()) return@withContext null
        try {
            val json = request("GET", "/api/auth")
            json.optJSONObject("user")?.toAccount()
        } catch (error: Exception) {
            if (error is ApiException && error.statusCode == 401) sessionStore.clear()
            null
        }
    }

    suspend fun login(whatsapp: String, password: String): Account = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("action", "login")
            .put("whatsapp", normalizeWhatsApp(whatsapp))
            .put("password", password)
        request("POST", "/api/auth", body).requireUser()
    }

    suspend fun signup(name: String, whatsapp: String, password: String): Account = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("action", "signup")
            .put("name", name.trim())
            .put("whatsapp", normalizeWhatsApp(whatsapp))
            .put("password", password)
            .put("acceptedTerms", true)
        request("POST", "/api/auth", body).requireUser()
    }

    suspend fun completeGoogleLogin(payload: String): Account = withContext(Dispatchers.IO) {
        val values = payload.split('&').mapNotNull {
            val separator = it.indexOf('=')
            if (separator < 1) null else it.substring(0, separator) to java.net.URLDecoder.decode(it.substring(separator + 1), "UTF-8")
        }.toMap()
        val accessToken = values["access_token"].orEmpty()
        val refreshToken = values["refresh_token"].orEmpty()
        if (accessToken.isBlank() || refreshToken.isBlank()) throw ApiException("Sesi Google tidak lengkap.")
        val body = JSONObject()
            .put("action", "oauth_session")
            .put("accessToken", accessToken)
            .put("refreshToken", refreshToken)
            .put("expiresIn", values["expires_in"]?.toIntOrNull() ?: 3600)
        request("POST", "/api/auth", body).requireUser()
    }

    suspend fun loadWorkspace(): WorkspaceData = withContext(Dispatchers.IO) {
        request("GET", "/api/data").toWorkspace()
    }

    suspend fun finishOnboarding(draft: OnboardingDraft): WorkspaceData = withContext(Dispatchers.IO) {
        val profile = JSONObject()
            .put("name", draft.storeName.trim())
            .put("province", draft.province.trim())
            .put("city", draft.city.trim())
            .put("businessType", draft.businessType)
            .put("currency", "IDR")
            .put("targetMargin", draft.targetMargin)
        val body = JSONObject()
            .put("table", "onboarding")
            .put("data", JSONObject().put("profile", profile).put("choice", if (draft.useSampleData) "sample" else "empty"))
        request("POST", "/api/data", body).toWorkspace()
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching { request("DELETE", "/api/auth") }
        sessionStore.clear()
    }

    private fun request(method: String, path: String, body: JSONObject? = null): JSONObject {
        val connection = (URL("${BuildConfig.API_BASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val cookies = sessionStore.cookieHeader()
            if (cookies.isNotBlank()) setRequestProperty("Cookie", cookies)
            if (body != null) {
                doOutput = true
                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
        }
        val status = connection.responseCode
        val setCookies = connection.headerFields.entries
            .filter { it.key?.equals("Set-Cookie", ignoreCase = true) == true }
            .flatMap { it.value ?: emptyList() }
        sessionStore.mergeSetCookies(setCookies)
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { reader -> reader.readText() }.orEmpty()
        val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
        if (status !in 200..299) throw ApiException(json.optString("error", "Koneksi ke KopiKontrol gagal."), status)
        return json
    }

    private fun JSONObject.requireUser(): Account = optJSONObject("user")?.toAccount()
        ?: throw ApiException("Sesi akun belum dapat dibuat.")

    private fun JSONObject.toAccount(): Account {
        val metadata = optJSONObject("user_metadata") ?: JSONObject()
        val name = metadata.optString("owner_name").ifBlank {
            metadata.optString("name").ifBlank { metadata.optString("full_name", "Pemilik Kedai") }
        }
        return Account(
            id = optString("id"),
            name = name,
            email = optString("email"),
            whatsapp = metadata.optString("whatsapp"),
        )
    }

    private fun JSONObject.toWorkspace(): WorkspaceData {
        val profileJson = optJSONObject("profile")
        val subscriptionJson = optJSONObject("subscription")
        val ingredientJson = optJSONArray("ingredients") ?: JSONArray()
        val recipeJson = optJSONArray("recipes") ?: JSONArray()
        val subrecipeJson = optJSONArray("subrecipes") ?: JSONArray()
        return WorkspaceData(
            profile = profileJson?.let {
                StoreProfile(
                    name = it.optString("name"), province = it.optString("province"), city = it.optString("city"),
                    businessType = it.optString("businessType"), targetMargin = it.optInt("targetMargin", 60)
                )
            },
            subscription = subscriptionJson?.let {
                Subscription(
                    plan = it.optString("plan", "Starter"), status = it.optString("status", "trial"),
                    daysLeft = if (it.isNull("daysLeft")) null else it.optInt("daysLeft"),
                    isLifetime = it.optBoolean("isLifetime")
                )
            },
            ingredients = (0 until ingredientJson.length()).map { index -> ingredientJson.getJSONObject(index) }.map {
                IngredientSummary(it.optString("name"), it.optString("category", "Lainnya"), it.optString("unit"), it.optDouble("price"), it.optString("stock", "-"))
            },
            recipes = (0 until recipeJson.length()).map { index -> recipeJson.getJSONObject(index) }.map {
                RecipeSummary(it.optString("name"), it.optString("category", "Minuman"), it.optDouble("hpp"), it.optDouble("price"), it.optDouble("margin"), it.optString("status", "-"))
            },
            subrecipeCount = subrecipeJson.length(),
        )
    }

    private fun normalizeWhatsApp(value: String): String {
        var digits = value.filter(Char::isDigit)
        if (digits.startsWith("0")) digits = "62${digits.drop(1)}"
        if (!digits.startsWith("62")) digits = "62$digits"
        return digits
    }
}
