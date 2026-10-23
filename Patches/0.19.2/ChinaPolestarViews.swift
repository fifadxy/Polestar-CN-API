import AppKit
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
        .init(label: "Official China Web", url: "https://www.polestar.cn/zh-cn/auth/login/"),
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

    @State private var hasSession = ChinaPolestarSessionStore.hasStoredSession
    @State private var maskedPhone: String?
    @State private var vehicleCount = 0

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            VStack(alignment: .leading, spacing: 3) {
                Label("极星中国区 · 网页登录", systemImage: "safari")
                    .hisType(.body, weight: .semibold)
                Text("使用中国区官方网页，不再调用缺少公共参数的短信接口。")
                    .hisType(.caption)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            HStack(spacing: 8) {
                Button {
                    ChinaPolestarWebSignInWindow.shared.open()
                } label: {
                    Label("打开中国区登录窗口", systemImage: "safari.fill")
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.small)

                Button {
                    NSWorkspace.shared.open(ChinaPolestarWebSignInWindow.officialLoginURL)
                } label: {
                    Label("在浏览器打开", systemImage: "arrow.up.right.square")
                }
                .controlSize(.small)
            }

            HStack(alignment: .top, spacing: 8) {
                Image(systemName: hasSession ? "key.horizontal.fill" : "info.circle")
                    .foregroundStyle(hasSession ? HisingenTheme.semanticGood : HisingenTheme.semanticWarning)
                VStack(alignment: .leading, spacing: 2) {
                    Text(hasSession ? "车联 API：存在已保存凭据" : "车联 API：尚未获得独立授权")
                        .hisType(.caption, weight: .semibold)
                    if hasSession {
                        let account = maskedPhone ?? "本地钥匙串"
                        let vehicles = vehicleCount > 0 ? " · 已发现 \(vehicleCount) 辆车" : " · 等待车辆校验"
                        Text("\(account)\(vehicles)")
                            .hisType(.caption)
                            .foregroundStyle(.secondary)
                    } else {
                        Text("网页登录成功并不代表获得 C3/PCCS 车辆访问权限。")
                            .hisType(.caption)
                            .foregroundStyle(.secondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }

            if hasSession {
                HStack(spacing: 8) {
                    Button {
                        onSettingsChanged(.credentials)
                    } label: {
                        Label("验证已保存的车联凭据", systemImage: "arrow.clockwise")
                    }
                    .controlSize(.small)

                    Button(role: .destructive) {
                        onSettingsChanged(.chinaSignOut)
                        hasSession = false
                        maskedPhone = nil
                        vehicleCount = 0
                    } label: {
                        Label("退出车联会话", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                    .controlSize(.small)
                }
            }

            Text("网页登录会话由 macOS WebKit 保存；不会导出 Cookie 或自动转换为车辆 API Token。已有的车联凭据仍保存在独立钥匙串中。")
                .hisType(.micro)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .task { await reloadStatus() }
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
                    Label("Official China web sign-in · separate C3 API session", systemImage: "key.horizontal.fill")
                        .hisType(.caption, weight: .medium)
                        .foregroundStyle(HisingenTheme.semanticGood)

                    Text("网页登录与车联 API 凭据独立，Global 登录不受影响。")
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
                    Text("Polestar-CN-API · 0.19.2")
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
