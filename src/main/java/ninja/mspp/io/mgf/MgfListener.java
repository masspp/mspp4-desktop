package ninja.mspp.io.mgf;

import java.io.File;
import java.util.Collections;

import javafx.stage.FileChooser;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.MenuAction;
import ninja.mspp.io.mzml.MsDataOpener;
import ninja.mspp.view.GuiManager;

@Listener("MGF Input Listener")
public class MgfListener {
	private static final String FOLDER_KEY = "MGF_INPUT_FOLDER";

	@MenuAction(value = "File > Open > MGF...", order = 1)
	public void onOpenMgf() {
		GuiManager guiManager = GuiManager.getInstance();
		MsppManager manager = MsppManager.getInstance();

		String folderName = manager.getParameter(FOLDER_KEY);

		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MGF Files", "*.mgf"));
		chooser.setTitle("Open MGF File");
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

		File parent = file.getParentFile();
		if(parent != null) {
			manager.saveParameter(FOLDER_KEY, parent.getAbsolutePath());
		}

		MsDataOpener.open(Collections.singletonList(file));
	}
}
