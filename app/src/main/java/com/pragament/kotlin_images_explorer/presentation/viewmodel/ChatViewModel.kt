package com.pragament.kotlin_images_explorer.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pragament.kotlin_images_explorer.data.local.SettingsDataStore
import com.pragament.kotlin_images_explorer.domain.mobilebertmodel.MobileBertHelper
import com.pragament.kotlin_images_explorer.domain.model.ImageInfo
import com.pragament.kotlin_images_explorer.domain.repository.ImageRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// sealed class ChatEvent {
//     data object StartImageProcessing : ChatEvent()
//     data object StopImageProcessing : ChatEvent()
//     data object ScanImage : ChatEvent()
//     data object TextGeneration : ChatEvent()
//     data object TextGenerationStopped : ChatEvent()
// }


class ChatViewModel(private val mobileBert : MobileBertHelper ,   private val repository: ImageRepository,
                    private val settingsDataStore: SettingsDataStore) : ViewModel() {
    private val _answer = MutableStateFlow("")
    val answer = _answer.asStateFlow()
    private var contextText by mutableStateOf("")
   var questionText by mutableStateOf("")
    private var tags = DetectedObject("", "", "" , "")

    val userChat = mutableStateListOf<UserRequest>()
    val modelChat = mutableStateListOf<String>()
    fun processImage(uris: List<String>){
  viewModelScope.launch {
      val selectedModel = settingsDataStore.selectedModel.first()
      uris.forEachIndexed { index, imageUri ->
          try {
              val imageId = System.currentTimeMillis() + index
              val image = ImageInfo(
                  id = imageId,
                  uri = imageUri,
                  displayName = imageUri.substringAfterLast('/'),
                  dateAdded = System.currentTimeMillis(),
                  extractedText = null,
                  label = null,
                  confidence = null,
                  modelName = null
              )

              repository.insertImage(image)
              // process it to extract text and generate tags
              val tags_ = repository.processImage(imageId, imageUri, selectedModel)
              if (tags_.isNotBlank()) {
                  Log.d("tag-", tags_)
                  // Clean up tags before saving
                  val cleanTags = tags_.split(Regex("\\s+"))
                      .map { word ->
                          word.trim()
                              .replace(Regex("[^a-zA-Z0-9]"), "") // Remove special characters
                              .replace(Regex("\\s+"), "") // Remove any spaces
                      }
                      .filter { it.isNotBlank() }
                      .joinToString(" ")
                  val objectTags = tags_.split(Regex("\\s+"))
                      .map { word ->
                          word.trim()
                              .replace(Regex("[^a-zA-Z0-9]"), "") // Remove special characters
                              .replace(Regex("\\s+"), "") // Remove any spaces
                      }
                      .filter { it.isNotBlank() }

                  println("DEBUG: Processing selected image ${image.displayName}, got tags: $cleanTags")
                  repository.updateImageText(imageId, cleanTags)
                 tags  = objectTags.convertToObject()
              } else {
                  println("DEBUG: No tags found for selected image ${image.displayName}")
              }
          } catch (e: Exception) {
              println("DEBUG: Error processing selected image $imageUri: ${e.message}")
          }
      }
  }
    }
    fun updateUserChat(userRequest: UserRequest){
     userChat.add(userRequest)
    }
    fun updateModelChat(text : String){
        modelChat.add(text)
    }
    fun getAnswer(question : String){
        Log.d("Tag_" , "I am running")
      viewModelScope.launch(Dispatchers.IO) {
          _answer.value = mobileBert.askQuestion(contextText , question)
          updateModelChat(_answer.value)
          Log.d("Tag_" , _answer.value)
      }
    }
    fun setQuestion(question : String){
        contextText = "The image most likely shows ${tags.classification}. This information was identified using the ${tags.modelName} model with ${tags.accuracy.removePrefix("0.")}% confidence. The image may also contains some text which says: \"${tags.text}\" "
        questionText = question
    }
}
fun List<String>.convertToObject() : DetectedObject{

    val newObject  = DetectedObject("" , "" ,"" ,"")
    var i = 1
    val findConf = this.indexOf("Confidence")
    val findText = this.indexOf("Text")
    newObject.classification = this.subList(1, findConf).joinToString(" ")
    newObject.accuracy = this[findConf + 1]
    newObject.modelName = this[findConf + 3]
    if (findText != this.size -1){
        newObject.text = this.subList(findText + 1, this.size).joinToString(" ")
    }
    return newObject
}

data class DetectedObject(var classification : String , var accuracy : String , var modelName : String , var text : String )
data class UserRequest(val imageUri : String , val text : String)