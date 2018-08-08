package com.cognifide.gradle.aem.base.copy

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.instance.InstanceSync
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class CopyTask : AemDefaultTask() {

    companion object {
        val NAME = "aemCopy"
    }

    init {
        description = "Copy built package from one instance to another(s)"

        outputs.upToDateWhen { false }
    }

    @get:Internal
    val sourceSync by lazy {
        val instance = config.parseInstance(project.properties["aem.copy.source.instance"] as String?
                ?: throw AemException("RCP param '-Paem.copy.source.instance' is not specified."))
        InstanceSync(project, instance)
    }

    @get:Internal
    val targetSync by lazy {
        val instance = config.parseInstance(project.properties["aem.copy.target.instance"] as String?
                ?: throw AemException("RCP param '-Paem.copy.target.instance' is not specified."))
        InstanceSync(project, instance)
    }

    @TaskAction
    fun copy() {
        deploySelf()

        // TODO ...
        // downloadPackage could read a built zip when it is being generated on instance
        // just execute: public VaultPackage assemble(final Session s, final ExportOptions opts, File file) with no storing it on repository (duplicating)

        // to sth like this: https://stackoverflow.com/a/14510582/3360007 multiplexoutputstream
        // boost will be to simultenous / multiplexed download and upload to multiple instances at same time
//        sourceSync.downloadPackage("/etc/designs/theme.zip", { out ->
//           targetSync.uploadPackage(out)
//
//        })
    }

    fun deploySelf() {
        listOf({ sourceSync.deploySelf() }, { targetSync.deploySelf() }).parallelStream().forEach { it() }
    }

}
