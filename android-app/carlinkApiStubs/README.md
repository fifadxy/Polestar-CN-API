# Public CarLink API contracts for validation

These small declarations and data containers are written from the documented API shape. They contain no OPPO SDK implementation, authentication, binding, networking, or OEM code.

Public CI uses them only when the separately supplied SDK AARs are absent. BuildConfig.OPPO_SDK_PACKAGED is false, and the runtime explicitly refuses SDK initialization. Such APKs are labelled VALIDATION and must never be delivered as complete CarLink builds.

A private build with both original AARs uses the real SDK instead; this project is omitted from the APK. Model/command tests run with the real AARs again before a FULL APK is delivered.
