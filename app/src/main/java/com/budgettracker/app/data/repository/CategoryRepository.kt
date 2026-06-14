package com.pardonmyspending.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pardonmyspending.app.data.model.CategoryItem
import com.pardonmyspending.app.data.model.DefaultCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.categoryDataStore by preferencesDataStore(name = "categories")

class CategoryRepository(private val context: Context) {

    companion object {
        private val CUSTOM_KEY = stringPreferencesKey("custom_categories")
        // Separators unlikely to appear in user text
        private const val RECORD_SEP = ";;;"
        private const val FIELD_SEP  = "~~"
    }

    /** Emits the full merged list: defaults (possibly overridden) + custom additions. */
    val categories: Flow<List<CategoryItem>> = context.categoryDataStore.data.map { prefs ->
        val custom = prefs[CUSTOM_KEY]?.deserialize() ?: emptyList()
        mergeWithDefaults(custom)
    }

    suspend fun save(items: List<CategoryItem>) {
        // Only persist items that differ from defaults OR are pure custom
        val toSave = items.filter { item ->
            val def = DefaultCategories.firstOrNull { it.id == item.id }
            def == null || def != item           // custom OR modified default
        }
        context.categoryDataStore.edit { prefs ->
            prefs[CUSTOM_KEY] = toSave.serialize()
        }
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    private fun List<CategoryItem>.serialize(): String =
        joinToString(RECORD_SEP) { "${it.id}${FIELD_SEP}${it.name}${FIELD_SEP}${it.iconId}${FIELD_SEP}${it.colorHex}${FIELD_SEP}${it.isDefault}" }

    private fun String.deserialize(): List<CategoryItem> =
        if (isBlank()) emptyList()
        else split(RECORD_SEP).mapNotNull { record ->
            val f = record.split(FIELD_SEP)
            if (f.size < 5) null
            else CategoryItem(f[0], f[1], f[2], f[3], f[4].toBooleanStrictOrNull() ?: true)
        }

    /** Replaces or appends [item] in [currentList] and persists the result. */
    suspend fun upsertCategory(currentList: List<CategoryItem>, item: CategoryItem) {
        val idx = currentList.indexOfFirst { it.id == item.id }
        val updated = if (idx >= 0) currentList.toMutableList().also { it[idx] = item }
                      else currentList + item
        save(updated)
    }

    private fun mergeWithDefaults(saved: List<CategoryItem>): List<CategoryItem> {
        val savedById  = saved.associateBy { it.id }
        val defaultIds = DefaultCategories.map { it.id }.toSet()
        return buildList {
            DefaultCategories.forEach { def -> add(savedById[def.id] ?: def) }
            saved.filter { it.id !in defaultIds }.forEach { add(it) }
        }
    }
}
