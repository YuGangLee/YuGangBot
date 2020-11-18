package co.yugang.bot.utils

import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

object ClassTypeHelper {
    inline fun <reified T> typeOf(): Type = object : TypeToken<T>() {}.type
}