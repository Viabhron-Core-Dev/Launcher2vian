package com.vian.vianlauncher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class LogViewerActivity : ComponentActivity() {

    private lateinit var tvLogContent: TextView
    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentFile: File? = null

    private val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    private val runningLogFile = File(downloadsDir, "vian_launcher_log.txt")
    private val crashLogFile = File(downloadsDir, "vian_launcher_crash_latest.txt")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_viewer)
        AppLogger.d("LogViewer", "onCreate")

        tvLogContent = findViewById(R.id.tv_log_content)

        findViewById<Button>(R.id.btn_tab_running).setOnClickListener {
            loadLogFile(runningLogFile)
        }

        findViewById<Button>(R.id.btn_tab_crash).setOnClickListener {
            loadLogFile(crashLogFile)
        }

        findViewById<Button>(R.id.btn_copy).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Log", tvLogContent.text)
            cm.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            Toast.makeText(this, "Logs are already saved in Downloads", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_share).setOnClickListener {
            currentFile?.let { file ->
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share Log"))
                }
            }
        }

        findViewById<Button>(R.id.btn_clear).setOnClickListener {
            currentFile?.let { file ->
                if (file.exists()) {
                    file.delete()
                    tvLogContent.text = ""
                    Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Default tab
        loadLogFile(runningLogFile)
    }

    private fun loadLogFile(file: File) {
        currentFile = file
        scope.launch {
            val content = withContext(Dispatchers.IO) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !android.os.Environment.isExternalStorageManager()) {
                    "Storage permission not granted. Grant it in Settings to view logs."
                } else if (file.exists()) {
                    file.readText()
                } else {
                    "File not found: ${file.name}"
                }
            }
            tvLogContent.text = content
        }
    }
}
