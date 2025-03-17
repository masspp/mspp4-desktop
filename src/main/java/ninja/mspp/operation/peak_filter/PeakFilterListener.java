package ninja.mspp.operation.peak_filter;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.HeatMapCanvasForeground;
import ninja.mspp.core.annotation.method.MenuAction;
import ninja.mspp.core.annotation.method.SpectrumCanvasForeground;
import ninja.mspp.core.annotation.parameter.ActiveSample;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.core.model.view.HeatMap;
import ninja.mspp.core.view.DrawInfo;
import ninja.mspp.core.view.ViewInfo;
import ninja.mspp.view.GuiManager;

@Listener("Peak Filter")
public class PeakFilterListener {
	@MenuAction("Tools > Peak Filter...")
	public void onMenu(@ActiveSample Sample sample) throws IOException {
		MsppManager manager = MsppManager.getInstance();
		GuiManager guiManager = GuiManager.getInstance();
		
		if(sample == null) {
			ResourceBundle messages = manager.getMessages();
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText(messages.getString("sample.not_selected.title"));
			alert.setContentText(messages.getString("sample.not_selected.content"));
			alert.showAndWait();
		}
		else {
			ViewInfo<PeakFilterDialog> viewInfo
				= guiManager.showDialog(PeakFilterDialog.class, "PeakFilterDialog.fxml", "Peak Filter");
			viewInfo.getController().setSample(sample);
		}
	}
	
	@SpectrumCanvasForeground
	public void drawLabel(DrawInfo<Spectrum> info) {
		PeakFilterManager manager = PeakFilterManager.getInstance();
		manager.drawLabel(info);
	}
	
	@HeatMapCanvasForeground
	public void drawPosition(DrawInfo<HeatMap> info) {
		PeakFilterManager manager = PeakFilterManager.getInstance();
		manager.drawPosition(info);
	}
}
