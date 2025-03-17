package ninja.mspp.core.view;

import java.awt.Graphics2D;

import org.apache.commons.math3.linear.RealMatrix;

import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.view.Bounds;
import ninja.mspp.core.model.view.Range;
import ninja.mspp.view.panel.ProfileCanvas;

public class DrawInfo<T> {
	private T object;
	private double width;
	private double height;
	private Bounds margin;
	private DataPoints points;
	private RealMatrix matrix;
	private Range xRange;
	private Range yRange;
	private Graphics2D g;
	private ProfileCanvas canvas;
	
	public DrawInfo(T object, double width, double height, Bounds margin, DataPoints points,
			RealMatrix matrix, Range xRange, Range yRange, Graphics2D g, ProfileCanvas canvas) {
		this.object = object;
		this.width = width;
		this.height = height;
		this.margin = margin;
		this.points = points;
		this.matrix = matrix;		
		this.xRange = xRange;
		this.yRange = yRange;
		this.g = g;
		this.canvas = canvas;
	}
	
	public T getObject() {
		return object;
	}
	
	public double getWidth() {
		return width;
	}
	
	public double getHeight() {
		return height;
	}
	
	public Bounds getMargin() {
		return margin;
	}
	
	public DataPoints getPoints() {
		return points;
	}
	
	public RealMatrix getMatrix() {
		return matrix;
	}
	
	public Range getXRange() {
		return xRange;
	}
	
	public Range getYRange() {
		return yRange;
	}
	
	public Graphics2D getGraphics() {
		return g;
	}
	
	public double top() {
		return this.margin.getTop();
	}
	
	public double bottom() {
		return this.height - this.margin.getBottom();
	}
	
	public double left() {
		return this.margin.getLeft();
	}
	
	public double right() {
		return this.width - this.margin.getRight();
	}
	
	public ProfileCanvas getCanvas() {
		return this.canvas;
	}
}