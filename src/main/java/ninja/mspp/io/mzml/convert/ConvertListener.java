package ninja.mspp.io.mzml.convert;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.MenuAction;
import ninja.mspp.io.mzml.ui.ProgressDialog;
import ninja.mspp.tool.docker.DockerManager;
import ninja.mspp.view.GuiManager;

@Listener("MS Data Convert Listener")
public class ConvertListener {
	private static final String IN_FOLDER_KEY = "MZML_CONVERT_INPUT_FOLDER";
	private static final String OUT_FOLDER_KEY = "MZML_CONVERT_OUTPUT_FOLDER";

	@MenuAction(value = "File > Convert > mzML...", order = 0)
	public void onConvert() {
		GuiManager gui = GuiManager.getInstance();
		MsppManager manager = MsppManager.getInstance();

		FileChooser inChooser = new FileChooser();
		inChooser.setTitle("Select vendor file to convert");
		inChooser.getExtensionFilters().add(
			new FileChooser.ExtensionFilter("Vendor Files", "*.raw", "*.wiff")
		);
		inChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Thermo RAW Files", "*.raw"));
		inChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Sciex WIFF Files", "*.wiff"));
		String inFolder = manager.getParameter(IN_FOLDER_KEY);
		if(inFolder != null) {
			File f = new File(inFolder);
			if(f.exists() && f.isDirectory()) {
				inChooser.setInitialDirectory(f);
			}
		}
		final File input = inChooser.showOpenDialog(gui.getMainStage());
		if(input == null) {
			return;
		}
		if(!VendorConverter.isVendorFile(input)) {
			showError("Unsupported file", input.getName() + " is not a vendor file (.raw / .wiff).");
			return;
		}
		if(input.getParentFile() != null) {
			manager.saveParameter(IN_FOLDER_KEY, input.getParentFile().getAbsolutePath());
		}

		if(!DockerManager.getInstance().isDockerAvailable()) {
			showError("Docker is not running",
				"Conversion requires Docker Desktop. Please start Docker Desktop and try again.");
			return;
		}

		FileChooser outChooser = new FileChooser();
		outChooser.setTitle("Save mzML as");
		outChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("mzML Files", "*.mzML"));
		String base = stripExt(input.getName());
		outChooser.setInitialFileName(base + ".mzML");
		String outFolder = manager.getParameter(OUT_FOLDER_KEY);
		if(outFolder != null) {
			File f = new File(outFolder);
			if(f.exists() && f.isDirectory()) {
				outChooser.setInitialDirectory(f);
			}
		}
		else if(input.getParentFile() != null) {
			outChooser.setInitialDirectory(input.getParentFile());
		}
		final File destination = outChooser.showSaveDialog(gui.getMainStage());
		if(destination == null) {
			return;
		}
		if(destination.getParentFile() != null) {
			manager.saveParameter(OUT_FOLDER_KEY, destination.getParentFile().getAbsolutePath());
		}

		final ProgressDialog dialog = new ProgressDialog(
			gui.getMainStage(),
			"Converting " + input.getName(),
			"Preparing..."
		);
		dialog.show();

		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				VendorConverter converter = new VendorConverter();
				converter.setStatusListener(s -> updateMessage(s));
				File produced = converter.convertToMzml(input);
				updateMessage("Saving to " + destination.getName() + "...");
				Files.copy(produced.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
				return null;
			}
		};
		task.messageProperty().addListener((obs, o, n) -> dialog.setStatus(n));
		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent e) {
				dialog.close();
				Alert info = new Alert(AlertType.INFORMATION);
				info.setTitle("Conversion complete");
				info.setHeaderText(null);
				info.setContentText("Saved: " + destination.getAbsolutePath());
				info.showAndWait();
			}
		});
		task.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent e) {
				dialog.close();
				Throwable ex = task.getException();
				if(ex != null) {
					ex.printStackTrace();
				}
				showError(
					"Failed to convert " + input.getName(),
					ex == null ? "Unknown error" : ex.getMessage()
				);
			}
		});

		Thread t = new Thread(task, "vendor-converter");
		t.setDaemon(true);
		t.start();
	}

	private static String stripExt(String name) {
		int dot = name.lastIndexOf('.');
		return dot < 0 ? name : name.substring(0, dot);
	}

	private static void showError(String header, String content) {
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
