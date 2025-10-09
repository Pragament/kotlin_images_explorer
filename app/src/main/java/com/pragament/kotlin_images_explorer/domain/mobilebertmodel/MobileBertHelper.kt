package com.pragament.kotlin_images_explorer.domain.mobilebertmodel

import android.content.Context
import org.tensorflow.lite.task.text.qa.BertQuestionAnswerer

 interface MobileBertHelper {
    var questionAnswerer : BertQuestionAnswerer?
    suspend fun askQuestion(contextText : String , question : String) : String
}
