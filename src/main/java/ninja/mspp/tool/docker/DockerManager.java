package ninja.mspp.tool.docker;

import java.io.File;

import ninja.mspp.MsppManager;
import ninja.mspp.core.util.FileUtil;

public class DockerManager {
	private static DockerManager instance;
	
	private File folder;
	
	private DockerManager() {
		this.folder = null;
	}
	
	public File getDokcerFolder() {
		if(this.folder == null) {
			MsppManager manager = MsppManager.getInstance();	
			File parent = manager.getConfigFolder();
			File dockerFolder = new File(parent, "docker");
			if(dockerFolder.exists()) {
				FileUtil.delete(dockerFolder);
			}
			dockerFolder.mkdir();
			this.folder = dockerFolder;
		}
		return this.folder;
	}
	
	public DockerManager getInstance() {
		if(instance == null) {
			instance = new DockerManager();
		}
		
		return instance;
	}
}
