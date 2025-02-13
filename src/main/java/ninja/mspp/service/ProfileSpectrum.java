package ninja.mspp.service;

import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;

public class ProfileSpectrum extends Spectrum {
	private DataPoints points;
	
	public ProfileSpectrum(Sample sample, DataPoints points) {
		super(sample, "dummy", "dummy", 0, 0.0, 0, Polarity.UNKNOWN, 0.0, 0.0, 0.0, false);
		this.points = points;
	}

	@Override
	protected DataPoints onReadDataPoints() {
		return this.points;
	}
}
