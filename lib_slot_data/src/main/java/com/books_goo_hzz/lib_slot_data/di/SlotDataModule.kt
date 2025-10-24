package com.books_goo_hzz.lib_slot_data.di

import com.books_goo_hzz.lib_slot_core.SlotRepository
import com.books_goo_hzz.lib_slot_data.SlotRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SlotDataModule {

    @Binds
    @Singleton
    abstract fun bindSlotRepository(impl: SlotRepositoryImpl): SlotRepository
}
