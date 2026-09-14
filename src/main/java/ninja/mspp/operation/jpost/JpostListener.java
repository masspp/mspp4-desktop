package ninja.mspp.operation.jpost;

import java.io.IOException;

import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.MenuAction;
import ninja.mspp.view.GuiManager;

@Listener("jPOST")
public class JpostListener {
	@MenuAction(value = "Tools > jPOST", order = 1)
	public void onMenu() throws IOException {
		GuiManager gui = GuiManager.getInstance();
		gui.showDialog(
			JpostDialog.class,
			"JpostDialog.fxml",
			"jPOST"
		);
	}
}
