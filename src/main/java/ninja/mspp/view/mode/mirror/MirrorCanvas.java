package ninja.mspp.view.mode.mirror;

import java.awt.Graphics2D;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.view.Bounds;
import ninja.mspp.core.model.view.DrawingData;
import ninja.mspp.core.model.view.DrawingPoint;
import ninja.mspp.core.model.view.Range;
import ninja.mspp.view.panel.ProfileCanvas;

public class MirrorCanvas extends ProfileCanvas {
	protected DrawingData data2;
	protected DataPoints points2;
	protected RealMatrix matrix2;
	protected Bounds margin2;
	
	
	public MirrorCanvas(String xTitle, String yTitle) {
		super(xTitle, yTitle);
		this.data2 = null;
		this.points2 = null;
		this.matrix2 = null;
		this.margin2 = null;
    }
	
	@Override
	public void setPoints(DataPoints points) {
		this.data = new DrawingData(points);
		this.points = points;
		this.yRanges.clear();
		this.draw();
	}
	
	public void setPoints2(DataPoints points) {
		this.data2 = new DrawingData(points);
		this.points2 = points;
		this.yRanges.clear();
		this.draw();
	}
	
	@Override
	protected Range getXRange() {
		double start = Double.NaN;
		double end = Double.NaN;
		
		if (this.xRanges.isEmpty()) {
			if(this.points != null) {
				start = this.points.getFirst().getX();
				end = this.points.getLast().getX();
			}
			
			if(this.points2 != null) {
				double start2 = this.points2.getFirst().getX();
	            double end2 = this.points2.getLast().getX();
				if(Double.isNaN(start) || start2 < start) {
                    start = start2;
                }
				if(Double.isNaN(end) || end2 > end) {
                    end = end2;
                }
			}
			
			if(Double.isNaN(start)) {
				start = 0.0;
			}
			if(Double.isNaN(end)) {
                end = 0.0;
            }
		} 
		else {
			start = this.xRanges.peek().getStart();
			end = this.xRanges.peek().getEnd();
		}

		start = Math.max(0.0, start);
		end = Math.max(end, start + 0.01);
		return new Range(start, end);		
	}
	
	@Override
	protected Range getYRange() {
		double start = 0.0;
		double end = 1.0;

		if (this.yRanges.isEmpty()) {
			Range xRange = this.getXRange();
			
			if(this.points != null) {
				double maxY = this.points.findMaxY(xRange.getStart(), xRange.getEnd());
				if(maxY > end) {
					end = maxY;
				}
			}
			if(this.points2 != null) {
				double maxY = this.points2.findMaxY(xRange.getStart(),  xRange.getEnd());
				if(maxY > end) {
					end = maxY;
				}
			}
			end = end * 1.15;
		} 
		else {
			start = this.yRanges.peek().getStart();
			end = this.yRanges.peek().getEnd();
		}

		start = Math.max(0.0, start);
		end = Math.max(end, start + 0.01);
		return new Range(start, end);
	}	
	
	protected RealMatrix calculateMatrix2(double width, double height, Range xRange, Range yRange, Bounds margin) {
		RealMatrix dataMatrix = new Array2DRowRealMatrix(3, 3);
		dataMatrix.setEntry(0, 0, xRange.getLength());
		dataMatrix.setEntry(0,  1,  0.0);
		dataMatrix.setEntry(0, 2, xRange.getStart());
		dataMatrix.setEntry(1, 0, 0.0);
		dataMatrix.setEntry(1, 1, yRange.getLength());
		dataMatrix.setEntry(1, 2, yRange.getStart());
		dataMatrix.setEntry(2, 0, 0.0);
		dataMatrix.setEntry(2, 1, 0.0);
		dataMatrix.setEntry(2, 2, 1.0);
		
		RealMatrix windowMatrix = new Array2DRowRealMatrix(3, 3);
		windowMatrix.setEntry(0, 0, width - margin.getLeft() - margin.getRight());
		windowMatrix.setEntry(0, 1, 0.0);
		windowMatrix.setEntry(0, 2, margin.getLeft());
		windowMatrix.setEntry(1, 0, 0.0);
		windowMatrix.setEntry(1, 1, height - margin.getTop() - margin.getBottom());
		windowMatrix.setEntry(1, 2, margin.getTop());
		windowMatrix.setEntry(2, 0, 0.0);
		windowMatrix.setEntry(2, 1, 0.0);
		windowMatrix.setEntry(2, 2, 1.0);
		
		RealMatrix inverse = MatrixUtils.inverse(dataMatrix);
		RealMatrix matrix = windowMatrix.multiply(inverse);
		return matrix;
	}	
	
	@Override
	protected void onDraw(Graphics2D g, double width, double height) {
		if(this.data != null || this.data2 != null) {
			drawData(g, width, height);
		}
	}
	
	@Override
	protected void drawData(Graphics2D g, double width, double height) {
		g.setFont(this.font);
		
		Range xRange = this.getXRange();
		Range yRange = this.getYRange();
		double[] xTicks = this.getTicks(xRange);
		double[] yTicks = this.getTicks(yRange);
		String[] xLabels = this.getTickLabels(xTicks);
		String[] yLabels = this.getTickLabels(yTicks);

		Bounds margin = this.calculateMargin(g, xLabels, yLabels);
		int center = (int)Math.round((height - margin.getTop() - margin.getBottom()) / 2.0 + margin.getTop());
		Bounds margin1 = new Bounds(margin.getTop(), margin.getRight(), (int)height - center, margin.getLeft());
		Bounds margin2 = new Bounds(center, margin.getRight(), margin.getBottom(), margin.getLeft());
		
		RealMatrix matrix1 = calculateMatrix(width, height, xRange, yRange, margin1);
		RealMatrix matrix2 = calculateMatrix2(width, height, xRange, yRange, margin2);
		this.matrix = matrix1;
		this.margin = margin;
		this.matrix2 = matrix2;
		this.margin2 = margin2;
		
		List<DrawingPoint> points1 = null;
		if(this.data != null) {
			int level1 = this.data.calculateLevel(width, xRange.getStart(), xRange.getEnd());
			points1 = this.data.getPoints(level1);
		}
		
		List<DrawingPoint> points2 = null;
		if(this.data2 != null) {
			int level2 = this.data2.calculateLevel(width, xRange.getStart(), xRange.getEnd());
			points2 = this.data2.getPoints(level2);
		}

		drawMouseBackground(g, matrix, width, height, margin, this.startPoint, this.currentPoint);
		drawBackground(g, width, height, margin, matrix, xRange, yRange);
		if(points1 != null) {
			drawProfile(g, matrix1, width, height, margin1, points1);
		}
		if(points2 != null) {
			drawProfile(g, matrix2, width, height, margin2, points2);
		}
		drawForeground(g, width, height, margin, matrix, xRange, yRange);
		drawRect(g, margin, width, height);
		drawXAxis(g, xTicks, xLabels, matrix1, margin, width, height);
		drawYAxis(g, yTicks, yLabels, matrix1, margin1, width, height);
		drawYAxis(g, yTicks, yLabels, matrix2, margin2, width, height);
		drawTitles(g, width, height);
	}	
}
