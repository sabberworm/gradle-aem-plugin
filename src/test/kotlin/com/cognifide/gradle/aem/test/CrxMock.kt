package com.cognifide.gradle.aem.test

import com.github.kristofa.test.http.Method
import com.github.kristofa.test.http.MockHttpServer
import com.github.kristofa.test.http.SimpleHttpResponseProvider
import java.net.ServerSocket

class CrxMock {

    private var port: Int

    constructor(port: Int) {
        this.port = port
        setupCRX()
    }

    fun setupCRX() {
        try {
            ServerSocket(port).close()
            startServer()
        } catch (e: Exception) {
            //NO SONAR - CRX is running
        }
    }

    fun startServer() {
        val crxResponseProvider = SimpleHttpResponseProvider()
        val server = MockHttpServer(port, crxResponseProvider)
        val uploadResponse = "{'isSuccess': true, 'msg': 'OK!', 'path': '/app/nonexisting'}"

        crxResponseProvider.expect(Method.POST, "/crx/packmgr/service/.json/?cmd=upload")
                .respondWith(200, "application/json", uploadResponse)
        server.start()
    }
}