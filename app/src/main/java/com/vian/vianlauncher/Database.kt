package com.vian.vianlauncher

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update

@Entity(tableName = "workspace_items")
data class WorkspaceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val activityName: String,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int,
    val spanY: Int,
    val page: Int,
    val container: Int
)

@Entity(tableName = "folder_info")
data class FolderInfo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val cellX: Int,
    val cellY: Int,
    val page: Int
)

@Entity(tableName = "app_preferences")
data class AppPreference(
    @PrimaryKey val packageName: String,
    val isHidden: Boolean,
    val customLabel: String?
)

@Dao
interface WorkspaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WorkspaceItem): Long

    @Update
    suspend fun update(item: WorkspaceItem)

    @Query("DELETE FROM workspace_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM workspace_items")
    suspend fun getAll(): List<WorkspaceItem>

    @Query("SELECT * FROM workspace_items WHERE container = :container")
    suspend fun getAllForContainer(container: Int): List<WorkspaceItem>

    @Query("DELETE FROM workspace_items WHERE container = :container")
    suspend fun clearContainer(container: Int)
}

@Dao
interface AppPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pref: AppPreference)

    @Update
    suspend fun update(pref: AppPreference)

    @Query("SELECT * FROM app_preferences")
    suspend fun getAll(): List<AppPreference>

    @Query("SELECT * FROM app_preferences WHERE packageName = :packageName")
    suspend fun get(packageName: String): AppPreference?
}

@Dao
interface FolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderInfo): Long
    @Update
    suspend fun update(folder: FolderInfo)
    @Query("DELETE FROM folder_info WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("SELECT * FROM folder_info")
    suspend fun getAll(): List<FolderInfo>
    @Query("SELECT * FROM folder_info WHERE page = :page")
    suspend fun getForPage(page: Int): List<FolderInfo>
    @Query("SELECT * FROM folder_info WHERE id = :id")
    suspend fun get(id: Long): FolderInfo?
}

@Database(entities = [WorkspaceItem::class, FolderInfo::class, AppPreference::class], version = 1, exportSchema = false)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun appPreferenceDao(): AppPreferenceDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: LauncherDatabase? = null

        fun getDatabase(context: Context): LauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LauncherDatabase::class.java,
                    "launcher_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
