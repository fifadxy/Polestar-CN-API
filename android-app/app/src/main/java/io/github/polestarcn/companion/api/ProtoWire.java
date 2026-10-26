package io.github.polestarcn.companion.api;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ProtoWire {
    private ProtoWire() {}

    public static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            for (byte[] part : parts) if (part != null) out.write(part);
        } catch (java.io.IOException impossible) {
            throw new IllegalStateException(impossible);
        }
        return out.toByteArray();
    }

    public static byte[] stringField(int field, String value) {
        return bytesField(field, value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] bytesField(int field, byte[] value) {
        byte[] body = value == null ? new byte[0] : value;
        return concat(varint(((long) field << 3) | 2L), varint(body.length), body);
    }

    public static byte[] varintField(int field, long value) {
        return concat(varint(((long) field << 3)), varint(value));
    }

    public static byte[] fixed32Field(int field, float value) {
        byte[] raw = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(Float.floatToIntBits(value)).array();
        return concat(varint(((long) field << 3) | 5L), raw);
    }

    public static byte[] varint(long value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long v = value;
        while ((v & ~0x7fL) != 0L) {
            out.write((int) ((v & 0x7fL) | 0x80L));
            v >>>= 7;
        }
        out.write((int) v);
        return out.toByteArray();
    }

    public static Map<Integer, Object> decode(byte[] data) {
        if (data == null) return Collections.emptyMap();
        LinkedHashMap<Integer, Object> out = new LinkedHashMap<>();
        Cursor c = new Cursor();
        while (c.pos < data.length) {
            long tag = readVarint(data, c);
            if (tag == 0) break;
            int field = (int) (tag >>> 3);
            int wire = (int) (tag & 7);
            Object value;
            switch (wire) {
                case 0:
                    value = readVarint(data, c);
                    break;
                case 1:
                    require(data, c.pos, 8);
                    long bits = ByteBuffer.wrap(data, c.pos, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
                    c.pos += 8;
                    value = Double.longBitsToDouble(bits);
                    break;
                case 2:
                    int len = (int) readVarint(data, c);
                    require(data, c.pos, len);
                    byte[] bytes = new byte[len];
                    System.arraycopy(data, c.pos, bytes, 0, len);
                    c.pos += len;
                    value = bytes;
                    break;
                case 5:
                    require(data, c.pos, 4);
                    int raw = ByteBuffer.wrap(data, c.pos, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    c.pos += 4;
                    value = Float.intBitsToFloat(raw);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported protobuf wire type " + wire);
            }
            out.put(field, value);
        }
        return out;
    }

    public static Map<Integer, Object> nested(Map<Integer, Object> map, int field) {
        if (map == null) return Collections.emptyMap();
        Object value = map.get(field);
        if (!(value instanceof byte[])) return Collections.emptyMap();
        try {
            return decode((byte[]) value);
        } catch (RuntimeException e) {
            return Collections.emptyMap();
        }
    }

    public static Integer integer(Map<Integer, Object> map, int field) {
        Object v = map == null ? null : map.get(field);
        return v instanceof Number ? ((Number) v).intValue() : null;
    }

    public static Long longValue(Map<Integer, Object> map, int field) {
        Object v = map == null ? null : map.get(field);
        return v instanceof Number ? ((Number) v).longValue() : null;
    }

    public static Double decimal(Map<Integer, Object> map, int field) {
        Object v = map == null ? null : map.get(field);
        if (!(v instanceof Number)) return null;
        double d = ((Number) v).doubleValue();
        return Double.isFinite(d) ? d : null;
    }

    public static String enumText(Map<Integer, Object> map, int field, String... labels) {
        Integer i = integer(map, field);
        if (i == null || i < 0 || i >= labels.length) return "未知";
        return labels[i];
    }

    private static long readVarint(byte[] data, Cursor c) {
        long result = 0L;
        int shift = 0;
        while (c.pos < data.length && shift < 64) {
            int b = data[c.pos++] & 0xff;
            result |= (long) (b & 0x7f) << shift;
            if ((b & 0x80) == 0) return result;
            shift += 7;
        }
        throw new IllegalArgumentException("Truncated protobuf varint");
    }

    private static void require(byte[] data, int pos, int len) {
        if (len < 0 || pos < 0 || pos + len > data.length)
            throw new IllegalArgumentException("Truncated protobuf field");
    }

    private static final class Cursor { int pos; }
}
