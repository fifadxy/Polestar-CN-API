package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import io.github.polestarcn.companion.api.ProtoWire;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.Test;

/** Synthetic wire fixtures match the independently inspected reference APK mapping. */
public class MileageMappingTest {
  private static byte[] doubleField(int field, double value) {
    return ProtoWire.concat(ProtoWire.varint(((long) field << 3) | 1L),
        ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array());
  }

  private static MileageSnapshot parse(byte[] data) {
    return MileageSnapshot.fromReply(ProtoWire.decode(ProtoWire.bytesField(3, data)));
  }

  @Test public void readsResponseThreeAndConvertsOnlyTotalMeters() {
    byte[] data = ProtoWire.concat(ProtoWire.varintField(2, 12_345_678L),
        ProtoWire.fixed32Field(3, 127.5f), doubleField(4, 43.25));
    byte[] reply = ProtoWire.concat(ProtoWire.varintField(2, 1), ProtoWire.bytesField(3, data));
    MileageSnapshot mileage = MileageSnapshot.fromReply(ProtoWire.decode(reply));
    assertEquals(Long.valueOf(12_345_678L), mileage.totalMeters);
    assertEquals(Integer.valueOf(12_345), mileage.wholeKm());
    assertEquals("12345 km", mileage.totalText());
    assertEquals(127.5, mileage.manualKm, 0.0001);
    assertEquals(43.25, mileage.automaticKm, 0.0001);
  }

  @Test public void preservesMetersBeyondSignedIntRange() {
    MileageSnapshot mileage = parse(ProtoWire.varintField(2, 3_000_123_456L));
    assertEquals(Long.valueOf(3_000_123_456L), mileage.totalMeters);
    assertEquals("3000123 km", mileage.totalText());
  }

  @Test public void neverTreatsMetadataBranchAsMileage() {
    byte[] wrongBranch = ProtoWire.bytesField(2, ProtoWire.varintField(2, 999_000));
    MileageSnapshot mileage = MileageSnapshot.fromReply(ProtoWire.decode(wrongBranch));
    assertNull(mileage.totalMeters);
    assertNull(mileage.manualKm);
    assertNull(mileage.automaticKm);
  }

  @Test public void missingTotalDoesNotDiscardEitherTrip() {
    MileageSnapshot mileage = parse(ProtoWire.concat(ProtoWire.fixed32Field(3, 0f), doubleField(4, 0.75)));
    assertNull(mileage.totalMeters);
    assertEquals(0.0, mileage.manualKm, 0.0001);
    assertEquals(0.75, mileage.automaticKm, 0.0001);
    assertEquals("--", mileage.totalText());
  }

  @Test public void invalidNumbersRemainUnknown() {
    MileageSnapshot mileage = parse(ProtoWire.concat(ProtoWire.varintField(2, -1),
        ProtoWire.fixed32Field(3, Float.NaN), doubleField(4, Double.POSITIVE_INFINITY)));
    assertNull(mileage.totalMeters);
    assertNull(mileage.manualKm);
    assertNull(mileage.automaticKm);
    assertEquals("--", MileageSnapshot.tripText(Double.NaN));
    assertEquals("--", MileageSnapshot.tripText(-2.5));
  }

  @Test public void floatingTotalCannotMasqueradeAsMeterCounter() {
    MileageSnapshot mileage = parse(ProtoWire.fixed32Field(2, 12000f));
    assertNull(mileage.totalMeters);
  }
}
