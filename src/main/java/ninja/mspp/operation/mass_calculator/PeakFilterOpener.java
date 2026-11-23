package ninja.mspp.operation.mass_calculator;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import ninja.mspp.MsppManager;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.operation.peak_filter.PeakFilterDialog;
import ninja.mspp.view.GuiManager;

/**
 * Opens the peak filter dialog with a name and an m/z value from the mass calculator.
 * The dialog is created once and reused.
 */
public class PeakFilterOpener {

	private Stage stage;
	private PeakFilterDialog peakFilterDialog;

	public PeakFilterOpener() {
		this.stage = null;
		this.peakFilterDialog = null;
	}

	/**
	 * Shows the peak filter dialog with the given name and m/z value.
	 * Shows an error instead if no sample is selected.
	 * @param name name of the peak
	 * @param mz m/z value of the peak
	 * @throws IOException if the dialog cannot be loaded
	 */
	public void open(String name, String mz) throws IOException {
		MsppManager manager = MsppManager.getInstance();

		Sample sample = manager.getActiveSample();

		// Check if a sample is selected
		if(sample == null) {
			ResourceBundle messages = manager.getMessages();
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText(messages.getString("sample.not_selected.title"));
			alert.setContentText(messages.getString("sample.not_selected.content"));
			alert.showAndWait();
			return;
		}

		if (this.stage == null) {
			GuiManager guiManager = GuiManager.getInstance();
			setUpPeakFilterDialog(manager, guiManager);
		}

		this.peakFilterDialog.setInformation(name, mz, null, false);

		this.stage.show();
	}

	private void setUpPeakFilterDialog(MsppManager manager, GuiManager guiManager) throws IOException {
		FXMLLoader loader = new FXMLLoader(PeakFilterDialog.class.getResource("PeakFilterDialog.fxml"));
		Parent root = loader.load();
		this.peakFilterDialog = loader.getController();
		this.peakFilterDialog.setSample(manager.getActiveSample());

		Stage stage = new Stage();
		stage.initOwner(guiManager.getMainStage());
		stage.setTitle("Peak Filter");
		Scene scene = new Scene(root);
		stage.setScene(scene);
		this.stage = stage;
	}
}
