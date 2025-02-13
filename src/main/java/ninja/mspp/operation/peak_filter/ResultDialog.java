package ninja.mspp.operation.peak_filter;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.method.OnSelectSpectrum;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.operation.peak_filter.model.HitPeak;
import ninja.mspp.operation.peak_filter.model.entity.FilterPeak;


public class ResultDialog implements Initializable {		
	@FXML
	private TableView<HitPeak> table;
	
	@FXML
	private CheckBox drawCheck;
	
	@FXML
	public void onClose(ActionEvent event) {
		PeakFilterManager manager = PeakFilterManager.getInstance();
		
		Button button = (Button)event.getSource();
		Scene scene = button.getScene();
		Stage stage = (Stage)scene.getWindow();
		stage.close();
		
		manager.unsetResult();
		manager.setDrawingFlag(false);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		MsppManager manager = MsppManager.getInstance();
		
		this.table.getItems().clear();
		this.table.getColumns().clear();
		
		TableColumn<HitPeak, String> spectrumColumn = new TableColumn<>("Spectrum");
		spectrumColumn.setCellValueFactory(
			data -> {
				Spectrum spectrum = data.getValue().getSpectrum();
				return new ReadOnlyStringWrapper(spectrum.getName());
			}
		);
		this.table.getColumns().add(spectrumColumn);
			
		TableColumn<HitPeak, String> rtColumn = new TableColumn<>("RT");
		rtColumn.setCellValueFactory(
			data -> {
				Spectrum spectrum = data.getValue().getSpectrum();
				String text = String.format("%.4f", spectrum.getRt());
				return new ReadOnlyStringWrapper(text);
			}
		);
		rtColumn.setCellFactory(
			column -> {
				TableCell<HitPeak, String> cell = new TableCell<>() {
					@Override
					protected void updateItem(String text, boolean empty) {
						super.updateItem(text, empty);
						if(text == null || empty) {
							setText("");
						}
						else {
							setText(text);
							setAlignment(Pos.CENTER_RIGHT);
						}
					}
				};
				return cell;
			}
		);
		this.table.getColumns().add(rtColumn);
			
		TableColumn<HitPeak, String> stageColumn = new TableColumn<>("MS Stage");
		stageColumn.setCellValueFactory(					
			data -> {
				Spectrum spectrum = data.getValue().getSpectrum();
				String text = String.format("%d", spectrum.getMsLevel());
				return new ReadOnlyStringWrapper(text);
			}
		);
		stageColumn.setCellFactory(
			column -> {
				TableCell<HitPeak, String> cell = new TableCell<>() {
					@Override
					protected void updateItem(String text, boolean empty) {
						super.updateItem(text, empty);
						if(text == null || empty) {
							setText("");
						}
						else {
							setText(text);
							setAlignment(Pos.CENTER_RIGHT);
						}
					}
				};
				return cell;
			}
		);					
		this.table.getColumns().add(stageColumn);
			 
		TableColumn<HitPeak, String> precursorColumn = new TableColumn<>("Precursor");
		precursorColumn.setCellValueFactory(
			data -> {
				Spectrum spectrum = data.getValue().getSpectrum();
				String text = String.format("%.2f", spectrum.getPrecursorMass());
				return new ReadOnlyStringWrapper(text);
			}
		);
		precursorColumn.setCellFactory(
			column -> {
				TableCell<HitPeak, String> cell = new TableCell<>() {
					@Override
					protected void updateItem(String text, boolean empty) {
						super.updateItem(text, empty);
						if(text == null || empty) {
							setText("");
						}
						else {
							setText(text);
							setAlignment(Pos.CENTER_RIGHT);
						}
					}
				};
				return cell;
			}
		);
		this.table.getColumns().add(precursorColumn);
		
		this.table.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldValue, newValue) -> {
				if (newValue != null) {
					manager.invoke(OnSelectSpectrum.class, newValue.getSpectrum());
				}
			}
		);
		
		PeakFilterManager peakFilterManager = PeakFilterManager.getInstance();
		this.drawCheck.selectedProperty().addListener(
			(observable, oldValue, newValue) -> {
				peakFilterManager.setDrawingFlag(newValue);
			}
		);
	}
	
	public void setResult(List<FilterPeak> peaks, List<HitPeak> result) {
		for(FilterPeak peak : peaks) {
			String peakName = peak.getName();
			String title = String.format("%s [%.2f]]", peakName, peak.getMz());
			TableColumn<HitPeak, String> resultColumn = new TableColumn<>(title);
			resultColumn.setCellValueFactory(
				data -> {
					String value = "";
					HitPeak hit = data.getValue();
					Double mz = hit.getHitMap().get(peak);
					if(mz != null) {
						value = String.format("%.2f", mz);
					}
					return new ReadOnlyStringWrapper(value);
				}
			);
			resultColumn.setCellFactory(
				column -> {
					TableCell<HitPeak, String> cell = new TableCell<>() {
						@Override
						protected void updateItem(String text, boolean empty) {
							super.updateItem(text, empty);
							if(text == null || empty) {
								setText("");
							}
							else {
								setText(text);
								setAlignment(Pos.CENTER_RIGHT);
							}
						}
					};
					return cell;
				}
			);
			this.table.getColumns().add(resultColumn);
		}
		
		for(HitPeak hit : result) {
			this.table.getItems().add(hit);
		}	
		
		PeakFilterManager manager = PeakFilterManager.getInstance();
		manager.setDrawingFlag(this.drawCheck.isSelected());
	}
}
