package com.dewa.filemanager.data.repository

import com.dewa.filemanager.data.model.PasswordEntry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class PasswordRepository(
    private val fileRepo: FileManagerRepository = FileManagerRepository()
) {

    private fun passwordFile(): File {
        fileRepo.ensureAppDirectories()
        val folder = File(fileRepo.getPasswordPath())
        if (!folder.exists()) folder.mkdirs()
        val file = File(folder, "passwords.json")
        if (!file.exists()) file.writeText("[]")
        return file
    }

    fun loadAll(): List<PasswordEntry> {
        return try {
            val arr = JSONArray(passwordFile().readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(
                        PasswordEntry(
                            id = o.optString("id"),
                            name = o.optString("name"),
                            contact = o.optString("contact"),
                            password = o.optString("password"),
                            createdAt = o.optLong("created_at", System.currentTimeMillis())
                        )
                    )
                }
            }.sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(name: String, contact: String, password: String): Boolean {
        return try {
            val file = passwordFile()
            val arr = JSONArray(file.readText())
            val item = JSONObject().apply {
                put("id", UUID.randomUUID().toString())
                put("name", name)
                put("contact", contact)
                put("password", password)
                put("created_at", System.currentTimeMillis())
            }
            arr.put(item)
            file.writeText(arr.toString(2))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun update(id: String, name: String, contact: String, password: String): Boolean {
        return try {
            val file = passwordFile()
            val arr = JSONArray(file.readText())
            var found = false
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                if (o.optString("id") == id) {
                    o.put("name", name)
                    o.put("contact", contact)
                    o.put("password", password)
                    found = true
                    break
                }
            }
            if (!found) return false
            file.writeText(arr.toString(2))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun delete(id: String): Boolean {
        return try {
            val file = passwordFile()
            val arr = JSONArray(file.readText())
            val out = JSONArray()
            var deleted = false
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                if (o.optString("id") == id) {
                    deleted = true
                } else {
                    out.put(o)
                }
            }
            if (!deleted) return false
            file.writeText(out.toString(2))
            true
        } catch (_: Exception) {
            false
        }
    }
}
