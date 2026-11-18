package whitehat.client


/** Maps failures at the Swift boundary so Kotlin exceptions cannot terminate the application. */
class IosHostClient(apiBase: String, config: NativeOidcConfig, platform: IosHostPlatform) {
    private val connection = iosConnection(apiBase, config, platform)
    private val client = connection.client

    @Throws(Exception::class)
    suspend fun signIn(): Profile = client.run { signIn(); me() }

    @Throws(Exception::class)
    suspend fun profile(): Profile = client.me()

    @Throws(Exception::class)
    suspend fun signOut() = client.signOut()

    @Throws(Exception::class)
    suspend fun createWorkspace(name: String): Workspace = client.createWorkspace(name)

    @Throws(Exception::class)
    suspend fun decisions(workspace: String): Decisions = client.decisions(workspace)

    @Throws(Exception::class)
    suspend fun createDecision(workspace: String, title: String, prompt: String, mode: String): Decision =
        client.createDecision(workspace, DecisionInput(title = title, decision = prompt, mode = mode))

    @Throws(Exception::class)
    suspend fun enqueue(workspace: String, decision: Decision, key: String): Run =
        client.enqueue(RunSubmission(workspace, decision.id, decision.revision, key))

    @Throws(Exception::class)
    suspend fun loadRun(workspace: String, id: String): Run = client.run(workspace, id)

    fun renderRunResult(run: Run): String? = run.result?.toString()

    @Throws(Exception::class)
    suspend fun cancel(workspace: String, id: String): Run = client.cancel(workspace, id)

    @Throws(Exception::class)
    fun close() = connection.close()
}

@Throws(Exception::class)
fun makeIosHostClient(
    api: String, issuer: String, clientId: String, authorization: String, token: String,
    revocation: String, redirect: String, platform: IosHostPlatform,
): IosHostClient = IosHostClient(api, NativeOidcConfig(issuer, clientId, authorization, token, revocation, redirect), platform)
