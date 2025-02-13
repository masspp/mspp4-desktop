package ninja.mspp.view.mode.mirror;

import java.awt.Color;

import ninja.mspp.core.model.ms.Chromatogram;
import ninja.mspp.core.model.ms.DataPoints;

public class MirrorChromatogramCanvas extends MirrorCanvas {

	public MirrorChromatogramCanvas() {
		super("RT", "Int.");
		this.setProfileColor(Color.BLUE);
	}
	
	public void setChromatogram1(Chromatogram chromatogram) {
		DataPoints points = chromatogram.readDataPoints();
		this.setPoints(points);
	}
	
	public void setChromatogram2(Chromatogram chromatogram) {
		DataPoints points = chromatogram.readDataPoints();
		this.setPoints2(points);
	}
}
