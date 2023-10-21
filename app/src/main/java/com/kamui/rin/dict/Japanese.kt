package com.kamui.rin.dict

fun toHiragana(c: Char): Char {
    if (isFullWidthKatakana(c)) {
        return (c.code - 0x60).toChar()
    } else if (isHalfWidthKatakana(c)) {
        return (c.code - 0xcf25).toChar()
    }
    return c
}

fun isHalfWidthKatakana(c: Char): Boolean {
    return c in '\uff66'..'\uff9d'
}

fun isFullWidthKatakana(c: Char): Boolean {
    return c in '\u30a1'..'\u30fe'
}

fun isHiragana(c: Char): Boolean {
    return c in '\u3041'..'\u309e'
}

fun allHiragana(word: String): Boolean {
    for (x in word.toCharArray()) {
        if (!isHiragana(x)) {
            return false
        }
    }
    return true
}

fun isAllKana(word: String): Boolean {
    for (element in word) {
        if (!(element in 'ぁ'..'ゞ' || element in 'ァ'..'ヾ')) {
            return false
        }
    }
    return true
}

fun katakanaToHiragana(katakanaWord: String): String {
    return katakanaWord.map { c -> toHiragana(c) }.toString()
}
