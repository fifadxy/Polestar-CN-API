# OPPO SDK inputs

From the user-supplied official CarAppDemo_v2.0.1 archive; libraries only. No demo AUTH_CODE, company identity, accounts or test keys are included.

com.oplus.ocs.carlink-release-1.4.0-SNAPSHOT.aar · 100918 bytes · SHA256 e3243827e5e14e9560ff965d4e4ca796217524ac579725a4af51bf3b39251b5a

com.oplus.ocs.base-release-1.0.9.aar · 103710 bytes · SHA256 db6cc2298f5335e2c45fd3d3d0d1ae1be256f0f5ea358be82ded8709df031fb1

The available demo is SDK 1.4.0-SNAPSHOT + base 1.0.9, not the documentation's newer 1.4.9/base 1.0.16. The obsolete demo receiver is removed; PsAssistant supplies its own permission-protected, control-only receiver and scoped package visibility. Private build inputs PSA_OPPO_AUTH_CODE and PSA_OPPO_COMPANY_ID must identify this application and its signing certificate.

The AARs are private inputs and excluded from git. Place the two files here from the supplied archive before the local FULL build. Public CI selects the API-contract project instead, reports OPPO_SDK_PACKAGED=false and cannot initialize a real SDK. FULL delivery must be compiled again locally with the actual libraries and all tests.
