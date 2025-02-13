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
	
	public MzmlSpectrum(Sample sample, MsScan scan) {
		super(
			sample,
			"Scan " + scan.getScanNumber() + String.format(" [%.4f]", getRetentionTime(scan)),
			scan.getScanDefinition(),
			scan.getScanNumber(),
			getRetentionTime(scan),
			scan.getMsLevel(),
			getPolarity(scan.getPolarity()),
			getPrecursorMz(scan),
			getStartMz(scan),
			getEndMz(scan),
			isCentroidMode(scan)
		);
		this.scan = scan;
	}

	@Override
	protected DataPoints onReadDataPoints() {
		double[] masses = this.scan.getMzValues();
		float[] intensities = this.scan.getIntensityValues();
		DataPoints points = new DataPoints();
		for (int i = 0; i < masses.length; i++) {
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
	
	private static double getStartMz(MsScan scan) {
		double startMz = -1.0;
		if (scan != null) {
			Range<Double> range = scan.getMzRange();
			if(range != null) {
				startMz = range.lowerEndpoint();
			}
		}
		return startMz;
	}
	
	private static double getEndMz(MsScan scan) {
		double endMz = -1.0;
		if (scan != null) {
			Range<Double> range = scan.getMzRange();
			if (range != null) {
				endMz = range.upperEndpoint();
			}
		}
		return endMz;
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
