package ninja.mspp.view.panel;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.imageio.ImageIO;

import org.jfree.fx.FXGraphics2D;
import org.jfree.graphics2d.svg.SVGGraphics2D;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

public abstract class CanvasBase extends Canvas {
	public CanvasBase() {
		this.widthProperty().addListener(observable -> draw());
		this.heightProperty().addListener(observable -> draw());
	}
			
	protected void draw() {
		GraphicsContext gc = this.getGraphicsContext2D();
		
		double width = this.getWidth();
		double height = this.getHeight();
		
		gc.beginPath();
		gc.setFill(Color.WHITE);
		gc.setStroke(Color.WHITE);
		gc.rect(0.0,  0.0,  width,  height);
		gc.closePath();
		gc.fill();
		
		Graphics2D g = new FXGraphics2D(gc);		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		onDraw(g, width, height);
	}
	
	protected void savePng() throws IOException {
		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Files", "*.png"));
		chooser.setTitle("Save PNG File");
		
		File file = chooser.showSaveDialog(this.getScene().getWindow());
		if (file != null) {
			this.savePng(file);
		}
	}
	
	protected void savePng(File file) throws IOException {
		int width = (int)Math.floor(this.widthProperty().doubleValue());
		int height = (int)Math.floor(this.heightProperty().doubleValue());
		
		WritableImage image = new WritableImage(width, height);
		SnapshotParameters parameters = new SnapshotParameters();
		parameters.setFill(Color.TRANSPARENT);
		this.snapshot(parameters, image);
		
		ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
	}
	
	protected void saveSvg() throws IOException {
		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SVG Files", "*.svg"));
		chooser.setTitle("Save SVG File");

		File file = chooser.showSaveDialog(this.getScene().getWindow());
		if (file != null) {
			this.saveSvg(file);
		}
	}
	
	protected void saveSvg(File file) throws IOException {
		int width = (int)Math.floor(this.widthProperty().doubleValue());
		int height = (int)Math.floor(this.heightProperty().doubleValue());
		
		SVGGraphics2D g = new SVGGraphics2D(width, height);
		this.onDraw(g, width, height);
		
		String svg = g.getSVGDocument();
		PrintWriter writer = new PrintWriter(new FileWriter(file));
		writer.print(svg);
		writer.close();
		

	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
	
	@Override
	public double prefWidth(double height) {
		return 0.0;
	}
	
	@Override
	public double prefHeight(double width) {
		return 0.0;
	}
	
	protected abstract void onDraw(Graphics2D g, double width, double height);
}
