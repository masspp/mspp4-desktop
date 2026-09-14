package ninja.mspp.view.mode.stack;

import java.io.IOException;

import javafx.scene.Parent;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.ViewMode;
import ninja.mspp.core.view.ViewInfo;
import ninja.mspp.view.GuiManager;

@Listener("Stack View Mode")
public class StackViewListener {
	@ViewMode(value = "Stack", order = 2)
	public Parent createStackView() throws IOException {
		GuiManager manager = GuiManager.getInstance();
		ViewInfo<StackViewMode> info = manager.createWindow(StackViewMode.class, "StackViewMode.fxml");
		return info.getWindow();
	}
}
