#!/usr/bin/env python3
from pathlib import Path
import os
import plistlib
import shutil

ROOT = Path(os.environ.get("HISINGEN_SRC", ".")).resolve()
PATCH_ROOT = Path(__file__).resolve().parents[1] / "Patches" / "0.18.0"

def replace_once(path: Path, old: str, new: str, label: str):
    text = path.read_text(encoding="utf-8")
    if new in text:
        print(f"[ok] {label}: already applied")
        return
    if old not in text:
        raise SystemExit(f"[error] {label}: anchor not found in {path}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"[ok] {label}")

# 1) Install China integration.
src = PATCH_ROOT / "ChinaPolestarIntegration.swift"
dst = ROOT / "Sources/Hisingen/App/ChinaPolestarIntegration.swift"
shutil.copy2(src, dst)
print("[ok] copied ChinaPolestarIntegration.swift")

# 2) Settings: region card + preview update policy.
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
    "Settings region card"
)
replace_once(
    settings,
    """                        SettingsUpdatesCard(binder: binder)
                            .transition(Self.sectionSwapTransition)""",
    """                        ChinaPreviewUpdatesCard()
                            .transition(Self.sectionSwapTransition)""",
    "China preview updates card"
)

# 3) Integrate China into the ORIGINAL AccountCredentialsForm instead of leaving a parallel
# engineering-only card. Volvo remains untouched; Global Polestar remains untouched.
account = ROOT / "Sources/Hisingen/UI/Settings/AccountCredentialsForm.swift"
replace_once(
    account,
    """    @State private var enablePolestarID = true
    @State private var enableDataPortal = false""",
    """    @State private var enablePolestarID = true
    @State private var enableDataPortal = false
    @State private var polestarRegion = ChinaPolestarRuntime.currentRegion""",
    "Account form region state"
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
    "Account form China integration"
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
    "Account form live region updates"
)

# 4) Provider registry: region-aware China provider.
registry = ROOT / "Sources/Hisingen/Services/Refresh/ProviderRegistry.swift"
replace_once(
    registry,
    """    private let polestarDataPortal: (any VehicleProviding)?
    private let polestarAugmented: (any VehicleProviding)?""",
    """    private let polestarDataPortal: (any VehicleProviding)?
    private let polestarChina: (any VehicleProviding)?
    private let polestarAugmented: (any VehicleProviding)?""",
    "ProviderRegistry China property"
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
    "ProviderRegistry China initializer"
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
    "ProviderRegistry China assignment"
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
    "ProviderRegistry China routing"
)

# 5) Vehicle session: do not reuse Global cached/auth state in China, and FORCE provider
# reconstruction whenever the user switches China/Global at runtime.
session = ROOT / "Sources/Hisingen/Services/Refresh/VehicleSessionController.swift"
replace_once(
    session,
    """    func primeDisplayState() {
        let authenticated = preferences.hasResumableSession(for: preferences.activeBrand)""",
    """    func primeDisplayState() {
        if preferences.activeBrand == .polestar,
           ChinaPolestarRuntime.currentRegion == .mainlandChina {
            sessionValid = false
            latest = nil
            lastError = nil
            setFleet([], activeVIN: nil)
            context?.sessionStateDidChange()
            return
        }
        let authenticated = preferences.hasResumableSession(for: preferences.activeBrand)""",
    "China avoids Global cached display state"
)
replace_once(
    session,
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

    /// China/Global is a provider change even though the vehicle brand stays Polestar.
    /// Rebuild the refresh coordinator so a runtime region switch can never keep polling
    /// the provider that belonged to the previous region.
    func polestarRegionDidChange() {
        guard preferences.activeBrand == .polestar else { return }
        refreshCoordinator.stop()
        latest = nil
        lastError = nil
        sessionValid = false
        setFleet([], activeVIN: nil)
        switchActiveBrand(to: .polestar, force: true)
        if ChinaPolestarRuntime.currentRegion == .mainlandChina {
            sessionValid = false
        }
        context?.sessionStateDidChange()
        resumeStoredSession()
    }""",
    "Runtime region provider rebuild"
)

# 6) Remote command catalog: Global retains original behavior; China fails closed in the UI too.
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
    "Disable China remote controls in capability catalog"
)

# 7) App composition root.
app = ROOT / "Sources/Hisingen/App/AppDelegate.swift"
replace_once(
    app,
    """    private lazy var polestarAPI = PolestarAPI(imageCache: imageCache, preferences: preferences)
    private lazy var polestarDataPortalAPI = PolestarDataPortalAPI(imageCache: imageCache, preferences: preferences)""",
    """    private lazy var polestarAPI = PolestarAPI(imageCache: imageCache, preferences: preferences)
    private lazy var polestarChinaAPI = ChinaPolestarAPI()
    private lazy var polestarDataPortalAPI = PolestarDataPortalAPI(imageCache: imageCache, preferences: preferences)""",
    "AppDelegate China provider"
)
replace_once(
    app,
    """        polestar: polestarAPI, polestarDataPortal: polestarDataPortalAPI, volvo: volvoAPI, preferences: preferences)""",
    """        polestar: polestarAPI, polestarDataPortal: polestarDataPortalAPI,
        polestarChina: polestarChinaAPI, volvo: volvoAPI, preferences: preferences)""",
    "AppDelegate registry wiring"
)
replace_once(
    app,
    """        stateStore.activate()
        mainMenuController = MainMenuController(""",
    """        stateStore.activate()
        ChinaPolestarRuntime.install()
        ChinaPolestarRuntime.ensureInitialRegionSelection()
        mainMenuController = MainMenuController(""",
    "First-launch Select Polestar Region"
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
    "Runtime region menu + observer"
)
replace_once(
    app,
    """        preferences.migrateLegacyDefaults()
        preferences.migrateLegacyPassword()""",
    """        preferences.migrateLegacyDefaults()
        preferences.migrateLegacyPassword()
        ChinaPolestarRuntime.enforcePreviewUpdatePolicy(preferences)""",
    "Preview update policy"
)
replace_once(
    app,
    """        let initiallyAuthenticated = preferences.hasResumableSession(for: preferences.activeBrand)""",
    """        let initiallyAuthenticated: Bool = {
            if preferences.activeBrand == .polestar,
               ChinaPolestarRuntime.currentRegion == .mainlandChina {
                return false
            }
            return preferences.hasResumableSession(for: preferences.activeBrand)
        }()""",
    "Initial auth respects region"
)
replace_once(
    app,
    """    private func render() {
        let isAuth = vehicleSession.sessionValid || preferences.hasResumableSession(for: preferences.activeBrand)""",
    """    private func render() {
        let storedAuth = (preferences.activeBrand == .polestar &&
                          ChinaPolestarRuntime.currentRegion == .mainlandChina)
            ? false
            : preferences.hasResumableSession(for: preferences.activeBrand)
        let isAuth = vehicleSession.sessionValid || storedAuth""",
    "Render auth respects region"
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
    "Region change handler"
)

# Block upstream Sparkle manual checks.
app_text = app.read_text(encoding="utf-8")
app_text = app_text.replace(
    "onCheckForUpdates: { [weak self] in self?.updateController.checkNow() },",
    "onCheckForUpdates: { ChinaPolestarRuntime.presentUpdatePolicyNotice() },"
)
app_text = app_text.replace(
    "        case .checkForUpdates:\n            updateController.checkNow()",
    "        case .checkForUpdates:\n            ChinaPolestarRuntime.presentUpdatePolicyNotice()"
)
app.write_text(app_text, encoding="utf-8")
print("[ok] blocked upstream manual update checks")

# 8) Bundle metadata.
plist_path = ROOT / "Resources/Info.plist"
with plist_path.open("rb") as fh:
    info = plistlib.load(fh)
info["SUEnableAutomaticChecks"] = False
info["SUAutomaticallyUpdate"] = False
info["HisingenChinaPatchVersion"] = "0.18.1"
info["CFBundleDisplayName"] = "Hisingen China"
with plist_path.open("wb") as fh:
    plistlib.dump(info, fh, sort_keys=False)
print("[ok] Info.plist China 0.18.0 metadata")

print("Hisingen China Preview 0.18.1 patch applied.")
