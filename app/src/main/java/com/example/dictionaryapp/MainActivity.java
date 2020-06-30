package com.example.dictionaryapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dictionaryapp.database.DBHelper;
import com.example.dictionaryapp.database.DictEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    DBHelper helper;
    DictEntryAdapter adapter;
    ProgressBar pbar;
    RecyclerView recyclerView;
    List<DictEntry> results;
    SharedPreferences sharedPreferences;
    ImageView img;
    TextView support;
    TextView notFoundView;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


        pbar = findViewById(R.id.pBar);
        pbar.setVisibility(View.GONE);

        support = findViewById(R.id.glassText);
        img = findViewById(R.id.glass);

        notFoundView = findViewById(R.id.noResultsFound);


        Toolbar toolbar = findViewById(R.id.searchToolbar);
        toolbar.bringToFront();
        setSupportActionBar(toolbar);


        try {
            helper = new DBHelper(this, getDisabledDicts(), shouldDeconj(), bilingualFirst(), readDeinflectJsonFile());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        BottomNavigationView btmNavView = findViewById(R.id.bottom_navigation);
        btmNavView.setSelectedItemId(R.id.search_page);

        btmNavView.setOnNavigationItemSelectedListener(menuItem -> {
            switch (menuItem.getItemId()) {
//                    case R.id.word_page:
//                        Intent intent_word = new Intent(MainActivity.this, WordsActivity.class);
//                        startActivity(intent_word);
//                        return true;
                case R.id.setting_page:
                    Intent intent_settings = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent_settings);
                    return true;
            }
            return true;
        });




    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar, menu);

        final MenuItem myActionMenuItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) myActionMenuItem.getActionView();


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public boolean onQueryTextSubmit(String query) {
                // do stuff here
                AsyncTask<String, Void, List<DictEntry>> task = new AsyncLookup();
                task.execute(query);

                if (!searchView.isIconified()) {
                    searchView.setIconified(true);
                }
                myActionMenuItem.collapseActionView();

                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (adapter != null && s.length() == 1) {
                    results.clear();
                    adapter.notifyDataSetChanged();
                }

                img.setVisibility(View.INVISIBLE);
                support.setVisibility(View.INVISIBLE);
                notFoundView.setText("");
                return false;
            }
        });

        Intent intent = getIntent();
        String value = handleIntent(intent);
        if (value != null) {
            // TODO: Add the back button
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            myActionMenuItem.expandActionView();
            searchView.setQuery(value, true);

        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public List<String> getDisabledDicts() {
        boolean jmdictEnabled = sharedPreferences.getBoolean("jmdictEnable", true);
        boolean kenkyuuEnable = sharedPreferences.getBoolean("kenkyuuEnable", true);
        boolean shinmeiEnable = sharedPreferences.getBoolean("shinmeiEnable", true);
        boolean daijirinEnable = sharedPreferences.getBoolean("daijirinEnable", true);
        boolean meikyoEnable = sharedPreferences.getBoolean("meikyoEnable", true);

        List<String> disabledDicts = new ArrayList<>();

        if (!jmdictEnabled)
            disabledDicts.add("JMdict (English)");
        if (!kenkyuuEnable)
            disabledDicts.add("研究社　新和英大辞典　第５版");
        if (!shinmeiEnable)
            disabledDicts.add("新明解国語辞典 第五版");
        if (!daijirinEnable)
            disabledDicts.add("三省堂　スーパー大辞林");
        if (!meikyoEnable)
            disabledDicts.add("明鏡国語辞典");

        return disabledDicts;

    }

    public boolean bilingualFirst() {
        return sharedPreferences.getBoolean("showBilingualFirst", false);
    }

    public boolean shouldDeconj() {
        return sharedPreferences.getBoolean("deconjSettei", true);
    }

    private String handleIntent(Intent intent) {
        if (Intent.ACTION_PROCESS_TEXT.equals(intent.getAction())) {
            CharSequence keyword = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
            return keyword.toString();
        }

        else {
            return null;
        }
    }

    public JSONObject readDeinflectJsonFile() throws JSONException {
        String tContents = "";

        try {
            InputStream stream = getAssets().open("deinflect.json");

            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            tContents = new String(buffer);
        } catch (IOException e) {
            // Handle exceptions here
        }

        return new JSONObject(tContents);
    }

    private class AsyncLookup extends AsyncTask<String, Void, List<DictEntry>> {
        ProgressBar pbar;

        @Override
        protected void onPreExecute() {
            pbar = findViewById(R.id.pBar);
            pbar.bringToFront();
            pbar.setVisibility(View.VISIBLE);
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected List<DictEntry> doInBackground(String... query) {

            try {
                results = helper.lookup(query[0]);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return results;
        }

        @Override
        protected void onPostExecute(List<DictEntry> aVoid) {
            pbar.setVisibility(View.INVISIBLE);

            if (results.isEmpty()){
                notFoundView.setText("Not found! Try refining your search.");
            }

            recyclerView = findViewById(R.id.resultRecyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));


            adapter = new DictEntryAdapter(MainActivity.this, results);
            recyclerView.setAdapter(adapter);


        }
    }

}
