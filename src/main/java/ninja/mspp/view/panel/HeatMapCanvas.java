package ninja.mspp.view.panel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Stack;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.method.HeatMapCanvasBackground;
import ninja.mspp.core.annotation.method.HeatMapCanvasForeground;
import ninja.mspp.core.model.ms.Point;
import ninja.mspp.core.model.view.Bounds;
import ninja.mspp.core.model.view.ColorTheme;
import ninja.mspp.core.model.view.DrawingPoint;
import ninja.mspp.core.model.view.HeatMap;
import ninja.mspp.core.model.view.Range;
import ninja.mspp.core.model.view.Rect;
import ninja.mspp.core.view.DrawInfo;

public class HeatMapCanvas extends ProfileCanvas {
	private HeatMap heatmap;
	private Image image;
	
	protected Point endPoint;
	
	private RealMatrix matrix;
	
	private Stack<Rect> rangeStack;
	
	private ColorTheme theme;
	
	public HeatMapCanvas(HeatMap heatmap) {
		super("RT", "m/z");
		
		this.heatmap = heatmap;
		this.image = null;
		this.startPoint = null;
		this.endPoint = null;
		
		this.rangeStack = new Stack<Rect>();
		
		this.theme = ColorTheme.getThemes().get(0);
		this.draw();
	}
		
	public HeatMapCanvas() {
		this(null);
	}
	
	public void setHeatMap(HeatMap heatmap) {
		this.heatmap = heatmap;
		this.image = null;
		this.rangeStack.clear();
		this.draw();
	}
	
	public void setTheme(ColorTheme theme) {
		this.theme = theme;
		this.image = null;
		this.draw();
	}
		
	protected void drawImage(Graphics2D g, int width, int height, Bounds margin, double[][] data) {
		int rtSize = HeatMap.RT_SIZE;
		int mzSize = HeatMap.MZ_SIZE;
		
		if(this.image == null) {
			BufferedImage image = new BufferedImage(rtSize, mzSize, BufferedImage.TYPE_INT_ARGB);
			int[] pixels = new int[rtSize * mzSize];
			
			for(int i = 0; i < mzSize; i++) {
				for(int j = 0; j < rtSize; j++) {
					int index = (mzSize - 1 - i) * rtSize + j;
					double intensity = Math.sqrt(data[j][i]);
					intensity = Math.max(0.0, Math.min(1.0, intensity));
					int pixel = this.theme.getColor(intensity).getPixel();
					pixels[index] = pixel;
				}
			}
			image.setRGB(0, 0, rtSize, mzSize, pixels, 0, rtSize);
			this.image = image;
		}
		
		g.drawImage(
			this.image,
			(int)Math.round(margin.getLeft()),
			(int)Math.round(margin.getTop()),
			(int)Math.round((double)width - margin.getRight() - margin.getLeft()),
			(int)Math.round((double)height - margin.getTop() - margin.getBottom()),
			null
		);
	}
	
	private void changeRange() {
		if(this.heatmap == null || this.startPoint == null || this.endPoint == null) {
			return;
		}
		
		RealMatrix matrix = this.matrix;
		if(matrix == null) {
			return;
		}
		
		RealMatrix inverse = MatrixUtils.inverse(this.startMatrix);
		double[] startPos = {this.startPoint.getX(), this.startPoint.getY(), 1.0};
		double[] endPos = {this.endPoint.getX(), this.endPoint.getY(), 1.0};
		
		double[] startData = inverse.operate(startPos);
		double[] endData = inverse.operate(endPos);
		
		double minRt = Math.min(startData[0], endData[0]);
		double maxRt = Math.max(startData[0], endData[0]);
		double minMz = Math.min(startData[1], endData[1]);
		double maxMz = Math.max(startData[1], endData[1]);
		
		if(maxRt - minRt >= 0.001 && maxMz - minMz >= 0.001) {
			Rect range = new Rect(
					this.heatmap.getRtRange().getStart(),
					this.heatmap.getMzRange().getStart(),
					this.heatmap.getRtRange().getEnd(),
					this.heatmap.getMzRange().getEnd()
			);
			this.rangeStack.push(range);
			this.heatmap.changeRange(minRt, maxRt, minMz, maxMz);
			this.image = null;
		}
		
		this.startPoint = null;
		this.endPoint = null;
		this.draw();
	}

	@Override
	protected void onMousePressed(MouseEvent event) {
		if(this.heatmap == null) {
			return;
		}
		
		double x = event.getX();
		double y = event.getY();
		
		double width = this.getWidth();
		double height = this.getHeight();
		
		if(x >= this.margin.getLeft() && x <= width - this.margin.getRight()
				&& y >= this.margin.getTop() && y <= height - this.margin.getBottom()) {
			this.startMatrix = this.matrix;
			this.startPoint = new Point(x, y);
		}
		else {
			this.startMatrix = null;
			this.startPoint = null;
		}
	}	

	@Override
	protected void onMouseDragged(MouseEvent event) {
		if(this.heatmap == null) {
			return;
		}
		
		if(this.startPoint != null) {
			double width = this.getWidth();
			double height = this.getHeight();
			
			Bounds margin = this.margin;
			
			double x = event.getX();
			double y = event.getY();
			
			x = Math.max(margin.getLeft(), Math.min(x, width - margin.getRight()));
			y = Math.max(margin.getTop(), Math.min(y, height - margin.getBottom()));
			
			Point point = new Point(x, y);
			this.endPoint = point;
		
			this.draw();
		}
	}

	@Override
	protected void onMouseReleased(MouseEvent event) {
		if(this.heatmap == null) {
			return;
		}
		
		if(this.startPoint != null) {
			double width = this.getWidth();
			double height = this.getHeight();
			
			Bounds margin = this.margin;
			
			double x = event.getX();
			double y = event.getY();
			
			x = Math.max(margin.getLeft(), Math.min(x, width - margin.getRight()));
			y = Math.max(margin.getTop(), Math.min(y, height - margin.getBottom()));
			
			Point point = new Point(x, y);
			this.endPoint = point;
		
			this.changeRange();
		}		
	}
	
	@Override
	protected void onMouseMoved(MouseEvent event) {
		double x = event.getX();
		double y = event.getY();
		
		double width = this.getWidth();
		double height = this.getHeight();
		
		Bounds margin = this.margin;
		
		if(margin != null
				&& x >= margin.getLeft() && x <= width - margin.getHeight()
				&& y >= margin.getTop() && y <=  height - margin.getBottom()) {
			this.setCursor(Cursor.CROSSHAIR);
		}
		else {
			this.setCursor(Cursor.DEFAULT);
		}
	}

	@Override
	protected void onMouseClicked(MouseEvent event) {
		if(event.getClickCount() >= 2) {
			this.startPoint = null;
			this.endPoint = null;
			
			if(!this.rangeStack.isEmpty() && this.heatmap != null) {
				Rect range = this.rangeStack.pop();
				this.heatmap.changeRange(
					range.getStartX(),
					range.getEndX(),					
					range.getStartY(),
					range.getEndY()
				);
				this.image = null;
				this.draw();
			}
		}
	}
	
	@Override
	protected Range getXRange() {
		double startX = 0.0;
		double endX = 0.0;
		
		if(this.heatmap != null) {
			startX = this.heatmap.getRtRange().getStart();
			endX = this.heatmap.getRtRange().getEnd();
		}
		
		endX = Math.max(startX + 0.01, endX);
		
		return new Range(startX, endX);
	}

	@Override
	protected Range getYRange() {
		double startY = 0.0;
		double endY = 0.0;
		
		if(this.heatmap != null) {
			startY = this.heatmap.getMzRange().getStart();
			endY =  this.heatmap.getMzRange().getEnd();
		}
		
		endY = Math.max(startY + 0.01, endY);
		
		return new Range(startY, endY);
	}

	@Override
	protected void onDraw(Graphics2D g, double width, double height) {
		if(this.heatmap != null) {
			this.drawData(g, width, height);
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
		RealMatrix matrix = calculateMatrix(width, height, xRange, yRange, margin);
		this.matrix = matrix;
		this.margin = margin;

		drawMouseBackground(g, matrix, width, height, margin, this.startPoint, this.currentPoint);
		drawBackground(g, width, height, margin, matrix, xRange, yRange);
		drawImage(g, (int)width, (int)height, margin, this.heatmap.getData());
		drawForeground(g, width, height, margin, matrix, xRange, yRange);
		drawSelectedRange(g);
		drawRect(g, margin, width, height);
		drawXAxis(g, xTicks, xLabels, matrix, margin, width, height);
		drawYAxis(g, yTicks, yLabels, matrix, margin, width, height);	
		drawTitles(g, width, height);
	}
	
	private void drawSelectedRange(Graphics2D g) {
		Color oldColor = g.getColor();		
		g.setColor(Color.GRAY);
		if(this.startPoint != null && this.endPoint != null) {
			double minX = Math.min(this.startPoint.getX(), this.endPoint.getX());
			double minY = Math.min(this.startPoint.getY(), this.endPoint.getY());
			double maxX = Math.max(this.startPoint.getX(), this.endPoint.getX());
			double maxY = Math.max(this.startPoint.getY(), this.endPoint.getY());
			
			g.drawRect(
				(int)Math.round(minX), (int)Math.round(minY),
				(int)Math.round(maxX - minX), (int)Math.round(maxY - minY)
			);
		}
		g.setColor(oldColor);
	}

	@Override
	public void refresh() {
		this.image = null;
		super.refresh();
	}

	@Override
	protected void drawProfile(Graphics2D g, RealMatrix matrix, double width, double height, Bounds margin,
			List<DrawingPoint> points) {
		this.drawImage(g, (int)width, (int)height, margin, this.heatmap.getData());
	}


	@Override
	protected void drawForeground(Graphics2D g, double width, double height, Bounds margin, RealMatrix matrix,
			Range xRange, Range yRange) {
		MsppManager manager = MsppManager.getInstance();
		
		DrawInfo<HeatMap> drawInfo = new DrawInfo<HeatMap>(
			this.heatmap, width, height, margin, null, matrix, xRange, yRange, g, this
		);
		manager.invoke(HeatMapCanvasForeground.class, drawInfo);
	}

	@Override
	protected void drawBackground(Graphics2D g, double width, double height, Bounds margin, RealMatrix matrix,
			Range xRange, Range yRange) {
		MsppManager manager = MsppManager.getInstance();
		
		DrawInfo<HeatMap> drawInfo = new DrawInfo<HeatMap>(
			this.heatmap, width, height, margin, null, matrix, xRange, yRange, g, this
		);
		manager.invoke(HeatMapCanvasBackground.class, drawInfo);
	}
}
