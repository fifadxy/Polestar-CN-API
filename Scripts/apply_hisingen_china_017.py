#!/usr/bin/env python3
from pathlib import Path
import os
import plistlib
import shutil

ROOT = Path(os.environ.get("HISINGEN_SRC", ".")).resolve()
PATCH_ROOT = Path(__file__).resolve().parents[1] / "Patches" / "0.17.0"

def replace_once(path: Path, old: str, new: str, label: str):
    text = path.read_text(encoding="utf-8")
    if new in text:
        print(f"[ok] {label}: already applied")
        return
    if old not in text:
        raise SystemExit(f"[error] {label}: anchor not found in {path}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"[ok] {label}")

# 1) Install the separate China module.
src = PATCH_ROOT / "ChinaPolestarIntegration.swift"
dst = ROOT / "Sources/Hisingen/App/ChinaPolestarIntegration.swift"
shutil.copy2(src, dst)
print("[ok] copied ChinaPolestarIntegration.swift")

# 2) Settings: native region card before the normal account card.
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

# 3) Replace upstream update card with China-preview update policy UI.
replace_once(
    settings,
    """                        SettingsUpdatesCard(binder: binder)
                            .transition(Self.sectionSwapTransition)""",
    """                        ChinaPreviewUpdatesCard()
                            .transition(Self.sectionSwapTransition)""",
    "China preview updates card"
)

# 4) Provider registry: route Polestar to a separate China provider only when explicitly selected.
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

# 5) Composition root: instantiate China provider and register it.
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
    """        mainMenuController.install()
        DistributedNotificationCenter.default().addObserver(""",
    """        mainMenuController.install()
        ChinaPolestarRuntime.install()
        DistributedNotificationCenter.default().addObserver(""",
    "China runtime install"
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

# 6) Block all upstream Sparkle manual checks in the China preview.
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

# 7) Bundle metadata: disable automatic upstream updates and identify patch version.
plist_path = ROOT / "Resources/Info.plist"
with plist_path.open("rb") as fh:
    info = plistlib.load(fh)
info["SUEnableAutomaticChecks"] = False
info["SUAutomaticallyUpdate"] = False
info["HisingenChinaPatchVersion"] = "0.17.0"
info["CFBundleDisplayName"] = "Hisingen China"
with plist_path.open("wb") as fh:
    plistlib.dump(info, fh, sort_keys=False)
print("[ok] Info.plist preview update policy + version metadata")

print("Hisingen China Preview 0.17.0 patch applied.")
