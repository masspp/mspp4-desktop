package ninja.mspp;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.prefs.Preferences;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.model.listener.ListenerInfo;
import ninja.mspp.core.model.listener.ListenerMethod;
import ninja.mspp.core.model.ms.Chromatogram;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.core.view.ViewInfo;
import ninja.mspp.interfaces.Job;
import ninja.mspp.view.GuiManager;
import ninja.mspp.view.MainFrame;

public class MsppManager {
	private static MsppManager instance = null;
	
	private List<ListenerInfo> listeners;
	
	private List<Sample> openedSamples;
	
	private Sample activeSample;
	private Spectrum activeSpectrum;
	private Chromatogram activeChromatogram;
	private Stage mainStage;
	private MainFrame mainFrame;
	private Preferences preferences;
	private Properties status;
	private ResourceBundle config;
	private ResourceBundle messages;	
	private File configFolder;
	private SessionFactory sessionFactory;
	
	private MsppManager() {
		this.openedSamples = new ArrayList<Sample>();
		this.listeners = null;
		this.activeSample = null;
		this.activeSpectrum = null;
		this.activeChromatogram = null;
		this.mainStage = null;
		this.mainFrame = null;
		this.preferences = Preferences.userRoot().node("mspp4.1/parameters");
		this.status = new Properties();
		this.config = null;
		this.configFolder = null;
	}
	
	public List<ListenerInfo> getListeners() {
		if (this.listeners == null) {
			this.listeners = this.createListenerList();
		}
		
		return this.listeners;
	}
	
	private List<ListenerInfo> createListenerList() {
		List<ListenerInfo> listeners = new ArrayList<ListenerInfo>();
		
		Reflections reflections = new Reflections(new ConfigurationBuilder()
				.setUrls(ClasspathHelper.forPackage("ninja.mspp"))
				.setScanners(new SubTypesScanner(false))
				.filterInputsBy(new FilterBuilder().include("ninja.mspp.*")));

		Set<Class<?>> classes = reflections.getSubTypesOf(Object.class);
		for (Class<?> clazz : classes) {
			Listener listener = clazz.getAnnotation(Listener.class);
			if (listener != null) {
				String name = listener.value();
				try {
					Constructor<?> constructor = clazz.getConstructor();
					Object object = constructor.newInstance();
					ListenerInfo info = new ListenerInfo(object, name);
					listeners.add(info);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}

		return listeners;
	}
	
	
	public <T extends Annotation> List<ListenerMethod<T>> getMethods(Class<T> clazz) {
		List<ListenerInfo> listeners = this.getListeners();
		List<ListenerMethod<T>> methods = new ArrayList<ListenerMethod<T>>();
		for (ListenerInfo listener : listeners) {
			Object object = listener.getObject();
			Method[] classMethods = object.getClass().getMethods();
			for (Method method : classMethods) {
				T annotation = method.getDeclaredAnnotation(clazz);
				if (annotation != null) {
					ListenerMethod<T> listenerMethod = new ListenerMethod<T>(object, method, annotation);
					methods.add(listenerMethod);
				}
			}
		}
		
		return methods;
	}

	
	public <T extends Annotation> void invoke(Class<T> clazz, Object... args) {
		List<ListenerMethod<T>> methods = this.getMethods(clazz);
		for (ListenerMethod<T> method : methods) {
			method.invoke(args);
		}
	}
	
	public <T> ViewInfo<T> createWindow(Class<T> clazz, String fxml) throws IOException {
		FXMLLoader loader = new FXMLLoader(clazz.getResource(fxml));
		Parent root = loader.load();
		T controller = loader.getController();
		return new ViewInfo<T>(root, controller);
	}
	
	public <T> ViewInfo<T> showDialog(Class<T> clazz, String fxml, String title) throws IOException {
		ViewInfo<T> info = this.createWindow(clazz, fxml);
		
		Stage stage = new Stage();
		stage.initOwner(this.mainStage);
		Scene scene = new Scene(info.getWindow());
		stage.setScene(scene);
		stage.setTitle(title);
		stage.show();
		
		return info;
	}
	
	public Sample getActiveSample() {
		return activeSample;
	}

	public void setActiveSample(Sample activeSample) {
		this.activeSample = activeSample;
	}

	public Spectrum getActiveSpectrum() {
		return activeSpectrum;
	}

	public void setActiveSpectrum(Spectrum activeSpectrum) {
		this.activeSpectrum = activeSpectrum;
	}

	public Chromatogram getActiveChromatogram() {
		return activeChromatogram;
	}

	public void setActiveChromatogram(Chromatogram activeChromatogram) {
		this.activeChromatogram = activeChromatogram;
	}
	
	public Stage getMainStage() {
		return mainStage;
	}
	
	public void setMainStage(Stage mainStage) {
		this.mainStage = mainStage;
	}
	
	public MainFrame getMainFrame() {
		return mainFrame;
	}
	
	public void setMainFrame(MainFrame mainFrame) {
		this.mainFrame = mainFrame;
	}
	
	public List<Sample> getOpenedSamples() {
		return openedSamples;
	}
	
	public void saveParameter(String key, String value) {
		this.preferences.put(key, value);
	}
	
	public String getParameter(String key) {
		return this.preferences.get(key, null);
	}
	
	public void setStatus(String key, String value) {
		this.status.setProperty(key, value);
	}
	
	public String getStatus(String key) {
		return this.status.getProperty(key, null);
	}
	
	public ResourceBundle getConfig() {
		if (this.config == null) {
			this.config = ResourceBundle.getBundle("ninja.mspp.conf.config");
		}

		return this.config;
	}
	
	public ResourceBundle getMessages() {
		if (this.messages == null) {
			this.messages = ResourceBundle.getBundle("ninja.mspp.conf.messages");
		}

		return this.messages;
	}
	
	public File getConfigFolder() {
		if(this.configFolder == null) {
			this.configFolder = new File(System.getProperty("user.home"), ".mspp4.1");
			if (!this.configFolder.exists()) {
				this.configFolder.mkdir();
			}
		}

		return this.configFolder;
	}
	
	public Session getSession() {
		if (this.sessionFactory == null) {
			this.sessionFactory = new Configuration().configure().buildSessionFactory();
		}
		return this.sessionFactory.openSession();
	}
	
	public void closeSession() {
		if (this.sessionFactory != null) {
			this.sessionFactory.close();
		}
	}
	
	public static MsppManager getInstance() {
		if (MsppManager.instance == null) {
            MsppManager.instance = new MsppManager();
        }
        return MsppManager.instance;
    }

	
	public void startTask(Job job) throws InterruptedException {
		GuiManager gui = GuiManager.getInstance();		
		
		Task<Object> task = new Task<Object>() {
			@Override
			protected Object call() throws Exception {
				return job.execute();
			}
		};
		task.setOnSucceeded(
			event -> {
				try {
					Object result = task.get();
					job.onSucceeded(result);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		);
				
		Thread thread = new Thread(task);
		
		Thread cursorThread = new Thread() {
			@Override
			public void run() {
				Platform.runLater(
					() -> {
						gui.startWaitingCursor();
					}
				);
				thread.start();
				try {
					thread.join();
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				Platform.runLater(
					() -> {
						gui.endWaitingCursor();
					}
				);
			}
		};

		cursorThread.start();
	}
}
