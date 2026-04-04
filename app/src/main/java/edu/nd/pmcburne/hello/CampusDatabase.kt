package edu.nd.pmcburne.hello

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "placemarks")
data class PlacemarkEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double
)

@Entity(
    tableName = "placemark_tags",
    primaryKeys = ["placemarkId", "tag"]
)
data class PlacemarkTagEntity(
    val placemarkId: Int,
    val tag: String
)

@Dao
interface PlacemarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlacemark(placemark: PlacemarkEntity)

    @Query("DELETE FROM placemark_tags WHERE placemarkId = :placemarkId")
    suspend fun deleteTagsForPlacemark(placemarkId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: PlacemarkTagEntity)

    @Query("SELECT DISTINCT tag FROM placemark_tags ORDER BY tag ASC")
    fun tagsAlphabetical(): Flow<List<String>>

    @Query(
        """
        SELECT DISTINCT p.* FROM placemarks p
        INNER JOIN placemark_tags t ON p.id = t.placemarkId
        WHERE t.tag = :tag
        ORDER BY p.name ASC
        """
    )
    fun placemarksWithTag(tag: String): Flow<List<PlacemarkEntity>>
}

@Database(
    entities = [PlacemarkEntity::class, PlacemarkTagEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CampusDatabase : RoomDatabase() {
    abstract fun placemarkDao(): PlacemarkDao
}
