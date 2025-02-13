package ninja.mspp.operation.peak_filter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import ninja.mspp.MsppManager;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.operation.peak_filter.model.HitPeak;
import ninja.mspp.operation.peak_filter.model.entity.FilterPeak;
import ninja.mspp.operation.peak_filter.model.entity.FilterPeakSet;

public class PeakFilterDialog implements Initializable {
	private Sample sample;
	
	@FXML
	private TableView<FilterPeak> table;
	
	@FXML
	private TableColumn<FilterPeak, String> nameColumn;
	
	@FXML
	private TableColumn<FilterPeak, Double> mzColumn;
	
	@FXML
	private TableColumn<FilterPeak, String> colorColumn;
	
	@FXML
	private TableColumn<FilterPeak, Boolean> neutralLossColumn;
	
	@FXML
	private ChoiceBox<FilterPeakSet> filterChoice;
	
	@FXML
	private TextField nameText;
	
	@FXML
	private TextField mzText;
	
	@FXML
	private ColorPicker colorPicker;
	
	@FXML
	private CheckBox neutralLossCheck;
	
	@FXML
	private TextField toleranceText;
	
	@FXML
	private TextField thresholdText;
	
	@FXML
	private ChoiceBox<String> unitChoice;
	
	@FXML
	private BorderPane setPane;
	
	@FXML
	private void onAdd(ActionEvent event) {
		try {
			String name = this.nameText.getText();
			Double mz = Double.parseDouble(this.mzText.getText());
			Color color = this.colorPicker.getValue();
			boolean neutralLoss = this.neutralLossCheck.isSelected();
		
			FilterPeak peak = new FilterPeak();
			peak.setName(name);
			peak.setMz(mz);
			peak.setColor(color.toString());
			peak.setNeutralLoss(neutralLoss);
		
			this.nameText.setText("");
			this.mzText.setText("");
			this.colorPicker.setValue(Color.BLACK);
			this.neutralLossCheck.setSelected(false);

			this.table.getItems().add(peak);
		}
		catch(Exception e) {
		}
	}
	
	public void setSample(Sample sample) {
		this.sample = sample;
	}
	
	public Sample getSample() {
		return this.sample;
	}
	
	@FXML
	private void onDeletePeak(ActionEvent event) {
		FilterPeak peak = this.table.getSelectionModel().getSelectedItem();
		this.table.getItems().remove(peak);
	}
	
	@FXML
	private void onSearch(ActionEvent event) throws IOException {
		PeakFilterManager manager = PeakFilterManager.getInstance();
		
		List<FilterPeak> peaks = new ArrayList<FilterPeak>();
		for (FilterPeak peak : this.table.getItems()) {
			peaks.add(peak);
		}
		System.out.println(peaks);
		
		double tolerance = Double.parseDouble(this.toleranceText.getText());
		double threshold = Double.parseDouble(this.thresholdText.getText());
		String unit = this.unitChoice.getSelectionModel().getSelectedItem();		
		List<HitPeak> result = manager.searchPeaks(this.sample, peaks, tolerance, threshold, unit);
		System.out.println(result);
		
		manager.openResultDialog(peaks, result);
	}
	
	@FXML
	private void onClose(ActionEvent event) {
		Button button = (Button)event.getSource();
		Scene scene = button.getScene();
		Stage stage = (Stage)scene.getWindow();
		stage.close();
	}
	
	@FXML
	private void onSave(ActionEvent event) {
		PeakFilterManager manager = PeakFilterManager.getInstance();
		MsppManager msppManager = MsppManager.getInstance();
		ResourceBundle messages = msppManager.getMessages();
		
		List<FilterPeak> peaks = new ArrayList<FilterPeak>();
		for (FilterPeak peak : this.table.getItems()) {
			peaks.add(peak);
		}
		
		if(peaks.isEmpty()) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText(messages.getString("peak_filter.no_peaks.title"));
			alert.setContentText(messages.getString("peak_filter.no_peaks.content"));
			alert.showAndWait();			
		}
		else {
			FilterPeakSet set = this.filterChoice.getSelectionModel().getSelectedItem();
			if(set == null) {
				set = manager.saveNew(peaks);
				this.refreshSet();
				this.selectSet(set.getId());
			}
			else {
				manager.savePeaks(set.getId(), peaks);
				this.refreshSet();
				this.selectSet(set.getId());
			}
		}
	}
	
	@FXML
	private void onSaveAs(ActionEvent event) {
		PeakFilterManager manager = PeakFilterManager.getInstance();
		MsppManager msppManager = MsppManager.getInstance();
		ResourceBundle messages = msppManager.getMessages();
		
		List<FilterPeak> peaks = new ArrayList<FilterPeak>();
		for (FilterPeak peak : this.table.getItems()) {
			peaks.add(peak);
		}
		
		if(peaks.isEmpty()) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText(messages.getString("peak_filter.no_peaks.title"));
			alert.setContentText(messages.getString("peak_filter.no_peaks.content"));
			alert.showAndWait();			
		}
		else {
			FilterPeakSet set = manager.saveNew(peaks);
			this.refreshSet();
			this.selectSet(set.getId());
		}
	}
	
	@FXML
	private void onDeleteSet(ActionEvent event) {
		PeakFilterManager manager = PeakFilterManager.getInstance();
		MsppManager msppManager = MsppManager.getInstance();
		ResourceBundle messages = msppManager.getMessages();
		
		FilterPeakSet set = this.filterChoice.getSelectionModel().getSelectedItem();
		if(set == null) {			
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText(messages.getString("peak_filter.no_set_selected.title"));
			alert.setContentText(messages.getString("peak_filter.no_set_selected.content"));
			alert.showAndWait();
		}
		else {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Confirmation");
			alert.setHeaderText(messages.getString("peak_filter.delete_set.title"));
			alert.setContentText(messages.getString("peak_filter.delete_set.content"));
			alert.getButtonTypes().clear();
			alert.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
			ButtonType type = alert.showAndWait().get();
			if(type == ButtonType.OK) {
				manager.delete(set);
				this.refreshSet();
			}
		}
	}
	
	protected void refreshSet() {
		this.filterChoice.getItems().clear();
		this.table.getItems().clear();
		this.createSetChoice();
	}
	
	protected void selectSet(long id) {
		FilterPeakSet selectedSet = null;
		for (FilterPeakSet set : this.filterChoice.getItems()) {
			if(set != null) {
				if (set.getId() == id) {
					selectedSet = set;
				}
			}
		}
		
		if (selectedSet != null) {
			this.filterChoice.getSelectionModel().select(selectedSet);
			this.onSelectSet(id);
		}
	}
	
	protected void onSelectSet(long id) {
		PeakFilterManager manager = PeakFilterManager.getInstance();
		
		this.table.getItems().clear();
		List<FilterPeak> peaks = manager.getPeaks(id);
		for (FilterPeak peak : peaks) {
			this.table.getItems().add(peak);
		}		
	}
	
	private void setUnitChoice() {
		this.unitChoice.getItems().add("%");
		this.unitChoice.getItems().add("count");
		this.unitChoice.getSelectionModel().select(0);		
	}
	
	private void setTable() {
		this.nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		
		this.mzColumn.setCellValueFactory(new PropertyValueFactory<>("mz"));
		this.mzColumn.setCellFactory(
			column -> {
				TableCell<FilterPeak, Double> cell = new TableCell<>() {
					@Override
					protected void updateItem(Double mz, boolean empty) {
						super.updateItem(mz, empty);
                        if(!empty && mz != null) {
							String text = String.format("%.4f",  mz);
							setText(text);
							setAlignment(Pos.CENTER_RIGHT);
						}
                        else {
                        	setText("");
                        }
					}
				};
				return cell;
			}
		);
		this.colorColumn.setCellValueFactory(new PropertyValueFactory<>("color"));
		this.colorColumn.setCellFactory(
			column -> {
				TableCell<FilterPeak, String> cell = new TableCell<>() {
					@Override
					protected void updateItem(String color, boolean empty) {
						super.updateItem(color, empty);
						if(color == null || empty) {
							setStyle("");
						}
						else {
							String text = color.replace("0x", "#");
							setStyle("-fx-background-color: " + text + ";");
						}
					}
				};
				return cell;
			}
		);
		
		this.neutralLossColumn.setCellValueFactory(new PropertyValueFactory<>("neutralLoss"));
		this.neutralLossColumn.setCellFactory(
			column -> {
				TableCell<FilterPeak, Boolean> cell = new TableCell<>() {
					@Override
					protected void updateItem(Boolean neutralLoss, boolean empty) {
						super.updateItem(neutralLoss, empty);
						if (neutralLoss == null || empty) {
							setText("");
						} 
						else {
							if (neutralLoss) {
								setText("O");
							} 
							else {
								setText("-");
							}
						}
					}
				};
				return cell;
			}
		);
	}
	
	private void createSetChoice() {
		PeakFilterManager manager = PeakFilterManager.getInstance();		
		List<FilterPeakSet> sets = manager.getFilterPeakSets();
		
		this.filterChoice.getItems().clear();
		this.filterChoice.getItems().add(null);
		for (FilterPeakSet set : sets) {
			this.filterChoice.getItems().add(set);
		}		
	}
	
	private void setSetChoice() {
		this.createSetChoice();
		
		this.filterChoice.setConverter(
			new javafx.util.StringConverter<FilterPeakSet>() {
				@Override
				public String toString(FilterPeakSet set) {
					if (set == null) {
						return "(New)";
					}
					else {
						return set.getName();
					}
				}

				@Override
				public FilterPeakSet fromString(String string) {
					return null;
				}
			}
		);

		this.setPane.widthProperty().addListener(
			(observable, oldValue, newValue) -> {
				this.filterChoice.setPrefWidth(newValue.doubleValue() - 250);
			}
		);
		
		this.filterChoice.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldValue, newValue) -> {
				if (newValue == null) {
					this.table.getItems().clear();
				}
				else {
					this.onSelectSet(newValue.getId());
				}
			}
		);
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.setUnitChoice();
		this.setTable();
		this.setSetChoice();
		
		this.colorPicker.setValue(Color.BLACK);
		this.toleranceText.setText("0.01");
		this.thresholdText.setText("1");
	}

	public void setInformation(String name, String mz, String color, boolean neutralLoss) {
		this.nameText.setText(name);
		this.mzText.setText(mz);
		this.colorPicker.setValue(color == null ? Color.BLACK : Color.web(color));
		this.neutralLossCheck.setSelected(neutralLoss);
	}
}
