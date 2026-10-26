package ninja.mspp.io.mgf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.Point;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;

/**
 * Reads spectra from an MGF (Mascot Generic Format) file.
 *
 * <p>Each spectrum is written between BEGIN IONS and END IONS, as header lines
 * such as TITLE, PEPMASS, CHARGE, RTINSECONDS and SCANS, followed by lines of
 * "m/z intensity". Unknown header lines are ignored.
 */
public class MgfReader {

	public Sample read(File file) throws IOException {
		return this.read(file, null, null);
	}

	public Sample read(File file, DoubleConsumer progressListener, Consumer<String> statusListener)
			throws IOException {
		long total = Math.max(file.length(), 1L);
		Sample sample = new Sample(file.getAbsolutePath(), file.getName());

		try(BufferedReader reader = new BufferedReader(
				new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
			long read = 0L;
			double reported = 0.0;
			String line;
			Block block = null;

			while((line = reader.readLine()) != null) {
				// Line separators are not counted exactly; the progress is an estimate.
				read += line.length() + 1;

				String text = line.trim();
				if(text.isEmpty() || text.startsWith("#") || text.startsWith(";")
						|| text.startsWith("!") || text.startsWith("/" + "/")) {
					continue;
				}

				String upper = text.toUpperCase(Locale.ROOT);
				if(upper.startsWith("BEGIN IONS")) {
					block = new Block();
					continue;
				}
				if(upper.startsWith("END IONS")) {
					if(block != null) {
						Spectrum spectrum = block.toSpectrum(sample, sample.getSpectra().size() + 1);
						if(spectrum != null) {
							sample.getSpectra().add(spectrum);
						}
						block = null;
					}
					if(statusListener != null) {
						statusListener.accept("Reading MGF: " + sample.getSpectra().size() + " spectra...");
					}
					continue;
				}
				if(block == null) {
					// Header of the file, for example MASS=Monoisotopic.
					continue;
				}

				int equals = text.indexOf('=');
				if(equals > 0 && !Character.isDigit(text.charAt(0))) {
					block.addHeader(text.substring(0, equals).trim(), text.substring(equals + 1).trim());
				}
				else {
					block.addPoint(text);
				}

				if(progressListener != null) {
					double fraction = Math.min((double)read / (double)total, 1.0);
					if(fraction - reported >= 0.01) {
						reported = fraction;
						progressListener.accept(fraction);
					}
				}
			}

			if(block != null) {
				// The last spectrum has no END IONS line.
				Spectrum spectrum = block.toSpectrum(sample, sample.getSpectra().size() + 1);
				if(spectrum != null) {
					sample.getSpectra().add(spectrum);
				}
			}
		}

		if(sample.getSpectra().isEmpty()) {
			throw new IOException("No spectrum was found in " + file.getName()
				+ ". The file does not look like an MGF file.");
		}
		if(progressListener != null) {
			progressListener.accept(1.0);
		}
		return sample;
	}

	/** One BEGIN IONS ... END IONS block. */
	private static class Block {
		private final DataPoints points = new DataPoints();
		private String title;
		private double precursorMz = -1.0;
		private double rt = -1.0;
		private int scanNumber = -1;
		private Spectrum.Polarity polarity = Spectrum.Polarity.UNKNOWN;

		void addHeader(String key, String value) {
			switch(key.toUpperCase(Locale.ROOT)) {
			case "TITLE":
				this.title = value;
				break;
			case "PEPMASS":
				// "PEPMASS=<m/z>" or "PEPMASS=<m/z> <intensity>"
				this.precursorMz = parseDouble(firstToken(value), -1.0);
				break;
			case "CHARGE":
				this.polarity = parsePolarity(value);
				break;
			case "RTINSECONDS":
				double seconds = parseDouble(firstToken(value), -1.0);
				this.rt = seconds < 0.0 ? -1.0 : seconds / 60.0;
				break;
			case "SCANS":
				this.scanNumber = (int)parseDouble(firstToken(value.replace('-', ' ')), -1.0);
				break;
			default:
				break;
			}
		}

		void addPoint(String line) {
			String[] tokens = line.split("[\\s,]+");
			if(tokens.length < 2) {
				return;
			}
			double mz = parseDouble(tokens[0], Double.NaN);
			double intensity = parseDouble(tokens[1], Double.NaN);
			if(Double.isNaN(mz) || Double.isNaN(intensity)) {
				return;
			}
			this.points.add(new Point(mz, intensity));
		}

		Spectrum toSpectrum(Sample sample, int index) {
			if(this.points.isEmpty()) {
				return null;
			}
			Collections.sort(this.points);

			int scanNumber = this.scanNumber > 0 ? this.scanNumber : index;
			String name = this.rt > 0.0
				? String.format("Scan %d [%.4f]", scanNumber, this.rt)
				: String.format("Scan %d", scanNumber);
			// MGF files hold fragment spectra, unless the precursor is missing.
			int msLevel = this.precursorMz > 0.0 ? 2 : 1;

			return new MgfSpectrum(
				sample,
				name,
				this.title == null ? "" : this.title,
				scanNumber,
				this.rt,
				msLevel,
				this.polarity,
				this.precursorMz,
				this.points
			);
		}

		private static String firstToken(String value) {
			String[] tokens = value.trim().split("[\\s,]+");
			return tokens.length > 0 ? tokens[0] : "";
		}

		private static double parseDouble(String text, double defaultValue) {
			try {
				return Double.parseDouble(text);
			}
			catch(NumberFormatException e) {
				return defaultValue;
			}
		}

		private static Spectrum.Polarity parsePolarity(String value) {
			if(value.contains("-")) {
				return Spectrum.Polarity.NEGATIVE;
			}
			if(value.contains("+")) {
				return Spectrum.Polarity.POSITIVE;
			}
			return Spectrum.Polarity.UNKNOWN;
		}
	}
}
