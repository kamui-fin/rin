package com.kamui.rin;

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.kamui.rin.database.DBHelper
import com.kamui.rin.database.DictEntry
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import java.text.DecimalFormat

class DictEntryAdapter(private val mContext: Context, data: List<DictEntry>) : RecyclerView.Adapter<DictEntryAdapter.ViewHolder>() {
    private val mData: List<DictEntry> = data
    private var lastPosition = -1

    // inflates the row layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.layout_listentry, parent, false)
        return ViewHolder(view)
    }

    // binds the data to the TextView in each row
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry: DictEntry = mData[position]
        holder.titleView.text = entry.kanji
        holder.secondaryView.text = entry.reading
        var meaning: String = entry.getMeaning()
        if (meaning.length >= 100) {
            meaning = meaning.substring(0, 100) + "..."
        }
        holder.detailView.text = meaning
        holder.dictName.text = entry.shortenedDictName
        setAnimation(holder.itemView, position)
        holder.card.setOnClickListener { v: View? ->
            val intent = Intent(mContext, WordDetailActivity::class.java)
            intent.putExtra("word", entry.kanji)
            intent.putExtra("reading", entry.reading)
            intent.putExtra("meaning", entry.getMeaning())
            intent.putExtra("pitch", entry.pitchAccent)
            val frequency: Int = entry.freq
            val formatter = DecimalFormat("#,###")
            intent.putExtra("freq", "Freq: " + formatter.format(frequency))
            var splittedTags = JSONArray()
            try {
                splittedTags = DBHelper.getSplittedTags(entry, mContext)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            intent.putExtra("tags", splittedTags.toString())
            mContext.startActivity(intent)
        }
    }

    private fun setAnimation(viewToAnimate: View, position: Int) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(mContext, android.R.anim.fade_in)
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }

    // total number of rows
    override fun getItemCount(): Int {
        return mData.size
    }

    // stores and recycles views as they are scrolled off screen
    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var titleView: TextView = itemView.findViewById(R.id.wordTextView)
        var secondaryView: TextView = itemView.findViewById(R.id.secondaryTextCard)
        var detailView: TextView = itemView.findViewById(R.id.meaningTextView)
        var dictName: TextView = itemView.findViewById(R.id.dictName)
        var card: CardView = itemView.findViewById(R.id.card)
        override fun onClick(v: View) {}

    }

    // convenience method for getting data at click position
    fun getItem(id: Int): DictEntry {
        return mData[id]
    }
}