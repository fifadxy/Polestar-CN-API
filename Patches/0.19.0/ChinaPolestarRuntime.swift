import AppKit
import Foundation

// Hisingen China 0.19.0
//
// China and Global are separate providers and separate credential stores. Selecting China
// never reads or mutates the Global Polestar refresh token. Selecting Global never reads the
// China Keychain session.

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

extension Notification.Name {
    static let hisingenChinaPolestarRegionDidChange =
        Notification.Name("io.kheirallah.hisingen.cn.region-did-change")
}

enum ChinaPolestarRuntime {
    nonisolated static let patchVersion = "0.19.0"
    nonisolated static let regionKey = "hisingen.polestar.signInRegion"
    nonisolated static let chooserVersionKey = "hisingen.polestar.regionChooserVersion"
    nonisolated static let chooserVersion = 1
    nonisolated static let globalVehicleBackupKey = "hisingen.polestar.globalVehicleBeforeChina"
    nonisolated static let chinaVehicleKey = "hisingen.polestar.chinaSelectedVehicle"
    nonisolated static let snapshotRegionMarkerPrefix = "hisingen.polestar.snapshotRegion."

    nonisolated static var currentRegion: ChinaPolestarRegion {
        let raw = UserDefaults.standard.string(forKey: regionKey)
        return raw == ChinaPolestarRegion.mainlandChina.rawValue ? .mainlandChina : .global
    }

    nonisolated static var hasStoredChinaSession: Bool {
        ChinaPolestarSessionStore.hasStoredSession
    }

    nonisolated static func mayRestoreCachedSnapshot(vin: String, brand: VehicleBrand) -> Bool {
        guard brand == .polestar else { return true }
        let key = snapshotRegionMarkerPrefix + vin.uppercased()
        guard let marker = UserDefaults.standard.string(forKey: key) else {
            // Snapshots created before 0.19.0 can only be trusted as original Global data.
            return currentRegion == .global
        }
        return marker == currentRegion.rawValue
    }

    @MainActor
    static func install() {}

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
            "Choose the account region Hisingen should use. China and Global sessions are stored separately, and you can switch later from Settings or the menu bar."
        alert.addButton(withTitle: "China")
        alert.addButton(withTitle: "Global")

        let response = alert.runModal()
        let selected: ChinaPolestarRegion = response == .alertFirstButtonReturn ? .mainlandChina : .global
        setRegion(selected, notify: false)
        UserDefaults.standard.set(chooserVersion, forKey: chooserVersionKey)
    }

    @MainActor
    static func setRegion(_ region: ChinaPolestarRegion, notify: Bool = true) {
        let previous = currentRegion
        guard previous != region || UserDefaults.standard.object(forKey: regionKey) == nil else { return }

        if previous == .global && region == .mainlandChina {
            UserDefaults.standard.set(
                PreferencesStore.shared.vin(for: .polestar), forKey: globalVehicleBackupKey
            )
            if let chinaVIN = UserDefaults.standard.string(forKey: chinaVehicleKey), !chinaVIN.isEmpty {
                PreferencesStore.shared.setVin(chinaVIN, for: .polestar)
            } else {
                PreferencesStore.shared.setVin("", for: .polestar)
            }
        } else if previous == .mainlandChina && region == .global {
            let chinaVIN = PreferencesStore.shared.vin(for: .polestar)
            if !chinaVIN.isEmpty {
                UserDefaults.standard.set(chinaVIN, forKey: chinaVehicleKey)
            }
            let globalVIN = UserDefaults.standard.string(forKey: globalVehicleBackupKey) ?? ""
            PreferencesStore.shared.setVin(globalVIN, for: .polestar)
        }

        UserDefaults.standard.set(region.rawValue, forKey: regionKey)
        if notify {
            NotificationCenter.default.post(name: .hisingenChinaPolestarRegionDidChange, object: nil)
        }
    }

    @MainActor
    static func rememberChinaVehicle(_ vin: String) {
        let normalized = vin.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard !normalized.isEmpty else { return }
        UserDefaults.standard.set(normalized, forKey: chinaVehicleKey)
    }

    @MainActor
    static func forgetChinaVehicle() {
        UserDefaults.standard.removeObject(forKey: chinaVehicleKey)
        if currentRegion == .mainlandChina {
            PreferencesStore.shared.setVin("", for: .polestar)
        }
    }

    @MainActor
    static func markCachedSnapshotRegion(vin: String, brand: VehicleBrand) {
        guard brand == .polestar else { return }
        let normalized = vin.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard !normalized.isEmpty else { return }
        UserDefaults.standard.set(
            currentRegion.rawValue,
            forKey: snapshotRegionMarkerPrefix + normalized
        )
    }

    @MainActor
    static func enforcePreviewUpdatePolicy(_ preferences: PreferencesStore) {
        preferences.automaticallyChecksForUpdates = false
        preferences.automaticallyDownloadsUpdates = false
    }

    @MainActor
    static func presentUpdatePolicyNotice() {
        let alert = NSAlert()
        alert.messageText = "Updates are managed by Hisingen China"
        alert.informativeText =
            "Upstream Sparkle updates are disabled because they do not contain the China provider. Install a newer Hisingen China build to update this app."
        alert.addButton(withTitle: "OK")
        alert.runModal()
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

        let settingsIndex = appMenu.items.firstIndex(where: {
            $0.action == Selector(("openSettingsMenuItem"))
        }) ?? 0
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
