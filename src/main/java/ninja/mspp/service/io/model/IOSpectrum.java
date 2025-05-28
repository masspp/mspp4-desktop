package ninja.mspp.service.io.model;

import ninja.mspp.core.api.io.model.Scan;
import ninja.mspp.core.api.io.model.ScanPoint;
import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.Point;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;

public class IOSpectrum extends Spectrum {
	private DataPoints points;
	
	public IOSpectrum(Sample sample, int scanNumber, Scan scan) {
		super(
			sample,
			"Scan " + scanNumber,
			"",
			scanNumber, 
			getDouble(scan.getRt(), -1.0),
			getInt(scan.getMsLevel(), 1),
			Polarity.UNKNOWN,
			getDouble(scan.getPrecursorMz(), -1.0),
			getDouble(scan.getMinMz(), -1.0),
			getDouble(scan.getMaxMz(), -1.0),
			getBoolean(scan.getCentroidMode(), false)
		);
		this.points = new DataPoints();
		
		for (int i = 0; i < scan.getPoints().size(); i++) {
			ScanPoint point = scan.getPoints().get(i);
			this.points.add(new Point(point.getX(), point.getY()));
		}
	}
	
	private static double getDouble(Double value, double defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		return value;
	}
	
	private static int getInt(Integer value, int defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		return value;
	}
	
	private static boolean getBoolean(Boolean value, boolean defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		return value;
	}

	@Override
	protected DataPoints onReadDataPoints() {
		return this.points;
	}
	

}
