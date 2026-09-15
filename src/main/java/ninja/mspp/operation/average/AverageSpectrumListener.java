package ninja.mspp.operation.average;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.ChromatogramAction;
import ninja.mspp.core.annotation.method.OnSelectSpectrum;
import ninja.mspp.core.model.ms.Chromatogram;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.core.view.ChromatogramActionEvent;
import ninja.mspp.core.view.ViewInfo;
import ninja.mspp.interfaces.Job;
import ninja.mspp.operation.average.AverageSpectrumDialog.Target;
import ninja.mspp.view.GuiManager;

@Listener("Average Spectrum")
public class AverageSpectrumListener {
	@ChromatogramAction(value = "Spectrum...", order = 0)
	public void openSpectrum(ChromatogramActionEvent event) throws IOException, InterruptedException {
		Chromatogram chromatogram = event.getChromatogram();
		if(chromatogram == null) {
			return;
		}

		GuiManager gui = GuiManager.getInstance();
		ViewInfo<AverageSpectrumDialog> info = gui.createWindow(AverageSpectrumDialog.class, "AverageSpectrumDialog.fxml");
		AverageSpectrumDialog controller = info.getController();
		controller.setRtRange(event.getRt(), event.getRt());

		Stage stage = new Stage();
		stage.setTitle("Spectrum");
		stage.initOwner(gui.getMainStage());
		stage.initModality(Modality.WINDOW_MODAL);
		stage.setScene(new Scene(info.getWindow()));
		stage.showAndWait();

		if(controller.isConfirmed()) {
			this.showSpectrum(
				chromatogram.getSample(),
				controller.getStartRt(),
				controller.getEndRt(),
				controller.getTarget()
			);
		}
	}

	private void showSpectrum(Sample sample, double startRt, double endRt, Target target) throws InterruptedException {
		List<Spectrum> candidates = new ArrayList<Spectrum>();
		List<Spectrum> inRange = new ArrayList<Spectrum>();
		for(Spectrum spectrum : sample.getSpectra()) {
			if(target.matches(spectrum)) {
				candidates.add(spectrum);
				if(spectrum.getRt() >= startRt && spectrum.getRt() <= endRt) {
					inRange.add(spectrum);
				}
			}
		}

		if(candidates.isEmpty()) {
			showMessage(AlertType.INFORMATION, "No spectra",
				"The sample does not contain any " + target.getLabel() + " spectra.");
			return;
		}

		MsppManager manager = MsppManager.getInstance();
		if(inRange.isEmpty()) {
			manager.invoke(OnSelectSpectrum.class, findNearest(candidates, startRt, endRt));
			return;
		}
		if(inRange.size() == 1) {
			manager.invoke(OnSelectSpectrum.class, inRange.get(0));
			return;
		}

		AverageSpectrum average = new AverageSpectrum(sample, inRange, startRt, endRt);
		GuiManager.getInstance().startTask(new Job() {
			@Override
			public Object execute() {
				try {
					// Averages the spectra in the background; the result is cached in the spectrum.
					average.readDataPoints();
					return average;
				}
				catch(Exception e) {
					e.printStackTrace();
					return e;
				}
			}

			@Override
			public void onSucceeded(Object result) {
				if(result instanceof Exception) {
					showMessage(AlertType.ERROR, "Failed to average spectra", ((Exception)result).getMessage());
				}
				else {
					manager.invoke(OnSelectSpectrum.class, average);
				}
			}
		});
	}

	private static Spectrum findNearest(List<Spectrum> spectra, double startRt, double endRt) {
		Spectrum nearest = null;
		double minDistance = Double.MAX_VALUE;
		for(Spectrum spectrum : spectra) {
			double rt = spectrum.getRt();
			double distance = rt < startRt ? startRt - rt : rt - endRt;
			if(distance < minDistance) {
				minDistance = distance;
				nearest = spectrum;
			}
		}
		return nearest;
	}

	private static void showMessage(AlertType type, String header, String content) {
		Alert alert = new Alert(type);
		alert.setTitle("Spectrum");
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
	}
}
