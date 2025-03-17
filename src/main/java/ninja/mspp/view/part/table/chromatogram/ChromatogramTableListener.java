package ninja.mspp.view.part.table.chromatogram;

import javafx.scene.control.TableView;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.OnSelectSample;
import ninja.mspp.core.model.ms.Chromatogram;
import ninja.mspp.core.model.ms.Sample;

@Listener("Chromatogram Table Listener")
public class ChromatogramTableListener {
	@OnSelectSample
	public void onSelectSample(Sample sample) {
		ChromatogramTableManager manager = ChromatogramTableManager.getInstance();
		TableView<Chromatogram> table = manager.getTableView();
		if (table != null) {
			table.getItems().clear();
			for (Chromatogram chromatogram : sample.getChromatograms()) {
				table.getItems().add(chromatogram);
			}
		}
	}
}
