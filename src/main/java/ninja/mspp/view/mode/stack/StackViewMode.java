package ninja.mspp.view.mode.stack;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import ninja.mspp.core.model.ms.Chromatogram;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.view.mode.stack.StackViewManager.Kind;
import ninja.mspp.view.part.table.chromatogram.ChromatogramTableManager;
import ninja.mspp.view.part.table.sample.SampleTableManager;
import ninja.mspp.view.part.table.spectrum.SpectrumTableManager;

public class StackViewMode implements Initializable {
	@FXML private BorderPane sampleTablePane;
	@FXML private BorderPane chromatogramTablePane;
	@FXML private BorderPane spectrumTablePane;
	@FXML private TabPane tabs;
	@FXML private Tab spectrumTab;
	@FXML private Tab chromatogramTab;
	@FXML private VBox spectrumStack;
	@FXML private VBox chromatogramStack;
	@FXML private ScrollPane spectrumScroll;
	@FXML private ScrollPane chromatogramScroll;
	@FXML private Slider heightSlider;
	@FXML private CheckBox ySyncCheckBox;

	private final List<StackEntry> spectrumEntries = new ArrayList<StackEntry>();
	private final List<StackEntry> chromatogramEntries = new ArrayList<StackEntry>();

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.installSampleTable();
		this.installSpectrumTable();
		this.installChromatogramTable();

		this.heightSlider.setValue(StackViewManager.getInstance().getEntryHeight());
		this.heightSlider.valueProperty().addListener((obs, oldV, newV) -> {
			double h = newV.doubleValue();
			StackViewManager.getInstance().setEntryHeight(h);
			for(StackEntry e : this.spectrumEntries) {
				e.applyHeight(h);
			}
			for(StackEntry e : this.chromatogramEntries) {
				e.applyHeight(h);
			}
		});

		this.ySyncCheckBox.setSelected(StackViewManager.getInstance().isYSync());
		this.ySyncCheckBox.selectedProperty().addListener((obs, oldV, newV) -> {
			StackViewManager.getInstance().setYSync(newV.booleanValue());
			StackViewManager.getInstance().redrawAll(Kind.SPECTRUM);
			StackViewManager.getInstance().redrawAll(Kind.CHROMATOGRAM);
		});

		StackViewManager.getInstance().setController(this);
	}

	private void installSampleTable() {
		TableView<Sample> table = SampleTableManager.getInstance().createTableView();
		this.sampleTablePane.setCenter(table);
	}

	private void installSpectrumTable() {
		TableView<Spectrum> table = SpectrumTableManager.getInstance().createTableView();
		table.setRowFactory(tv -> {
			TableRow<Spectrum> row = new TableRow<Spectrum>();
			row.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
				if(e.getClickCount() == 2 && !row.isEmpty()) {
					this.addSpectrum(row.getItem());
				}
			});
			return row;
		});
		this.spectrumTablePane.setCenter(table);
	}

	private void installChromatogramTable() {
		TableView<Chromatogram> table = ChromatogramTableManager.getInstance().createTableView();
		table.setRowFactory(tv -> {
			TableRow<Chromatogram> row = new TableRow<Chromatogram>();
			row.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
				if(e.getClickCount() == 2 && !row.isEmpty()) {
					this.addChromatogram(row.getItem());
				}
			});
			return row;
		});
		this.chromatogramTablePane.setCenter(table);
	}

	void addSpectrum(Spectrum spectrum) {
		StackEntry[] holder = new StackEntry[1];
		StackEntry entry = new StackEntry(Kind.SPECTRUM, spectrum.getName(),
			() -> this.removeEntry(holder[0]));
		holder[0] = entry;
		entry.getCanvas().setSpectrum(spectrum);
		this.spectrumEntries.add(entry);
		this.spectrumStack.getChildren().add(entry);
		this.tabs.getSelectionModel().select(this.spectrumTab);
		StackViewManager.getInstance().redrawAll(Kind.SPECTRUM);
	}

	void addChromatogram(Chromatogram chromatogram) {
		StackEntry[] holder = new StackEntry[1];
		StackEntry entry = new StackEntry(Kind.CHROMATOGRAM, formatChromatogramTitle(chromatogram),
			() -> this.removeEntry(holder[0]));
		holder[0] = entry;
		entry.getCanvas().setChromatogram(chromatogram);
		this.chromatogramEntries.add(entry);
		this.chromatogramStack.getChildren().add(entry);
		this.tabs.getSelectionModel().select(this.chromatogramTab);
		StackViewManager.getInstance().redrawAll(Kind.CHROMATOGRAM);
	}

	private static String formatChromatogramTitle(Chromatogram chromatogram) {
		String name = chromatogram.getName();
		Double mz = chromatogram.getMz();
		if(mz != null && mz.doubleValue() > 0.0) {
			return name + String.format(" [m/z %.4f]", mz.doubleValue());
		}
		return name;
	}

	void removeEntry(StackEntry entry) {
		Kind kind = entry.getKind();
		List<StackEntry> entries = kind == Kind.SPECTRUM ? this.spectrumEntries : this.chromatogramEntries;
		VBox container = kind == Kind.SPECTRUM ? this.spectrumStack : this.chromatogramStack;
		entries.remove(entry);
		container.getChildren().remove(entry);
		entry.getCanvas().dispose();
		StackViewManager.getInstance().redrawAll(kind);
	}
}
