package ninja.mspp.io.mzml;

import ninja.mspp.core.model.ms.Chromatogram;
import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.Point;
import ninja.mspp.core.model.ms.Sample;

public class MzmlChromatogram extends Chromatogram{
	private io.github.msdk.datamodel.Chromatogram chromatogram;
	public MzmlChromatogram(Sample sample, io.github.msdk.datamodel.Chromatogram chromatogram) {
		super(
			sample,
			chromatogram.getChromatogramNumber(),
			chromatogram.getChromatogramType().name(),
			chromatogram.getMz() == null ? -1.0 : chromatogram.getMz()
		);
		this.chromatogram = chromatogram;
	}

	@Override
	protected DataPoints onReadDataPoints() {
		float[] times = this.chromatogram.getRetentionTimes();
		float[] intensities = this.chromatogram.getIntensityValues();
		DataPoints points = new DataPoints();
		for (int i = 0; i < times.length; i++) {
			points.add(new Point(times[i], intensities[i]));
		}
		return points;
	}
}
