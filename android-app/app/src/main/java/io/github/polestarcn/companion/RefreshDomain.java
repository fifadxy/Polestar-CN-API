package io.github.polestarcn.companion;
/** Non-safety periodic telemetry scopes. Remote commands and camping never use this queue. */
public enum RefreshDomain { CORE, CLIMATE, AIR, CHARGING, HEALTH, MAINTENANCE, MILEAGE }
