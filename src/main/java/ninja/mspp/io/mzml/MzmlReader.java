package ninja.mspp.io.mzml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.Chromatogram;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.RawDataFile;
import io.github.msdk.io.mzml.MzMLFileImportMethod;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;



public class MzmlReader {
	public Sample read(String path) throws MSDKException {
		File file = new File(path);
		MzMLFileImportMethod importer = new MzMLFileImportMethod(file);
		RawDataFile rawFile = importer.execute();
		Sample sample = new Sample(file.getAbsolutePath(), file.getName());
		
		List<Chromatogram> chromatograms = rawFile.getChromatograms();
		for (Chromatogram chromatogram : chromatograms) {
			MzmlChromatogram mzmlChromatogram = new MzmlChromatogram(sample, chromatogram);
			sample.getChromatograms().add(mzmlChromatogram);
		}
		
		Map<Integer, Spectrum> map = new HashMap<Integer, Spectrum>();
		List<MzmlSpectrum> list = new ArrayList<MzmlSpectrum>();
		
		List<MsScan> scans = rawFile.getScans();
		for(MsScan scan : scans) {
			MzmlSpectrum mzmlSpectrum = new MzmlSpectrum(sample, scan);
			sample.getSpectra().add(mzmlSpectrum);
			list.add(mzmlSpectrum);
			map.put(
				mzmlSpectrum.getScanNumber(),
				mzmlSpectrum
			);
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
}
