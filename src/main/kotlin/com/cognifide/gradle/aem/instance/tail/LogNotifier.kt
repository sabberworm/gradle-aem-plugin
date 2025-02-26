package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.common.notifier.NotifierFacade
import com.cognifide.gradle.aem.instance.tail.io.LogFiles
import java.net.URI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

@UseExperimental(ObsoleteCoroutinesApi::class)
class LogNotifier(
    private val notificationChannel: ReceiveChannel<LogChunk>,
    private val notifier: NotifierFacade,
    private val logFiles: LogFiles
) {

    fun listenTailed() {
        GlobalScope.launch {
            notificationChannel.consumeEach { logs ->
                val file = snapshotErrorsToSeparateFile(logs)
                notifyLogErrors(logs, file)
            }
        }
    }

    private fun notifyLogErrors(chunk: LogChunk, file: URI) {
        val errors = chunk.size
        val message = chunk.logs.lastOrNull()?.cause ?: ""
        val instance = chunk.instance.name

        notifier.notifyLogError("$errors error(s) on $instance", message, file)
    }

    private fun snapshotErrorsToSeparateFile(chunk: LogChunk): URI {
        return logFiles.writeToIncident(chunk.instance.name) { out ->
            chunk.logs.forEach {
                out.write("${it.text}\n")
            }
        }
    }
}
