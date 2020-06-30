package com.example.dictionaryapp.database;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.dictionaryapp.TagsHelper;
import com.example.dictionaryapp.deinflector.Deinflector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DBHelper {

    private AppDatabase db;
    private DictDao dao;
    private List<String> disabledDicts;
    private boolean shouldDeconj;
    private boolean bilingualFirst;
    private JSONObject inflectorJSON;
    Deinflector deinflector;
    private Context mContext;

    public DBHelper(Context context, List<String> disabledDicts, boolean shouldDeconj, boolean bilingualFirst, JSONObject inflectorJson) {
        this.disabledDicts = disabledDicts;
        mContext = context;
        this.shouldDeconj = shouldDeconj;
        this.bilingualFirst = bilingualFirst;
        this.inflectorJSON = inflectorJson;
        db = AppDatabase.buildDatabase(context);
        dao = db.dictDao();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public List<DictEntry> lookup(String query) throws JSONException {
        System.out.println(this.disabledDicts);
        List<String> possible = normalizeWord(query);
        List<DictEntry> entries = new ArrayList<>();
        if (possible.isEmpty()){
            possible.add(query);
        }
        for (String p : possible) {
            List<DictEntry> res;
            if (isAllKana(p)) {
                String convertedToHiragana;
                if (!allHiragana(p)){
                    convertedToHiragana = katakanaToHiragana(p);
                }
                else {
                    convertedToHiragana = p;
                }
                res = dao.searchEntryReading(convertedToHiragana, this.disabledDicts);
                if (res.isEmpty()){
                    res = dao.searchEntryByKanji(p, this.disabledDicts);
                }
            } else {
                System.out.println("Doing kanji search...");
                res = dao.searchEntryByKanji(p, this.disabledDicts);
            }
            System.out.println(res);
            entries.addAll(res);
        }
        if (this.bilingualFirst) {
            Collections.sort(entries, Collections.reverseOrder());
        } else {
            Collections.sort(entries);
        }
        return entries;

    }

    // helper japanese methods
    public boolean isAllKana(String word) {

        for (int x = 0; x < word.length(); x++) {
            char c = word.charAt(x);
            if ((c > '\u3040' && c < '\u309F') || (c > '\u30A0' && c < '\u30FF')) {
                continue;
            } else {
                return false;
            }
        }
        return true;
    }

    public List<String> normalizeWord(String word) throws JSONException {
        if (this.shouldDeconj) {
            return deconjugateWord(word.trim());
        } else {
            List<String> res = new ArrayList<>();
            res.add(word);
            return res;
        }
    }

    public List<String> deconjugateWord(String word) throws JSONException {
        List<String> results = new ArrayList<>();
        deinflector = new Deinflector(inflectorJSON);
        JSONArray res = deinflector.deinflect(word);
        for (int x = 0; x < res.length(); x++) {
            results.add(res.getJSONObject(x).getString("term"));
        }
        return results;
    }

    public String katakanaToHiragana(String katakanaWord){
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < katakanaWord.length(); i++) {
            out.append(toHiragana(katakanaWord.charAt(i)));
        }
        return out.toString();
    }

    public static char toHiragana(char c) {
        if (isFullWidthKatakana(c)) {
            return (char) (c - 0x60);
        } else if (isHalfWidthKatakana(c)) {
            return (char) (c - 0xcf25);
        }
        return c;
    }

    public static boolean isHalfWidthKatakana(char c) {
        return (('\uff66' <= c) && (c <= '\uff9d'));
    }

    public static boolean isFullWidthKatakana(char c) {
        return (('\u30a1' <= c) && (c <= '\u30fe'));
    }

    public static boolean isHiragana(char c) {
        return (('\u3041' <= c) && (c <= '\u309e'));
    }

    public static boolean allHiragana(String word){
        for (char x : word.toCharArray()){
            if (!isHiragana(x)){
                return false;
            }
        }
        return true;
    }

    public static JSONArray getSplittedTags(DictEntry entry, Context mContext) throws IOException, JSONException {
        TagsHelper helper = new TagsHelper(mContext);
        String[] splitted = entry.getTags().split("\\s+");
        JSONArray info = new JSONArray();
        for (String tag : splitted) {
            if (!tag.isEmpty()) {
                JSONArray temp = new JSONArray();
                String[] tagInfo = helper.getFullTag(tag);
                temp.put(tag);
                temp.put(tagInfo[0]);
                temp.put(tagInfo[1]);
                info.put(temp);
            }
        }
        return info;
    }


}
