package com.cognifide.gradle.aem.test.deploy

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Test
import java.io.File

class ComposeTaskTest : BaseTaskTest() {

    override val TESTED_TASK = ":aemCompose"

    @Test
    fun simpleComposeTest() {

        val example = testProjectDir.newFolder("example")
        val buildScript = File("src/test/resources/build.gradle")
        buildScript.copyTo((File("${example.path}/${buildScript.name}")))

        val runner = GradleRunner.create()
                .withProjectDir(example)
                .withArguments(listOf(TESTED_TASK, "-i"))
                .build()

        Assert.assertEquals(TaskOutcome.SUCCESS, runner.task(TESTED_TASK).outcome)
    }
}