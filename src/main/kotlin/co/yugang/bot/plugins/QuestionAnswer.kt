package co.yugang.bot.plugins

import co.yugang.bot.plugins.QuestionAnswer.MatchType.Companion.parseMatchType
import co.yugang.bot.utils.GsonUtils
import co.yugang.bot.utils.parseString
import net.mamoe.mirai.message.GroupMessageEvent
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.sendAsImageTo
import java.io.File
import kotlin.random.Random


class QuestionAnswer : BasePlugin("人工智障") {
    companion object {
        const val QA_FILE_PATH = "res/question_and_answer.json"
        const val IMAGE_RESOURCE_PATH = "res/image"
    }

    private val questionAndAnswer: QuestionAndAnswer

    init {
        val qaFile = File(QA_FILE_PATH)
        questionAndAnswer = try {
            val json = qaFile.readText().trim()
            GsonUtils.instance.fromJson(json, QuestionAndAnswer::class.java)
        } catch (e: Throwable) {
            QuestionAndAnswer("", listOf())
        }
    }

    override suspend fun onMessage(message: GroupMessageEvent) {
        val group = message.group
        val msg = message.message.parseString() ?: ""
        val response = mutableListOf<Qa>()
        questionAndAnswer.qaList.forEach { qa ->
            val match = when (qa.matchType.parseMatchType()) {
                MatchType.FULL -> msg == qa.question
                MatchType.REGULAR -> qa.question.toRegex().matches(msg)
                MatchType.PART -> msg.contains(qa.question)
                else -> false
            }
            if (match) {
                response.add(qa)
            }
        }
        if (response.isNotEmpty()) {
            val index = (0 until response.size).random()
            sendMessage(group, response[index])
        }
    }

    private suspend fun sendMessage(group: Group, qa: Qa) {
        if (qa.answer.startsWith(questionAndAnswer.imageHead)) {
            File(IMAGE_RESOURCE_PATH, qa.answer.replaceFirst(questionAndAnswer.imageHead, "")).let {
                if (it.exists()) {
                    it.sendAsImageTo(group)
                }
            }
        } else {
            group.sendMessage(qa.answer)
        }
    }

    enum class MatchType(val typeName: String) {
        FULL("full"),
        REGULAR("regular"),
        PART("part");

        companion object {
            fun parseString(name: String) = when (name) {
                FULL.typeName -> FULL
                REGULAR.typeName -> REGULAR
                PART.typeName -> PART
                else -> null
            }

            fun String.parseMatchType() = parseString(this)
        }
    }

    data class QuestionAndAnswer(
        @SerializedName("imageHead")
        val imageHead: String,
        @SerializedName("qaList")
        val qaList: List<Qa>
    )

    data class Qa(
        @SerializedName("answer")
        val answer: String,
        @SerializedName("matchType")
        val matchType: String,
        @SerializedName("question")
        val question: String
    )
}