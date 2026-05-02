package ninja.mspp.io.mzml;

import java.util.List;

import com.google.common.collect.Range;

import io.github.msdk.datamodel.IsolationInfo;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.MsSpectrumType;
import io.github.msdk.datamodel.PolarityType;
import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;

public class MzmlSpectrum extends Spectrum {
	private MsScan scan;
	private MzmlBinaryIndex binaryIndex;
	private int binaryIndexEntry = -1;
	private Range<Double> mzRange;
	private boolean mzRangeResolved;
	private double precursorMz = Double.NaN;

	public MzmlSpectrum(Sample sample, MsScan scan) {
		super(
			sample,
			"Scan " + scan.getScanNumber() + String.format(" [%.4f]", getRetentionTime(scan)),
			scan.getScanDefinition(),
			scan.getScanNumber(),
			getRetentionTime(scan),
			scan.getMsLevel(),
			getPolarity(scan.getPolarity()),
			-1.0,
			-1.0,
			-1.0,
			isCentroidMode(scan)
		);
		this.scan = scan;
	}

	public void setBinaryIndex(MzmlBinaryIndex index, int entryIdx) {
		this.binaryIndex = index;
		this.binaryIndexEntry = entryIdx;
	}

	@Override
	public double getPrecursorMass() {
		if(Double.isNaN(this.precursorMz)) {
			this.precursorMz = getPrecursorMz(this.scan);
		}
		return this.precursorMz;
	}

	@Override
	public double getMinMz() {
		Range<Double> r = this.resolveMzRange();
		return r == null ? -1.0 : r.lowerEndpoint();
	}

	@Override
	public double getMaxMz() {
		Range<Double> r = this.resolveMzRange();
		return r == null ? -1.0 : r.upperEndpoint();
	}

	private Range<Double> resolveMzRange() {
		if(!this.mzRangeResolved) {
			this.mzRange = this.scan.getMzRange();
			this.mzRangeResolved = true;
		}
		return this.mzRange;
	}

	@Override
	protected DataPoints onReadDataPoints() {
		double[] masses;
		float[] intensities;
		if(this.binaryIndex != null && this.binaryIndexEntry >= 0) {
			MzmlBinaryIndex.Entry mzEntry = this.binaryIndex.getSpectrumEntry(
				this.binaryIndexEntry, MzmlBinaryIndex.ArrayType.MZ);
			MzmlBinaryIndex.Entry intEntry = this.binaryIndex.getSpectrumEntry(
				this.binaryIndexEntry, MzmlBinaryIndex.ArrayType.INTENSITY);
			try {
				masses = MzmlBinaryDecoder.readDoubles(this.binaryIndex.getFile(), mzEntry);
				intensities = MzmlBinaryDecoder.readFloats(this.binaryIndex.getFile(), intEntry);
			}
			catch(java.io.IOException e) {
				throw new RuntimeException("Failed to read spectrum binary data", e);
			}
		}
		else {
			masses = this.scan.getMzValues();
			intensities = this.scan.getIntensityValues();
		}
		DataPoints points = new DataPoints();
		int n = Math.min(masses.length, intensities.length);
		for(int i = 0; i < n; i++) {
			points.add(new ninja.mspp.core.model.ms.Point(masses[i], intensities[i]));
		}
		return points;
	}
	
	private static Spectrum.Polarity getPolarity(PolarityType type) {
		Polarity polarity = Polarity.UNKNOWN;
        if(type == PolarityType.POSITIVE) {
        	polarity = Polarity.POSITIVE;
        }
		else if (type == PolarityType.NEGATIVE) {
			polarity = Polarity.NEGATIVE;
		}
        return polarity;
	}
	
	private static double getPrecursorMz(MsScan scan) {
		double precursor = -1.0;
		if(scan != null) {
			List<IsolationInfo> isolations = scan.getIsolations();
			if (isolations != null && isolations.size() > 0) {
				IsolationInfo isolation = isolations.get(0);
				if (isolation != null) {
					precursor = isolation.getPrecursorMz();
				}
			}
		}
		return precursor;
	}
	
	private static boolean isCentroidMode(MsScan scan) {
        MsSpectrumType type = scan.getSpectrumType();
        boolean isCentroid = (type == MsSpectrumType.CENTROIDED);
        return isCentroid;

    }
	
	public int getPrecursorScanNumber() {
		int precursorScan = -1;
		List<IsolationInfo> isolations = this.scan.getIsolations();
		if(isolations != null && isolations.size() > 0) {
			IsolationInfo isolation = isolations.get(0);
			Integer integer = isolation.getPrecursorScanNumber();
			if(integer != null) {
				precursorScan = integer.intValue();
			}
		}
		return precursorScan;
	}

	private static Float getRetentionTime(MsScan scan) {
		if (scan != null && scan.getRetentionTime() != null) {
			return scan.getRetentionTime() / 60.0f;
		}
		return 1.0f * scan.getScanNumber();
	}
}
