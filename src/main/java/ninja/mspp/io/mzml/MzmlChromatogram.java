package ninja.mspp.io.mzml;

import ninja.mspp.core.model.ms.Chromatogram;
import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.Point;
import ninja.mspp.core.model.ms.Sample;

public class MzmlChromatogram extends Chromatogram{
	private io.github.msdk.datamodel.Chromatogram chromatogram;
	private MzmlBinaryIndex binaryIndex;
	private int binaryIndexEntry = -1;

	public MzmlChromatogram(Sample sample, io.github.msdk.datamodel.Chromatogram chromatogram) {
		super(
			sample,
			chromatogram.getChromatogramNumber(),
			chromatogram.getChromatogramType().name(),
			chromatogram.getMz() == null ? -1.0 : chromatogram.getMz()
		);
		this.chromatogram = chromatogram;
	}

	public void setBinaryIndex(MzmlBinaryIndex index, int entryIdx) {
		this.binaryIndex = index;
		this.binaryIndexEntry = entryIdx;
	}

	@Override
	protected DataPoints onReadDataPoints() {
		float[] times;
		float[] intensities;
		if(this.binaryIndex != null && this.binaryIndexEntry >= 0) {
			MzmlBinaryIndex.Entry timeEntry = this.binaryIndex.getChromatogramEntry(
				this.binaryIndexEntry, MzmlBinaryIndex.ArrayType.TIME);
			MzmlBinaryIndex.Entry intEntry = this.binaryIndex.getChromatogramEntry(
				this.binaryIndexEntry, MzmlBinaryIndex.ArrayType.INTENSITY);
			try {
				times = MzmlBinaryDecoder.readFloats(this.binaryIndex.getFile(), timeEntry);
				intensities = MzmlBinaryDecoder.readFloats(this.binaryIndex.getFile(), intEntry);
			}
			catch(java.io.IOException e) {
				throw new RuntimeException("Failed to read chromatogram binary data", e);
			}
		}
		else {
			times = this.chromatogram.getRetentionTimes();
			intensities = this.chromatogram.getIntensityValues();
		}
		DataPoints points = new DataPoints();
		int n = Math.min(times.length, intensities.length);
		for(int i = 0; i < n; i++) {
			points.add(new Point(times[i] / 60.0, intensities[i]));
		}
		return points;
	}
}
