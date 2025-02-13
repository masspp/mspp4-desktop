package ninja.mspp.io.mzml;

import java.io.File;

import io.github.msdk.MSDKException;
import javafx.stage.FileChooser;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.MenuAction;
import ninja.mspp.core.annotation.method.OnOpenSample;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.TicChromatogram;
import ninja.mspp.view.GuiManager;

@Listener("mzML Input Listener")
public class MzmlListener {
	private static final String FOLDER_KEY = "MZML_INPUT_FOLDER";
	
	@MenuAction(value = "File > Open > mzML...", order = 0)
	public void onMzml() throws MSDKException {
		MsppManager manager = MsppManager.getInstance();
		
		String folderName = manager.getParameter(FOLDER_KEY);
				
		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("mzML Files", "*.mzML"));
		chooser.setTitle("Open mzML File");
		if (folderName != null) {
			File folder = new File(folderName);
			if(folder.exists() && folder.isDirectory()) {
				chooser.setInitialDirectory(new File(folderName));
			}
		}
		File file = chooser.showOpenDialog(manager.getMainStage());

		if (file != null) {
			GuiManager gui = GuiManager.getInstance();
			gui.startWaitingCursor();
			
			File folder = file.getParentFile();
			manager.saveParameter(FOLDER_KEY, folder.getAbsolutePath());
			MzmlReader reader = new MzmlReader();
			Sample sample = reader.read(file.getAbsolutePath());
			if(sample.getChromatograms().isEmpty()) {
				TicChromatogram tic = new TicChromatogram(sample);
				sample.getChromatograms().add(tic);
			}
			manager.invoke(OnOpenSample.class, sample);
			
			gui.endWaitingCursor();
		}
	}
}
