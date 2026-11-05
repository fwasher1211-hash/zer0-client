package dev.zer0.client.ui.core;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/** Minimal fast PNG encoder (RGBA, fastest deflate). ~5x quicker than ImageIO for animated backgrounds. */
public final class Png {
    private Png() {}

    public static byte[] encode(int[] argb, int w, int h) throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream(w * h * 4 + h);
        Deflater def = new Deflater(Deflater.BEST_SPEED);
        try (DeflaterOutputStream z = new DeflaterOutputStream(raw, def, 1 << 16)) {
            byte[] row = new byte[1 + w * 4];
            for (int y = 0; y < h; y++) {
                row[0] = 0;
                for (int x = 0, i = 1; x < w; x++) {
                    int c = argb[y * w + x];
                    row[i++] = (byte) (c >> 16); row[i++] = (byte) (c >> 8); row[i++] = (byte) c; row[i++] = (byte) (c >>> 24);
                }
                z.write(row);
            }
        } finally { def.end(); }
        ByteArrayOutputStream out = new ByteArrayOutputStream(raw.size() + 64);
        DataOutputStream d = new DataOutputStream(out);
        d.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
        DataOutputStream ih = new DataOutputStream(ihdr);
        ih.writeInt(w); ih.writeInt(h); ih.writeByte(8); ih.writeByte(6); ih.writeByte(0); ih.writeByte(0); ih.writeByte(0);
        chunk(d, "IHDR", ihdr.toByteArray());
        chunk(d, "IDAT", raw.toByteArray());
        chunk(d, "IEND", new byte[0]);
        return out.toByteArray();
    }
    private static void chunk(DataOutputStream d, String type, byte[] data) throws IOException {
        d.writeInt(data.length);
        byte[] t = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        d.write(t); d.write(data);
        CRC32 crc = new CRC32(); crc.update(t); crc.update(data);
        d.writeInt((int) crc.getValue());
    }
}
