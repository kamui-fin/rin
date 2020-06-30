package com.example.dictionaryapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    BottomNavigationView btmNavView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        btmNavView = findViewById(R.id.bottom_navigation);
        btmNavView.setSelectedItemId(R.id.setting_page);
        btmNavView.setOnNavigationItemSelectedListener(menuItem -> {
            switch (menuItem.getItemId()) {
//                    case R.id.word_page:
//                        Intent intent_word = new Intent(SettingsActivity.this, WordsActivity.class);
//                        startActivity(intent_word);
//                        return true;
                case R.id.search_page:
                    Intent intent_search = new Intent(SettingsActivity.this, MainActivity.class);
                    startActivity(intent_search);
                    return true;
            }
            return true;
        });

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();

    }

}
