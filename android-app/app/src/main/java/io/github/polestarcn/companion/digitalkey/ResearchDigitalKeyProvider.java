package io.github.polestarcn.companion.digitalkey;

/**
 * Default provider on the research branch. Deliberately non-operational.
 *
 * This prevents static APK evidence or an emulator BLE connection from being
 * mistaken for a verified vehicle key.
 */
public final class ResearchDigitalKeyProvider implements DigitalKeyProvider {
  private final DigitalKeyStatus status;

  public ResearchDigitalKeyProvider(String detail) {
    status = DigitalKeyStatus.researchOnly(detail);
  }

  @Override public DigitalKeyStatus status() {
    return status;
  }

  @Override public boolean isOperational() {
    return false;
  }
}
