package ninja.mspp.view.mode.normal;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import ninja.mspp.MsppManager;
import ninja.mspp.core.model.ms.Chromatogram;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.core.view.ViewInfo;
import ninja.mspp.view.panel.ChromatogramCanvas;
import ninja.mspp.view.panel.HeatMapPanel;
import ninja.mspp.view.panel.SpectrumCanvas;
import ninja.mspp.view.panel.ThreeDPanel;
import ninja.mspp.view.part.table.chromatogram.ChromatogramTableManager;
import ninja.mspp.view.part.table.sample.SampleTableManager;
import ninja.mspp.view.part.table.spectrum.SpectrumTableManager;

public class NormalViewMode implements Initializable {
	@FXML
	private SplitPane tablePane;
	
	@FXML
	private BorderPane spectrumPane;
	
	@FXML
	private BorderPane chromatogramPane;
	
	@FXML
	private BorderPane heatmapPane;
	
	@FXML
	private BorderPane threeDPane;
	

	@Override
	public void initialize(URL location, ResourceBundle resources){
		NormalViewModeManager manager = NormalViewModeManager.getInstance();
		
		TableView<Sample> sampleTable = SampleTableManager.getInstance().createTableView();
		this.tablePane.getItems().add(sampleTable);
		
		TableView<Chromatogram> chromatogramTable = ChromatogramTableManager.getInstance().createTableView();
		this.tablePane.getItems().add(chromatogramTable);
		
		TableView<Spectrum> spectrumTable = SpectrumTableManager.getInstance().createTableView();
		this.tablePane.getItems().add(spectrumTable);
		
		this.tablePane.setDividerPositions(0.2, 0.4, 1.0);
		
		SpectrumCanvas spectrumCanvas = new SpectrumCanvas();
		this.spectrumPane.setCenter(spectrumCanvas);
		manager.setSpectrumCanvas(spectrumCanvas);
		this.spectrumPane.widthProperty().addListener(
			(observable, oldVal, newVal) -> {
				spectrumCanvas.setWidth(newVal.doubleValue());
			}
		);
		this.spectrumPane.heightProperty().addListener(
			(observable, oldVal, newVal) -> {
				spectrumCanvas.setHeight(newVal.doubleValue());
			}
		);
		
		ChromatogramCanvas chromatogramCanvas = new ChromatogramCanvas();
		this.chromatogramPane.setCenter(chromatogramCanvas);
		manager.setChromatogramCanvas(chromatogramCanvas);
		this.chromatogramPane.widthProperty().addListener(
			(observable, oldVal, newVal) -> {
				chromatogramCanvas.setWidth(newVal.doubleValue());
			}
		);
		this.chromatogramPane.heightProperty().addListener(
			(observable, oldVal, newVal) -> {
				chromatogramCanvas.setHeight(newVal.doubleValue());
			}
		);
		
		MsppManager msppManager = MsppManager.getInstance();
		
		try {
			ViewInfo<HeatMapPanel> heatmapInfo = msppManager.createWindow(HeatMapPanel.class, "HeatMapPanel.fxml");
			this.heatmapPane.setCenter(heatmapInfo.getWindow());
			manager.setHeatMapCanvas(heatmapInfo.getController().getCanvas());
			this.heatmapPane.widthProperty().addListener(
				(observable, oldVal, newVal) -> {
					heatmapInfo.getController().getPane().setPrefWidth(newVal.doubleValue());
				}
			);
			this.heatmapPane.heightProperty().addListener(
				(observable, oldVal, newVal) -> {
					heatmapInfo.getController().getPane().setPrefHeight(newVal.doubleValue());
				}
			);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		try {
			ViewInfo<ThreeDPanel> threeDInfo = msppManager.createWindow(ThreeDPanel.class, "ThreeDPanel.fxml");
			this.threeDPane.setCenter(threeDInfo.getWindow());
			manager.setThreeDPanel(threeDInfo.getController());
			this.threeDPane.widthProperty().addListener(
				(observable, oldVal, newVal) -> {
					threeDInfo.getController().getPane().setPrefWidth(newVal.doubleValue());
				}
			);
			this.threeDPane.heightProperty().addListener(
				(observable, oldVal, newVal) -> {
					threeDInfo.getController().getPane().setPrefHeight(newVal.doubleValue());
				}
			);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
