package ninja.mspp.operation.peaks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceDialog;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.Refresh;
import ninja.mspp.core.annotation.method.SpectrumAction;
import ninja.mspp.core.model.PeakManager;
import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.Peak;
import ninja.mspp.core.model.ms.PeakList;
import ninja.mspp.core.model.ms.Point;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.io.mgf.MgfReader;
import ninja.mspp.io.mzml.MzmlReader;
import ninja.mspp.io.mzml.ui.ProgressDialog;
import ninja.mspp.core.view.SpectrumActionEvent;
import ninja.mspp.view.GuiManager;

/**
 * Imports the peaks of a spectrum of an mzML or MGF file, and uses them as the
 * peaks of the spectrum that is drawn. The imported peaks replace the peaks that
 * were picked automatically, and are shown as labels on the spectrum.
 */
@Listener("Import Peaks")
public class ImportPeaksListener {
	private static final String FOLDER_KEY = "PEAK_IMPORT_FOLDER";

	@SpectrumAction(value = "Import Peaks ...", order = 6)
	public void importPeaks(SpectrumActionEvent event) {
		final Spectrum target = event.getSpectrum();
		if(target == null) {
			return;
		}

		GuiManager guiManager = GuiManager.getInstance();
		MsppManager manager = MsppManager.getInstance();

		String folderName = manager.getParameter(FOLDER_KEY);

		javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
		chooser.getExtensionFilters().add(
			new javafx.stage.FileChooser.ExtensionFilter("Peak Files", "*.mzML", "*.mgf")
		);
		chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("mzML Files", "*.mzML"));
		chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("MGF Files", "*.mgf"));
		chooser.setTitle("Import Peaks");
		if(folderName != null) {
			File folder = new File(folderName);
			if(folder.exists() && folder.isDirectory()) {
				chooser.setInitialDirectory(folder);
			}
		}
		final File file = chooser.showOpenDialog(guiManager.getMainStage());
		if(file == null) {
			return;
		}
		File parent = file.getParentFile();
		if(parent != null) {
			manager.saveParameter(FOLDER_KEY, parent.getAbsolutePath());
		}

		final ProgressDialog dialog = new ProgressDialog(
			guiManager.getMainStage(),
			"Importing peaks from " + file.getName(),
			"Preparing..."
		);
		dialog.show();

		final Task<Sample> task = new Task<Sample>() {
			@Override
			protected Sample call() throws Exception {
				if(file.getName().toLowerCase(Locale.ROOT).endsWith(".mgf")) {
					updateMessage("Reading MGF...");
					return new MgfReader().read(file, p -> dialog.setProgress(p), s -> updateMessage(s));
				}
				updateMessage("Reading mzML...");
				return new MzmlReader().read(
					file.getAbsolutePath(),
					p -> dialog.setProgress(p),
					s -> updateMessage(s)
				);
			}
		};
		task.messageProperty().addListener((obs, oldVal, newVal) -> dialog.setStatus(newVal));
		task.setOnSucceeded(e -> {
			dialog.close();
			this.onRead(target, file, task.getValue());
		});
		task.setOnFailed(e -> {
			dialog.close();
			Throwable ex = task.getException();
			if(ex != null) {
				ex.printStackTrace();
			}
			showMessage(AlertType.ERROR, "Failed to read " + file.getName(),
				ex == null ? "Unknown error" : ex.getMessage());
		});

		Thread thread = new Thread(task, "peak-importer");
		thread.setDaemon(true);
		thread.start();
	}

	private void onRead(Spectrum target, File file, Sample sample) {
		List<Spectrum> spectra = sample == null ? null : sample.getSpectra();
		if(spectra == null || spectra.isEmpty()) {
			showMessage(AlertType.INFORMATION, "No spectrum was found",
				file.getName() + " contains no spectrum.");
			return;
		}

		Spectrum source = spectra.get(0);
		if(spectra.size() > 1) {
			// Let the user choose which spectrum of the file is used as the peaks.
			ChoiceDialog<Spectrum> choice = new ChoiceDialog<Spectrum>(spectra.get(0), spectra);
			choice.setTitle("Import Peaks");
			choice.setHeaderText(file.getName() + " contains " + spectra.size() + " spectra.");
			choice.setContentText("Select the spectrum to import:");
			Optional<Spectrum> selected = choice.showAndWait();
			if(!selected.isPresent()) {
				return;
			}
			source = selected.get();
		}

		PeakList peaks = toPeaks(source.readDataPoints());
		if(peaks.isEmpty()) {
			showMessage(AlertType.INFORMATION, "No peak was found",
				"The selected spectrum of " + file.getName() + " has no peak.");
			return;
		}

		PeakManager.getInstance().setPeaks(target, peaks);
		MsppManager.getInstance().invoke(Refresh.class);
	}

	/** Uses every data point with an intensity above zero as a peak. */
	private static PeakList toPeaks(DataPoints points) {
		PeakList peaks = new PeakList();
		if(points == null) {
			return peaks;
		}
		List<Point> sorted = new ArrayList<Point>(points);
		sorted.sort((a, b) -> Double.compare(a.getX(), b.getX()));
		for(Point point : sorted) {
			if(point.getY() > 0.0) {
				peaks.add(new Peak(point.getX(), point.getY(), point.getX(), point.getX()));
			}
		}
		return peaks;
	}

	private static void showMessage(AlertType type, String header, String content) {
		Alert alert = new Alert(type);
		alert.setTitle("Import Peaks");
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
	}
}
