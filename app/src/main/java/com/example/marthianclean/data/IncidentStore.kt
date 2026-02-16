package com.example.marthianclean.data

import android.content.Context
import android.net.Uri
import com.example.marthianclean.model.Incident
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object IncidentStore {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private fun file(context: Context): File =
        File(context.filesDir, "incidents.json")

    suspend fun loadAll(context: Context): List<Incident> = withContext(Dispatchers.IO) {
        val f = file(context)
        if (!f.exists()) return@withContext emptyList()

        runCatching {
            val text = f.readText(Charsets.UTF_8)
            if (text.isBlank()) return@runCatching emptyList()

            val type = object : TypeToken<List<Incident>>() {}.type
            gson.fromJson<List<Incident>>(text, type) ?: emptyList()
        }.getOrElse { emptyList() }
    }

    private suspend fun saveAll(context: Context, list: List<Incident>) = withContext(Dispatchers.IO) {
        val f = file(context)
        f.writeText(gson.toJson(list), Charsets.UTF_8)
    }

    suspend fun upsert(context: Context, incident: Incident) {
        val cur = loadAll(context).toMutableList()
        val idx = cur.indexOfFirst { it.id == incident.id }
        if (idx >= 0) cur[idx] = incident else cur.add(0, incident) // 최신 위
        saveAll(context, cur)
    }

    suspend fun deleteMany(context: Context, ids: List<String>) {
        val next = loadAll(context).filterNot { ids.contains(it.id) }
        saveAll(context, next)
    }

    suspend fun exportToUri(context: Context, uri: Uri, incidents: List<Incident>) =
        withContext(Dispatchers.IO) {
            val content = gson.toJson(incidents)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(content.toByteArray(Charsets.UTF_8))
            } ?: error("OutputStream open failed")
        }
}
