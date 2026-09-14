package ninja.mspp.io.mzml;

import java.io.File;

import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.concurrent.WorkerStateEvent;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.MenuAction;
import ninja.mspp.core.annotation.method.OnOpenSample;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.TicChromatogram;
import ninja.mspp.io.mzml.convert.VendorConverter;
import ninja.mspp.io.mzml.ui.DockerRequiredDialog;
import ninja.mspp.io.mzml.ui.ProgressDialog;
import ninja.mspp.view.GuiManager;

@Listener("MS Data Input Listener")
public class MzmlListener {
	private static final String FOLDER_KEY = "MZML_INPUT_FOLDER";

	@MenuAction(value = "File > Open > MS Data...", order = 0)
	public void onOpenMsData() {
		GuiManager guiManager = GuiManager.getInstance();
		MsppManager manager = MsppManager.getInstance();

		String folderName = manager.getParameter(FOLDER_KEY);

		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().add(
			new FileChooser.ExtensionFilter("MS Files", "*.mzML", "*.raw", "*.wiff", "*.lcd")
		);
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("mzML Files", "*.mzML"));
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Thermo RAW Files", "*.raw"));
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Sciex WIFF Files", "*.wiff"));
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Shimadzu LCD Files", "*.lcd"));
		chooser.setTitle("Open MS Data File");
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

		if(!DockerRequiredDialog.check(file.getName())) {
			return;
		}

		File parent = file.getParentFile();
		if(parent != null) {
			manager.saveParameter(FOLDER_KEY, parent.getAbsolutePath());
		}

		final ProgressDialog dialog = new ProgressDialog(
			guiManager.getMainStage(),
			"Opening " + file.getName(),
			"Preparing..."
		);
		dialog.show();

		final boolean willConvert = VendorConverter.isVendorFile(file);
		final double convertWeight = willConvert ? 0.7 : 0.0;
		final double readWeight = 1.0 - convertWeight;

		final Task<Sample> task = new Task<Sample>() {
			@Override
			protected Sample call() throws Exception {
				File toRead = file;
				if(willConvert) {
					updateMessage("Converting " + file.getName() + " to mzML...");
					VendorConverter converter = new VendorConverter();
					converter.setStatusListener(s -> updateMessage(s));
					converter.setProgressListener(p -> {
						if(p < 0.0) {
							dialog.setProgress(-1.0);
						}
						else {
							dialog.setProgress(p * convertWeight);
						}
					});
					toRead = converter.convertToMzml(file);
					dialog.setProgress(convertWeight);
				}
				updateMessage("Reading mzML...");
				MzmlReader reader = new MzmlReader();
				return reader.read(
					toRead.getAbsolutePath(),
					p -> dialog.setProgress(convertWeight + p * readWeight),
					s -> updateMessage(s)
				);
			}
		};
		task.messageProperty().addListener((obs, oldVal, newVal) -> dialog.setStatus(newVal));
		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent e) {
				dialog.close();
				Sample sample = task.getValue();
				if(sample.getChromatograms().isEmpty()) {
					sample.getChromatograms().add(new TicChromatogram(sample));
				}
				manager.invoke(OnOpenSample.class, sample);
			}
		});
		task.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent e) {
				dialog.close();
				Throwable ex = task.getException();
				ex.printStackTrace();
				showError(
					"Failed to open " + file.getName(),
					ex == null ? "Unknown error" : ex.getMessage()
				);
			}
		});

		Thread t = new Thread(task, "ms-data-loader");
		t.setDaemon(true);
		t.start();
	}

	static void showError(String header, String content) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(header);
		if(content != null && content.length() > 200) {
			TextArea area = new TextArea(content);
			area.setEditable(false);
			area.setWrapText(true);
			area.setPrefSize(560, 320);
			alert.getDialogPane().setContent(area);
		}
		else {
			alert.setContentText(content);
		}
		alert.showAndWait();
	}
}
