package ninja.mspp.operation.mass_calculator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.glycoinfo.ms.GlycanMassUtility.dict.molecule.NeutralType;
import org.glycoinfo.ms.GlycanMassUtility.om.IMassElement;
import org.glycoinfo.ms.GlycanMassUtility.om.IonCloud;
import org.glycoinfo.ms.GlycanMassUtility.om.Molecule;
import org.glycoinfo.ms.GlycanMassUtility.utils.CompoundParserType;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import ninja.mspp.operation.mass_calculator.model.mass.MassCalculator;

public class MassCalculatorDialog {

	@FXML
	private TextField nameField;

	@FXML
	private ChoiceBox<String> typeChoiceBox;

	@FXML
	private Label egLabel;

	@FXML
	private TextField massField;

	@FXML
	private Label warningLabel;

	@FXML
	private RadioButton mhRadioButton;

	@FXML
	private RadioButton m2hRadioButton;

	@FXML
	private RadioButton m3hRadioButton;

	@FXML
	private RadioButton mNaRadioButton;

	@FXML
	private RadioButton m2NaRadioButton;

	@FXML
	private RadioButton m3NaRadioButton;

	@FXML
	private RadioButton mXRadioButton;

	@FXML
	private TextField ionField;

	@FXML
	private CheckBox watLossCheckBox;

	@FXML
	private TextField mzField;

	@FXML
	private Button searchButton;

	@FXML
	private Button resetButton;

	private ToggleGroup adductGroup;

	@FXML
	private ProFormaCalculatorPane proFormaPaneController;

	private PeakFilterOpener peakFilterOpener;

	@FXML
	private void initialize() {
		// Add items to ChoiceBox
		List<String> typeNames = new ArrayList<>();
		for (CompoundParserType type : CompoundParserType.values())
			typeNames.add(type.getName());
		typeChoiceBox.getItems().addAll(typeNames);
		typeChoiceBox.getSelectionModel().selectFirst();
		updateEgLabel();

		// Create and set up the ToggleGroup for the RadioButtons
		adductGroup = new ToggleGroup();
		mhRadioButton.setToggleGroup(adductGroup);
		m2hRadioButton.setToggleGroup(adductGroup);
		m3hRadioButton.setToggleGroup(adductGroup);
		mNaRadioButton.setToggleGroup(adductGroup);
		m2NaRadioButton.setToggleGroup(adductGroup);
		m3NaRadioButton.setToggleGroup(adductGroup);
		mXRadioButton.setToggleGroup(adductGroup);

		// Initial settings and other initialization tasks
		warningLabel.setText("");  // Clear initial warning message

		// Add listeners to handle changes in nameField and typeChoiceBox
		nameField.textProperty().addListener((observable, oldValue, newValue) -> handleInputChange());
		typeChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> handleInputChange());
		adductGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> handleInputChange());
		ionField.textProperty().addListener((observable, oldValue, newValue) -> handleInputChange());
		watLossCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> handleInputChange());

		// Search peaks only when m/z is calculated
		searchButton.disableProperty().bind(mzField.textProperty().isEmpty());

		// Share the peak filter dialog with the ProForma tab
		peakFilterOpener = new PeakFilterOpener();
		proFormaPaneController.setPeakFilterOpener(peakFilterOpener);
	}

	private void updateEgLabel() {
		egLabel.setText("");
		if (getSelectedType() != null)
			egLabel.setText("e.g. " + getSelectedType().getFormatExample());
	}

	private void reset() {
		nameField.clear();
		typeChoiceBox.getSelectionModel().selectFirst();
		updateEgLabel();

		warningLabel.setText("");
		massField.setText("");
		ionField.setText("");
		mzField.setText("");
	}

	// Method to handle changes in input fields
	private void handleInputChange() {
		// Clear warning message
		warningLabel.setText("");

		updateEgLabel();

		CompoundParserType type = getSelectedType();
		String name = nameField.getText();
		// Treat a blank name (e.g. spaces only) as empty
		if (name.trim().isEmpty()) {
			massField.clear();
			mzField.clear();
			return;
		}

		ionField.setEditable(false);
		ionField.setDisable(true);
		if (mXRadioButton.isSelected()) {
			ionField.setEditable(true);
			ionField.setDisable(false);
			if (ionField.getText().isEmpty()) {
				mzField.clear();
				return;
			}
		}

		// Calculate mass and mz
		MassCalculator calc = new MassCalculator();
		try {
			IMassElement compound = type.parse(name);
			// Parser returns null for blank input (e.g. spaces only)
			if (compound == null) {
				massField.clear();
				mzField.clear();
				return;
			}
			calc.addCompound(compound);

			IonCloud ionCloud = IonCloud.parse( getSelectedIon() );

			Molecule waterLoss = null;
			if ( watLossCheckBox.isSelected() )
				waterLoss = NeutralType.Water.getMolecule();

			// Display mass and mz
			massField.setText(String.valueOf(calc.computeMass()));
			Double mz = calc.computeMz(ionCloud, waterLoss);
			if (mz == null) {
				warningLabel.setText("The total charge of the ion is 0, so m/z cannot be calculated.");
				mzField.clear();
				return;
			}
			mzField.setText(String.valueOf(mz));
		} catch (IllegalArgumentException e) {
			// handle invalid input
			warningLabel.setText(e.getMessage());
			massField.clear();
			mzField.clear();
			return;
		}
	}

	@FXML
	private void onAddToPeakFilter() throws IOException {
		// Call the dialog for adding the current mass to the peak filter
		String name = nameField.getText();
		if (watLossCheckBox.isSelected())
			name += " - H2O";
		name += " + "+getSelectedIon();
		peakFilterOpener.open(name, mzField.getText());
	}

	@FXML
	private void onReset() {
		reset();
	}

	private CompoundParserType getSelectedType() {
		return CompoundParserType.fromName(typeChoiceBox.getValue());
	}

	private String getSelectedIon() {
		if (mhRadioButton.isSelected())
			return "H";
		else if (m2hRadioButton.isSelected())
			return "2H";
		else if (m3hRadioButton.isSelected())
			return "3H";
		else if (mNaRadioButton.isSelected())
			return "Na";
		else if (m2NaRadioButton.isSelected())
			return "2Na";
		else if (m3NaRadioButton.isSelected())
			return "3Na";
		else if (mXRadioButton.isSelected())
			return ionField.getText();
		return null;
	}

}
