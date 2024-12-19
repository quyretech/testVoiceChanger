package com.remotetechs.testvoicechanger

import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.ln


class EffectsAdapter(
    private val effects: List<String>, // List of effects like "Default", "Boy", "Girl", etc.
    private var mediaPlayer: MediaPlayer?, // Pass mediaPlayer to adjust volume
    private val onEffectSelected: (String) -> Unit // Callback to handle effect selection
) : RecyclerView.Adapter<EffectsAdapter.EffectViewHolder>() {

    private var expandedPosition: Int = 0

    inner class EffectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEffectName: TextView = itemView.findViewById(R.id.tv_effect_name)
        val seekBarContainer: LinearLayout = itemView.findViewById(R.id.seekBarContainer)
        val seekBarVolume: SeekBar = itemView.findViewById(R.id.seekBarVolume)
        val seekBarEffect: SeekBar = itemView.findViewById(R.id.seekBarEffect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EffectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_effects, parent, false)
        return EffectViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: EffectViewHolder, position: Int) {
        val effect = effects[position] // Lấy 1 item duy nhất tại vị trí position
        holder.tvEffectName.text = effect

        // Log chỉ mục và tên effect
        Log.d("effect position", "Position: $position, Effect: $effect")

        // Show/hide SeekBars based on whether the item is expanded
        holder.seekBarContainer.visibility = if (expandedPosition == position) View.VISIBLE else View.GONE

        holder.tvEffectName.setOnClickListener {
            val previousExpandedPosition = expandedPosition
            expandedPosition = if (expandedPosition == position) -1 else position

            // Update only the previous and current items
            notifyItemChanged(previousExpandedPosition)
            notifyItemChanged(position)

            onEffectSelected(effect) // Callback to handle effect selection in VoiceActivity
        }

        holder.seekBarVolume.max = 100  // Set the SeekBar's maximum value
        holder.seekBarVolume.progress = 50  // Default SeekBar progress (middle/normal)
        holder.seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                Log.d("quyquyquy","$mediaPlayer")
                Log.d("progress","$progress")
                val volume = progress / 100f
                Log.d("volume","$volume")
                mediaPlayer?.setVolume(volume, volume)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        // Handle effect seekbar changes (if any specific effect intensity adjustment is needed)
        holder.seekBarEffect.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Handle effect adjustments if needed
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun getItemCount(): Int = effects.size
}
