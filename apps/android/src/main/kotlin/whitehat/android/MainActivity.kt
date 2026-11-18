package whitehat.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.client.engine.okhttp.OkHttp
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import whitehat.client.*

class MainActivity : ComponentActivity() {
    private var expectedState: String? = null
    private var callback: CompletableDeferred<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent = Intent(Intent.ACTION_MAIN)
        setContent {
            val resource = remember { runCatching { openClient() } }
            DisposableEffect(resource) { onDispose { resource.getOrNull()?.close() } }
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    resource.getOrNull()?.let { WhiteHat(it.client) }
                        ?: Text("Configure the approved White Hat test environment.", Modifier.padding(24.dp))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val data = intent.data ?: return
        if (intent.action == Intent.ACTION_VIEW && data.scheme == "https" &&
            data.encodedAuthority == Uri.parse(BuildConfig.REDIRECT).encodedAuthority &&
            data.encodedPath == "/oauth/android" &&
            data.getQueryParameters("state").singleOrNull()?.let { it == expectedState } == true) {
            callback?.complete(data.toString())
        }
    }

    private fun openClient(): Connection {
        val config = NativeOidcConfig(BuildConfig.ISSUER, BuildConfig.CLIENT_ID, BuildConfig.AUTHORIZATION_URL,
            BuildConfig.TOKEN_URL, BuildConfig.REVOCATION_URL, BuildConfig.REDIRECT)
        val verifier = JvmRs256IdTokenVerifier(URI(BuildConfig.JWKS_URL))
        val browser = NativeAuthorizationBrowser { url, _ ->
            check(callback == null)
            val attempt = CompletableDeferred<String>()
            expectedState = requireNotNull(Uri.parse(url).getQueryParameter("state"))
            callback = attempt
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
                withTimeoutOrNull(120_000) { attempt.await() }
                    ?: throw OidcFailure(null, "Sign-in timed out")
            } finally {
                callback = null
                expectedState = null
            }
        }
        return Connection(BuildConfig.API, config, browser, verifier, AndroidTokenStore(applicationContext))
    }
}

private class Connection(api: String, config: NativeOidcConfig, browser: NativeAuthorizationBrowser,
                         verifier: NativeIdTokenVerifier, store: NativeTokenStore) : AutoCloseable {
    private val apiEngine = OkHttp.create()
    private val oidcEngine = OkHttp.create()
    private val identity = OidcNativeIdentity(config, oidcEngine, browser, JvmOidcProofs(), verifier)
    val client = WhiteHatClient(api, apiEngine, NativeSession(identity, store))
    override fun close() {
        client.close()
        identity.close()
        apiEngine.close()
        oidcEngine.close()
    }
}

@Composable
private fun WhiteHat(client: WhiteHatClient) {
    val scope = rememberCoroutineScope()
    var task by remember { mutableStateOf<Job?>(null) }
    var profile by remember { mutableStateOf<Profile?>(null) }
    var workspace by remember { mutableStateOf<Workspace?>(null) }
    var decision by remember { mutableStateOf<Decision?>(null) }
    var decisions by remember { mutableStateOf<List<Decision>>(emptyList()) }
    var modes by remember { mutableStateOf<List<DecisionMode>>(emptyList()) }
    var mode by remember { mutableStateOf("challenge") }
    var submission by remember { mutableStateOf<RunSubmission?>(null) }
    var run by remember { mutableStateOf<Run?>(null) }
    var title by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    fun open(value: Decision?, active: Run? = null) {
        decision = value
        submission = null
        run = active
        title = value?.title.orEmpty()
        prompt = value?.decision.orEmpty()
        mode = value?.mode ?: "challenge"
    }

    suspend fun select(value: Workspace?) {
        decisions = value?.let { client.decisions(it.id).items }.orEmpty()
        workspace = value
        open(null)
    }

    suspend fun loadProfile() {
        val me = client.me()
        modes = client.modes()
        select(me.workspaces.firstOrNull())
        profile = me
    }

    fun start(action: suspend () -> Unit) {
        if (task?.isActive == true) return
        message = ""
        task = scope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: ApiFailure) {
                if (failure.recovery == Recovery.SIGN_IN) {
                    profile = null
                    select(null)
                }
                message = "${failure.code} (${failure.recovery})"
            } catch (_: Exception) {
                message = "The operation failed. Check the connection and try again."
            } finally {
                task = null
            }
        }
    }

    LaunchedEffect(client) { start { loadProfile() } }
    val busy = task?.isActive == true
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("White Hat", style = MaterialTheme.typography.h4)
        if (profile == null) {
            Button(enabled = !busy, onClick = { start { client.signIn(); loadProfile() } }) { Text("Sign in") }
        } else {
            OutlinedButton(enabled = !busy, onClick = { start {
                try { client.signOut() } finally { profile = null; select(null) }
            } }) { Text("Sign out") }
            profile?.workspaces.orEmpty().forEach { item ->
                OutlinedButton(enabled = !busy && item.id != workspace?.id, onClick = { start { select(item) } }) { Text(item.name) }
            }
            OutlinedTextField(name, { name = it }, label = { Text("Workspace name") })
            Button(enabled = !busy && name.isNotBlank(), onClick = { start {
                val created = client.createWorkspace(name)
                profile = client.me()
                select(created)
                name = ""
            } }) { Text("Create workspace") }
            workspace?.let { selected ->
                OutlinedButton(enabled = !busy, onClick = { open(null) }) { Text("New decision") }
                decisions.forEach { item ->
                    OutlinedButton(enabled = !busy, onClick = { start {
                        val current = client.decision(selected.id, item.id)
                        open(current, client.ownRuns(selected.id).firstOrNull { it.decisionId == item.id })
                    } }) { Text(item.title) }
                }
                OutlinedTextField(title, { title = it }, label = { Text("Decision title") })
                OutlinedTextField(prompt, { prompt = it }, label = { Text("Decision") })
                var menu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(enabled = !busy, onClick = { menu = true }) { Text("Mode: $mode") }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        modes.forEach { choice ->
                            DropdownMenuItem(onClick = { mode = choice.id; menu = false }) { Text(choice.id) }
                        }
                    }
                }
                Button(enabled = !busy && title.isNotBlank() && prompt.isNotBlank(), onClick = { start {
                    val old = decision
                    val input = DecisionInput(title, prompt, mode, old?.objective.orEmpty(), old?.context.orEmpty(),
                        old?.sourceNotes.orEmpty())
                    open(if (old == null) client.createDecision(selected.id, input)
                        else client.replaceDecision(selected.id, old.id, old.revision, input))
                    decisions = client.decisions(selected.id).items
                } }) { Text("Save decision") }
                OutlinedButton(enabled = !busy && decision != null && run?.isActive != true, onClick = { start {
                    val current = requireNotNull(decision)
                    val intent = submission ?: RunSubmission(selected.id, current.id, current.revision,
                        UUID.randomUUID().toString()).also { submission = it }
                    run = client.enqueue(intent)
                    submission = null
                } }) { Text("Start or recover analysis") }
                run?.let { current ->
                    Text(current.status)
                    current.result?.let { Text(it.toString()) }
                    if (current.isActive) {
                        OutlinedButton(enabled = !busy, onClick = { start { run = client.run(selected.id, current.id) } }) {
                            Text("Refresh analysis")
                        }
                        OutlinedButton(enabled = !busy, onClick = { start { run = client.cancel(selected.id, current.id) } }) {
                            Text("Cancel analysis")
                        }
                    }
                }
            }
        }
        if (busy) OutlinedButton(onClick = { task?.cancel() }) { Text("Stop waiting") }
        if (message.isNotEmpty()) Text(message)
    }
}
