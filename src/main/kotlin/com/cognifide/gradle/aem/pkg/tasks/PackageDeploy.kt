package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.checkAvailable
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class PackageDeploy : PackageTask() {

    /**
     * Check instance(s) health after deploying package(s).
     */
    @Input
    var awaited: Boolean = aem.props.boolean("package.deploy.awaited") ?: true

    /**
     * Enables deployment via CRX package activation from author to publishers when e.g they are not accessible.
     */
    @Input
    var distributed: Boolean = aem.props.flag("package.deploy.distributed")

    /**
     * Force upload CRX package regardless if it was previously uploaded.
     */
    @Input
    var uploadForce: Boolean = aem.props.boolean("package.deploy.uploadForce") ?: true

    /**
     * Repeat upload when failed (brute-forcing).
     */
    @Internal
    @get:JsonIgnore
    var uploadRetry = aem.retry { afterSquaredSecond(aem.props.long("package.deploy.uploadRetry") ?: 3) }

    /**
     * Repeat install when failed (brute-forcing).
     */
    @Internal
    @get:JsonIgnore
    var installRetry = aem.retry { afterSquaredSecond(aem.props.long("package.deploy.installRetry") ?: 2) }

    /**
     * Determines if when on package install, sub-packages included in CRX package content should be also installed.
     */
    @Input
    var installRecursive: Boolean = aem.props.boolean("package.deploy.installRecursive") ?: true

    /**
     * Allows to temporarily enable or disable workflows during CRX package deployment.
     */
    @Input
    var workflowToggle: Map<String, Boolean> = aem.props.map("package.deploy.workflowToggle")
            ?.mapValues { it.value.toBoolean() } ?: mapOf()

    /**
     * Hook for preparing instance before deploying packages
     */
    @Internal
    @get:JsonIgnore
    var initializer: InstanceSync.() -> Unit = {}

    /**
     * Hook for cleaning instance after deploying packages
     */
    @Internal
    @get:JsonIgnore
    var finalizer: InstanceSync.() -> Unit = {}

    /**
     * Hook after deploying all packages to all instances.
     */
    @Internal
    @get:JsonIgnore
    var completer: () -> Unit = { awaitUp() }

    private var awaitUpOptions: AwaitUpAction.() -> Unit = {}

    /**
     * Controls await up action.
     */
    fun awaitUp(options: AwaitUpAction.() -> Unit) {
        this.awaitUpOptions = options
    }

    fun awaitUp() {
        if (awaited) {
            aem.instanceActions.awaitUp {
                instances = this@PackageDeploy.instances
                awaitUpOptions()
            }
        }
    }

    override fun projectsEvaluated() {
        if (instances.isEmpty()) {
            instances = if (distributed) {
                aem.authorInstances
            } else {
                aem.instances
            }
        }

        if (packages.isEmpty()) {
            packages = aem.dependentPackages(this)
        }
    }

    @TaskAction
    open fun deploy() {
        instances.checkAvailable()

        aem.progress(instances.size * packages.size) {
            aem.syncFiles(instances, packages) { pkg ->
                increment("Deploying package '${pkg.name}' to instance '${instance.name}'") {
                    initializer()
                    workflowManager.toggleTemporarily(workflowToggle) {
                        packageManager.deploy(pkg, uploadForce, uploadRetry, installRecursive, installRetry, distributed)
                    }
                    finalizer()
                }
            }
        }

        completer()

        aem.notifier.notify("Package deployed", "${packages.fileNames} on ${instances.names}")
    }

    init {
        description = "Deploys CRX package on instance(s). Upload then install (and optionally activate)."
    }

    companion object {
        const val NAME = "packageDeploy"
    }
}
