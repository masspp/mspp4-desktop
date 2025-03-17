package ninja.mspp.core.view;

import ninja.mspp.core.model.ms.Spectrum;

public class SpectrumActionEvent {
	private Spectrum spectrum;
	private double mz;
	
	public SpectrumActionEvent(Spectrum spectrum, double mz) {
		this.spectrum = spectrum;
		this.mz = mz;
	}
	
	public Spectrum getSpectrum() {
		return this.spectrum;
	}
	
	public double getMz() {
		return this.mz;
	}
}