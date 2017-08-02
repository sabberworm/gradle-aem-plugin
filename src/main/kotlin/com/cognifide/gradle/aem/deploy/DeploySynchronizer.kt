package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.instance.Instance
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.multipart.*
import org.apache.commons.httpclient.params.HttpConnectionParams
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class DeploySynchronizer(val instance: Instance, val config: AemConfig) {

    companion object {
        private val LOG = LoggerFactory.getLogger(DeploySynchronizer::class.java)

        private val PACKAGE_MANAGER_SERVICE_SUFFIX = "/crx/packmgr/service"

        private val PACKAGE_MANAGER_LIST_SUFFIX = "/crx/packmgr/list.jsp"
    }

    val jsonTargetUrl = instance.httpUrl + PACKAGE_MANAGER_SERVICE_SUFFIX + "/.json"

    val htmlTargetUrl = instance.httpUrl + PACKAGE_MANAGER_SERVICE_SUFFIX + "/.html"

    val listPackagesUrl = instance.httpUrl + PACKAGE_MANAGER_LIST_SUFFIX

    val bundlesUrl = "${instance.httpUrl}/system/console/bundles.json"

    fun get(url: String, parametrizer: (HttpConnectionParams) -> Unit = {}): String {
        val method = GetMethod(url)

        return execute(method, parametrizer)
    }

    fun post(url: String, params: Map<String, Any> = mapOf(), parametrizer: (HttpConnectionParams) -> Unit = {}): String {
        val method = PostMethod(url)
        method.requestEntity = MultipartRequestEntity(createParts(params).toTypedArray(), method.params)

        return execute(method, parametrizer)
    }

    fun execute(method: HttpMethod, parametrizer: (HttpConnectionParams) -> Unit = {}): String {
        try {
            val client = createHttpClient()
            parametrizer(client.httpConnectionManager.params)

            val status = client.executeMethod(method)
            if (status == HttpStatus.SC_OK) {
                return IOUtils.toString(method.responseBodyAsStream)
            } else {
                LOG.debug(method.responseBodyAsString)
                throw DeployException("Request to the instance failed, cause: "
                        + HttpStatus.getStatusText(status) + " (check URL, user and password)")
            }

        } catch (e: IOException) {
            throw DeployException("Request to the instance failed, cause: " + e.message, e)
        } finally {
            method.releaseConnection()
        }
    }

    fun createHttpClient(): HttpClient {
        val client = HttpClient()
        client.httpConnectionManager.params.connectionTimeout = config.deployConnectionTimeout
        client.httpConnectionManager.params.soTimeout = config.deployConnectionTimeout
        client.params.isAuthenticationPreemptive = true
        client.state.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(instance.user, instance.password))

        return client
    }

    private fun createParts(params: Map<String, Any>): List<Part> {
        val partList = mutableListOf<Part>()
        for ((key, value) in params) {
            if (value is File) {
                val file = value
                try {
                    partList.add(FilePart(key, FilePartSource(file.name, file)))
                } catch (e: FileNotFoundException) {
                    throw DeployException(String.format("Upload param '%s' has invalid file specified.", key), e)
                }
            } else {
                val str = value.toString()
                if (!str.isNullOrBlank()) {
                    partList.add(StringPart(key, str))
                }
            }
        }

        return partList
    }

}