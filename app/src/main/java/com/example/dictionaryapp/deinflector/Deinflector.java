package com.example.dictionaryapp.deinflector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class Deinflector {

    private JSONArray reasons;

    public Deinflector(JSONObject reasons) throws JSONException {
        this.reasons = this.normalizeReasons(reasons);
    }

    public JSONArray deinflect(String source) throws JSONException {
        JSONObject temp = new JSONObject();
        temp.put("source", source);
        temp.put("term", source);
        temp.put("rules", 0);
        temp.put("definitions", new JSONArray());
        temp.put("reasons", new JSONArray());

        JSONArray results = new JSONArray();
        results.put(temp);


        for (int i = 0; i < results.length(); ++i){

            int rules = results.getJSONObject(i).getInt("rules");
            String term = results.getJSONObject(i).getString("term");
            JSONArray reasons = results.getJSONObject(i).getJSONArray("reasons");

            for (int j = 0; j < this.reasons.length(); j++){
                JSONArray currReasonInfo = this.reasons.getJSONArray(j);
                String reason = currReasonInfo.getString(0);
                JSONArray variants = currReasonInfo.getJSONArray(1);

                for (int h = 0; h < variants.length(); h++ ){
                    JSONArray currVariant = variants.getJSONArray(h);
                    String kanaIn = currVariant.getString(0);
                    String kanaOut = currVariant.getString(1);
                    int rulesIn = currVariant.getInt(2);
                    int rulesOut = currVariant.getInt(3);

                    if (
                            (rules != 0 && (rules & rulesIn) == 0) ||
                                    !term.endsWith(kanaIn) ||
                                    (term.length() - kanaIn.length() + kanaOut.length()) <= 0
                    ){
                        continue;
                    }
                    JSONObject infoIGot = new JSONObject();
                    infoIGot.put("source", source);
                    infoIGot.put("term", term.substring(0, term.length() - kanaIn.length()) + kanaOut);
                    infoIGot.put("rules", rulesOut);
                    infoIGot.put("definitions", new JSONArray());
                    JSONArray fullReasons = new JSONArray();
                    fullReasons.put(reason);
                    for (int r = 0; r < reasons.length(); r++){
                        fullReasons.put(reasons.getString(r));
                    }
                    infoIGot.put("reasons", fullReasons);
                    results.put(infoIGot);

                }
            }

        }

        return results;


    }


    public static JSONArray normalizeReasons(JSONObject reasons) throws JSONException {
        JSONArray normalizedReasons = new JSONArray();
        for (Iterator<String> it = reasons.keys(); it.hasNext(); ) {
            String mainKey = it.next();
            JSONArray reasonInfo = reasons.getJSONArray(mainKey);
            JSONArray variants = new JSONArray();

            for (int x = 0; x < reasonInfo.length(); x ++){
                JSONObject currInfo = reasonInfo.getJSONObject(x);
                JSONArray temp = new JSONArray();
                temp.put(currInfo.get("kanaIn"));
                temp.put(currInfo.get("kanaOut"));
                temp.put(Deinflector.rulesToRuleFlags((JSONArray) currInfo.get("rulesIn")));
                temp.put(Deinflector.rulesToRuleFlags((JSONArray) currInfo.get("rulesOut")));

                variants.put(temp);
            }
            JSONArray tempReasonVariants = new JSONArray();
            tempReasonVariants.put(mainKey);
            tempReasonVariants.put(variants);

            normalizedReasons.put(tempReasonVariants);

        }
        ;

        return normalizedReasons;
    }

    public static int rulesToRuleFlags(JSONArray rules) throws JSONException {
        JSONObject ruleTypes = new JSONObject();
        ruleTypes.put("v1", 0b0000001);
        ruleTypes.put("v5", 0b0000010);
        ruleTypes.put("vs", 0b0000100);
        ruleTypes.put("vk", 0b0001000);
        ruleTypes.put("adj-i", 0b0010000);
        ruleTypes.put("iru", 0b0100000);

        int value = 0;
        for (int k = 0; k < rules.length(); k++){
            int ruleBits;
            try {
                ruleBits = ruleTypes.getInt(rules.getString(k));
            }
            catch (JSONException e){
                continue;
            }
            value |= ruleBits;
        }
        return value;
    }


}