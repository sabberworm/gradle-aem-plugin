package com.cognifide.gradle.aem.pkg.tasks.compose

import com.cognifide.gradle.aem.bundle.tasks.BundleCompose
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import org.gradle.api.tasks.TaskProvider

class BundleInstalledBuilt(target: PackageCompose, private val task: TaskProvider<BundleCompose>) : BundleInstalled {

    private val aem = target.aem

    override val file = aem.obj.file { fileProvider(aem.obj.provider { task.get().archiveFile.get().asFile }) }

    override val dirPath = aem.obj.string { convention(task.flatMap { t -> t.installPath }) }

    override val fileName = aem.obj.string { convention(task.flatMap { t -> t.archiveFileName }) }

    override val vaultFilter = aem.obj.boolean { convention(task.flatMap { t -> t.vaultFilter }) }
}
