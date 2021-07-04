package com.kamui.rin.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class ReasonInfo<T>(
    val kanaIn: String,
    val kanaOut: String,
    val rulesIn: T,
    val rulesOut: T
)

@Serializable
data class ReasonEntry<T>(
    val reason: String,
    val information: List<ReasonInfo<T>>
)

data class Deinflection(
    val term: String,
    val rules: Int,
    val reasons: List<ReasonEntry<Int>>
)

class Deinflector(private val deinflectionText: String) {
    private val reasons = normalizeReasons()

    fun deinflect(source: String): MutableList<Deinflection> {
        val results = mutableListOf(Deinflection(source, 0, listOf()))
        for (result in results) {
            for (reason in this.reasons) {
                for (variant in reason.information) {
                    if (
                        (result.rules != 0 && (result.rules and variant.rulesIn) == 0) ||
                        !result.term.endsWith(variant.kanaIn) ||
                        (result.term.length - variant.kanaIn.length + variant.kanaOut.length) <= 0
                    ) {
                        continue
                    }

                    results.add(
                        Deinflection(
                            result.term.substring(
                                0,
                                result.term.length - variant.kanaIn.length
                            ) + variant.kanaOut, variant.rulesOut, result.reasons + reason
                        )
                    )
                }
            }
        }
        return results
    }

    private fun normalizeReasons(): List<ReasonEntry<Int>> {
        val entries = Json.decodeFromString<List<ReasonEntry<List<String>>>>(deinflectionText)
        val normalized: MutableList<ReasonEntry<Int>> = ArrayList()
        for (reason in entries) {
            val variants: List<ReasonInfo<Int>> = reason.information.map { i ->
                ReasonInfo(
                    i.kanaIn,
                    i.kanaOut,
                    rulesToRuleFlags(i.rulesIn),
                    rulesToRuleFlags(i.rulesOut)
                )
            }
            normalized.add(ReasonEntry(reason.reason, variants))
        }
        return normalized
    }

    private fun rulesToRuleFlags(rules: List<String>): Int {
        val ruleTypes = mapOf(
            "v1" to 0b00000001,
            "v5" to 0b00000010,
            "vs" to 0b00000100,
            "vk" to 0b00001000,
            "adj-i" to 0b00010000,
            "iru" to 0b00100000
        )
        var value = 0
        for (rule in rules) {
            val bits = ruleTypes[rule] ?: continue
            value = value or bits
        }
        return value
    }
}