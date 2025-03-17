package ninja.mspp.view.part.table.spectrum;

import javafx.scene.control.TableView;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.OnSelectSample;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;

@Listener("Spectrum Table Listener")
public class SpectrumTableListener {
	
	@OnSelectSample
	public void onSelectSample(Sample sample) {
		SpectrumTableManager manager = SpectrumTableManager.getInstance();
		TableView<Spectrum> table = manager.getTableView();
		if (table != null) {
			table.getItems().clear();
			for (Spectrum spectrum : sample.getSpectra()) {
				table.getItems().add(spectrum);
			}
		}
	}
}
