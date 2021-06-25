package com.kamui.rin

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@RequiresApi(Build.VERSION_CODES.KITKAT)
class TagsHelper(private val mContext: Context) {
    private var data: JSONArray? = null

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @Throws(IOException::class, JSONException::class)
    fun init() {
        val readText = StringBuilder()
        val reader = BufferedReader(
                InputStreamReader(mContext.assets.open("tag_bank_1.json"), StandardCharsets.UTF_8))

        // do reading, usually loop until end of file reading
        var mLine: String?
        while (reader.readLine().also { mLine = it } != null) {
            readText.append(mLine)
        }
        data = JSONArray(readText.toString())
    }

    @Throws(JSONException::class)
    fun getFullTag(shortened: String): Array<String> {
        for (x in 0 until data!!.length()) {
            val current = data!!.getJSONArray(x)
            if (current.getString(0) == shortened) {
                return arrayOf(current.getString(3), current.getString(5))
            }
        }
        return arrayOf(shortened, "#FFFFFF")
    }

    init {
        init()
    }
}