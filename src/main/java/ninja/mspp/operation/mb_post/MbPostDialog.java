package ninja.mspp.operation.mb_post;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.method.OnOpenSample;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.TicChromatogram;
import ninja.mspp.io.mzml.MzmlReader;
import ninja.mspp.io.mzml.ui.ProgressDialog;
import ninja.mspp.operation.mb_post.model.MbPostFile;
import ninja.mspp.operation.mb_post.model.MbPostFileDetail;
import ninja.mspp.operation.mb_post.model.MbPostFileDetail.PresetGroup;
import ninja.mspp.operation.mb_post.model.MbPostFileDetail.PresetItem;
import ninja.mspp.operation.mb_post.model.MbPostProject;
import ninja.mspp.view.GuiManager;

public class MbPostDialog {

	@FXML private StackPane rootPane;

	// List view
	@FXML private VBox listView;
	@FXML private TextField searchField;
	@FXML private TableView<MbPostProject> projectTable;
	@FXML private TableColumn<MbPostProject, String> projIdColumn;
	@FXML private TableColumn<MbPostProject, String> projTitleColumn;
	@FXML private TableColumn<MbPostProject, String> projPiColumn;
	@FXML private TableColumn<MbPostProject, Integer> projFileCountColumn;
	@FXML private TableColumn<MbPostProject, String> projDateColumn;
	@FXML private TableColumn<MbPostProject, MbPostProject> projActionColumn;
	@FXML private ScrollPane projectDetailScroll;
	@FXML private VBox projectDetailContent;
	@FXML private Label projectDetailPlaceholder;
	@FXML private Label listStatusLabel;

	// Detail view
	@FXML private VBox detailView;
	@FXML private Button backButton;
	@FXML private Label detailTitleLabel;
	@FXML private Label idLabel;
	@FXML private Label titleLabel;
	@FXML private Label piLabel;
	@FXML private Label affiliationLabel;
	@FXML private Label keywordsLabel;
	@FXML private Label descriptionLabel;
	@FXML private TableView<MbPostFile> filesTable;
	@FXML private TableColumn<MbPostFile, String> nameColumn;
	@FXML private TableColumn<MbPostFile, String> typeColumn;
	@FXML private TableColumn<MbPostFile, Long> sizeColumn;
	@FXML private TableColumn<MbPostFile, MbPostFile> actionColumn;
	@FXML private ScrollPane detailScroll;
	@FXML private VBox detailContent;
	@FXML private Label detailPlaceholder;
	@FXML private Label fileStatusLabel;

	private final ObservableList<MbPostProject> projects = FXCollections.observableArrayList();
	private final ObservableList<MbPostFile> files = FXCollections.observableArrayList();
	private FilteredList<MbPostProject> filteredProjects;
	private MbPostProject currentProject;
	private long fileDetailRequestId;

	@FXML
	private void initialize() {
		this.initListView();
		this.initDetailView();
		this.loadProjects();
	}

	// =============================== LIST VIEW ===============================

	private void initListView() {
		this.projIdColumn.setCellValueFactory(new PropertyValueFactory<>("mbpostId"));
		this.projTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
		this.projPiColumn.setCellValueFactory(new PropertyValueFactory<>("principalInvestigator"));
		this.projFileCountColumn.setCellValueFactory(new PropertyValueFactory<>("fileCount"));
		this.projDateColumn.setCellValueFactory(new PropertyValueFactory<>("modifiedAt"));
		this.projActionColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
		this.projActionColumn.setCellFactory(col -> new ProjectActionCell());

		this.filteredProjects = new FilteredList<>(this.projects, p -> true);
		this.projectTable.setItems(this.filteredProjects);

		this.searchField.textProperty().addListener((obs, oldV, newV) -> {
			String k = newV == null ? "" : newV.trim();
			this.filteredProjects.setPredicate(p -> p.matchesKeyword(k));
			this.updateListStatus();
		});

		this.projectTable.getSelectionModel().selectedItemProperty().addListener(
			(obs, oldV, newV) -> this.renderProjectDetail(newV));
	}

	private void renderProjectDetail(MbPostProject p) {
		this.projectDetailContent.getChildren().clear();
		if(p == null) {
			this.projectDetailContent.getChildren().add(
				new Label("Select a project to view its details."));
			return;
		}
		this.projectDetailContent.getChildren().add(sectionHeader(safe(p.getMbpostId())));
		this.projectDetailContent.getChildren().add(infoGrid(new String[][] {
			{ "Title", safe(p.getTitle()) },
			{ "PI", safe(p.getPrincipalInvestigator()) },
			{ "Affiliation", safe(p.getAffiliation()) },
			{ "Submitter", safe(p.getSubmitter()) },
			{ "Keywords", safe(p.getKeywords()) },
			{ "Description", safe(p.getDescription()) },
			{ "Files", String.valueOf(p.getFileCount()) },
			{ "Announcement", safe(p.getAnnouncementDate()) },
			{ "Created", safe(p.getCreatedAt()) },
			{ "Modified", safe(p.getModifiedAt()) },
			{ "PubMed", safe(p.getPubmedId()) },
			{ "Note", safe(p.getNote()) }
		}));
	}

	private class ProjectActionCell extends TableCell<MbPostProject, MbPostProject> {
		private final Button button = new Button("Open");

		ProjectActionCell() {
			this.button.setOnAction(e -> {
				MbPostProject item = this.getItem();
				if(item != null) {
					MbPostDialog.this.showDetail(item);
				}
			});
		}

		@Override
		protected void updateItem(MbPostProject item, boolean empty) {
			super.updateItem(item, empty);
			this.setGraphic(empty || item == null ? null : this.button);
		}
	}

	private void loadProjects() {
		this.listStatusLabel.setText("Loading projects...");
		final MbPostClient client = new MbPostClient();
		Task<List<MbPostProject>> task = new Task<List<MbPostProject>>() {
			@Override
			protected List<MbPostProject> call() throws Exception {
				return client.listAllProjects(fetched ->
					updateMessage("Loaded " + fetched + " projects..."));
			}
		};
		task.messageProperty().addListener((obs, oldV, newV) -> this.listStatusLabel.setText(newV));
		task.setOnSucceeded(e -> {
			this.projects.setAll(task.getValue());
			this.updateListStatus();
		});
		task.setOnFailed(e -> {
			this.listStatusLabel.setText("Failed to load projects");
			showError("Failed to load projects", task.getException());
		});
		Thread t = new Thread(task, "mb-post-projects");
		t.setDaemon(true);
		t.start();
	}

	private void updateListStatus() {
		int shown = this.filteredProjects.size();
		int total = this.projects.size();
		this.listStatusLabel.setText(shown == total
			? total + " projects"
			: shown + " / " + total + " projects");
	}

	// ============================= DETAIL VIEW ===============================

	private void initDetailView() {
		this.nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		this.typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
		this.sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
		this.sizeColumn.setCellFactory(col -> new TableCell<MbPostFile, Long>() {
			@Override
			protected void updateItem(Long item, boolean empty) {
				super.updateItem(item, empty);
				this.setText(empty || item == null ? "" : formatSize(item.longValue()));
				this.setAlignment(Pos.CENTER_RIGHT);
			}
		});
		this.actionColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
		this.actionColumn.setCellFactory(col -> new ActionCell());
		this.filesTable.setItems(this.files);

		this.filesTable.getSelectionModel().selectedItemProperty().addListener(
			(obs, oldV, newV) -> this.loadFileDetail(newV));
	}

	private void showDetail(MbPostProject project) {
		this.currentProject = project;
		this.detailTitleLabel.setText(project.getMbpostId() + " : " + safe(project.getTitle()));
		this.idLabel.setText(safe(project.getMbpostId()));
		this.titleLabel.setText(safe(project.getTitle()));
		this.piLabel.setText(safe(project.getPrincipalInvestigator()));
		this.affiliationLabel.setText(safe(project.getAffiliation()));
		this.keywordsLabel.setText(safe(project.getKeywords()));
		this.descriptionLabel.setText(safe(project.getDescription()));

		this.files.clear();
		this.clearFileDetail("Select a file to view its details.");
		this.fileStatusLabel.setText("Loading files...");

		this.setView(false);
		this.loadFiles();
	}

	@FXML
	private void onBack() {
		this.currentProject = null;
		this.setView(true);
	}

	private void setView(boolean showList) {
		this.listView.setVisible(showList);
		this.listView.setManaged(showList);
		this.detailView.setVisible(!showList);
		this.detailView.setManaged(!showList);
	}

	private String location() {
		return this.currentProject.getLocation() == null
			? this.currentProject.getMbpostId()
			: this.currentProject.getLocation();
	}

	private void loadFiles() {
		final MbPostClient client = new MbPostClient();
		final int total = this.currentProject.getFileCount();
		final String location = this.location();
		final MbPostProject pinned = this.currentProject;

		Task<List<MbPostFile>> task = new Task<List<MbPostFile>>() {
			@Override
			protected List<MbPostFile> call() throws Exception {
				return client.listAllFiles(location, total,
					fetched -> updateMessage("Loaded " + fetched
						+ (total > 0 ? " / " + total : "") + " files..."));
			}
		};
		task.messageProperty().addListener((obs, oldV, newV) -> {
			if(this.currentProject == pinned) {
				this.fileStatusLabel.setText(newV);
			}
		});
		task.setOnSucceeded(e -> {
			if(this.currentProject != pinned) {
				return;
			}
			this.files.setAll(task.getValue());
			this.fileStatusLabel.setText(this.files.size() + " files");
		});
		task.setOnFailed(e -> {
			if(this.currentProject != pinned) {
				return;
			}
			this.fileStatusLabel.setText("Failed to load files");
			showError("Failed to load files", task.getException());
		});
		Thread t = new Thread(task, "mb-post-files");
		t.setDaemon(true);
		t.start();
	}

	// =========================== FILE DETAIL PANE ============================

	private void loadFileDetail(MbPostFile file) {
		final long requestId = ++this.fileDetailRequestId;
		if(file == null) {
			this.clearFileDetail("Select a file to view its details.");
			return;
		}
		this.clearFileDetail("Loading file detail...");

		final String location = this.location();
		final String fileId = file.getId();
		Task<MbPostFileDetail> task = new Task<MbPostFileDetail>() {
			@Override
			protected MbPostFileDetail call() throws Exception {
				return new MbPostClient().getFileDetail(location, fileId);
			}
		};
		task.setOnSucceeded(e -> {
			if(requestId != this.fileDetailRequestId) {
				return;
			}
			this.renderFileDetail(task.getValue());
		});
		task.setOnFailed(e -> {
			if(requestId != this.fileDetailRequestId) {
				return;
			}
			this.clearFileDetail("Failed to load file detail.");
		});
		Thread t = new Thread(task, "mb-post-file-detail");
		t.setDaemon(true);
		t.start();
	}

	private void clearFileDetail(String placeholder) {
		this.detailContent.getChildren().clear();
		Label l = new Label(placeholder);
		this.detailContent.getChildren().add(l);
	}

	private void renderFileDetail(MbPostFileDetail detail) {
		this.detailContent.getChildren().clear();
		if(detail == null) {
			this.detailContent.getChildren().add(new Label("No detail."));
			return;
		}

		this.detailContent.getChildren().add(sectionHeader(safe(detail.getName())));
		this.detailContent.getChildren().add(infoGrid(new String[][] {
			{ "ID", safe(detail.getId()) },
			{ "Type", safe(detail.getType()) },
			{ "Size", formatSize(detail.getSize()) },
			{ "Server", detail.isOnServer() ? "Yes" : "No" },
			{ "Checksum", safe(detail.getChecksum()) }
		}));

		for(PresetGroup g : detail.getPresets()) {
			List<PresetItem> items = g.getPresets();
			if(items.isEmpty()) {
				continue;
			}
			this.detailContent.getChildren().add(sectionHeader(prettyCategory(g.getCategory())));
			String[][] rows = new String[items.size()][2];
			for(int i = 0; i < items.size(); i++) {
				PresetItem it = items.get(i);
				String label = it.getLabel() == null || it.getLabel().isEmpty()
					? safe(it.getKey()) : it.getLabel();
				String value = safe(it.getValue());
				if(it.getOntologyValue() != null && !it.getOntologyValue().isEmpty()
						&& !it.getOntologyValue().equals(it.getValue())) {
					value = value + "  [" + it.getOntologyValue() + "]";
				}
				rows[i][0] = label;
				rows[i][1] = value;
			}
			this.detailContent.getChildren().add(infoGrid(rows));
		}
	}

	private static Label sectionHeader(String text) {
		Label l = new Label(text);
		l.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4 0 2 0;");
		return l;
	}

	private static GridPane infoGrid(String[][] rows) {
		GridPane g = new GridPane();
		g.setHgap(8.0);
		g.setVgap(2.0);
		g.setPadding(new Insets(2, 2, 2, 8));
		ColumnConstraints c0 = new ColumnConstraints();
		c0.setMinWidth(110.0);
		c0.setPrefWidth(140.0);
		ColumnConstraints c1 = new ColumnConstraints();
		c1.setHgrow(Priority.ALWAYS);
		g.getColumnConstraints().addAll(c0, c1);
		for(int i = 0; i < rows.length; i++) {
			Label k = new Label(rows[i][0]);
			k.setStyle("-fx-text-fill: #555;");
			Label v = new Label(rows[i][1] == null ? "" : rows[i][1]);
			v.setWrapText(true);
			g.add(k, 0, i);
			g.add(v, 1, i);
		}
		return g;
	}

	private static String prettyCategory(String category) {
		if(category == null || category.isEmpty()) {
			return "Other";
		}
		return Character.toUpperCase(category.charAt(0)) + category.substring(1);
	}

	// ============================ FILE OPEN FLOW =============================

	private void onOpen(MbPostFile file) {
		FileKind kind = FileKind.classify(file.getName());
		if(kind == FileKind.UNSUPPORTED) {
			return;
		}
		List<MbPostFile> companions = this.findCompanions(file);
		this.downloadAndOpen(file, companions, kind);
	}

	private List<MbPostFile> findCompanions(MbPostFile primary) {
		List<MbPostFile> result = new ArrayList<MbPostFile>();
		String name = primary.getName();
		String lower = name.toLowerCase(Locale.ROOT);
		if(!lower.endsWith(".wiff")) {
			return result;
		}
		String prefix = (name + ".").toLowerCase(Locale.ROOT);
		for(MbPostFile f : this.files) {
			if(f == primary) {
				continue;
			}
			if(f.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
				result.add(f);
			}
		}
		return result;
	}

	private void downloadAndOpen(MbPostFile primary, List<MbPostFile> companions, FileKind kind) {
		GuiManager gui = GuiManager.getInstance();
		ProgressDialog dialog = new ProgressDialog(
			gui.getMainStage(),
			"Downloading " + primary.getName(),
			"Preparing..."
		);
		dialog.show();

		final String location = this.location();
		final Path destDir = downloadDir(location);

		Task<Path> task = new Task<Path>() {
			@Override
			protected Path call() throws Exception {
				MbPostClient client = new MbPostClient();
				List<MbPostFile> queue = new ArrayList<MbPostFile>();
				queue.add(primary);
				queue.addAll(companions);
				Path primaryPath = null;
				int idx = 0;
				for(MbPostFile f : queue) {
					idx++;
					Path dest = destDir.resolve(f.getName());
					final long fileSize = f.getSize();
					final String label = (queue.size() > 1
						? "[" + idx + "/" + queue.size() + "] " : "")
						+ "Downloading " + f.getName();
					updateMessage(label);
					if(Files.exists(dest) && Files.size(dest) == fileSize && fileSize > 0L) {
						dialog.setProgress(1.0);
					}
					else {
						client.download(location, f.getName(), dest, bytes -> {
							if(fileSize > 0L) {
								dialog.setProgress((double) bytes / (double) fileSize);
							}
							else {
								dialog.setProgress(-1.0);
							}
						});
					}
					if(f == primary) {
						primaryPath = dest;
					}
				}
				return primaryPath;
			}
		};
		task.messageProperty().addListener((obs, oldV, newV) -> dialog.setStatus(newV));
		task.setOnSucceeded(e -> {
			dialog.close();
			Path local = task.getValue();
			if(kind == FileKind.MS_DATA) {
				openMsData(local.toFile());
			}
			else {
				openExternal(local.toFile());
			}
		});
		task.setOnFailed(e -> {
			dialog.close();
			showError("Download failed", task.getException());
		});
		Thread t = new Thread(task, "mb-post-download");
		t.setDaemon(true);
		t.start();
	}

	private static Path downloadDir(String location) {
		File configFolder = MsppManager.getInstance().getConfigFolder();
		return new File(configFolder, "mb-post" + File.separator + location).toPath();
	}

	private static void openMsData(File file) {
		GuiManager gui = GuiManager.getInstance();
		MsppManager manager = MsppManager.getInstance();
		ProgressDialog dialog = new ProgressDialog(
			gui.getMainStage(),
			"Opening " + file.getName(),
			"Preparing..."
		);
		dialog.show();

		Task<Sample> task = new Task<Sample>() {
			@Override
			protected Sample call() throws Exception {
				updateMessage("Reading " + file.getName() + "...");
				MzmlReader reader = new MzmlReader();
				return reader.read(
					file.getAbsolutePath(),
					p -> dialog.setProgress(p),
					s -> updateMessage(s)
				);
			}
		};
		task.messageProperty().addListener((obs, oldV, newV) -> dialog.setStatus(newV));
		task.setOnSucceeded(e -> {
			dialog.close();
			Sample sample = task.getValue();
			if(sample.getChromatograms().isEmpty()) {
				sample.getChromatograms().add(new TicChromatogram(sample));
			}
			manager.invoke(OnOpenSample.class, sample);
		});
		task.setOnFailed(e -> {
			dialog.close();
			showError("Failed to open " + file.getName(), task.getException());
		});
		Thread t = new Thread(task, "mb-post-open");
		t.setDaemon(true);
		t.start();
	}

	private static void openExternal(File file) {
		new Thread(() -> {
			try {
				if(!Desktop.isDesktopSupported()) {
					Platform.runLater(() -> showError(
						"Cannot open file",
						new IOException("Desktop API is not supported on this platform")));
					return;
				}
				Desktop.getDesktop().open(file);
			}
			catch(IOException ex) {
				Platform.runLater(() -> showError("Failed to open " + file.getName(), ex));
			}
		}, "mb-post-external-open").start();
	}

	// ================================ HELPERS ================================

	private static void showError(String header, Throwable t) {
		Runnable r = () -> {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("MB-POST");
			alert.setHeaderText(header);
			alert.setContentText(t == null ? null : t.getMessage());
			alert.showAndWait();
		};
		if(Platform.isFxApplicationThread()) {
			r.run();
		}
		else {
			Platform.runLater(r);
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s;
	}

	private static String formatSize(long size) {
		if(size < 1024L) {
			return size + " B";
		}
		double v = size;
		String[] units = { "KB", "MB", "GB", "TB" };
		int i = -1;
		do {
			v /= 1024.0;
			i++;
		} while(v >= 1024.0 && i < units.length - 1);
		return String.format("%.1f %s", v, units[i]);
	}

	private enum FileKind {
		MS_DATA, EXTERNAL, UNSUPPORTED;

		static FileKind classify(String name) {
			if(name == null) {
				return UNSUPPORTED;
			}
			String n = name.toLowerCase(Locale.ROOT);
			if(n.endsWith(".mzml") || n.endsWith(".raw") || n.endsWith(".wiff") || n.endsWith(".lcd")) {
				return MS_DATA;
			}
			if(n.endsWith(".txt") || n.endsWith(".xlsx") || n.endsWith(".csv")) {
				return EXTERNAL;
			}
			return UNSUPPORTED;
		}
	}

	private class ActionCell extends TableCell<MbPostFile, MbPostFile> {
		private final Button button = new Button("Open");

		ActionCell() {
			this.button.setOnAction(e -> {
				MbPostFile item = this.getItem();
				if(item != null) {
					MbPostDialog.this.onOpen(item);
				}
			});
		}

		@Override
		protected void updateItem(MbPostFile item, boolean empty) {
			super.updateItem(item, empty);
			if(empty || item == null) {
				this.setGraphic(null);
				return;
			}
			FileKind kind = FileKind.classify(item.getName());
			this.setGraphic(kind == FileKind.UNSUPPORTED ? null : this.button);
		}
	}
}
