package com.example.dictionaryapp.database;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.dictionaryapp.deinflector.Deinflector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    public DBHelper(Context context, List<String> disabledDicts, boolean shouldDeconj, boolean bilingualFirst, JSONObject inflectorJson) {
        this.disabledDicts = disabledDicts;
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

        for (String p : possible) {
            List<DictEntry> res;
            if (isAllKana(p)) {
                res = dao.searchEntryReading(p, this.disabledDicts);
            } else {
                res = dao.searchEntryByKanji(p, this.disabledDicts);
            }
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


}
