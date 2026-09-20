package ninja.mspp.io.heatmap;

import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.OnHeatMap;
import ninja.mspp.core.annotation.method.OnSelectSample;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.core.model.view.HeatMap;
import ninja.mspp.interfaces.Job;
import ninja.mspp.view.GuiManager;
import ninja.mspp.view.panel.HeatMapCanvas;

@Listener("Heatmap Listener")
public class HeatMapListener {
	// Incremented for each selected sample, so that only the heatmap of the latest one is shown.
	private long request;

	@OnSelectSample
	public void onSelectSample(Sample sample) throws InterruptedException {
		MsppManager manager = MsppManager.getInstance();
		final long current = ++this.request;

		int count = 0;
		for (Spectrum spectrum : sample.getSpectra()) {
			if (spectrum.getMsLevel() == 1 && spectrum.getRt() > 0.0) {
				count++;
			}
		}

		// Clear the heatmap of the previous sample, and show "Now Loading..." while calculating.
		manager.setStatus(HeatMapCanvas.LOADING_STATUS, count > 1 ? "true" : "false");
		manager.invoke(OnHeatMap.class, (Object)null);

		if(count > 1) {
			GuiManager guiManager = GuiManager.getInstance();

			Job job = new Job() {
				@Override
				public Object execute() {
					try {
						return new HeatMap(sample.getSpectra());
					}
					catch(Exception e) {
						e.printStackTrace();
						return null;
					}
				}

				@Override
				public void onSucceeded(Object result) {
					if(current != request) {
						return;
					}
					manager.setStatus(HeatMapCanvas.LOADING_STATUS, "false");
					manager.invoke(OnHeatMap.class, result);
				}
			};
			guiManager.startTask(job);
		}
	}
}
