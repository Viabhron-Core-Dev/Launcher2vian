package com.vian.vianlauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : ComponentActivity() {

    private lateinit var drawerOverlay: View
    private lateinit var appGrid: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var adapter: AppGridAdapter
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        AppLogger.d("HomeActivity", "onCreate")

        drawerOverlay = findViewById(R.id.app_drawer_overlay)
        appGrid = findViewById(R.id.rv_app_grid)
        searchInput = findViewById(R.id.et_search_apps)
        
        val btnOpenDrawer: Button = findViewById(R.id.btn_open_drawer)
        val btnSettings: Button = findViewById(R.id.btn_settings)

        btnOpenDrawer.setOnClickListener {
            openDrawer()
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnSettings.setOnLongClickListener {
            startActivity(Intent(this, LogViewerActivity::class.java))
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerOverlay.visibility == View.VISIBLE) {
                    closeDrawer()
                } else {
                    // Do nothing, we are at home
                }
            }
        })

        setupDrawer()
    }

    private fun setupDrawer() {
        appGrid.layoutManager = GridLayoutManager(this, 4)
        adapter = AppGridAdapter(emptyList(), packageManager) { resolveInfo ->
            launchApp(resolveInfo)
        }
        appGrid.adapter = adapter

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        loadApps()
    }

    private fun loadApps() {
        scope.launch {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = withContext(Dispatchers.IO) {
                val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                list.sortedBy { it.loadLabel(packageManager).toString().lowercase() }
            }
            adapter.updateApps(resolveInfos)
        }
    }

    private fun openDrawer() {
        AppLogger.d("Drawer", "Drawer opened")
        drawerOverlay.visibility = View.VISIBLE
        searchInput.text.clear()
        searchInput.requestFocus()
    }

    private fun closeDrawer() {
        AppLogger.d("Drawer", "Drawer closed")
        drawerOverlay.visibility = View.INVISIBLE
    }

    private fun launchApp(resolveInfo: ResolveInfo) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                closeDrawer()
            }
        } catch (e: Exception) {
            AppLogger.e("Drawer", "Failed to launch app", e)
        }
    }
}
