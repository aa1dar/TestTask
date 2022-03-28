package com.example.testtask

import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class RecyclerViewAdapter() : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    lateinit var dataSet: MutableList<File>
    lateinit var onItemClick: OnItemClick
    val mmr: MediaMetadataRetriever = MediaMetadataRetriever()


    interface OnItemClick {
        fun setOnPlayListener(position: Int, file: File)
        fun setOnPauseListener(position: Int, file: File)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val textView: TextView
        val dateTime: TextView
        var durationTime: TextView
        val playButton: Button
        val pauseButton: Button
        var progressBar: ProgressBar

        init {
            textView = view.findViewById(R.id.textView)
            dateTime = view.findViewById(R.id.textDate)
            durationTime = view.findViewById(R.id.durationText)
            playButton = view.findViewById(R.id.playButton)
            pauseButton = view.findViewById(R.id.pauseButton)
            progressBar = view.findViewById(R.id.progressBar)
            progressBar.progress = 0

            playButton.setOnClickListener(this)
            pauseButton.setOnClickListener(this)


        }

        override fun onClick(view: View?) {

            when (view?.id) {
                R.id.playButton -> {
                    onItemClick.setOnPlayListener(adapterPosition, dataSet[adapterPosition])
                }
                R.id.pauseButton -> {
                    onItemClick.setOnPauseListener(adapterPosition, dataSet[adapterPosition])
                }
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.rec_view_item, parent, false)

        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mills = getDurationFromPath(dataSet[position].absolutePath)
        val durationTime = timeToString(mills / 1000)
        holder.durationTime.text = durationTime
        holder.textView.text = dataSet[position].nameWithoutExtension
        val sdf = SimpleDateFormat("dd.MM.yyyy в hh:mm")
        holder.dateTime.text = sdf.format(dataSet[position].lastModified())
        holder.progressBar.max = mills
        holder.progressBar.progress = 0
        holder.playButton.visibility = View.VISIBLE
        holder.pauseButton.visibility = View.GONE
    }

    override fun getItemCount(): Int = dataSet.size

    private fun timeToString(seconds: Int): String {
        val time = Date()
        time.minutes = 0
        time.hours = 0
        time.seconds = seconds
        val sdf = SimpleDateFormat("m:ss")
        return sdf.format(time).toString()
    }

    private fun getDurationFromPath(absPath: String): Int {
        mmr.setDataSource(absPath)
        val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        return durationStr!!.toInt();
    }
}