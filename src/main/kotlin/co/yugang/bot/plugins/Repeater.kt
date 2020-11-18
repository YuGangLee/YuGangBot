package co.yugang.bot.plugins

import co.yugang.bot.utils.BotHelper
import co.yugang.bot.utils.parseString
import net.mamoe.mirai.message.GroupMessageEvent
import net.mamoe.mirai.message.code.parseMiraiCode
import kotlin.random.Random

class Repeater : BasePlugin("复读机") {
    companion object {
        private const val DEFAULT_REPEAT_COUNT = 3
    }

    private val messageBuffer = mutableMapOf<Long, LastMessage>()

    override suspend fun onMessage(message: GroupMessageEvent) {
        val group = message.group
        val groupId = group.id
        val userId = message.sender.id
        val msg = message.message.parseString()
        val lastMessage = messageBuffer[groupId] ?: LastMessage()

        if (msg == null) {
            messageBuffer[groupId] = lastMessage.also {
                it.msg = null
                it.repeated = false
                it.userSet.clear()
            }
            return
        }

        if (!lastMessage.repeated && lastMessage.msg == msg) {
            if (lastMessage.userSet.size < DEFAULT_REPEAT_COUNT
                && !lastMessage.userSet.contains(userId)
            ) {
                lastMessage.userSet.add(userId)
            }
            lastMessage.repeated = lastMessage.userSet.size >= DEFAULT_REPEAT_COUNT

            val repeat = lastMessage.repeated && Random(942).nextBoolean()
            BotHelper.logi("random result $repeat")
            if (repeat) {
                group.sendMessage(lastMessage.msg?.parseMiraiCode() ?: return)
            }
        } else if (!lastMessage.repeated) {
            lastMessage.msg = msg
            lastMessage.userSet.clear()
            lastMessage.userSet.add(userId)
        } else if (lastMessage.msg != msg) {
            lastMessage.msg = msg
            lastMessage.repeated = false
            lastMessage.userSet.clear()
            lastMessage.userSet.add(userId)
        }

        messageBuffer[groupId] = lastMessage

        BotHelper.logi("$messageBuffer")
    }

    data class LastMessage(
        var msg: String? = null,
        var repeated: Boolean = false,
        val userSet: MutableSet<Long> = mutableSetOf()
    )
}