package ninja.mspp.view.mode.mirror;

import java.awt.Color;

import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.Spectrum;

public class MirrorSpectrumCanvas extends MirrorCanvas {

	public MirrorSpectrumCanvas() {
		super("m/z", "Intensity");
		this.setProfileColor(Color.RED);
	}

	public void setSpectrum1(Spectrum spectrum) {
		DataPoints points = spectrum.readDataPoints();
		this.setImpulseMode(spectrum.isCentroidMode());
		this.setPoints(points);
	}
	
	public void setSpectrum2(Spectrum spectrum) {
		DataPoints points = spectrum.readDataPoints();
		this.setImpulseMode(spectrum.isCentroidMode());
		this.setPoints2(points);
	}
}
