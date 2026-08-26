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
    val id: String,
    val name: String,
    val category: String,
    val unit: String,
    val price: Double,
    val stock: String,
)

data class RecipeComponent(
    val sourceId: String,
    val kind: String,
    val name: String,
    val quantity: Double,
    val unit: String,
)

data class SubrecipeSummary(
    val id: String,
    val name: String,
    val output: Double,
    val unit: String,
    val components: List<RecipeComponent>,
)

data class RecipeSummary(
    val id: String,
    val name: String,
    val category: String,
    val hpp: Double,
    val price: Double,
    val margin: Double,
    val status: String,
    val sku: String = "",
    val photo: String = "",
    val components: List<RecipeComponent> = emptyList(),
)

data class WorkspaceData(
    val profile: StoreProfile?,
    val subscription: Subscription?,
    val ingredients: List<IngredientSummary>,
    val recipes: List<RecipeSummary>,
    val subrecipes: List<SubrecipeSummary>,
)

val WorkspaceData.subrecipeCount: Int get() = subrecipes.size

data class OnboardingDraft(
    val storeName: String,
    val province: String,
    val city: String,
    val businessType: String,
    val targetMargin: Int,
    val useSampleData: Boolean,
)
