package com.cognifide.gradle.aem.test.deploy

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import java.io.File

class UploadTaskTest : BaseTaskTest() {

    override val TESTED_TASK = ":aemUpload"

    @Test
    fun packageMissingTest() {

        if (!isCrxRunning(4502) || !isCrxRunning(4503)) {
            Assert.fail("UploadTaskTest requires AEM to be running on ports 4502 and 4503")
        }

        val example = testProjectDir.newFolder("example")
        val buildScript = File("src/test/resources/uploadTask")
        buildScript.copyTo(example, true)


        val runner = GradleRunner.create()
                .withProjectDir(example)
                .withArguments(listOf(TESTED_TASK, "-i"))
                .build()

        Assert.assertEquals(TaskOutcome.SUCCESS, runner.task(TESTED_TASK).outcome)
    }

}