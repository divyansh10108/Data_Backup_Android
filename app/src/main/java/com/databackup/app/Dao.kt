package com.databackup.app
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface Dao {
    @Upsert
    suspend fun insert(data: Data)

    @Query("UPDATE data SET count = :newCount WHERE fileName = :fileName")
    suspend fun update(fileName: String, newCount: Int)

    @Query("SELECT count FROM Data WHERE fileName = :fileName")
    suspend fun get(fileName: String): Int?

    @Query("SELECT * FROM Data")
    suspend fun getAll(): List<Data>

    @Query("SELECT * FROM Data ORDER BY count DESC LIMIT :limit")
    suspend fun getTopFiles(limit: Int): List<Data>
}
