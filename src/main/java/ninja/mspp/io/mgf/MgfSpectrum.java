package ninja.mspp.io.mgf;

import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;

/**
 * Spectrum read from an MGF file. MGF files contain peak lists, so the spectrum
 * is always in centroid mode.
 */
public class MgfSpectrum extends Spectrum {
	private final DataPoints points;

	public MgfSpectrum(
			Sample sample,
			String name,
			String title,
			int scanNumber,
			double rt,
			int msLevel,
			Polarity polarity,
			double precursorMass,
			DataPoints points
	) {
		super(
			sample,
			name,
			title,
			scanNumber,
			rt,
			msLevel,
			polarity,
			precursorMass,
			points.isEmpty() ? -1.0 : points.get(0).getX(),
			points.isEmpty() ? -1.0 : points.get(points.size() - 1).getX(),
			true
		);
		this.points = points;
	}

	@Override
	protected DataPoints onReadDataPoints() {
		return this.points;
	}
}
