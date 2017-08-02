package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemInstancePlugin
import com.cognifide.gradle.aem.deploy.DeploySynchronizer
import com.cognifide.gradle.aem.internal.FileOperations
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.ProgressLogger
import com.cognifide.gradle.aem.internal.PropertyParser
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.internal.os.OperatingSystem
import org.gradle.util.GFileUtils
import org.zeroturnaround.zip.ZipUtil
import java.io.File

class LocalHandle(val project: Project, val sync: DeploySynchronizer) {

    companion object {
        val JAR_STATIC_FILES_PATH = "static/"
    }

    class Script(val file: File, val command: List<String>) {
        val commandLine: List<String>
            get() = command + listOf(file.absolutePath)
    }

    val instance = sync.instance

    val logger: Logger = project.logger

    val config = AemConfig.of(project)

    val dir = File("${config.instancesPath}/${instance.name}")

    val jar: File by lazy {
        FileOperations.find(dir, listOf("cq-quickstart*.jar")) ?: File(dir, "cq-quickstart.jar")
    }

    val lock = File(dir, "local-handle.lock")

    val staticDir = File(dir, "crx-quickstart")

    val license = File(dir, "license.properties")

    val startScript: Script
        get() {
            return if (OperatingSystem.current().isWindows) {
                Script(File(dir, "start.bat"), listOf("cmd", "/C"))
            } else {
                Script(File(dir, "start.sh"), listOf("sh"))
            }
        }

    val stopScript: Script
        get() {
            return if (OperatingSystem.current().isWindows) {
                Script(File(dir, "stop.bat"), listOf("cmd", "/C"))
            } else {
                Script(File(dir, "stop.sh"), listOf("sh"))
            }
        }

    fun create(files: List<File>) {
        cleanDir(true)

        logger.info("Creating instance at path '${dir.absolutePath}'")

        logger.info("Copying resolved instance files: ${files.map { it.absolutePath }}")
        GFileUtils.mkdirs(dir)
        files.forEach { FileUtils.copyFileToDirectory(it, dir) }

        logger.info("Extracting AEM static files from JAR")
        extractStaticFiles()

        logger.info("Correcting AEM static files")
        correctStaticFiles()

        logger.info("Creating default instance files")
        FileOperations.copyResources(AemInstancePlugin.FILES_PATH, dir, true)

        val filesDir = File(config.instanceFilesPath)

        logger.info("Overriding instance files using: ${filesDir.absolutePath}")
        if (filesDir.exists()) {
            FileUtils.copyDirectory(filesDir, dir)
        }

        logger.info("Expanding instance files")
        FileOperations.amendFiles(dir, config.instanceFilesExpanded, { source ->
            PropertyParser(project).expand(source, properties)
        })

        logger.info("Creating lock file")
        lock()

        logger.info("Created instance with success")
    }

    private fun correctStaticFiles() {
        if (OperatingSystem.current().isWindows) {
            FileOperations.amendFile(startScript.file, { source ->
                source.replace(
                        "start \"CQ\" cmd.exe /C java %CQ_JVM_OPTS% -jar %CurrDirName%\\%CQ_JARFILE% %START_OPTS%",
                        "start \"CQ\" cmd.exe /K java %CQ_JVM_OPTS% -jar %CurrDirName%\\%CQ_JARFILE% %START_OPTS%"
                )
            })
        }
    }

    fun extractStaticFiles() {
        val progressLogger = ProgressLogger(project, "Extracting static files from JAR: ${jar.absolutePath}")
        progressLogger.started()

        var total = 0
        ZipUtil.iterate(jar, { entry ->
            if (entry.name.startsWith(JAR_STATIC_FILES_PATH)) {
                total++
            }
        })

        var processed: Int = 0
        ZipUtil.unpack(jar, staticDir, { name ->
            if (name.startsWith(JAR_STATIC_FILES_PATH)) {
                progressLogger.progress("Extracting $processed/$total [${Formats.percent(processed, total)}]")
                processed++
                name.substring(JAR_STATIC_FILES_PATH.length)
            } else {
                name
            }
        })

        progressLogger.completed()
    }

    private fun cleanDir(create: Boolean) {
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        if (create) {
            dir.mkdirs()
        }
    }

    fun validate() {
        if (!jar.exists()) {
            throw AemException("Instance JAR file not found at path: ${jar.absolutePath}")
        }

        if (!license.exists()) {
            throw AemException("License file not found at path: ${license.absolutePath}")
        }
    }

    fun up() {
        execute(startScript)
    }

    fun down() {
        execute(stopScript)
    }

    private fun execute(script: Script) {
        ProcessBuilder(*script.commandLine.toTypedArray())
                .directory(dir)
                .start()
    }

    val properties: Map<String, Any>
        get() {
            return mapOf(
                    "instance" to sync.instance,
                    "instancePath" to dir.absolutePath,
                    "handle" to this
            )
        }

    fun destroy() {
        logger.info("Destroying instance at path '${dir.absolutePath}'")

        cleanDir(false)

        logger.info("Destroyed instance with success")
    }

    fun lock() {
        val metaJson = Formats.toJson(mapOf("locked" to Formats.dateISO8601()))
        lock.printWriter().use { it.print(metaJson) }
    }

    override fun toString(): String {
        return "AemLocalHandler(dir=${dir.absolutePath}, instance=$instance)"
    }

}