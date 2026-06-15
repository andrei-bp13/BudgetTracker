package com.pardonmyspending.app.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class CategoryType { EXPENSE, INCOME }

@Immutable
data class CategoryItem(
    val id: String,
    val name: String,
    val iconId: String,
    val colorHex: String,   // "#RRGGBB"
    val isDefault: Boolean = true,
    val type: CategoryType = CategoryType.EXPENSE
) {
    val color: Color = Color(android.graphics.Color.parseColor(colorHex))
}

val DefaultCategories: List<CategoryItem> = listOf(
    CategoryItem("cat_food",         "Food & Dining",    "restaurant",      "#E53935", type = CategoryType.EXPENSE),
    CategoryItem("cat_groceries",    "Groceries",        "groceries",       "#F4511E", type = CategoryType.EXPENSE),
    CategoryItem("cat_shopping",     "Shopping",         "shopping_bag",    "#8E24AA", type = CategoryType.EXPENSE),
    CategoryItem("cat_transport",    "Transportation",   "car",             "#1E88E5", type = CategoryType.EXPENSE),
    CategoryItem("cat_entertainment","Entertainment",    "movie",           "#FF6F00", type = CategoryType.EXPENSE),
    CategoryItem("cat_healthcare",   "Healthcare",       "hospital",        "#00897B", type = CategoryType.EXPENSE),
    CategoryItem("cat_housing",      "Housing",          "home",            "#5C6BC0", type = CategoryType.EXPENSE),
    CategoryItem("cat_utilities",    "Utilities",        "electric",        "#757575", type = CategoryType.EXPENSE),
    CategoryItem("cat_education",    "Education",        "school",          "#039BE5", type = CategoryType.EXPENSE),
    CategoryItem("cat_travel",       "Travel",           "flight",          "#43A047", type = CategoryType.EXPENSE),
    CategoryItem("cat_personal",     "Personal Care",    "content_cut",     "#D81B60", type = CategoryType.EXPENSE),
    CategoryItem("cat_pets",         "Pets",             "pets",            "#FF8F00", type = CategoryType.EXPENSE),
    CategoryItem("cat_sports",       "Sports & Fitness", "fitness",         "#E53935", type = CategoryType.EXPENSE),
    CategoryItem("cat_family",       "Family & Kids",    "family",          "#9C27B0", type = CategoryType.EXPENSE),
    CategoryItem("cat_gifts",        "Gifts",            "card_giftcard",   "#E91E63", type = CategoryType.EXPENSE),
    CategoryItem("cat_subscriptions","Subscriptions",    "subscriptions",   "#3F51B5", type = CategoryType.EXPENSE),
    CategoryItem("cat_salary",       "Salary",           "work",            "#2E7D32", type = CategoryType.INCOME),
    CategoryItem("cat_freelance",    "Freelance",        "computer",        "#1565C0", type = CategoryType.INCOME),
    CategoryItem("cat_investment",   "Investment",       "trending_up",     "#558B2F", type = CategoryType.INCOME),
    CategoryItem("cat_savings",      "Savings",          "account_balance", "#1B5E20", type = CategoryType.INCOME),
    CategoryItem("cat_other_income", "Other Income",     "savings",         "#00695C", type = CategoryType.INCOME),
)

// 19 vivid colors -" the 20th slot in EditCategorySheet is the custom RGB picker
val CategoryColorOptions: List<Color> = listOf(
    Color(0xFF4361EE), // vivid blue
    Color(0xFF8338EC), // violet
    Color(0xFFF72585), // hot pink
    Color(0xFFE63946), // coral red
    Color(0xFFFB8500), // orange
    Color(0xFFFFB703), // golden amber
    Color(0xFF06D6A0), // emerald
    Color(0xFF118AB2), // teal blue
    Color(0xFF4CC9F0), // sky blue
    Color(0xFF3A86FF), // bright blue
    Color(0xFFFF006E), // neon pink
    Color(0xFFFFBE0B), // vivid yellow
    Color(0xFF8AC926), // lime green
    Color(0xFF1982C4), // cerulean
    Color(0xFF6A4C93), // deep purple
    Color(0xFFFF595E), // salmon
    Color(0xFF26A96C), // forest green
    Color(0xFF48CAE4), // light cyan
    Color(0xFFF4A261), // sandy orange
)

fun colorToHex(color: Color): String {
    val r = (color.red   * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue  * 255).toInt()
    return "#%02X%02X%02X".format(r, g, b)
}
