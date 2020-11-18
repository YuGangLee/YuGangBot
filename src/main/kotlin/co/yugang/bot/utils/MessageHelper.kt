package co.yugang.bot.utils

import net.mamoe.mirai.message.code.CodableMessage
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.isContentEmpty
import net.mamoe.mirai.message.data.isPlain

fun MessageChain.parseString(): String? {
    var buffer = ""
    for (msg in this) {
        buffer += if (msg.isContentEmpty()) continue
        else if (msg.isPlain()) msg.content
        else if (msg is CodableMessage) msg.toMiraiCode()
        else return null
    }
    if (buffer.isBlank()) {
        return null
    }
    return buffer
}

fun toHex(byteArray: ByteArray): String {
    val result = with(StringBuilder()) {
        byteArray.forEach {
            val hex = it.toInt() and (0xFF)
            val hexStr = Integer.toHexString(hex)
            if (hexStr.length == 1) {
                this.append("0").append(hexStr)
            } else {
                this.append(hexStr)
            }
        }
        this.toString()
    }
    //转成16进制后是32字节
    return result
}