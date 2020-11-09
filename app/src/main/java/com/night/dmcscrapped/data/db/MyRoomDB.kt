package com.night.dmcscrapped.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.night.dmcscrapped.data.model.*

@Database(entities = [
    BDevice::class,
    ROptionItem::class,
    OptDep::class,
    SnList::class,
    PlateInfo::class,
    Station::class,
    DmcScrappedRecord::class,
//    ScanInRecord::class,
    MyLog::class
    ],exportSchema = false,version = 1
)
abstract class MyRoomDB : RoomDatabase() {

    abstract fun dao() : Dao

    private class DBCallback : RoomDatabase.Callback(){
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Log.d("@@@DB:", "onCreate")
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            Log.d("@@@DB:", "onOpen")
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            Log.d("@@@DB", "onDestructiveMigration")
        }
    }

    companion object{
        @Volatile
        var INSTANCE : MyRoomDB? = null
        fun getDatabase(context: Context) : MyRoomDB {
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context,MyRoomDB::class.java,"SingleReportDB")
                    .addCallback(DBCallback())
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}