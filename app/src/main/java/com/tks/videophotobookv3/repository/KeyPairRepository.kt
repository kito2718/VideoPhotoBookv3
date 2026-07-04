package com.tks.videophotobookv3.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tks.videophotobookv3.model.ArKeyPair

class KeyPairRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ar_key_pairs_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "ar_key_pairs_list"

    fun getPairs(): List<ArKeyPair> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ArKeyPair>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun savePairs(pairs: List<ArKeyPair>) {
        val json = gson.toJson(pairs)
        prefs.edit().putString(key, json).apply()
    }

    fun addPair(pair: ArKeyPair) {
        val current = getPairs().toMutableList()
        current.add(pair)
        savePairs(current)
    }

    fun deletePair(id: String) {
        val current = getPairs().filter { it.id != id }
        savePairs(current)
    }
}
