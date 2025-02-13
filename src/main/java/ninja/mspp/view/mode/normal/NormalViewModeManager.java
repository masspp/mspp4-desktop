package ninja.mspp.view.mode.normal;

import ninja.mspp.view.panel.ChromatogramCanvas;
import ninja.mspp.view.panel.HeatMapCanvas;
import ninja.mspp.view.panel.SpectrumCanvas;
import ninja.mspp.view.panel.ThreeDPanel;

public class NormalViewModeManager {
	private static NormalViewModeManager instance;
	
	private SpectrumCanvas spectrumCanvas;
	private ChromatogramCanvas chromatogramCanvas;
	private HeatMapCanvas heatMapCanvas;
	private ThreeDPanel threeDPanel;
	
	private NormalViewModeManager() {
		this.spectrumCanvas = null;
		this.chromatogramCanvas = null;
	}
	
	public SpectrumCanvas getSpectrumCanvas() {
		return spectrumCanvas;
	}
	
	public void setSpectrumCanvas(SpectrumCanvas spectrumCanvas) {
		this.spectrumCanvas = spectrumCanvas;
	}
	
	public ChromatogramCanvas getChromatogramCanvas() {
		return chromatogramCanvas;
	}
	
	public void setChromatogramCanvas(ChromatogramCanvas chromatogramCanvas) {
		this.chromatogramCanvas = chromatogramCanvas;
	}
	
	public HeatMapCanvas getHeatMapCanvas() {
		return heatMapCanvas;
	}

	public void setHeatMapCanvas(HeatMapCanvas heatMapCanvas) {
		this.heatMapCanvas = heatMapCanvas;
	}
	
	public void setThreeDPanel(ThreeDPanel threeDPanel) {
		this.threeDPanel = threeDPanel;
	}
	
	public ThreeDPanel getThreeDPanel() {
		return this.threeDPanel;
	}

	public void refresh() {
		if (this.spectrumCanvas != null) {
			this.spectrumCanvas.refresh();
		}
		if (this.chromatogramCanvas != null) {
			this.chromatogramCanvas.refresh();
		}
		if(this.heatMapCanvas != null)  {
			this.heatMapCanvas.refresh();
		}
	}
	
	public static NormalViewModeManager getInstance() {
		if (instance == null) {
			instance = new NormalViewModeManager();
		}
		return instance;
	}
}
