package com.nocturne.player.remote

import android.content.Context
import com.nocturne.player.constants.SourceNameDisplayKey
import com.nocturne.player.utils.dataStore
import com.nocturne.player.utils.get

/** Resolves a display label for a sourceId, honoring the original/alias preference. */
object SourceLabel {
    fun name(context: Context, sourceId: String?): String {
        if (sourceId.isNullOrBlank()) return ""
        val alias = runCatching { context.dataStore.get(SourceNameDisplayKey, "original") == "alias" }.getOrDefault(false)
        return when (sourceId) {
            "wy" -> if (alias) "wy" else "网易云音乐"
            "mg" -> if (alias) "mg" else "咪咕音乐"
            "tx" -> if (alias) "tx" else "QQ音乐"
            "kg" -> if (alias) "kg" else "酷狗音乐"
            "kw" -> if (alias) "kw" else "酷我音乐"
            else -> if (sourceId.startsWith("custom_")) {
                runCatching {
                    CustomSourceStore(context).get(sourceId.removePrefix("custom_"))?.name ?: sourceId
                }.getOrDefault(sourceId)
            } else sourceId
        }
    }
}
