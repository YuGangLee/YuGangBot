package co.yugang.bot.utils

import mu.KotlinLogging
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.isAdministrator
import net.mamoe.mirai.contact.isOwner
import java.io.File

object BotHelper {
    private val logger = KotlinLogging.logger {}

    // Set up a bot owner using res/owner.txt
    private var botOwner: Long? = null
    private val BOT_NAME = arrayOf("bot", "Bot", "BOT", "波特", "机器人")

    fun loadConfig() {
        val ownerConfigFile = File("res/owner.txt")
        if (!ownerConfigFile.exists()) return
        botOwner = ownerConfigFile.readText().trim().toLong()
        logger.info { "Bot admin set to $botOwner" }
    }

    fun memberIsAdmin(member: Member): Boolean {
        return member.isAdministrator() || member.isOwner() || member.id == botOwner
    }

    fun memberIsBotOwner(member: Member): Boolean {
        return member.id == botOwner
    }

    fun containBotName(message: String): Boolean {
        BOT_NAME.forEach { name ->
            if (message.contains(name, true)) {
                return true
            }
        }
        return false
    }

    fun logi(message: String) {
        logger.info { message }
    }

    fun loge(message: String) {
        logger.error { message }
    }
}