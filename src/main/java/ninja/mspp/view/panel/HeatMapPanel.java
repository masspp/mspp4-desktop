package ninja.mspp.view.panel;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import ninja.mspp.core.model.view.ColorTheme;

public class HeatMapPanel implements Initializable {	
	private HeatMapCanvas canvas;
	
	@FXML
	private BorderPane pane;
	
	@FXML
	private ComboBox<ColorTheme> combo;
	
	public HeatMapCanvas getCanvas() {
		return this.canvas;
	}
	
	public BorderPane getPane() {
		return this.pane;
	}
	
	@FXML
	private void onSelectTheme(ActionEvent event) {
		ColorTheme theme = this.combo.getSelectionModel().getSelectedItem();
		this.canvas.setTheme(theme);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.canvas = new HeatMapCanvas();
		this.pane.setCenter(this.canvas);
		this.pane.widthProperty().addListener((observable, oldValue, newValue) -> {
			this.canvas.setWidth(newValue.doubleValue());
		});
		this.pane.heightProperty().addListener((observable, oldValue, newValue) -> {
			this.canvas.setHeight(newValue.doubleValue() - 50.0);
		});
		
		for (ColorTheme theme : ColorTheme.getThemes()) {
			this.combo.getItems().add(theme);
		}
		this.combo.getSelectionModel().select(0);
		this.onSelectTheme(null);
	}
}
