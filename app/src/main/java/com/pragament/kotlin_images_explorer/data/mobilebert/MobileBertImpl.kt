package com.pragament.kotlin_images_explorer.data.mobilebert

import android.content.Context
import com.pragament.kotlin_images_explorer.domain.mobilebertmodel.MobileBertHelper
import org.tensorflow.lite.task.text.qa.BertQuestionAnswerer

class MobileBertImpl(private val context : Context) : MobileBertHelper {
    override var questionAnswerer: BertQuestionAnswerer? = null
       init {
           questionAnswerer = BertQuestionAnswerer.createFromFileAndOptions(
              context , "mobilebert.tflite" , BertQuestionAnswerer.BertQuestionAnswererOptions.builder().build()
           )
       }
   override  suspend  fun askQuestion(contextText: String, question: String): String {
       val answer = questionAnswerer?.answer(contextText , question)
       return answer?.firstOrNull()?.text ?: "No answer found"
    }
}