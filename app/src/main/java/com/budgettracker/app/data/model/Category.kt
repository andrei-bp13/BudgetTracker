package com.pardonmyspending.app.data.model

// Legacy helpers kept for CSV compatibility -" resolved at runtime via CategoryRepository
object LegacyCategories {
    val knownNames: Set<String> = (DefaultCategories).map { it.name }.toSet()
}
