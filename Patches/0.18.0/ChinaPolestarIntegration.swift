import AppKit
import Foundation
import SwiftUI
import WebKit

// Hisingen China Preview 0.18.0
//
// Region and provider rules:
// - Global uses the original Hisingen Polestar provider without modification.
// - Mainland China uses a separate provider.
// - The currently operational China read-only route is a loopback bridge backed by the
//   user's already signed-in official China Android app. It does not export bearer tokens,
//   cookies, SMS codes, VINs, phone numbers, Digital Key secrets, or precise location.
// - Native macOS China OAuth/C3 token exchange remains MAPPING_REQUIRED.
// - China remote commands remain disabled until request/authorization/result semantics
//   are independently verified end-to-end.

enum ChinaPolestarRegion: String, CaseIterable, Sendable {
    case mainlandChina
    case global

    var title: String {
        switch self {
        case .mainlandChina: return "China"
        case .global: return "Global"
        }
    }
}

enum ChinaPolestarSnapshotSource: String, Codable, Sendable {
    case androidAppLive = "Android App Live"
    case liveC3 = "Live C3"
    case latestC3 = "C3 Latest"
    case cache = "Cached"
}

extension Notification.Name {
    static let hisingenChinaPolestarRegionDidChange =
        Notification.Name("io.kheirallah.hisingen.cnpreview.region-did-change")
}

enum ChinaPolestarRuntime {
    nonisolated static let patchVersion = "0.18.0"
    nonisolated static let regionKey = "hisingen.polestar.signInRegion"
    nonisolated static let chooserVersionKey = "hisingen.polestar.regionChooserVersion"
    nonisolated static let chooserVersion = 1
    nonisolated static let bridgeURL = URL(string: "http://127.0.0.1:28765")!

    nonisolated static var currentRegion: ChinaPolestarRegion {
        let raw = UserDefaults.standard.string(forKey: regionKey)
        return raw == ChinaPolestarRegion.mainlandChina.rawValue ? .mainlandChina : .global
    }

    @MainActor
    static func install() {
        // 0.17.0 wrote a default Global region automatically. Do not treat that write as
        // explicit consent: chooserVersion is new in 0.18.0 so upgrading users still see
        // the requested first-launch region choice exactly once.
    }

    @MainActor
    static func ensureInitialRegionSelection() {
        if ProcessInfo.processInfo.environment["HISINGEN_CI_SMOKE"] == "1" {
            if UserDefaults.standard.object(forKey: regionKey) == nil {
                UserDefaults.standard.set(ChinaPolestarRegion.global.rawValue, forKey: regionKey)
            }
            return
        }
        guard UserDefaults.standard.integer(forKey: chooserVersionKey) < chooserVersion else { return }

        NSApp.activate(ignoringOtherApps: true)
        let alert = NSAlert()
        alert.alertStyle = .informational
        alert.messageText = "Select Polestar Region"
        alert.informativeText =
            "Choose the Polestar account region used by Hisingen. You can switch between China and Global later from the Hisingen menu or Settings."
        alert.addButton(withTitle: "China")
        alert.addButton(withTitle: "Global")

        let response = alert.runModal()
        let selected: ChinaPolestarRegion = response == .alertFirstButtonReturn ? .mainlandChina : .global
        setRegion(selected, notify: false)
        UserDefaults.standard.set(chooserVersion, forKey: chooserVersionKey)
    }

    @MainActor
    static func setRegion(_ region: ChinaPolestarRegion, notify: Bool = true) {
        guard currentRegion != region || UserDefaults.standard.object(forKey: regionKey) == nil else {
            return
        }
        UserDefaults.standard.set(region.rawValue, forKey: regionKey)
        if notify {
            NotificationCenter.default.post(name: .hisingenChinaPolestarRegionDidChange, object: nil)
        }
    }

    @MainActor
    static func enforcePreviewUpdatePolicy(_ preferences: PreferencesStore) {
        preferences.automaticallyChecksForUpdates = false
        preferences.automaticallyDownloadsUpdates = false
    }

    @MainActor
    static func openChinaWebSignIn() {
        ChinaPolestarWebSignInWindow.shared.open()
    }

    @MainActor
    static func presentUpdatePolicyNotice() {
        let alert = NSAlert()
        alert.messageText = "Updates are disabled in Hisingen China Preview"
        alert.informativeText =
            "This preview does not install upstream Hisingen updates because they would overwrite the China integration. Install a newer Polestar-CN-API China build instead."
        alert.addButton(withTitle: "OK")
        alert.runModal()
    }

    static func bridgeReachable() async -> Bool {
        struct BridgeStatus: Decodable {
            let ready: Bool
            let batteryAvailable: Bool
        }
        do {
            var request = URLRequest(url: bridgeURL.appendingPathComponent("v1/status"))
            request.timeoutInterval = 2
            request.cachePolicy = .reloadIgnoringLocalAndRemoteCacheData
            let (data, response) = try await URLSession.shared.data(for: request)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { return false }
            let status = try JSONDecoder().decode(BridgeStatus.self, from: data)
            return status.ready && status.batteryAvailable
        } catch {
            return false
        }
    }
}

@MainActor
final class ChinaPolestarRegionMenuController: NSObject {
    static let shared = ChinaPolestarRegionMenuController()

    private var chinaItem: NSMenuItem?
    private var globalItem: NSMenuItem?
    private var observer: NSObjectProtocol?

    func install() {
        guard let appMenu = NSApp.mainMenu?.items.first?.submenu else { return }
        if appMenu.items.contains(where: { $0.identifier?.rawValue == "hisingen.polestar.region" }) {
            updateChecks()
            return
        }

        let parent = NSMenuItem(title: "Polestar Region", action: nil, keyEquivalent: "")
        parent.identifier = NSUserInterfaceItemIdentifier("hisingen.polestar.region")
        let submenu = NSMenu(title: "Polestar Region")

        let china = NSMenuItem(title: "China", action: #selector(selectChina), keyEquivalent: "")
        china.target = self
        let global = NSMenuItem(title: "Global", action: #selector(selectGlobal), keyEquivalent: "")
        global.target = self
        submenu.addItem(china)
        submenu.addItem(global)
        parent.submenu = submenu

        let settingsIndex = appMenu.items.firstIndex(where: { $0.action == Selector(("openSettingsMenuItem")) }) ?? 0
        appMenu.insertItem(parent, at: min(settingsIndex + 1, appMenu.items.count))

        chinaItem = china
        globalItem = global
        updateChecks()

        observer = NotificationCenter.default.addObserver(
            forName: .hisingenChinaPolestarRegionDidChange,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in self?.updateChecks() }
        }
    }

    private func updateChecks() {
        chinaItem?.state = ChinaPolestarRuntime.currentRegion == .mainlandChina ? .on : .off
        globalItem?.state = ChinaPolestarRuntime.currentRegion == .global ? .on : .off
    }

    @objc private func selectChina() {
        ChinaPolestarRuntime.setRegion(.mainlandChina)
    }

    @objc private func selectGlobal() {
        ChinaPolestarRuntime.setRegion(.global)
    }
}

@MainActor
final class ChinaPolestarWebSignInWindow: NSObject {
    static let shared = ChinaPolestarWebSignInWindow()
    private var window: NSWindow?

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
        window = newWindow
        newWindow.makeKeyAndOrderFront(nil)
        NSApp.activate(ignoringOtherApps: true)
    }
}

enum ChinaPolestarProviderError: LocalizedError, Sendable {
    case bridgeUnavailable
    case bridgeSessionUnavailable
    case invalidBridgeResponse
    case vehicleNotResolved
    case readOnlyPreview

    var errorDescription: String? {
        switch self {
        case .bridgeUnavailable:
            return "China local bridge is not running. Start the read-only Android App bridge, then retry."
        case .bridgeSessionUnavailable:
            return "The bridge is running, but the official China Polestar Android app is not exposing an active signed-in vehicle session."
        case .invalidBridgeResponse:
            return "The China bridge returned an unsupported response."
        case .vehicleNotResolved:
            return "No China vehicle is available from the bridge."
        case .readOnlyPreview:
            return "China Preview is read-only. China remote commands remain disabled until their API mapping is verified."
        }
    }
}

private struct ChinaBridgeEnvelope: Codable, Sendable {
    struct Session: Codable, Sendable {
        var ready: Bool
        var source: String
    }

    struct Vehicle: Codable, Sendable {
        var id: String
        var title: String
        var modelName: String?
        var modelYear: String?
    }

    struct Battery: Codable, Sendable {
        var reportedAtMs: Int64?
        var percentage: Double?
        var rangeKm: Int?
        var rangeMi: Int?
        var estimatedChargingTimeMinutes: Int?
        var averageConsumption: Double?
        var chargingState: String?
        var chargingPowerWatts: Int?
        var chargingCurrentAmps: Int?
        var chargingVoltageVolts: Int?
        var chargingType: String?
        var chargerPowerStatus: String?
    }

    struct Exterior: Codable, Sendable {
        var reportedAtMs: Int64?
        var isLocked: Bool?
        var alarmTriggered: Bool?
        var openings: [String: String]?
    }

    var version: Int
    var session: Session
    var vehicle: Vehicle?
    var battery: Battery?
    var exterior: Exterior?
    var odometerKm: Double?
    var source: ChinaPolestarSnapshotSource?
}

actor ChinaPolestarAPI: VehicleProviding, VehicleLiveStreaming {
    nonisolated let brand: VehicleBrand = .polestar

    private let baseURL = ChinaPolestarRuntime.bridgeURL
    private var knownCars: [CarSummary] = []

    var cars: [CarSummary] { knownCars }
    var hasWarmSession: Bool { !knownCars.isEmpty }
    var acceptsStoredPasswordSignIn: Bool { false }

    func authenticate(
        email: String,
        password: String,
        preferredVIN: String?,
        features: FeatureSelection
    ) async throws {
        try await restoreSession(preferredVIN: preferredVIN, features: features)
    }

    func restoreSession(preferredVIN: String?, features: FeatureSelection) async throws {
        let snapshot = try await fetchEnvelope()
        guard snapshot.session.ready else { throw ChinaPolestarProviderError.bridgeSessionUnavailable }
        guard let vehicle = snapshot.vehicle else { throw ChinaPolestarProviderError.vehicleNotResolved }
        knownCars = [
            CarSummary(
                vin: vehicle.id,
                title: vehicle.title,
                modelName: vehicle.modelName,
                modelYear: vehicle.modelYear
            )
        ]
    }

    func resetSession() async {
        knownCars = []
    }

    func signOut() async throws {
        knownCars = []
    }

    func resolvedVIN(preferred: String?) async -> String? {
        if let preferred, knownCars.contains(where: { $0.vin == preferred }) { return preferred }
        return knownCars.first?.vin
    }

    func reloadVehicleMetadata(vin: String, features: FeatureSelection) async throws {
        try await restoreSession(preferredVIN: vin, features: features)
    }

    func fetchVehicleState(vin: String, features: FeatureSelection) async throws -> VehicleState {
        let envelope = try await fetchEnvelope()
        guard envelope.session.ready else { throw ChinaPolestarProviderError.bridgeSessionUnavailable }
        guard let vehicle = envelope.vehicle else { throw ChinaPolestarProviderError.vehicleNotResolved }
        return makeVehicleState(envelope, vehicle: vehicle)
    }

    func identitySnapshot(for vin: String, features: FeatureSelection) async -> VehicleIdentitySnapshot? {
        guard let car = knownCars.first(where: { $0.vin == vin }) else { return nil }
        return VehicleIdentitySnapshot(
            availability: .unknown,
            modelName: car.modelName,
            modelYear: car.modelYear,
            vin: car.vin,
            accountMarket: "CN"
        )
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

    func refreshLiveStreamAuthorization() async throws {
        let envelope = try await fetchEnvelope()
        guard envelope.session.ready else { throw ChinaPolestarProviderError.bridgeSessionUnavailable }
    }

    func liveVehicleUpdates(
        vin: String,
        purpose: VehicleLiveStreamPurpose
    ) async throws -> AsyncThrowingStream<VehicleLiveUpdate, Error> {
        AsyncThrowingStream { continuation in
            let task = Task {
                // Local Android snapshot polling, not a direct China C3 gRPC stream.
                continuation.yield(.connected(activeTransportStreams: 0))
                do {
                    while !Task.isCancelled {
                        let envelope = try await self.fetchEnvelope()
                        // Do not push old Android memory as if it were a fresh live frame.
                        if envelope.source != .cache, let battery = envelope.battery {
                            continuation.yield(.battery(self.makeBatteryExtras(battery)))
                        }
                        if envelope.source != .cache, let exterior = envelope.exterior {
                            let snapshot = self.makeExterior(exterior)
                            continuation.yield(.exterior(snapshot, reportedAt: snapshot.reportedAt))
                        }
                        try await Task.sleep(for: .seconds(5))
                    }
                } catch is CancellationError {
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }

    private func fetchEnvelope() async throws -> ChinaBridgeEnvelope {
        var request = URLRequest(url: baseURL.appendingPathComponent("v1/state"))
        request.timeoutInterval = 4
        request.cachePolicy = .reloadIgnoringLocalAndRemoteCacheData
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
                throw ChinaPolestarProviderError.bridgeUnavailable
            }
            let envelope = try JSONDecoder().decode(ChinaBridgeEnvelope.self, from: data)
            guard envelope.version == 1 else { throw ChinaPolestarProviderError.invalidBridgeResponse }
            return envelope
        } catch let error as ChinaPolestarProviderError {
            throw error
        } catch {
            throw ChinaPolestarProviderError.bridgeUnavailable
        }
    }

    nonisolated private func makeVehicleState(
        _ envelope: ChinaBridgeEnvelope,
        vehicle: ChinaBridgeEnvelope.Vehicle
    ) -> VehicleState {
        let now = Date()
        let batteryDate = envelope.battery?.reportedAtMs.map {
            Date(timeIntervalSince1970: Double($0) / 1000.0)
        }
        let exteriorDate = envelope.exterior?.reportedAtMs.map {
            Date(timeIntervalSince1970: Double($0) / 1000.0)
        }

        var dates: [VehicleReading: Date] = [:]
        if let batteryDate {
            dates[.battery] = batteryDate
            dates[.range] = batteryDate
            dates[.charging] = batteryDate
        }
        if let exteriorDate {
            dates[.locks] = exteriorDate
            dates[.openings] = exteriorDate
        }
        // No verified China odometer report timestamp; never attribute it to "now".

        let battery = envelope.battery
        let chargingState = mapChargingState(battery?.chargingState)
        let connection = mapConnection(battery?.chargingState)

        let diagnostics = BatteryDiagnostics(
            timeToTargetMinutes: nil,
            timeToMinimumSOCMinutes: nil,
            timeToTargetDistanceMinutes: nil,
            chargerPowerState: mapPowerState(battery?.chargerPowerStatus),
            averageConsumption: battery?.averageConsumption,
            averageConsumptionSinceCharge: nil,
            energyUsedSinceChargeWh: nil
        )

        let energy = EnergyAndChargingSnapshot(
            batteryPercentage: battery?.percentage,
            rangeKm: battery?.rangeKm,
            chargingState: chargingState,
            estimatedTimeToFullMinutes: battery?.estimatedChargingTimeMinutes,
            powerWatts: battery?.chargingPowerWatts,
            currentAmps: battery?.chargingCurrentAmps,
            voltageVolts: battery?.chargingVoltageVolts,
            type: mapChargingType(battery?.chargingType),
            connection: connection,
            diagnostics: diagnostics
        )

        let identity = VehicleIdentitySnapshot(
            availability: .unknown,
            availabilityReportedAt: nil,
            modelName: vehicle.modelName,
            modelYear: vehicle.modelYear,
            vin: vehicle.id,
            accountMarket: "CN"
        )

        let reportedDate = batteryDate ?? exteriorDate
        let reportedAge = reportedDate.map { now.timeIntervalSince($0) }
        let isFresh = reportedAge.map { $0 >= -120 && $0 <= 600 } ?? false

        let freshness = SnapshotFreshness(
            isCached: envelope.source != .androidAppLive || !isFresh,
            fetchedAt: now,
            vehicleReportedAt: batteryDate ?? exteriorDate,
            readingDates: dates,
            dataWarnings: [
                "China read-only data is currently supplied by the signed-in official Android app bridge; native macOS China OAuth/C3 token exchange remains under verification."
            ]
        )

        return VehicleState(
            energy: energy,
            identity: identity,
            maintenance: MaintenanceAndHealthSnapshot(
                odometerKm: envelope.odometerKm.map { Int($0.rounded(.down)) },
                odometerKmPrecise: envelope.odometerKm
            ),
            freshness: freshness,
            exteriorStatus: envelope.exterior.map(makeExterior)
        )
    }

    nonisolated private func makeBatteryExtras(_ battery: ChinaBridgeEnvelope.Battery) -> GrpcBatteryExtras {
        let date = battery.reportedAtMs.map { Date(timeIntervalSince1970: Double($0) / 1000.0) }
        return GrpcBatteryExtras(
            reportedAt: date,
            batteryPercentage: battery.percentage,
            rangeKm: battery.rangeKm,
            estimatedChargingTimeToFullMinutes: battery.estimatedChargingTimeMinutes,
            chargingState: mapChargingState(battery.chargingState),
            chargerConnection: mapConnection(battery.chargingState),
            chargingType: mapChargingType(battery.chargingType),
            chargingPowerWatts: battery.chargingPowerWatts,
            chargingCurrentAmps: battery.chargingCurrentAmps,
            chargingVoltageVolts: battery.chargingVoltageVolts,
            diagnostics: BatteryDiagnostics(
                timeToTargetMinutes: nil,
                timeToMinimumSOCMinutes: nil,
                chargerPowerState: mapPowerState(battery.chargerPowerStatus),
                averageConsumption: battery.averageConsumption,
                averageConsumptionSinceCharge: nil,
                energyUsedSinceChargeWh: nil
            ),
            reportedBatteryCapacityKwh: nil,
            unknownFields: []
        )
    }

    nonisolated private func makeExterior(_ exterior: ChinaBridgeEnvelope.Exterior) -> ExteriorSnapshot {
        let openings = (exterior.openings ?? [:]).compactMap { key, value -> OpeningReading? in
            guard let opening = mapOpening(key) else { return nil }
            return OpeningReading(opening: opening, state: mapOpeningState(value))
        }
        return ExteriorSnapshot(
            openings: openings,
            isLocked: exterior.isLocked,
            alarmTriggered: exterior.alarmTriggered,
            reportedAt: exterior.reportedAtMs.map {
                Date(timeIntervalSince1970: Double($0) / 1000.0)
            }
        )
    }

    nonisolated private func mapChargingState(_ raw: String?) -> ChargingState {
        let value = (raw ?? "").uppercased()
        if value.contains("UNPLUGGED") || value.contains("NOPOWER") || value.contains("NO_POWER") {
            return .idle
        }
        if value.contains("CHARG") && !value.contains("NOT") { return .charging }
        if value.contains("PAUSE") { return .paused }
        if value.contains("SCHEDULE") { return .scheduled }
        if value.contains("DONE") || value.contains("COMPLETE") || value.contains("FULL") { return .complete }
        if value.contains("ERROR") || value.contains("FAULT") { return .fault }
        return ChargingState(apiValue: raw)
    }

    nonisolated private func mapConnection(_ raw: String?) -> ChargerConnection {
        let value = (raw ?? "").uppercased()
        if value.contains("UNPLUGGED") || value.contains("DISCONNECT") { return .disconnected }
        if value.contains("FAULT") || value.contains("ERROR") { return .fault }
        if !value.isEmpty { return .connected }
        return .unknown
    }

    nonisolated private func mapChargingType(_ raw: String?) -> ChargingType {
        let value = (raw ?? "").uppercased()
        if value.contains("DC") { return .dc }
        if value.contains("AC") { return .ac }
        if value.contains("WIRELESS") { return .wireless }
        if value.contains("UNKNOWN") || value.isEmpty { return .unknown }
        return .unknown
    }

    nonisolated private func mapPowerState(_ raw: String?) -> ChargerPowerState {
        let value = (raw ?? "").uppercased()
        if value.contains("NOPOWER") || value.contains("NO_POWER") { return .noPower }
        if value.contains("PROVID") { return .providingPower }
        if value.contains("AVAILABLE") { return .available }
        if value.contains("INIT") { return .initializing }
        if value.contains("FAULT") || value.contains("ERROR") { return .fault }
        return .unknown
    }

    nonisolated private func mapOpening(_ raw: String) -> VehicleOpening? {
        switch raw.lowercased() {
        case "frontleftdoor": return .frontLeftDoor
        case "frontrightdoor": return .frontRightDoor
        case "rearleftdoor": return .rearLeftDoor
        case "rearrightdoor": return .rearRightDoor
        case "frontleftwindow": return .frontLeftWindow
        case "frontrightwindow": return .frontRightWindow
        case "rearleftwindow": return .rearLeftWindow
        case "rearrightwindow": return .rearRightWindow
        case "hood": return .hood
        case "tailgate": return .tailgate
        case "chargelid": return .chargeLid
        case "sunroof": return .sunroof
        default: return nil
        }
    }

    nonisolated private func mapOpeningState(_ raw: String) -> OpeningState {
        switch raw.lowercased() {
        case "closed": return .closed
        case "open": return .open
        case "ajar": return .ajar
        default: return .unknown
        }
    }
}

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
        .init(label: "Android App Bridge", url: "http://127.0.0.1:28765/v1/status"),
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
                group.addTask { await probe(endpoint) }
            }
            var results: [ChinaPolestarDiagnosticProbe] = []
            for await result in group { results.append(result) }
            let order = Dictionary(uniqueKeysWithValues: endpoints.enumerated().map { ($0.element.label, $0.offset) })
            return results.sorted { (order[$0.label] ?? 999) < (order[$1.label] ?? 999) }
        }
    }

    private static func probe(_ endpoint: Endpoint) async -> ChinaPolestarDiagnosticProbe {
        guard let url = URL(string: endpoint.url), let host = url.host else {
            return .init(id: endpoint.label, label: endpoint.label, host: endpoint.url,
                         statusCode: nil, latencyMilliseconds: nil, reachable: false)
        }
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.timeoutInterval = endpoint.url.hasPrefix("http://127.") ? 2 : 10
        let start = Date()
        do {
            let (_, response) = try await URLSession.shared.data(for: request)
            let ms = Int(Date().timeIntervalSince(start) * 1000)
            let status = (response as? HTTPURLResponse)?.statusCode
            return .init(id: endpoint.label, label: endpoint.label, host: host,
                         statusCode: status, latencyMilliseconds: ms, reachable: true)
        } catch {
            return .init(id: endpoint.label, label: endpoint.label, host: host,
                         statusCode: nil, latencyMilliseconds: nil, reachable: false)
        }
    }
}

@MainActor
struct ChinaPolestarAccountView: View {
    let onSettingsChanged: (SettingsChange) -> Void

    @State private var bridgeReachable = false
    @State private var checking = false

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Circle()
                    .fill(bridgeReachable ? HisingenTheme.semanticGood : HisingenTheme.semanticWarning)
                    .frame(width: 8, height: 8)
                VStack(alignment: .leading, spacing: 1) {
                    Text("Polestar China")
                        .hisType(.body, weight: .semibold)
                    Text(bridgeReachable
                         ? "Android official-app bridge detected · read-only vehicle session available"
                         : "Official web sign-in is available; start the Android app bridge for vehicle data")
                        .hisType(.caption)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer()
            }
            .padding(10)
            .background(
                (bridgeReachable ? HisingenTheme.semanticGood : HisingenTheme.semanticWarning).opacity(0.08),
                in: RoundedRectangle(cornerRadius: 8)
            )

            HStack(spacing: 8) {
                Button {
                    ChinaPolestarRuntime.openChinaWebSignIn()
                } label: {
                    Label("Official China Sign-In", systemImage: "safari")
                }
                .buttonStyle(.bordered)
                .controlSize(.small)

                Button {
                    refreshBridge()
                } label: {
                    if checking {
                        ProgressView().controlSize(.mini)
                    } else {
                        Label("Connect / Refresh Vehicle", systemImage: "arrow.clockwise")
                    }
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.small)
                .disabled(checking)
            }

            Text("The website session and vehicle API session are separate. Hisingen does not read website cookies or tokens. Vehicle telemetry is currently supplied by the signed-in official Android app through a localhost-only bridge.")
                .hisType(.micro)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .task { await checkBridge() }
    }

    private func refreshBridge() {
        checking = true
        Task {
            await checkBridge()
            checking = false
            if bridgeReachable {
                onSettingsChanged(.credentials)
            }
        }
    }

    private func checkBridge() async {
        bridgeReachable = await ChinaPolestarRuntime.bridgeReachable()
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
                CardHeader(symbol: "globe.asia.australia.fill", title: "Polestar Region",
                           color: HisingenTheme.accent)

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
                    Label("Original Hisingen Global Polestar ID / Data Portal provider", systemImage: "checkmark.circle.fill")
                        .hisType(.caption, weight: .medium)
                        .foregroundStyle(HisingenTheme.semanticGood)
                } else {
                    ChinaPolestarAccountView(onSettingsChanged: onSettingsChanged)

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
                                if let ms = item.latencyMilliseconds {
                                    Text("\(ms) ms").hisType(.micro).foregroundStyle(.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .hisingenChinaPolestarRegionDidChange)) { _ in
            region = ChinaPolestarRuntime.currentRegion
        }
    }
}

@MainActor
struct ChinaPreviewUpdatesCard: View {
    var body: some View {
        Card {
            VStack(alignment: .leading, spacing: 10) {
                CardHeader(symbol: "shield.checkered", title: "Hisingen China Preview Updates",
                           color: HisingenTheme.semanticActive)
                Text("Automatic and manual upstream Sparkle updates are disabled so the China integration cannot be silently overwritten.")
                    .hisType(.caption)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
                HStack {
                    Label("Update channel", systemImage: "shippingbox.fill")
                        .hisType(.body, weight: .medium)
                    Spacer()
                    Text("Polestar-CN-API · Preview 0.18.0")
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
