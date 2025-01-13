package com.example.musicapp.data.repository

import com.example.musicapp.data.local.ContentResolverHelper
import com.example.musicapp.data.local.model.Audio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AudioRepository @Inject constructor(
    private val contentResolverHelper: ContentResolverHelper
) {

    suspend fun getAudioData() : List<Audio> = withContext(Dispatchers.IO){
        contentResolverHelper.getAudioData()
    }
}