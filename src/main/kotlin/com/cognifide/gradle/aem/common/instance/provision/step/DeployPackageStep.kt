package com.cognifide.gradle.aem.common.instance.provision.step

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.provision.ProvisionException
import com.cognifide.gradle.aem.common.instance.provision.Provisioner
import com.cognifide.gradle.common.utils.toLowerCamelCase

class DeployPackageStep(provisioner: Provisioner) : AbstractStep(provisioner) {

    val source = aem.obj.typed<Any>()

    val sourceProperties by lazy { DeployPackageSource.from(source.get(), aem) }

    val file by lazy {
        val sourceFile = provisioner.fileResolver.get(source.get()).file
        aem.packageOptions.wrapper.wrap(sourceFile).also { common.checksumFile(it) }
    }

    val name = aem.obj.string { convention(aem.obj.provider { sourceProperties.name }) }

    override fun validate() {
        if (!source.isPresent) {
            throw ProvisionException("Deploy package source is not defined!")
        }
    }

    override fun init() {
        logger.debug("Resolved package '${name.get()}' to be deployed is located at path: '$file'")
    }

    override fun action(instance: Instance) = instance.sync {
        logger.info("Deploying package '${name.get()}' to $instance")
        awaitIf { packageManager.deploy(file) }
    }

    fun isDeployedOn(instance: Instance) = instance.sync.packageManager.isDeployed(file)

    fun notDeployedOn(instance: Instance) = !isDeployedOn(instance)

    init {
        id.convention(name.map { "deployPackage/${it.toLowerCamelCase()}" })
        description.convention(name.map { "Deploying package '${name.get()}'" })
        version.convention(aem.obj.provider { sourceProperties.version })

        if (aem.prop.boolean("instance.provision.deployPackage.strict") == true) {
            condition { notDeployedOn(instance) }
        }
    }
}
