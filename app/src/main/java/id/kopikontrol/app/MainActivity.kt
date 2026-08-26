package id.kopikontrol.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import id.kopikontrol.app.ui.KopiKontrolApp
import id.kopikontrol.app.ui.theme.KopiKontrolTheme

class MainActivity : ComponentActivity() {
    private var oauthCallback by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        oauthCallback = intent?.data
        setContent {
            KopiKontrolTheme {
                KopiKontrolApp(
                    oauthCallback = oauthCallback,
                    consumeOauthCallback = { oauthCallback = null },
                    openGoogleLogin = {
                        CustomTabsIntent.Builder().build().launchUrl(
                            this,
                            Uri.parse("${BuildConfig.API_BASE_URL}/api/auth?oauth=google&platform=android-native")
                        )
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        oauthCallback = intent.data
    }
}
