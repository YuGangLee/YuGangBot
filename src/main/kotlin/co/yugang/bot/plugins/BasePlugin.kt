package co.yugang.bot.plugins

import co.yugang.bot.plugins.intf.IPlugin
import java.util.*

abstract class BasePlugin(private val pluginName: String) : IPlugin {
    private var realId: String = UUID.randomUUID().toString()

    override val name: String
        get() = pluginName

    override var id: String
        get() = realId
        set(value) {
            realId = value
        }
}