package ninja.mspp.help;

import java.awt.Desktop;
import java.net.URI;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.MenuAction;

@Listener("Help")
public class HelpListener {
	/** Quick manual of this version of Mass++4, which is the development version. */
	private static final String MANUAL_URL = "https://github.com/masspp/quick_manual/tree/develop";

	@MenuAction(value = "Help > Quick Manual...", order = 0)
	public void onQuickManual() {
		openInBrowser(MANUAL_URL);
	}

	private static void openInBrowser(String url) {
		new Thread(
			() -> {
				try {
					if(Desktop.isDesktopSupported()
							&& Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
						Desktop.getDesktop().browse(new URI(url));
					}
					else {
						showUrl(url);
					}
				}
				catch(Exception e) {
					e.printStackTrace();
					showUrl(url);
				}
			},
			"help-browser"
		).start();
	}

	/**
	 * Shows the address when it cannot be opened, so that it can be opened by hand.
	 */
	private static void showUrl(String url) {
		Platform.runLater(
			() -> {
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Help");
				alert.setHeaderText("The browser could not be opened");
				alert.setContentText("Open this page in your browser:" + System.lineSeparator() + url);
				alert.showAndWait();
			}
		);
	}
}
