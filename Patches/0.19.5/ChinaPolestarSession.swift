import Foundation
import Security

struct ChinaPolestarSession: Codable, Sendable {
    var phone: String
    var localAccessToken: String
    var localRefreshToken: String
    var localTokenType: String
    var localExpiresAt: Date
    var globalAccessToken: String
    var globalRefreshToken: String
    var globalIDToken: String
    var globalTokenType: String
    var globalExpiresAt: Date
    var userUID: String
    var userName: String
}

struct ChinaPolestarCar: Sendable, Equatable {
    let vin: String
    let market: String?
    let modelName: String?
    let modelYear: String?
    let registrationPlate: String?
    let structureWeek: String?
    let consumerSoftwareVersion: String?
    let supportsChargingFunctions: Bool
    let supportsGlobalChargeAmperageLimitSetting: Bool
    let supportsTargetChargeLevelSetting: Bool
    let supportsGlobalChargeTimer: Bool
    let supportsChargeLocations: Bool
    let supportsParkingClimatizationTimerSettings: Bool
}

struct ChinaPolestarAccountStatus: Sendable {
    let signedIn: Bool
    let maskedPhone: String?
    let vehicleCount: Int
}

struct ChinaPolestarChallengePayload: Sendable {
    let sessionID: String
    let signature: String
    let token: String
}

enum ChinaPolestarSignInError: Error, LocalizedError, Sendable {
    case invalidPhone
    case invalidCode
    case captchaRequired
    case throttled
    case rejected(String?)

    var errorDescription: String? {
        switch self {
        case .invalidPhone:
            return "Enter an 11-digit mainland China mobile number."
        case .invalidCode:
            return "Enter the verification code sent to your phone."
        case .captchaRequired:
            return "Polestar requires an interactive verification challenge before sending the SMS code."
        case .throttled:
            return "Too many code requests. Wait before trying again."
        case .rejected(let message):
            return message?.isEmpty == false ? message : "Polestar China rejected the sign-in request."
        }
    }
}

enum ChinaPolestarSessionStore {
    nonisolated static let service = "io.kheirallah.hisingen.polestar-cn"
    nonisolated static let account = "native-session-v1"
    nonisolated static let presenceKey = "hisingen.polestar.china.hasSession"

    nonisolated static var hasStoredSession: Bool {
        UserDefaults.standard.bool(forKey: presenceKey)
    }

    nonisolated static func markAbsent() {
        UserDefaults.standard.set(false, forKey: presenceKey)
    }

    static func load() throws -> ChinaPolestarSession? {
        var query = baseQuery
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound {
            UserDefaults.standard.set(false, forKey: presenceKey)
            return nil
        }
        if status == errSecInteractionRequired {
            throw KeychainError.interactionRequired
        }
        guard status == errSecSuccess, let data = result as? Data else {
            throw KeychainError.status(status == errSecSuccess ? errSecDecode : status)
        }
        do {
            let session = try JSONDecoder().decode(ChinaPolestarSession.self, from: data)
            UserDefaults.standard.set(true, forKey: presenceKey)
            return session
        } catch {
            throw KeychainError.status(errSecDecode)
        }
    }

    static func save(_ session: ChinaPolestarSession) throws {
        let data = try JSONEncoder().encode(session)

        let updateStatus = SecItemUpdate(
            baseQuery as CFDictionary,
            [kSecValueData as String: data] as CFDictionary
        )
        if updateStatus == errSecItemNotFound {
            var add = baseQuery
            add[kSecValueData as String] = data
            add[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            let addStatus = SecItemAdd(add as CFDictionary, nil)
            if addStatus == errSecInteractionRequired { throw KeychainError.interactionRequired }
            guard addStatus == errSecSuccess else { throw KeychainError.status(addStatus) }
        } else {
            if updateStatus == errSecInteractionRequired { throw KeychainError.interactionRequired }
            guard updateStatus == errSecSuccess else { throw KeychainError.status(updateStatus) }
        }
        UserDefaults.standard.set(true, forKey: presenceKey)
    }

    static func delete() throws {
        markAbsent()
        let status = SecItemDelete(baseQuery as CFDictionary)
        if status == errSecInteractionRequired { throw KeychainError.interactionRequired }
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.status(status)
        }
    }

    private static var baseQuery: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecUseDataProtectionKeychain as String: true
        ]
    }
}

enum ChinaPolestarOAuthMaterialError: Error, LocalizedError, Sendable {
    case missing
    case invalid

    var errorDescription: String? {
        switch self {
        case .missing:
            return "本地访问令牌需要刷新，但尚未配置 OAuth 刷新材料。请运行安装包内的本地 DEX 导入工具；工具只写入 macOS 钥匙串，不显示或上传材料内容。"
        case .invalid:
            return "中国区 OAuth 刷新材料格式无效，请重新从你自己的官方 Android App DEX 导入。"
        }
    }
}

/// The official Android OAuth client secret is used only by the local-token refresh endpoint.
/// It is intentionally absent from source and the app bundle, and is provisioned from the user's
/// own official DEX into a dedicated Keychain item. Initial SMS sign-in does not depend on it.
enum ChinaPolestarOAuthMaterialStore {
    nonisolated static let service = "io.kheirallah.hisingen.polestar-cn.oauth"
    nonisolated static let account = "local-refresh-client-secret-v1"

    nonisolated static var hasStoredMaterial: Bool {
        var query = baseQuery
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        return SecItemCopyMatching(query as CFDictionary, nil) == errSecSuccess
    }

    static func loadRequired() throws -> String {
        var query = baseQuery
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound { throw ChinaPolestarOAuthMaterialError.missing }
        if status == errSecInteractionRequired { throw KeychainError.interactionRequired }
        guard status == errSecSuccess, let data = result as? Data,
              let value = String(data: data, encoding: .utf8),
              isValid(value)
        else {
            throw ChinaPolestarOAuthMaterialError.invalid
        }
        return value
    }

    private nonisolated static var baseQuery: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
    }

    private nonisolated static func isValid(_ value: String) -> Bool {
        value.count == 32 && value.allSatisfy { character in
            character.isNumber || ("A"..."F").contains(String(character))
        }
    }
}

struct ChinaPolestarHTTPTransport: Sendable {
    private struct LoginPayload: Sendable {
        let accessToken: String
        let refreshToken: String
        let tokenType: String
        let expiresIn: Int
        let userUID: String
        let userName: String
        let global: GlobalPayload?
    }

    private struct GlobalPayload: Sendable {
        let accessToken: String
        let refreshToken: String
        let idToken: String
        let tokenType: String
        let expiresIn: Int
    }

    private static let gateway = URL(string: "https://gw.app.polestar-auto.com")!
    private static let userCenter = URL(string: "https://uc.app.polestar-auto.com")!
    private static let c3Discovery = URL(string: "https://cnepmob.volvocars.com.cn")!
    private static let clientID = "polestar-app-android"
    private static let protocolVersion = "4.25.0"
    private static let maximumResponseBytes = 2_000_000

    let session: URLSession

    init(session: URLSession? = nil) {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = 20
        configuration.timeoutIntervalForResource = 30
        configuration.requestCachePolicy = .reloadIgnoringLocalAndRemoteCacheData
        self.session = session ?? URLSession(configuration: configuration)
    }

    func requestVerificationCode(
        phone: String,
        challenge: ChinaPolestarChallengePayload
    ) async throws {
        let normalized = try Self.normalizedPhone(phone)
        let body: [String: Any] = [
            "principal": normalized,
            "aliScene": "ic_login_h5",
            "aliSessionId": challenge.sessionID,
            "aliSign": challenge.signature,
            "aliToken": challenge.token,
            "clientId": Self.clientID,
            "type": "SIGNIN_BY_PHONE"
        ]
        let request = try Self.jsonRequest(
            url: Self.gateway.appendingPathComponent("user/api/v1/credcode/send"),
            body: body
        )
        let (data, response) = try await data(for: request)
        if Self.gatewayCode(in: data) == 2_002_016 {
            throw ChinaPolestarSignInError.captchaRequired
        }
        if response.statusCode == 429 { throw ChinaPolestarSignInError.throttled }
        guard (200..<300).contains(response.statusCode) else {
            throw ChinaPolestarSignInError.rejected(Self.gatewayMessage(in: data))
        }
        if let code = Self.gatewayCode(in: data), code != 0 && code != 200 {
            throw ChinaPolestarSignInError.rejected(Self.gatewayMessage(in: data))
        }
    }

    func signIn(phone: String, code: String) async throws -> ChinaPolestarSession {
        let normalized = try Self.normalizedPhone(phone)
        let cleanCode = code.trimmingCharacters(in: .whitespacesAndNewlines)
        guard cleanCode.count >= 4, cleanCode.allSatisfy(\.isNumber) else {
            throw ChinaPolestarSignInError.invalidCode
        }
        let body: [String: Any] = [
            "clientId": Self.clientID,
            "phone": normalized,
            "credential": cleanCode,
            "autocrt": true
        ]
        let request = try Self.jsonRequest(
            url: Self.gateway.appendingPathComponent("user/api/v2/oauth/login/phone"),
            body: body
        )
        let (data, response) = try await data(for: request)
        guard (200..<300).contains(response.statusCode) else {
            if response.statusCode == 429 { throw ChinaPolestarSignInError.throttled }
            throw ChinaPolestarSignInError.rejected(Self.gatewayMessage(in: data))
        }
        if Self.gatewayCode(in: data) == 2_002_016 {
            throw ChinaPolestarSignInError.captchaRequired
        }
        if let code = Self.gatewayCode(in: data), code != 0 && code != 200 {
            throw ChinaPolestarSignInError.rejected(Self.gatewayMessage(in: data))
        }
        let payload = try Self.decodeLoginPayload(data, requiresGlobal: true)
        guard let global = payload.global else {
            throw PolestarError.incompatibleAPI(operation: "China phone sign-in")
        }
        let now = Date()
        return ChinaPolestarSession(
            phone: normalized,
            localAccessToken: payload.accessToken,
            localRefreshToken: payload.refreshToken,
            localTokenType: payload.tokenType,
            localExpiresAt: Self.expiry(token: payload.accessToken, expiresIn: payload.expiresIn, now: now),
            globalAccessToken: global.accessToken,
            globalRefreshToken: global.refreshToken,
            globalIDToken: global.idToken,
            globalTokenType: global.tokenType,
            globalExpiresAt: Self.expiry(token: global.accessToken, expiresIn: global.expiresIn, now: now),
            userUID: payload.userUID,
            userName: payload.userName
        )
    }

    func refreshLocalSession(_ current: ChinaPolestarSession) async throws -> ChinaPolestarSession {
        guard !current.localRefreshToken.isEmpty else {
            throw PolestarError.authenticationRequired(.expiredSession)
        }
        var components = URLComponents(
            url: Self.userCenter.appendingPathComponent("oauth/token"),
            resolvingAgainstBaseURL: false
        )!
        let oauthClientSecret = try ChinaPolestarOAuthMaterialStore.loadRequired()
        components.queryItems = [
            URLQueryItem(name: "grant_type", value: "refresh_token"),
            URLQueryItem(name: "client_id", value: Self.clientID),
            URLQueryItem(name: "client_secret", value: oauthClientSecret),
            URLQueryItem(name: "refresh_token", value: current.localRefreshToken)
        ]
        var request = URLRequest(url: components.url!)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        let (data, response) = try await data(for: request)
        guard (200..<300).contains(response.statusCode) else {
            throw Self.httpError(response.statusCode, operation: "China local token refresh")
        }
        let payload = try Self.decodeLoginPayload(
            data,
            requiresGlobal: false,
            fallbackRefreshToken: current.localRefreshToken
        )
        var updated = current
        updated.localAccessToken = payload.accessToken
        updated.localRefreshToken = payload.refreshToken
        updated.localTokenType = payload.tokenType
        updated.localExpiresAt = Self.expiry(
            token: payload.accessToken, expiresIn: payload.expiresIn, now: Date()
        )
        if !payload.userUID.isEmpty { updated.userUID = payload.userUID }
        if !payload.userName.isEmpty { updated.userName = payload.userName }
        return updated
    }

    func refreshGlobalSession(_ current: ChinaPolestarSession) async throws -> ChinaPolestarSession {
        guard !current.globalRefreshToken.isEmpty else {
            throw PolestarError.authenticationRequired(.expiredSession)
        }
        var request = try Self.jsonRequest(
            url: Self.gateway.appendingPathComponent("user/api/v2/oauth/refresh-global-token"),
            body: ["refreshToken": current.globalRefreshToken]
        )
        request.setValue("Bearer \(current.localAccessToken)", forHTTPHeaderField: "Authorization")
        if !current.userUID.isEmpty {
            request.setValue(current.userUID, forHTTPHeaderField: "psoneid")
        }
        let (data, response) = try await data(for: request)
        guard (200..<300).contains(response.statusCode) else {
            throw Self.httpError(response.statusCode, operation: "China global token refresh")
        }
        let global = try Self.decodeGlobalPayload(
            data,
            fallbackRefreshToken: current.globalRefreshToken,
            fallbackIDToken: current.globalIDToken
        )
        var updated = current
        updated.globalAccessToken = global.accessToken
        updated.globalRefreshToken = global.refreshToken
        updated.globalIDToken = global.idToken
        updated.globalTokenType = global.tokenType
        updated.globalExpiresAt = Self.expiry(
            token: global.accessToken, expiresIn: global.expiresIn, now: Date()
        )
        return updated
    }

    func fetchCars(accessToken: String) async throws -> [ChinaPolestarCar] {
        let c3REST = try await discoverC3RESTBaseURL()
        var request = URLRequest(url: c3REST.appendingPathComponent("car-information/car"))
        request.httpMethod = "GET"
        request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        request.setValue("Hisingen-China/0.19.5 macOS", forHTTPHeaderField: "User-Agent")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        let (data, response) = try await data(for: request)
        guard (200..<300).contains(response.statusCode) else {
            throw Self.httpError(response.statusCode, operation: "China vehicle discovery")
        }
        return try Self.decodeCars(data)
    }

    /// The China discovery document is public. The official app performs this request without
    /// any bearer token and uses the returned `c3.url` for REST vehicle discovery.
    private func discoverC3RESTBaseURL() async throws -> URL {
        var request = URLRequest(url: Self.c3Discovery)
        request.httpMethod = "GET"
        request.setValue("application/volvo.cloud.cnepmob.v1+json", forHTTPHeaderField: "Accept")
        let (data, response) = try await data(for: request)
        guard (200..<300).contains(response.statusCode) else {
            throw PolestarError.httpFailure(
                statusCode: response.statusCode,
                forbiddenIsAuthentication: false,
                operation: "China C3 discovery"
            ) ?? PolestarError.invalidResponse(operation: "China C3 discovery")
        }
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let c3 = root["c3"] as? [String: Any],
              let value = c3["url"] as? String,
              let url = URL(string: value),
              url.scheme?.lowercased() == "https",
              url.host != nil
        else {
            throw PolestarError.incompatibleAPI(operation: "China C3 REST discovery")
        }
        return url
    }

    static func normalizedPhone(_ input: String) throws -> String {
        var digits = input.filter(\.isNumber)
        if digits.count == 13, digits.hasPrefix("86") {
            digits.removeFirst(2)
        }
        guard digits.count == 11, digits.hasPrefix("1") else {
            throw ChinaPolestarSignInError.invalidPhone
        }
        return digits
    }

    static func maskedPhone(_ phone: String) -> String {
        guard phone.count == 11 else { return phone }
        return "\(phone.prefix(3))••••\(phone.suffix(4))"
    }

    private func data(for request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        do {
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse else {
                throw PolestarError.invalidResponse(operation: "China network request")
            }
            guard data.count <= Self.maximumResponseBytes else {
                throw PolestarError.responseTooLarge(operation: "China network request")
            }
            return (data, http)
        } catch let error as PolestarError {
            throw error
        } catch let error as URLError {
            throw PolestarError.network(error)
        } catch {
            throw PolestarError.invalidResponse(operation: "China network request")
        }
    }

    private static func jsonRequest(url: URL, body: [String: Any]) throws -> URLRequest {
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        addGatewayHeaders(to: &request)
        return request
    }

    private static var deviceModel: String { "Mac" }

    /// Exact public headers added by the official Pscn gateway interceptor. The separate
    /// baselinelibrary SignInterceptor is not part of the account-login Retrofit client.
    private static func addGatewayHeaders(to request: inout URLRequest) {
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(protocolVersion, forHTTPHeaderField: "Version")
        request.setValue("Android", forHTTPHeaderField: "Device")
        request.setValue(deviceModel, forHTTPHeaderField: "X-User-Agent")
    }

    private static func httpError(_ status: Int, operation: String) -> PolestarError {
        if status == 401 || status == 403 {
            return .authenticationRequired(.expiredSession)
        }
        return PolestarError.httpFailure(
            statusCode: status,
            forbiddenIsAuthentication: true,
            operation: operation
        ) ?? .invalidResponse(operation: operation)
    }

    private static func decodeLoginPayload(
        _ data: Data,
        requiresGlobal: Bool,
        fallbackRefreshToken: String? = nil
    ) throws -> LoginPayload {
        let root = try dictionaryPayload(data)
        guard let access = string(root, keys: ["accessToken", "access_token"]) else {
            throw PolestarError.incompatibleAPI(operation: "China token response")
        }
        let refresh = string(root, keys: ["refreshToken", "refresh_token"])
            ?? fallbackRefreshToken
            ?? ""
        guard !refresh.isEmpty else {
            throw PolestarError.incompatibleAPI(operation: "China refresh token response")
        }
        let global = dictionary(root["globleToken"] ?? root["globle_token"] ?? root["globalToken"])
            .flatMap { decodeGlobalDictionary($0) }
        if requiresGlobal && global == nil {
            throw PolestarError.incompatibleAPI(operation: "China global token response")
        }
        return LoginPayload(
            accessToken: access,
            refreshToken: refresh,
            tokenType: string(root, keys: ["tokenType", "token_type"]) ?? "Bearer",
            expiresIn: integer(root, keys: ["expiresIn", "expires_in"]) ?? 3_600,
            userUID: string(root, keys: ["userUid", "user_uid"]) ?? "",
            userName: string(root, keys: ["userName", "user_name"]) ?? "",
            global: global
        )
    }

    private static func decodeGlobalPayload(
        _ data: Data,
        fallbackRefreshToken: String,
        fallbackIDToken: String
    ) throws -> GlobalPayload {
        let root = try dictionaryPayload(data)
        let candidate = dictionary(root["globleToken"] ?? root["globle_token"] ?? root["globalToken"]) ?? root
        guard let global = decodeGlobalDictionary(
            candidate,
            fallbackRefreshToken: fallbackRefreshToken,
            fallbackIDToken: fallbackIDToken
        ) else {
            throw PolestarError.incompatibleAPI(operation: "China global token refresh")
        }
        return global
    }

    private static func decodeGlobalDictionary(
        _ root: [String: Any],
        fallbackRefreshToken: String? = nil,
        fallbackIDToken: String? = nil
    ) -> GlobalPayload? {
        guard let access = string(root, keys: ["accessToken", "access_token"]) else { return nil }
        let refresh = string(root, keys: ["refreshToken", "refresh_token"])
            ?? fallbackRefreshToken
            ?? ""
        guard !refresh.isEmpty else { return nil }
        return GlobalPayload(
            accessToken: access,
            refreshToken: refresh,
            idToken: string(root, keys: ["idToken", "id_token"]) ?? fallbackIDToken ?? "",
            tokenType: string(root, keys: ["tokenType", "token_type"]) ?? "Bearer",
            expiresIn: integer(root, keys: ["expiresIn", "expires_in"]) ?? 3_600
        )
    }

    private static func decodeCars(_ data: Data) throws -> [ChinaPolestarCar] {
        let object = try JSONSerialization.jsonObject(with: data)
        let rows: [[String: Any]]
        if let direct = object as? [[String: Any]] {
            rows = direct
        } else if let root = object as? [String: Any],
                  let nested = carRows(root["data"]) ?? carRows(root["result"])
                    ?? carRows(root["cars"]) ?? carRows(root["vehicles"]) {
            rows = nested
        } else {
            throw PolestarError.incompatibleAPI(operation: "China vehicle discovery")
        }
        return rows.compactMap { row -> ChinaPolestarCar? in
            let nested = dictionary(row["carInformation"])
                ?? dictionary(row["vehicle"])
                ?? dictionary(row["car"])
                ?? [:]
            let car = row.merging(nested) { _, nestedValue in nestedValue }
            guard let vin = string(
                car,
                keys: ["vin", "vinCode", "vehicleIdentificationNumber"]
            )?.trimmingCharacters(in: .whitespacesAndNewlines),
                  !vin.isEmpty else { return nil }
            let maximumLocations = integer(car, keys: ["chargingMaxNumberOfLocations"]) ?? 0
            let supportsChargeLocations =
                boolean(car, keys: ["supportsChargeLocationsChargeAmperageLimitSetting"])
                || boolean(car, keys: ["supportsChargeLocationsChargeTimerMinChargeLevelSetting"])
                || boolean(car, keys: ["supportsChargeLocationsDepartureTime"])
                || maximumLocations > 0
            return ChinaPolestarCar(
                vin: vin.uppercased(),
                market: string(car, keys: ["market", "countryCode"]),
                modelName: string(car, keys: ["modelName", "vehicleTypeName", "model"]),
                modelYear: string(car, keys: ["modelYear", "year"]),
                registrationPlate: string(car, keys: ["registrationPlate", "licensePlate"]),
                structureWeek: string(car, keys: ["structureWeek"])
                    ?? integer(car, keys: ["structureWeek"]).map(String.init),
                consumerSoftwareVersion: string(
                    car,
                    keys: ["consumerSoftwareVersion", "softwareVersion"]
                ),
                supportsChargingFunctions: boolean(car, keys: ["supportsChargingFunctions"]),
                supportsGlobalChargeAmperageLimitSetting: boolean(
                    car, keys: ["supportsGlobalChargeAmperageLimitSetting"]
                ),
                supportsTargetChargeLevelSetting: boolean(
                    car, keys: ["supportsTargetChargeLevelSetting"]
                ),
                supportsGlobalChargeTimer: boolean(car, keys: ["supportsGlobalChargeTimer"]),
                supportsChargeLocations: supportsChargeLocations,
                supportsParkingClimatizationTimerSettings: boolean(
                    car, keys: ["supportsParkingClimatizationTimerSettings"]
                )
            )
        }
    }

    private static func dictionaryPayload(_ data: Data) throws -> [String: Any] {
        guard let object = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw PolestarError.incompatibleAPI(operation: "China JSON response")
        }
        for key in ["data", "result"] {
            if let nested = dictionary(object[key]),
               nested["accessToken"] != nil || nested["access_token"] != nil
                || nested["globleToken"] != nil || nested["globle_token"] != nil {
                return nested
            }
        }
        return object
    }

    private static func dictionary(_ value: Any?) -> [String: Any]? {
        value as? [String: Any]
    }

    private static func carRows(_ value: Any?) -> [[String: Any]]? {
        if let rows = value as? [[String: Any]] { return rows }
        guard let container = value as? [String: Any] else { return nil }
        for key in ["cars", "vehicles", "list", "content", "result", "data"] {
            if let rows = carRows(container[key]) { return rows }
        }
        return nil
    }

    private static func string(_ root: [String: Any], _ key: String) -> String? {
        if let value = root[key] as? String, !value.isEmpty { return value }
        if let value = root[key] as? NSNumber { return value.stringValue }
        return nil
    }

    private static func string(_ root: [String: Any], keys: [String]) -> String? {
        for key in keys {
            if let value = string(root, key) { return value }
        }
        return nil
    }

    private static func integer(_ root: [String: Any], _ key: String) -> Int? {
        if let value = root[key] as? NSNumber { return value.intValue }
        if let value = root[key] as? String { return Int(value) }
        return nil
    }

    private static func integer(_ root: [String: Any], keys: [String]) -> Int? {
        for key in keys {
            if let value = integer(root, key) { return value }
        }
        return nil
    }

    private static func boolean(_ root: [String: Any], keys: [String]) -> Bool {
        for key in keys {
            if let value = root[key] as? Bool { return value }
            if let value = root[key] as? NSNumber { return value.boolValue }
            if let value = root[key] as? String {
                switch value.lowercased() {
                case "true", "1", "yes": return true
                case "false", "0", "no": return false
                default: continue
                }
            }
        }
        return false
    }

    private static func gatewayCode(in data: Data) -> Int? {
        guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return nil }
        return integer(root, "code") ?? dictionary(root["error"]).flatMap { integer($0, "code") }
    }

    private static func gatewayMessage(in data: Data) -> String? {
        guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return nil }
        let message = string(root, "message") ?? string(root, "msg")
            ?? dictionary(root["error"]).flatMap { string($0, "message") }
        return message.map { String($0.prefix(180)) }
    }

    private static func expiry(token: String, expiresIn: Int, now: Date) -> Date {
        if let jwtExpiry = jwtExpiry(token), jwtExpiry > now { return jwtExpiry }
        return now.addingTimeInterval(TimeInterval(max(expiresIn, 300)))
    }

    private static func jwtExpiry(_ token: String) -> Date? {
        let parts = token.split(separator: ".")
        guard parts.count > 1 else { return nil }
        var encoded = String(parts[1]).replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        encoded.append(String(repeating: "=", count: (4 - encoded.count % 4) % 4))
        guard let data = Data(base64Encoded: encoded),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let epoch = integer(root, "exp") else { return nil }
        return Date(timeIntervalSince1970: TimeInterval(epoch))
    }
}
