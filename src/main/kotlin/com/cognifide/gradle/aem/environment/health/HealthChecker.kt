package com.cognifide.gradle.aem.environment.health

import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.common.utils.Formats
import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentException

class HealthChecker(val environment: Environment) {

    private val aem = environment.aem

    private val checks = mutableListOf<HealthCheck>()

    private var httpOptions: HttpClient.() -> Unit = {
        connectionRetries = aem.props.boolean("environment.health.http.connectionRetries") ?: false
        connectionTimeout = aem.props.int("environment.health.http.connectionTimeout") ?: 1000
    }

    var retry = aem.retry { afterSecond(aem.props.long("environment.health.retry") ?: 5) }

    fun define(name: String, check: () -> Unit) {
        checks += HealthCheck(name, check)
    }

    // Evaluation

    @Suppress("ComplexMethod")
    fun check(verbose: Boolean = true): List<HealthStatus> {
        var all = listOf<HealthStatus>()
        var passed = listOf<HealthStatus>()
        var failed = listOf<HealthStatus>()
        val count by lazy { "${passed.size}/${all.size} (${Formats.percent(passed.size, all.size)})" }

        aem.progress(checks.size) {
            try {
                retry.withSleep<Unit, EnvironmentException> { no ->
                    reset()

                    step = if (no > 1) {
                        "Health rechecking (${failed.size} failed)"
                    } else {
                        "Health checking"
                    }

                    all = aem.parallel.map(checks) { check ->
                        increment(check.name) {
                            check.perform()
                        }
                    }.toList()
                    passed = all.filter { it.passed }
                    failed = all - passed

                    if (failed.isNotEmpty()) {
                        throw EnvironmentException("There are failed environment health checks. Retrying...")
                    }
                }

                val message = "Environment health check(s) succeed: $count"
                if (!verbose) {
                    aem.logger.lifecycle(message)
                } else {
                    aem.logger.info(message)
                }
            } catch (e: EnvironmentException) {
                val message = "Environment health check(s) failed: $count:\n${all.joinToString("\n")}"
                if (!verbose) {
                    aem.logger.error(message)
                } else {
                    throw EnvironmentException(message)
                }
            }
        }

        return all
    }

    // Shorthand methods for defining health checks

    /**
     * Check URL using specified criteria (HTTP options and e.g text & status code assertions).
     */
    fun url(checkName: String, url: String, criteria: UrlCheck.() -> Unit) {
        define(checkName) {
            aem.http {
                val check = UrlCheck(url).apply(criteria)

                apply(httpOptions)
                apply(check.options)

                request(check.method, check.url) { response ->
                    check.checks.forEach { it(response) }
                }
            }
        }
    }

    fun http(options: HttpClient.() -> Unit) {
        this.httpOptions = options
    }
}
