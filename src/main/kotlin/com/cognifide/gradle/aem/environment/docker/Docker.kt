package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.utils.Formats
import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentException
import java.io.File

class Docker(val environment: Environment) {

    val aem = environment.aem

    private val logger = aem.logger

    val running: Boolean
        get() = stack.running && containers.running

    val up: Boolean
        get() = stack.running && containers.up

    /**
     * Represents Docker stack named 'aem' and provides API for manipulating it.
     */
    val stack = Stack(environment)

    /**
     * Provides API for manipulating Docker containers defined in 'docker-compose.yml'.
     */
    val containers = ContainerManager(this)

    /**
     * Configure additional behavior for Docker containers defined in 'docker-compose.yml'.
     */
    fun containers(options: ContainerManager.() -> Unit) {
        containers.apply(options)
    }

    val runtime: Runtime = Runtime.determine(aem)

    val composeFile
        get() = File(environment.rootDir, "docker-compose.yml")

    val composeTemplateFile: File
        get() = File(environment.configDir, "docker-compose.yml.peb")

    val configPath: String
        get() = runtime.determinePath(environment.configDir)

    val rootPath: String
        get() = runtime.determinePath(environment.rootDir)

    fun init() {
        syncComposeFile()
        containers.resolve()
    }

    private fun syncComposeFile() {
        logger.info("Generating Docker compose file '$composeFile' from template '$composeTemplateFile'")

        if (!composeTemplateFile.exists()) {
            throw EnvironmentException("Docker compose file template does not exist: $composeTemplateFile")
        }

        composeFile.takeIf { it.exists() }?.delete()
        composeTemplateFile.copyTo(composeFile)
        aem.props.expand(composeFile, mapOf("docker" to this))
    }

    fun up() {
        stack.reset()
        containers.up()
    }

    fun reload() {
        containers.reload()
    }

    fun down() {
        stack.undeploy()
    }

    fun run(image: String, command: String, exitCode: Int = 0) = run {
        this.image = image
        this.command = command
        this.exitCodes = listOf(exitCode)
    }

    fun runShell(image: String, command: String, exitCode: Int = 0) = run(image, "sh -c '$command'", exitCode)

    fun run(operation: String, image: String, command: String, exitCode: Int = 0) = run {
        this.operation = { operation }
        this.image = image
        this.command = command
        this.exitCodes = listOf(exitCode)
    }

    fun runShell(operation: String, image: String, command: String, exitCode: Int = 0) = run(operation, image, "sh -c '$command'", exitCode)

    fun run(options: RunSpec.() -> Unit): DockerResult {
        val spec = RunSpec().apply(options)
        val operation = spec.operation()

        lateinit var result: DockerResult
        val action = {
            try {
                result = run(spec)
            } catch (e: DockerException) {
                logger.debug("Run operation '$operation' error", e)
                throw EnvironmentException("Failed to run operation on Docker!\n$operation\n${e.message}")
            }
        }

        if (spec.indicator) {
            aem.progress {
                message = operation
                action()
            }
        } else {
            action()
        }

        return result
    }

    private fun run(spec: RunSpec): DockerResult {
        if (spec.image.isBlank()) {
            throw DockerException("Run image cannot be blank!")
        }
        if (spec.command.isBlank()) {
            throw DockerException("Run command cannot be blank!")
        }

        val customSpec = DockerCustomSpec(spec, mutableListOf<String>().apply {
            add("run")
            addAll(spec.volumes.map { (localPath, containerPath) -> "-v=${runtime.determinePath(localPath)}:$containerPath" })
            addAll(spec.ports.map { (hostPort, containerPort) -> "-p=$hostPort:$containerPort" })
            addAll(spec.options)
            add(spec.image)
            addAll(Formats.commandToArgs(spec.command))
        })

        logger.info("Running Docker command '${customSpec.fullCommand}'")

        return DockerProcess.execSpec(customSpec)
    }
}
