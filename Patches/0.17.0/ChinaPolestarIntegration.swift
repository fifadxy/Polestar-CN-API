import AppKit
import Foundation
import SwiftUI
import WebKit

// Hisingen China Preview 0.17.0
// Safety boundary:
// - Global Hisingen authentication remains untouched.
// - China website sign-in is a website session only.
// - No cookies, form fields, JavaScript values, access tokens, refresh tokens, SMS codes,
//   Digital Key material, VINs, phone numbers, or precise vehicle locations are extracted.
// - China remote commands stay unavailable until their API/token mapping is independently verified.

enum ChinaPolestarRegion: String, CaseIterable, Sendable {
    case global
    case mainlandChina

    var title: String {
        switch self {
        case .global: return "Global"
        case .mainlandChina: return "Mainland China"
        }
    }
}

enum ChinaPolestarSnapshotSource: String, Sendable {
    case liveC3 = "Live C3"
    case latestC3 = "C3 Latest"
    case cache = "Cached"
}

struct ChinaPolestarFreshness: Sendable, Equatable {
    let source: ChinaPolestarSnapshotSource
    let reportedAt: Date
}

extension Notification.Name {
    static let hisingenChinaPolestarRegionDidChange =
        Notification.Name("io.kheirallah.hisingen.cnpreview.region-did-change")
}

@MainActor
enum ChinaPolestarRuntime {
    static let patchVersion = "0.17.0"
    static let regionKey = "hisingen.polestar.signInRegion"
    static let globalValue = "global"
    static let chinaValue = "mainlandChina"

    static var currentRegion: ChinaPolestarRegion {
        let raw = UserDefaults.standard.string(forKey: regionKey) ?? globalValue
        return raw == chinaValue ? .mainlandChina : .global
    }

    static func install() {
        if UserDefaults.standard.object(forKey: regionKey) == nil {
            // Preserve upstream behavior and existing Global sessions by default.
            UserDefaults.standard.set(globalValue, forKey: regionKey)
        }
    }

    static func enforcePreviewUpdatePolicy(_ preferences: PreferencesStore) {
        // A China preview build must never silently update itself back to upstream Hisingen.
        preferences.automaticallyChecksForUpdates = false
        preferences.automaticallyDownloadsUpdates = false
    }

    static func setRegion(_ region: ChinaPolestarRegion) {
        let value = region == .mainlandChina ? chinaValue : globalValue
        UserDefaults.standard.set(value, forKey: regionKey)
        NotificationCenter.default.post(name: .hisingenChinaPolestarRegionDidChange, object: nil)
    }

    static func openChinaWebSignIn() {
        ChinaPolestarWebSignInWindow.shared.open()
    }

    static func presentUpdatePolicyNotice() {
        let alert = NSAlert()
        alert.messageText = "Updates are disabled in Hisingen China Preview"
        alert.informativeText =
            "Automatic and manual Sparkle updates are disabled in this preview so an upstream Hisingen release cannot overwrite the China integration. Install a newer China Preview build from the Polestar-CN-API project instead."
        alert.addButton(withTitle: "OK")
        alert.runModal()
    }
}

@MainActor
final class ChinaPolestarWebSignInWindow: NSObject {
    static let shared = ChinaPolestarWebSignInWindow()

    private var window: NSWindow?
    private var webView: WKWebView?

    func open() {
        if let window {
            window.makeKeyAndOrderFront(nil)
            NSApp.activate(ignoringOtherApps: true)
            return
        }

        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .default()

        let web = WKWebView(
            frame: NSRect(x: 0, y: 0, width: 760, height: 860),
            configuration: configuration
        )
        guard let url = URL(string: "https://www.polestar.cn/zh-cn/auth/login/") else { return }
        web.load(URLRequest(url: url))

        let newWindow = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 760, height: 860),
            styleMask: [.titled, .closable, .miniaturizable, .resizable],
            backing: .buffered,
            defer: false
        )
        newWindow.title = "Polestar China · Official Sign-In"
        newWindow.contentView = web
        newWindow.center()
        newWindow.isReleasedWhenClosed = false

        webView = web
        window = newWindow
        newWindow.makeKeyAndOrderFront(nil)
        NSApp.activate(ignoringOtherApps: true)
    }
}

enum ChinaPolestarProviderError: LocalizedError, Sendable {
    case vehicleAPISessionNotMapped
    case readOnlyPreview
    case vehicleNotResolved

    var errorDescription: String? {
        switch self {
        case .vehicleAPISessionNotMapped:
            return "China website session is available, but a verified China vehicle API session has not been mapped yet."
        case .readOnlyPreview:
            return "China Preview is read-only. Remote commands remain disabled until their China API mapping is verified."
        case .vehicleNotResolved:
            return "No China vehicle has been resolved yet."
        }
    }
}

/// Separate China adapter. It deliberately does not inherit PolestarAPI authentication or
/// Global token assumptions. This is the seam where verified China vehicle discovery/C3 reads
/// will be added. Until then it fails closed instead of leaking Global credentials to China hosts.
actor ChinaPolestarAPI: VehicleProviding {
    nonisolated let brand: VehicleBrand = .polestar

    private var knownCars: [CarSummary] = []

    var cars: [CarSummary] { knownCars }
    var hasWarmSession: Bool { false }
    var acceptsStoredPasswordSignIn: Bool { false }

    func authenticate(
        email: String,
        password: String,
        preferredVIN: String?,
        features: FeatureSelection
    ) async throws {
        throw ChinaPolestarProviderError.vehicleAPISessionNotMapped
    }

    func restoreSession(preferredVIN: String?, features: FeatureSelection) async throws {
        throw ChinaPolestarProviderError.vehicleAPISessionNotMapped
    }

    func resetSession() async {
        knownCars = []
    }

    func signOut() async throws {
        knownCars = []
    }

    func resolvedVIN(preferred: String?) async -> String? {
        nil
    }

    func reloadVehicleMetadata(vin: String, features: FeatureSelection) async throws {
        throw ChinaPolestarProviderError.vehicleAPISessionNotMapped
    }

    func fetchVehicleState(vin: String, features: FeatureSelection) async throws -> VehicleState {
        throw ChinaPolestarProviderError.vehicleAPISessionNotMapped
    }

    func identitySnapshot(for vin: String, features: FeatureSelection) async -> VehicleIdentitySnapshot? {
        nil
    }

    func consumerTelemetryOverlay(
        for vin: String,
        features: FeatureSelection
    ) async -> ConsumerTelemetryOverlay? {
        nil
    }

    func executeRemoteCommand(_ command: RemoteCommand, vin: String) async throws -> RemoteCommandResult {
        throw ChinaPolestarProviderError.readOnlyPreview
    }
}

struct ChinaPolestarDiagnosticProbe: Identifiable, Sendable, Equatable {
    let id: String
    let label: String
    let host: String
    let statusCode: Int?
    let latencyMilliseconds: Int?
    let reachable: Bool
    let detail: String
}

enum ChinaPolestarDiagnostics {
    private struct Endpoint: Sendable {
        let label: String
        let url: String
    }

    private static let endpoints: [Endpoint] = [
        .init(label: "Official Web", url: "https://www.polestar.cn/zh-cn/auth/login/"),
        .init(label: "C3 Discovery", url: "https://cnepmob.volvocars.com.cn"),
        .init(label: "C3 Token", url: "https://cepmobtoken.prod.c3.volvocars.com.cn"),
        .init(label: "C3 LBS", url: "https://cepmob.lbs.prod.c3.volvocars.com.cn"),
        .init(label: "C3 Signature", url: "https://cepmobsig.prod.c3.volvocars.com.cn"),
        .init(label: "PCCS China", url: "https://api.pccs-prod.origin.polestar.cn")
    ]

    static func run() async -> [ChinaPolestarDiagnosticProbe] {
        await withTaskGroup(of: ChinaPolestarDiagnosticProbe.self) { group in
            for endpoint in endpoints {
                group.addTask {
                    await probe(endpoint)
                }
            }
            var results: [ChinaPolestarDiagnosticProbe] = []
            for await result in group {
                results.append(result)
            }
            let rank = Dictionary(
                uniqueKeysWithValues: endpoints.enumerated().map { ($0.element.label, $0.offset) }
            )
            return results.sorted {
                (rank[$0.label] ?? Int.max) < (rank[$1.label] ?? Int.max)
            }
        }
    }

    private static func probe(_ endpoint: Endpoint) async -> ChinaPolestarDiagnosticProbe {
        guard let url = URL(string: endpoint.url), let host = url.host else {
            return .init(
                id: endpoint.label, label: endpoint.label, host: endpoint.url,
                statusCode: nil, latencyMilliseconds: nil, reachable: false,
                detail: "Invalid URL"
            )
        }

        var request = URLRequest(url: url)
        request.httpMethod = "HEAD"
        request.timeoutInterval = 10
        request.cachePolicy = .reloadIgnoringLocalAndRemoteCacheData

        let started = Date()
        do {
            let (_, response) = try await URLSession.shared.data(for: request)
            let latency = Int(Date().timeIntervalSince(started) * 1000)
            if let http = response as? HTTPURLResponse {
                return .init(
                    id: endpoint.label, label: endpoint.label, host: host,
                    statusCode: http.statusCode, latencyMilliseconds: latency,
                    reachable: true,
                    detail: "HTTPS reachable"
                )
            }
            return .init(
                id: endpoint.label, label: endpoint.label, host: host,
                statusCode: nil, latencyMilliseconds: latency,
                reachable: true,
                detail: "Transport reachable"
            )
        } catch {
            return .init(
                id: endpoint.label, label: endpoint.label, host: host,
                statusCode: nil, latencyMilliseconds: nil, reachable: false,
                detail: "Network/TLS unavailable"
            )
        }
    }
}

@MainActor
struct ChinaPolestarSettingsCard: View {
    let onSettingsChanged: (SettingsChange) -> Void

    @State private var region = ChinaPolestarRuntime.currentRegion
    @State private var diagnostics: [ChinaPolestarDiagnosticProbe] = []
    @State private var isRunningDiagnostics = false

    var body: some View {
        Card {
            VStack(alignment: .leading, spacing: 12) {
                CardHeader(
                    symbol: "globe.asia.australia.fill",
                    title: "Polestar Region",
                    color: HisingenTheme.accent
                )

                Picker("Region", selection: $region) {
                    ForEach(ChinaPolestarRegion.allCases, id: \.self) { item in
                        Text(item.title).tag(item)
                    }
                }
                .pickerStyle(.segmented)
                .onChange(of: region) { _, newValue in
                    ChinaPolestarRuntime.setRegion(newValue)
                    diagnostics = []
                    onSettingsChanged(.credentials)
                }

                if region == .global {
                    globalStatus
                } else {
                    chinaStatus
                }
            }
        }
    }

    private var globalStatus: some View {
        VStack(alignment: .leading, spacing: 6) {
            statusRow(
                symbol: "checkmark.circle.fill",
                title: "Global provider",
                detail: "Original Hisingen Polestar ID / Data Portal path",
                good: true
            )
            Text("Existing Global sign-in, Keychain data and provider logic remain unchanged.")
                .hisType(.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var chinaStatus: some View {
        VStack(alignment: .leading, spacing: 9) {
            statusRow(
                symbol: "safari.fill",
                title: "Official web sign-in",
                detail: "Available · persistent WebKit website session",
                good: true
            )
            statusRow(
                symbol: "car.side.fill",
                title: "Vehicle API",
                detail: "Not connected · verified China API session mapping pending",
                good: false
            )
            statusRow(
                symbol: "antenna.radiowaves.left.and.right",
                title: "China C3 provider",
                detail: "Read-only boundary prepared · no Global token reuse",
                good: true
            )
            statusRow(
                symbol: "clock.badge.questionmark",
                title: "Data freshness",
                detail: "Unavailable until verified live/current vehicle data is connected",
                good: false
            )

            HStack(spacing: 8) {
                Button {
                    ChinaPolestarRuntime.openChinaWebSignIn()
                } label: {
                    Label("Open Official China Sign-In", systemImage: "safari")
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.small)

                Button {
                    runDiagnostics()
                } label: {
                    if isRunningDiagnostics {
                        ProgressView().controlSize(.mini)
                    } else {
                        Label("Run Network Diagnostics", systemImage: "stethoscope")
                    }
                }
                .controlSize(.small)
                .disabled(isRunningDiagnostics)
            }

            Text("Website sign-in and vehicle API authentication are intentionally shown as separate states. This preview does not inspect cookies, form fields or tokens.")
                .hisType(.caption)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            if !diagnostics.isEmpty {
                Divider().opacity(HisingenTheme.dividerOpacity)
                VStack(alignment: .leading, spacing: 6) {
                    Text("China Backend Diagnostics")
                        .hisType(.label, weight: .semibold)
                    ForEach(diagnostics) { item in
                        diagnosticRow(item)
                    }
                }
            }
        }
    }

    private func statusRow(symbol: String, title: String, detail: String, good: Bool) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: symbol)
                .foregroundStyle(good ? HisingenTheme.semanticGood : HisingenTheme.semanticWarning)
                .frame(width: 18)
            VStack(alignment: .leading, spacing: 1) {
                Text(title).hisType(.body, weight: .medium)
                Text(detail)
                    .hisType(.caption)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer()
        }
    }

    private func diagnosticRow(_ item: ChinaPolestarDiagnosticProbe) -> some View {
        HStack(spacing: 8) {
            Circle()
                .fill(item.reachable ? HisingenTheme.semanticGood : HisingenTheme.semanticCritical)
                .frame(width: 7, height: 7)
            VStack(alignment: .leading, spacing: 1) {
                Text(item.label).hisType(.caption, weight: .semibold)
                Text(item.host).hisType(.micro).foregroundStyle(.secondary)
            }
            Spacer()
            if let code = item.statusCode {
                Text("HTTP \(code)").hisType(.micro).foregroundStyle(.secondary)
            }
            if let latency = item.latencyMilliseconds {
                Text("\(latency) ms").hisType(.micro).foregroundStyle(.secondary)
            }
        }
    }

    private func runDiagnostics() {
        isRunningDiagnostics = true
        diagnostics = []
        Task {
            let result = await ChinaPolestarDiagnostics.run()
            diagnostics = result
            isRunningDiagnostics = false
        }
    }
}

@MainActor
struct ChinaPreviewUpdatesCard: View {
    var body: some View {
        Card {
            VStack(alignment: .leading, spacing: 10) {
                CardHeader(
                    symbol: "shield.checkered",
                    title: "Hisingen China Preview Updates",
                    color: HisingenTheme.semanticActive
                )
                Text("Automatic and manual Sparkle updates are disabled in this preview so an upstream Hisingen release cannot overwrite the China integration.")
                    .hisType(.caption)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)

                HStack {
                    Label("Update channel", systemImage: "shippingbox.fill")
                        .hisType(.body, weight: .medium)
                    Spacer()
                    Text("Polestar-CN-API · Preview 0.17.0")
                        .hisType(.caption, weight: .semibold)
                        .foregroundStyle(HisingenTheme.accent)
                }

                Button {
                    ChinaPolestarRuntime.presentUpdatePolicyNotice()
                } label: {
                    Label("Update Policy", systemImage: "info.circle")
                }
                .controlSize(.small)
            }
        }
    }
}
