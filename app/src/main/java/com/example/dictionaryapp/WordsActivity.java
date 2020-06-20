package com.example.dictionaryapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

public class WordsActivity extends AppCompatActivity {
    private static final String TAG = "WordsActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_words);

        BottomNavigationView btmNavView = findViewById(R.id.bottom_navigation);

        btmNavView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.search_page:
                        Intent intent_search = new Intent(WordsActivity.this, MainActivity.class);
                        System.out.println("Going to word search page");
                        startActivity(intent_search);
                        return true;
                    case R.id.setting_page:
                        Intent intent_settings = new Intent(WordsActivity.this, SettingsActivity.class);
                        System.out.println("Going to word settings page");

                        startActivity(intent_settings);
                        return true;
                }
                return true;
            }
        });


        TabLayout tabs = findViewById(R.id.tabs);
        ViewPager viewPager = findViewById(R.id.viewPager);
        TabPageAdapter adapter = new TabPageAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPager.setAdapter(adapter);
        tabs.setupWithViewPager(viewPager);


    }

}
