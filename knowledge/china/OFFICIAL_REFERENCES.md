# Official/public source links and interpretation limits

Updated: 2026-10-22. This list complements the internal observed Android runtime record. **A public page describing a feature does not prove an unofficial Mac API is authenticated or authorized.**

| Source | URL | What it directly supports | What it does not prove |
| --- | --- | --- | --- |
| Hisingen upstream repository | https://github.com/NicolasKheirallah/Hisingen | Global Polestar ID, Developer Portal and original provider implementation | China consumer API access |
| Polestar CN official mobile app FAQ | https://www.polestar.cn/zh-cn/support/faq/polestar-app/ | Available features vary by vehicle architecture; some cloud status/control points differ by platform | Every global C3/PCCS RPC works on China PS2 |
| Polestar CN mobile app introduction | https://www.polestar.cn/zh-cn/owning-a-polestar/polestar-mobile-apps/ | Official app supports vehicle status and some functions such as climate/charging/unlock/software | Third-party account grants or write RPC permission |
| Polestar 2 CN app linking manual | https://www.polestar.cn/zh-cn/manual/polestar-2/2026/article/d5c0b6257a4b7a39c0a801510f2cfea5/ | Vehicle/account linking prerequisite and car-side approval | How to obtain a native Mac C3 bearer grant |
| China official login | https://www.polestar.cn/zh-cn/auth/login/ | Official website session entry | Website session is a vehicle API token |
| CN official app in Xiaomi app market | https://app.mi.com/details?id=com.polestar.china.android | CN package identifier and visible app version 4.25.0 | Internal RPC authorization semantics |
| Polestar privacy policy (CN) | https://policy-static.app.polestar-auto.com/fms/polestar/policy/5b07d6d7e6d59aa5799a6ae75fe0d068_9ec679281c5e5fa589d3a8d8131662fa_43.html | Identity processing information, account and feature data context | Cross-region token equivalence or permission to extract secrets |
| Volvo Cars Developer Portal | https://developer.volvocars.com/apis/docs/getting-started/ | Volvo supported API developer path and app authorization concept | Polestar China-specific client registration |
| Volvo official API sample repository | https://github.com/volvo-cars/developer-portal-api-samples | Reference OAuth and vehicle-read samples | China Polestar App credentials or direct C3 method mapping |

## Evidence discipline

- User-collected Frida/runtime class and field observations are more specific than broad website marketing descriptions; mark them `VERIFIED Android runtime` only for the observed app/version, not universal CN behavior.
- Public URLs and upstream source prove **where APIs exist**, not entitlement from the user's account.
- Stage12 was passive inspection; no permissioned China write end-to-end test was performed.
- No user authentication state, tokens, VINs or private positions should be copied into GitHub.
- For release claims, keep a separate distinction between `compiled`, `mock tested`, `launched`, `real account read verified`, and `command executed and vehicle confirmed`.

