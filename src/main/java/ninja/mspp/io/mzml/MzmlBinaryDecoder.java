package ninja.mspp.io.mzml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

import ninja.mspp.io.mzml.MzmlBinaryIndex.Compression;
import ninja.mspp.io.mzml.MzmlBinaryIndex.Entry;
import ninja.mspp.io.mzml.MzmlBinaryIndex.Precision;

/**
 * Reads and decodes a single binary array referenced by an {@link MzmlBinaryIndex.Entry},
 * by seeking the file directly. Avoids MSDK 0.0.27's broken decode path entirely.
 */
public class MzmlBinaryDecoder {
	private MzmlBinaryDecoder() {
	}

	public static double[] readDoubles(File file, Entry entry) throws IOException {
		byte[] bytes = readBinary(file, entry);
		int bytesPerValue = entry.precision == Precision.FLOAT64 ? 8 : 4;
		int count = bytes.length / bytesPerValue;
		ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		double[] out = new double[count];
		if(entry.precision == Precision.FLOAT64) {
			for(int i = 0; i < count; i++) {
				out[i] = buf.getDouble();
			}
		}
		else {
			for(int i = 0; i < count; i++) {
				out[i] = buf.getFloat();
			}
		}
		return out;
	}

	public static float[] readFloats(File file, Entry entry) throws IOException {
		byte[] bytes = readBinary(file, entry);
		int bytesPerValue = entry.precision == Precision.FLOAT64 ? 8 : 4;
		int count = bytes.length / bytesPerValue;
		ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		float[] out = new float[count];
		if(entry.precision == Precision.FLOAT64) {
			for(int i = 0; i < count; i++) {
				out[i] = (float) buf.getDouble();
			}
		}
		else {
			for(int i = 0; i < count; i++) {
				out[i] = buf.getFloat();
			}
		}
		return out;
	}

	private static byte[] readBinary(File file, Entry entry) throws IOException {
		if(entry == null || entry.encodedLength <= 0L) {
			return new byte[0];
		}
		byte[] base64 = new byte[(int) entry.encodedLength];
		try(RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			raf.seek(entry.position);
			raf.readFully(base64);
		}
		byte[] decoded = Base64.getDecoder().decode(base64);
		if(entry.compression == Compression.ZLIB) {
			return inflate(decoded);
		}
		return decoded;
	}

	private static byte[] inflate(byte[] data) throws IOException {
		Inflater inflater = new Inflater();
		inflater.setInput(data);
		ByteArrayOutputStream out = new ByteArrayOutputStream(data.length * 2);
		byte[] buf = new byte[1 << 14];
		try {
			while(!inflater.finished()) {
				int n = inflater.inflate(buf);
				if(n == 0) {
					if(inflater.needsInput() || inflater.needsDictionary()) {
						break;
					}
				}
				out.write(buf, 0, n);
			}
		}
		catch(DataFormatException e) {
			throw new IOException("zlib decode failed", e);
		}
		finally {
			inflater.end();
		}
		return out.toByteArray();
	}
}
