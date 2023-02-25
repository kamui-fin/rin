package com.kamui.rin.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler.Callback
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kamui.rin.DictEntryAdapter
import com.kamui.rin.R
import com.kamui.rin.databinding.FragmentSavedWordsBinding
import com.kamui.rin.databinding.LayoutListentryBinding
import com.kamui.rin.databinding.SavedWordsItemBinding
import kotlinx.coroutines.NonDisposableHandle.parent

val items = mutableListOf<String>("その", "後", "いつ", "定期", "購入", "商品", "未開封", "連絡", "電話", "メール")

class SavedWords : Fragment() {
    private var _binding: FragmentSavedWordsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSavedWordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    inner class ViewHolder(val binding: SavedWordsItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.wordListRecycler.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        binding.wordListRecycler.layoutManager = LinearLayoutManager(context)
        binding.wordListRecycler.adapter = object : RecyclerView.Adapter<SavedWords.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedWords.ViewHolder {
                val binding = SavedWordsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return ViewHolder(binding)
            }

            override fun getItemCount(): Int {
                return items.size
            }

            override fun onBindViewHolder(holder: SavedWords.ViewHolder, position: Int) {
                holder.binding.word.text = items[position]
//                val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//                val clip: ClipData = ClipData.newPlainText("Word", items[position])
//                clipboard.setPrimaryClip(clip)
//                Toast.makeText(context, "Copied ${items[position]} to clipboard", Toast.LENGTH_SHORT).show()
            }
        }
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return makeMovementFlags(0, ItemTouchHelper.RIGHT)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.absoluteAdapterPosition
                items.removeAt(pos)
                (binding.wordListRecycler.adapter as RecyclerView.Adapter).notifyItemRemoved(pos)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.wordListRecycler)
    }
}