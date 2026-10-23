import Foundation
import SwiftUI

struct ChinaPolestarDiagnosticProbe: Identifiable, Sendable, Equatable {
    let id: String
    let label: String
    let host: String
    let statusCode: Int?
    let latencyMilliseconds: Int?
    let reachable: Bool
}

enum ChinaPolestarDiagnostics {
    private struct Endpoint: Sendable {
        let label: String
        let url: String
    }

    private static let endpoints: [Endpoint] = [
        .init(label: "Account Gateway", url: "https://gw.app.polestar-auto.com"),
        .init(label: "User Center", url: "https://uc.app.polestar-auto.com"),
        .init(label: "C3 Discovery", url: "https://cnepmob.volvocars.com.cn"),
        .init(label: "C3 Vehicle Service", url: "https://cepmobtoken.prod.c3.volvocars.com.cn"),
        .init(label: "PCCS China", url: "https://api.pccs-prod.origin.polestar.cn")
    ]

    static func run() async -> [ChinaPolestarDiagnosticProbe] {
        await withTaskGroup(of: ChinaPolestarDiagnosticProbe.self) { group in
            for endpoint in endpoints {
                group.addTask { await probe(endpoint) }
            }
            var results: [ChinaPolestarDiagnosticProbe] = []
            for await result in group { results.append(result) }
            let order = Dictionary(
                uniqueKeysWithValues: endpoints.enumerated().map { ($0.element.label, $0.offset) }
            )
            return results.sorted { (order[$0.label] ?? 999) < (order[$1.label] ?? 999) }
        }
    }

    private static func probe(_ endpoint: Endpoint) async -> ChinaPolestarDiagnosticProbe {
        guard let url = URL(string: endpoint.url), let host = url.host else {
            return .init(
                id: endpoint.label, label: endpoint.label, host: endpoint.url,
                statusCode: nil, latencyMilliseconds: nil, reachable: false
            )
        }
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.timeoutInterval = 10
        request.cachePolicy = .reloadIgnoringLocalAndRemoteCacheData
        let start = Date()
        do {
            let (_, response) = try await URLSession.shared.data(for: request)
            return .init(
                id: endpoint.label,
                label: endpoint.label,
                host: host,
                statusCode: (response as? HTTPURLResponse)?.statusCode,
                latencyMilliseconds: Int(Date().timeIntervalSince(start) * 1_000),
                reachable: true
            )
        } catch {
            return .init(
                id: endpoint.label, label: endpoint.label, host: host,
                statusCode: nil, latencyMilliseconds: nil, reachable: false
            )
        }
    }
}

@MainActor
struct ChinaPolestarAccountView: View {
    let onSettingsChanged: (SettingsChange) -> Void

    @State private var phone = ""
    @State private var verificationCode = ""
    @State private var codeRequested = false
    @State private var isSendingCode = false
    @State private var isSigningIn = false
    @State private var hasSession = ChinaPolestarSessionStore.hasStoredSession
    @State private var maskedPhone: String?
    @State private var vehicleCount = 0
    @State private var notice: String?
    @State private var noticeIsError = false

    private var isBusy: Bool { isSendingCode || isSigningIn }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            sessionBanner

            if hasSession {
                HStack(spacing: 8) {
                    Button {
                        notice = nil
                        onSettingsChanged(.credentials)
                    } label: {
                        Label("Refresh Session", systemImage: "arrow.clockwise")
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.small)

                    Button(role: .destructive) {
                        hasSession = false
                        maskedPhone = nil
                        vehicleCount = 0
                        notice = nil
                        onSettingsChanged(.chinaSignOut)
                    } label: {
                        Text("Sign Out")
                    }
                    .controlSize(.small)
                }
            } else {
                signInFields
            }

            if let notice {
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Image(systemName: noticeIsError ? "exclamationmark.triangle.fill" : "checkmark.circle.fill")
                        .foregroundStyle(
                            noticeIsError ? HisingenTheme.semanticCritical : HisingenTheme.semanticGood
                        )
                    Text(notice)
                        .hisType(.caption)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .transition(.opacity)
            }

            Text("The phone number and session tokens stay in the macOS Keychain. Verification codes are used only for the current request and are never saved.")
                .hisType(.micro)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .task { await reloadStatus() }
    }

    private var sessionBanner: some View {
        let color = hasSession ? HisingenTheme.semanticGood : HisingenTheme.semanticWarning
        return HStack(spacing: 10) {
            if isBusy {
                ProgressView().controlSize(.mini)
                    .frame(width: 10, height: 10)
            } else {
                Circle().fill(color)
                    .frame(width: 8, height: 8)
                    .frame(width: 10, height: 10)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(hasSession ? "Connected · Polestar China" : "Not Connected · Polestar China")
                    .hisType(.body, weight: .semibold)
                if hasSession {
                    let account = maskedPhone ?? "Keychain session"
                    let vehicles = vehicleCount > 0 ? " · \(vehicleCount) vehicle\(vehicleCount == 1 ? "" : "s")" : ""
                    Text("\(account)\(vehicles)")
                        .hisType(.caption)
                        .foregroundStyle(.secondary)
                } else {
                    Text("Sign in with the phone number and SMS code used by Polestar China.")
                        .hisType(.caption)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            Spacer()
        }
        .padding(10)
        .background(color.opacity(0.08), in: RoundedRectangle(cornerRadius: 8))
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(color.opacity(0.25), lineWidth: 0.5)
        )
    }

    private var signInFields: some View {
        VStack(alignment: .leading, spacing: 8) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Phone Number")
                    .hisType(.caption, weight: .semibold)
                    .foregroundStyle(.secondary)
                HStack(spacing: 8) {
                    TextField("Mainland China mobile number", text: $phone)
                        .textFieldStyle(.roundedBorder)
                        .disabled(isBusy)
                    Button {
                        sendCode()
                    } label: {
                        if isSendingCode {
                            ProgressView().controlSize(.mini)
                        } else {
                            Text(codeRequested ? "Send Again" : "Send Code")
                        }
                    }
                    .controlSize(.small)
                    .disabled(isBusy || phone.filter(\.isNumber).count < 11)
                }
            }

            if codeRequested {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Verification Code")
                        .hisType(.caption, weight: .semibold)
                        .foregroundStyle(.secondary)
                    HStack(spacing: 8) {
                        TextField("SMS code", text: $verificationCode)
                            .textFieldStyle(.roundedBorder)
                            .disabled(isBusy)
                            .onSubmit { signIn() }
                        Button {
                            signIn()
                        } label: {
                            if isSigningIn {
                                ProgressView().controlSize(.mini)
                            } else {
                                Label("Sign In", systemImage: "arrow.right.circle.fill")
                            }
                        }
                        .buttonStyle(.borderedProminent)
                        .controlSize(.small)
                        .disabled(isBusy || verificationCode.count < 4)
                    }
                }
            }
        }
    }

    private func sendCode() {
        isSendingCode = true
        notice = nil
        Task {
            do {
                try await ChinaPolestarAPI.shared.requestVerificationCode(phone: phone)
                codeRequested = true
                notice = "Verification code requested."
                noticeIsError = false
            } catch {
                notice = error.localizedDescription
                noticeIsError = true
            }
            isSendingCode = false
        }
    }

    private func signIn() {
        isSigningIn = true
        notice = nil
        Task {
            do {
                let count = try await ChinaPolestarAPI.shared.completePhoneSignIn(
                    phone: phone,
                    code: verificationCode
                )
                verificationCode = ""
                hasSession = true
                vehicleCount = count
                notice = count == 1 ? "Signed in and found 1 vehicle." : "Signed in and found \(count) vehicles."
                noticeIsError = false
                await reloadStatus()
                onSettingsChanged(.credentials)
            } catch {
                notice = error.localizedDescription
                noticeIsError = true
                await reloadStatus()
            }
            isSigningIn = false
        }
    }

    private func reloadStatus() async {
        let status = await ChinaPolestarAPI.shared.accountStatus()
        hasSession = status.signedIn
        maskedPhone = status.maskedPhone
        vehicleCount = status.vehicleCount
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
                    Text("China").tag(ChinaPolestarRegion.mainlandChina)
                    Text("Global").tag(ChinaPolestarRegion.global)
                }
                .pickerStyle(.segmented)
                .onChange(of: region) { _, newValue in
                    ChinaPolestarRuntime.setRegion(newValue)
                    diagnostics = []
                }

                if region == .global {
                    Label(
                        "Original Hisingen Polestar ID, Data Portal and Augmented providers",
                        systemImage: "checkmark.circle.fill"
                    )
                    .hisType(.caption, weight: .medium)
                    .foregroundStyle(HisingenTheme.semanticGood)
                } else {
                    Label("Native macOS session · Phone + SMS · China C3", systemImage: "key.horizontal.fill")
                        .hisType(.caption, weight: .medium)
                        .foregroundStyle(HisingenTheme.semanticGood)

                    Text("China uses its own Keychain session and China service hosts. Global credentials and the Global provider stay separate.")
                        .hisType(.caption)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)

                    Button {
                        isRunningDiagnostics = true
                        Task {
                            diagnostics = await ChinaPolestarDiagnostics.run()
                            isRunningDiagnostics = false
                        }
                    } label: {
                        if isRunningDiagnostics {
                            ProgressView().controlSize(.mini)
                        } else {
                            Label("Run China Backend Diagnostics", systemImage: "stethoscope")
                        }
                    }
                    .controlSize(.small)
                    .disabled(isRunningDiagnostics)

                    if !diagnostics.isEmpty {
                        Divider().opacity(HisingenTheme.dividerOpacity)
                        ForEach(diagnostics) { item in
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
                                if let milliseconds = item.latencyMilliseconds {
                                    Text("\(milliseconds) ms").hisType(.micro).foregroundStyle(.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
        .onReceive(
            NotificationCenter.default.publisher(for: .hisingenChinaPolestarRegionDidChange)
        ) { _ in
            region = ChinaPolestarRuntime.currentRegion
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
                    title: "Hisingen China Updates",
                    color: HisingenTheme.semanticActive
                )
                Text("Upstream Sparkle updates are disabled so they cannot replace the China provider.")
                    .hisType(.caption)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
                HStack {
                    Label("Update channel", systemImage: "shippingbox.fill")
                        .hisType(.body, weight: .medium)
                    Spacer()
                    Text("Polestar-CN-API · 0.19.1")
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
