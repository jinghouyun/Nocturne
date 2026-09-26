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
            Log.d("LyricDebug", "[$source] raw JS result type=${r?.javaClass?.name}, len=${r?.toString()?.length}, preview=${r?.toString()?.take(200)}")
            // Result may be a plain LRC string, or an object {lyric, tlyric, rlyric}.
            val (lyricRaw, tRaw, rRaw) = when (r) {
                is Map<*, *> -> Triple(
                    r["lyric"]?.toString(),
                    r["tlyric"]?.toString(),
                    r["rlyric"]?.toString(),
                )
                is String -> Triple(r, null, null)
                else -> {
                    Log.w("LyricDebug", "[$source] unexpected lyric result type=${r?.javaClass?.name}")
                    return@runCatching null
                }
            }

            val lyric = decodeLrcField(lyricRaw, "lyric")
            if (lyric.isNullOrBlank() || !looksLikeLrc(lyric)) {
                Log.w("LyricDebug", "[$source] lyric has no [mm:ss] timestamps after decode, falling back to built-in")
                return@runCatching null
            }
            val tlyric = decodeLrcField(tRaw, "tlyric")
            val rlyric = decodeLrcField(rRaw, "rlyric")

            Log.i("LyricDebug", "[$source] lyric decoded OK, ${lyric.lines().size} lines, preview=${lyric.take(120)}")
            RemoteLyric(
                lyric = lyric.takeIf { it.length <= 51200 },
                translated = tlyric?.takeIf { it.isNotBlank() && it.length <= 5120 },
                roman = rlyric?.takeIf { it.isNotBlank() && it.length <= 5120 },
            )
        }.onFailure { Log.e("LyricDebug", "[$source] lyric failed", it) }.getOrNull()
    }

    /** True if [s] contains a [mm:ss] / [mm:ss.xx] timestamp tag. */
    private fun looksLikeLrc(s: String): Boolean =
        Regex("""\[\d{1,2}:\d{2}([.:]\d{1,3})?]""").containsMatchIn(s)

    /**
     * Decode one lyric field. Accept plain LRC as-is. If it looks like base64, decode and then
     * try (in order) plain UTF-8, gzip, and zlib inflate. Return only if the final text looks
     * like LRC (has [mm:ss] tags); otherwise null (so caller falls back).
     */
    private fun decodeLrcField(raw: String?, tag: String): String? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim()
        if (looksLikeLrc(s)) {
            Log.d("LyricDebug", "field $tag: plain LRC, len=${s.length}")
            return s
        }
        // Heuristic: pure base64, no whitespace, length multiple of 4, reasonably long.
        if (s.length >= 16 && s.length % 4 == 0 &&
            Regex("""^[A-Za-z0-9+/=\s]+$""").matches(s)
        ) {
            runCatching {
                val bytes = android.util.Base64.decode(s, android.util.Base64.DEFAULT)
                Log.d("LyricDebug", "field $tag: base64 decoded -> ${bytes.size} bytes, head=${bytes.take(8).joinToString(" ") { "%02x".format(it) }}")
                // 1) direct UTF-8
                String(bytes, Charsets.UTF_8).let { if (looksLikeLrc(it)) { Log.d("LyricDebug","field $tag: direct UTF8 OK"); return it } }
                // 2) gzip
                runCatching {
                    java.util.zip.GZIPInputStream(bytes.inputStream()).use { it.readBytes().toString(Charsets.UTF_8) }
                }.getOrNull()?.let { if (looksLikeLrc(it)) { Log.d("LyricDebug","field $tag: gzip OK"); return it } }
                // 3) zlib inflate
                runCatching { inflateZlib(bytes) }.getOrNull()?.let { if (looksLikeLrc(it)) { Log.d("LyricDebug","field $tag: zlib OK"); return it } }
                Log.w("LyricDebug", "field $tag: base64 decoded but no LRC timestamps after UTF8/gzip/zlib, rejecting")
            }.onFailure { Log.w("LyricDebug", "field $tag: base64 decode failed", it) }
        }
        return null
    }

    /** zlib-deflate [bytes] to a UTF-8 string. */
    private fun inflateZlib(bytes: ByteArray): String {
        val inflater = java.util.zip.Inflater()
        inflater.setInput(bytes)
        val out = java.io.ByteArrayOutputStream(bytes.size * 4)
        val buf = ByteArray(8192)
        try {
            while (!inflater.finished()) {
                val n = inflater.inflate(buf)
                if (n == 0) break
                out.write(buf, 0, n)
            }
        } finally {
            inflater.end()
        }
        return out.toString(Charsets.UTF_8.name())
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
