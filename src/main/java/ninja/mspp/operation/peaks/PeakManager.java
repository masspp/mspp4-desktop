package ninja.mspp.operation.peaks;

import java.util.Map;

import ninja.mspp.core.model.ms.Chromatogram;
import ninja.mspp.core.model.ms.PeakList;
import ninja.mspp.core.model.ms.Spectrum;

public class PeakManager {
	private Map<Spectrum, PeakList> spectrumPeakMap;
	private Map<Chromatogram, PeakList> chromatogramPeakMap;
	
	private static PeakManager instance;
	
	private PeakManager() {
		this.spectrumPeakMap = new java.util.HashMap<Spectrum, PeakList>();
		this.chromatogramPeakMap = new java.util.HashMap<Chromatogram, PeakList>();
	}
	
	public void setPeaks(Spectrum spectrum, PeakList peaks) {
		this.spectrumPeakMap.put(spectrum, peaks);
	}
	
	public PeakList getPeaks(Spectrum spectrum) {
		return this.spectrumPeakMap.get(spectrum);
	}
	
	public void setPeaks(Chromatogram chromatogram, PeakList peaks) {
		this.chromatogramPeakMap.put(chromatogram, peaks);
	}
	
	public PeakList getPeaks(Chromatogram chromatogram) {
		return this.chromatogramPeakMap.get(chromatogram);
	}
	
	public static PeakManager getInstance() {
		if (instance == null) {
			instance = new PeakManager();
		}
		return instance;
	}
}
