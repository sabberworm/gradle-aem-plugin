package com.cognifide.gradle.aem.instance.satisfy

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.internal.file.resolver.FileResolution
import com.cognifide.gradle.aem.pkg.PackageJarWrapper
import org.apache.commons.io.FilenameUtils
import java.io.File

class PackageResolution(group: PackageGroup, id: String, action: (FileResolution) -> File) : FileResolution(group, id, action) {

    val config = AemConfig.of(group.resolver.project)

    val logger = group.resolver.project.logger

    override fun process(file: File): File {
        val origin = super.process(file)

        return when (FilenameUtils.getExtension(file.name)) {
            "jar" -> wrap(origin)
            "zip" -> origin
            else -> throw PackageException("File $origin must have *.jar or *.zip extension")
        }
    }

    private fun wrap(jar: File): File {
        val wrapper = PackageJarWrapper(group.resolver.project, jar)
        wrapper.pkgDir = dir
        wrapper.pkgPath = "${config.satisfyBundlePath}/${jar.name}"
        wrapper.overrideProps = { config.satisfyBundleProperties(it) }

        return wrapper.wrap()
    }
}