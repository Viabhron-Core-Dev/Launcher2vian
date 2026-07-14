import re
with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'r') as f:
    content = f.read()

target = """                } else {
                    showAppPicker(page, cellX, cellY, pageLayout)
                }"""

replacement = """                } else {
                    showEmptyCellOptions(page, cellX, cellY, pageLayout)
                }"""

content = content.replace(target, replacement)

# Add showEmptyCellOptions and createFolderPrompt and openFolder and showFolderOptions
new_methods = """    private fun showEmptyCellOptions(pageIndex: Int, cellX: Int, cellY: Int, pageLayout: CellLayout) {
        val options = arrayOf("Add Icon", "Add Folder", "Add Page")
        AlertDialog.Builder(this)
            .setTitle("Add to Home")
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Add Icon" -> showAppPicker(pageIndex, cellX, cellY, pageLayout)
                    "Add Folder" -> createFolderPrompt(pageIndex, cellX, cellY)
                    "Add Page" -> {
                        val prefs = getSharedPreferences("vian_launcher_prefs", Context.MODE_PRIVATE)
                        val currentPages = prefs.getInt("grid_pages", 3)
                        if (currentPages < 5) {
                            prefs.edit().putInt("grid_pages", currentPages + 1).apply()
                            val cols = prefs.getInt("grid_cols", 4)
                            val rows = prefs.getInt("grid_rows", 5)
                            val dockCount = prefs.getInt("dock_count", 5)
                            lastGridCols = cols
                            lastGridRows = rows
                            lastGridPages = currentPages + 1
                            lastDockCount = dockCount
                            scope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.Main) {
                                    rebuildWorkspaceAndHotseat()
                                }
                            }
                        } else {
                            Toast.makeText(this, "Maximum pages reached", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }

    private fun createFolderPrompt(pageIndex: Int, cellX: Int, cellY: Int) {
        val input = EditText(this)
        input.hint = "Folder Name"
        AlertDialog.Builder(this)
            .setTitle("New Folder")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().takeIf { it.isNotBlank() } ?: "Folder"
                val newFolder = FolderInfo(name = name, color = Color.GRAY, cellX = cellX, cellY = cellY, page = pageIndex)
                scope.launch(Dispatchers.IO) {
                    LauncherDatabase.getDatabase(this@HomeActivity).folderDao().insert(newFolder)
                    withContext(Dispatchers.Main) {
                        rebuildWorkspaceAndHotseat()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createFolderView(folder: FolderInfo): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(4, 4, 4, 4)
            tag = CellInfo(folder.cellX, folder.cellY, 1, 1)
        }
        val iconContainer = FrameLayout(this).apply {
            val size = dpToPx(56)
            layoutParams = LinearLayout.LayoutParams(size, size)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(folder.color)
            }
        }
        // Small icon inside folder
        val innerIcon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_agenda)
            layoutParams = FrameLayout.LayoutParams(dpToPx(32), dpToPx(32)).apply {
                gravity = Gravity.CENTER
            }
        }
        iconContainer.addView(innerIcon)
        
        val label = TextView(this).apply {
            text = folder.name
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        container.addView(iconContainer)
        container.addView(label)
        
        container.setOnClickListener { openFolder(folder) }
        container.setOnLongClickListener {
            showFolderOptions(folder)
            true
        }
        return container
    }

    private fun openFolder(folder: FolderInfo) {
        scope.launch(Dispatchers.IO) {
            val db = LauncherDatabase.getDatabase(this@HomeActivity)
            val items = db.workspaceDao().getAllForContainer(-folder.id.toInt())
            withContext(Dispatchers.Main) {
                showFolderPopup(folder, items)
            }
        }
    }

    private fun showFolderPopup(folder: FolderInfo, items: List<WorkspaceItem>) {
        val grid = GridLayout(this).apply {
            columnCount = 3
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(folder.name)
            .setView(grid)
            .setPositiveButton("Close", null)
            .create()

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
        
        for (item in items) {
            val appInfo = resolveInfos.find { it.activityInfo.packageName == item.packageName && it.activityInfo.name == item.activityName }
            if (appInfo != null) {
                val appView = createAppView(appInfo, true)
                appView.setOnClickListener { 
                    launchApp(appInfo)
                    dialog.dismiss()
                }
                appView.setOnLongClickListener {
                    // Option to remove from folder
                    AlertDialog.Builder(this)
                        .setTitle("Remove ${appInfo.loadLabel(packageManager)}")
                        .setItems(arrayOf("Remove from Folder")) { _, _ ->
                            scope.launch(Dispatchers.IO) {
                                LauncherDatabase.getDatabase(this@HomeActivity).workspaceDao().delete(item.id)
                                withContext(Dispatchers.Main) {
                                    dialog.dismiss()
                                    rebuildWorkspaceAndHotseat()
                                }
                            }
                        }
                        .show()
                    true
                }
                grid.addView(appView)
            }
        }
        if (items.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "Folder is empty"
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            }
            grid.addView(emptyText)
        }
        dialog.show()
    }

    private fun showFolderOptions(folder: FolderInfo) {
        val options = arrayOf("Rename", "Change Color", "Remove Folder")
        AlertDialog.Builder(this)
            .setTitle(folder.name)
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Rename" -> {
                        val input = EditText(this)
                        input.setText(folder.name)
                        AlertDialog.Builder(this)
                            .setTitle("Rename Folder")
                            .setView(input)
                            .setPositiveButton("Save") { _, _ ->
                                val newName = input.text.toString().takeIf { it.isNotBlank() } ?: folder.name
                                scope.launch(Dispatchers.IO) {
                                    val db = LauncherDatabase.getDatabase(this@HomeActivity)
                                    db.folderDao().update(folder.copy(name = newName))
                                    withContext(Dispatchers.Main) { rebuildWorkspaceAndHotseat() }
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    "Change Color" -> {
                        val colors = arrayOf("Gray", "Red", "Green", "Blue")
                        val colorValues = arrayOf(Color.GRAY, Color.parseColor("#E53935"), Color.parseColor("#43A047"), Color.parseColor("#1E88E5"))
                        AlertDialog.Builder(this)
                            .setTitle("Select Color")
                            .setItems(colors) { _, colorWhich ->
                                scope.launch(Dispatchers.IO) {
                                    val db = LauncherDatabase.getDatabase(this@HomeActivity)
                                    db.folderDao().update(folder.copy(color = colorValues[colorWhich]))
                                    withContext(Dispatchers.Main) { rebuildWorkspaceAndHotseat() }
                                }
                            }
                            .show()
                    }
                    "Remove Folder" -> {
                        AlertDialog.Builder(this)
                            .setTitle("Remove Folder")
                            .setMessage("Are you sure? Items inside will be removed from the home screen.")
                            .setPositiveButton("Remove") { _, _ ->
                                scope.launch(Dispatchers.IO) {
                                    val db = LauncherDatabase.getDatabase(this@HomeActivity)
                                    db.workspaceDao().clearContainer(-folder.id.toInt())
                                    db.folderDao().delete(folder.id)
                                    withContext(Dispatchers.Main) { rebuildWorkspaceAndHotseat() }
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
            .show()
    }

    fun showAppOptions"""

content = content.replace("    fun showAppOptions", new_methods)

with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'w') as f:
    f.write(content)
print("Updated HomeActivity Part 1 and folder UI")
