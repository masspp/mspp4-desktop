package ninja.mspp.operation.average;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.Point;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;

/**
 * Spectrum averaged over the given spectra. Like XicChromatogram, the data points
 * are calculated from the source spectra when they are first read, then cached.
 */
public class AverageSpectrum extends Spectrum {
	private List<Spectrum> spectra;
	private DataPoints points;

	public AverageSpectrum(Sample sample, List<Spectrum> spectra, double startRt, double endRt) {
		super(
			sample,
			"Average",
			String.format("Average [RT=%.2f-%.2f, %d spectra]", startRt, endRt, spectra.size()),
			-1,
			meanRt(spectra),
			commonMsLevel(spectra),
			commonPolarity(spectra),
			0.0,
			minMz(spectra),
			maxMz(spectra),
			hasCentroid(spectra)
		);
		this.spectra = new ArrayList<Spectrum>(spectra);
		this.points = null;
	}

	public List<Spectrum> getSpectra() {
		return this.spectra;
	}

	@Override
	protected synchronized DataPoints onReadDataPoints() {
		if(this.points == null) {
			if(this.isCentroidMode()) {
				this.points = this.averageCentroids();
			}
			else {
				this.points = this.averageProfiles();
			}
		}
		DataPoints copy = new DataPoints();
		copy.addAll(this.points);
		return copy;
	}

	/**
	 * Centroid spectra: intensities at the same m/z are summed and divided by the number of spectra.
	 */
	private DataPoints averageCentroids() {
		DataPoints all = new DataPoints();
		for(Spectrum spectrum : this.spectra) {
			all.addAll(spectrum.readDataPoints());
		}
		Collections.sort(all);

		double count = (double)this.spectra.size();
		DataPoints result = new DataPoints();
		int i = 0;
		while(i < all.size()) {
			double x = all.get(i).getX();
			double total = 0.0;
			while(i < all.size() && all.get(i).getX() == x) {
				total += all.get(i).getY();
				i++;
			}
			result.add(new Point(x, total / count));
		}
		return result;
	}

	/**
	 * Profile spectra: every spectrum is linearly interpolated onto the union of all m/z values
	 * (0 outside its own m/z range), then the intensities are averaged.
	 */
	private DataPoints averageProfiles() {
		List<double[]> xsList = new ArrayList<double[]>();
		List<double[]> ysList = new ArrayList<double[]>();
		int total = 0;
		for(Spectrum spectrum : this.spectra) {
			DataPoints spectrumPoints = spectrum.readDataPoints();
			Collections.sort(spectrumPoints);
			double[] xs = new double[spectrumPoints.size()];
			double[] ys = new double[spectrumPoints.size()];
			for(int i = 0; i < xs.length; i++) {
				xs[i] = spectrumPoints.get(i).getX();
				ys[i] = spectrumPoints.get(i).getY();
			}
			xsList.add(xs);
			ysList.add(ys);
			total += xs.length;
		}

		double[] grid = new double[total];
		int offset = 0;
		for(double[] xs : xsList) {
			System.arraycopy(xs, 0, grid, offset, xs.length);
			offset += xs.length;
		}
		Arrays.sort(grid);
		int size = 0;
		for(int i = 0; i < grid.length; i++) {
			if(size == 0 || grid[i] != grid[size - 1]) {
				grid[size] = grid[i];
				size++;
			}
		}

		double[] sums = new double[size];
		for(int n = 0; n < xsList.size(); n++) {
			double[] xs = xsList.get(n);
			double[] ys = ysList.get(n);
			if(xs.length == 0) {
				continue;
			}
			int start = Arrays.binarySearch(grid, 0, size, xs[0]);
			int end = Arrays.binarySearch(grid, 0, size, xs[xs.length - 1]);
			int j = 0;
			for(int k = start; k <= end; k++) {
				double x = grid[k];
				while(j + 1 < xs.length && xs[j + 1] <= x) {
					j++;
				}
				if(xs[j] == x || j + 1 >= xs.length) {
					sums[k] += ys[j];
				}
				else {
					sums[k] += ys[j] + (ys[j + 1] - ys[j]) * (x - xs[j]) / (xs[j + 1] - xs[j]);
				}
			}
		}

		double count = (double)this.spectra.size();
		DataPoints result = new DataPoints();
		for(int k = 0; k < size; k++) {
			result.add(new Point(grid[k], sums[k] / count));
		}
		return result;
	}

	private static double meanRt(List<Spectrum> spectra) {
		double total = 0.0;
		for(Spectrum spectrum : spectra) {
			total += spectrum.getRt();
		}
		return spectra.isEmpty() ? 0.0 : total / (double)spectra.size();
	}

	private static int commonMsLevel(List<Spectrum> spectra) {
		int level = spectra.isEmpty() ? 0 : spectra.get(0).getMsLevel();
		for(Spectrum spectrum : spectra) {
			if(spectrum.getMsLevel() != level) {
				return 0;
			}
		}
		return level;
	}

	private static Polarity commonPolarity(List<Spectrum> spectra) {
		Polarity polarity = spectra.isEmpty() ? Polarity.UNKNOWN : spectra.get(0).getPolarity();
		for(Spectrum spectrum : spectra) {
			if(spectrum.getPolarity() != polarity) {
				return Polarity.UNKNOWN;
			}
		}
		return polarity;
	}

	private static double minMz(List<Spectrum> spectra) {
		double mz = Double.MAX_VALUE;
		for(Spectrum spectrum : spectra) {
			mz = Math.min(mz, spectrum.getMinMz());
		}
		return spectra.isEmpty() ? 0.0 : mz;
	}

	private static double maxMz(List<Spectrum> spectra) {
		double mz = 0.0;
		for(Spectrum spectrum : spectra) {
			mz = Math.max(mz, spectrum.getMaxMz());
		}
		return mz;
	}

	private static boolean hasCentroid(List<Spectrum> spectra) {
		for(Spectrum spectrum : spectra) {
			if(spectrum.isCentroidMode()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return this.getTitle();
	}
}
