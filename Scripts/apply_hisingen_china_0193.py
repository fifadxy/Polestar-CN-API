#!/usr/bin/env python3
from pathlib import Path
import os
import plistlib
import shutil

ROOT = Path(os.environ.get("HISINGEN_SRC", ".")).resolve()
PATCH_ROOT = Path(__file__).resolve().parents[1] / "Patches" / "0.19.3"


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    if new in text:
        print(f"[ok] {label}: already applied")
        return
    if old not in text:
        raise SystemExit(f"[error] {label}: anchor not found in {path}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"[ok] {label}")


def copy_patch(name: str, destination: str) -> None:
    source = PATCH_ROOT / name
    target = ROOT / destination
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)
    print(f"[ok] copied {name}")


copy_patch("ChinaPolestarRuntime.swift", "Sources/Hisingen/App/ChinaPolestarRuntime.swift")
copy_patch("ChinaPolestarSession.swift", "Sources/Hisingen/Services/API/ChinaPolestarSession.swift")
copy_patch("ChinaPolestarAPI.swift", "Sources/Hisingen/Services/API/ChinaPolestarAPI.swift")
copy_patch("ChinaPolestarViews.swift", "Sources/Hisingen/UI/Settings/ChinaPolestarViews.swift")
copy_patch("ChinaPolestarWebSignInWindow.swift", "Sources/Hisingen/Services/Security/ChinaPolestarWebSignInWindow.swift")

# PolestarGRPC remains the original implementation. Only its two service roots become
# injectable so the China provider can select China hosts while Global keeps exact defaults.
grpc = ROOT / "Sources/Hisingen/Services/API/PolestarGRPC.swift"
replace_once(
    grpc,
    '    private let discoveryURL = URL(string: "https://cnepmob.volvocars.com")!\n',
    '    private let discoveryURL: URL\n',
    "Configurable C3 discovery root",
)
replace_once(
    grpc,
    '    private let pccsURL = URL(string: "https://api.pccs-prod.plstr.io:443")!\n',
    '    private let pccsURL: URL\n',
    "Configurable PCCS root",
)
replace_once(
    grpc,
    """    init(defaultsSuiteName: String? = nil, session: URLSession? = nil,
         diagnosticLog: APIDiagnosticLogStore = .shared) {
        self.defaults = defaultsSuiteName.flatMap(UserDefaults.init(suiteName:)) ?? .standard""",
    """    init(defaultsSuiteName: String? = nil, session: URLSession? = nil,
         diagnosticLog: APIDiagnosticLogStore = .shared,
         discoveryURL: URL = URL(string: "https://cnepmob.volvocars.com")!,
         pccsURL: URL = URL(string: "https://api.pccs-prod.plstr.io:443")!) {
        self.discoveryURL = discoveryURL
        self.pccsURL = pccsURL
        self.defaults = defaultsSuiteName.flatMap(UserDefaults.init(suiteName:)) ?? .standard""",
    "PolestarGRPC endpoint injection with Global defaults",
)

# Settings uses the same cards, theme tokens, type scale and account composition as Hisingen.
settings = ROOT / "Sources/Hisingen/UI/Settings/SettingsView.swift"
replace_once(
    settings,
    """                        Group {
                            accountCard
                            SettingsFleetCard(""",
    """                        Group {
                            ChinaPolestarSettingsCard(onSettingsChanged: onSettingsChanged)
                            accountCard
                            SettingsFleetCard(""",
    "Settings region card",
)
replace_once(
    settings,
    """                        SettingsUpdatesCard(binder: binder)
                            .transition(Self.sectionSwapTransition)""",
    """                        ChinaPreviewUpdatesCard()
                            .transition(Self.sectionSwapTransition)""",
    "China update policy card",
)

account = ROOT / "Sources/Hisingen/UI/Settings/AccountCredentialsForm.swift"
replace_once(
    account,
    """    @State private var enablePolestarID = true
    @State private var enableDataPortal = false""",
    """    @State private var enablePolestarID = true
    @State private var enableDataPortal = false
    @State private var polestarRegion = ChinaPolestarRuntime.currentRegion""",
    "Account form region state",
)
replace_once(
    account,
    """            accountStatusBanner

            if style == .welcoming || connectionHealth == .notConnected || showUpdateFields {
                if selectedBrand == .polestar {
                    polestarFields
                } else {
                    volvoFields
                }
            }""",
    """            if selectedBrand == .polestar && polestarRegion == .mainlandChina {
                ChinaPolestarAccountView(onSettingsChanged: onSettingsChanged)
            } else {
                accountStatusBanner

                if style == .welcoming || connectionHealth == .notConnected || showUpdateFields {
                    if selectedBrand == .polestar {
                        polestarFields
                    } else {
                        volvoFields
                    }
                }
            }""",
    "Native China account form",
)
replace_once(
    account,
    """            selectedBrand = preferences.activeBrand
        }
    }""",
    """            selectedBrand = preferences.activeBrand
            polestarRegion = ChinaPolestarRuntime.currentRegion
        }
        .onReceive(NotificationCenter.default.publisher(for: .hisingenChinaPolestarRegionDidChange)) { _ in
            polestarRegion = ChinaPolestarRuntime.currentRegion
        }
    }""",
    "Account form live region updates",
)

popover = ROOT / "Sources/Hisingen/UI/Shell/HisingenContentView.swift"
replace_once(
    popover,
    '    @AppStorage("his_appearanceMode") private var storedAppearanceMode: String = AppearanceMode.system.rawValue',
    '    @AppStorage("his_appearanceMode") private var storedAppearanceMode: String = AppearanceMode.system.rawValue\n    @AppStorage("hisingen.polestar.signInRegion") private var selectedPolestarRegion: String = "global"',
    "Popover active region display",
)
replace_once(
    popover,
    """    private var footerBar: some View {
        HStack(spacing: 8) {
            if garageStates.count > 1 || cars.count > 1 || otherBrandResumable {""",
    """    private var footerBar: some View {
        HStack(spacing: 8) {
            if preferences.activeBrand == .polestar {
                Menu {
                    Button {
                        ChinaPolestarRuntime.setRegion(.mainlandChina)
                    } label: {
                        Label("China", systemImage: selectedPolestarRegion == "mainlandChina" ? "checkmark.circle.fill" : "globe.asia.australia")
                    }
                    Button {
                        ChinaPolestarRuntime.setRegion(.global)
                    } label: {
                        Label("Global", systemImage: selectedPolestarRegion == "global" ? "checkmark.circle.fill" : "globe")
                    }
                } label: {
                    Label(selectedPolestarRegion == "mainlandChina" ? "China" : "Global",
                          systemImage: "globe.asia.australia.fill")
                        .hisType(.micro, weight: .semibold)
                }
                .menuStyle(.borderlessButton)
                .help("Switch Polestar Region: China / Global")
            }
            if garageStates.count > 1 || cars.count > 1 || otherBrandResumable {""",
    "Popover region switch",
)

registry = ROOT / "Sources/Hisingen/Services/Refresh/ProviderRegistry.swift"
replace_once(
    registry,
    """    private let polestarDataPortal: (any VehicleProviding)?
    private let polestarAugmented: (any VehicleProviding)?""",
    """    private let polestarDataPortal: (any VehicleProviding)?
    private let polestarChina: (any VehicleProviding)?
    private let polestarAugmented: (any VehicleProviding)?""",
    "ProviderRegistry China property",
)
replace_once(
    registry,
    """        polestar: any VehicleProviding,
        polestarDataPortal: (any VehicleProviding)? = nil,
        volvo: any VehicleProviding,""",
    """        polestar: any VehicleProviding,
        polestarDataPortal: (any VehicleProviding)? = nil,
        polestarChina: (any VehicleProviding)? = nil,
        volvo: any VehicleProviding,""",
    "ProviderRegistry China initializer",
)
replace_once(
    registry,
    """        self.polestar = polestar
        self.polestarDataPortal = polestarDataPortal
        if let polestarDataPortal {""",
    """        self.polestar = polestar
        self.polestarDataPortal = polestarDataPortal
        self.polestarChina = polestarChina
        if let polestarDataPortal {""",
    "ProviderRegistry China assignment",
)
replace_once(
    registry,
    """        case .polestar:
            switch preferences.polestarConnectionMode {""",
    """        case .polestar:
            if ChinaPolestarRuntime.currentRegion == .mainlandChina,
               let polestarChina {
                return polestarChina
            }
            switch preferences.polestarConnectionMode {""",
    "ProviderRegistry China routing",
)

session_controller = ROOT / "Sources/Hisingen/Services/Refresh/VehicleSessionController.swift"
replace_once(
    session_controller,
    """    func signOut() { refreshCoordinator.signOut() }
    func stop() { refreshCoordinator.stop() }""",
    """    func signOut() { refreshCoordinator.signOut() }

    /// China owns a separate Keychain item. Signing it out must not erase Global email,
    /// Global credentials, or a same-VIN Global snapshot through the generic Polestar path.
    func signOutChinaSession() {
        guard preferences.activeBrand == .polestar,
              ChinaPolestarRuntime.currentRegion == .mainlandChina else {
            signOut()
            return
        }
        refreshCoordinator.stop()
        let provider = activeProvider
        lastError = nil
        do {
            try ChinaPolestarSessionStore.delete()
        } catch {
            lastError = error.localizedDescription
        }
        ChinaPolestarRuntime.forgetChinaVehicle()
        latest = nil
        sessionValid = false
        setFleet([], activeVIN: nil)
        context?.sessionStateDidChange()
        Task {
            await provider.resetSession()
        }
    }

    func stop() { refreshCoordinator.stop() }""",
    "Independent China sign-out",
)
replace_once(
    session_controller,
    """    func primeDisplayState() {
        let authenticated = preferences.hasResumableSession(for: preferences.activeBrand)
        let vin = preferences.vin(for: preferences.activeBrand)
        let nickname = preferences.vehicleNickname(for: vin)
        if !vin.isEmpty {
            setFleet(
                [CarSummary(vin: vin, title: nickname.isEmpty ? preferences.activeBrand.displayName : nickname)],
                activeVIN: vin)
        }
        sessionValid = authenticated
        latest = vin.isEmpty ? nil : fleetStore.snapshot(for: vin)
        context?.sessionStateDidChange()
    }""",
    """    func primeDisplayState() {
        let isChinaRegion = preferences.activeBrand == .polestar
            && ChinaPolestarRuntime.currentRegion == .mainlandChina
        let authenticated = isChinaRegion
            ? ChinaPolestarRuntime.hasStoredChinaSession
            : preferences.hasResumableSession(for: preferences.activeBrand)
        let vin = preferences.vin(for: preferences.activeBrand)
        let nickname = preferences.vehicleNickname(for: vin)
        let mayRestoreCachedState = !vin.isEmpty
            && ChinaPolestarRuntime.mayRestoreCachedSnapshot(
                vin: vin, brand: preferences.activeBrand
            )
        if !vin.isEmpty && !mayRestoreCachedState {
            // A same-VIN snapshot from the other region must not be reachable through the
            // FleetStore fallback. This clears only session/cache state, not durable history.
            stateStore.clearRegionScopedSessionCache(vin: vin)
            fleetStore.dropCachedSnapshots(.vehicle(vin))
        }
        // Persisted snapshots are keyed by VIN in upstream Hisingen. A region marker prevents
        // the same VIN from displaying China data in Global, or Global data in China.
        if mayRestoreCachedState {
            setFleet(
                [CarSummary(vin: vin, title: nickname.isEmpty ? preferences.activeBrand.displayName : nickname)],
                activeVIN: vin)
        }
        sessionValid = authenticated
        latest = mayRestoreCachedState ? fleetStore.snapshot(for: vin) : nil
        context?.sessionStateDidChange()
    }""",
    "China cached display session state",
)
replace_once(
    session_controller,
    """    /// After an interactive sign-in: force past the idempotence guard, then resume.
    func adoptBrandAfterSignIn(_ brand: VehicleBrand) {
        switchActiveBrand(to: brand, force: true)
        resumeStoredSession()
    }""",
    """    /// After an interactive sign-in: force past the idempotence guard, then resume.
    func adoptBrandAfterSignIn(_ brand: VehicleBrand) {
        switchActiveBrand(to: brand, force: true)
        resumeStoredSession()
    }

    /// China and Global are different providers even though both belong to Polestar.
    func polestarRegionDidChange() {
        guard preferences.activeBrand == .polestar else { return }
        refreshCoordinator.stop()
        let selectedRegionVIN = preferences.vin(for: .polestar)
        if !selectedRegionVIN.isEmpty,
           !ChinaPolestarRuntime.mayRestoreCachedSnapshot(
               vin: selectedRegionVIN, brand: .polestar
           ) {
            stateStore.clearRegionScopedSessionCache(vin: selectedRegionVIN)
            fleetStore.dropCachedSnapshots(.vehicle(selectedRegionVIN))
        }
        latest = nil
        lastError = nil
        sessionValid = false
        setFleet([], activeVIN: nil)
        switchActiveBrand(to: .polestar, force: true, restoreCachedState: false)
        if ChinaPolestarRuntime.currentRegion == .mainlandChina {
            sessionValid = ChinaPolestarRuntime.hasStoredChinaSession
        }
        context?.sessionStateDidChange()
        resumeStoredSession()
    }""",
    "Runtime region provider rebuild",
)
replace_once(
    session_controller,
    """    private func switchActiveBrand(to brand: VehicleBrand, targetVin: String? = nil, force: Bool = false) {""",
    """    private func switchActiveBrand(
        to brand: VehicleBrand,
        targetVin: String? = nil,
        force: Bool = false,
        restoreCachedState: Bool = true
    ) {""",
    "Region switch cache-isolation parameter",
)
replace_once(
    session_controller,
    """        let vin = preferences.vin(for: brand)
        let nick = preferences.vehicleNickname(for: vin)
        latest = vin.isEmpty ? nil : fleetStore.snapshot(for: vin)
        lastError = nil
        setFleet(
            vin.isEmpty ? [] : [CarSummary(vin: vin, title: nick.isEmpty ? brand.displayName : nick)],
            activeVIN: vin.isEmpty ? nil : vin)""",
    """        let vin = preferences.vin(for: brand)
        let nick = preferences.vehicleNickname(for: vin)
        let mayRestoreCachedState = restoreCachedState && !vin.isEmpty
        latest = mayRestoreCachedState ? fleetStore.snapshot(for: vin) : nil
        lastError = nil
        setFleet(
            mayRestoreCachedState
                ? [CarSummary(vin: vin, title: nick.isEmpty ? brand.displayName : nick)]
                : [],
            activeVIN: mayRestoreCachedState ? vin : nil)""",
    "Region switch cached telemetry isolation",
)
replace_once(
    session_controller,
    """        case .state(let state):
            context?.didReceiveVehicleState(state)
            latest = state""",
    """        case .state(let state):
            ChinaPolestarRuntime.markCachedSnapshotRegion(
                vin: state.identity.vin,
                brand: preferences.activeBrand
            )
            context?.didReceiveVehicleState(state)
            latest = state""",
    "Region-tagged Polestar snapshot cache",
)

state_store = ROOT / "Sources/Hisingen/Services/Persistence/VehicleStateStore.swift"
replace_once(
    state_store,
    """    func clear(vin: String, eraseHistory: Bool = false) {
        historyRecorder.resetTransientState(vin: vin)""",
    """    /// Drops provider-session state when the same VIN moves between China and Global.
    /// Reader-authored preferences and durable history remain untouched.
    func clearRegionScopedSessionCache(vin: String) {
        historyRecorder.resetTransientState(vin: vin)
        database.snapshots.deleteSnapshot(for: vin)
        database.baselines.deleteBaseline(for: vin)
        database.commandReceipts.deleteCommandReceipts(for: vin)
        database.providerBackoffs.deleteProviderBackoffs(for: vin)
    }

    func clear(vin: String, eraseHistory: Bool = false) {
        historyRecorder.resetTransientState(vin: vin)""",
    "Region-scoped state cache isolation",
)

catalog = ROOT / "Sources/Hisingen/Services/Coordination/ProviderCommandCatalog.swift"
replace_once(
    catalog,
    """    private func implementsPolestar(_ command: RemoteCommand) -> Bool {
        if polestarConnectionMode == .dataPortal {""",
    """    private func implementsPolestar(_ command: RemoteCommand) -> Bool {
        if ChinaPolestarRuntime.currentRegion == .mainlandChina {
            return false
        }
        if polestarConnectionMode == .dataPortal {""",
    "China remote commands fail closed",
)

settings_change = ROOT / "Sources/Hisingen/UI/Settings/PreferenceBinder.swift"
replace_once(
    settings_change,
    """    case credentials
    case features""",
    """    case credentials
    case chinaSignOut
    case features""",
    "China sign-out setting action",
)

app = ROOT / "Sources/Hisingen/App/AppDelegate.swift"
replace_once(
    app,
    """    private lazy var polestarAPI = PolestarAPI(imageCache: imageCache, preferences: preferences)
    private lazy var polestarDataPortalAPI = PolestarDataPortalAPI(imageCache: imageCache, preferences: preferences)""",
    """    private lazy var polestarAPI = PolestarAPI(imageCache: imageCache, preferences: preferences)
    private lazy var polestarChinaAPI = ChinaPolestarAPI.shared
    private lazy var polestarDataPortalAPI = PolestarDataPortalAPI(imageCache: imageCache, preferences: preferences)""",
    "AppDelegate China provider",
)
replace_once(
    app,
    """        polestar: polestarAPI, polestarDataPortal: polestarDataPortalAPI, volvo: volvoAPI, preferences: preferences)""",
    """        polestar: polestarAPI, polestarDataPortal: polestarDataPortalAPI,
        polestarChina: polestarChinaAPI, volvo: volvoAPI, preferences: preferences)""",
    "AppDelegate registry wiring",
)
replace_once(
    app,
    """        stateStore.activate()
        mainMenuController = MainMenuController(""",
    """        stateStore.activate()
        ChinaPolestarRuntime.install()
        mainMenuController = MainMenuController(""",
    "Register native China runtime before UI startup",
)
replace_once(
    app,
    """        mainMenuController.install()
        DistributedNotificationCenter.default().addObserver(""",
    """        mainMenuController.install()
        ChinaPolestarRegionMenuController.shared.install()
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(polestarRegionDidChange),
            name: .hisingenChinaPolestarRegionDidChange,
            object: nil
        )
        DistributedNotificationCenter.default().addObserver(""",
    "Runtime region observer",
)
# The China build is menu-bar-only, so show a visible window after choosing a region.
# NSAlert.runModal must run after app initialization, never before StatusItemController.
replace_once(
    app,
    """        if !initiallyAuthenticated {
            statusController.openPopover()
        }
    }""",
    """        if ChinaPolestarRuntime.needsInitialRegionSelection {
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                ChinaPolestarRuntime.ensureInitialRegionSelection()
                self.showSettingsWindow()
            }
        } else if !initiallyAuthenticated {
            // Recover installs that already completed the old 0.19.0 chooser.
            DispatchQueue.main.async { [weak self] in
                self?.showSettingsWindow()
            }
        }
    }""",
    "Visible first-run Settings after deferred region selection",
)

replace_once(
    app,
    """        preferences.migrateLegacyDefaults()
        preferences.migrateLegacyPassword()""",
    """        preferences.migrateLegacyDefaults()
        preferences.migrateLegacyPassword()
        ChinaPolestarRuntime.enforcePreviewUpdatePolicy(preferences)""",
    "China update policy",
)
replace_once(
    app,
    """            hasResumableSession: { [preferences] brand in preferences.hasResumableSession(for: brand) },""",
    """            hasResumableSession: { [preferences] brand in
                if brand == .polestar,
                   ChinaPolestarRuntime.currentRegion == .mainlandChina {
                    return ChinaPolestarRuntime.hasStoredChinaSession
                }
                return preferences.hasResumableSession(for: brand)
            },""",
    "Garage scanner China session presence",
)
replace_once(
    app,
    """        let initiallyAuthenticated = preferences.hasResumableSession(for: preferences.activeBrand)""",
    """        let initiallyAuthenticated = (preferences.activeBrand == .polestar &&
            ChinaPolestarRuntime.currentRegion == .mainlandChina)
            ? ChinaPolestarRuntime.hasStoredChinaSession
            : preferences.hasResumableSession(for: preferences.activeBrand)""",
    "Initial authentication respects region",
)
replace_once(
    app,
    """    private func render() {
        let isAuth = vehicleSession.sessionValid || preferences.hasResumableSession(for: preferences.activeBrand)""",
    """    private func render() {
        let storedAuth = (preferences.activeBrand == .polestar &&
                          ChinaPolestarRuntime.currentRegion == .mainlandChina)
            ? ChinaPolestarRuntime.hasStoredChinaSession
            : preferences.hasResumableSession(for: preferences.activeBrand)
        let isAuth = vehicleSession.sessionValid || storedAuth""",
    "Render authentication respects region",
)
replace_once(
    app,
    """        case .credentials:
            vehicleSession.credentialsDidChange(for: .polestar)
        case .volvoSignIn""",
    """        case .credentials:
            vehicleSession.credentialsDidChange(for: .polestar)
        case .chinaSignOut:
            signOut()
        case .volvoSignIn""",
    "China sign-out routing",
)
replace_once(
    app,
    """    private func signOut() {
        signInCoordinator.cancelPolestarSignIn()
        SpotlightIndexer.removeAll()
        vehicleSession.signOut()
    }""",
    """    private func signOut() {
        signInCoordinator.cancelPolestarSignIn()
        SpotlightIndexer.removeAll()
        if preferences.activeBrand == .polestar,
           ChinaPolestarRuntime.currentRegion == .mainlandChina {
            vehicleSession.signOutChinaSession()
        } else {
            vehicleSession.signOut()
        }
    }""",
    "Independent China sign-out dispatch",
)
replace_once(
    app,
    """    @objc private func systemAppearanceDidChange() {""",
    """    @objc private func polestarRegionDidChange() {
        guard vehicleSession != nil else { return }
        vehicleSession.polestarRegionDidChange()
        if settingsWindow?.isVisible == true {
            closeSettingsWindow()
            DispatchQueue.main.async { [weak self] in self?.showSettingsWindow() }
        }
        render()
    }

    @objc private func systemAppearanceDidChange() {""",
    "Region change handler",
)

app_text = app.read_text(encoding="utf-8")
app_text = app_text.replace(
    "onCheckForUpdates: { [weak self] in self?.updateController.checkNow() },",
    "onCheckForUpdates: { ChinaPolestarRuntime.presentUpdatePolicyNotice() },",
)
app_text = app_text.replace(
    "        case .checkForUpdates:\n            updateController.checkNow()",
    "        case .checkForUpdates:\n            ChinaPolestarRuntime.presentUpdatePolicyNotice()",
)
app.write_text(app_text, encoding="utf-8")
print("[ok] blocked upstream manual update checks")

plist_path = ROOT / "Resources/Info.plist"
with plist_path.open("rb") as handle:
    info = plistlib.load(handle)
info["SUEnableAutomaticChecks"] = False
info["SUAutomaticallyUpdate"] = False
info["HisingenChinaPatchVersion"] = "0.19.3"
info["CFBundleDisplayName"] = "Hisingen China"
with plist_path.open("wb") as handle:
    plistlib.dump(info, handle, sort_keys=False)
print("[ok] Info.plist China 0.19.3 metadata")

print("Hisingen China 0.19.3 native Mac patch applied.")
