package ninja.mspp.io.heatmap;

import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.OnHeatMap;
import ninja.mspp.core.annotation.method.OnSelectSample;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.core.model.view.HeatMap;
import ninja.mspp.interfaces.Job;

@Listener("Heatmap Listener")
public class HeatMapListener {
	@OnSelectSample
	public void onSelectSample(Sample sample) throws InterruptedException {
		int count = 0;
		for (Spectrum spectrum : sample.getSpectra()) {
			if (spectrum.getMsLevel() == 1 && spectrum.getRt() > 0.0) {
				count++;
			}
		}

		if(count > 1) {
			MsppManager manager = MsppManager.getInstance();
			Job job = new Job() {
				@Override
				public Object execute() {
					HeatMap heatmap = new HeatMap(sample.getSpectra());
					return heatmap;
				}

				@Override
				public void onSucceeded(Object result) {
					HeatMap heatmap = (HeatMap)result;
					manager.invoke(OnHeatMap.class, heatmap);
				}
			};
			manager.startTask(job);
		}
	}
}
