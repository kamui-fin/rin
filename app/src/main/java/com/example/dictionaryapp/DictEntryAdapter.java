package com.example.dictionaryapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dictionaryapp.database.DBHelper;
import com.example.dictionaryapp.database.DictEntry;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

public class DictEntryAdapter extends RecyclerView.Adapter<DictEntryAdapter.ViewHolder> {

    private List<DictEntry> mData;
    private Context mContext;
    private int lastPosition = -1;


    // data is passed into the constructor
    public DictEntryAdapter(Context context, List<DictEntry> data) {
        this.mContext = context;
        this.mData = data;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_listentry, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final DictEntry entry = mData.get(position);
        holder.titleView.setText(entry.getKanji());

        holder.secondaryView.setText(entry.getReading());
        String meaning = entry.getMeaning();
        if (meaning.length() >= 100) {
            meaning = meaning.substring(0, 100) + "...";
        }
        holder.detailView.setText(meaning);
        holder.dictName.setText(entry.getShortenedDictName());

        setAnimation(holder.itemView, position);

        holder.card.setOnClickListener(v -> {
            System.out.println("HI");
            Intent intent = new Intent(mContext, WordDetailActivity.class);
            intent.putExtra("word", entry.getKanji());
            intent.putExtra("reading", entry.getReading());
            intent.putExtra("meaning", entry.getMeaning());
            intent.putExtra("pitch", entry.getPitchAccent());
            Integer frequency = entry.getFreq();
            if (frequency != null) {
                DecimalFormat formatter = new DecimalFormat("#,###");
                intent.putExtra("freq", "Freq: " + formatter.format(entry.getFreq()));
            } else {
                intent.putExtra("freq", "");
            }

            JSONArray splittedTags = new JSONArray();
            try {
                splittedTags = DBHelper.getSplittedTags(entry, mContext);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            intent.putExtra("tags", splittedTags.toString());

            mContext.startActivity(intent);
        });


    }

    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(mContext, android.R.anim.fade_in);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView titleView;
        TextView secondaryView;
        TextView detailView;
        TextView dictName;
        CardView card;

        ViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            titleView = itemView.findViewById(R.id.wordTextView);
            secondaryView = itemView.findViewById(R.id.secondaryTextCard);
            detailView = itemView.findViewById(R.id.meaningTextView);
            dictName = itemView.findViewById(R.id.dictName);
        }

        @Override
        public void onClick(View v) {

        }
    }

    // convenience method for getting data at click position
    DictEntry getItem(int id) {
        return mData.get(id);
    }

}