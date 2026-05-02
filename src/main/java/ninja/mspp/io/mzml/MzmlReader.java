package ninja.mspp.io.mzml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.Chromatogram;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.RawDataFile;
import io.github.msdk.io.mzml.MzMLFileImportMethod;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.io.mzml.convert.VendorConverter;



public class MzmlReader {
	public Sample read(String path) throws MSDKException {
		return this.read(path, null, null);
	}

	public Sample read(String path, DoubleConsumer progressListener) throws MSDKException {
		return this.read(path, progressListener, null);
	}

	public Sample read(
			String path,
			DoubleConsumer progressListener,
			Consumer<String> statusListener
	) throws MSDKException {
		File originalFile = new File(path);
		File mzmlFile = this.ensureMzml(originalFile);
		MzMLFileImportMethod importer = new MzMLFileImportMethod(mzmlFile);

		AtomicBoolean stopPoll = new AtomicBoolean(false);
		Thread poller = null;
		if(progressListener != null) {
			poller = new Thread(() -> {
				while(!stopPoll.get()) {
					Float pct = importer.getFinishedPercentage();
					if(pct != null) {
						double v = pct.floatValue();
						if(v > 1.0) {
							v = v / 100.0;
						}
						if(v > 0.95) {
							v = 0.95;
						}
						progressListener.accept(v);
					}
					try {
						Thread.sleep(100L);
					}
					catch(InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
			}, "mzml-progress-poller");
			poller.setDaemon(true);
			poller.start();
		}

		RawDataFile rawFile;
		try {
			rawFile = importer.execute();
		}
		finally {
			stopPoll.set(true);
			if(poller != null) {
				poller.interrupt();
			}
		}
		if(statusListener != null) {
			statusListener.accept("Indexing binary arrays...");
		}
		MzmlBinaryIndex binaryIndex;
		try {
			binaryIndex = new MzmlBinaryIndex(mzmlFile);
		}
		catch(java.io.IOException e) {
			throw new MSDKException(e);
		}

		if(statusListener != null) {
			statusListener.accept("Building sample (" + rawFile.getScans().size() + " scans)...");
		}
		Sample sample = new Sample(originalFile.getAbsolutePath(), originalFile.getName());

		List<Chromatogram> chromatograms = rawFile.getChromatograms();
		int chromIdx = 0;
		for (Chromatogram chromatogram : chromatograms) {
			MzmlChromatogram mzmlChromatogram = new MzmlChromatogram(sample, chromatogram);
			mzmlChromatogram.setBinaryIndex(binaryIndex, chromIdx);
			sample.getChromatograms().add(mzmlChromatogram);
			chromIdx++;
		}

		Map<Integer, Spectrum> map = new HashMap<Integer, Spectrum>();
		List<MzmlSpectrum> list = new ArrayList<MzmlSpectrum>();

		List<MsScan> scans = rawFile.getScans();
		int total = scans.size();
		int idx = 0;
		for(MsScan scan : scans) {
			MzmlSpectrum mzmlSpectrum = new MzmlSpectrum(sample, scan);
			mzmlSpectrum.setBinaryIndex(binaryIndex, idx);
			sample.getSpectra().add(mzmlSpectrum);
			list.add(mzmlSpectrum);
			map.put(
				mzmlSpectrum.getScanNumber(),
				mzmlSpectrum
			);
			idx++;
			if(progressListener != null && total > 0 && (idx % 64 == 0 || idx == total)) {
				progressListener.accept(0.95 + 0.05 * ((double) idx / total));
			}
		}

		for(MzmlSpectrum spectrum : list) {
			int precursorScan = spectrum.getPrecursorScanNumber();

			if(map.containsKey(precursorScan)) {
				Spectrum precursor = map.get(precursorScan);
				spectrum.setPrecursor(precursor);
				precursor.addProduct(spectrum);
			}
		}
		
		return sample;
	}

	private File ensureMzml(File file) throws MSDKException {
		if(!VendorConverter.isVendorFile(file)) {
			return file;
		}
		try {
			return new VendorConverter().convertToMzml(file);
		}
		catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MSDKException(e);
		}
		catch(Exception e) {
			throw new MSDKException(e);
		}
	}
}
