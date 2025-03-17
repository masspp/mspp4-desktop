package ninja.mspp.operation.mass_calculator;

import java.io.IOException;

import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.MenuAction;
import ninja.mspp.core.annotation.parameter.ActiveSample;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.view.GuiManager;

@Listener("Mass Calculator")
public class MassCalculatorListener {
	@MenuAction("Tools > Mass Calculator...")
	public void onMenu(@ActiveSample Sample sample) throws IOException {
		GuiManager manager = GuiManager.getInstance();
		
		manager.showDialog(MassCalculatorDialog.class, "MassCalculatorDialog.fxml", "Mass Calculator");
	}
}
