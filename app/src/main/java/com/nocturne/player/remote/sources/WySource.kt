package com.nocturne.player.remote.sources

import com.nocturne.player.remote.RemoteHttp
import com.nocturne.player.remote.RemoteLyric
import com.nocturne.player.remote.RemoteMusicSource
import com.nocturne.player.remote.RemoteSong
import com.nocturne.player.remote.crypto.WyCrypto
import org.json.JSONObject

/**
 * NetEase Cloud Music (wy) source.
 * Search via weapi cloudsearch/pc, stream URL via weapi player/url, lyric via eapi.
 */
object WySource : RemoteMusicSource {

    override val sourceId = "wy"
    override val displayName = "网易云"

    private const val UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36"

    override fun search(query: String, page: Int, limit: Int): List<RemoteSong> {
        val offset = limit * (page - 1)
        // The weapi cloudsearch endpoint now rejects requests without a logged-in cookie.
        // Use the legacy open search endpoint which still returns results unencrypted.
        val url = "https://music.163.com/api/search/get/web" +
            "?s=" + java.net.URLEncoder.encode(query, "UTF-8") +
            "&type=1&offset=$offset&limit=$limit"
        val headers = mapOf("User-Agent" to UA, "Referer" to "https://music.163.com")
        val resp = RemoteHttp.get(url, headers)
        val json = JSONObject(resp)
        val songs = json.optJSONObject("result")?.optJSONArray("songs") ?: return emptyList()

        // Collect raw rows first; the legacy search endpoint has no picUrl, so fetch covers
        // in ONE batched song-detail request below.
        data class Row(val id: String, val name: String, val artists: List<String>, val albumName: String?, val durationSec: Int)
        val rows = ArrayList<Row>()
        for (i in 0 until songs.length()) {
            val item = songs.getJSONObject(i)
            val id = item.getLong("id").toString()
            val ar = item.optJSONArray("artists")
            val artists = mutableListOf<String>()
            ar?.let { for (j in 0 until it.length()) artists.add(it.getJSONObject(j).optString("name")) }
            val al = item.optJSONObject("album")
            rows.add(
                Row(
                    id = id,
                    name = item.optString("name"),
                    artists = artists,
                    albumName = al?.optString("name")?.takeIf { it != "null" },
                    durationSec = (item.optLong("duration", 0) / 1000).toInt()
                )
            )
        }
        val picMap = HashMap<String, String>()
        runCatching {
            if (rows.isNotEmpty()) {
                val idsJson = rows.joinToString(",", "[", "]") { it.id }
                val detail = JSONObject(
                    RemoteHttp.get(
                        "https://music.163.com/api/song/detail/?ids=$idsJson", headers
                    )
                )
                detail.optJSONArray("songs")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val s = arr.getJSONObject(i)
                        val pic = s.optJSONObject("album")?.optString("picUrl")
                        if (!pic.isNullOrEmpty() && pic != "null") picMap[s.getLong("id").toString()] = pic
                    }
                }
            }
        }
        return rows.map {
            RemoteSong(
                id = "NMwy${it.id}",
                source = sourceId,
                sourceSongId = it.id,
                title = it.name,
                artists = it.artists,
                albumName = it.albumName,
                durationSec = it.durationSec,
                thumbnailUrl = picMap[it.id],
                extra = emptyMap()
            )
        }
    }

    override fun resolveStreamUrl(song: RemoteSong, quality: String): String? {
        val id = song.sourceSongId.toLongOrNull() ?: return null
        val br = when (quality) {
            "flac" -> 999000
            "320k" -> 320000
            else -> 128000
        }
        val payload = JSONObject()
            .put("ids", org.json.JSONArray().put(id))
            .put("br", br)
            .toString()
        val form = WyCrypto.weapi(JSONObject(payload))
        val headers = mapOf("User-Agent" to UA, "Referer" to "https://music.163.com")
        val resp = RemoteHttp.postForm(
            "https://music.163.com/weapi/song/enhance/player/url", form, headers
        )
        return runCatching {
            val data = JSONObject(resp).getJSONArray("data")
            if (data.length() == 0) return null
            val url = data.getJSONObject(0).optString("url")
            if (url.isNullOrEmpty() || url == "null") null else url
        }.getOrNull()
    }

    override fun getLyric(song: RemoteSong): RemoteLyric? {
        val id = song.sourceSongId.toLongOrNull() ?: return null
        // The eapi /api/song/lyric/v1 endpoint rejects requests without a logged-in
        // cookie (returns empty / error), same as weapi cloudsearch. Use the legacy
        // open lyric endpoint which still returns the full LRC unencrypted.
        val url = "https://music.163.com/api/song/lyric?id=$id&lv=-1&kv=-1&tv=-1"
        val headers = mapOf("User-Agent" to UA, "Referer" to "https://music.163.com")
        val resp = RemoteHttp.get(url, headers)
        return runCatching {
            val json = JSONObject(resp)
            val lrc = json.optJSONObject("lrc")?.optString("lyric")
            val tlyric = json.optJSONObject("tlyric")?.optString("lyric")
            val rlyric = json.optJSONObject("romalrc")?.optString("lyric")
            RemoteLyric(
                lyric = lrc?.takeIf { it.isNotEmpty() },
                translated = tlyric?.takeIf { it.isNotEmpty() },
                roman = rlyric?.takeIf { it.isNotEmpty() }
            )
        }.getOrNull()
    }

    /** NetEase hot search words (top 10). */
    fun getHotSearch(): List<String> {
        return runCatching {
            val payload = JSONObject()
                .put("id", "HOT_SEARCH_SONG#@#")
            val form = WyCrypto.eapi("/api/search/chart/detail", payload)
            val headers = mapOf("User-Agent" to UA)
            val resp = RemoteHttp.postForm(
                "https://interface3.music.163.com/eapi/search/chart/detail", form, headers
            )
            val json = JSONObject(resp)
            val items = json.optJSONObject("data")?.optJSONArray("itemList") ?: return emptyList()
            (0 until items.length()).mapNotNull { i ->
                items.getJSONObject(i).optString("searchWord").takeIf { it.isNotEmpty() }
            }.take(10)
        }.getOrDefault(emptyList())
    }

    /** NetEase rank boards list. */
    fun getRankBoards(): List<Pair<String, String>> {
        return runCatching {
            val form = WyCrypto.weapi(JSONObject())
            val headers = mapOf("User-Agent" to UA, "Referer" to "https://music.163.com")
            val resp = RemoteHttp.postForm(
                "https://music.163.com/weapi/toplist", form, headers
            )
            val json = JSONObject(resp)
            val list = json.optJSONArray("list") ?: return emptyList()
            (0 until list.length()).mapNotNull { i ->
                val o = list.getJSONObject(i)
                val id = o.optLong("id").toString()
                val name = o.optString("name")
                if (id.isNotEmpty() && name.isNotEmpty()) id to name else null
            }.take(15)
        }.getOrDefault(emptyList())
    }

    /** NetEase rank board songs. */
    fun getRankSongs(boardId: String, limit: Int = 50): List<RemoteSong> {
        return runCatching {
            val payload = JSONObject()
                .put("id", boardId.toLong())
                .put("n", limit)
            val form = WyCrypto.weapi(payload)
            val headers = mapOf("User-Agent" to UA, "Referer" to "https://music.163.com")
            val resp = RemoteHttp.postForm(
                "https://music.163.com/weapi/v3/playlist/detail", form, headers
            )
            val json = JSONObject(resp)
            val tracks = json.optJSONObject("playlist")?.optJSONArray("tracks") ?: return emptyList()
            (0 until tracks.length()).mapNotNull { i ->
                val item = tracks.getJSONObject(i)
                val id = item.getLong("id").toString()
                val name = item.optString("name")
                val ar = item.optJSONArray("ar")
                val artists = mutableListOf<String>()
                ar?.let { for (j in 0 until it.length()) artists.add(it.getJSONObject(j).optString("name")) }
                val al = item.optJSONObject("al")
                val pic = al?.optString("picUrl")?.let { if (it.startsWith("http://")) "https://" + it.substring(7) else it }
                RemoteSong(
                    id = "NMwy$id", source = "wy", sourceSongId = id,
                    title = name, artists = artists, albumName = al?.optString("name"),
                    durationSec = item.optInt("dt", 0) / 1000, thumbnailUrl = pic,
                )
            }
        }.getOrDefault(emptyList())
    }
}
