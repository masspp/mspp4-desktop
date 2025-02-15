package ninja.mspp.operation.peaks;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.linear.RealMatrix;

import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.ChromatogramCanvasForeground;
import ninja.mspp.core.annotation.method.OnSelectChromatogram;
import ninja.mspp.core.annotation.method.OnSelectSpectrum;
import ninja.mspp.core.annotation.method.SpectrumCanvasForeground;
import ninja.mspp.core.model.PeakManager;
import ninja.mspp.core.model.ms.Chromatogram;
import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.Peak;
import ninja.mspp.core.model.ms.PeakList;
import ninja.mspp.core.model.ms.Point;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.core.model.view.Bounds;
import ninja.mspp.core.model.view.Range;
import ninja.mspp.core.model.view.Rect;
import ninja.mspp.core.view.DrawInfo;

@Listener("peaks")
public class PeaksListener {
	@OnSelectSpectrum(order = 0)
	public void onSelectSpectrum(Spectrum spectrum) {
		PeakManager manager = PeakManager.getInstance();
				
		if (!manager.hasPeaks(spectrum)) {
			DataPoints points = spectrum.readDataPoints();
		
			PeakList peaks = null;
			if (spectrum.isCentroidMode()) {
				peaks = this.getCentroidPeaks(points);
			}
			else {
				WakuraPeakDetection detector = new WakuraPeakDetection();
				peaks = detector.detect(points);
			}
		
			if(peaks != null) {			
				manager.setPeaks(spectrum, peaks);
			}
		}
	}
	
	@OnSelectChromatogram(order = 0)
	public void onSelectChromatogram(Chromatogram chromatogram) {
		PeakManager manager = PeakManager.getInstance();
		
		if (!manager.hasPeaks(chromatogram)) {
			DataPoints points = chromatogram.readDataPoints();
			WakuraPeakDetection detector = new WakuraPeakDetection();
			PeakList peaks = detector.detect(points);

			if (peaks != null) {			
				manager.setPeaks(chromatogram, peaks);
			}
		}
	}
	
	private PeakList getCentroidPeaks(DataPoints points) {
		PeakList peaks = new PeakList();
		
		for (Point point : points) {
			double x = point.getX();
			double y = point.getY();

			if (y > 0.0) {
				Peak peak = new Peak(x, y, x, x);
				peaks.add(peak);
			}
		}
		
		return peaks;
	}


	@SpectrumCanvasForeground
	public void drawSpectrumLabels(DrawInfo<Spectrum> draw) {
		PeakManager manager = PeakManager.getInstance();
		
		Spectrum spectrum = draw.getObject();
		if (manager.hasPeaks(spectrum)) {
			PeakList peaks = manager.getPeaks(spectrum);
		
			Graphics2D g = draw.getGraphics();
			Bounds margin = draw.getMargin();
			DataPoints points = draw.getPoints();
			double width = draw.getWidth();
			double height = draw.getHeight();
			RealMatrix matrix = draw.getMatrix();
			Range xRange = draw.getXRange();
			Range yRange = draw.getYRange();
		
			this.drawLabels(peaks, g, points, matrix, width, height, xRange, yRange, margin);
		}
	}


	@ChromatogramCanvasForeground
	public void drawChromatogramLabels(DrawInfo<Chromatogram> draw) {
		PeakManager manager = PeakManager.getInstance();

		Chromatogram chromatogram = draw.getObject();
		if (manager.hasPeaks(chromatogram)) {
			PeakList peaks = manager.getPeaks(chromatogram);

			Graphics2D g = draw.getGraphics();
			Bounds margin = draw.getMargin();
			DataPoints points = draw.getPoints();
			double width = draw.getWidth();
			double height = draw.getHeight();
			RealMatrix matrix = draw.getMatrix();
			Range xRange = draw.getXRange();
			Range yRange = draw.getYRange();

			this.drawLabels(peaks, g, points, matrix, width, height, xRange, yRange, margin);
		}
	}
	
	private void drawLabels(
			PeakList peaks, 
			Graphics2D g, 
			DataPoints points,
			RealMatrix matrix,
			double width,
			double height,
			Range xRange,
			Range yRange,
			Bounds margin
	) {
		Color color = g.getColor();
		g.setColor(Color.BLACK);
		
		PeakList list = new PeakList();
		for (Peak peak : peaks) {
			double x = peak.getX();
			double y = peak.getY();
			double start = peak.getStart();
			double end = peak.getEnd();
			
			if (x >= xRange.getStart() && x <= xRange.getEnd() && y >= yRange.getStart() && y <= yRange.getEnd()) {
				Peak newPeak = new Peak(x, y, start, end);
				list.add(newPeak);
			}
		}
			
		list.sort(
			(a, b) -> {
				int cmd = 0;
				double diff = a.getY() - b.getY();
				if (diff < 0.0) {
					cmd = 1;
				}
				else if (diff > 0.0) {
					cmd = -1;
				}
				return cmd;
			}
		);
		
		List<Rect> rects = new ArrayList<Rect>();
		double left = margin.getLeft();
		double top = margin.getTop();
		double bottom = height - margin.getBottom();
		double right = width - margin.getRight();
		FontMetrics metrics = g.getFontMetrics();
		
		double textHeight = g.getFont().getSize();

		for(Peak peak : list) {
			String label = String.format("%.3f", peak.getX());
			double textWidth = metrics.stringWidth(label);
			
			double[] coordinates = {peak.getX(), peak.getY(), 1.0};
			double[] position = matrix.operate(coordinates);
			
			double textLeft = position[0] - textWidth / 2.0;
			if(textLeft < left) {
				textLeft = left;
			}
			
			double textRight = textLeft + textWidth;
			if(textRight > right) {
				textRight = right;
                textLeft = right - textWidth;
            }
			
			double textTop = position[1] - textHeight;
			if(textTop < top) {
                textTop = top;
            }
			
			double textBottom = textTop + textHeight;
			if(textBottom > bottom) {
                textBottom = bottom;
                textTop = bottom - textHeight;
            }
			
			boolean canDraw = true;
			Rect rect = new Rect(textLeft, textTop, textRight, textBottom);
			for(Rect r : rects) {
                if(r.intersects(rect)) {
                    canDraw = false;
                }
            }
			
			if(canDraw) {
				g.drawString(label, (int)textLeft, (int)textTop);
				rects.add(rect);
			}
		}
		
		g.setColor(color);
	}
}
