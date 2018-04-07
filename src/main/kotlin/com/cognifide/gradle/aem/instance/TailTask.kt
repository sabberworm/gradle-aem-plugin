package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.ProgressLogger
import com.cognifide.gradle.aem.pkg.deploy.DeployException
import com.cognifide.gradle.aem.pkg.deploy.SyncTask
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GFileUtils

open class TailTask : SyncTask() {

    companion object {
        val NAME = "aemTail"
    }

    init {
        description = "Monitor logs of AEM instance."
    }

    @TaskAction
    fun tail() {
        val instanceName = propertyParser.string("aem.tail.instance.name",
                config.instances.values.first().name)
        val instance = Instance.byName(project, instanceName)

        val lineCount = 400
        val remotePath = "logs/error.log"
        val localFile = AemTask.temporaryFile(project, NAME, remotePath)
        val interval = 2000L
        var tryCount = 0

        var errors = 0
        var warns = 0
        var message = ""

        logger.lifecycle("Writing to local file: $localFile")
        logger.lifecycle("View it interactively using command: 'tail -f $localFile'")

        // TODO use aem.instance.name (remove aem.deploy prefix), proxy all instances in parallel
        synchronizeInstance({ sync ->
            val progress = ProgressLogger(project, "Reading remote file '$remotePath' at '$instance'")
            progress.started()

            GFileUtils.mkdirs(localFile.parentFile)
            localFile.printWriter().use { printer ->
                Behaviors.loopForever(interval, {
                    val timestamp = System.currentTimeMillis()

                    try {
                        if (tryCount > 30) {
                            throw AemException("Instance cannot be monitored, because it is not responding.")
                        }

                        // TODO amend lines, eliminate duplicates
                        // TODO make progress logger calculation configurable
                        val lines = sync.get("${sync.instance.httpUrl}/system/console/slinglog/tailer.txt?_dc=$timestamp&tail=$lineCount&name=/$remotePath")
                        lines.splitToSequence("\r\n").forEach { line ->
                            if (line.contains("*WARN*")) {
                                warns++
                                message = line.trim()
                            }
                            if (line.contains("*ERROR*")) {
                                errors++
                                message = line.trim()
                            }
                        }

                        progress.progress("$errors error(s), $warns warning(s) | $message")
                        printer.print(lines)

                        tryCount = 0
                    } catch (e: DeployException) {
                        tryCount++
                    }
                })
            }

            progress.completed()
        }, instance)
    }
}