import SwiftUI
import WhiteHatClient

@main
struct WhiteHatApp: App {
    var body: some Scene {
        WindowGroup { HomeView() }
    }
}

@MainActor
private struct HomeView: View {
    @State private var client: IosHostClient?
    @State private var profile: Profile?
    @State private var workspace: Workspace?
    @State private var decisions: [Decision] = []
    @State private var decision: Decision?
    @State private var run: Run?
    @State private var pendingKey: String?
    @State private var workspaceName = ""
    @State private var title = ""
    @State private var prompt = ""
    @State private var mode = "challenge"
    @State private var message = ""
    @State private var busy = false

    var body: some View {
        NavigationStack {
            Form {
                if let client {
                    if profile == nil {
                        Button("Sign in") { perform {
                            let signedIn = try await client.signIn()
                            try await select(signedIn.workspaces.first, using: client)
                            profile = signedIn
                        } }
                    } else {
                        workspaceSection(client)
                        if let workspace { decisionSection(client, workspace) }
                        Button("Sign out") { perform {
                            defer { profile = nil; workspace = nil; decisions = []; open(nil) }
                            try await client.signOut()
                        } }
                    }
                } else {
                    Text("Configure the approved White Hat test environment.")
                }
                if !message.isEmpty { Text(message).foregroundStyle(.red) }
            }
            .disabled(busy)
            .overlay { if busy { ProgressView() } }
            .navigationTitle("White Hat")
        }
        .task {
            guard client == nil, let config = try? HostConfiguration(),
                  let host = try? IosHostClientKt.makeIosHostClient(
                    api: config.api, issuer: config.issuer, clientId: config.clientID,
                    authorization: config.authorization, token: config.token, revocation: config.revocation,
                    redirect: config.redirect, platform: AppleHostPlatform(config: config)) else { return }
            client = host
            busy = true
            defer { busy = false }
            // A fresh install has no session; offer sign-in.
            if let restored = try? await host.profile() {
                do {
                    try await select(restored.workspaces.first, using: host)
                    profile = restored
                } catch {
                    message = "Could not restore the workspace."
                }
            }
        }
    }

    private func workspaceSection(_ client: IosHostClient) -> some View {
        Section("Workspace") {
            ForEach(profile?.workspaces ?? [], id: \.id) { item in
                Button(item.name) { perform { try await select(item, using: client) } }
                    .disabled(item.id == workspace?.id)
            }
            TextField("Workspace name", text: $workspaceName)
            Button("Create workspace") { perform {
                let created = try await client.createWorkspace(name: workspaceName)
                profile = try await client.profile()
                try await select(created, using: client)
                workspaceName = ""
            } }.disabled(workspaceName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        }
    }

    private func decisionSection(_ client: IosHostClient, _ workspace: Workspace) -> some View {
        Section("Decisions") {
            ForEach(decisions, id: \.id) { item in Button(item.title) { open(item) } }
            TextField("Title", text: $title)
            TextField("Decision", text: $prompt, axis: .vertical)
            Picker("Mode", selection: $mode) {
                ForEach(["challenge", "kill", "blind", "premortem", "reality", "compare", "devil"], id: \.self) {
                    Text($0).tag($0)
                }
            }
            Button("Save new decision") { perform {
                open(try await client.createDecision(workspace: workspace.id, title: title, prompt: prompt, mode: mode))
                decisions = try await client.decisions(workspace: workspace.id).items
            } }.disabled(title.isEmpty || prompt.isEmpty)
            if let decision {
                Button("Start or recover analysis") { perform {
                    let key = pendingKey ?? UUID().uuidString
                    pendingKey = key
                    run = try await client.enqueue(workspace: workspace.id, decision: decision, key: key)
                    pendingKey = nil
                } }.disabled(run?.isActive == true || run?.status == "reconciliation_required")
            }
            if let run {
                Text(run.status)
                if let result = client.renderRunResult(run: run) { Text(result) }
                if run.isActive {
                    Button("Refresh analysis") { perform {
                        self.run = try await client.loadRun(workspace: workspace.id, id: run.id)
                    } }
                    Button("Cancel analysis") { perform {
                        self.run = try await client.cancel(workspace: workspace.id, id: run.id)
                    } }
                }
            }
        }
    }

    private func select(_ value: Workspace?, using client: IosHostClient) async throws {
        let items: [Decision]
        if let value { items = try await client.decisions(workspace: value.id).items }
        else { items = [] }
        workspace = value
        decisions = items
        open(nil)
    }

    private func open(_ value: Decision?) {
        decision = value
        pendingKey = nil
        run = nil
    }

    private func perform(_ operation: @escaping @MainActor () async throws -> Void) {
        guard !busy else { return }
        busy = true
        message = ""
        Task { @MainActor in
            defer { busy = false }
            do { try await operation() }
            catch { message = "The operation failed. Check your session and connection." }
        }
    }
}
