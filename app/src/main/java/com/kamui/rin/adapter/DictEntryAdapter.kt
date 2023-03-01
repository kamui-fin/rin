package com.kamui.rin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.kamui.rin.databinding.LayoutListentryBinding
import com.kamui.rin.db.model.DictEntry
import com.kamui.rin.ui.fragment.LookupFragmentDirections

class DictEntryAdapter(private val context: Context, private val entries: List<DictEntry>) :
    RecyclerView.Adapter<DictEntryAdapter.ViewHolder>() {
    private var lastPosition = -1

    override fun getItemCount(): Int {
        return entries.size
    }

    private fun trimDefinition(meaning: String): String {
        if (meaning.length >= 100) {
            return meaning.substring(0, 100) + "..."
        }
        return meaning
    }

    private fun setAnimation(viewToAnimate: View, position: Int) {
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LayoutListentryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]

        holder.binding.wordTextView.text = entry.kanji
        holder.binding.secondaryTextCard.text = entry.reading
        holder.binding.meaningTextView.text = trimDefinition(entry.formattedMeaning)
        holder.binding.dictName.text = entry.shortenedDictName
        holder.binding.card.setOnClickListener {
            val action = LookupFragmentDirections.getDetails(entry.id, entry.kanji)
            it.findNavController().navigate(action)
        }
        setAnimation(holder.itemView, position)
    }

    inner class ViewHolder(val binding: LayoutListentryBinding) : RecyclerView.ViewHolder(binding.root)
}