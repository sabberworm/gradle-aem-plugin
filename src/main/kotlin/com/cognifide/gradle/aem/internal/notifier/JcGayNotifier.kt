package com.cognifide.gradle.aem.internal.notifier

import com.cognifide.gradle.aem.api.AemPlugin
import com.cognifide.gradle.aem.base.Notifier
import com.cognifide.gradle.aem.internal.BuildScope
import fr.jcgay.notification.Application
import fr.jcgay.notification.Icon
import fr.jcgay.notification.Notification
import fr.jcgay.notification.SendNotification
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import fr.jcgay.notification.Notifier as Base
import com.cognifide.gradle.aem.internal.notifier.Notifier as BaseNotifier

class JcGayNotifier(
        val project: Project,
        val appBuilder: Application.Builder.() -> Unit,
        val messageBuilder: Notification.Builder.() -> Unit
) : BaseNotifier {

    private val notifier by lazy {
        val notifier = BuildScope.of(project).getOrPut(JcGayNotifier::class.java.canonicalName, {
            SendNotification()
                    .setApplication(Application.builder()
                            .id(AemPlugin.ID)
                            .name(AemPlugin.NAME)
                            .icon(icon)
                            .apply(appBuilder)
                            .build())
                    .initNotifier()
        })

        if (project == project.rootProject) {
            project.gradle.buildFinished { notifier.close() }
        }

        notifier
    }

    private val icon by lazy {
        Icon.create(javaClass.getResource(Notifier.IMAGE_PATH), "default")
    }

    override fun notify(title: String, text: String, level: LogLevel) {
        try {
            val notification = Notification.builder()
                    .icon(icon)
                    .apply(messageBuilder)
                    .title(title)
                    .message(text)
                    .level(LOG_LEVEL_NOTIFY_MAP[level] ?: Notification.Level.INFO)
                    .build()
            notifier.send(notification)
        } catch (e: Exception) {
            project.logger.debug("Cannot show system notification", e)
        }
    }

    companion object {

        val LOG_LEVEL_NOTIFY_MAP = mapOf(
                LogLevel.ERROR to Notification.Level.ERROR,
                LogLevel.WARN to Notification.Level.WARNING
        )

    }
}