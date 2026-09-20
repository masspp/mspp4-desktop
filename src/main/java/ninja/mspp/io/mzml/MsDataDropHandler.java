package ninja.mspp.io.mzml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Opens MS data files dropped on the main window. While files are dragged over the
 * window, an overlay tells that dropping them opens them.
 */
public class MsDataDropHandler {
	private static final int MAX_LISTED_FILES = 3;

	/**
	 * @param scene     scene of the main window
	 * @param container the root of the scene, on which the overlay is shown
	 */
	public static void install(Scene scene, StackPane container) {
		Node[] overlay = new Node[1];

		scene.addEventHandler(DragEvent.DRAG_OVER, event -> {
			if(event.getGestureSource() == null) {
				Dragboard board = event.getDragboard();
				List<File> files = supportedFiles(board);
				if(!files.isEmpty()) {
					event.acceptTransferModes(TransferMode.COPY);
					if(overlay[0] == null) {
						overlay[0] = createOverlay(files, skippedFiles(board).size());
						container.getChildren().add(overlay[0]);
					}
				}
			}
			event.consume();
		});

		// The drag left the window.
		scene.setOnDragExited(event -> hideOverlay(container, overlay));

		scene.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
			hideOverlay(container, overlay);

			Dragboard board = event.getDragboard();
			List<File> files = supportedFiles(board);
			List<File> skipped = skippedFiles(board);
			event.setDropCompleted(!files.isEmpty());
			event.consume();

			if(files.isEmpty()) {
				return;
			}
			// Open the files after the drop has finished, so that the dialogs do not block the drag source.
			Platform.runLater(() -> {
				if(!skipped.isEmpty()) {
					showSkipped(skipped);
				}
				MsDataOpener.open(files);
			});
		});
	}

	private static void hideOverlay(StackPane container, Node[] overlay) {
		if(overlay[0] != null) {
			container.getChildren().remove(overlay[0]);
			overlay[0] = null;
		}
	}

	private static Node createOverlay(List<File> files, int skipped) {
		Label title = new Label("Drop to open in Mass++4");
		title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e4e8c;");

		Label detail = new Label(describe(files, skipped));
		detail.setStyle("-fx-font-size: 14px; -fx-text-fill: #1e4e8c;");
		detail.setTextAlignment(TextAlignment.CENTER);
		detail.setWrapText(true);

		VBox box = new VBox(8.0, title, detail);
		box.setAlignment(Pos.CENTER);
		box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		box.setStyle(
			"-fx-background-color: rgba(255, 255, 255, 0.92);"
			+ " -fx-background-radius: 10;"
			+ " -fx-padding: 20 32 20 32;"
		);

		StackPane overlay = new StackPane(box);
		overlay.setStyle(
			"-fx-background-color: rgba(30, 111, 217, 0.15);"
			+ " -fx-background-insets: 8;"
			+ " -fx-background-radius: 12;"
			+ " -fx-border-color: #1e6fd9;"
			+ " -fx-border-width: 3;"
			+ " -fx-border-style: dashed;"
			+ " -fx-border-insets: 8;"
			+ " -fx-border-radius: 12;"
		);
		// Let the drag events reach the window below the overlay.
		overlay.setMouseTransparent(true);
		return overlay;
	}

	private static String describe(List<File> files, int skipped) {
		StringBuilder text = new StringBuilder();
		if(files.size() == 1) {
			text.append(files.get(0).getName());
		}
		else {
			text.append(files.size()).append(" files");
			for(int i = 0; i < Math.min(files.size(), MAX_LISTED_FILES); i++) {
				text.append("\n").append(files.get(i).getName());
			}
			if(files.size() > MAX_LISTED_FILES) {
				text.append("\nand ").append(files.size() - MAX_LISTED_FILES).append(" more");
			}
		}
		if(skipped > 0) {
			text.append("\n\n").append(skipped)
				.append(skipped == 1 ? " file is" : " files are")
				.append(" not MS data and will be skipped.");
		}
		return text.toString();
	}

	private static List<File> supportedFiles(Dragboard board) {
		List<File> files = new ArrayList<File>();
		if(board.hasFiles()) {
			for(File file : board.getFiles()) {
				if(MsDataOpener.isSupported(file)) {
					files.add(file);
				}
			}
		}
		return files;
	}

	/**
	 * Returns the dropped files that are not opened. Sciex .wiff.scan files are not included,
	 * because they are read together with their .wiff file.
	 */
	private static List<File> skippedFiles(Dragboard board) {
		List<File> files = new ArrayList<File>();
		if(board.hasFiles()) {
			for(File file : board.getFiles()) {
				String name = file.getName().toLowerCase(Locale.ROOT);
				if(!MsDataOpener.isSupported(file) && !name.endsWith(".wiff.scan")) {
					files.add(file);
				}
			}
		}
		return files;
	}

	private static void showSkipped(List<File> files) {
		StringBuilder names = new StringBuilder();
		for(File file : files) {
			names.append(System.lineSeparator()).append(file.getName());
		}
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Open MS Data");
		alert.setHeaderText("Some files were not opened");
		alert.setContentText(
			"Only mzML, Thermo .raw, Sciex .wiff and Shimadzu .lcd files can be opened."
			+ System.lineSeparator() + names
		);
		alert.showAndWait();
	}
}
