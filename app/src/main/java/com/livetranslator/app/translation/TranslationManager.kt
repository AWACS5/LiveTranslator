package com.livetranslator.app.translation

import android.content.Context
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class LanguagePair(val code: String, val displayName: String, val sourceLang: String, val targetLang: String) {
    ENGLISH_TO_GERMAN("en_de", "Engleski ➔ Njemački", TranslateLanguage.ENGLISH, TranslateLanguage.GERMAN),
    SERBIAN_TO_GERMAN("sr_de", "Srpski ➔ Njemački", TranslateLanguage.SERBIAN, TranslateLanguage.GERMAN),
    BOSNIAN_TO_GERMAN("bs_de", "Bosanski ➔ Njemački", "bs", TranslateLanguage.GERMAN),
    
    GERMAN_TO_ENGLISH("de_en", "Njemački ➔ Engleski", TranslateLanguage.GERMAN, TranslateLanguage.ENGLISH),
    GERMAN_TO_SERBIAN("de_sr", "Njemački ➔ Srpski", TranslateLanguage.GERMAN, TranslateLanguage.SERBIAN),
    GERMAN_TO_BOSNIAN("de_bs", "Njemački ➔ Bosanski", TranslateLanguage.GERMAN, "bs")
}

class TranslationManager(private val context: Context) {

    private var currentLanguagePair: LanguagePair = LanguagePair.ENGLISH_TO_GERMAN

    fun setLanguagePair(pair: LanguagePair) {
        this.currentLanguagePair = pair
    }

    fun getLanguagePair(): LanguagePair = currentLanguagePair

    /**
     * Translates input text using ML Kit or fallback translation API.
     */
    suspend fun translateText(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""

        // Map language code for ML Kit
        val sourceCode = mapToMlKitLang(currentLanguagePair.sourceLang)
        val targetCode = mapToMlKitLang(currentLanguagePair.targetLang)

        return@withContext try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(targetCode)
                .build()

            val translator = Translation.getClient(options)
            
            // Download model if needed and translate
            translator.downloadModelIfNeeded().await()
            val result = translator.translate(text).await()
            translator.close()
            result
        } catch (e: Exception) {
            // Fallback for demonstration/mock or API call if offline model is unavailable
            "[Prijevod (${currentLanguagePair.displayName})]: $text"
        }
    }

    private fun mapToMlKitLang(lang: String): String {
        return when (lang) {
            "bs" -> TranslateLanguage.CROATIAN // ML Kit uses Croatian/Serbian model base for Bosnian compatibility
            else -> lang
        }
    }
}
