package ninja.mspp.view.mode.mirror;

import java.io.IOException;

import javafx.scene.Parent;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.OnSelectChromatogram;
import ninja.mspp.core.annotation.method.OnSelectSpectrum;
import ninja.mspp.core.annotation.method.ViewMode;
import ninja.mspp.core.model.ms.Chromatogram;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.core.view.ViewInfo;
import ninja.mspp.view.GuiManager;

@Listener("Mirror View Mode")
public class MirrorViewListener {
	@ViewMode(value = "Mirror")
	public Parent createMirrorView() throws IOException {
		GuiManager manager = GuiManager.getInstance();
        ViewInfo<MirrorViewMode> info = manager.createWindow(MirrorViewMode.class, "MirrorViewMode.fxml");
        return info.getWindow();
    }
	
	@OnSelectSpectrum
	public void onSpectrumOpen(Spectrum spectrum) {
		MsppManager manager = MsppManager.getInstance();
		String viewMode = manager.getStatus("VIEW_MODE");
		if(viewMode.equals("Mirror")) {
			MirrorViewManager modeManager = MirrorViewManager.getInstance();
			MirrorSpectrumCanvas canvas = modeManager.getSpectrumCanvas();
			if(canvas != null) {
				MirrorViewMode controller = modeManager.getController();
				if(modeManager.isSpectrumUp()) {
					canvas.setSpectrum1(spectrum);
					controller.setSpectrumUpLabel(spectrum.getName());
				}
				else {
					canvas.setSpectrum2(spectrum);
					controller.setSpectrumDownLabel(spectrum.getName());
				}
				controller.openSpectrumTab();
			}
		}
	}
	
	@OnSelectChromatogram
	public void onChromatogramOpen(Chromatogram chromatogram) {
		MsppManager manager = MsppManager.getInstance();
        String viewMode = manager.getStatus("VIEW_MODE");
        if(viewMode.equals("Mirror")) {
            MirrorViewManager modeManager = MirrorViewManager.getInstance();
            MirrorChromatogramCanvas canvas = modeManager.getChromatogramCanvas();
            if(canvas != null) {
            	MirrorViewMode controller = modeManager.getController();
                if(modeManager.isChromatogramUp()) {
                    canvas.setChromatogram1(chromatogram);
                    controller.setChromatogramUpLabel(chromatogram.getName());
                }
                else {
                    canvas.setChromatogram2(chromatogram);
                    controller.setChromatogramDownLabel(chromatogram.getName());
                }
                controller.openChromatogramTab();
            }
        }
	}
}
