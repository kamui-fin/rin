package com.kamui.rin.deinflector

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

data class ReasonInfo<T>(
    val kanaIn: String,
    val kanaOut: String,
    val rulesIn: T,
    val rulesOut: T
)

data class ReasonEntry<T>(
    val reason: String,
    val information: List<ReasonInfo<T>>
)

class Deinflector(deinflectionText: String) {
    private val reasons = normalizeReasons(deinflectionText)

    fun deinflect(source: String): JSONArray {
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

    private fun normalizeReasons(deinflectionText: String): List<ReasonEntry<Int>> {
        val entries = Json.decodeFromString<List<ReasonEntry<List<String>>>>(deinflectionText)
        val normalized: MutableList<ReasonEntry<Int>> = ArrayList()
        for (reason in entries) {
            val variants: List<ReasonInfo<Int>> = reason.information.map { i ->
                ReasonInfo(i.kanaIn, i.kanaOut, rulesToRuleFlags(i.rulesIn), rulesToRuleFlags(i.rulesOut))
            }
            normalized.add(ReasonEntry(reason.reason, variants))
        }
        return normalized
    }

    private fun rulesToRuleFlags(rules: List<String>): Int {
        val ruleTypes = mapOf(
            "v1"    to 0b00000001,
            "v5"    to 0b00000010,
            "vs"    to 0b00000100,
            "vk"    to 0b00001000,
            "adj-i" to 0b00010000,
            "iru"   to 0b00100000
        )
        var value = 0
        for (rule in rules) {
            val bits = ruleTypes[rule] ?: continue
            value = value or bits
        }
        return value
    }
}