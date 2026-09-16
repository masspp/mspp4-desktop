package ninja.mspp.io.mzml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

/**
 * Pre-scans an mzML file with a byte-level scanner and indexes the byte position
 * and encoding metadata of every &lt;binary&gt; element. This bypasses MSDK 0.0.27's
 * decoder, which mis-tracks file positions for msconvert-produced binary chunks.
 *
 * <p>Uses byte-level scanning rather than StAX because:
 * <ul>
 *   <li>{@code Location.getCharacterOffset()} returns {@code int}, overflowing on
 *       files larger than 2 GiB.</li>
 *   <li>StAX implementations may report positions that differ from raw byte offsets
 *       once internal buffering kicks in.</li>
 * </ul>
 *
 * <p>Assumes mzML metadata regions are ASCII (true for msconvert and standard
 * writers). Base64 content is also pure ASCII, so byte offset == char offset.
 */
public class MzmlBinaryIndex {
	public enum ArrayType { MZ, INTENSITY, TIME, OTHER }
	public enum Compression { NONE, ZLIB }
	public enum Precision { FLOAT32, FLOAT64 }

	/** Unit accession of a time array recorded in minutes. */
	public static final String UNIT_MINUTE = "UO:0000031";

	public static class Entry {
		public final long position;
		public final long encodedLength;
		public final long arrayLength;
		public final Compression compression;
		public final Precision precision;
		public final ArrayType type;
		public final String unitAccession;

		Entry(long position, long encodedLength, long arrayLength,
				Compression compression, Precision precision, ArrayType type, String unitAccession) {
			this.position = position;
			this.encodedLength = encodedLength;
			this.arrayLength = arrayLength;
			this.compression = compression;
			this.precision = precision;
			this.type = type;
			this.unitAccession = unitAccession;
		}
	}

	private final File file;
	private final List<List<Entry>> spectrumEntries = new ArrayList<List<Entry>>();
	private final List<List<Entry>> chromatogramEntries = new ArrayList<List<Entry>>();

	public MzmlBinaryIndex(File mzmlFile) throws IOException {
		this(mzmlFile, null);
	}

	public MzmlBinaryIndex(File mzmlFile, DoubleConsumer progress) throws IOException {
		this.file = mzmlFile;
		this.scan(progress);
	}

	public File getFile() {
		return this.file;
	}

	public Entry getSpectrumEntry(int spectrumIdx, ArrayType type) {
		return findEntry(this.spectrumEntries, spectrumIdx, type);
	}

	public Entry getChromatogramEntry(int chromIdx, ArrayType type) {
		return findEntry(this.chromatogramEntries, chromIdx, type);
	}

	public int getSpectrumCount() {
		return this.spectrumEntries.size();
	}

	public int getChromatogramCount() {
		return this.chromatogramEntries.size();
	}

	private static Entry findEntry(List<List<Entry>> all, int idx, ArrayType type) {
		if(idx < 0 || idx >= all.size()) {
			return null;
		}
		for(Entry e : all.get(idx)) {
			if(e.type == type) {
				return e;
			}
		}
		return null;
	}

	private void scan(DoubleConsumer progress) throws IOException {
		long fileLen = this.file.length();
		long lastReport = 0L;
		try(InputStream in = new FileInputStream(this.file)) {
			byte[] buf = new byte[1 << 16];
			long basePos = 0L;
			ScanState s = new ScanState();
			StringBuilder tag = new StringBuilder(128);
			boolean inTag = false;
			int n;
			while((n = in.read(buf)) > 0) {
				for(int i = 0; i < n; i++) {
					byte b = buf[i];
					if(!inTag) {
						if(b == (byte) '<') {
							tag.setLength(0);
							inTag = true;
						}
					}
					else if(b == (byte) '>') {
						long posAfterTag = basePos + i + 1L;
						this.processTag(tag, posAfterTag, s);
						inTag = false;
					}
					else {
						tag.append((char) (b & 0xFF));
					}
				}
				basePos += n;
				if(progress != null && fileLen > 0L && basePos - lastReport > fileLen / 100L) {
					lastReport = basePos;
					progress.accept((double) basePos / (double) fileLen);
				}
			}
		}
	}

	private void processTag(StringBuilder tag, long posAfterTag, ScanState s) {
		if(tag.length() == 0) {
			return;
		}
		char first = tag.charAt(0);
		if(first == '?' || first == '!') {
			return; // XML decl or comment/doctype
		}
		boolean closing = first == '/';
		int nameStart = closing ? 1 : 0;
		int nameEnd = nameStart;
		while(nameEnd < tag.length()) {
			char c = tag.charAt(nameEnd);
			if(c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '/') {
				break;
			}
			nameEnd++;
		}
		String name = tag.substring(nameStart, nameEnd);

		if(closing) {
			if("binaryDataArray".equals(name)) {
				s.inBinaryArray = false;
			}
			return;
		}

		if("spectrum".equals(name)) {
			s.currentList = new ArrayList<Entry>();
			this.spectrumEntries.add(s.currentList);
		}
		else if("chromatogram".equals(name)) {
			s.currentList = new ArrayList<Entry>();
			this.chromatogramEntries.add(s.currentList);
		}
		else if("binaryDataArray".equals(name)) {
			s.currentEncodedLength = parseLongAttr(tag, "encodedLength");
			s.currentArrayLength = parseLongAttr(tag, "arrayLength");
			s.currentCompression = Compression.NONE;
			s.currentPrecision = Precision.FLOAT64;
			s.currentType = ArrayType.OTHER;
			s.currentUnit = null;
			s.inBinaryArray = true;
		}
		else if("cvParam".equals(name) && s.inBinaryArray) {
			String acc = parseStringAttr(tag, "accession");
			if(acc == null) {
				return;
			}
			if("MS:1000523".equals(acc)) {
				s.currentPrecision = Precision.FLOAT64;
			}
			else if("MS:1000521".equals(acc)) {
				s.currentPrecision = Precision.FLOAT32;
			}
			else if("MS:1000574".equals(acc)) {
				s.currentCompression = Compression.ZLIB;
			}
			else if("MS:1000576".equals(acc)) {
				s.currentCompression = Compression.NONE;
			}
			else if("MS:1000514".equals(acc)) {
				s.currentType = ArrayType.MZ;
			}
			else if("MS:1000515".equals(acc)) {
				s.currentType = ArrayType.INTENSITY;
			}
			else if("MS:1000595".equals(acc)) {
				s.currentType = ArrayType.TIME;
				s.currentUnit = parseStringAttr(tag, "unitAccession");
			}
		}
		else if("binary".equals(name) && s.inBinaryArray && s.currentList != null) {
			Entry entry = new Entry(
				posAfterTag,
				s.currentEncodedLength,
				s.currentArrayLength,
				s.currentCompression,
				s.currentPrecision,
				s.currentType,
				s.currentUnit
			);
			s.currentList.add(entry);
		}
	}

	private static String parseStringAttr(StringBuilder tag, String name) {
		String t = tag.toString();
		int from = 0;
		while(true) {
			int idx = t.indexOf(name, from);
			if(idx < 0) {
				return null;
			}
			int afterName = idx + name.length();
			if(afterName >= t.length()) {
				return null;
			}
			boolean before = idx == 0 || isAttrSep(t.charAt(idx - 1));
			char ec = skipWs(t, afterName);
			if(before && ec == '=') {
				int eqIdx = indexOfNonWs(t, afterName) + 1;
				int qIdx = indexOfNonWs(t, eqIdx);
				if(qIdx < 0) {
					return null;
				}
				char quote = t.charAt(qIdx);
				if(quote != '"' && quote != '\'') {
					return null;
				}
				int end = t.indexOf(quote, qIdx + 1);
				if(end < 0) {
					return null;
				}
				return t.substring(qIdx + 1, end);
			}
			from = afterName;
		}
	}

	private static long parseLongAttr(StringBuilder tag, String name) {
		String s = parseStringAttr(tag, name);
		if(s == null) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		}
		catch(NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean isAttrSep(char c) {
		return c == ' ' || c == '\t' || c == '\n' || c == '\r';
	}

	private static char skipWs(String t, int from) {
		int i = from;
		while(i < t.length() && isAttrSep(t.charAt(i))) {
			i++;
		}
		return i < t.length() ? t.charAt(i) : '\0';
	}

	private static int indexOfNonWs(String t, int from) {
		int i = from;
		while(i < t.length() && isAttrSep(t.charAt(i))) {
			i++;
		}
		return i < t.length() ? i : -1;
	}

	private static class ScanState {
		List<Entry> currentList;
		boolean inBinaryArray;
		long currentEncodedLength;
		long currentArrayLength;
		Compression currentCompression = Compression.NONE;
		Precision currentPrecision = Precision.FLOAT64;
		ArrayType currentType = ArrayType.OTHER;
		String currentUnit;
	}
}
