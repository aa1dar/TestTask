package com.example.testtask


import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log

import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.IOException
import java.lang.NullPointerException


import java.text.SimpleDateFormat

import java.util.*


class MainFragment : Fragment(), View.OnClickListener, RecyclerViewAdapter.OnItemClick {

    lateinit var floatingActionButtonRecord: FloatingActionButton
    lateinit var floatingActionButtonStop: FloatingActionButton
    lateinit var mediaRecorder: MediaRecorder
    lateinit var mediaPlayer: MediaPlayer
    lateinit var navController: NavController
    lateinit var myAdapter: RecyclerViewAdapter
    lateinit var recView: RecyclerView;
    lateinit var progressbarHandler: Timer;
    val PERMISSION_CODE = 21
    var isRecording = false;
    var isPlaying = false;
    var currentRecordDestination: String? = null;
    private var playingCardNumber: Int? = null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_main, container, false)
        recView = view.findViewById(R.id.myRecyclerView);

        myAdapter = RecyclerViewAdapter().apply {
            dataSet = mutableListOf()
            dataSet.addAll(getFileList().toMutableList())
            onItemClick = this@MainFragment
        };
        recView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            this.adapter = myAdapter
        }
        return view;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        floatingActionButtonRecord = view.findViewById(R.id.floatingActionButtonRecord)
        floatingActionButtonStop = view.findViewById(R.id.floatingActionButtonStop)

        floatingActionButtonRecord.setOnClickListener(this)
        floatingActionButtonStop.setOnClickListener(this)
        navController = findNavController();

        val navBackStackEntry = navController.getBackStackEntry(R.id.mainFragment)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME
                && navBackStackEntry.savedStateHandle.contains("key")
            ) {
                when (navBackStackEntry.savedStateHandle.get<Int>("key")) {
                    1 -> saveRecordName(navBackStackEntry.savedStateHandle.get<String>("nameOfFile"))
                    0 -> deleteRecord()
                    else -> {
                    }
                }
                navBackStackEntry.savedStateHandle.remove<Int>("key")
                navBackStackEntry.savedStateHandle.remove<String>("nameOfFile")
            }
        }
        navBackStackEntry.lifecycle.addObserver(observer)

        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                navBackStackEntry.lifecycle.removeObserver(observer)
            }
        })


        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        cleanPlayingState()
        super.onDestroyView()
    }


    private fun checkPermission(): Boolean {
        val audioPermission = ActivityCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val storagePermission = ActivityCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (audioPermission && storagePermission) {
            return true
        }
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), PERMISSION_CODE
        )
        return false;
    }

    override fun onClick(it: View?) {
        when (it?.id) {
            R.id.floatingActionButtonRecord -> {
                isRecording = true
                if (checkPermission()) {
                    startRecording()
                    it.visibility = View.GONE
                    floatingActionButtonStop.visibility = View.VISIBLE
                }
            }
            R.id.floatingActionButtonStop -> {
                stopRecording()
                isRecording = false;
                it.visibility = View.GONE
                floatingActionButtonRecord.visibility = View.VISIBLE

                findNavController().navigate(R.id.action_mainFragment_to_saveRecordDialogFragment)
            }
            else -> return
        }
    }


    private fun stopRecording() {
        mediaRecorder.stop()
        mediaRecorder.release()
    }

    private fun startRecording() {
        cleanPlayingState()

        mediaRecorder = MediaRecorder()
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

        val recordPath = activity?.getExternalFilesDir("/")?.absolutePath
        val tempFileName = "record_"


        val sdf = SimpleDateFormat("dd_MM_yyyy_hh_mm_ss")
        val currentDate = sdf.format(Date())
        currentRecordDestination = "$recordPath/$tempFileName$currentDate.3gp"

        mediaRecorder.setOutputFile("$recordPath/$tempFileName$currentDate.3gp")
        mediaRecorder.prepare()
        mediaRecorder.start()

    }

    private fun saveRecordName(name: String?) {
        if (currentRecordDestination == null || name.isNullOrBlank()) {
            updateRecView()
            return
        }
        val recording: File = File(currentRecordDestination!!)
        val newRecordName = name + ".3gp"
        Log.d("122", newRecordName)
        val newFile = File("${recording.parent}/$newRecordName")
        if (newFile.exists()) {
            val bundle = bundleOf(Pair("isAlert", 121))
            navController.navigate(R.id.action_mainFragment_to_saveRecordDialogFragment, bundle)
            return
        }

        recording.renameTo(File("${recording.parent}/$newRecordName"))
        updateRecView()
    }

    private fun updateRecView() {
        myAdapter.dataSet.clear()
        myAdapter.dataSet.addAll(getFileList().toMutableList())
        myAdapter.notifyItemInserted(0)
        recView.scrollToPosition(0)
    }

    private fun deleteRecord() {
        if (currentRecordDestination == null) {
            return
        }
        try {
            File(currentRecordDestination.toString()).delete()
        } catch (e: IOException) {
        }
    }

    private fun getFileList(): List<File> {
        val path = context?.getExternalFilesDir("/")?.absolutePath
        val files = File(path).listFiles()!!.toList()
        return files.sortedByDescending { it.lastModified() }
    }

    private fun updateProgressBar(position: Int, progress: Int) {
        val viewHolder = recView.findViewHolderForAdapterPosition(position)
        try {
            (viewHolder as? RecyclerViewAdapter.ViewHolder)?.progressBar?.progress = progress
        } catch (e: NullPointerException) {
            Log.d("NPE", e.stackTraceToString())
        }

    }


    private fun changePlayOrPauseButtonIcons(position: Int, isPlaying: Boolean) {
        val viewHolder = recView.findViewHolderForAdapterPosition(position)
        val playButton = (viewHolder as? RecyclerViewAdapter.ViewHolder)?.playButton
        val pauseButton = (viewHolder as? RecyclerViewAdapter.ViewHolder)?.pauseButton
        try {
            if (isPlaying) {
                playButton?.visibility = View.GONE
                pauseButton?.visibility = View.VISIBLE
            } else {
                pauseButton?.visibility = View.GONE
                playButton?.visibility = View.VISIBLE
            }
        } catch (e: NullPointerException) {
            Log.d("NLE", e.stackTraceToString())
        }

    }

    private fun cleanPlayingState() {
        if (playingCardNumber != null) {
            stopPlaying()
            changePlayOrPauseButtonIcons(playingCardNumber!!, false)
            updateProgressBar(playingCardNumber!!, 0)
        }
        playingCardNumber = null
        isPlaying = false
    }

    override fun setOnPlayListener(position: Int, file: File) {
        if (!isPlaying && playingCardNumber == null) {
            startPlaying(file, position)
        }

        if (!isPlaying && playingCardNumber == position) {
            resumePlaying(position)
        }
        if (!isPlaying && playingCardNumber != null && playingCardNumber != position) {
            updateProgressBar(playingCardNumber!!, 0)
            clearProgressbarTimer()
            startPlaying(file, position)
        }

        if (isPlaying && playingCardNumber != null && position != playingCardNumber) {
            stopPlaying()
            changePlayOrPauseButtonIcons(playingCardNumber!!, false)
            updateProgressBar(playingCardNumber!!, 0)
            clearProgressbarTimer()
            startPlaying(file, position)
        }
        changePlayOrPauseButtonIcons(position, true)
        playingCardNumber = position
        isPlaying = true

    }

    override fun setOnPauseListener(position: Int, file: File) {
        if (isPlaying) {
            changePlayOrPauseButtonIcons(position, false)
            mediaPlayer.pause()
        }
        pausePlaying()
        isPlaying = false
    }

    private fun startPlaying(file: File, position: Int) {
        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(file.absolutePath)

        mediaPlayer.setOnCompletionListener(MediaPlayer.OnCompletionListener {
            cleanPlayingState()
            clearProgressbarTimer()
        })
        mediaPlayer.prepare()
        mediaPlayer.start()

        Log.d("duration", mediaPlayer.duration.toString())
        startProgressbarTimer(position)


    }

    private fun startProgressbarTimer(position: Int) {
        progressbarHandler = Timer()
        progressbarHandler.schedule(object : TimerTask() {
            override fun run() {
                updateProgressBar(position, mediaPlayer.currentPosition)
            }
        }, 0, 50)
    }

    private fun clearProgressbarTimer() {
        progressbarHandler.cancel()
        progressbarHandler.purge()
    }

    private fun resumePlaying(position: Int) {
        mediaPlayer.start()
        startProgressbarTimer(position)
    }

    private fun pausePlaying() {
        progressbarHandler.cancel()
        progressbarHandler.purge()

    }

    private fun stopPlaying() {
        mediaPlayer.stop()
        clearProgressbarTimer()
    }


}