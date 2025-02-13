package ninja.mspp.view.mode.mirror;

import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import ninja.mspp.core.model.ms.Chromatogram;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.view.part.table.chromatogram.ChromatogramTableManager;
import ninja.mspp.view.part.table.sample.SampleTableManager;
import ninja.mspp.view.part.table.spectrum.SpectrumTableManager;

public class MirrorViewMode implements Initializable {
	@FXML
	private BorderPane pane;
	
	@FXML
	private BorderPane spectrumTablePane;
	
	@FXML
	private BorderPane chromatogramTablePane;
	
	@FXML
	private BorderPane sampleTablePane;
	
	@FXML
	private BorderPane spectrumPane;
	
	@FXML
	private BorderPane chromatogramPane;
	
	@FXML
	private ToggleButton spectrumUpButton;
	
	@FXML
	private ToggleButton spectrumDownButton;
	
	@FXML
	private ToggleButton chromatogramUpButton;
	
	@FXML
	private ToggleButton chromatogramDownButton;
	
	@FXML
	private Label spectrumUpLabel;
	
	@FXML
	private Label spectrumDownLabel;
	
	@FXML
	private Label chromatogramUpLabel;
	
	@FXML
	private Label chromatogramDownLabel;
	
	@FXML
	private TabPane tabs;
	
	@FXML
	private Tab spectrumTab;
	
	@FXML
	private Tab chromatogramTab;
	
	@FXML
	private void onSpectrumUp(ActionEvent event) {
		this.spectrumUpButton.setSelected(true);
		this.spectrumDownButton.setSelected(false);
		
		MirrorViewManager manager = MirrorViewManager.getInstance();
		manager.setSpectrumUp(true);
	}

	@FXML
	private void onSpectrumDown(ActionEvent event) {
		this.spectrumUpButton.setSelected(false);
		this.spectrumDownButton.setSelected(true);		
		
		MirrorViewManager manager = MirrorViewManager.getInstance();
		manager.setSpectrumUp(false);
	}
	
	@FXML
	private void onChromatogramUp(ActionEvent event) {
		this.chromatogramUpButton.setSelected(true);
		this.chromatogramDownButton.setSelected(false);
		
		MirrorViewManager manager = MirrorViewManager.getInstance();
		manager.setChromatogramUp(true);
	}
	
	@FXML
	private void onChromatogramDown(ActionEvent event) {
		this.chromatogramUpButton.setSelected(false);
		this.chromatogramDownButton.setSelected(true);
		
		MirrorViewManager manager = MirrorViewManager.getInstance();
		manager.setChromatogramUp(false);
	}
	
	private void setIcons() {
		InputStream upStream = this.getClass().getResourceAsStream("/ninja/mspp/images/icon/button/up-arrow_256.png");
		Image upImage = new Image(upStream);
		
		ImageView chromatogramUpView = new ImageView(upImage);
		chromatogramUpView.setFitHeight(12.0);
		chromatogramUpView.setFitWidth(12.0);
		this.chromatogramUpButton.setText("");
		this.chromatogramUpButton.setGraphic(chromatogramUpView);

		ImageView spectrumUpView = new ImageView(upImage);
		spectrumUpView.setFitHeight(12.0);
		spectrumUpView.setFitWidth(12.0);		
		this.spectrumUpButton.setText("");
		this.spectrumUpButton.setGraphic(spectrumUpView);
		
		InputStream downStream = this.getClass().getResourceAsStream("/ninja/mspp/images/icon/button/down-arrow_256.png");
		Image downImage = new Image(downStream);
		
		ImageView chromatogramDownView = new ImageView(downImage);
		chromatogramDownView.setFitHeight(12.0);
		chromatogramDownView.setFitWidth(12.0);
		this.chromatogramDownButton.setText("");
		this.chromatogramDownButton.setGraphic(chromatogramDownView);

		ImageView spectrumDownView = new ImageView(downImage);
		spectrumDownView.setFitHeight(12.0);
		spectrumDownView.setFitWidth(12.0);		
		this.spectrumDownButton.setText("");
		this.spectrumDownButton.setGraphic(spectrumDownView);		
	}
	
	
	private void setTables() {
        TableView<Sample> sampleTable = SampleTableManager.getInstance().createTableView();
        this.sampleTablePane.setCenter(sampleTable);
        
        TableView<Chromatogram> chromatogramTable = ChromatogramTableManager.getInstance().createTableView();
        this.chromatogramTablePane.setCenter(chromatogramTable);
        
        TableView<Spectrum> spectrumTable = SpectrumTableManager.getInstance().createTableView();
        this.spectrumTablePane.setCenter(spectrumTable);		
	}
	
	public void setSpectrumUpLabel(String label) {
		this.spectrumUpLabel.setText(label);
	}
	
	public void setSpectrumDownLabel(String label) {
		this.spectrumDownLabel.setText(label);
	}
	
	public void setChromatogramUpLabel(String label) {
		this.chromatogramUpLabel.setText(label);
	}
	
	public void setChromatogramDownLabel(String label) {
		this.chromatogramDownLabel.setText(label);
	}
	
	public void openChromatogramTab() {
		this.tabs.getSelectionModel().select(this.chromatogramTab);
	}
	
	public void openSpectrumTab() {
		this.tabs.getSelectionModel().select(this.spectrumTab);
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setIcons();
		setTables();
		
		MirrorViewManager manager = MirrorViewManager.getInstance();
		
		MirrorChromatogramCanvas chromatogramCanvas = new MirrorChromatogramCanvas();
		this.chromatogramPane.setCenter(chromatogramCanvas);
		manager.setChromatogramCanvas(chromatogramCanvas);
		this.chromatogramPane.widthProperty().addListener(
			(ov, oldVal, newVal) -> {
				chromatogramCanvas.widthProperty().set(newVal.doubleValue());
			}
		);
		this.chromatogramPane.heightProperty().addListener(
			(ov, oldVal, newVal) -> {
				chromatogramCanvas.heightProperty().set(newVal.doubleValue());
			}
		);
		
		MirrorSpectrumCanvas spectrumCanvas = new MirrorSpectrumCanvas();
		this.spectrumPane.setCenter(spectrumCanvas);
		manager.setSpectrumCanvas(spectrumCanvas);
		this.spectrumPane.widthProperty().addListener(
			(ov, oldVal, newVal) -> {
				spectrumCanvas.widthProperty().set(newVal.doubleValue());
			}
		);
		this.spectrumPane.heightProperty().addListener(
            (ov, oldVal, newVal) -> {
                spectrumCanvas.heightProperty().set(newVal.doubleValue());
            }
        );
		
		this.chromatogramUpButton.setSelected(true);
		this.chromatogramDownButton.setSelected(false);
		manager.setChromatogramUp(true);
		
		this.spectrumUpButton.setSelected(true);
		this.spectrumDownButton.setSelected(false);
		manager.setSpectrumUp(true);
		
		manager.setController(this);
	}
}
