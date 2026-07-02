package com.vian.vianlauncher

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        AppLogger.d("SettingsActivity", "onCreate")

        findViewById<Button>(R.id.btn_view_logs).setOnClickListener {
            startActivity(android.content.Intent(this, LogViewerActivity::class.java))
        }

        val prefs = getSharedPreferences("vian_launcher_prefs", Context.MODE_PRIVATE)

        // Grid Settings
        val spinColumns = findViewById<Spinner>(R.id.spin_columns)
        val spinRows = findViewById<Spinner>(R.id.spin_rows)
        
        val colAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("3", "4", "5", "6"))
        spinColumns.adapter = colAdapter
        spinColumns.setSelection(prefs.getInt("grid_cols", 4) - 3)
        spinColumns.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val value = position + 3
                prefs.edit().putInt("grid_cols", value).apply()
                AppLogger.d("SettingsActivity", "Set grid columns: $value")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val rowAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("4", "5", "6", "7", "8"))
        spinRows.adapter = rowAdapter
        spinRows.setSelection(prefs.getInt("grid_rows", 5) - 4)
        spinRows.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val value = position + 4
                prefs.edit().putInt("grid_rows", value).apply()
                AppLogger.d("SettingsActivity", "Set grid rows: $value")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Icon Size
        val spinIconSize = findViewById<Spinner>(R.id.spin_icon_size)
        val sizes = listOf("Small", "Medium", "Large")
        val sizeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sizes)
        spinIconSize.adapter = sizeAdapter
        spinIconSize.setSelection(prefs.getInt("icon_size", 1))
        spinIconSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putInt("icon_size", position).apply()
                AppLogger.d("SettingsActivity", "Set icon size: ${sizes[position]}")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Clear Home Screen
        findViewById<Button>(R.id.btn_clear_home).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Home Screen")
                .setMessage("Are you sure you want to clear the workspace?")
                .setPositiveButton("Clear") { _, _ ->
                    scope.launch(Dispatchers.IO) {
                        LauncherDatabase.getDatabase(this@SettingsActivity).workspaceDao().clearContainer(0)
                        AppLogger.d("SettingsActivity", "Workspace cleared")
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Modules
        setupSwitch(R.id.switch_sidebar, "sidebarEnabled") { ModuleRegistry.sidebarEnabled = it }
        setupSwitch(R.id.switch_netspeed, "netSpeedEnabled") { ModuleRegistry.netSpeedEnabled = it }
        setupSwitch(R.id.switch_callrecorder, "callRecorderEnabled") { ModuleRegistry.callRecorderEnabled = it }
        setupSwitch(R.id.switch_epub, "epubReaderEnabled") { ModuleRegistry.epubReaderEnabled = it }
        setupSwitch(R.id.switch_apptracker, "appTrackerEnabled") { ModuleRegistry.appTrackerEnabled = it }
        setupSwitch(R.id.switch_pwaloader, "pwaloaderEnabled") { ModuleRegistry.pwaloaderEnabled = it }
        setupSwitch(R.id.switch_llmservice, "llmServiceEnabled") { ModuleRegistry.llmServiceEnabled = it }
        setupSwitch(R.id.switch_scheduler, "schedulerEnabled") { ModuleRegistry.schedulerEnabled = it }

        val tvAbout = findViewById<TextView>(R.id.tv_about)
        tvAbout.text = "${getString(R.string.app_name)}\n${packageName}\nVersion ${BuildConfig.VERSION_NAME}"
    }

    private fun setupSwitch(id: Int, name: String, setter: (Boolean) -> Unit) {
        val switch = findViewById<Switch>(id)
        val prefs = getSharedPreferences("vian_launcher_prefs", Context.MODE_PRIVATE)
        switch.isChecked = prefs.getBoolean(name, false)
        switch.setOnCheckedChangeListener { _, isChecked ->
            setter(isChecked)
            AppLogger.d("SettingsActivity", "Module $name set to $isChecked")
        }
    }
}
