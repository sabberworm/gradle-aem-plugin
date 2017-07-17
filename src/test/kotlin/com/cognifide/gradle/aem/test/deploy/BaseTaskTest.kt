package com.cognifide.gradle.aem.test.deploy

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.net.ServerSocket

abstract class BaseTaskTest {

    protected abstract val TESTED_TASK: String

    @Rule @JvmField var testProjectDir = TemporaryFolder()

    protected fun isCrxRunning(port: Int): Boolean {
        try {
            ServerSocket(port).close()
            return false
        } catch (e: Exception) {
            return true
        }
    }

}