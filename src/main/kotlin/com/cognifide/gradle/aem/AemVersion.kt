package com.cognifide.gradle.aem

import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.Patterns
import com.cognifide.gradle.common.zip.ZipFile
import org.gradle.api.JavaVersion
import java.io.File

class AemVersion(val value: String) : Comparable<AemVersion> {

    private val base = Formats.asVersion(value)

    val version get() = base.version

    fun atLeast(other: AemVersion) = this >= other

    fun atLeast(other: String) = atLeast(AemVersion(other))

    fun atMost(other: AemVersion) = other <= this

    fun atMost(other: String) = atMost(AemVersion(other))

    fun inRange(range: String): Boolean = range.contains("-") && this in unclosedRange(range, "-")

    fun inRangeOrMatches(valueOrPattern: String) = inRange(valueOrPattern) || Patterns.wildcard(value, valueOrPattern)

    fun javaCompatibleVersions(compatibility: Map<String, String>): List<JavaVersion> {
        return compatibility.entries.mapNotNull { (aemVersionValue, javaVersionList) ->
            if (inRangeOrMatches(aemVersionValue)) javaVersionList.javaVersions("|") else null
        }.firstOrNull() ?: listOf()
    }

    /**
     * Indicates repository restructure performed in AEM 6.4.0 / preparations for making AEM available on cloud.
     *
     * After this changes, nodes under '/apps' or '/libs' are frozen and some features (like workflow manager)
     * requires to copy these nodes under '/var' by plugin (or AEM itself).
     *
     * @see <https://docs.adobe.com/content/help/en/experience-manager-64/deploying/restructuring/repository-restructuring.html>
     */
    val frozen get() = atLeast(VERSION_6_4_0)

    /**
     * Cloud manager version contains time in the end
     */
    val cloud get() = Patterns.wildcard(value, "*.*.*.*T*Z")

    val type get() = when {
        cloud -> "cloud"
        else -> "on-prem"
    }

    // === Overriddes ===

    override fun toString() = "$value ($type)"

    override fun compareTo(other: AemVersion): Int = base.compareTo(other.base)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AemVersion
        if (base != other.base) return false
        return true
    }

    override fun hashCode(): Int = base.hashCode()

    class UnclosedRange(val start: AemVersion, val end: AemVersion) {
        operator fun contains(version: AemVersion) = version >= start && version < end
    }

    companion object {

        val UNKNOWN = AemVersion("0.0.0")

        val VERSION_6_0_0 = AemVersion("6.0.0")

        val VERSION_6_1_0 = AemVersion("6.1.0")

        val VERSION_6_2_0 = AemVersion("6.2.0")

        val VERSION_6_3_0 = AemVersion("6.3.0")

        val VERSION_6_4_0 = AemVersion("6.4.0")

        val VERSION_6_5_0 = AemVersion("6.5.0")

        fun unclosedRange(value: String, delimiter: String = "-"): UnclosedRange {
            val versions = value.split(delimiter)
            if (versions.size != 2) {
                throw AemException("AEM version range has invalid format: '$value'!")
            }

            val (start, end) = versions.map { AemVersion(it) }
            return UnclosedRange(start, end)
        }

        fun fromJar(jar: File): AemVersion? = jar.takeIf { it.exists() }
            ?.let { ZipFile(it).listDir("static/app") }
            ?.map { it.substringAfterLast("/") }
            ?.firstOrNull { it.startsWith("cq-quickstart-") && it.endsWith(".jar") }
            ?.let { fromJarFileName(it) }

        fun fromJarFileName(fileName: String): AemVersion = fileName
            .removePrefix("cq-quickstart-").removePrefix("cloudready-")
            .substringBefore("-").let { AemVersion(it) }
    }
}

fun String.javaVersions(delimiter: String = ",") = this.split(delimiter).map { JavaVersion.toVersion(it) }
