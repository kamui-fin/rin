package com.example.dictionaryapp.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.dictionaryapp.deinflector.Deinflector;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;



public class DictHelper extends SQLiteOpenHelper {

    private static final String DICTIONARY = "DICTIONARY";
    private static final String KANJI = "KANJI";
    private static final String READING = "READING";
    private static final String TAGS = "TAGS";
    private static final String MEANING = "MEANING";
    private static final String ID = "ID";
    public static final String DICTNAME = "DICTNAME";
    Deinflector deinflector;
    private JSONObject inflectorJSON;
    private static final String TAG = "DictHelper";
    List<String> disabledDicts;
    boolean shouldDeconj;
    boolean bilingualFirst;
    Context context;
    private boolean createdb = false;


    public DictHelper(@Nullable Context context, List<String> disabledDicts, boolean shouldDeconj, boolean bilingualFirst, JSONObject inflectorJson) {

        super(context, "dict.db", null,1);
        this.disabledDicts = disabledDicts;
        this.shouldDeconj = shouldDeconj;
        this.bilingualFirst = bilingualFirst;

        inflectorJSON = inflectorJson;
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void clearAllEntriesByDict(String dict){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from " + DICTIONARY + " where " + DICTNAME + " = " + dict);
        db.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public List<DictEntry> searchWord(String query) throws JSONException {
        List<String> possible = normalizeWord(query);
        List<DictEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String tupleDisabled = String.join(",", this.disabledDicts);
        Cursor c = null;

        for (String poss: possible){
            String columnToSelect = KANJI;
            if (isAllKana(poss)){
                columnToSelect  = READING;
            }
            String selectQuery = "SELECT * FROM DICTIONARY " +
                    "WHERE TRIM(" + columnToSelect + ") = '" + poss + "' and DICTNAME NOT IN (" + tupleDisabled +") ORDER BY ORDERDICT";
            c = db.rawQuery(selectQuery, null);
            if (c.moveToFirst()){
                do{
                    String kanji = c.getString(1);
                    String reading = c.getString(2);
                    String tags = c.getString(3);
                    String meaning = c.getString(4);
                    String dictName = c.getString(5);
                    Integer dictOrder = c.getInt(6);
                    System.out.println(dictOrder);
                    DictEntry entry = new DictEntry(kanji, reading, tags, meaning, dictName, dictOrder);
                    entries.add(entry);
                }
                while (c.moveToNext());
            }
        }

        c.close();
        db.close();
        if (this.bilingualFirst){
            System.out.println("sorting reverse");
            Collections.sort(entries, Collections.reverseOrder());

        }
        else {
            Collections.sort(entries);

        }
        return entries;
    }

    public List<String> normalizeWord(String word) throws JSONException {
        if (this.shouldDeconj){
            return deconjugateWord(word.trim());
        }
        else {
            List<String> res = new ArrayList<>();
            res.add(word);
            return res;
        }
    }

    public List<String> deconjugateWord(String word) throws JSONException {
        List<String> results = new ArrayList<>();
         deinflector = new Deinflector(inflectorJSON);
        JSONArray res = deinflector.deinflect(word);
        for (int x = 0; x < res.length(); x ++){
             results.add(res.getJSONObject(x).getString("term"));
        }
        return results;
    }

    public boolean isAllKana(String word){

        for (int x = 0; x < word.length(); x++){
            char c = word.charAt(x);
            if ((c > '\u3040' && c < '\u309F') || (c > '\u30A0' && c < '\u30FF')){
                continue;
            }
            else {
                return false;
            }
        }
        return true;
    }
}
