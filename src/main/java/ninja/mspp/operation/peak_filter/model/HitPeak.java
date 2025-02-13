package ninja.mspp.operation.peak_filter.model;

import java.util.HashMap;
import java.util.Map;

import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.operation.peak_filter.model.entity.FilterPeak;

public class HitPeak {
	private Spectrum spectrum;
	private Map<FilterPeak, Double> hitMap;
	
	public HitPeak(Spectrum spectrum) {
		this.spectrum = spectrum;
		this.hitMap = new HashMap<FilterPeak, Double>();
	}
	
	public Spectrum getSpectrum() {
		return this.spectrum;
	}
	
	public Map<FilterPeak, Double> getHitMap() {
		return this.hitMap;
	}
}
