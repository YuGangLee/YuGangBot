package co.yugang.bot.plugins

import co.yugang.bot.net.DefaultClient
import co.yugang.bot.utils.BotHelper
import co.yugang.bot.utils.ClassTypeHelper
import co.yugang.bot.utils.GsonUtils
import co.yugang.bot.utils.parseString
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.GroupMessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.sendAsImageTo
import net.mamoe.mirai.message.uploadAsImage
import okhttp3.Request
import java.io.File
import java.net.URL
import java.security.MessageDigest

class EroPicture : BasePlugin("色图机") {
    companion object {
        var APIKEY_PATH = "res/apikey.txt"
        var KEY_MAP_PATH = "res/ero_picture_key_map.json"
        var DEFAULT_CD = 15 * 1000

        private const val CODE_SUCCESS = 0
        private const val DAYLY_COUNT = 250
        private const val TEMP_PATH = "temp"

        private val COMMAND_HEAD = arrayOf("来份", "来点", "来张")
        private val COMMAND_TAIL = arrayOf("色图", "涩图", "瑟图")

        private val ERROR_CODE_MAP: Map<Int, String> = mapOf(
            Pair(-1, "API内部错误"),
            Pair(401, "BOT被封禁"),
            Pair(403, "我出毛病了: 403"),
            Pair(404, "找不到符合关键字的色图"),
            Pair(429, "达到调用额度限制")
        )
    }

    private val picApiPath by lazy {
        try {
            val file = File(APIKEY_PATH)
            if (file.exists()) {
                "https://api.lolicon.app/setu/?apikey=${file.readText().trim()}&size1200="
            } else {
                "https://api.lolicon.app/setu/?size1200="
            }
        } catch (e: Exception) {
            BotHelper.loge(e.toString())
            "https://api.lolicon.app/setu/?size1200="
        }
    }

    private var group: Group? = null
    private var lastTime = 0L

    private val keyMap = mutableMapOf<String, String>()

    private val md5 = MessageDigest.getInstance("MD5")

    var enableKeyMap = false
        set(value) {
            field = value
            keyMap.clear()
            if (field) try {
                val file = File(KEY_MAP_PATH)
                val json = file.readText()
                val keyMapList: List<KeyMap> = GsonUtils.instance.fromJson(json, ClassTypeHelper.typeOf<List<KeyMap>>())
                keyMapList.forEach {
                    keyMap[it.key] = it.place
                }
            } catch (e: Throwable) {
                BotHelper.loge(e.toString())
            }
        }

    override suspend fun onMessage(message: GroupMessageEvent) {
        group = message.group
        val msg = message.message.parseString() ?: ""
        COMMAND_HEAD.forEach { head ->
            COMMAND_TAIL.forEach { tail ->
                if (msg.startsWith(head) && msg.endsWith(tail)) {
                    val now = System.currentTimeMillis()
                    if (lastTime + DEFAULT_CD > now) {
                        group?.sendMessage("色图机还有${(lastTime + DEFAULT_CD - now) / 1000}秒CD")
                        return@onMessage
                    }
                    var key = msg.replaceFirst(head, "")
                        .reversed()
                        .replaceFirst(tail.reversed(), "")
                        .reversed()
                        .trim()
                    if (enableKeyMap && keyMap.keys.contains(key)) {
                        key = keyMap[key] ?: ""
                    }
                    val url = "$picApiPath&keyword=$key"
                    getPicture(url).catch { e ->
                        if (e is PictureException) {
                            group?.sendMessage("${ERROR_CODE_MAP[e.code]}")
                        } else {
                            group?.sendMessage("没拿到色图…")
                        }
                    }.collect { picUrl ->
                        try {
                            lastTime = now
                            group?.let {
                                val id = URL(picUrl).openStream().uploadAsImage(it).imageId
                                it.sendMessage(Image(id))
                            }
                        } catch (e: Throwable) {
                            group?.sendMessage("图片发送失败")
                        }
                    }
                }
            }
        }
    }

    private fun getPicture(url: String) = flow {
        val response = DefaultClient.httpClient.newCall(Request.Builder().url(url).build()).execute()
        val json = response.body?.string() ?: ""
        val result = GsonUtils.instance.fromJson(json, PictureResult::class.java)
        if (result.code == CODE_SUCCESS && result.picList.isNotEmpty()) {
            emit(result.picList[0].url)
        } else if (ERROR_CODE_MAP[result.code] != null) {
            throw PictureException(result.code)
        } else {
            throw NullPointerException()
        }
    }

    class PictureException(val code: Int) : Exception()

    data class KeyMap(@SerializedName("key") val key: String, @SerializedName("place") val place: String)

    data class PictureResult(
        @SerializedName("code")
        val code: Int,
        @SerializedName("count")
        val count: Int,
        @SerializedName("data")
        val picList: List<PictureData>,
        @SerializedName("msg")
        val msg: String,
        @SerializedName("quota")
        val quota: Int,
        @SerializedName("quota_min_ttl")
        val quotaMinTtl: Int
    )

    data class PictureData(
        @SerializedName("author")
        val author: String,
        @SerializedName("height")
        val height: Int,
        @SerializedName("p")
        val p: Int,
        @SerializedName("pid")
        val pid: Int,
        @SerializedName("r18")
        val r18: Boolean,
        @SerializedName("tags")
        val tags: List<String>,
        @SerializedName("title")
        val title: String,
        @SerializedName("uid")
        val uid: Int,
        @SerializedName("url")
        val url: String,
        @SerializedName("width")
        val width: Int
    )
}