package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.tasks.TaskAction

open class ActivateTask : SyncTask() {

    companion object {
        val NAME = "aemActivate"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Activates AEM package on instance(s)."
    }

    @TaskAction
    fun activate() {
        synchronize({ sync ->
            activatePackage(determineRemotePackagePath(sync), sync)
        })
    }

}
