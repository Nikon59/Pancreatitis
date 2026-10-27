package com.pancreatitis.infrastructure.storage

import android.content.Context
import com.pancreatitis.core.storage.StoragePathProvider
import java.io.File
import java.nio.file.Path

class AndroidStoragePathProvider(private val context: Context) : StoragePathProvider {

    private val baseDir: Path by lazy {
        val dir = File(context.filesDir, "pancreatitis")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir.toPath()
    }

    override fun getBaseDataDirectory(): Path = baseDir

    override fun getModelsDirectory(): Path {
        val dir = baseDir.resolve("models").toFile()
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.toPath()
    }

    override fun getDatabasePath(): Path {
        val dbFile = context.getDatabasePath("mobile.db")
        val parent = dbFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        return dbFile.toPath()
    }

    override fun getKeysDirectory(): Path {
        val dir = baseDir.resolve("keys").toFile()
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.toPath()
    }

    override fun getTempDirectory(): Path {
        val dir = File(context.cacheDir, "temp")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.toPath()
    }
}
