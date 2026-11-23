package com.pardonmyspending.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pardonmyspending.app.data.model.CategoryItem
import com.pardonmyspending.app.data.model.CategoryType
import com.pardonmyspending.app.data.model.DefaultCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

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

    // â"€â"€ Serialization â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    private fun List<CategoryItem>.serialize(): String {
        val arr = JSONArray()
        forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("iconId", item.iconId)
                put("colorHex", item.colorHex)
                put("isDefault", item.isDefault)
                put("type", item.type.name)
            })
        }
        return arr.toString()
    }

    private fun String.deserialize(): List<CategoryItem> {
        if (isBlank()) return emptyList()
        val trimmed = trimStart()
        if (trimmed.startsWith("[")) {
            return runCatching {
                val arr = JSONArray(trimmed)
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    val id = o.optString("id", "").ifBlank { return@mapNotNull null }
                    CategoryItem(
                        id        = id,
                        name      = o.optString("name", ""),
                        iconId    = o.optString("iconId", "more_horiz"),
                        colorHex  = o.optString("colorHex", "#757575"),
                        isDefault = o.optBoolean("isDefault", true),
                        type      = runCatching { CategoryType.valueOf(o.optString("type", "EXPENSE")) }
                            .getOrDefault(CategoryType.EXPENSE)
                    )
                }
            }.getOrDefault(emptyList())
        }
        // Legacy ";;;" / "~~" format fallback for existing installs
        return split(RECORD_SEP).mapNotNull { record ->
            val f = record.split(FIELD_SEP)
            if (f.size < 5) null
            else CategoryItem(
                id        = f[0],
                name      = f[1],
                iconId    = f[2],
                colorHex  = f[3],
                isDefault = f[4].toBooleanStrictOrNull() ?: true,
                type      = if (f.size >= 6) runCatching { CategoryType.valueOf(f[5]) }.getOrDefault(CategoryType.EXPENSE)
                            else CategoryType.EXPENSE
            )
        }
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
            // Always inherit type from DefaultCategories so stale serialized data
            // (written before the type field existed) gets the correct type.
            DefaultCategories.forEach { def ->
                add(savedById[def.id]?.copy(type = def.type) ?: def)
            }
            saved.filter { it.id !in defaultIds }.forEach { add(it) }
        }
    }
}
