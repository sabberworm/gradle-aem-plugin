package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.ProgressLogger
import com.cognifide.gradle.aem.pkg.deploy.DeployException
import com.cognifide.gradle.aem.pkg.deploy.SyncTask
import org.gradle.api.tasks.TaskAction

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
        val path = "/logs/error.log"
        val interval = 2000L
        var tryCount = 0

        synchronizeInstance({ sync ->
            val progress = ProgressLogger(project, "Tailing logs at $instance")
            progress.started()

            Behaviors.loopForever(interval, {
                val timestamp = System.currentTimeMillis()

                try {
                    if (tryCount > 30) {
                        throw AemException("Instance cannot be tailed, because it is not responding.")
                    }

                    val lines = sync.get("${sync.instance.httpUrl}/system/console/slinglog/tailer.txt?_dc=$timestamp&tail=$lineCount&name=$path")
                    progress.progress(lines)
                    tryCount = 0
                } catch (e: DeployException) {
                    tryCount++
                }
            })

            progress.completed()
        }, instance)
    }
}