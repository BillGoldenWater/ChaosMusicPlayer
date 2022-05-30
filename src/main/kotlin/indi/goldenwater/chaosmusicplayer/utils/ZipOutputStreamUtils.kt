package indi.goldenwater.chaosmusicplayer.utils

import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun ZipOutputStream.writeFile(file: File, rootDirectory: File) {
    this.putNextEntry(ZipEntry(file.relativeTo(rootDirectory).path))

    val fis = FileInputStream(file)
    this.write(fis.readAllBytes())

    this.closeEntry()
}

fun ZipOutputStream.writeDirectory(directory: File, rootDirectory: File) {
    directory
        .listFiles()
        ?.filter { it.name != ".DS_Store" }
        ?.let { subFiles ->
            if (subFiles.isNotEmpty()) {
                subFiles.forEach {
                    if (it.isDirectory) this.writeDirectory(it, rootDirectory)
                    else this.writeFile(it, rootDirectory)
                }
            } else {
                directory
                    .relativeTo(rootDirectory)
                    .let {
                        if (it.name != "") {
                            this.putNextEntry(ZipEntry(it.path + File.separator))
                            this.closeEntry()
                        }
                    }
            }
        }
}