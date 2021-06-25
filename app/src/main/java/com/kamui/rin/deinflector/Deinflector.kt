package com.kamui.rin.deinflector

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class Deinflector(reasons: JSONObject?) {
    private val reasons: JSONArray

    @Throws(JSONException::class)
    fun deinflect(source: String?): JSONArray {
        val temp = JSONObject()
        temp.put("source", source)
        temp.put("term", source)
        temp.put("rules", 0)
        temp.put("definitions", JSONArray())
        temp.put("reasons", JSONArray())
        val results = JSONArray()
        results.put(temp)
        for (i in 0 until results.length()) {
            val rules = results.getJSONObject(i).getInt("rules")
            val term = results.getJSONObject(i).getString("term")
            val reasons = results.getJSONObject(i).getJSONArray("reasons")
            for (j in 0 until this.reasons.length()) {
                val currReasonInfo = this.reasons.getJSONArray(j)
                val reason = currReasonInfo.getString(0)
                val variants = currReasonInfo.getJSONArray(1)
                for (h in 0 until variants.length()) {
                    val currVariant = variants.getJSONArray(h)
                    val kanaIn = currVariant.getString(0)
                    val kanaOut = currVariant.getString(1)
                    val rulesIn = currVariant.getInt(2)
                    val rulesOut = currVariant.getInt(3)
                    if (rules != 0 && rules and rulesIn == 0 ||
                        !term.endsWith(kanaIn) || term.length - kanaIn.length + kanaOut.length <= 0
                    ) {
                        continue
                    }
                    val infoIGot = JSONObject()
                    infoIGot.put("source", source)
                    infoIGot.put("term", term.substring(0, term.length - kanaIn.length) + kanaOut)
                    infoIGot.put("rules", rulesOut)
                    infoIGot.put("definitions", JSONArray())
                    val fullReasons = JSONArray()
                    fullReasons.put(reason)
                    for (r in 0 until reasons.length()) {
                        fullReasons.put(reasons.getString(r))
                    }
                    infoIGot.put("reasons", fullReasons)
                    results.put(infoIGot)
                }
            }
        }
        return results
    }

    companion object {
        @Throws(JSONException::class)
        fun normalizeReasons(reasons: JSONObject): JSONArray {
            val normalizedReasons = JSONArray()
            val it = reasons.keys()
            while (it.hasNext()) {
                val mainKey = it.next()
                val reasonInfo = reasons.getJSONArray(mainKey)
                val variants = JSONArray()
                for (x in 0 until reasonInfo.length()) {
                    val currInfo = reasonInfo.getJSONObject(x)
                    val temp = JSONArray()
                    temp.put(currInfo["kanaIn"])
                    temp.put(currInfo["kanaOut"])
                    temp.put(rulesToRuleFlags(currInfo["rulesIn"] as JSONArray))
                    temp.put(rulesToRuleFlags(currInfo["rulesOut"] as JSONArray))
                    variants.put(temp)
                }
                val tempReasonVariants = JSONArray()
                tempReasonVariants.put(mainKey)
                tempReasonVariants.put(variants)
                normalizedReasons.put(tempReasonVariants)
            }
            return normalizedReasons
        }

        @Throws(JSONException::class)
        fun rulesToRuleFlags(rules: JSONArray): Int {
            val ruleTypes = JSONObject()
            ruleTypes.put("v1", 1)
            ruleTypes.put("v5", 2)
            ruleTypes.put("vs", 4)
            ruleTypes.put("vk", 8)
            ruleTypes.put("adj-i", 16)
            ruleTypes.put("iru", 32)
            var value = 0
            for (k in 0 until rules.length()) {
                var ruleBits: Int
                ruleBits = try {
                    ruleTypes.getInt(rules.getString(k))
                } catch (e: JSONException) {
                    continue
                }
                value = value or ruleBits
            }
            return value
        }
    }

    init {
        this.reasons = reasons?.let { normalizeReasons(it) }!!
    }
}