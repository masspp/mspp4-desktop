package ninja.mspp.view.mode.normal;

import java.io.IOException;

import javafx.scene.Parent;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.OnHeatMap;
import ninja.mspp.core.annotation.method.OnSelectChromatogram;
import ninja.mspp.core.annotation.method.OnSelectSpectrum;
import ninja.mspp.core.annotation.method.Refresh;
import ninja.mspp.core.annotation.method.ViewMode;
import ninja.mspp.core.model.ms.Chromatogram;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.core.model.view.HeatMap;
import ninja.mspp.core.view.ViewInfo;
import ninja.mspp.view.panel.ChromatogramCanvas;
import ninja.mspp.view.panel.HeatMapCanvas;
import ninja.mspp.view.panel.SpectrumCanvas;
import ninja.mspp.view.panel.ThreeDPanel;

@Listener("Normal View Mode")
public class NormalViewListener {
	@ViewMode(value = "Normal", order = 0)
	public Parent createNormalView() throws IOException {
		MsppManager manager = MsppManager.getInstance();
		ViewInfo<NormalViewMode> info = manager.createWindow(NormalViewMode.class, "NormalViewMode.fxml");
		return info.getWindow();
	}
	
	@OnSelectSpectrum
	public void onSelectSpectrum(Spectrum spectrum) {
		MsppManager manager = MsppManager.getInstance();
		String viewMode = manager.getStatus("VIEW_MODE");
		if (viewMode.equals("Normal")) {
			NormalViewModeManager modeManager = NormalViewModeManager.getInstance();
			
			SpectrumCanvas canvas = modeManager.getSpectrumCanvas();
			if (canvas != null) {
				canvas.setSpectrum(spectrum);
			}
		}
	}
	
	@OnSelectChromatogram
	public void onSelectChromatogram(Chromatogram chromatogram) {
		MsppManager manager = MsppManager.getInstance();
		String viewMode = manager.getStatus("VIEW_MODE");
		if (viewMode.equals("Normal")) {
			NormalViewModeManager modeManager = NormalViewModeManager.getInstance();

			ChromatogramCanvas canvas = modeManager.getChromatogramCanvas();
			if (canvas != null) {
				canvas.setChromatogram(chromatogram);
			}
		}
	}
	
	@OnHeatMap
	public void onHeatMap(HeatMap heatmap) {
		MsppManager manager = MsppManager.getInstance();
        String viewMode = manager.getStatus("VIEW_MODE");
        if (viewMode.equals("Normal")) {
            NormalViewModeManager modeManager = NormalViewModeManager.getInstance();
            
            HeatMapCanvas canvas = modeManager.getHeatMapCanvas();
            if (canvas != null) {
                canvas.setHeatMap(heatmap);
            }
            
            ThreeDPanel panel = modeManager.getThreeDPanel();
            if(panel != null) {
            	panel.setHeatmap(heatmap);
            }
        }
	}
	
	@Refresh
	public void refresh() {
		MsppManager manager = MsppManager.getInstance();
		String viewMode = manager.getStatus("VIEW_MODE");
		if (viewMode.equals("Normal")) {
			NormalViewModeManager modeManager = NormalViewModeManager.getInstance();
			modeManager.refresh();
		}
	}
	
}
