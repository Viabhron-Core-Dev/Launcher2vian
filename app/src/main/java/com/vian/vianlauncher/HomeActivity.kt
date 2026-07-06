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
    private lateinit var dragLayer: DragLayer
    private lateinit var dragController: DragController
    
    private var lastGridCols = 4
    private var lastGridRows = 5
    private var lastGridPages = 3
    
    private var allAppsList = listOf<ResolveInfo>()
    private var currentWorkspaceItems = listOf<WorkspaceItem>()
    private var isFirstResume = true

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
        
        dragLayer = findViewById(R.id.drag_layer)
        dragController = DragController(this, workspace, hotseat, dragLayer)
        dragLayer.dragController = dragController
        
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

        workspace.onCellTap = { page, cellX, cellY ->
            val pageLayout = workspace.pages[page]
            if (pageLayout.isOccupied(cellX, cellY)) {
                val item = currentWorkspaceItems.find { 
                    it.page == page && 
                    cellX >= it.cellX && cellX < it.cellX + it.spanX &&
                    cellY >= it.cellY && cellY < it.cellY + it.spanY 
                }
                if (item != null) {
                    if (item.packageName == "__CLOCK_WIDGET__") {
                        for (i in 0 until pageLayout.childCount) {
                            val child = pageLayout.getChildAt(i)
                            val info = child.tag as? CellInfo
                            if (info?.cellX == item.cellX && info?.cellY == item.cellY && child is ClockWidgetView) {
                                child.launchClockApp()
                                break
                            }
                        }
                    } else {
                        val appInfo = allAppsList.find { it.activityInfo.packageName == item.packageName && it.activityInfo.name == item.activityName }
                        if (appInfo != null) launchApp(appInfo)
                    }
                }
            }
        }

        workspace.onCellLongPress = { page, cellX, cellY ->
            val pageLayout = workspace.pages.getOrNull(page)
            val isOccupied = pageLayout?.isOccupied(cellX, cellY) ?: false
            AppLogger.d("HomeActivity", "onCellLongPress received: page=$page cellX=$cellX cellY=$cellY, occupied=$isOccupied")
            if (pageLayout != null) {
                if (isOccupied) {
                    val item = currentWorkspaceItems.find { 
                        it.page == page && 
                        cellX >= it.cellX && cellX < it.cellX + it.spanX &&
                        cellY >= it.cellY && cellY < it.cellY + it.spanY 
                    }
                    AppLogger.d("HomeActivity", "Found item for long press: $item")
                    if (item != null) {
                        var targetView: View? = null
                        for (i in 0 until pageLayout.childCount) {
                            val child = pageLayout.getChildAt(i)
                            val info = child.tag as? CellInfo
                            if (info?.cellX == item.cellX && info?.cellY == item.cellY) {
                                targetView = child
                                break
                            }
                        }
                        if (item.packageName == "__CLOCK_WIDGET__") {
                            showClockOptions(item, pageLayout, targetView)
                        } else {
                            val appInfo = allAppsList.find { it.activityInfo.packageName == item.packageName && it.activityInfo.name == item.activityName }
                            AppLogger.d("HomeActivity", "Found appInfo for long press: $appInfo")
                            if (appInfo != null) showAppOptions(item, appInfo, pageLayout, targetView)
                        }
                    }
                } else {
                    showAppPicker(page, cellX, cellY, pageLayout)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("vian_launcher_prefs", Context.MODE_PRIVATE)
        val cols = prefs.getInt("grid_cols", 4)
        val rows = prefs.getInt("grid_rows", 5)
        val pages = prefs.getInt("grid_pages", 3)
        
        if (isFirstResume) {
            isFirstResume = false
            lastGridCols = cols
            lastGridRows = rows
            lastGridPages = pages
            scope.launch(Dispatchers.IO) {
                migrateClockPosition(rows, cols)
                withContext(Dispatchers.Main) {
                    rebuildWorkspaceAndHotseat()
                }
            }
        } else if (cols != lastGridCols || rows != lastGridRows || pages != lastGridPages) {
            AppLogger.d("HomeActivity", "Grid size or pages changed, updating layout")
            
            scope.launch(Dispatchers.IO) {
                val db = LauncherDatabase.getDatabase(this@HomeActivity).workspaceDao()
                val items = db.getAllForContainer(0)
                
                var itemsLost = 0
                for (item in items) {
                    val outOfBounds = item.packageName != "__CLOCK_WIDGET__" && (item.cellX >= cols || item.cellY >= rows)
                    val removedPage = item.page >= pages
                    if (outOfBounds || removedPage) {
                        itemsLost++
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (itemsLost > 0) {
                        AlertDialog.Builder(this@HomeActivity)
                            .setTitle("Grid Size Reduced")
                            .setMessage("Shrinking the grid or page count will delete $itemsLost item(s) that no longer fit. Continue?")
                            .setPositiveButton("Confirm") { _, _ ->
                                lastGridCols = cols
                                lastGridRows = rows
                                lastGridPages = pages
                                scope.launch(Dispatchers.IO) {
                                    for (item in items) {
                                        val outOfBounds = item.packageName != "__CLOCK_WIDGET__" && (item.cellX >= cols || item.cellY >= rows)
                                        val removedPage = item.page >= pages
                                        if (outOfBounds || removedPage) {
                                            db.delete(item.id)
                                        }
                                    }
                                    migrateClockPosition(rows, cols)
                                    withContext(Dispatchers.Main) {
                                        rebuildWorkspaceAndHotseat()
                                    }
                                }
                            }
                            .setNegativeButton("Cancel") { _, _ ->
                                prefs.edit()
                                    .putInt("grid_cols", lastGridCols)
                                    .putInt("grid_rows", lastGridRows)
                                    .putInt("grid_pages", lastGridPages)
                                    .apply()
                            }
                            .show()
                    } else {
                        lastGridCols = cols
                        lastGridRows = rows
                        lastGridPages = pages
                        scope.launch(Dispatchers.IO) {
                            for (item in items) {
                                val outOfBounds = item.packageName != "__CLOCK_WIDGET__" && (item.cellX >= cols || item.cellY >= rows)
                                val removedPage = item.page >= pages
                                if (outOfBounds || removedPage) {
                                    db.delete(item.id)
                                }
                            }
                            migrateClockPosition(rows, cols)
                            withContext(Dispatchers.Main) {
                                rebuildWorkspaceAndHotseat()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun findValidClockPosition(rows: Int, cols: Int, page: Int, existingItems: List<WorkspaceItem>): Pair<Int, Int>? {
        val spanX = 4
        val spanY = 2
        
        if (spanX > cols || spanY > rows) return null
        
        val itemsOnPage = existingItems.filter { it.page == page && it.packageName != "__CLOCK_WIDGET__" }
        
        fun isOccupied(x: Int, y: Int): Boolean {
            for (item in itemsOnPage) {
                val itemSpanX = item.spanX
                val itemSpanY = item.spanY
                val overlapX = x < item.cellX + itemSpanX && x + spanX > item.cellX
                val overlapY = y < item.cellY + itemSpanY && y + spanY > item.cellY
                if (overlapX && overlapY) return true
            }
            return false
        }

        val defaultY = Math.max(0, rows - 3)
        val defaultX = 0
        if (defaultX + spanX <= cols && defaultY + spanY <= rows && !isOccupied(defaultX, defaultY)) {
            return Pair(defaultX, defaultY)
        }
        
        for (y in 0 .. rows - spanY) {
            for (x in 0 .. cols - spanX) {
                if (!isOccupied(x, y)) {
                    return Pair(x, y)
                }
            }
        }
        
        return null
    }

    private suspend fun migrateClockPosition(rows: Int, cols: Int) {
        val db = LauncherDatabase.getDatabase(this@HomeActivity).workspaceDao()
        val items = db.getAllForContainer(0)
        val clockItem = items.find { it.packageName == "__CLOCK_WIDGET__" }
        
        if (clockItem != null) {
            if (clockItem.cellX + clockItem.spanX > cols || clockItem.cellY + clockItem.spanY > rows) {
                val newPos = findValidClockPosition(rows, cols, clockItem.page, items)
                if (newPos != null) {
                    db.update(clockItem.copy(cellX = newPos.first, cellY = newPos.second))
                    AppLogger.d("HomeActivity", "Migrated clock to valid position: ${newPos.first}, ${newPos.second}")
                } else {
                    db.delete(clockItem.id)
                    AppLogger.d("HomeActivity", "No valid position found for clock migration, deleted")
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun rebuildWorkspaceAndHotseat() {
        workspace.setup(lastGridCols, lastGridRows, lastGridPages)
        setupPageIndicator(workspace.pages.size)
        updatePageIndicator(workspace.currentPage)
        
        hotseat.clearItems()
        
        val drawerToggle = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_sort_by_size)
            setOnClickListener { openDrawer() }
            val size = dpToPx(56)
            layoutParams = ViewGroup.LayoutParams(size, size)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        val drawerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addView(drawerToggle)
        }
        hotseat.placeView(drawerContainer, 2, 0)
        
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
                    appView.setOnLongClickListener { 
                        showAppOptions(null, appInfo, null, appView)
                        true 
                    }
                    hotseatPackages.add(appInfo.activityInfo.packageName)
                    hotseatIndex++
                }
            }
            AppLogger.d("Hotseat", "Populated with packages: $hotseatPackages")
            
            var items = withContext(Dispatchers.IO) {
                LauncherDatabase.getDatabase(this@HomeActivity).workspaceDao().getAllForContainer(0)
            }
            
            val hasClock = items.any { it.packageName == "__CLOCK_WIDGET__" }
            if (!hasClock) {
                val newPos = findValidClockPosition(lastGridRows, lastGridCols, 0, items)
                if (newPos != null) {
                    val clockItem = WorkspaceItem(
                        packageName = "__CLOCK_WIDGET__",
                        activityName = "",
                        cellX = newPos.first,
                        cellY = newPos.second,
                        spanX = 4,
                        spanY = 2,
                        page = 0,
                        container = 0
                    )
                    withContext(Dispatchers.IO) {
                        LauncherDatabase.getDatabase(this@HomeActivity).workspaceDao().insert(clockItem)
                    }
                    items = withContext(Dispatchers.IO) {
                        LauncherDatabase.getDatabase(this@HomeActivity).workspaceDao().getAllForContainer(0)
                    }
                } else {
                    AppLogger.d("HomeActivity", "No valid position found for clock widget seed, skipped")
                }
            }
            
            currentWorkspaceItems = items
            
            for (item in items) {
                if (item.page in 0 until workspace.pages.size) {
                    val page = workspace.pages[item.page]
                    if (item.packageName == "__CLOCK_WIDGET__") {
                        val clockView = ClockWidgetView(this@HomeActivity)
                        page.placeView(clockView, item.cellX, item.cellY, item.spanX, item.spanY)
                    } else {
                        val appInfo = resolveInfos.find { it.activityInfo.packageName == item.packageName && it.activityInfo.name == item.activityName }
                        if (appInfo != null) {
                            val appView = createAppView(appInfo, false)
                            page.placeView(appView, item.cellX, item.cellY, item.spanX, item.spanY)
                        }
                    }
                }
            }
        }
    }

    private fun createAppView(resolveInfo: ResolveInfo, showLabel: Boolean): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(4, 4, 4, 4)
        }
        val icon = ImageView(this).apply {
            setImageDrawable(resolveInfo.loadIcon(packageManager))
            val size = dpToPx(56)
            layoutParams = LinearLayout.LayoutParams(size, size)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        container.addView(icon)
        
        if (showLabel) {
            val label = TextView(this).apply {
                text = resolveInfo.loadLabel(packageManager)
                setTextColor(Color.parseColor("#FFFFFF"))
                textSize = 10f
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 4
                }
            }
            container.addView(label)
        }
        return container
    }

    private fun showClockOptions(item: WorkspaceItem, pageLayout: CellLayout, view: View?) {
        val options = arrayOf("Move", "Remove from Home")
        AlertDialog.Builder(this)
            .setTitle("Clock Widget")
            .setItems(options) { _, which ->
                if (options[which] == "Remove from Home") {
                    scope.launch(Dispatchers.IO) {
                        LauncherDatabase.getDatabase(this@HomeActivity).workspaceDao().delete(item.id)
                        withContext(Dispatchers.Main) {
                            rebuildWorkspaceAndHotseat()
                        }
                    }
                } else if (options[which] == "Move" && view != null) {
                    dragController.startDrag(view, item, item.page, item.cellX, item.cellY, false)
                }
            }
            .show()
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

    private fun showAppOptions(item: WorkspaceItem?, resolveInfo: ResolveInfo, pageLayout: CellLayout?, view: View? = null) {
        val options = if (item != null) arrayOf("App Info", "Remove from Home", "Move") else arrayOf("App Info", "Move")
        AlertDialog.Builder(this)
            .setTitle(resolveInfo.loadLabel(packageManager))
            .setItems(options) { _, which ->
                val option = options[which]
                if (option == "App Info") {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${resolveInfo.activityInfo.packageName}")
                    }
                    startActivity(intent)
                } else if (option == "Remove from Home" && item != null) {
                    scope.launch(Dispatchers.IO) {
                        LauncherDatabase.getDatabase(this@HomeActivity).workspaceDao().delete(item.id)
                        withContext(Dispatchers.Main) {
                            rebuildWorkspaceAndHotseat()
                        }
                    }
                } else if (option == "Move" && view != null) {
                    val isHotseat = item == null
                    if (isHotseat) {
                        val slot = view.parent as? View
                        val index = if (slot != null) hotseat.indexOfChild(slot) else -1
                        if (index != -1) {
                            dragController.startDrag(view, null, -1, index, 0, true)
                        }
                    } else {
                        dragController.startDrag(view, item, item.page, item.cellX, item.cellY, false)
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

    fun refreshWorkspaceItemsList() {
        scope.launch(Dispatchers.IO) {
            val items = LauncherDatabase.getDatabase(this@HomeActivity).workspaceDao().getAllForContainer(0)
            withContext(Dispatchers.Main) {
                currentWorkspaceItems = items
                AppLogger.d("HomeActivity", "Refreshed currentWorkspaceItems, count=${items.size}")
            }
        }
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
