package ninja.mspp.misc.cycling;

import java.io.IOException;

import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.ChromatogramAction;
import ninja.mspp.core.annotation.method.ChromatogramCanvasForeground;
import ninja.mspp.core.annotation.method.SpectrumAction;
import ninja.mspp.core.annotation.method.SpectrumCanvasForeground;
import ninja.mspp.core.model.ms.Chromatogram;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.core.view.ChromatogramActionEvent;
import ninja.mspp.core.view.DrawInfo;
import ninja.mspp.core.view.SpectrumActionEvent;

@Listener("Cycling")
public class CyclingListener {	
	@ChromatogramAction(value="Cycling", order=10)
	public void startChromatogram(ChromatogramActionEvent event) throws IOException {
		CyclingManager manager = CyclingManager.getInstance();
		manager.startChromatogram(event.getChromatogram());
	}
	
	@ChromatogramCanvasForeground
	public void drawChromatogram(DrawInfo<Chromatogram> info) throws IOException {
		CyclingManager manager = CyclingManager.getInstance();
		manager.drawChromatogram(info);
	}
	
	@SpectrumAction(value="Cycling", order=10)
	public void startSpectrum(SpectrumActionEvent event) throws IOException {
		CyclingManager manager = CyclingManager.getInstance();
		manager.startSpectrum(event.getSpectrum());
	}
	
	@SpectrumCanvasForeground
	public void drawSpectrum(DrawInfo<Spectrum> info) throws IOException {
		CyclingManager manager = CyclingManager.getInstance();
		manager.drawSpectrum(info);
	}	
}
