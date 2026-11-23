package ninja.mspp.operation.mass_calculator;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ninja.mspp.proformabuilder.Model.ChargeState;
import ninja.mspp.proformabuilder.ProForma;

/**
 * Pane of the mass calculator that calculates the mass and m/z of a ProForma string.
 * The ion is given by the charge state of the string.
 */
public class ProFormaCalculatorPane {

	private static final String EXAMPLE = "e.g. PEPT[Phospho]IDE/2, PEPTIDE/[Na:z+1^2], PEPTIDE/[H-1:z-1^2]";

	@FXML
	private TextField proFormaField;

	@FXML
	private Label egLabel;

	@FXML
	private TextField massField;

	@FXML
	private TextField mzField;

	@FXML
	private Button resetButton;

	@FXML
	private Button searchButton;

	@FXML
	private Label warningLabel;

	private PeakFilterOpener peakFilterOpener;

	@FXML
	private void initialize() {
		egLabel.setText(EXAMPLE);
		warningLabel.setText("");

		proFormaField.textProperty().addListener((observable, oldValue, newValue) -> handleInputChange());

		// Search peaks only when m/z is calculated
		searchButton.disableProperty().bind(mzField.textProperty().isEmpty());

		peakFilterOpener = new PeakFilterOpener();
	}

	/**
	 * Sets the opener of the peak filter dialog, to share the dialog with the other tabs.
	 * @param opener PeakFilterOpener
	 */
	public void setPeakFilterOpener(PeakFilterOpener opener) {
		this.peakFilterOpener = opener;
	}

	private void handleInputChange() {
		warningLabel.setText("");

		String text = proFormaField.getText().trim();
		if (text.isEmpty()) {
			massField.clear();
			mzField.clear();
			return;
		}

		try {
			ProForma proForma = ProForma.parse(text);
			massField.setText(String.valueOf(proForma.getMonoisotopicMass().doubleValue()));

			Integer charge = proForma.getCharge();
			if (charge == null || charge == 0) {
				warningLabel.setText("Add a charge state (e.g. /2) to calculate m/z.");
				mzField.clear();
				return;
			}
			mzField.setText(String.valueOf(proForma.getMz().doubleValue()));

			// A negative charge without charge carriers adds electrons (ProForma 2.1)
			ChargeState chargeState = proForma.getChargeState();
			if (chargeState != null && !chargeState.hasCarriers() && chargeState.getCharge() < 0)
				warningLabel.setText(getElectronNote(-chargeState.getCharge()));
		} catch (IllegalArgumentException e) {
			// handle invalid input, including ProFormaException
			warningLabel.setText(e.getMessage());
			massField.clear();
			mzField.clear();
		}
	}

	private static String getElectronNote(int n) {
		String carrier = (n == 1)? "/[H-1:z-1]" : "/[H-1:z-1^"+n+"]";
		String ion = (n == 1)? "[M-H]-" : "[M-"+n+"H]"+n+"-";
		return "/-"+n+" adds electrons. Use "+carrier+" for "+ion+".";
	}

	@FXML
	private void onReset() {
		proFormaField.clear();
		warningLabel.setText("");
		massField.clear();
		mzField.clear();
	}

	@FXML
	private void onAddToPeakFilter() throws IOException {
		peakFilterOpener.open(proFormaField.getText().trim(), mzField.getText());
	}
}
