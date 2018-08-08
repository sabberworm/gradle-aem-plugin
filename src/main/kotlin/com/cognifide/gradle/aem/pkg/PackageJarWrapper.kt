package com.cognifide.gradle.aem.pkg

import aQute.bnd.osgi.Jar
import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.base.vlt.VltFilter
import com.cognifide.gradle.aem.internal.file.FileOperations
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.util.GFileUtils
import org.zeroturnaround.zip.ZipUtil
import java.io.File

class PackageJarWrapper(val project: Project, val jar: File) {

    private val config = AemConfig.of(project)

    private val logger = project.logger

    var pkgDir: File = jar.parentFile

    var pkgPath: String = "/apps/gap/jar-wrapper/install/${jar.name}"

    var overrideProps: (Jar) -> Map<String, Any> = { mapOf() }

    fun wrap(): File {
        val pkgName = jar.nameWithoutExtension
        val pkg = File(pkgDir, "$pkgName.zip")
        if (pkg.exists()) {
            logger.info("CRX package wrapping OSGi bundle already exists: $pkg")
            return pkg
        }

        logger.info("Wrapping OSGi bundle to CRX package: $jar")

        val pkgRoot = File(pkgDir, pkgName)

        val vaultDir = File(pkgRoot, PackagePlugin.VLT_PATH)

        // Copy package template files
        GFileUtils.mkdirs(vaultDir)
        FileOperations.copyResources(PackagePlugin.VLT_PATH, vaultDir)

        // Expand package properties
        val bundle = Jar(jar)
        val description = bundle.manifest.mainAttributes.getValue("Bundle-Description") ?: ""
        val symbolicName = bundle.manifest.mainAttributes.getValue("Bundle-SymbolicName")
        val group = symbolicName.substringBeforeLast(".")
        val version = bundle.manifest.mainAttributes.getValue("Bundle-Version")
        val filters = listOf(VltFilter.rootElementForPath(pkgPath))
        val bundleProps = mapOf<String, Any>(
                "project.group" to group,
                "project.name" to symbolicName,
                "project.version" to version,
                "project.description" to description,
                "config.packageName" to symbolicName,
                "filters" to filters,
                "filterRoots" to filters.joinToString(config.vaultLineSeparatorString) { it.toString() }
        )
        val generalProps = config.props.packageProps
        val effectiveProps = generalProps + bundleProps + overrideProps(bundle)

        FileOperations.amendFiles(vaultDir, listOf("**/${PackagePlugin.VLT_PATH}/*.xml")) { file, content ->
            config.props.expand(content, effectiveProps, file.absolutePath)
        }

        // Copy bundle to install path
        val pkgJar = File(pkgRoot, "jcr_root$pkgPath")

        GFileUtils.mkdirs(pkgJar.parentFile)
        FileUtils.copyFile(jar, pkgJar)

        // ZIP all to CRX package
        ZipUtil.pack(pkgRoot, pkg)
        pkgRoot.deleteRecursively()

        return pkg
    }

}