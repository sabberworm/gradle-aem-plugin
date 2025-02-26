package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.Instance
import org.apache.commons.lang3.time.StopWatch

class CheckProgress(private val instance: Instance) {

    var currentCheck: CheckGroup? = null

    var previousCheck: CheckGroup? = null

    val stateChanged: Boolean
        get() {
            val current = currentCheck ?: return true
            val previous = previousCheck ?: return true

            return current.state != previous.state
        }

    var stateChanges = 0

    internal var stateWatch = StopWatch()

    val stateTime: Long
        get() = stateWatch.time

    val summary: String
        get() = "${instance.name}: ${currentCheck?.summary ?: "In progress"}"
}
