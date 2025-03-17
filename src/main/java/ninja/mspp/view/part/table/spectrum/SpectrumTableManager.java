package ninja.mspp.view.part.table.spectrum;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.method.OnSelectSpectrum;
import ninja.mspp.core.model.ms.Spectrum;

public class SpectrumTableManager {
	private static SpectrumTableManager instance;
	
	private TableView<Spectrum> table;
	
	private SpectrumTableManager() {
		this.table = null;
	}
	
	public TableView<Spectrum> createTableView() {
		MsppManager manager = MsppManager.getInstance();
		
		TableView<Spectrum> table = new TableView<Spectrum>();
		
		TableColumn<Spectrum, String> nameColumn = new TableColumn<Spectrum, String>("Name");
		nameColumn.setPrefWidth(150);
		nameColumn.setCellValueFactory(new PropertyValueFactory<Spectrum, String>("name"));
		table.getColumns().add(nameColumn);
		
		TableColumn<Spectrum, Double> rtColumn = new TableColumn<Spectrum, Double>("RT");
		rtColumn.setPrefWidth(75);
		rtColumn.setCellValueFactory(new PropertyValueFactory<Spectrum, Double>("rt"));
		rtColumn.setCellFactory(
			(column) -> {
				return new TableCell<Spectrum, Double>() {
					@Override
					protected void updateItem(Double item, boolean empty) {
						super.updateItem(item, empty);
						if (item != null && item > 0.0) {
							this.setText(String.format("%.4f", item));
							this.setAlignment(Pos.CENTER_RIGHT);
						} 
						else {
							this.setText("");
						}
					}
				};
			}
		);
		table.getColumns().add(rtColumn);
		
		TableColumn<Spectrum, Integer> stageColumn = new TableColumn<Spectrum, Integer>("MS Stage");
		stageColumn.setPrefWidth(75);
		stageColumn.setCellValueFactory(new PropertyValueFactory<Spectrum, Integer>("msLevel"));
		stageColumn.setCellFactory(
			(column) -> {
				return new TableCell<Spectrum, Integer>() {
					@Override
					protected void updateItem(Integer item, boolean empty) {
						super.updateItem(item, empty);
						if (item != null && item > 0) {
							this.setText(String.format("%d", item));
							this.setAlignment(Pos.CENTER_RIGHT);
						} 
						else {
							this.setText("");
						}
					}
				};
			}
		);
		table.getColumns().add(stageColumn);
		
		TableColumn<Spectrum, Double> precursorColumn = new TableColumn<Spectrum, Double>("Precursor");
		precursorColumn.setPrefWidth(75);
		precursorColumn.setCellValueFactory(new PropertyValueFactory<Spectrum, Double>("precursorMass"));
		precursorColumn.setCellFactory(
			(column) -> {
				return new TableCell<Spectrum, Double>() {
					@Override
					protected void updateItem(Double item, boolean empty) {
						super.updateItem(item, empty);
						if (item != null && item > 0.0) {
							this.setText(String.format("%.4f", item));
							this.setAlignment(Pos.CENTER_RIGHT);
						} 
						else {
							this.setText("");
						}
					}
				};
			}
		);
		table.getColumns().add(precursorColumn);
		
		table.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldValue, newValue) -> {
				Spectrum spectrum = table.getSelectionModel().getSelectedItem();
				manager.setActiveSpectrum(spectrum);
				manager.invoke(OnSelectSpectrum.class, spectrum);
			}
		);
		
		this.table = table;
		return table;
	}
	
	public TableView<Spectrum> getTableView() {
        return this.table;
	}
	
	public static SpectrumTableManager getInstance() {
		if (instance == null) {
			instance = new SpectrumTableManager();
		}
		return instance;
	}
}
