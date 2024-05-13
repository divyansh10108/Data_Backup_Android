package com.databackup.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data")
data class Data(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fileName: String,
    val count: Int
)