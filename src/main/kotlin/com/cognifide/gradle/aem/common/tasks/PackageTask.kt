package com.cognifide.gradle.aem.common.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.Instance
import java.io.File
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles

open class PackageTask : AemDefaultTask() {

    @Input
    var instances: List<Instance> = listOf()

    @InputFiles
    var packages: List<File> = listOf()

    override fun projectsEvaluated() {
        if (instances.isEmpty()) {
            instances = aem.instances
        }

        if (packages.isEmpty()) {
            packages = aem.dependentPackages(this)
        }
    }

    fun instance(urlOrName: String) {
        instances += aem.instance(urlOrName)
    }

    fun `package`(path: String) {
        packages += project.file(path)
    }

    fun pkg(path: String) = `package`(path)
}
