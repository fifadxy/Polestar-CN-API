package io.github.polestarcn.companion.digitalkey;

/**
 * Integration boundary for Polestar 2 digital-key research.
 *
 * No production implementation is enabled until the normal new-device
 * provisioning chain has been verified with the user's authorized vehicle.
 */
public interface DigitalKeyProvider {
  DigitalKeyStatus status();
  boolean isOperational();
}
