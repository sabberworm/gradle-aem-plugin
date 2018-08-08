package com.cognifide.gradle.aem.osgi

import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Deactivate
import org.slf4j.LoggerFactory

@Component(immediate = true, service = [PackageBuilder::class])
class PackageBuilder {

    @Activate
    protected fun activate() {
        LOG.info("Package builder deployed by GAP is running...")
    }

    @Deactivate
    protected fun deactivate() {
        LOG.info("Package builder deployed by GAP is stopping...")
    }

    companion object {
        val LOG = LoggerFactory.getLogger(PackageBuilder::class.java)
    }

}