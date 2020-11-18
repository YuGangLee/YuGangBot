package co.yugang.bot.plugins.intf

import net.mamoe.mirai.message.GroupMessageEvent

interface IPlugin {
    val name: String

    var id: String

    suspend fun onMessage(message: GroupMessageEvent)
}