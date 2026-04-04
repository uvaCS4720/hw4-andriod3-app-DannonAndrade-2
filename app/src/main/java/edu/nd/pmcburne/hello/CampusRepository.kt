package edu.nd.pmcburne.hello

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class CampusRepository(context: Context) {

    private val database: CampusDatabase = Room.databaseBuilder(
        context.applicationContext,
        CampusDatabase::class.java,
        "campus_data.db"
    ).build()

    private val placemarkDao = database.placemarkDao()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun getTags(): Flow<List<String>> = placemarkDao.tagsAlphabetical()

    fun getPlacemarks(tag: String): Flow<List<PlacemarkEntity>> =
        placemarkDao.placemarksWithTag(tag)

    suspend fun syncFromApi() = withContext(Dispatchers.IO) {
        val url = "https://www.cs.virginia.edu/~wxt4gm/placemarks.json"
        val request = Request.Builder().url(url).build()
        
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext
                
                val jsonString = response.body?.string() ?: return@withContext
                val jsonArray = JSONArray(jsonString)
                
                database.withTransaction {
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val id = item.getInt("id")
                        val name = item.getString("name")
                        val description = item.getString("description")
                        
                        val center = item.getJSONObject("visual_center")
                        val lat = center.getDouble("latitude")
                        val lng = center.getDouble("longitude")

                        placemarkDao.insertPlacemark(
                            PlacemarkEntity(
                                id = id,
                                name = name,
                                description = description,
                                latitude = lat,
                                longitude = lng
                            )
                        )
                        
                        // Refresh tags for this placemark
                        placemarkDao.deleteTagsForPlacemark(id)
                        val tagsArray = item.getJSONArray("tag_list")
                        for (j in 0 until tagsArray.length()) {
                            val tagValue = tagsArray.getString(j)
                            placemarkDao.insertTag(
                                PlacemarkTagEntity(placemarkId = id, tag = tagValue)
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
