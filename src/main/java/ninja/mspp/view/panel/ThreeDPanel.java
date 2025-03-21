package ninja.mspp.view.panel;

import java.net.URL;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import ninja.mspp.core.model.view.ColorTheme;
import ninja.mspp.core.model.view.HeatMap;

public class ThreeDPanel implements Initializable {
	enum HAlign {
		LEFT, CENTER, RIGHT
	}
	
	enum VAlign {
		TOP, CENTER, BOTTOM
	}
	
	private class TextInfo {
		public Text text;
		public double x;
		public double y;
		public double z;
		public HAlign hAlign;
		public VAlign vAlign;
	}
	
	private HeatMap heatmap;
	
	private double scale;
	private Group root;
	private Group group;
	
	private static final double DEFAULT_Z_POSITION = 200.0;
	private static final double MIN_Z_POSITION     = -50.0;
	private static final double MAX_Z_POSITION     = 400;
	private static final double AXIS_PERCENTAGE    = 10.0;
	private static final double MEASURE_DENSITY    = 3.0;
	private static final double MEASURE_LENGTH     = 10.0;
	
	private double px;
	private double py;
	private double xAngle;
	private double zAngle;
	
	private List<TextInfo> textArray;
	
	@FXML
	private BorderPane pane;
	
	@FXML
	private Slider slider;
	
	@FXML
	private ChoiceBox<ColorTheme> themeChoice;
	
	@FXML
	private CheckBox axisCheck;
	
	@FXML
	private CheckBox rtCheck;
	
	@FXML
	private CheckBox mzCheck;
	
	@FXML
	private CheckBox intCheck;
	
	private void onSize(Group group, PerspectiveCamera camera, double width, double height) {
		group.getTransforms().clear();
		double scale = height / 100.0;
		this.scale = scale;
		
		group.setScaleX(scale);
		group.setScaleY(scale);
		group.setScaleZ(scale);
		group.setTranslateX(width / 2.0);
		group.setTranslateY(height / 2.0);
		group.setTranslateZ(scale * DEFAULT_Z_POSITION);
		
		this.slider.setValue(DEFAULT_Z_POSITION);
	}
	
	public BorderPane getPane() {
		return this.pane;
	}
	
	private void createHeatmap(HeatMap heatmap, Group group) {
		Image image = this.createTextureImage();
		PhongMaterial material = new PhongMaterial();
		material.setDiffuseMap( image );
		material.setSpecularColor( Color.TRANSPARENT );
		material.setSpecularPower( 0.0 );

		TriangleMesh mesh = new TriangleMesh();

		int rtCount = HeatMap.RT_SIZE;
		int mzCount = HeatMap.MZ_SIZE;

		double[][] data = heatmap.getData();

		for(int i = 0; i <= 2000; i++) {
			mesh.getTexCoords().addAll((float)((double)i / 2000.0), 0.0f);
		}

		List<Integer> textures = new ArrayList<Integer>();

		for(int i = 0; i < rtCount; i++) {
			double x = ((double)i / (double)rtCount - 0.5) * 200.0;
			for(int j = 0; j < mzCount; j++) {
				double y = ((double)(mzCount - 1 - j) / (double)mzCount - 0.5) * 200.0;
				double intensity = data[ i ][ j ];

				double z = - 50.0 * intensity;

				mesh.getPoints().addAll((float)x, (float)y, (float)z);

				int texture = (int)Math.round(Math.sqrt(intensity) * 1000.0 + 500.0);
				textures.add(texture);
			}
		}

		for(int i = 0; i < rtCount - 1; i++) {
			for(int j = 0; j < mzCount - 1; j++) {
				int index0 = i * mzCount + j;
				int index1 = index0 + 1;
				int index2 = (i + 1) * mzCount + j;
				int index3 = index2 + 1;

				int texture0 = textures.get(index0);
				int texture1 = textures.get(index1);
				int texture2 = textures.get(index2);
				int texture3 = textures.get(index3);

				mesh.getFaces().addAll(
					index2, texture2, index1, texture1, index0, texture0,
					index1, texture1, index2, texture2, index3, texture3
				);
			}
		}

		MeshView view = new MeshView();
		view.setMaterial(material);
		view.setMesh(mesh);

		group.getChildren().add(view);
	}
	
	private PhongMaterial createAxisMaterial(String color) {
		PhongMaterial material = new PhongMaterial();
		material.setDiffuseColor(Color.web(color));
		material.setSpecularColor(Color.WHITE);
		return material;
	}

	private void addAxes(HeatMap heatmap, Group group) {
		if(this.axisCheck.isSelected()) {
			this.addXAxis(heatmap, group);
			this.addYAxis(heatmap, group);
			this.addZAxis(heatmap, group);
		}
	}

	private void addText(String string, double x, double y, double z, HAlign hAlign, VAlign vAlign) {
		Font font = new Font(10.0);

		Text text = new Text(string);
		text.setFont(font);
		text.setFill(Color.BLACK);
		text.setStroke(Color.TRANSPARENT);
		text.setCacheHint(CacheHint.QUALITY);
		group.getChildren().add(text);

		TextInfo info = new TextInfo();
		info.text = text;
		info.x = x;
		info.y = y;
		info.z = z;
		info.hAlign = hAlign;
		info.vAlign = vAlign;
		this.textArray.add(info);
	}

	private double getMeasureRange(double range) {
		double log = Math.log10(range / MEASURE_DENSITY);
		double base = Math.pow(10.0, Math.floor(log));
		double result = base;
		boolean flag = true;
		while(flag) {
			if(range / (result + base) > MEASURE_DENSITY) {
				result = result + base;
			}
			else {
				flag = false;
			}
		}
		return result;
	}

	private String getMeasureFormat(double range) {
		int log = (int)Math.floor(Math.log10(range / MEASURE_DENSITY));
		String format = "%.0f";
		if(log > 2 || log < -2) {
			format = "%.1g";
		}
		if(log < 0) {
			format = String.format("%%.%df", (-log));
		}
		return format;
	}

	private void addXAxis(HeatMap heatmap, Group group) {
		PhongMaterial material = this.createAxisMaterial("000050");
		double rate = (100.0 + AXIS_PERCENTAGE) / 100.0;
		Cylinder cylinder = new Cylinder(0.5, 200.0 * rate);
		cylinder.setMaterial(material);
		cylinder.setRotationAxis(new Point3D(0.0, 0.0, 1.0));
		cylinder.setRotate(90.0);
		cylinder.setTranslateY(100.0 * rate);
		group.getChildren().add(cylinder);

		this.addText("RT",  100.0 * rate + 10.0,  100.0 * rate, 0.0, HAlign.LEFT,  VAlign.CENTER);

		if(this.rtCheck.isSelected()) {
			double start = heatmap.getRtRange().getStart();
			double end = heatmap.getRtRange().getEnd();
			double range = this.getMeasureRange(end - start);
			String format = this.getMeasureFormat(end - start);

			int startIndex = (int)Math.ceil(start / range);
			int endIndex = (int)Math.floor(end / range);

			for(int i = startIndex; i <= endIndex; i++) {
				double value = range * (double)i;
				double pos = (value - start) / (end - start) * 200.0 - 100.0;
				this.addText(
					String.format(format, value),
					pos,
					100.0 * rate + MEASURE_LENGTH,
					0.0,
					HAlign.CENTER,
					VAlign.TOP
				);

				cylinder = new Cylinder(0.5, MEASURE_LENGTH);
				cylinder.setMaterial(material);
				cylinder.setTranslateX(pos);
				cylinder.setTranslateY(100.0 * rate + MEASURE_LENGTH / 2.0);
				group.getChildren().add(cylinder);
			}
		}
	}

	private void addYAxis(HeatMap heatmap, Group group) {
		PhongMaterial material = this.createAxisMaterial("500000");
		double rate = (100.0 + AXIS_PERCENTAGE) / 100.0;
		Cylinder cylinder = new Cylinder(0.5, 200.0 * rate);
		cylinder.setMaterial(material);
		cylinder.setTranslateX(-100.0 * rate);
		group.getChildren().add(cylinder);

		this.addText("m/z",  -100.0 * rate,  -100.0 * rate - 10.0, 0.0, HAlign.CENTER, VAlign.BOTTOM);

		if(this.mzCheck.isSelected()) {
			double start = heatmap.getMzRange().getStart();
			double end = heatmap.getMzRange().getEnd();
			double range = this.getMeasureRange(end - start);
			String format = this.getMeasureFormat(end - start);

			int startIndex = (int)Math.ceil(start / range);
			int endIndex = (int)Math.floor(end / range);

			for(int i = startIndex; i <= endIndex; i++) {
				double value = range * (double)i;
				double pos = - (value - start) / (end - start) * 200.0 + 100.0;
				this.addText(
					String.format(format, value),
					-100.0 * rate - MEASURE_LENGTH,
					pos,
					0.0,
					HAlign.RIGHT,
					VAlign.CENTER
				);

				cylinder = new Cylinder(0.5, MEASURE_LENGTH);
				cylinder.setMaterial(material);
				cylinder.setRotationAxis(new Point3D(0.0, 0.0, 1.0));
				cylinder.setRotate(90.0);
				cylinder.setTranslateX(- 100.0 * rate - MEASURE_LENGTH / 2.0);
				cylinder.setTranslateY(pos);
				group.getChildren().add(cylinder);
			}
		}
	}

	private void addZAxis(HeatMap heatmap, Group group) {
		PhongMaterial material = this.createAxisMaterial("005000");
		double rate = (100.0 + AXIS_PERCENTAGE) / 100.0;
		double length = 75.0 * rate;
		Cylinder cylinder = new Cylinder(0.5, length);
		cylinder.setMaterial(material);
		cylinder.setRotationAxis(new Point3D(1.0, 0.0, 0.0));
		cylinder.setRotate(90.0);
		cylinder.setTranslateX(-100.0 * rate);
		cylinder.setTranslateY(100.0 * rate);
		cylinder.setTranslateZ(- length / 2.0);
		group.getChildren().add(cylinder);

		this.addText("Int.", -100.0 * rate, 100.0 * rate, -length - 10.0, HAlign.CENTER, VAlign.BOTTOM);

		if(this.intCheck.isSelected()) {
			double start = 0.0;
			double end = heatmap.getMaxIntensity() * 1.25;
			double range = this.getMeasureRange(end - start);
			String format = this.getMeasureFormat(end - start);

			int startIndex = (int)Math.ceil(start / range);
			int endIndex = (int)Math.floor(end / range);

			for(int i = startIndex; i <= endIndex; i++) {
				double value = range * (double)i;
				double pos = - (value - start) / (end - start) * 75.0;
				this.addText(
					String.format(format, value),
					-100.0 * rate - MEASURE_LENGTH,
					100.0 * rate + MEASURE_LENGTH,
					pos,
					HAlign.RIGHT,
					VAlign.CENTER
				);

				cylinder = new Cylinder(0.5, MEASURE_LENGTH);
				cylinder.setMaterial(material);
				cylinder.setRotationAxis(new Point3D(0.0, 0.0, 1.0));
				cylinder.setRotate(45.0);
				cylinder.setTranslateX(- 100.0 * rate - MEASURE_LENGTH / 2.0 / 1.414);
				cylinder.setTranslateY(100.0 * rate + MEASURE_LENGTH / 2.0 / 1.414);
				cylinder.setTranslateZ(pos);
				group.getChildren().add(cylinder);
			}
		}
	}

	/**
	 * sets the rotation
	 */
	private void setRotation() {
		if(this.group == null ) {
			return;
		}

		this.group.getTransforms().clear();
		this.group.getTransforms().add(new Rotate(this.xAngle, new Point3D(1.0, 0.0, 0.0)));
		this.group.getTransforms().add(new Rotate(this.zAngle, new Point3D(0.0, 0.0, 1.0)));

		for(TextInfo info : this.textArray) {
			this.setTextRotation(info);
		}
	}

	private void setTextRotation(TextInfo info) {
		Text text = info.text;
		double width = text.getBoundsInLocal().getWidth();
		double height = text.getBoundsInLocal().getHeight();

		double hGap = 0.0;
		if(info.hAlign == HAlign.CENTER) {
			hGap = - width / 2.0;
		}
		else if(info.hAlign == HAlign.RIGHT) {
			hGap = - width;
		}

		double vGap = 0.0;
		if(info.vAlign == VAlign.CENTER) {
			vGap = height / 2.0;
		}
		else if(info.vAlign == VAlign.TOP) {
			vGap = height;
		}

		text.getTransforms().clear();
		text.getTransforms().add(new Translate(info.x, info.y, info.z));
		text.getTransforms().add(new Rotate(- this.zAngle, new Point3D(0.0, 0.0, 1.0)));
		text.getTransforms().add(new Rotate(- this.xAngle, new Point3D(1.0, 0.0, 0.0)));
		text.getTransforms().add(new Translate(hGap, vGap, 0.0));
	}


	private Image createTextureImage() {
		WritableImage image = new WritableImage(2000, 1);

		WritablePixelFormat<IntBuffer> format = WritablePixelFormat.getIntArgbInstance();
		int[] pixels = new int[2000];

		ColorTheme theme = this.themeChoice.getSelectionModel().getSelectedItem();

		for(int i = 0; i < 2000; i++) {
			int pixel = 0;
			if(i < 500) {
				pixel = theme.getColor(0.0).getPixel();
			}
			else if(i > 1500) {
				pixel = theme.getColor(1.0).getPixel();
			}
			else {
				double value = (double)(i - 500) / 1000.0;
				pixel = theme.getColor(value).getPixel();
			}
			pixels[ i ] = pixel;
		}
		image.getPixelWriter().setPixels(0,  0,  2000,  1,  format,  pixels, 0, 2000);

		return image;
	}

	private void onSlider(double value) {
		if(this.root == null) {
			return;
		}

		double scale = DEFAULT_Z_POSITION - (value - DEFAULT_Z_POSITION);
		scale = this.scale * scale;
		this.root.setTranslateZ(scale);
	}


	public HeatMap getHeatmap() {
		return this.heatmap;
	}

	
	public void setHeatmap(HeatMap heatmap) {
		this.heatmap = heatmap;

		this.textArray = new ArrayList<TextInfo>();

		Group root = new Group();
		root.getChildren().clear();
		if(heatmap == null) {
			return;
		}
		this.root = root;

		Group group = new Group();
		group.getChildren().clear();
		root.getChildren().add(group);
		this.group = group;

		this.addAxes(heatmap, group);
		this.createHeatmap(heatmap, group);

		AmbientLight light = new AmbientLight();
		light.setColor(Color.WHITE);
		root.getChildren().add(light);

		ThreeDPanel me = this;

		SubScene scene = new SubScene(root, 100.0, 100.0, true, SceneAntialiasing.BALANCED);
		scene.setFill(Color.WHITE);

		PerspectiveCamera camera = new PerspectiveCamera();
		scene.setCamera(camera);

		scene.widthProperty().bind(this.pane.widthProperty().subtract(20.0));
		scene.heightProperty().bind(this.pane.heightProperty().subtract(50.0));
		scene.widthProperty().addListener(
			(observable, oldValue, newValue) -> {
				double width = newValue.doubleValue();
				double height = scene.getHeight();
				me.onSize(root, camera, width,  height);
			}
		);
		scene.heightProperty().addListener(
			(observable, oldValue, newValue) -> {
				double width = scene.getWidth();
				double height = newValue.doubleValue();
				me.onSize(root, camera, width,  height);
			}
		);
		scene.setOnMousePressed(
			(event) -> {
				me.onMousePressed(event);
			}
		);
		scene.setOnMouseDragged(
			(event) -> {
				me.onMouseDrag(event);
			}
		);

		this.pane.setCenter(scene);
		this.onSize(root, camera, scene.getWidth(),  scene.getHeight());

		this.xAngle = - 60.0;
		this.zAngle = 0.0;

		this.setRotation();
	}


	private void onMousePressed(MouseEvent event) {
		this.px = event.getX();
		this.py = event.getY();
	}

	private void onMouseDrag(MouseEvent event) {
		double x = event.getX();
		double y = event.getY();

		double dx = x - this.px;
		double dy = y - this.py;

		if(this.group != null) {
			if( dx != 0.0 ) {
				double angle = - dx;
				this.zAngle += angle;

				while( this.zAngle > 360.0 ) {
					this.zAngle -= 360.0;
				}
				while( this.zAngle < - 360.0 ) {
					this.zAngle += 360.0;
				}
			}

			if(dy != 0.0) {
				double angle = dy;
				this.xAngle += angle;

				if( this.xAngle < - 90.0 ) {
					this.xAngle = - 90.0;
				}
				if( this.xAngle > 0.0 ) {
					this.xAngle = 0.0;
				}
			}

			this.setRotation();
		}

		this.px = x;
		this.py = y;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		ThreeDPanel me = this;

		this.slider.setPrefWidth(20.0);
		this.pane.heightProperty().addListener(
			(observable, oldValue, newValue) -> {
				this.slider.setPrefHeight(newValue.doubleValue());
			}
		);
		this.slider.setMax(MAX_Z_POSITION);
		this.slider.setMin(MIN_Z_POSITION);
		this.slider.setValue(DEFAULT_Z_POSITION);
		this.slider.valueProperty().addListener(
			(observable, oldValue, newValue) -> {
				me.onSlider(newValue.doubleValue());
			}
		);

		this.root = null;

		for(ColorTheme theme : ColorTheme.getThemes()) {
			this.themeChoice.getItems().add(theme);
		}
		this.themeChoice.getSelectionModel().select(0);
		this.themeChoice.getSelectionModel().selectedItemProperty().addListener(
			(observable, oldVal, newVal) -> {
				me.onUpdateConfig();
			}
		);

		CheckBox[] checks = {this.axisCheck, this.rtCheck, this.mzCheck, this.intCheck};
		for(CheckBox check : checks) {
			check.selectedProperty().addListener(
				(observable, oldVal, newVal) -> {
					me.onUpdateConfig();
				}
			);
		}
	}

	private void onUpdateConfig() {
		this.pane.setCenter(null);
		this.setHeatmap(this.heatmap);
	}
}
