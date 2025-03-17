package ninja.mspp.view.part.table.chromatogram;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.method.OnSelectChromatogram;
import ninja.mspp.core.model.ms.Chromatogram;

public class ChromatogramTableManager {
	private static ChromatogramTableManager instance;
	
	private TableView<Chromatogram> chromatogramTable;
	
	private ChromatogramTableManager() {
		this.chromatogramTable = null;
	}
	
	public TableView<Chromatogram> createTableView() {
		MsppManager manager = MsppManager.getInstance();
		
		TableView<Chromatogram> table = new TableView<Chromatogram>();
		
		TableColumn<Chromatogram, String> nameColumn = new TableColumn<Chromatogram, String>("Name");		
		nameColumn.setPrefWidth(100);
		nameColumn.setCellValueFactory(new PropertyValueFactory<Chromatogram, String>("name"));
		table.getColumns().add(nameColumn);
		
		TableColumn<Chromatogram, Double> mzColumn = new TableColumn<Chromatogram, Double>("m/z");
		mzColumn.setPrefWidth(75);
		mzColumn.setCellValueFactory(new PropertyValueFactory<Chromatogram, Double>("mz"));
		mzColumn.setCellFactory(
			(column) -> {
				return new TableCell<Chromatogram, Double>() {
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
		table.getColumns().add(mzColumn);
		
		table.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldValue, newValue) -> {
				Chromatogram chromatogram = table.getSelectionModel().getSelectedItem();
				manager.setActiveChromatogram(chromatogram);
				manager.invoke(OnSelectChromatogram.class, chromatogram);
			}
		);
		
		this.chromatogramTable = table;		
		return table;
	}
	
	public TableView<Chromatogram> getTableView() {
		return this.chromatogramTable;
	}
	
	public static ChromatogramTableManager getInstance() {
		if (instance == null) {
			instance = new ChromatogramTableManager();
		}
		return instance;
	}
}
