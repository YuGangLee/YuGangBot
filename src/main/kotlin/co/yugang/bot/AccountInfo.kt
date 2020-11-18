package co.yugang.bot

import co.yugang.bot.utils.BotHelper
import java.io.File

data class AccountInfo(val qqNumber: Long, val psw: String) {
    companion object {
        private const val DEFAULT_ACCOUNT_FILE_PATH = "res/account.txt"

        val DEFAULT = AccountInfo(0L, "")

        fun fromFile(): AccountInfo? = fromFile(DEFAULT_ACCOUNT_FILE_PATH)

        fun fromFile(path: String): AccountInfo? {
            val accountFile = File(path)
            return try {
                if (accountFile.exists()) {
                    val info = accountFile.readLines()
                    AccountInfo(info[0].trim().toLong(), info[1].trim())
                } else {
                    BotHelper.loge("读取账号数据失败，请检查文件格式是否正确")
                    null
                }
            } catch (e: Exception) {
                BotHelper.loge("读取账号数据失败，请检查文件格式是否正确")
                null
            }
        }
    }
}