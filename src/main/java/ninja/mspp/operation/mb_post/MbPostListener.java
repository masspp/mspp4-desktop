package ninja.mspp.operation.mb_post;

import java.io.IOException;

import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.MenuAction;
import ninja.mspp.view.GuiManager;

@Listener("MB-POST")
public class MbPostListener {
	@MenuAction(value = "Tools > MB-POST", order = 0)
	public void onMenu() throws IOException {
		GuiManager gui = GuiManager.getInstance();
		gui.showDialog(
			MbPostDialog.class,
			"MbPostDialog.fxml",
			"MB-POST"
		);
	}
}
