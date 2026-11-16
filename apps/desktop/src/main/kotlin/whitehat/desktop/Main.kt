package whitehat.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.client.engine.cio.CIO
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import whitehat.client.*

private class DesktopConnection : AutoCloseable {
    private fun setting(name: String) = requireNotNull(System.getenv("WH_DESKTOP_$name")?.takeIf { it.isNotBlank() }) {
        "Missing setting: WH_DESKTOP_$name"
    }
    private val config = NativeOidcConfig(setting("ISSUER"), setting("CLIENT_ID"), setting("AUTHORIZATION_URL"),
        setting("TOKEN_URL"), setting("REVOCATION_URL"), setting("REDIRECT_URI"))
    private val verifier = JvmRs256IdTokenVerifier(URI(setting("JWKS_URL")))
    private val api = setting("API")
    private val store = desktopTokenStore(config.issuer, config.clientId, api)
    private val oidcEngine = CIO.create()
    private val apiEngine = CIO.create()
    private val identity = OidcNativeIdentity(config, oidcEngine, JvmLoopbackBrowser(), JvmOidcProofs(), verifier)
    val client = WhiteHatClient(api, apiEngine, NativeSession(identity, store))

    override fun close() {
        client.close()
        identity.close()
        apiEngine.close()
        oidcEngine.close()
    }
}

fun main(args: Array<String>) {
    if (args.contentEquals(arrayOf("--version"))) {
        println("White Hat Desktop")
        return
    }
    require(args.isEmpty())
    application {
        Window(onCloseRequest = ::exitApplication, title = "White Hat") {
            val connection = remember { runCatching(::DesktopConnection) }
            val resource = connection.getOrNull()
            DisposableEffect(resource) { onDispose { resource?.close() } }
            MaterialTheme {
                if (resource != null) Desktop(resource.client)
                else Text(connection.exceptionOrNull()?.message ?: "Invalid configuration", Modifier.padding(24.dp))
            }
        }
    }
}

@Composable
private fun Desktop(client: WhiteHatClient) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf<Profile?>(null) }
    var workspace by remember { mutableStateOf<Workspace?>(null) }
    var decisions by remember { mutableStateOf<List<Decision>>(emptyList()) }
    var modes by remember { mutableStateOf<List<DecisionMode>>(emptyList()) }
    var decision by remember { mutableStateOf<Decision?>(null) }
    var run by remember { mutableStateOf<Run?>(null) }
    var submission by remember { mutableStateOf<RunSubmission?>(null) }
    var workspaceName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("challenge") }

    fun open(value: Decision?, existingRun: Run? = null) {
        decision = value
        run = existingRun
        submission = null
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

    suspend fun handleOperation(restoring: Boolean = false, action: suspend () -> Unit) {
        try {
            action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: ApiFailure) {
            if (failure.recovery == Recovery.SIGN_IN) {
                profile = null
                select(null)
                if (restoring) return
            }
            error = "${failure.code} (${failure.recovery})"
        } catch (_: Exception) {
            error = "The operation failed. Check the connection and try again."
        } finally {
            busy = false
        }
    }

    LaunchedEffect(client) {
        handleOperation(restoring = true) { loadProfile() }
    }

    fun perform(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = ""
        scope.launch { handleOperation(action = action) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("White Hat", style = MaterialTheme.typography.h4)
        if (profile == null) {
            Button(enabled = !busy, onClick = { perform {
                client.signIn()
                loadProfile()
            } }) { Text("Sign in") }
        } else {
            OutlinedButton(enabled = !busy, onClick = { perform {
                try { client.signOut() } finally { profile = null; select(null) }
            } }) { Text("Sign out") }
            profile?.workspaces.orEmpty().forEach { item ->
                OutlinedButton(enabled = !busy && workspace?.id != item.id,
                    onClick = { perform { select(item) } }) { Text(item.name) }
            }
            OutlinedTextField(workspaceName, { workspaceName = it }, label = { Text("Workspace name") })
            Button(enabled = !busy && workspaceName.isNotBlank(), onClick = { perform {
                val created = client.createWorkspace(workspaceName)
                profile = client.me()
                select(created)
                workspaceName = ""
            } }) { Text("Create workspace") }

            workspace?.let { selected ->
                OutlinedButton(enabled = !busy, onClick = { open(null) }) { Text("New decision") }
                decisions.forEach { saved ->
                    OutlinedButton(enabled = !busy, onClick = { perform {
                        val value = client.decision(selected.id, saved.id)
                        open(value, client.ownRuns(selected.id).firstOrNull { it.decisionId == value.id })
                    } }) { Text(saved.title) }
                }
                OutlinedTextField(title, { title = it }, label = { Text("Decision title") })
                OutlinedTextField(prompt, { prompt = it }, label = { Text("Decision") })
                var showModes by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(enabled = !busy, onClick = { showModes = true }) { Text("Mode: $mode") }
                    DropdownMenu(expanded = showModes, onDismissRequest = { showModes = false }) {
                        modes.forEach { choice ->
                            DropdownMenuItem(onClick = { mode = choice.id; showModes = false }) { Text(choice.id) }
                        }
                    }
                }
                Button(enabled = !busy && title.isNotBlank() && prompt.isNotBlank(), onClick = { perform {
                    val old = decision
                    val input = DecisionInput(title, prompt, mode, old?.objective.orEmpty(), old?.context.orEmpty(),
                        old?.sourceNotes.orEmpty())
                    val updated = if (old == null) client.createDecision(selected.id, input)
                        else client.replaceDecision(selected.id, old.id, old.revision, input)
                    open(updated)
                    decisions = client.decisions(selected.id).items
                } }) { Text("Save decision") }
                OutlinedButton(enabled = !busy && decision != null && run?.isActive != true, onClick = { perform {
                    val value = requireNotNull(decision)
                    val intent = submission ?: RunSubmission(selected.id, value.id, value.revision,
                        UUID.randomUUID().toString()).also { submission = it }
                    run = client.enqueue(intent)
                    submission = null
                } }) { Text("Start or recover analysis") }
                run?.let { analysis ->
                    Text("Analysis status: ${analysis.status}")
                    analysis.result?.let { Text(it.toString()) }
                    if (analysis.isActive) {
                        OutlinedButton(enabled = !busy, onClick = { perform { run = client.run(selected.id, analysis.id) } }) {
                            Text("Refresh analysis")
                        }
                        OutlinedButton(enabled = !busy, onClick = { perform { run = client.cancel(selected.id, analysis.id) } }) {
                            Text("Cancel analysis")
                        }
                    }
                }
            }
        }
        if (busy) LinearProgressIndicator()
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colors.error)
    }
}
