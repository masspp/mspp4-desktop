package ninja.mspp.io.mzml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.method.OnOpenSample;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.TicChromatogram;
import ninja.mspp.io.mgf.MgfReader;
import ninja.mspp.io.mzml.ui.ProgressDialog;
import ninja.mspp.view.GuiManager;

/**
 * Opens mzML and MGF files with a progress dialog.
 * Used by the File > Open menu and by dropping files on the main window.
 */
public class MsDataOpener {
	static final String FOLDER_KEY = "MZML_INPUT_FOLDER";

	private static final String MZML_EXTENSION = ".mzml";
	private static final String MGF_EXTENSION = ".mgf";

	/**
	 * @return true if the file is an mzML or MGF file that can be opened.
	 */
	public static boolean isSupported(File file) {
		return isMzml(file) || isMgf(file);
	}

	static boolean isMzml(File file) {
		return hasExtension(file, MZML_EXTENSION);
	}

	static boolean isMgf(File file) {
		return hasExtension(file, MGF_EXTENSION);
	}

	private static boolean hasExtension(File file, String extension) {
		if(file == null || !file.isFile()) {
			return false;
		}
		return file.getName().toLowerCase(Locale.ROOT).endsWith(extension);
	}

	/**
	 * Opens the files one after another.
	 * Must be called on the JavaFX application thread.
	 */
	public static void open(List<File> files) {
		openNext(new ArrayList<File>(files), 0);
	}

	private static void openNext(List<File> files, int index) {
		if(index >= files.size()) {
			return;
		}
		open(files.get(index), () -> openNext(files, index + 1));
	}

	private static void open(File file, Runnable next) {
		GuiManager guiManager = GuiManager.getInstance();
		MsppManager manager = MsppManager.getInstance();

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

		final Task<Sample> task = new Task<Sample>() {
			@Override
			protected Sample call() throws Exception {
				if(isMgf(file)) {
					updateMessage("Reading MGF...");
					return new MgfReader().read(
						file,
						p -> dialog.setProgress(p),
						s -> updateMessage(s)
					);
				}
				updateMessage("Reading mzML...");
				MzmlReader reader = new MzmlReader();
				return reader.read(
					file.getAbsolutePath(),
					p -> dialog.setProgress(p),
					s -> updateMessage(s)
				);
			}
		};
		task.messageProperty().addListener((obs, oldVal, newVal) -> dialog.setStatus(newVal));
		task.setOnSucceeded(e -> {
			dialog.close();
			Sample sample = task.getValue();
			if(sample.getChromatograms().isEmpty()) {
				sample.getChromatograms().add(new TicChromatogram(sample));
			}
			manager.invoke(OnOpenSample.class, sample);
			next.run();
		});
		task.setOnFailed(e -> {
			dialog.close();
			Throwable ex = task.getException();
			if(ex != null) {
				ex.printStackTrace();
			}
			showError(
				"Failed to open " + file.getName(),
				ex == null ? "Unknown error" : ex.getMessage()
			);
			next.run();
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
