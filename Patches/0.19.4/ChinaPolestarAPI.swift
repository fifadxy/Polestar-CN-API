import Foundation

actor ChinaPolestarAPI: VehicleProviding, VehicleLiveStreaming {
    static let shared = ChinaPolestarAPI()

    nonisolated let brand: VehicleBrand = .polestar

    private let transport: ChinaPolestarHTTPTransport
    private let grpc: PolestarGRPC
    private var accountSession: ChinaPolestarSession?
    private var knownCars: [CarSummary] = []
    private var carDetails: [String: ChinaPolestarCar] = [:]
    private var tokenRefreshTask: Task<ChinaPolestarSession, Error>?

    private init() {
        transport = ChinaPolestarHTTPTransport()
        grpc = PolestarGRPC(
            defaultsSuiteName: "io.kheirallah.hisingen.polestar-cn.grpc",
            discoveryURL: URL(string: "https://cnepmob.volvocars.com.cn")!,
            pccsURL: URL(string: "https://api.pccs-prod.origin.polestar.cn")!
        )
    }

    var cars: [CarSummary] { knownCars }
    var hasWarmSession: Bool { accountSession != nil && !knownCars.isEmpty }
    var acceptsStoredPasswordSignIn: Bool { false }

    func requestVerificationCode(
        phone: String,
        challenge: ChinaPolestarChallengePayload
    ) async throws {
        try await transport.requestVerificationCode(phone: phone, challenge: challenge)
    }

    @discardableResult
    func completePhoneSignIn(phone: String, code: String) async throws -> Int {
        let signedIn = try await transport.signIn(phone: phone, code: code)
        try ChinaPolestarSessionStore.save(signedIn)
        accountSession = signedIn
        await grpc.invalidateDiscoveredHost()
        try await loadCarsWithTokenRecovery()
        return knownCars.count
    }

    func accountStatus() -> ChinaPolestarAccountStatus {
        let stored = accountSession ?? (try? ChinaPolestarSessionStore.load())
        return ChinaPolestarAccountStatus(
            signedIn: stored != nil,
            maskedPhone: stored.map { ChinaPolestarHTTPTransport.maskedPhone($0.phone) },
            vehicleCount: knownCars.count
        )
    }

    func authenticate(
        email: String,
        password: String,
        preferredVIN: String?,
        features: FeatureSelection
    ) async throws {
        // China interactive SMS authentication is explicitly driven by ChinaPolestarAccountView.
        // Never reinterpret a stored password field as a one-time verification code.
        throw PolestarError.authenticationRequired(.noStoredSession)
    }

    func restoreSession(preferredVIN: String?, features: FeatureSelection) async throws {
        guard let stored = try ChinaPolestarSessionStore.load() else {
            throw PolestarError.notConfigured
        }
        accountSession = stored
        try await loadCarsWithTokenRecovery()
        if let selected = await resolvedVIN(preferred: preferredVIN) {
            await MainActor.run { ChinaPolestarRuntime.rememberChinaVehicle(selected) }
        }
    }

    func resetSession() async {
        tokenRefreshTask?.cancel()
        tokenRefreshTask = nil
        accountSession = nil
        knownCars = []
        carDetails = [:]
        await grpc.invalidateDiscoveredHost()
    }

    func signOut() async throws {
        tokenRefreshTask?.cancel()
        tokenRefreshTask = nil
        accountSession = nil
        knownCars = []
        carDetails = [:]
        await grpc.invalidateDiscoveredHost()
        try ChinaPolestarSessionStore.delete()
    }

    func resolvedVIN(preferred: String?) async -> String? {
        if let preferred {
            let normalized = preferred.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
            if knownCars.contains(where: { $0.vin == normalized }) { return normalized }
        }
        return knownCars.first?.vin
    }

    func reloadVehicleMetadata(vin: String, features: FeatureSelection) async throws {
        try await loadCarsWithTokenRecovery()
    }

    func identitySnapshot(
        for vin: String,
        features: FeatureSelection
    ) async -> VehicleIdentitySnapshot? {
        let normalized = vin.uppercased()
        guard let car = carDetails[normalized] else { return nil }
        return VehicleIdentitySnapshot(
            availability: .unknown,
            modelName: car.modelName,
            modelYear: car.modelYear,
            registrationNo: car.registrationPlate,
            vin: car.vin,
            structureWeek: car.structureWeek,
            accountMarket: car.market ?? "CN"
        )
    }

    func consumerTelemetryOverlay(
        for vin: String,
        features: FeatureSelection
    ) async -> ConsumerTelemetryOverlay? {
        nil
    }

    func fetchVehicleState(vin: String, features: FeatureSelection) async throws -> VehicleState {
        let normalized = vin.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard knownCars.contains(where: { $0.vin == normalized }) else {
            throw PolestarError.invalidResponse(operation: "China vehicle selection")
        }
        let token = try await globalAccessToken()
        do {
            return try await fetchVehicleState(vin: normalized, features: features, token: token)
        } catch let error as PolestarError {
            guard case .authenticationRequired = error else { throw error }
            let refreshed = try await globalAccessToken(forceRefresh: true)
            return try await fetchVehicleState(vin: normalized, features: features, token: refreshed)
        }
    }

    func executeRemoteCommand(
        _ command: RemoteCommand,
        vin: String
    ) async throws -> RemoteCommandResult {
        // The official China app proves the RPC names and token family, but this build does not
        // enable a physical vehicle mutation until request and acknowledgement semantics have
        // been verified on a consenting test vehicle.
        throw RemoteCommandError.unsupported
    }

    func refreshLiveStreamAuthorization() async throws {
        _ = try await globalAccessToken(forceRefresh: true)
    }

    func liveVehicleUpdates(
        vin: String,
        purpose: VehicleLiveStreamPurpose
    ) async throws -> AsyncThrowingStream<VehicleLiveUpdate, Error> {
        let token = try await globalAccessToken()
        return try await grpc.liveUpdates(
            vin: vin.trimmingCharacters(in: .whitespacesAndNewlines).uppercased(),
            accessToken: token,
            purpose: purpose
        )
    }

    private func loadCarsWithTokenRecovery() async throws {
        let token = try await globalAccessToken()
        let userUID = accountSession?.userUID ?? ""
        do {
            applyCars(try await transport.fetchCars(accessToken: token, userUID: userUID))
        } catch let error as PolestarError {
            guard case .authenticationRequired = error else { throw error }
            let refreshed = try await globalAccessToken(forceRefresh: true)
            applyCars(try await transport.fetchCars(
                accessToken: refreshed,
                userUID: accountSession?.userUID ?? userUID
            ))
        }
    }

    private func applyCars(_ discovered: [ChinaPolestarCar]) {
        carDetails = Dictionary(uniqueKeysWithValues: discovered.map { ($0.vin, $0) })
        knownCars = discovered.map { car in
            let title = car.modelName?.isEmpty == false ? car.modelName! : "Polestar"
            return CarSummary(
                vin: car.vin,
                title: title,
                modelName: car.modelName,
                modelYear: car.modelYear,
                registrationNo: car.registrationPlate
            )
        }
    }

    private func globalAccessToken(forceRefresh: Bool = false) async throws -> String {
        if accountSession == nil {
            accountSession = try ChinaPolestarSessionStore.load()
        }
        guard let current = accountSession else { throw PolestarError.notConfigured }
        let refreshMargin: TimeInterval = 5 * 60
        if !forceRefresh,
           !current.globalAccessToken.isEmpty,
           current.globalExpiresAt.timeIntervalSinceNow > refreshMargin {
            return current.globalAccessToken
        }

        if let tokenRefreshTask {
            let refreshed = try await tokenRefreshTask.value
            return refreshed.globalAccessToken
        }

        let task = Task<ChinaPolestarSession, Error> {
            var refreshed = current
            if refreshed.localExpiresAt.timeIntervalSinceNow <= refreshMargin {
                refreshed = try await self.transport.refreshLocalSession(refreshed)
            }
            refreshed = try await self.transport.refreshGlobalSession(refreshed)
            try ChinaPolestarSessionStore.save(refreshed)
            return refreshed
        }
        tokenRefreshTask = task
        do {
            let refreshed = try await task.value
            tokenRefreshTask = nil
            accountSession = refreshed
            await grpc.invalidateDiscoveredHost()
            return refreshed.globalAccessToken
        } catch {
            tokenRefreshTask = nil
            throw error
        }
    }

    private func fetchVehicleState(
        vin: String,
        features: FeatureSelection,
        token: String
    ) async throws -> VehicleState {
        let needsCharging = features.contains(.chargingDetails)
            || features.contains(.batteryDiagnostics)
            || features.contains(.chargingSchedule)
        let needsExterior = features.contains(.exteriorStatus)
        let needsHealth = features.contains(.vehicleHealth) || features.contains(.tyreAndWarnings)
        let needsTrips = features.contains(.tripMeters) || features.contains(.vehicleHealth)
        let needsSoftware = features.contains(.softwareUpdates)
        let needsSchedules = features.contains(.chargingSchedule)
        let needsClimate = features.contains(.climateStatus)

        async let batteryTask = grpc.fetchBattery(vin: vin, accessToken: token)
        async let availabilityTask: GrpcAvailabilityReport? = Self.optional(true) {
            Optional(try await self.grpc.fetchAvailabilityReport(vin: vin, accessToken: token))
        }
        async let targetTask: Int? = Self.optional(needsCharging) {
            try await self.grpc.fetchTargetSoc(vin: vin, accessToken: token)
        }
        async let ampTask: Int? = Self.optional(needsCharging) {
            try await self.grpc.fetchAmpLimit(vin: vin, accessToken: token)
        }
        async let exteriorTask: ExteriorSnapshot? = Self.optional(needsExterior) {
            try await self.grpc.fetchExterior(vin: vin, accessToken: token)
        }
        async let healthTask: GrpcHealthReport? = Self.optional(needsHealth) {
            try await self.grpc.fetchHealth(vin: vin, accessToken: token)
        }
        async let odometerTask: GrpcOdometerReport? = Self.optional(needsTrips) {
            try await self.grpc.fetchOdometer(vin: vin, accessToken: token)
        }
        async let softwareTask: VehicleSoftwareInfo? = Self.optional(needsSoftware) {
            try await self.grpc.fetchSoftware(vin: vin, accessToken: token, locale: "en")
        }
        async let schedulesTask: [VehicleSchedule]? = Self.optional(needsSchedules) {
            Optional(try await self.grpc.fetchChargingSchedules(vin: vin, accessToken: token))
        }
        async let locationsTask: [ChargeLocationSnapshot]? = Self.optional(needsSchedules) {
            Optional(try await self.grpc.fetchChargeLocations(vin: vin, accessToken: token))
        }
        async let climateTask: VehicleClimateStatus? = Self.optional(needsClimate) {
            try await self.grpc.fetchClimate(vin: vin, accessToken: token)
        }
        async let climateTimersTask: [VehicleSchedule]? = Self.optional(needsClimate) {
            Optional(try await self.grpc.fetchClimateTimers(vin: vin, accessToken: token))
        }
        async let connectivityTask: VehicleConnectivity? = Self.optional(
            features.contains(.connectivityDiagnostics)
        ) {
            try await self.grpc.fetchConnectivity(vin: vin, accessToken: token)
        }
        async let airTask: VehicleAirQuality? = Self.optional(features.contains(.airQuality)) {
            try await self.grpc.fetchAirQuality(vin: vin, accessToken: token)
        }
        async let locationTask: VehicleLocation? = Self.optional(features.contains(.vehicleLocation)) {
            try await self.grpc.fetchLocation(vin: vin, accessToken: token)
        }
        async let weatherTask: VehicleWeather? = Self.optional(features.contains(.vehicleWeather)) {
            try await self.grpc.fetchWeather(vin: vin, accessToken: token)
        }
        async let otaTask: VehicleOTACapabilities? = Self.optional(
            features.contains(.softwareUpdates) || features.contains(.vehicleIdentity)
        ) {
            try await self.grpc.fetchMyCars(vin: vin, accessToken: token)
        }

        let battery = try await batteryTask
        let availability = await availabilityTask
        let target = await targetTask
        let amps = await ampTask
        let exterior = await exteriorTask
        let health = await healthTask
        let odometer = await odometerTask
        let software = await softwareTask
        let schedules = await schedulesTask
        let locations = await locationsTask
        let climate = await climateTask
        let climateTimers = await climateTimersTask
        let connectivity = await connectivityTask
        let air = await airTask
        let location = await locationTask
        let weather = await weatherTask
        let ota = await otaTask

        let details = carDetails[vin]
        let now = Date()
        var readingDates: [VehicleReading: Date] = [:]
        if let date = battery.reportedAt {
            readingDates[.battery] = date
            readingDates[.range] = date
            readingDates[.charging] = date
        }
        if let date = availability?.reportedAt { readingDates[.availability] = date }
        if let date = exterior?.reportedAt {
            readingDates[.locks] = date
            readingDates[.openings] = date
        }
        if let date = health?.reportedAt { readingDates[.health] = date }
        if let date = odometer?.reportedAt { readingDates[.odometer] = date }
        if let date = software?.updatedAt { readingDates[.software] = date }
        if let date = connectivity?.updatedAt { readingDates[.connectivity] = date }
        if let date = air?.reportedAt { readingDates[.airQuality] = date }
        if let date = location?.timestamp { readingDates[.location] = date }

        var unavailable: [AppFeature] = []
        Self.mark(&unavailable, .vehicleAvailability,
                  when: features.contains(.vehicleAvailability) && availability == nil)
        Self.mark(&unavailable, .exteriorStatus,
                  when: features.contains(.exteriorStatus) && exterior == nil)
        Self.mark(&unavailable, .vehicleHealth,
                  when: features.contains(.vehicleHealth) && health == nil && odometer == nil)
        Self.mark(&unavailable, .tyreAndWarnings,
                  when: features.contains(.tyreAndWarnings) && health == nil)
        Self.mark(&unavailable, .tripMeters,
                  when: features.contains(.tripMeters) && odometer == nil)
        Self.mark(&unavailable, .softwareUpdates,
                  when: features.contains(.softwareUpdates) && software == nil && ota == nil)
        Self.mark(&unavailable, .chargingSchedule,
                  when: features.contains(.chargingSchedule) && schedules == nil)
        Self.mark(&unavailable, .climateStatus,
                  when: features.contains(.climateStatus) && climate == nil && climateTimers == nil)
        Self.mark(&unavailable, .connectivityDiagnostics,
                  when: features.contains(.connectivityDiagnostics) && connectivity == nil)
        Self.mark(&unavailable, .airQuality,
                  when: features.contains(.airQuality) && air == nil)
        Self.mark(&unavailable, .vehicleLocation,
                  when: features.contains(.vehicleLocation) && location == nil)
        Self.mark(&unavailable, .vehicleWeather,
                  when: features.contains(.vehicleWeather) && weather == nil)
        Self.mark(&unavailable, .vehicleImage, when: features.contains(.vehicleImage))
        Self.mark(&unavailable, .ownerGreeting, when: features.contains(.ownerGreeting))
        for feature in AppFeature.remoteFeatures where features.contains(feature) {
            unavailable.append(feature)
        }

        let energy = EnergyAndChargingSnapshot(
            batteryPercentage: battery.batteryPercentage,
            rangeKm: battery.rangeKm,
            chargingState: battery.chargingState ?? .unknown("UNSPECIFIED"),
            estimatedTimeToFullMinutes: battery.estimatedChargingTimeToFullMinutes,
            estimatedTimeToTargetMinutes: battery.diagnostics.timeToTargetMinutes,
            targetPercentage: target,
            powerWatts: battery.chargingPowerWatts,
            currentAmps: battery.chargingCurrentAmps,
            voltageVolts: battery.chargingVoltageVolts,
            type: battery.chargingType,
            connection: battery.chargerConnection,
            currentLimitAmps: amps,
            reportedBatteryCapacityKwh: battery.reportedBatteryCapacityKwh,
            diagnostics: battery.diagnostics,
            schedules: schedules ?? [],
            locations: locations ?? []
        )

        let identity = VehicleIdentitySnapshot(
            availability: availability?.availability ?? .unknown,
            availabilityReportedAt: availability?.reportedAt,
            availabilityUnknownWireFields: availability?.unknownFields,
            modelName: details?.modelName,
            modelYear: details?.modelYear,
            registrationNo: details?.registrationPlate,
            vin: vin,
            structureWeek: details?.structureWeek,
            accountMarket: details?.market ?? "CN"
        )

        let maintenance = MaintenanceAndHealthSnapshot(
            odometerKm: odometer?.odometerKm,
            odometerKmPrecise: odometer?.odometerKm.map(Double.init),
            details: health?.details,
            service: ServiceSnapshot(
                daysToService: health?.daysToService,
                distanceToServiceKm: health?.distanceToServiceKm,
                serviceWarning: health?.serviceWarning ?? false,
                engineHoursToService: health?.engineHoursToService
            )
        )

        let tripComputer = TripComputerSnapshot(
            manualTripKm: odometer?.manualTripKm,
            automaticTripKm: odometer?.automaticTripKm,
            manualAverageSpeedKmH: odometer?.manualAverageSpeedKmH,
            automaticAverageSpeedKmH: odometer?.automaticAverageSpeedKmH
        )
        let reportedAt = battery.reportedAt
        let age = reportedAt.map { now.timeIntervalSince($0) }
        let isFresh = age.map { $0 >= -120 && $0 <= 600 } ?? false
        let freshness = SnapshotFreshness(
            isCached: !isFresh,
            fetchedAt: now,
            vehicleReportedAt: reportedAt,
            readingDates: readingDates,
            dataWarnings: [],
            unavailableFeatures: Array(Set(unavailable))
        )

        return VehicleState(
            energy: energy,
            identity: identity,
            maintenance: maintenance,
            freshness: freshness,
            exteriorStatus: exterior,
            softwareInfo: software,
            climateStatus: climate,
            climateTimers: climateTimers ?? [],
            tripComputer: tripComputer,
            connectivity: connectivity,
            airQuality: air,
            weather: weather,
            location: location,
            otaCapabilities: ota
        )
    }

    private nonisolated static func optional<T: Sendable>(
        _ enabled: Bool,
        operation: @escaping @Sendable () async throws -> T?
    ) async -> T? {
        guard enabled else { return nil }
        do {
            return try await operation()
        } catch is CancellationError {
            return nil
        } catch {
            return nil
        }
    }

    private nonisolated static func mark(
        _ list: inout [AppFeature],
        _ feature: AppFeature,
        when condition: Bool
    ) {
        if condition { list.append(feature) }
    }
}
