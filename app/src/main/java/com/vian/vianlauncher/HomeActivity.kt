package com.vian.vianlauncher

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.provider.Settings

class HomeActivity : ComponentActivity() {

    private lateinit var drawerOverlay: View
    private lateinit var appGrid: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var adapter: AppGridAdapter
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private lateinit var workspace: Workspace
    private lateinit var hotseat: Hotseat
    private lateinit var pageIndicator: LinearLayout
    
    private var lastGridCols = 4
    private var lastGridRows = 5
    
    private var allAppsList = listOf<ResolveInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_home)
        AppLogger.d("HomeActivity", "onCreate")

        workspace = findViewById(R.id.workspace)
        hotseat = findViewById(R.id.hotseat)
        pageIndicator = findViewById(R.id.page_indicator)
        drawerOverlay = findViewById(R.id.app_drawer_overlay)
        appGrid = findViewById(R.id.rv_app_grid)
        searchInput = findViewById(R.id.et_search_apps)
        
        val btnSettings: Button = findViewById(R.id.btn_settings)

        ViewCompat.setOnApplyWindowInsetsListener(hotseat) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, systemBars.bottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(btnSettings) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            (view.layoutParams as ViewGroup.MarginLayoutParams).topMargin = systemBars.top + 32
            view.requestLayout()
            insets
        }
        
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerOverlay.visibility == View.VISIBLE) {
                    closeDrawer()
                }
            }
        })

        setupDrawer()
        
        workspace.onPageChangeListener = { page ->
            updatePageIndicator(page)
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("vian_launcher_prefs", Context.MODE_PRIVATE)
        val cols = prefs.getInt("grid_cols", 4)
        val rows = prefs.getInt("grid_rows", 5)
        
        if (cols != lastGridCols || rows != lastGridRows) {
            AppLogger.d("HomeActivity", "Grid size changed, clearing workspace")
            lastGridCols = cols
            lastGridRows = rows
            scope.launch(Dispatchers.IO) {
                LauncherDatabase.getDatabase(this@HomeActivity).workspaceDao().clearContainer(0)
                withContext(Dispatchers.Main) {
                    rebuildWorkspaceAndHotseat()
                }
            }
        } else {
            rebuildWorkspaceAndHotseat()
        }
    }

    private fun rebuildWorkspaceAndHotseat() {
        workspace.setup(lastGridCols, lastGridRows)
        setupPageIndicator(workspace.pages.size)
        updatePageIndicator(workspace.currentPage)
        
        hotseat.clearItems()
        
        val drawerToggle = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_sort_by_size)
            setOnClickListener { openDrawer() }
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        hotseat.placeView(drawerToggle, 2, 0)
        
        scope.launch {
            val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val resolveInfos = withContext(Dispatchers.IO) {
                val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                list.sortedBy { it.loadLabel(packageManager).toString().lowercase() }
            }
            allAppsList = resolveInfos
            
            var hotseatIndex = 0
            val hotseatPackages = mutableListOf<String>()
            for (i in 0 until 4) {
                if (i < resolveInfos.size) {
                    if (hotseatIndex == 2) hotseatIndex++
                    val appInfo = resolveInfos[i]
                    val appView = createAppView(appInfo, false)
                    hotseat.placeView(appView, hotseatIndex, 0)
                    appView.setOnClickListener { launchApp(appInfo) }
                    hotseatPackages.add(appInfo.activityInfo.packageName)
                    hotseatIndex++
                }
            }
            AppLogger.d("Hotseat", "Populated with packages: $hotseatPackages")
            
            val items = withContext(Dispatchers.IO) {
                LauncherDatabase.getDatabase(this@HomeActivity).workspaceDao().getAllForContainer(0)
            }
            
            for (item in items) {
                if (item.page in 0 until workspace.pages.size) {
                    val page = workspace.pages[item.page]
                    val appInfo = resolveInfos.find { it.activityInfo.packageName == item.packageName && it.activityInfo.name == item.activityName }
                    if (appInfo != null) {
                        val appView = createAppView(appInfo, true)
                        page.placeView(appView, item.cellX, item.cellY)
                        appView.setOnClickListener { launchApp(appInfo) }
                        appView.setOnLongClickListener { showAppOptions(item, appInfo, page); true }
                    }
                }
            }
            
            for (p in 0 until workspace.pages.size) {
                val page = workspace.pages[p]
                page.setOnTouchListener { _, ev ->
                    if (ev.action == android.view.MotionEvent.ACTION_UP) {
                        val cellWidth = page.cellWidth
                        val cellHeight = page.cellHeight
                        if (cellWidth > 0 && cellHeight > 0) {
                            val cellX = (ev.x / cellWidth).toInt()
                            val cellY = (ev.y / cellHeight).toInt()
                            if (cellX in 0 until page.columnCount && cellY in 0 until page.rowCount) {
                                if (!page.isOccupied(cellX, cellY)) {
                                    showAppPicker(p, cellX, cellY, page)
                                }
                            }
                        }
                    }
                    true
                }
            }
        }
    }

    private fun createAppView(resolveInfo: ResolveInfo, showLabel: Boolean): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setPadding(8, 8, 8, 8)
        }
        val icon = ImageView(this).apply {
            setImageDrawable(resolveInfo.loadIcon(packageManager))
            layoutParams = LinearLayout.LayoutParams(120, 120)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        container.addView(icon)
        
        if (showLabel) {
            val label = TextView(this).apply {
                text = resolveInfo.loadLabel(packageManager)
                setTextColor(Color.parseColor("#FFFFFF"))
                textSize = 12f
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 8
                }
            }
            container.addView(label)
        }
        return container
    }

    private fun showAppPicker(pageIndex: Int, cellX: Int, cellY: Int, pageLayout: CellLayout) {
        val appNames = allAppsList.map { it.loadLabel(packageManager).toString() }
        AlertDialog.Builder(this)
            .setTitle("Select App")
            .setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, appNames)) { _, which ->
                val appInfo = allAppsList[which]
                val item = WorkspaceItem(
                    packageName = appInfo.activityInfo.packageName,
                    activityName = appInfo.activityInfo.name,
                    cellX = cellX,
                    cellY = cellY,
                    spanX = 1,
                    spanY = 1,
                    page = pageIndex,
                    container = 0
                )
                scope.launch(Dispatchers.IO) {
                    LauncherDatabase.getDatabase(this@HomeActivity).workspaceDao().insert(item)
                    withContext(Dispatchers.Main) {
                        rebuildWorkspaceAndHotseat()
                    }
                }
            }
            .show()
    }

    private fun showAppOptions(item: WorkspaceItem, resolveInfo: ResolveInfo, pageLayout: CellLayout) {
        AlertDialog.Builder(this)
            .setTitle(resolveInfo.loadLabel(packageManager))
            .setItems(arrayOf("App Info", "Remove from Home")) { _, which ->
                if (which == 0) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${item.packageName}")
                    }
                    startActivity(intent)
                } else if (which == 1) {
                    scope.launch(Dispatchers.IO) {
                        LauncherDatabase.getDatabase(this@HomeActivity).workspaceDao().delete(item.id)
                        withContext(Dispatchers.Main) {
                            rebuildWorkspaceAndHotseat()
                        }
                    }
                }
            }
            .show()
    }

    private fun setupPageIndicator(count: Int) {
        pageIndicator.removeAllViews()
        for (i in 0 until count) {
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(20, 20).apply {
                    setMargins(8, 0, 8, 0)
                }
                setBackgroundColor(Color.parseColor(if (i == 0) "#FFFFFF" else "#444444"))
            }
            pageIndicator.addView(dot)
        }
    }

    private fun updatePageIndicator(position: Int) {
        for (i in 0 until pageIndicator.childCount) {
            val dot = pageIndicator.getChildAt(i)
            dot.setBackgroundColor(Color.parseColor(if (i == position) "#FFFFFF" else "#444444"))
        }
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
