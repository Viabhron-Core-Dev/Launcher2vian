import re
with open('app/src/main/java/com/vian/vianlauncher/Database.kt', 'r') as f:
    content = f.read()

dao_code = """@Dao
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

@Database"""

content = content.replace("@Database", dao_code)

abstract_method = """    abstract fun appPreferenceDao(): AppPreferenceDao
    abstract fun folderDao(): FolderDao"""

content = content.replace("    abstract fun appPreferenceDao(): AppPreferenceDao", abstract_method)

with open('app/src/main/java/com/vian/vianlauncher/Database.kt', 'w') as f:
    f.write(content)
print("Updated Database.kt")
