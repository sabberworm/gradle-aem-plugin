package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class EnvironmentUp : AemDefaultTask() {

    init {
        description = "Turns on AEM virtualized environment (e.g HTTPD service with AEM dispatcher module)"
    }

    @TaskAction
    fun up() {
        if (aem.environment.up) {
            aem.notifier.notify("Environment up", "Cannot turn on as it is already up")
            return
        }

        aem.environment.up()

        aem.notifier.notify("Environment up", "Turned on with success")
    }

    companion object {
        const val NAME = "environmentUp"
    }
}
