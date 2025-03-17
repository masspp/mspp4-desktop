package ninja.mspp.view.part.table.sample;

import java.util.ResourceBundle;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableView;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.OnOpenSample;
import ninja.mspp.core.model.ms.Sample;

@Listener("Sample Table Listener")
public class SampleTableListener {
	@OnOpenSample
	public void onOpenSample(Sample sample) {
		MsppManager manager = MsppManager.getInstance();
		ResourceBundle messages = manager.getMessages();
		
		boolean found = false;
		for (Sample s : manager.getOpenedSamples()) {
			if (s.getId().equals(sample.getId())) {
				found = true;
			}
		}
		
		if(found) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText(messages.getString("file.already_opened.title"));
			alert.setContentText(messages.getString("file.already_opened.content"));
			alert.showAndWait();
		}
		else {
			manager.getOpenedSamples().add(sample);
			TableView<Sample> table = SampleTableManager.getInstance().getTableView();
			table.getItems().add(sample);
		}
	}
}
