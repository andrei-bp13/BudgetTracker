package com.budgettracker.app.data.model

import androidx.compose.ui.graphics.Color

data class CategoryItem(
    val id: String,
    val name: String,
    val iconId: String,
    val colorHex: String,   // "#RRGGBB"
    val isDefault: Boolean = true
) {
    val color: Color get() = Color(android.graphics.Color.parseColor(colorHex))
}

val DefaultCategories: List<CategoryItem> = listOf(
    CategoryItem("cat_food",         "Food & Dining",    "restaurant",      "#E53935"),
    CategoryItem("cat_groceries",    "Groceries",        "groceries",       "#F4511E"),
    CategoryItem("cat_shopping",     "Shopping",         "shopping_bag",    "#8E24AA"),
    CategoryItem("cat_transport",    "Transportation",   "car",             "#1E88E5"),
    CategoryItem("cat_entertainment","Entertainment",    "movie",           "#FF6F00"),
    CategoryItem("cat_healthcare",   "Healthcare",       "hospital",        "#00897B"),
    CategoryItem("cat_housing",      "Housing",          "home",            "#5C6BC0"),
    CategoryItem("cat_utilities",    "Utilities",        "electric",        "#757575"),
    CategoryItem("cat_education",    "Education",        "school",          "#039BE5"),
    CategoryItem("cat_travel",       "Travel",           "flight",          "#43A047"),
    CategoryItem("cat_personal",     "Personal Care",    "content_cut",     "#D81B60"),
    CategoryItem("cat_pets",         "Pets",             "pets",            "#FF8F00"),
    CategoryItem("cat_sports",       "Sports & Fitness", "fitness",         "#E53935"),
    CategoryItem("cat_family",       "Family & Kids",    "family",          "#9C27B0"),
    CategoryItem("cat_gifts",        "Gifts",            "card_giftcard",   "#E91E63"),
    CategoryItem("cat_subscriptions","Subscriptions",    "subscriptions",   "#3F51B5"),
    CategoryItem("cat_salary",       "Salary",           "work",            "#2E7D32"),
    CategoryItem("cat_freelance",    "Freelance",        "computer",        "#1565C0"),
    CategoryItem("cat_investment",   "Investment",       "trending_up",     "#558B2F"),
    CategoryItem("cat_other_income", "Other Income",     "savings",         "#00695C"),
)

val CategoryColorOptions: List<Color> = listOf(
    Color(0xFFEF5350), // Red
    Color(0xFFFF7043), // Deep Orange
    Color(0xFFFFB300), // Amber
    Color(0xFFFDD835), // Yellow
    Color(0xFF66BB6A), // Green
    Color(0xFF26A69A), // Teal
    Color(0xFF29B6F6), // Sky Blue
    Color(0xFF1E88E5), // Blue
    Color(0xFF5C6BC0), // Indigo
    Color(0xFF7E57C2), // Deep Purple
    Color(0xFFAB47BC), // Purple
    Color(0xFFEC407A), // Pink
    Color(0xFFF06292), // Light Pink
    Color(0xFF00BCD4), // Cyan
    Color(0xFF00E5FF), // Bright Cyan
    Color(0xFF69F0AE), // Light Green
    Color(0xFFFFCC02), // Gold
    Color(0xFFFF6E40), // Coral
    Color(0xFF607D8B), // Blue-Grey
    Color(0xFF9E9E9E), // Grey
)

fun colorToHex(color: Color): String {
    val r = (color.red   * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue  * 255).toInt()
    return "#%02X%02X%02X".format(r, g, b)
}
