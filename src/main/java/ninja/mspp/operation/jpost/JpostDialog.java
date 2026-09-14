package ninja.mspp.operation.jpost;

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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.method.OnOpenSample;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.TicChromatogram;
import ninja.mspp.io.mzml.MzmlReader;
import ninja.mspp.io.mzml.ui.ProgressDialog;
import ninja.mspp.operation.jpost.JpostClient.ProjectPage;
import ninja.mspp.operation.jpost.model.JpostFile;
import ninja.mspp.operation.jpost.model.JpostFile.Preset;
import ninja.mspp.operation.jpost.model.JpostProject;
import ninja.mspp.view.GuiManager;

public class JpostDialog {

	private static final int PAGE_SIZE = 50;

	@FXML private StackPane rootPane;

	// List view
	@FXML private VBox listView;
	@FXML private TextField searchField;
	@FXML private Button searchButton;
	@FXML private TableView<JpostProject> projectTable;
	@FXML private TableColumn<JpostProject, String> projIdColumn;
	@FXML private TableColumn<JpostProject, String> projTitleColumn;
	@FXML private TableColumn<JpostProject, String> projPiColumn;
	@FXML private TableColumn<JpostProject, String> projPxColumn;
	@FXML private TableColumn<JpostProject, String> projDateColumn;
	@FXML private TableColumn<JpostProject, JpostProject> projActionColumn;
	@FXML private ScrollPane projectDetailScroll;
	@FXML private VBox projectDetailContent;
	@FXML private Label projectDetailPlaceholder;
	@FXML private Button firstButton;
	@FXML private Button prevButton;
	@FXML private Button nextButton;
	@FXML private Button lastButton;
	@FXML private Label pageLabel;
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
	@FXML private TableView<JpostFile> filesTable;
	@FXML private TableColumn<JpostFile, String> nameColumn;
	@FXML private TableColumn<JpostFile, String> typeColumn;
	@FXML private TableColumn<JpostFile, Long> sizeColumn;
	@FXML private TableColumn<JpostFile, JpostFile> actionColumn;
	@FXML private ScrollPane detailScroll;
	@FXML private VBox detailContent;
	@FXML private Label detailPlaceholder;
	@FXML private Label fileStatusLabel;

	private final ObservableList<JpostProject> projects = FXCollections.observableArrayList();
	private final ObservableList<JpostFile> files = FXCollections.observableArrayList();
	private JpostProject currentProject;

	private int pageIndex;
	private int totalProjects;
	private String currentKeyword = "";
	private long pageRequestId;

	@FXML
	private void initialize() {
		this.initListView();
		this.initDetailView();
		this.loadPage();
	}

	// =============================== LIST VIEW ===============================

	private void initListView() {
		this.projIdColumn.setCellValueFactory(new PropertyValueFactory<>("jpostId"));
		this.projTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
		this.projPiColumn.setCellValueFactory(new PropertyValueFactory<>("principalInvestigator"));
		this.projPxColumn.setCellValueFactory(new PropertyValueFactory<>("pxid"));
		this.projDateColumn.setCellValueFactory(new PropertyValueFactory<>("announcementDate"));
		this.projActionColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
		this.projActionColumn.setCellFactory(col -> new ProjectActionCell());

		this.projectTable.setItems(this.projects);

		this.searchField.setOnAction(e -> this.onSearch());

		this.projectTable.getSelectionModel().selectedItemProperty().addListener(
			(obs, oldV, newV) -> this.renderProjectDetail(newV));
	}

	private void renderProjectDetail(JpostProject p) {
		this.projectDetailContent.getChildren().clear();
		if(p == null) {
			this.projectDetailContent.getChildren().add(
				new Label("Select a project to view its details."));
			return;
		}
		this.projectDetailContent.getChildren().add(sectionHeader(safe(p.getJpostId())));
		this.projectDetailContent.getChildren().add(infoGrid(new String[][] {
			{ "Title", safe(p.getTitle()) },
			{ "PI", safe(p.getPrincipalInvestigator()) },
			{ "Affiliation", safe(p.getAffiliation()) },
			{ "Submitter", safe(p.getSubmitter()) },
			{ "Keywords", safe(p.getKeywords()) },
			{ "Description", safe(p.getDescription()) },
			{ "Status", safe(p.getStatus()) },
			{ "Type", safe(p.getType()) },
			{ "PX ID", safe(p.getPxid()) },
			{ "Announcement", safe(p.getAnnouncementDate()) },
			{ "Created", safe(p.getCreatedDate()) },
			{ "Modified", safe(p.getModifiedDate()) },
			{ "PubMed", safe(p.getPmid()) },
			{ "Note", safe(p.getNote()) }
		}));
	}

	private class ProjectActionCell extends TableCell<JpostProject, JpostProject> {
		private final Button button = new Button("Open");

		ProjectActionCell() {
			this.button.setOnAction(e -> {
				JpostProject item = this.getItem();
				if(item != null) {
					JpostDialog.this.showDetail(item);
				}
			});
		}

		@Override
		protected void updateItem(JpostProject item, boolean empty) {
			super.updateItem(item, empty);
			this.setGraphic(empty || item == null ? null : this.button);
		}
	}

	// ------------------------------- paging ---------------------------------

	@FXML
	private void onSearch() {
		this.currentKeyword = this.searchField.getText() == null
			? "" : this.searchField.getText().trim();
		this.pageIndex = 0;
		this.loadPage();
	}

	@FXML
	private void onFirstPage() {
		if(this.pageIndex != 0) {
			this.pageIndex = 0;
			this.loadPage();
		}
	}

	@FXML
	private void onPrevPage() {
		if(this.pageIndex > 0) {
			this.pageIndex--;
			this.loadPage();
		}
	}

	@FXML
	private void onNextPage() {
		if(this.pageIndex < this.lastPageIndex()) {
			this.pageIndex++;
			this.loadPage();
		}
	}

	@FXML
	private void onLastPage() {
		int last = this.lastPageIndex();
		if(this.pageIndex != last) {
			this.pageIndex = last;
			this.loadPage();
		}
	}

	private int lastPageIndex() {
		if(this.totalProjects <= 0) {
			return 0;
		}
		return (this.totalProjects - 1) / PAGE_SIZE;
	}

	private void loadPage() {
		final long requestId = ++this.pageRequestId;
		this.setPagerDisabled(true);
		this.listStatusLabel.setText("Loading projects...");
		final JpostClient client = new JpostClient();
		final int offset = this.pageIndex * PAGE_SIZE;
		final String keyword = this.currentKeyword;

		Task<ProjectPage> task = new Task<ProjectPage>() {
			@Override
			protected ProjectPage call() throws Exception {
				return client.listProjects(offset, PAGE_SIZE, keyword);
			}
		};
		task.setOnSucceeded(e -> {
			if(requestId != this.pageRequestId) {
				return;
			}
			ProjectPage page = task.getValue();
			this.totalProjects = page.total;
			this.projects.setAll(page.projects);
			if(!this.projects.isEmpty()) {
				this.projectTable.getSelectionModel().select(0);
			}
			this.updatePager();
		});
		task.setOnFailed(e -> {
			if(requestId != this.pageRequestId) {
				return;
			}
			this.listStatusLabel.setText("Failed to load projects");
			this.setPagerDisabled(true);
			showError("Failed to load projects", task.getException());
		});
		Thread t = new Thread(task, "jpost-projects");
		t.setDaemon(true);
		t.start();
	}

	private void updatePager() {
		int totalPages = Math.max(1, this.lastPageIndex() + 1);
		this.pageLabel.setText("Page " + (this.pageIndex + 1) + " / " + totalPages);

		String scope = this.currentKeyword.isEmpty()
			? this.totalProjects + " projects"
			: this.totalProjects + " projects matching \"" + this.currentKeyword + "\"";
		int from = this.projects.isEmpty() ? 0 : this.pageIndex * PAGE_SIZE + 1;
		int to = this.pageIndex * PAGE_SIZE + this.projects.size();
		this.listStatusLabel.setText(this.projects.isEmpty()
			? scope
			: from + " - " + to + " of " + scope);

		this.firstButton.setDisable(this.pageIndex <= 0);
		this.prevButton.setDisable(this.pageIndex <= 0);
		this.nextButton.setDisable(this.pageIndex >= this.lastPageIndex());
		this.lastButton.setDisable(this.pageIndex >= this.lastPageIndex());
	}

	private void setPagerDisabled(boolean disabled) {
		this.firstButton.setDisable(disabled);
		this.prevButton.setDisable(disabled);
		this.nextButton.setDisable(disabled);
		this.lastButton.setDisable(disabled);
	}

	// ============================= DETAIL VIEW ===============================

	private void initDetailView() {
		this.nameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
		this.typeColumn.setCellValueFactory(new PropertyValueFactory<>("fileType"));
		this.sizeColumn.setCellValueFactory(new PropertyValueFactory<>("sizeBytes"));
		this.sizeColumn.setCellFactory(col -> new TableCell<JpostFile, Long>() {
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
			(obs, oldV, newV) -> this.renderFileDetail(newV));
	}

	private void showDetail(JpostProject project) {
		this.currentProject = project;
		this.detailTitleLabel.setText(project.getJpostId() + " : " + safe(project.getTitle()));
		this.idLabel.setText(safe(project.getJpostId()));
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
		return this.currentProject.getJpostId();
	}

	private void loadFiles() {
		final JpostClient client = new JpostClient();
		final String location = this.location();
		final JpostProject pinned = this.currentProject;

		Task<List<JpostFile>> task = new Task<List<JpostFile>>() {
			@Override
			protected List<JpostFile> call() throws Exception {
				return client.listAllFiles(location, 0,
					fetched -> updateMessage("Loaded " + fetched + " files..."));
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
		Thread t = new Thread(task, "jpost-files");
		t.setDaemon(true);
		t.start();
	}

	// =========================== FILE DETAIL PANE ============================

	private void clearFileDetail(String placeholder) {
		this.detailContent.getChildren().clear();
		this.detailContent.getChildren().add(new Label(placeholder));
	}

	private void renderFileDetail(JpostFile file) {
		this.detailContent.getChildren().clear();
		if(file == null) {
			this.detailContent.getChildren().add(new Label("Select a file to view its details."));
			return;
		}

		this.detailContent.getChildren().add(sectionHeader(safe(file.getFileName())));
		this.detailContent.getChildren().add(infoGrid(new String[][] {
			{ "Type", safe(file.getFileType()) },
			{ "Size", formatSize(file.getSizeBytes()) },
			{ "Status", safe(file.getStatus()) },
			{ "Archived", file.isArchived() ? "Yes" : "No" },
			{ "Checksum", safe(file.getChecksum()) }
		}));

		int index = 0;
		List<Preset> presets = file.getElements();
		for(Preset p : presets) {
			List<String[]> rows = new ArrayList<String[]>();
			addRow(rows, "Sample", p.getSampleTitle());
			addRow(rows, "Fractionation", p.getFractionationTitle());
			addRow(rows, "Enzyme / Mod.", p.getEnzymeModTitle());
			addRow(rows, "MS mode", p.getMsModeTitle());
			addRow(rows, "Software", p.getSoftware());
			addRow(rows, "Format", p.getFormat());
			addRow(rows, "Label", p.getLabelText());
			if(rows.isEmpty()) {
				continue;
			}
			index++;
			String header = presets.size() > 1 ? "Preset " + index : "Experimental presets";
			this.detailContent.getChildren().add(sectionHeader(header));
			this.detailContent.getChildren().add(infoGrid(rows.toArray(new String[0][])));
		}
	}

	private static void addRow(List<String[]> rows, String label, String value) {
		if(value != null && !value.isEmpty()) {
			rows.add(new String[] { label, value });
		}
	}

	private static Label sectionHeader(String text) {
		Label l = new Label(text);
		l.setWrapText(true);
		l.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4 0 2 0;");
		return l;
	}

	/**
	 * Two-column key/value grid. Value labels always wrap and are never clipped, so
	 * long descriptions / affiliations are shown in full (never truncated to "...").
	 */
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
		c1.setFillWidth(true);
		g.getColumnConstraints().addAll(c0, c1);
		for(int i = 0; i < rows.length; i++) {
			Label k = new Label(rows[i][0]);
			k.setStyle("-fx-text-fill: #555;");
			k.setMinHeight(Region.USE_PREF_SIZE);
			Label v = new Label(rows[i][1] == null ? "" : rows[i][1]);
			v.setWrapText(true);
			v.setMaxWidth(Double.MAX_VALUE);
			v.setMinHeight(Region.USE_PREF_SIZE);
			g.add(k, 0, i);
			g.add(v, 1, i);
		}
		return g;
	}

	// ============================ FILE OPEN FLOW =============================
	// Opening follows the MB-POST behaviour: MS data files are opened in mspp via
	// the mzML reader, companion files (e.g. *.wiff.scan) are downloaded alongside
	// the primary file, and other supported documents are opened externally.

	private void onOpen(JpostFile file) {
		FileKind kind = FileKind.classify(file.getFileName());
		if(kind == FileKind.UNSUPPORTED) {
			return;
		}
		List<JpostFile> companions = this.findCompanions(file);
		this.downloadAndOpen(file, companions, kind);
	}

	private List<JpostFile> findCompanions(JpostFile primary) {
		List<JpostFile> result = new ArrayList<JpostFile>();
		String name = primary.getFileName();
		String lower = name.toLowerCase(Locale.ROOT);
		if(!lower.endsWith(".wiff")) {
			return result;
		}
		String prefix = (name + ".").toLowerCase(Locale.ROOT);
		for(JpostFile f : this.files) {
			if(f == primary) {
				continue;
			}
			if(f.getFileName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
				result.add(f);
			}
		}
		return result;
	}

	private void downloadAndOpen(JpostFile primary, List<JpostFile> companions, FileKind kind) {
		GuiManager gui = GuiManager.getInstance();
		ProgressDialog dialog = new ProgressDialog(
			gui.getMainStage(),
			"Downloading " + primary.getFileName(),
			"Preparing..."
		);
		dialog.show();

		final String location = this.location();
		final Path destDir = downloadDir(location);

		Task<Path> task = new Task<Path>() {
			@Override
			protected Path call() throws Exception {
				JpostClient client = new JpostClient();
				List<JpostFile> queue = new ArrayList<JpostFile>();
				queue.add(primary);
				queue.addAll(companions);
				Path primaryPath = null;
				int idx = 0;
				for(JpostFile f : queue) {
					idx++;
					Path dest = destDir.resolve(f.getFileName());
					final long fileSize = f.getSizeBytes();
					final String label = (queue.size() > 1
						? "[" + idx + "/" + queue.size() + "] " : "")
						+ "Downloading " + f.getFileName();
					updateMessage(label);
					if(Files.exists(dest) && Files.size(dest) == fileSize && fileSize > 0L) {
						dialog.setProgress(1.0);
					}
					else {
						client.download(f, dest, bytes -> {
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
		Thread t = new Thread(task, "jpost-download");
		t.setDaemon(true);
		t.start();
	}

	private static Path downloadDir(String location) {
		File configFolder = MsppManager.getInstance().getConfigFolder();
		return new File(configFolder, "jpost" + File.separator + location).toPath();
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
		Thread t = new Thread(task, "jpost-open");
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
		}, "jpost-external-open").start();
	}

	// ================================ HELPERS ================================

	private static void showError(String header, Throwable t) {
		Runnable r = () -> {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("jPOST");
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

	private class ActionCell extends TableCell<JpostFile, JpostFile> {
		private final Button button = new Button("Open");

		ActionCell() {
			this.button.setOnAction(e -> {
				JpostFile item = this.getItem();
				if(item != null) {
					JpostDialog.this.onOpen(item);
				}
			});
		}

		@Override
		protected void updateItem(JpostFile item, boolean empty) {
			super.updateItem(item, empty);
			if(empty || item == null) {
				this.setGraphic(null);
				return;
			}
			FileKind kind = FileKind.classify(item.getFileName());
			this.setGraphic(kind == FileKind.UNSUPPORTED ? null : this.button);
		}
	}
}
