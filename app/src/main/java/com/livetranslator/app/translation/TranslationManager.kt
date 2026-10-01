package com.livetranslator.app.translation

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class LanguagePair(val code: String, val displayName: String, val sourceLang: String, val targetLang: String) {
    ENGLISH_TO_GERMAN("en_de", "Engleski ➔ Njemački", TranslateLanguage.ENGLISH, TranslateLanguage.GERMAN),
    SERBIAN_TO_GERMAN("sr_de", "Srpski ➔ Njemački", TranslateLanguage.CROATIAN, TranslateLanguage.GERMAN),
    BOSNIAN_TO_GERMAN("bs_de", "Bosanski ➔ Njemački", TranslateLanguage.CROATIAN, TranslateLanguage.GERMAN),
    
    GERMAN_TO_ENGLISH("de_en", "Njemački ➔ Engleski", TranslateLanguage.GERMAN, TranslateLanguage.ENGLISH),
    GERMAN_TO_SERBIAN("de_sr", "Njemački ➔ Srpski", TranslateLanguage.GERMAN, TranslateLanguage.CROATIAN),
    GERMAN_TO_BOSNIAN("de_bs", "Njemački ➔ Bosanski", TranslateLanguage.GERMAN, TranslateLanguage.CROATIAN)
}

class TranslationManager(private val context: Context) {

    private var currentLanguagePair: LanguagePair = LanguagePair.ENGLISH_TO_GERMAN

    fun setLanguagePair(pair: LanguagePair) {
        this.currentLanguagePair = pair
    }

    fun getLanguagePair(): LanguagePair = currentLanguagePair

    suspend fun translateText(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""

        return@withContext try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(currentLanguagePair.sourceLang)
                .setTargetLanguage(currentLanguagePair.targetLang)
                .build()

            val translator = Translation.getClient(options)

            Tasks.await(translator.downloadModelIfNeeded())
            val result = Tasks.await(translator.translate(text))

            translator.close()
            result
        } catch (e: Exception) {
            "[Prijevod (${currentLanguagePair.displayName})]: $text"
        }
    }
}
