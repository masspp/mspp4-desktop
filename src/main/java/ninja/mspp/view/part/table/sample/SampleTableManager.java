package ninja.mspp.view.part.table.sample;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.method.OnSelectSample;
import ninja.mspp.core.model.ms.Sample;

public class SampleTableManager {
	private static SampleTableManager instance;
	
	private TableView<Sample> tableView;
	
	private SampleTableManager() {
		this.tableView = null;
	}
	
	public TableView<Sample> createTableView() {
		MsppManager manager = MsppManager.getInstance();
		
		TableView<Sample> table = new TableView<Sample>();
		TableColumn<Sample, String> nameColumn = new TableColumn<Sample, String>("Name");
		table.getColumns().add(nameColumn);
		nameColumn.prefWidthProperty().bind(table.widthProperty().multiply(0.95));
		nameColumn.setCellValueFactory(new PropertyValueFactory<Sample, String>("name"));
		table.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldValue, newValue) -> {
				Sample sample = table.getSelectionModel().getSelectedItem();
				manager.setActiveSample(sample);
				manager.invoke(OnSelectSample.class, sample);
			}
		);
			
		for (Sample sample : manager.getOpenedSamples()) {
			table.getItems().add(sample);
		}
		
		this.tableView = table;
		
		return table;
	}
	
	public TableView<Sample> getTableView() {
		return this.tableView;
	}
	
	public static SampleTableManager getInstance() {
		if (instance == null) {
			instance = new SampleTableManager();
		}
		return instance;
	}
}
