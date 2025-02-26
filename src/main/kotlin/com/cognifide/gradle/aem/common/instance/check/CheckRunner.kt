package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.build.Behaviors
import com.cognifide.gradle.aem.common.build.ProgressIndicator
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceException
import kotlinx.coroutines.isActive
import org.apache.commons.lang3.time.StopWatch

class CheckRunner(internal val aem: AemExtension) {

    private var checks: CheckFactory.() -> List<Check> = { throw InstanceException("No instance checks defined!") }

    /**
     * Defines which checks should be performed (and repeated).
     */
    fun checks(definitions: CheckFactory.() -> List<Check>) {
        checks = definitions
    }

    private var progresses: Map<Instance, CheckProgress> = mapOf()

    /**
     * Get current checking progress of concrete instance.
     */
    fun progress(instance: Instance): CheckProgress {
        return progresses[instance] ?: throw InstanceException("No progress available for instance '${instance.name}'!")
    }

    /**
     * How long to wait after failed checking before checking again.
     */
    var delay = 0L

    /**
     * Controls if aborted running should fail build.
     */
    var verbose = true

    /**
     * Time since running started.
     */
    val runningTime: Long
        get() = runningWatch.time

    private val runningWatch = StopWatch()

    /**
     * Error causing running stopped.
     */
    var abortCause: Exception? = null

    /**
     * Verify if running is stopped.
     */
    val aborted: Boolean
        get() = abortCause != null

    /**
     * Controls logging behavior
     *
     * By default it just keeps clean console if info logging level is not enabled.
     */
    var logInstantly = aem.logger.isInfoEnabled

    fun check(instances: Collection<Instance>) {
        aem.progressIndicator {
            updater { update(progresses.values.joinToString(" | ") { it.summary }) }
            doChecking(instances)
            doAbort()
        }
    }

    private fun ProgressIndicator.doChecking(instances: Collection<Instance>) {
        step = "Checking"

        runningWatch.start()

        progresses = instances
                .fold(mutableMapOf<Instance, CheckProgress>()) { r, i -> r[i] = CheckProgress(i) ; r }
                .toSortedMap(compareBy { it.name })

        aem.parallel.each(instances) { instance ->
            val progress = progresses[instance]!!

            progress.stateWatch.start()

            do {
                if (aborted) {
                    aem.logger.info("Checking aborted for $instance")
                    break
                }

                val checks = CheckGroup(this@CheckRunner, instance, checks).apply {
                    check()
                    if (logInstantly) {
                        log()
                    }
                }

                progress.currentCheck = checks

                if (progress.stateChanged) {
                    progress.stateChanges++
                    progress.stateWatch.apply { reset(); start() }
                }

                progress.previousCheck = progress.currentCheck

                if (checks.done) {
                    aem.logger.info("Checking done for $instance")
                    break
                }

                Behaviors.waitFor(delay)
            } while (isActive)
        }

        runningWatch.stop()
    }

    private fun ProgressIndicator.doAbort() {
        if (aborted) {
            step = "Aborting"

            if (!logInstantly) {
                progresses.values.forEach { it.currentCheck?.log() }
            }

            if (verbose) {
                abortCause?.let { throw it }
            } else {
                aem.logger.error("Checking error", abortCause)
            }
        }
    }
}
