package com.example.escaperoom.ui.main

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.escaperoom.R
import com.example.escaperoom.data.model.Line

/** Renders one line of dialogue per row. */
class TranscriptAdapter(
    private var items: List<Line>
) : RecyclerView.Adapter<TranscriptAdapter.LineViewHolder>() {

    class LineViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val speaker: TextView = view.findViewById(R.id.tvSpeaker)
        private val text: TextView = view.findViewById(R.id.tvText)

        fun bind(line: Line) {
            // The Enigma speaks from a card; the player's lines sit flat on the canvas.
            val isEnigma = line.speaker == "enigma"
            val ctx = itemView.context
            itemView.setBackgroundResource(if (isEnigma) R.drawable.bg_card else 0)

            speaker.text = if (line.hadPhoto) "${line.speaker} · photo" else line.speaker
            speaker.setTextColor(
                ContextCompat.getColor(ctx, if (isEnigma) R.color.primary else R.color.muted)
            )
            text.setTextColor(
                ContextCompat.getColor(ctx, if (isEnigma) R.color.ink else R.color.body)
            )
            text.text = line.text
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_line, parent, false)
        return LineViewHolder(view)
    }

    override fun onBindViewHolder(holder: LineViewHolder, position: Int) =
        holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: List<Line>) {
        items = newItems
        notifyDataSetChanged()
    }
}
