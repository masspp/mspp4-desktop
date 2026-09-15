package ninja.mspp.operation.average;

import java.util.Locale;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ninja.mspp.core.model.ms.Spectrum;

public class AverageSpectrumDialog {
	public enum Target {
		MS("MS"),
		MS_MS("MS/MS"),
		ALL("All");

		private final String label;

		private Target(String label) {
			this.label = label;
		}

		public String getLabel() {
			return this.label;
		}

		public boolean matches(Spectrum spectrum) {
			switch(this) {
			case MS:
				return spectrum.getMsLevel() == 1;
			case MS_MS:
				return spectrum.getMsLevel() >= 2;
			default:
				return true;
			}
		}
	}

	private boolean confirmed;
	private double startRt;
	private double endRt;

	@FXML
	private TextField startRtText;

	@FXML
	private TextField endRtText;

	@FXML
	private RadioButton msRadio;

	@FXML
	private RadioButton msmsRadio;

	@FXML
	private RadioButton allRadio;

	private void closeDialog() {
		Stage stage = (Stage)this.startRtText.getScene().getWindow();
		stage.close();
	}

	public boolean isConfirmed() {
		return this.confirmed;
	}

	public void setRtRange(double startRt, double endRt) {
		this.startRtText.setText(String.format(Locale.ROOT, "%.4f", startRt));
		this.endRtText.setText(String.format(Locale.ROOT, "%.4f", endRt));
	}

	public double getStartRt() {
		return this.startRt;
	}

	public double getEndRt() {
		return this.endRt;
	}

	public Target getTarget() {
		if(this.msmsRadio.isSelected()) {
			return Target.MS_MS;
		}
		if(this.allRadio.isSelected()) {
			return Target.ALL;
		}
		return Target.MS;
	}

	@FXML
	private void onOk(ActionEvent event) {
		double start;
		double end;
		try {
			start = Double.parseDouble(this.startRtText.getText().trim());
			end = Double.parseDouble(this.endRtText.getText().trim());
		}
		catch(NumberFormatException e) {
			Alert alert = new Alert(AlertType.WARNING);
			alert.setTitle("Spectrum");
			alert.setHeaderText("Invalid RT range");
			alert.setContentText("Please enter numbers for the RT range.");
			alert.showAndWait();
			return;
		}
		this.startRt = Math.min(start, end);
		this.endRt = Math.max(start, end);
		this.confirmed = true;
		this.closeDialog();
	}

	@FXML
	private void onCancel(ActionEvent event) {
		this.confirmed = false;
		this.closeDialog();
	}
}
