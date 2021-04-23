package co.yugang.bot.plugins

import co.yugang.bot.net.DefaultClient
import co.yugang.bot.utils.BotHelper
import co.yugang.bot.utils.ClassTypeHelper
import co.yugang.bot.utils.GsonUtils
import co.yugang.bot.utils.parseString
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.GroupMessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageSourceBuilder
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.message.data.sendTo
import net.mamoe.mirai.message.sendAsImageTo
import net.mamoe.mirai.message.uploadAsImage
import net.mamoe.mirai.utils.toExternalImage
import net.mamoe.mirai.utils.upload
import okhttp3.Request
import java.io.File
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

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

    private var lastTime = 0L

    private val keyMap = mutableMapOf<String, String>()

    private val md5 = MessageDigest.getInstance("MD5")

    private val sendQueue = ConcurrentLinkedQueue<SendTask>()

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

    init {
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                if (sendQueue.isEmpty()) {
                    delay(1000)
                } else {
                    while (sendQueue.isNotEmpty()) {
                        val task = sendQueue.poll()
                        task.run()
                    }
                }
            }
        }
    }

    override suspend fun onMessage(message: GroupMessageEvent) {
        val group = message.group
        val msg = message.message.parseString() ?: ""
        COMMAND_HEAD.forEach { head ->
            COMMAND_TAIL.forEach { tail ->
                if (msg.startsWith(head) && msg.endsWith(tail)) {
                    var key = msg.replaceFirst(head, "")
                        .reversed()
                        .replaceFirst(tail.reversed(), "")
                        .reversed()
                        .trim()
                    if (enableKeyMap && keyMap.keys.contains(key)) {
                        key = keyMap[key] ?: ""
                    }
                    sendQueue.add(SendTask(group, key))
                }
            }
        }
    }

    private inner class SendTask(val group: Group, val key: String) {
        suspend fun run() {
            val url = "$picApiPath&keyword=$key&r18=0"
            val response = DefaultClient.httpClient.newCall(Request.Builder().url(url).build()).execute()
            val json = response.body?.string() ?: ""
            val result = GsonUtils.instance.fromJson(json, PictureResult::class.java)
            if (result.code == CODE_SUCCESS && result.picList.isNotEmpty()) {
                try {
                    group.let {
                        buildMessageChain {
                            add("你点的${key}色图到了")
                            add(URL(result.picList[0].url).openStream().toExternalImage().upload(group))
                        }.sendTo(it)
//                        URL(result.picList[0].url).openStream().sendAsImageTo(it)
                    }
                } catch (e: Throwable) {
                    group.sendMessage("图片发送失败")
                }
            } else if (result.code == 404) {
                group.sendMessage("找不到符合关键字${key}的色图")
            } else if (ERROR_CODE_MAP[result.code] != null) {
                group.sendMessage("${ERROR_CODE_MAP[result.code]}")
            } else {
                group.sendMessage("没拿到色图…")
            }
        }
    }

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