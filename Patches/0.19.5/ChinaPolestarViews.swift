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
    @State private var isVerifyingSession = false
    @State private var hasSession = ChinaPolestarSessionStore.hasStoredSession
    @State private var maskedPhone: String?
    @State private var vehicleCount = 0
    @State private var notice: String?
    @State private var noticeIsError = false
    @State private var hasOAuthMaterial = ChinaPolestarOAuthMaterialStore.hasStoredMaterial

    private var isBusy: Bool { isSendingCode || isSigningIn || isVerifyingSession }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            sessionBanner

            if hasSession {
                HStack(spacing: 8) {
                    Button {
                        verifySession()
                    } label: {
                        if isVerifyingSession {
                            ProgressView().controlSize(.mini)
                        } else {
                            Label("验证车联会话", systemImage: "checkmark.shield")
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.small)
                    .disabled(isBusy)

                    Button(role: .destructive) {
                        hasSession = false
                        maskedPhone = nil
                        vehicleCount = 0
                        notice = nil
                        onSettingsChanged(.chinaSignOut)
                    } label: {
                        Text("退出")
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

            Text("登录成功后的中国区会话保存在独立 macOS 钥匙串中；短信验证码与滑块结果仅用于当前请求，不保存。")
                .hisType(.micro)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .task {
            hasOAuthMaterial = ChinaPolestarOAuthMaterialStore.hasStoredMaterial
            await reloadStatus()
        }
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
                Text(hasSession ? "已连接 · Polestar China" : "未连接 · Polestar China")
                    .hisType(.body, weight: .semibold)
                if hasSession {
                    let account = maskedPhone ?? "Keychain session"
                    let vehicles = vehicleCount > 0 ? " · \(vehicleCount) vehicle\(vehicleCount == 1 ? "" : "s")" : ""
                    Text("\(account)\(vehicles)")
                        .hisType(.caption)
                        .foregroundStyle(.secondary)
                } else {
                    Text("使用极星中国官方 App 相同的手机号 + 短信验证码链路登录。")
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
            HStack(alignment: .top, spacing: 7) {
                Image(systemName: hasOAuthMaterial ? "checkmark.shield.fill" : "info.circle.fill")
                    .foregroundStyle(hasOAuthMaterial ? HisingenTheme.semanticGood : HisingenTheme.semanticWarning)
                VStack(alignment: .leading, spacing: 2) {
                    Text(hasOAuthMaterial ? "OAuth 自动刷新：已就绪" : "OAuth 自动刷新：尚未配置")
                        .hisType(.caption, weight: .semibold)
                    if !hasOAuthMaterial {
                        Text("首次短信登录不需要额外材料；为支持本地令牌到期后的自动刷新，可运行安装包中的 provision_cn_oauth_secret.py，从你自己的官方 Android DEX 本地导入。")
                            .hisType(.micro)
                            .foregroundStyle(.secondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }

            VStack(alignment: .leading, spacing: 4) {
                Text("中国大陆手机号")
                    .hisType(.caption, weight: .semibold)
                    .foregroundStyle(.secondary)
                HStack(spacing: 8) {
                    TextField("11 位手机号", text: $phone)
                        .textFieldStyle(.roundedBorder)
                        .disabled(isBusy)
                    Button {
                        sendCode()
                    } label: {
                        if isSendingCode {
                            ProgressView().controlSize(.mini)
                        } else {
                            Text(codeRequested ? "重新发送" : "获取验证码")
                        }
                    }
                    .controlSize(.small)
                    .disabled(isBusy || phone.filter(\.isNumber).count < 11)
                }
            }

            if codeRequested {
                VStack(alignment: .leading, spacing: 4) {
                    Text("短信验证码")
                        .hisType(.caption, weight: .semibold)
                        .foregroundStyle(.secondary)
                    HStack(spacing: 8) {
                        TextField("验证码", text: $verificationCode)
                            .textFieldStyle(.roundedBorder)
                            .disabled(isBusy)
                            .onSubmit { signIn() }
                        Button {
                            signIn()
                        } label: {
                            if isSigningIn {
                                ProgressView().controlSize(.mini)
                            } else {
                                Label("登录", systemImage: "arrow.right.circle.fill")
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

    private func sendCode(challenge: ChinaPolestarChallengePayload? = nil) {
        notice = nil

        // The official Android flow obtains sessionId/sig/token from the H5 slider first,
        // then sends the verification-code request. Do not probe the gateway with null values.
        guard let challenge else {
            isSendingCode = true
            notice = "请完成极星中国官方滑块验证。"
            noticeIsError = false
            ChinaPolestarChallengeWindow.shared.open { result in
                guard let result else {
                    isSendingCode = false
                    notice = "滑块验证已取消。"
                    noticeIsError = true
                    return
                }
                sendCode(challenge: result)
            }
            return
        }

        isSendingCode = true
        Task {
            do {
                try await ChinaPolestarAPI.shared.requestVerificationCode(
                    phone: phone,
                    challenge: challenge
                )
                codeRequested = true
                notice = "验证码已发送。"
                noticeIsError = false
                isSendingCode = false
            } catch ChinaPolestarSignInError.captchaRequired {
                isSendingCode = false
                notice = "验证已失效，请重新完成官方滑块验证。"
                noticeIsError = false
                ChinaPolestarChallengeWindow.shared.open { result in
                    guard let result else {
                        notice = "滑块验证已取消。"
                        noticeIsError = true
                        return
                    }
                    sendCode(challenge: result)
                }
            } catch {
                notice = error.localizedDescription
                noticeIsError = true
                isSendingCode = false
            }
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
                if count == 0 {
                    notice = "登录成功，未发现已绑定车辆。"
                } else {
                    notice = count == 1 ? "登录成功，已发现 1 辆车。" : "登录成功，已发现 \(count) 辆车。"
                }
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

    private func verifySession() {
        isVerifyingSession = true
        notice = nil
        Task {
            do {
                let status = try await ChinaPolestarAPI.shared.verifyStoredSession()
                hasSession = status.signedIn
                maskedPhone = status.maskedPhone
                vehicleCount = status.vehicleCount
                notice = status.vehicleCount == 0
                    ? "会话有效，未发现已绑定车辆。"
                    : "会话有效，已发现 \(status.vehicleCount) 辆车。"
                noticeIsError = false
                onSettingsChanged(.credentials)
            } catch {
                notice = error.localizedDescription
                noticeIsError = true
                await reloadStatus()
            }
            isVerifyingSession = false
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
                    Label("中国账号 · 手机号 + 短信验证码 · China C3", systemImage: "key.horizontal.fill")
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
                            Label("Test China Network Routes", systemImage: "stethoscope")
                        }
                    }
                    .controlSize(.small)
                    .disabled(isRunningDiagnostics)

                    Text("仅检查 DNS、TLS 与 HTTP 可达性，不验证短信登录、令牌权限或具体 gRPC 方法。C3 车辆服务地址由 discovery 动态返回。")
                        .hisType(.micro)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)

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
                                    Text("Reached · HTTP \(code)").hisType(.micro).foregroundStyle(.secondary)
                                } else {
                                    Text("No response")
                                        .hisType(.micro)
                                        .foregroundStyle(HisingenTheme.semanticCritical)
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
                    Text("Polestar-CN-API · 0.19.5")
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
