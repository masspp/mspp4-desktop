package ninja.mspp.view.mode.mirror;

public class MirrorViewManager {
	private static MirrorViewManager instance;
	
	private MirrorSpectrumCanvas spectrumCanvas;
	private MirrorChromatogramCanvas chromatogramCanvas;
	
	private boolean chromatogramUp;
	private boolean spectrumUp;
	
	private MirrorViewMode controller;
	
	
	private MirrorViewManager() {
		this.spectrumCanvas = null;
		this.chromatogramCanvas = null;
	}

	public MirrorSpectrumCanvas getSpectrumCanvas() {
		return spectrumCanvas;
	}

	public void setSpectrumCanvas(MirrorSpectrumCanvas spectrumCanvas) {
		this.spectrumCanvas = spectrumCanvas;
	}
	
	public MirrorChromatogramCanvas getChromatogramCanvas() {
		return chromatogramCanvas;
	}

	public void setChromatogramCanvas(MirrorChromatogramCanvas chromatogramCanvas) {
		this.chromatogramCanvas = chromatogramCanvas;
	}

	public boolean isChromatogramUp() {
		return chromatogramUp;
	}

	public void setChromatogramUp(boolean chromatogramUp) {
		this.chromatogramUp = chromatogramUp;
	}

	public boolean isSpectrumUp() {
		return spectrumUp;
	}

	public void setSpectrumUp(boolean spectrumUp) {
		this.spectrumUp = spectrumUp;
	}
	
	public MirrorViewMode getController() {
		return controller;
	}

	public void setController(MirrorViewMode controller) {
		this.controller = controller;
	}

	public static MirrorViewManager getInstance() {
		if(instance == null) {
			instance = new MirrorViewManager();
		}
		return instance;
	}
}
