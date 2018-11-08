package com.cognifide.gradle.aem.api

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceType
import com.cognifide.gradle.aem.instance.LocalInstance
import com.cognifide.gradle.aem.instance.RemoteInstance
import com.cognifide.gradle.aem.internal.LineSeparator
import com.cognifide.gradle.aem.internal.notifier.Notifier
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.io.Serializable

/**
 * General AEM related configuration (shared for tasks).
 */
class AemConfig(
        @Transient
        @JsonIgnore
        private val aem: AemExtension
) : Serializable {

    /**
     * List of AEM instances on which packages could be deployed.
     * Instance stored in map ensures name uniqueness and allows to be referenced in expanded properties.
     */
    @Input
    var instances: MutableMap<String, Instance> = mutableMapOf()

    /**
     * Path in which local AEM instances will be stored.
     *
     * Default: "${System.getProperty("user.home")}/.aem/${project.rootProject.name}"
     */
    @Input
    var instanceRoot: String = "${System.getProperty("user.home")}/.aem/${aem.project.rootProject.name}"

    /**
     * Determines instances involved in CRX package deployment (filters preconfigured instances).
     *
     * TODO move it to extension
     */
    @Input
    var instanceName: String = aem.props.string("aem.instance.name", "${aem.environment}-*")

    /**
     * Forces instances involved in e.g CRX package deployment (uses explicit instances configuration).
     *
     * TODO move it to extension
     */
    @Input
    var instanceList: String = aem.props.string("aem.instance.list", "")

    /**
     * Determines instance which will be used when CRX package activation from author to publishers
     * will be performed (only if distributed deploy is enabled).
     *
     * TODO move it to extension
     */
    @Input
    var instanceAuthorName: String = aem.props.string("aem.instance.author.name", "${aem.environment}-${InstanceType.AUTHOR.type}*")

    /**
     * Defines maximum time after which initializing connection to AEM will be aborted (e.g on upload, install).
     *
     * Default value may look quite big, but it is just very fail-safe.
     */
    @Input
    var instanceConnectionTimeout: Int = aem.props.int("aem.instance.connectionTimeout", 30000)

    /**
     * Determines if connection to untrusted (e.g. self-signed) SSL certificates should be allowed.
     *
     * By default allows all SSL connections.
     */
    @Input
    var instanceConnectionUntrustedSsl: Boolean = aem.props.boolean("aem.instance.connectionUntrustedSsl", true)
    /**
     * CRX package name conventions (with wildcard) indicating that package can change over time
     * while having same version specified. Affects CRX packages composed and satisfied.
     */
    @Input
    var packageSnapshots: List<String> = aem.props.list("aem.package.snapshots")

    @Input
    var packageRoot: String = "${aem.project.file("src/main/content")}"

    @get:Internal
    @get:JsonIgnore
    val packageJcrRoot: String
        get() = "$packageRoot/${PackagePlugin.JCR_ROOT}"

    @get:Internal
    @get:JsonIgnore
    val packageVltRoot: String
        get() = "$packageRoot/${PackagePlugin.VLT_PATH}"

    /**
     * Content path for OSGi bundle jars being placed in CRX package.
     *
     * Default convention assumes that subprojects have separate bundle paths, because of potential re-installation of subpackages.
     * When all subprojects will have same bundle path, reinstalling one subpackage may end with deletion of other bundles coming from another subpackage.
     *
     * Beware that more nested bundle install directories are not supported by AEM by default.
     */
    @Input
    var packageInstallPath: String = if (aem.project == aem.project.rootProject) {
        "/apps/${aem.project.rootProject.name}/install"
    } else {
        "/apps/${aem.project.rootProject.name}/${aem.projectName}/install"
    }

    /**
     * Define known exceptions which could be thrown during package installation
     * making it impossible to succeed.
     *
     * When declared exception is encountered during package installation process, no more
     * retries will be applied.
     */
    @Input
    var packageErrors = aem.props.list("aem.package.errors", defaultValue = listOf(
            "javax.jcr.nodetype.ConstraintViolationException",
            "org.apache.jackrabbit.vault.packaging.DependencyException",
            "org.xml.sax.SAXException"
    ))

    /**
     * Determines number of lines to process at once during reading html responses.
     *
     * The higher the value, the bigger consumption of memory but shorter execution time.
     * It is a protection against exceeding max Java heap size.
     */
    @Input
    var packageResponseBuffer = aem.props.int("aem.package.responseBuffer", 4096)

    /**
     * Specify characters to be used as line endings when cleaning up checked out JCR content.
     */
    @Input
    var lineSeparator: String = aem.props.string("aem.lineSeparator", "LF")

    /**
     * Turn on/off default system notifications.
     */
    @Internal
    var notificationEnabled: Boolean = aem.props.flag("aem.notification.enabled")

    /**
     * Hook for customizing notifications being displayed.
     *
     * To customize notification use one of concrete provider methods: 'dorkbox' or 'jcgay' (and optionally pass configuration lambda(s)).
     * Also it is possible to implement own notifier directly in build script by using provider method 'custom'.
     */
    @Internal
    @JsonIgnore
    var notificationConfig: (AemNotifier) -> Notifier = { it.factory() }


    init {
        // Define through command line (forced instances)
        if (instanceList.isNotBlank()) {
            instances(Instance.parse(aem.project, instanceList))
        }

        // Define through properties
        instances(Instance.properties(aem.project))

        aem.project.afterEvaluate { _ ->
            // Ensure defaults if still no instances defined at all
            if (instances.isEmpty()) {
                instances(Instance.defaults(aem.project, aem.environment))
            }

            // Validate all
            instances.values.forEach { it.validate() }
        }
    }

    /**
     * Declare new deployment target (AEM instance).
     */
    fun localInstance(httpUrl: String) {
        localInstance(httpUrl) {}
    }

    fun localInstance(httpUrl: String, configurer: LocalInstance.() -> Unit) {
        instance(LocalInstance.create(aem.project, httpUrl) {
            this.environment = aem.environment
            this.apply(configurer)
        })
    }

    fun remoteInstance(httpUrl: String) {
        remoteInstance(httpUrl) {}
    }

    fun remoteInstance(httpUrl: String, configurer: RemoteInstance.() -> Unit) {
        instance(RemoteInstance.create(aem.project, httpUrl) {
            this.environment = aem.environment
            this.apply(configurer)
        })
    }

    fun parseInstance(urlOrName: String): Instance {
        return instances[urlOrName]
                ?: Instance.parse(aem.project, urlOrName).single().apply { validate() }
    }

    private fun instances(instances: Collection<Instance>) {
        instances.forEach { instance(it) }
    }

    private fun instance(instance: Instance) {
        if (instances.containsKey(instance.name)) {
            throw AemException("Instance named '${instance.name}' is already defined. Enumerate instance types (for example 'author1', 'author2') or distinguish environments.")
        }

        instances[instance.name] = instance
    }

    @get:Internal
    @get:JsonIgnore
    val lineSeparatorString: String = LineSeparator.string(lineSeparator)

}