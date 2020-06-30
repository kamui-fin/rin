package com.example.dictionaryapp;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TagsHelper {

    private JSONArray data;
    private Context mContext;


    public TagsHelper(Context ctx) throws IOException, JSONException {
        mContext = ctx;
        init();
    }

    public void init() throws IOException, JSONException {
        StringBuilder readText = new StringBuilder();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(mContext.getAssets().open("tag_bank_1.json"), StandardCharsets.UTF_8));

        // do reading, usually loop until end of file reading
        String mLine;
        while ((mLine = reader.readLine()) != null) {
            readText.append(mLine);
        }

        data = new JSONArray(readText.toString());

    }

    public String[] getFullTag(String shortened) throws JSONException {
        for (int x = 0; x < data.length(); x++) {
            JSONArray current = data.getJSONArray(x);
            if (current.getString(0).equals(shortened)) {
                return new String[]{current.getString(3), current.getString(5)};
            }
        }
        return new String[]{shortened, "#FFFFFF"};
    }


}
