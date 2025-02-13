package ninja.mspp.operation.xic;

import java.io.IOException;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.OnSelectChromatogram;
import ninja.mspp.core.annotation.method.OnSelectSample;
import ninja.mspp.core.annotation.method.SpectrumAction;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.XicChromatogram;
import ninja.mspp.core.view.SpectrumActionEvent;
import ninja.mspp.core.view.ViewInfo;

@Listener("XIC")
public class XicListener {
	@SpectrumAction("XIC ...")
	public void createXIC(SpectrumActionEvent event) throws IOException {
		MsppManager manager = MsppManager.getInstance();
		ViewInfo<XicDialog> info = manager.createWindow(XicDialog.class, "XicDialog.fxml");
		
		Parent parent = info.getWindow();
		XicDialog controller = info.getController();
		
		controller.setMz(event.getMz());
		controller.setTolerance(0.1);
		
		Stage stage = new Stage();
		stage.setTitle("XIC");
		stage.initModality(Modality.WINDOW_MODAL);
		stage.setScene(new Scene(parent));
		stage.showAndWait();

		if(controller.isConfirmed()) {
			double mz = controller.getMz();
			double tol = controller.getTolerance();
			this.openXic(event.getSpectrum().getSample(), mz, tol);
		}
	}
	
	private void openXic(Sample sample, double mz, double tol) {
		MsppManager manager = MsppManager.getInstance();
		
		int number = sample.getChromatograms().size();
		XicChromatogram xic = new XicChromatogram(sample, number, mz, tol);
		sample.getChromatograms().add(xic);
		
		manager.invoke(OnSelectSample.class, sample);
		manager.invoke(OnSelectChromatogram.class, xic);
	}
}
