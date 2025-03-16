package com.pragament.kotlin_images_explorer.di

import android.content.ContentResolver
import androidx.room.Room
import com.pragament.kotlin_images_explorer.data.local.ImageDatabase
import com.pragament.kotlin_images_explorer.data.local.SettingsDataStore
import com.pragament.kotlin_images_explorer.data.repository.ImageRepositoryImpl
import com.pragament.kotlin_images_explorer.domain.repository.ImageRepository
import com.pragament.kotlin_images_explorer.presentation.viewmodel.HomeViewModel
import com.pragament.kotlin_images_explorer.presentation.viewmodel.SettingsViewModel
import com.pragament.kotlin_images_explorer.presentation.viewmodel.TaggedImagesListViewModel
import com.pragament.kotlin_images_explorer.presentation.viewmodel.TaggedImagesViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            ImageDatabase::class.java,
            "images.db"
        ).build()
    }

    single { get<ImageDatabase>().imageDao }

    single<ContentResolver> {
        androidContext().contentResolver
    }

    single<ImageRepository> {
        ImageRepositoryImpl(
            context = androidContext(),
            contentResolver = get(),
            imageDao = get()
        )
    }

    single { SettingsDataStore(androidContext()) }

    viewModelOf(::HomeViewModel)
    viewModelOf(::TaggedImagesListViewModel)
    viewModel {
        TaggedImagesViewModel(
            repository = get(),
            savedStateHandle = get()
        )
    }
    viewModelOf(::SettingsViewModel)
}
