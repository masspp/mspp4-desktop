package ninja.mspp.operation.peaks;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
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
			} else {
				WakuraPeakDetection detector = new WakuraPeakDetection();
				peaks = detector.detect(points);
			}

			if (peaks != null) {
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
			Bounds margin) {
		Color color = g.getColor();
		g.setColor(Color.BLACK);

		PeakList list = new PeakList();
		if (peaks != null) {
			for (Peak peak : peaks) {
				double x = peak.getX();
				double y = peak.getY();
				double start = peak.getStart();
				double end = peak.getEnd();

				if (x >= xRange.getStart() && x <= xRange.getEnd() && y >= yRange.getStart() && y <= yRange.getEnd()) {
					Peak newPeak = new Peak(x, y, start, end);
					newPeak.setImage(peak.getImage());
					newPeak.setAnnotation(peak.getAnnotation());
					list.add(newPeak);
				}
			}

			list.sort((a, b) -> Double.compare(b.getY(), a.getY())); // 高い順
		}

		List<Rect> rects = new ArrayList<>();
		List<Rect> imageRects = new ArrayList<>();
		double left = margin.getLeft();
		double top = margin.getTop();
		double bottom = height - margin.getBottom();
		double right = width - margin.getRight();
		FontMetrics metrics = g.getFontMetrics();
		double textHeight = g.getFont().getSize();

		for (Peak peak : list) {
			String label = String.format("%.3f", peak.getX());
			double textWidth = metrics.stringWidth(label);
			double[] coordinates = { peak.getX(), peak.getY(), 1.0 };
			double[] position = matrix.operate(coordinates);

			double textLeft = Math.max(left, position[0] - textWidth / 2.0);
			double textRight = Math.min(right, textLeft + textWidth);
			textLeft = textRight - textWidth;

			double textTop = Math.max(top, position[1] - textHeight);
			double textBottom = Math.min(bottom, textTop + textHeight);
			textTop = textBottom - textHeight;

			boolean canDraw = true;
			Rect rect = new Rect(textLeft, textTop, textRight, textBottom);
			for (Rect r : rects) {
				if (r.intersects(rect)) {
					canDraw = false;
				}
			}

			Image image = peak.getImage();
			if (image != null) {
				Image transparentImage = makeWhiteTransparent(image);
				int originalWidth = transparentImage.getWidth(null);
				int originalHeight = transparentImage.getHeight(null);

				double scale = 1.0;
				int shortSide = Math.min(originalWidth, originalHeight);
				if (shortSide > 75) {
					scale = 75.0 / shortSide;
				}
				int scaledWidth = (int) (originalWidth * scale);
				int scaledHeight = (int) (originalHeight * scale);

				double imageLeft = Math.max(left, Math.min(right - scaledWidth, position[0] - scaledWidth / 2.0));
				double imageTop = Math.max(top, textTop - scaledHeight);
				textTop = imageTop - textHeight;
				if (textTop < top + textHeight + 1) {
					textTop = top + textHeight + 1;
				}
				imageTop = Math.min(imageTop, bottom - scaledHeight);

				Rect imageRect = new Rect(imageLeft, imageTop, imageLeft + scaledWidth, imageTop + scaledHeight);
				boolean canDrawImage = imageRects.stream().noneMatch(r -> r.intersects(imageRect));

				if (canDrawImage) {
					imageRects.add(imageRect);
					g.drawImage(transparentImage, (int) imageLeft, (int) imageTop, scaledWidth, scaledHeight, null);
				}
			}

			if (canDraw) {
				g.drawString(label, (int) textLeft, (int) textTop);
				rects.add(rect);
			}
		}

		g.setColor(color);
	}

	private Image makeWhiteTransparent(Image image) {
		int w = image.getWidth(null);
		int h = image.getHeight(null);
		BufferedImage transparent = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = transparent.createGraphics();
		g2.drawImage(image, 0, 0, null);
		g2.dispose();

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int rgb = transparent.getRGB(x, y);
				if ((rgb & 0x00FFFFFF) == 0x00FFFFFF) {
					transparent.setRGB(x, y, 0x00FFFFFF & rgb);
				}
			}
		}

		return transparent;
	}
}
