package co.yugang.bot

import co.yugang.bot.plugins.EroPicture
import co.yugang.bot.plugins.Repeater
import co.yugang.bot.plugins.intf.IPlugin
import co.yugang.bot.utils.BotHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.join
import net.mamoe.mirai.message.GroupMessageEvent

val plugins = mutableListOf<IPlugin>()

suspend fun main() {

    val account = AccountInfo.fromFile() ?: return
    val bot = Bot(account.qqNumber, account.psw) {
        fileBasedDeviceInfo()
    }.alsoLogin()

    BotHelper.loadConfig()
    plugins.add(EroPicture())
    plugins.add(Repeater())

    bot.subscribeAlways<GroupMessageEvent> { message ->
        plugins.forEach { plugin ->
            plugin.onMessage(message)
        }
    }

    GlobalScope.launch {
        while (bot.isActive) {
            BotHelper.logi("1-min heart beat event.")
            delay(60 * 1000L)
        }
    }

    GlobalScope.launch {
        while (bot.isActive) {
            BotHelper.logi("1-hour heart beat event.")
            delay(60 * 60 * 1000L)
        }
    }

    bot.join()
}