package ninja.mspp.io.mzml.ui;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * Modal indeterminate progress dialog with a status label.
 * All public mutators are safe to call from any thread.
 */
public class ProgressDialog {
	private final Stage stage;
	private final Label statusLabel;
	private final ProgressBar bar;

	public ProgressDialog(Stage owner, String title, String initialStatus) {
		this.stage = new Stage();
		if(owner != null) {
			this.stage.initOwner(owner);
		}
		this.stage.initModality(Modality.WINDOW_MODAL);
		this.stage.setTitle(title);
		this.stage.setResizable(false);

		this.statusLabel = new Label(initialStatus == null ? "" : initialStatus);
		this.bar = new ProgressBar();
		this.bar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
		this.bar.setPrefWidth(360);

		VBox box = new VBox(10, this.statusLabel, this.bar);
		box.setPadding(new Insets(16));
		this.stage.setScene(new Scene(box));

		this.stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent e) {
				e.consume();
			}
		});
	}

	public void show() {
		if(Platform.isFxApplicationThread()) {
			this.stage.show();
		}
		else {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					ProgressDialog.this.stage.show();
				}
			});
		}
	}

	public void close() {
		if(Platform.isFxApplicationThread()) {
			this.stage.close();
		}
		else {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					ProgressDialog.this.stage.close();
				}
			});
		}
	}

	/**
	 * Sets the bar to a fractional progress value in [0, 1], or to indeterminate
	 * if the value is negative or NaN.
	 */
	public void setProgress(final double fraction) {
		final double value;
		if(Double.isNaN(fraction) || fraction < 0.0) {
			value = ProgressBar.INDETERMINATE_PROGRESS;
		}
		else if(fraction > 1.0) {
			value = 1.0;
		}
		else {
			value = fraction;
		}
		if(Platform.isFxApplicationThread()) {
			this.bar.setProgress(value);
		}
		else {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					ProgressDialog.this.bar.setProgress(value);
				}
			});
		}
	}

	public void setStatus(final String message) {
		if(Platform.isFxApplicationThread()) {
			this.statusLabel.setText(message);
		}
		else {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					ProgressDialog.this.statusLabel.setText(message);
				}
			});
		}
	}
}
