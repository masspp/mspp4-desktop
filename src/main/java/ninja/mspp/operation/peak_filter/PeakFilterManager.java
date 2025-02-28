package ninja.mspp.operation.peak_filter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.commons.math3.linear.RealMatrix;
import org.hibernate.Session;
import org.hibernate.query.Query;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.method.Refresh;
import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.Point;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.core.model.view.Bounds;
import ninja.mspp.core.model.view.HeatMap;
import ninja.mspp.core.model.view.Range;
import ninja.mspp.core.view.DrawInfo;
import ninja.mspp.core.view.ViewInfo;
import ninja.mspp.operation.peak_filter.model.HitPeak;
import ninja.mspp.operation.peak_filter.model.entity.FilterPeak;
import ninja.mspp.operation.peak_filter.model.entity.FilterPeakSet;

public class PeakFilterManager {
	private static PeakFilterManager instance;
	
	public static final String SET_KEY = "PEAK_FILTER_SET";
	private static final int LABEL_MARGIN = 5; 
	private static final int POSITION_SIZE = 2;
	
	private ViewInfo<PeakFilterDialog> activeDialog;
	private boolean drawingLabel;
	private List<FilterPeak> peaks;
	private List<HitPeak> result;
	
	class LabelPosition {
		double left;
		double right;
		int level;
	};
	
	private PeakFilterManager() {
		this.drawingLabel = true;
		this.peaks = null;
		this.result = null;
	}
	
	public List<FilterPeakSet> getFilterPeakSets() {
		MsppManager manager = MsppManager.getInstance();
		Session session = manager.getSession();
		
		Query<FilterPeakSet> query = session.createQuery("from FilterPeakSet", FilterPeakSet.class);
		List<FilterPeakSet> list = query.getResultList();
		
		session.close();
		
		return list;
	}
	
	
	public FilterPeakSet saveNew(String name) {
		MsppManager manager = MsppManager.getInstance();
		Session session = manager.getSession();

		FilterPeakSet set = new FilterPeakSet();
		set.setName(name);
		set.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		session.save(set);

		session.close();

		return set;
	}
	
	
	protected void deletePeaks(long setId, Session session) {
		Query<FilterPeak> query = session.createQuery("from FilterPeak where setId = :setId", FilterPeak.class);
		query.setParameter("setId", setId);
		List<FilterPeak> list = query.getResultList();
		for (FilterPeak peak : list) {
			session.delete(peak);
		}				
	}
	
	public void delete(FilterPeakSet set) {
		MsppManager manager = MsppManager.getInstance();
		Session session = manager.getSession();
		session.beginTransaction();
		
		this.deletePeaks(set.getId(), session);
		session.delete(set);
		
		session.getTransaction().commit();
		session.close();
	}
	
	
	public void savePeaks(long setId, List<FilterPeak> peaks) {
		MsppManager manager = MsppManager.getInstance();
		Session session = manager.getSession();
		
		session.beginTransaction();
		
		this.deletePeaks(setId, session);
		for (FilterPeak peak : peaks) {
			peak.setSetId(setId);
			session.save(peak);
		}
		
		session.getTransaction().commit();		
		session.close();
	}
	
	public List<FilterPeak> getPeaks(long setId) {
		MsppManager manager = MsppManager.getInstance();
		Session session = manager.getSession();

		Query<FilterPeak> query = session.createQuery("from FilterPeak where setId = :setId", FilterPeak.class);
		query.setParameter("setId", setId);
		List<FilterPeak> list = query.getResultList();

		session.close();

		return list;
	}
	
	public FilterPeakSet saveNew(List<FilterPeak> peaks) {
		FilterPeakSet set = null;
		
		MsppManager manager = MsppManager.getInstance();
		ResourceBundle messages = manager.getMessages();
		
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Save As");
		dialog.setHeaderText(messages.getString("peak_filter.saveas.title"));
		dialog.setContentText(messages.getString("peak_filter.saveas.title"));
		
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			String name = result.get();
			set = this.saveNew(name);
			this.savePeaks(set.getId(), peaks);
		}
		
		return set;
	}
	
	
	private double getIntensityThreshold(DataPoints points, double threshold, String unit) {
		double intensityThreshold = threshold;
		if(unit.equals("%")) {
			double maxIntensity = 1.0;
			for (Point point : points) {
				if (point.getY() > maxIntensity) {
					maxIntensity = point.getY();
				}
			}
			intensityThreshold = maxIntensity * threshold / 100.0;
		}
		return intensityThreshold;
	}
	
	
	private Point searchPeak(DataPoints points, FilterPeak peak, double tolerance, double intensityThreshold) {
		Point result = null;
		double mz = peak.getMz();
		double minMz = mz - tolerance;
		double maxMz = mz + tolerance;
		double maxIntensity = 0.0;
		
		int startIndex = Collections.binarySearch(points, new Point(minMz, 0.0));
		if(startIndex < 0) {
			startIndex = - startIndex - 1;
		}
		int endIndex = Collections.binarySearch(points, new Point(maxMz, 0.0));
		if (endIndex < 0) {
			endIndex = - endIndex - 2;
		}
		
		for (int i = startIndex; i <= endIndex; i++) {
			Point point = points.get(i);
			if (point.getY() >= intensityThreshold) {
				if (point.getY() > maxIntensity) {
					maxIntensity = point.getY();
					result = point;
				}
			}
		}
		
		return result;
	}
	
	
	public List<HitPeak> searchPeaks(Sample sample, List<FilterPeak> peaks, double tolerance, double threshold, String unit) {
		List<HitPeak> result = new ArrayList<HitPeak>();
		
		for(Spectrum spectrum : sample.getSpectra()) {
			if(spectrum.getMsLevel() >= 2) {
				DataPoints points = spectrum.readDataPoints();
				HitPeak hitPeak = null;
				double intensityThreshold = getIntensityThreshold(points, threshold, unit);
				for (FilterPeak peak : peaks) {
					Point point = this.searchPeak(points, peak , tolerance, intensityThreshold);
					if(point != null) {
						if(hitPeak == null) {
							hitPeak = new HitPeak(spectrum);
						}
						hitPeak.getHitMap().put(peak, point.getY());
					}
				}
				if(hitPeak != null) {
					result.add(hitPeak);
				}
			}
		}
		
		return result;
	}

	public void openResultDialog(List<FilterPeak> peaks, List<HitPeak> result) throws IOException {
		this.peaks = peaks;
		this.result = result;
		MsppManager manager = MsppManager.getInstance();
		ViewInfo<ResultDialog> viewInfo = manager.showDialog(ResultDialog.class, "ResultDialog.fxml", "Peak Filter Result");
		ResultDialog dialog = viewInfo.getController();
		dialog.setResult(peaks, result);
		
		manager.invoke(Refresh.class);
	}
	
	public void unsetResult() {
		this.peaks = null;
		this.result = null;
	}
	
	public void openDialog(Sample sample) throws IOException {
		MsppManager manager = MsppManager.getInstance();
		ViewInfo<PeakFilterDialog> viewInfo = manager.showDialog(PeakFilterDialog.class, "PeakFilterDialog.fxml", "Peak Filter");
		PeakFilterDialog dialog = viewInfo.getController();
		dialog.setSample(sample);
		this.activeDialog = viewInfo;
	}
	
	public void setActiveDialog(ViewInfo<PeakFilterDialog> dialog) {
		this.activeDialog = dialog;
	}
	
	
	public ViewInfo<PeakFilterDialog> getActiveDialog() throws IOException {
		MsppManager manager = MsppManager.getInstance();
		
		if (this.activeDialog == null) {
			Sample sample = manager.getActiveSample();
			if(sample == null) {
				Alert alert = new Alert(AlertType.ERROR);
				ResourceBundle messages = manager.getMessages();
				alert.setTitle("Error");
				alert.setHeaderText(messages.getString("sample.not_selected.title"));
				alert.setContentText(messages.getString("sample.not_selected.content"));
				alert.showAndWait();
			}
			else {
				this.openDialog(sample);
			}
		}
		return this.activeDialog;
	}
	
	
	public void setDrawingFlag(boolean drawing) {
		this.drawingLabel = drawing;
		
		MsppManager manager = MsppManager.getInstance();
		manager.invoke(Refresh.class);
	}
	
	private HitPeak searchHit(Spectrum spectrum) {
		HitPeak result = null;
		if(this.result != null) {
			for(HitPeak peak : this.result) {
				if(peak.getSpectrum() == spectrum) {
					result = peak;
				}
			}
		}
		return result;
	}
	
	
	protected int searchLevel(List<LabelPosition> positions, double left, double right) {
		int level = 1;
		boolean loop = true;
		while(loop) {
			boolean found = false;
			for (LabelPosition position : positions) {
				if(position.level == level) {
					if(left <= position.right && right >= position.left) {
						found = true;
					}
				}
			}
			
			if(found) {
				level++;
			}
			else {
				loop = false;
			}
		}
		
		return level;
	}
	
	
	protected void drawLabel(DrawInfo<Spectrum> drawInfo, List<FilterPeak> peaks) {
		Range xRange = drawInfo.getXRange();
		RealMatrix matrix = drawInfo.getMatrix();
		Graphics2D g = drawInfo.getGraphics();
		
		Font font = new Font("Monospaced", Font.PLAIN, 12);
		g.setFont(font);
		FontMetrics metrics = g.getFontMetrics(font);
		
		List<LabelPosition> positions = new ArrayList<LabelPosition>();
				
		for(FilterPeak peak : peaks) {
			Color color = this.getColor(peak.getColor());
			double mz = peak.getMz();
			String name = peak.getName();
			if(xRange.contains(mz)) {
				double[] data = {mz, 0.0, 1.0};
				double px = matrix.operate(data)[0];
				
				double textWidth = (double)metrics.stringWidth(name);
				double textHeight = (double)metrics.getHeight();
				
				double textPx = px - textWidth / 2.0;
				
				if(textPx < drawInfo.left()) {
					textPx = drawInfo.left();
				}
				if(textPx + textWidth > drawInfo.right()) {
                    textPx = drawInfo.right() - textWidth;
                }
				
				g.setColor(color);
				
				int level = this.searchLevel(positions, textPx, textPx + textWidth);
				LabelPosition position = new LabelPosition();
				position.left = textPx;
				position.right = textPx + textWidth;
				position.level = level;
				positions.add(position);
				
				Stroke stroke = g.getStroke();
				
				float[] dashPattern = {1.0f, 3.0f};
				Stroke dotted = new BasicStroke(
					1.0f,
					BasicStroke.CAP_BUTT,
	                BasicStroke.JOIN_MITER,
	                10.0f,
	                dashPattern,
	                0.0f
	            );
				g.setStroke(dotted);
				g.drawLine(
					(int)Math.round(px), (int)Math.round(drawInfo.bottom()), 
					(int)Math.round(px), (int)Math.round(drawInfo.top() + LABEL_MARGIN + textHeight * (double)(level + 1))
				);
				
				g.setStroke(stroke);
				g.drawString(
					name, 
					(int)Math.round(textPx), 
					(int)Math.round(drawInfo.top() + LABEL_MARGIN + textHeight * (double)level)
				);
			}
		}
	}
	
	
	public void drawLabel(DrawInfo<Spectrum> drawInfo) {
		if (this.drawingLabel) {
			HitPeak hit = this.searchHit(drawInfo.getObject());
			if(hit != null) {
				Map<FilterPeak, Double> map = hit.getHitMap();
				if(map != null) {
					List<FilterPeak> peaks = new ArrayList<FilterPeak>();
					for(FilterPeak peak : this.peaks) {
						if(map.containsKey(peak)) {
							peaks.add(peak);
						}
					}
					if(peaks.size() > 0) {
						drawLabel(drawInfo, peaks);
					}
				}
			}
		}
	}
	
	public void drawPosition(DrawInfo<HeatMap> drawInfo) {
		if (this.drawingLabel && this.peaks != null && this.result != null) {
			RealMatrix matrix = drawInfo.getMatrix();
			Graphics2D g = drawInfo.getGraphics();
			Bounds margin = drawInfo.getMargin();
			double width = drawInfo.getWidth();
			double height = drawInfo.getHeight();
			
			Color oldColor = g.getColor();
			
			for(FilterPeak peak : this.peaks) {
				String colorString = peak.getColor();
				if(colorString.length() > 6) {
					colorString = colorString.substring(0, 6);
				}
				Color color = this.getColor(peak.getColor());
				g.setColor(color);
				
				double mz = peak.getMz();
				
				for(HitPeak hit : this.result) {
					Map<FilterPeak, Double> map = hit.getHitMap();
					if(map.containsKey(peak)) {
						Spectrum spectrum = hit.getSpectrum();
						double rt = spectrum.getRt();
						
						double[] coordinate = {rt, mz, 1.0};
						double[] position = matrix.operate(coordinate);
						
						double x = position[0];
						double y = position[1];
						
						double minX = Math.max(margin.getLeft(), Math.min(x - POSITION_SIZE, width - margin.getRight()));
						double maxX = Math.max(margin.getLeft(), Math.min(x + POSITION_SIZE, width - margin.getRight()));
						double minY = Math.max(margin.getTop(), Math.min(y - POSITION_SIZE, height - margin.getBottom()));
						double maxY = Math.max(margin.getTop(), Math.min(y + POSITION_SIZE, height - margin.getBottom()));
						
						g.drawRect(
							(int)Math.round(minX), (int)Math.round(minY),
							(int)Math.round(maxX - minX), (int)Math.round(maxY - minY)
						);
					}
				}
			}
			
			g.setColor(oldColor);
		}
	}
	
	private Color getColor(String colorString) {
		if (colorString.length() > 6) {
			colorString = colorString.substring(2, 8);
		}
		colorString = "#" + colorString;
		colorString = colorString.toUpperCase();
		return Color.decode(colorString);
	}

	
	public static PeakFilterManager getInstance() {
		if (instance == null) {
			instance = new PeakFilterManager();
		}
		return instance;
	}
}
