package whitehat.client

import com.sun.net.httpserver.HttpServer
import java.awt.Desktop
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** Desktop system-browser authorization for a registered IPv4 loopback callback. */
class JvmLoopbackBrowser(
    private val launch: (URI) -> Unit = { uri -> Desktop.getDesktop().browse(uri) },
) : NativeAuthorizationBrowser {
    override suspend fun authorize(url: String, redirect: String): String = withContext(Dispatchers.IO) {
        val callback = URI(redirect)
        val authorization = URI(url)
        require(authorization.scheme == "https" && authorization.host != null && authorization.userInfo == null)
        require(callback.scheme == "http" && callback.host == "127.0.0.1" &&
            callback.port in 1024..65535 && callback.rawPath.startsWith("/") &&
            callback.rawPath != "/" && callback.rawQuery == null && callback.rawFragment == null &&
            callback.userInfo == null)
        val expectedState = io.ktor.http.Url(url).parameters["state"]
        require(!expectedState.isNullOrBlank())
        val result = CompletableDeferred<String>()
        val listener = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), callback.port), 0)
        try {
            listener.createContext(callback.rawPath) { request ->
                request.use {
                    val query = request.requestURI.rawQuery
                    val received = query?.let { "$redirect?$it" }
                    val valid = request.requestMethod == "GET" && request.requestURI.rawPath == callback.rawPath &&
                        received != null && io.ktor.http.Url(received).parameters.getAll("state")?.singleOrNull() == expectedState
                    val message = if (valid) "Return to White Hat to finish signing in." else "Invalid callback."
                    val bytes = message.toByteArray(Charsets.UTF_8)
                    request.responseHeaders.set("Content-Type", "text/plain; charset=utf-8")
                    request.responseHeaders.set("Cache-Control", "no-store")
                    request.responseHeaders.set("Referrer-Policy", "no-referrer")
                    request.sendResponseHeaders(if (valid) 200 else 400, bytes.size.toLong())
                    request.responseBody.use { it.write(bytes) }
                    if (valid) result.complete(requireNotNull(received))
                }
            }
            listener.start()
            launch(authorization)
            withTimeout(120_000) { result.await() }
        } finally {
            listener.stop(0)
        }
    }
}
