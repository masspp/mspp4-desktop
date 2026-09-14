package ninja.mspp.view.mode.stack;

import java.util.ArrayList;
import java.util.List;

import ninja.mspp.core.model.view.Range;

/**
 * Holds the live state of the stack view: registered canvases per tab, the
 * currently shared X range, and the per-entry height. Used by StackedCanvas
 * subclasses to broadcast zoom/pan changes and to compute a shared Y scale.
 */
public class StackViewManager {
	public enum Kind { SPECTRUM, CHROMATOGRAM }

	private static StackViewManager instance;

	private final List<StackedCanvas> spectrumCanvases = new ArrayList<StackedCanvas>();
	private final List<StackedCanvas> chromatogramCanvases = new ArrayList<StackedCanvas>();
	private Range spectrumXRange;
	private Range chromatogramXRange;
	private double entryHeight = 150.0;
	private boolean ySync = true;

	private StackViewMode controller;

	private StackViewManager() {
	}

	public static StackViewManager getInstance() {
		if(instance == null) {
			instance = new StackViewManager();
		}
		return instance;
	}

	List<StackedCanvas> getCanvases(Kind kind) {
		return kind == Kind.SPECTRUM ? this.spectrumCanvases : this.chromatogramCanvases;
	}

	Range getSharedXRange(Kind kind) {
		return kind == Kind.SPECTRUM ? this.spectrumXRange : this.chromatogramXRange;
	}

	void setSharedXRange(Kind kind, Range range) {
		if(kind == Kind.SPECTRUM) {
			this.spectrumXRange = range;
		}
		else {
			this.chromatogramXRange = range;
		}
	}

	void register(StackedCanvas canvas) {
		this.getCanvases(canvas.getKind()).add(canvas);
		Range shared = this.getSharedXRange(canvas.getKind());
		if(shared != null) {
			canvas.setSyncedXRange(shared);
		}
	}

	void unregister(StackedCanvas canvas) {
		this.getCanvases(canvas.getKind()).remove(canvas);
	}

	void broadcastXRange(StackedCanvas source, Range range) {
		this.setSharedXRange(source.getKind(), range);
		for(StackedCanvas c : new ArrayList<StackedCanvas>(this.getCanvases(source.getKind()))) {
			if(c != source) {
				c.setSyncedXRange(range);
			}
		}
	}

	void broadcastXRangeReset(StackedCanvas source) {
		this.setSharedXRange(source.getKind(), null);
		for(StackedCanvas c : new ArrayList<StackedCanvas>(this.getCanvases(source.getKind()))) {
			if(c != source) {
				c.clearSyncedXRange();
			}
		}
	}

	void redrawAll(Kind kind) {
		for(StackedCanvas c : new ArrayList<StackedCanvas>(this.getCanvases(kind))) {
			c.refresh();
		}
	}

	public boolean isYSync() {
		return this.ySync;
	}

	public void setYSync(boolean ySync) {
		this.ySync = ySync;
	}

	public double getEntryHeight() {
		return this.entryHeight;
	}

	public void setEntryHeight(double height) {
		this.entryHeight = height;
	}

	public StackViewMode getController() {
		return this.controller;
	}

	public void setController(StackViewMode controller) {
		this.controller = controller;
	}
}
