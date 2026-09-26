package com.dd3boh.outertune.remote.js

import android.content.Context
import android.util.Log
import com.dd3boh.outertune.remote.CustomSourceStore
import com.dd3boh.outertune.remote.RemoteLyric

/**
 * Owns the single **active** lx-music user-script engine (at most one script active at a time,
 * mirroring lx-music-mobile's `setUserApi`).
 *
 * The active script acts as an *enhancement layer* over the built-in sources: it does not provide
 * search. When playback needs a musicUrl / lyric / pic for a built-in source (kw/kg/tx/wy/mg) and
 * the active script declared support for that source+action, we ask the script first.
 */
class LxScriptManager(private val context: Context) {

    private val store = CustomSourceStore(context)
    private val jsStore = JsScriptStore(context)

    @Volatile
    private var engine: LxScriptEngine? = null

    /** id of the currently active CustomSource (an isJs entry), or null. */
    @Volatile
    var activeId: String? = null
        private set

    /**
     * Reload the engine to match [CustomSourceStore.activeJsId]. Called lazily before any script
     * use (so settings changes take effect on next playback without cross-component wiring), and
     * explicitly from the settings screen after activation.
     */
    fun reload() {
        val want = store.activeJsId()
        if (want == activeId && engine != null) return
        release()
        if (want.isNullOrBlank()) return
        val src = store.get(want)?.takeIf { it.isJs } ?: return
        val script = jsStore.read(want) ?: run { release(); return }
        runCatching {
            val header = ScriptHeader.parse(script)
            val eng = LxScriptEngine(
                ScriptInfo(
                    name = header?.name ?: src.name,
                    version = header?.version ?: "",
                    author = header?.author ?: "",
                    homepage = header?.homepage ?: "",
                    description = header?.description ?: "",
                )
            )
            eng.load(script)
            engine = eng
            activeId = want
            Log.i("LxScript", "active script loaded: ${src.name}, sources=${eng.sources}")
        }.onFailure {
            Log.e("LxScript", "failed to load script ${src.name}", it)
            release()
        }
    }

    fun release() {
        runCatching { engine?.close() }
        engine = null
        activeId = null
    }

    fun supports(source: String, action: String): Boolean =
        engine?.sources?.get(source)?.contains(action) == true

    /** True when a script is loaded and usable (even if it declared nothing). */
    val isActive: Boolean get() = engine != null

    // ------------------------------------------------------------------ action attempts

    fun tryMusicUrl(source: String, quality: String, musicInfo: Map<String, Any?>): String? {
        val eng = engine ?: return null
        if (!supports(source, "musicUrl")) return null
        return runCatching {
            val info = mapOf("type" to quality, "musicInfo" to musicInfo)
            val r = eng.callAction(source, "musicUrl", info)
            (r as? String)?.takeIf { it.isNotBlank() && it.startsWith("http") && it.length <= 2048 }
        }.onFailure { Log.e("LxScript", "musicUrl failed for $source", it) }.getOrNull()
    }

    @Suppress("UNCHECKED_CAST")
    fun tryLyric(source: String, musicInfo: Map<String, Any?>): RemoteLyric? {
        val eng = engine ?: return null
        if (!supports(source, "lyric")) return null
        return runCatching {
            val r = eng.callAction(source, "lyric", mapOf("musicInfo" to musicInfo))
            // Result may be a plain LRC string, or an object {lyric, tlyric, rlyric}.
            val (lyricRaw, tRaw, rRaw) = when (r) {
                is Map<*, *> -> Triple(
                    r["lyric"]?.toString(),
                    r["tlyric"]?.toString(),
                    r["rlyric"]?.toString(),
                )
                is String -> Triple(r, null, null)
                else -> return@runCatching null
            }

            // Decode base64-wrapped fields, then require the main lyric to actually contain
            // [mm:ss] timestamps. If it doesn't (e.g. an encrypted binary payload), bail out so
            // the repository falls back to the built-in source lyric instead of feeding base64
            // gibberish to the LRC parser (which would render zero lines -> no scroll/highlight).
            val lyric = decodeLrcField(lyricRaw)
            if (lyric.isNullOrBlank() || !looksLikeLrc(lyric)) {
                Log.w("LxScript", "lyric for $source has no [mm:ss] timestamps after decode, falling back")
                return@runCatching null
            }
            val tlyric = decodeLrcField(tRaw)?.takeIf { looksLikeLrc(it) || it.isNotBlank() }
            val rlyric = decodeLrcField(rRaw)?.takeIf { looksLikeLrc(it) || it.isNotBlank() }

            Log.i("LxScript", "lyric for $source decoded OK, ${lyric.lines().size} lines")
            RemoteLyric(
                lyric = lyric.takeIf { it.length <= 51200 },
                translated = tlyric?.takeIf { it.isNotBlank() && it.length <= 5120 },
                roman = rlyric?.takeIf { it.isNotBlank() && it.length <= 5120 },
            )
        }.onFailure { Log.e("LxScript", "lyric failed for $source", it) }.getOrNull()
    }

    /** True if [s] contains a [mm:ss] / [mm:ss.xx] timestamp tag. */
    private fun looksLikeLrc(s: String): Boolean =
        Regex("""\[\d{1,2}:\d{2}([.:]\d{1,3})?]""").containsMatchIn(s)

    /**
     * If [raw] is plain LRC text, return it. If it looks like base64, decode it and return the
     * decoded bytes only when they are printable LRC text; otherwise return null (so the caller
     * knows the field was not usable).
     */
    private fun decodeLrcField(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim()
        if (looksLikeLrc(s)) return s
        // Heuristic: pure base64, no whitespace, length multiple of 4, reasonably long.
        if (s.length >= 16 && s.length % 4 == 0 &&
            Regex("""^[A-Za-z0-9+/]+={0,2}$""").matches(s)
        ) {
            runCatching {
                val bytes = android.util.Base64.decode(s, android.util.Base64.DEFAULT)
                val decoded = String(bytes, Charsets.UTF_8)
                // Accept only if it decodes to printable LRC-ish text (no NUL / high ratio of
                // non-printable bytes => encrypted binary, reject).
                val printableRatio = decoded.count { it == '\n' || it == '\r' || it == '\t' || it.code in 0x20..0x7E || it.code >= 0xA0 }
                    .toDouble() / decoded.length.coerceAtLeast(1)
                if (printableRatio > 0.9 && looksLikeLrc(decoded)) {
                    return decoded
                }
                Log.w("LxScript", "base64 lyric decoded but not LRC (printable=$printableRatio), rejecting")
            }
        }
        // Not base64 / not LRC shaped: treat as unusable for timed lyrics.
        return null
    }

    fun tryPic(source: String, musicInfo: Map<String, Any?>): String? {
        val eng = engine ?: return null
        if (!supports(source, "pic")) return null
        return runCatching {
            val r = eng.callAction(source, "pic", mapOf("musicInfo" to musicInfo))
            (r as? String)?.takeIf { it.isNotBlank() && it.startsWith("http") && it.length <= 2048 }
        }.onFailure { Log.e("LxScript", "pic failed for $source", it) }.getOrNull()
    }
}
