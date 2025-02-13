package ninja.mspp.operation.xic;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class XicDialog implements Initializable {
	private boolean confirmed;
	
	@FXML
	private TextField mzText;
	
	@FXML
	private TextField tolText;
	
	private void closeDialog() {
		Stage  stage = (Stage)this.mzText.getScene().getWindow();
		stage.close();
	}
	
	public boolean isConfirmed() {
		return this.confirmed;
	}
	
	public double getMz() {
		return Double.parseDouble(this.mzText.getText());
	}
	
	public void setMz(double mz) {
		this.mzText.setText(String.valueOf(mz));
	}
	
	public double getTolerance() {
		return Double.parseDouble(this.tolText.getText());
	}
	
	public void setTolerance(double tolerance) {
		this.tolText.setText(String.valueOf(tolerance));
	}
	
	@FXML
	private void onOk(ActionEvent event) {
		this.confirmed = true;
		this.closeDialog();
	}
	
	
	@FXML
	private void onCancel(ActionEvent event) {
		this.confirmed = false;
		this.closeDialog();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.mzText.textProperty().addListener(
			(ov, oldVal, newVal) -> {
				if(newVal.matches("¥¥d*(¥¥.¥¥d*)?")) {
					this.mzText.setText(oldVal);
				}
			}
		);
		
		this.tolText.textProperty().addListener(
			(ov, oldVal, newVal) -> {
				if(newVal.matches("¥¥d*(¥¥.¥¥d*)?")) {
					this.tolText.setText(oldVal);					
				}
			}
		);
	}
}
