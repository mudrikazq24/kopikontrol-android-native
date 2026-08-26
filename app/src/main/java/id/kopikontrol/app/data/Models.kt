package id.kopikontrol.app.data

data class Account(
    val id: String,
    val name: String,
    val email: String,
    val whatsapp: String,
)

data class StoreProfile(
    val name: String,
    val province: String,
    val city: String,
    val businessType: String,
    val targetMargin: Int,
)

data class Subscription(
    val plan: String,
    val status: String,
    val daysLeft: Int?,
    val isLifetime: Boolean,
)

data class IngredientSummary(
    val name: String,
    val category: String,
    val unit: String,
    val price: Double,
    val stock: String,
)

data class RecipeSummary(
    val name: String,
    val category: String,
    val hpp: Double,
    val price: Double,
    val margin: Double,
    val status: String,
)

data class WorkspaceData(
    val profile: StoreProfile?,
    val subscription: Subscription?,
    val ingredients: List<IngredientSummary>,
    val recipes: List<RecipeSummary>,
    val subrecipeCount: Int,
)

data class OnboardingDraft(
    val storeName: String,
    val province: String,
    val city: String,
    val businessType: String,
    val targetMargin: Int,
    val useSampleData: Boolean,
)
