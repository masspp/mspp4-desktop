package ninja.mspp.io.mzml;

import java.io.File;
import java.util.Collections;

import javafx.stage.FileChooser;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.MenuAction;
import ninja.mspp.view.GuiManager;

@Listener("MS Data Input Listener")
public class MzmlListener {

	@MenuAction(value = "File > Open > MS Data...", order = 0)
	public void onOpenMsData() {
		GuiManager guiManager = GuiManager.getInstance();
		MsppManager manager = MsppManager.getInstance();

		String folderName = manager.getParameter(MsDataOpener.FOLDER_KEY);

		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().add(
			new FileChooser.ExtensionFilter("MS Files", "*.mzML", "*.raw", "*.wiff", "*.lcd")
		);
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("mzML Files", "*.mzML"));
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Thermo RAW Files", "*.raw"));
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Sciex WIFF Files", "*.wiff"));
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Shimadzu LCD Files", "*.lcd"));
		chooser.setTitle("Open MS Data File");
		if(folderName != null) {
			File folder = new File(folderName);
			if(folder.exists() && folder.isDirectory()) {
				chooser.setInitialDirectory(folder);
			}
		}
		File file = chooser.showOpenDialog(guiManager.getMainStage());
		if(file == null) {
			return;
		}

		MsDataOpener.open(Collections.singletonList(file));
	}
}
