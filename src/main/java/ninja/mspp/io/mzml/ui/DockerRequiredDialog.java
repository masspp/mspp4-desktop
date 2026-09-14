package ninja.mspp.io.mzml.ui;

import java.io.File;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import ninja.mspp.io.mzml.convert.VendorConverter;
import ninja.mspp.tool.docker.DockerManager;

/**
 * Tells the user that Docker Desktop has to be running before a vendor raw file
 * (Thermo .raw, Sciex .wiff, Shimadzu .lcd) can be opened, because such files are
 * converted to mzML by msconvert inside a Docker container.
 */
public class DockerRequiredDialog {

	/**
	 * Must be called on the JavaFX application thread.
	 *
	 * @return true if the file can be opened now (it is not a vendor file, or Docker is running).
	 */
	public static boolean check(String fileName) {
		return check(fileName, null);
	}

	/**
	 * Must be called on the JavaFX application thread.
	 *
	 * @param note additional sentence appended to the message, or null.
	 * @return true if the file can be opened now (it is not a vendor file, or Docker is running).
	 */
	public static boolean check(String fileName, String note) {
		if(fileName == null || !VendorConverter.isVendorFile(new File(fileName))) {
			return true;
		}
		if(DockerManager.getInstance().isDockerAvailable()) {
			return true;
		}

		String content = "Opening " + fileName + " requires Docker Desktop to convert "
			+ "the vendor raw file to mzML.\nPlease start Docker Desktop and try again.";
		if(note != null && !note.isEmpty()) {
			content = content + "\n\n" + note;
		}

		Alert alert = new Alert(AlertType.WARNING);
		alert.setTitle("Docker Required");
		alert.setHeaderText("Docker is not running");
		alert.setContentText(content);
		alert.showAndWait();
		return false;
	}
}
