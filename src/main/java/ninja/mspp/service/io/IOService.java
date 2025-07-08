package ninja.mspp.service.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.OnOpenSample;
import ninja.mspp.core.annotation.method.OnSelectSample;
import ninja.mspp.core.annotation.method.OnSelectSpectrum;
import ninja.mspp.core.annotation.method.Service;
import ninja.mspp.core.api.io.model.Annotation;
import ninja.mspp.core.api.io.model.Scan;
import ninja.mspp.core.api.io.model.ScanPoint;
import ninja.mspp.core.model.PeakManager;
import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.PeakList;
import ninja.mspp.core.model.ms.Point;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.core.model.ms.TicChromatogram;

@Listener("IO Service")
public class IOService {
	@Service("io_create_sample")
	public static String createSample(String request) throws Exception {
		ObjectMapper mapper = new ObjectMapper();

		IOServiceManager manager = IOServiceManager.getInstance();
		Sample sample = manager.createSample();
		String id = sample.getId();

		Map<String, String> map = new HashMap<String, String>();
		map.put("id", id);

		String response = mapper.writeValueAsString(map);
		return response;
	}

	@Service("io_add_scan")
	public static String addScan(String request) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		IOServiceManager manager = IOServiceManager.getInstance();

		TypeReference<Scan> typeRef = new TypeReference<Scan>() {
		};
		Scan scan = mapper.readValue(request, typeRef);
		String id = scan.getId();

		manager.addScan(id, scan);

		Map<String, String> result = new HashMap<String, String>();
		result.put("success", "true");
		String response = mapper.writeValueAsString(result);

		return response;
	}

	@Service("io_flush")
	public static String flush(String request) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> requestMap = mapper.readValue(request, new TypeReference<Map<String, String>>() {
		});

		String id = requestMap.get("id");
		int index = Integer.parseInt(requestMap.get("index"));

		IOServiceManager manager = IOServiceManager.getInstance();
		Sample sample = manager.getSample(id);

		if (sample != null) {
			MsppManager msppManager = MsppManager.getInstance();
			PeakManager peakManager = PeakManager.getInstance();

			Spectrum spectrum = sample.getSpectra().get(index);

			PeakList peakList = manager.getPeakList(id);
			if (peakList != null) {
				peakList.sort(
						(p1, p2) -> {
							double mz1 = p1.getX();
							double mz2 = p2.getX();

							if (mz1 < mz2) {
								return -1;
							} else if (mz1 > mz2) {
								return 1;
							}
							return 0;
						});
			}

			for (Spectrum s : sample.getSpectra()) {
				peakManager.setPeaks(s, peakList);
			}

			if (sample.getSpectra().size() > 2) {
				TicChromatogram ticChromatogram = new TicChromatogram(sample);
				sample.getChromatograms().add(ticChromatogram);
			}

			msppManager.invoke(OnOpenSample.class, sample);
			msppManager.invoke(OnSelectSample.class, sample);
			msppManager.invoke(OnSelectSpectrum.class, spectrum);
		}

		Map<String, String> result = new HashMap<String, String>();
		result.put("success", "true");
		String response = mapper.writeValueAsString(result);

		return response;
	}

	@Service("io_add_annotation")
	public static String addAnnotation(String request) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<List<Annotation>> typeRef = new TypeReference<List<Annotation>>() {
		};

		IOServiceManager manager = IOServiceManager.getInstance();

		List<Annotation> annotations = mapper.readValue(request, typeRef);

		for (Annotation annotation : annotations) {
			String id = annotation.getId();
			manager.addAnnotation(id, annotation);
		}

		Map<String, String> result = new HashMap<String, String>();
		result.put("success", "true");
		String response = mapper.writeValueAsString(result);

		return response;
	}

	@Service("io_get_spectra_count")
	public static String getSpectraCount(String request) throws JsonProcessingException {
		MsppManager manager = MsppManager.getInstance();

		Sample sample = manager.getActiveSample();

		Map<String, String> map = new HashMap<String, String>();
		map.put("count", Integer.toString(sample.getSpectra().size()));

		ObjectMapper mapper = new ObjectMapper();
		String response = mapper.writeValueAsString(map);
		return response;
	}

	@Service("io_get_current_index")
	public static String getCurrentIndex(String request) throws JsonProcessingException {
		MsppManager manager = MsppManager.getInstance();

		Sample sample = manager.getActiveSample();
		Spectrum activeSpectrum = manager.getActiveSpectrum();
		int currentIndex = 0;
		int index = -1;

		if (sample != null) {
			for (Spectrum spectrum : sample.getSpectra()) {
				if (spectrum == activeSpectrum) {
					currentIndex = index;
				}
				index++;
			}
		}

		Map<String, String> map = new HashMap<String, String>();
		map.put("index", Integer.toString(currentIndex));

		ObjectMapper mapper = new ObjectMapper();
		String response = mapper.writeValueAsString(map);
		return response;
	}

	@Service("io_get_spectrum")
	public static String getSpectrum(String request) throws JsonProcessingException {
		System.out.println("Request:---- " + request);
		MsppManager manager = MsppManager.getInstance();

		ObjectMapper requestMapper = new ObjectMapper();

		Map<String, String> requestMap = requestMapper.readValue(request,
				new TypeReference<Map<String, String>>() {
				});

		int index = Integer.parseInt(requestMap.get("index"));

		Sample sample = manager.getActiveSample();
		Spectrum spectrum = sample.getSpectra().get(index);

		Scan scan = new Scan();
		List<ScanPoint> scanPoints = new ArrayList<ScanPoint>();
		DataPoints points = spectrum.readDataPoints();
		for (Point point : points) {
			ScanPoint scanPoint = new ScanPoint();
			scanPoint.setX(point.getX());
			scanPoint.setY(point.getY());
			scanPoints.add(scanPoint);
		}

		scan.setPoints(scanPoints);
		scan.setCentroidMode(spectrum.isCentroidMode());
		scan.setMaxMz(spectrum.getMaxMz());
		scan.setMinMz(spectrum.getMinMz());
		scan.setMsLevel(spectrum.getMsLevel());
		scan.setPrecursorMz(spectrum.getPrecursorMass());
		scan.setRt(spectrum.getRt());

		ObjectMapper mapper = new ObjectMapper();
		String response = mapper.writeValueAsString(scan);

		System.out.println(response);

		return response;
	}
}
