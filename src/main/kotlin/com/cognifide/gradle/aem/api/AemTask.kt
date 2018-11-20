package com.cognifide.gradle.aem.api

import com.cognifide.gradle.aem.base.BaseExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.util.GFileUtils
import java.io.File

interface AemTask : Task {

    val aem: BaseExtension

    fun projectEvaluated() {
        // intentionally empty
    }

    fun projectsEvaluated() {
        // intentionally empty
    }

    fun taskGraphReady(graph: TaskExecutionGraph) {
        // intentionally empty
    }

    companion object {
        const val GROUP = "AEM"

        fun taskDir(project: Project, taskName: String): File {
            val dir = File(project.buildDir, "aem/$taskName")

            GFileUtils.mkdirs(dir)

            return dir
        }

        fun temporaryDir(project: Project, taskName: String, path: String): File {
            val dir = File(project.buildDir, "aem/$taskName/$path")

            GFileUtils.mkdirs(dir)

            return dir
        }

        fun temporaryFile(project: Project, taskName: String, name: String): File {
            val dir = File(project.buildDir, "aem/$taskName")

            GFileUtils.mkdirs(dir)

            return File(dir, name)
        }
    }

}