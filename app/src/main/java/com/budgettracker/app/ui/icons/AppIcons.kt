package com.pardonmyspending.app.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

data class AppIcon(val id: String, val label: String, val vector: ImageVector)

val AppIcons: List<AppIcon> = listOf(
    // ── Home & Living (10) ────────────────────────────────────────────────────
    AppIcon("home",             "Home",          Icons.Outlined.Home),
    AppIcon("apartment",        "Apartment",     Icons.Outlined.Apartment),
    AppIcon("bathtub",          "Bathroom",      Icons.Outlined.Bathtub),
    AppIcon("kitchen",          "Kitchen",       Icons.Outlined.Kitchen),
    AppIcon("chair",            "Furniture",     Icons.Outlined.ChairAlt),
    AppIcon("build",            "Tools",         Icons.Outlined.Build),
    AppIcon("cleaning",         "Cleaning",      Icons.Outlined.CleaningServices),
    AppIcon("yard",             "Garden",        Icons.Outlined.Yard),
    AppIcon("construction",     "Repairs",       Icons.Outlined.Construction),
    AppIcon("lock",             "Security",      Icons.Outlined.Lock),

    // ── Utilities (6) ─────────────────────────────────────────────────────────
    AppIcon("electric",         "Electricity",   Icons.Outlined.ElectricBolt),
    AppIcon("water_drop",       "Water",         Icons.Outlined.WaterDrop),
    AppIcon("wifi",             "Internet",      Icons.Outlined.Wifi),
    AppIcon("phone_android",    "Phone",         Icons.Outlined.PhoneAndroid),
    AppIcon("whatshot",         "Gas/Heating",   Icons.Outlined.Whatshot),
    AppIcon("thermostat",       "Thermostat",    Icons.Outlined.Thermostat),

    // ── Food & Dining (7) ─────────────────────────────────────────────────────
    AppIcon("restaurant",       "Restaurant",    Icons.Outlined.Restaurant),
    AppIcon("groceries",        "Groceries",     Icons.Outlined.LocalGroceryStore),
    AppIcon("coffee",           "Coffee",        Icons.Outlined.Coffee),
    AppIcon("local_pizza",      "Pizza",         Icons.Outlined.LocalPizza),
    AppIcon("fastfood",         "Fast Food",     Icons.Outlined.Fastfood),
    AppIcon("local_bar",        "Bar/Drinks",    Icons.Outlined.LocalBar),
    AppIcon("icecream",         "Ice Cream",     Icons.Outlined.Icecream),

    // ── Shopping (5) ──────────────────────────────────────────────────────────
    AppIcon("shopping_bag",     "Shopping",      Icons.Outlined.ShoppingBag),
    AppIcon("shopping_cart",    "Online Order",  Icons.Outlined.ShoppingCart),
    AppIcon("storefront",       "Store",         Icons.Outlined.Storefront),
    AppIcon("checkroom",        "Clothing",      Icons.Outlined.Checkroom),
    AppIcon("watch",            "Accessories",   Icons.Outlined.Watch),

    // ── Transportation (7) ────────────────────────────────────────────────────
    AppIcon("car",              "Car",           Icons.Outlined.DirectionsCar),
    AppIcon("bus",              "Bus",           Icons.Outlined.DirectionsBus),
    AppIcon("train",            "Train",         Icons.Outlined.Train),
    AppIcon("flight",           "Flight",        Icons.Outlined.Flight),
    AppIcon("local_taxi",       "Taxi",          Icons.Outlined.LocalTaxi),
    AppIcon("bike",             "Bicycle",       Icons.Outlined.DirectionsBike),
    AppIcon("gas_station",      "Gas Station",   Icons.Outlined.LocalGasStation),

    // ── Health & Wellness (7) ─────────────────────────────────────────────────
    AppIcon("hospital",         "Hospital",      Icons.Outlined.LocalHospital),
    AppIcon("medication",       "Pharmacy",      Icons.Outlined.Medication),
    AppIcon("fitness",          "Gym",           Icons.Outlined.FitnessCenter),
    AppIcon("spa",              "Spa",           Icons.Outlined.Spa),
    AppIcon("health_safety",    "Health",        Icons.Outlined.HealthAndSafety),
    AppIcon("psychology",       "Therapy",       Icons.Outlined.Psychology),
    AppIcon("vaccines",         "Vaccines",      Icons.Outlined.Vaccines),

    // ── Entertainment (7) ─────────────────────────────────────────────────────
    AppIcon("movie",            "Movies",        Icons.Outlined.Movie),
    AppIcon("music_note",       "Music",         Icons.Outlined.MusicNote),
    AppIcon("sports_esports",   "Gaming",        Icons.Outlined.SportsEsports),
    AppIcon("tv",               "TV",            Icons.Outlined.Tv),
    AppIcon("theater",          "Theater",       Icons.Outlined.TheaterComedy),
    AppIcon("palette",          "Art",           Icons.Outlined.Palette),
    AppIcon("camera_alt",       "Photography",   Icons.Outlined.CameraAlt),

    // ── Education (5) ─────────────────────────────────────────────────────────
    AppIcon("school",           "School",        Icons.Outlined.School),
    AppIcon("menu_book",        "Books",         Icons.Outlined.MenuBook),
    AppIcon("computer",         "Computer",      Icons.Outlined.Computer),
    AppIcon("science",          "Science",       Icons.Outlined.Science),
    AppIcon("emoji_events",     "Achievement",   Icons.Outlined.EmojiEvents),

    // ── Finance & Work (7) ────────────────────────────────────────────────────
    AppIcon("work",             "Work/Salary",   Icons.Outlined.Work),
    AppIcon("account_balance",  "Bank",          Icons.Outlined.AccountBalance),
    AppIcon("savings",          "Savings",       Icons.Outlined.Savings),
    AppIcon("trending_up",      "Investment",    Icons.Outlined.TrendingUp),
    AppIcon("credit_card",      "Credit Card",   Icons.Outlined.CreditCard),
    AppIcon("receipt",          "Bills",         Icons.Outlined.Receipt),
    AppIcon("business",         "Business",      Icons.Outlined.BusinessCenter),

    // ── Travel (5) ────────────────────────────────────────────────────────────
    AppIcon("hotel",            "Hotel",         Icons.Outlined.Hotel),
    AppIcon("luggage",          "Luggage",       Icons.Outlined.Luggage),
    AppIcon("beach",            "Beach",         Icons.Outlined.BeachAccess),
    AppIcon("map",              "Navigation",    Icons.Outlined.Map),
    AppIcon("explore",          "Explore",       Icons.Outlined.Explore),

    // ── Nature & Outdoors (6) ─────────────────────────────────────────────────
    AppIcon("park",             "Park/Trees",    Icons.Outlined.Park),
    AppIcon("eco",              "Eco/Leaf",      Icons.Outlined.Eco),
    AppIcon("local_florist",    "Flowers",       Icons.Outlined.LocalFlorist),
    AppIcon("nature_people",    "Nature",        Icons.Outlined.NaturePeople),
    AppIcon("wb_sunny",         "Sunny",         Icons.Outlined.WbSunny),
    AppIcon("terrain",          "Mountain",      Icons.Outlined.Terrain),

    // ── Pets & Animals (4) ────────────────────────────────────────────────────
    AppIcon("pets",             "Dog/Cat",       Icons.Outlined.Pets),
    AppIcon("set_meal",         "Pet Food",      Icons.Outlined.SetMeal),
    AppIcon("cruelty_free",     "Small Pets",    Icons.Outlined.CrueltyFree),
    AppIcon("emoji_nature",     "Wildlife",      Icons.Outlined.EmojiNature),

    // ── Family & Kids (5) ────────────────────────────────────────────────────
    AppIcon("child_care",       "Childcare",     Icons.Outlined.ChildCare),
    AppIcon("cake",             "Birthday",      Icons.Outlined.Cake),
    AppIcon("card_giftcard",    "Gifts",         Icons.Outlined.CardGiftcard),
    AppIcon("family",           "Family",        Icons.Outlined.FamilyRestroom),
    AppIcon("smart_toy",        "Toys",          Icons.Outlined.SmartToy),

    // ── Personal Care (4) ────────────────────────────────────────────────────
    AppIcon("content_cut",      "Haircut",       Icons.Outlined.ContentCut),
    AppIcon("face",             "Face Care",     Icons.Outlined.Face),
    AppIcon("self_improve",     "Meditation",    Icons.Outlined.SelfImprovement),
    AppIcon("laundry",          "Laundry",       Icons.Outlined.LocalLaundryService),

    // ── Sports (6) ───────────────────────────────────────────────────────────
    AppIcon("soccer",           "Soccer",        Icons.Outlined.SportsSoccer),
    AppIcon("basketball",       "Basketball",    Icons.Outlined.SportsBasketball),
    AppIcon("tennis",           "Tennis",        Icons.Outlined.SportsTennis),
    AppIcon("running",          "Running",       Icons.Outlined.DirectionsRun),
    AppIcon("pool",             "Swimming",      Icons.Outlined.Pool),
    AppIcon("hiking",           "Hiking",        Icons.Outlined.Hiking),

    // ── Misc (4) ─────────────────────────────────────────────────────────────
    AppIcon("volunteer",        "Charity",       Icons.Outlined.VolunteerActivism),
    AppIcon("subscriptions",    "Subscriptions", Icons.Outlined.Subscriptions),
    AppIcon("star",             "Favourite",     Icons.Outlined.Star),
    AppIcon("more_horiz",       "Other",         Icons.Outlined.MoreHoriz),
)

fun iconById(id: String): ImageVector =
    AppIcons.firstOrNull { it.id == id }?.vector ?: Icons.Outlined.MoreHoriz
