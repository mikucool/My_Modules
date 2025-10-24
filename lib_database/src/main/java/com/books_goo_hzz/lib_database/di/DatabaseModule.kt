package com.books_goo_hzz.lib_database.di

import android.content.Context
import androidx.room.Room
import com.books_goo_hzz.lib_database.AppDatabase
import com.books_goo_hzz.lib_database.dao.slot.SlotResultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "my_modules_database"
        )
        .createFromAsset("slot_results.db") // 从assets目录加载预生成的数据库
        .build()
    }

    @Provides
    @Singleton
    fun provideSlotResultDao(database: AppDatabase): SlotResultDao {
        return database.slotResultDao()
    }
}
