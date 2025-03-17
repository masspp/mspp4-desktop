package ninja.mspp.core.view;

import ninja.mspp.core.model.ms.Chromatogram;

public class ChromatogramActionEvent {
	private Chromatogram chromatogram;
	private double rt;
	
	public ChromatogramActionEvent(Chromatogram chromatogram, double rt) {
		this.chromatogram = chromatogram;
		this.rt = rt;
	}
	
	public Chromatogram getChromatogram() {
		return this.chromatogram;
	}
	
	public double getRt() {
		return this.rt;
	}
}