package ninja.mspp.view.mode.stack;

import java.awt.Color;

import javafx.scene.input.MouseEvent;
import ninja.mspp.core.model.ms.Chromatogram;
import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.core.model.view.Range;
import ninja.mspp.view.mode.stack.StackViewManager.Kind;
import ninja.mspp.view.panel.ProfileCanvas;

/**
 * A profile canvas that participates in a stack. All canvases of the same
 * {@link Kind} share an X range (zoom/pan) and a Y scale (max across visible
 * data of all peers), driven through {@link StackViewManager}.
 */
public class StackedCanvas extends ProfileCanvas {
	private final Kind kind;
	private boolean syncing;

	StackedCanvas(Kind kind) {
		super(kind == Kind.SPECTRUM ? "m/z" : "RT", "Int.");
		this.kind = kind;
		this.setProfileColor(kind == Kind.SPECTRUM ? Color.RED : Color.BLUE);
		StackViewManager.getInstance().register(this);
	}

	Kind getKind() {
		return this.kind;
	}

	void dispose() {
		StackViewManager.getInstance().unregister(this);
	}

	void setSpectrum(Spectrum spectrum) {
		if(spectrum == null) {
			return;
		}
		this.setImpulseMode(spectrum.isCentroidMode());
		DataPoints points = spectrum.readDataPoints();
		this.applyPoints(points);
	}

	void setChromatogram(Chromatogram chromatogram) {
		if(chromatogram == null) {
			return;
		}
		DataPoints points = chromatogram.readDataPoints();
		this.applyPoints(points);
	}

	private void applyPoints(DataPoints points) {
		this.data = new ninja.mspp.core.model.view.DrawingData(points);
		this.points = points;
		this.xRanges.clear();
		this.yRanges.clear();
		Range shared = StackViewManager.getInstance().getSharedXRange(this.kind);
		if(shared != null) {
			this.xRanges.push(new Range(shared.getStart(), shared.getEnd()));
		}
		StackViewManager.getInstance().redrawAll(this.kind);
	}

	void setSyncedXRange(Range range) {
		if(this.syncing) {
			return;
		}
		this.xRanges.clear();
		if(range != null) {
			this.xRanges.push(new Range(range.getStart(), range.getEnd()));
		}
		this.draw();
	}

	void clearSyncedXRange() {
		if(this.syncing) {
			return;
		}
		this.xRanges.clear();
		this.draw();
	}

	@Override
	protected Range getYRange() {
		if(!this.yRanges.isEmpty()) {
			return new Range(this.yRanges.peek().getStart(), this.yRanges.peek().getEnd());
		}
		Range xRange = this.getXRange();
		StackViewManager mgr = StackViewManager.getInstance();
		double max = 0.0;
		if(mgr.isYSync()) {
			for(StackedCanvas c : mgr.getCanvases(this.kind)) {
				if(c.points == null) {
					continue;
				}
				double m = c.points.findMaxY(xRange.getStart(), xRange.getEnd());
				if(m > max) {
					max = m;
				}
			}
		}
		else if(this.points != null) {
			max = this.points.findMaxY(xRange.getStart(), xRange.getEnd());
		}
		double end = max > 0.0 ? max * 1.15 : 1.0;
		return new Range(0.0, end);
	}

	@Override
	protected void onMouseDragged(MouseEvent event) {
		this.syncing = true;
		try {
			super.onMouseDragged(event);
		}
		finally {
			this.syncing = false;
		}
		this.broadcastCurrent();
	}

	@Override
	protected void onMouseReleased(MouseEvent event) {
		this.syncing = true;
		try {
			super.onMouseReleased(event);
		}
		finally {
			this.syncing = false;
		}
		this.broadcastCurrent();
	}

	@Override
	protected void onMouseClicked(MouseEvent event) {
		this.syncing = true;
		try {
			super.onMouseClicked(event);
		}
		finally {
			this.syncing = false;
		}
		if(event.getClickCount() == 2) {
			StackViewManager.getInstance().broadcastXRangeReset(this);
		}
		else {
			this.broadcastCurrent();
		}
	}

	private void broadcastCurrent() {
		if(this.xRanges.isEmpty()) {
			StackViewManager.getInstance().broadcastXRangeReset(this);
		}
		else {
			Range r = this.xRanges.peek();
			StackViewManager.getInstance().broadcastXRange(this, new Range(r.getStart(), r.getEnd()));
		}
	}
}
