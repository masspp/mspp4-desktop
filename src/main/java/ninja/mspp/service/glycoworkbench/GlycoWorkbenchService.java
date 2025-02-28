package ninja.mspp.service.glycoworkbench;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.OnOpenSample;
import ninja.mspp.core.annotation.method.OnSelectSample;
import ninja.mspp.core.annotation.method.OnSelectSpectrum;
import ninja.mspp.core.annotation.method.Service;
import ninja.mspp.core.api.glycoworkbench.model.Annotation;
import ninja.mspp.core.api.glycoworkbench.model.GlycoWorkbenchData;
import ninja.mspp.core.api.glycoworkbench.model.Scan;
import ninja.mspp.core.api.glycoworkbench.model.ScanPoint;
import ninja.mspp.core.model.PeakManager;
import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.Peak;
import ninja.mspp.core.model.ms.PeakList;
import ninja.mspp.core.model.ms.Point;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;

@Listener("GlycoWorkbench Service")
public class GlycoWorkbenchService {
	@Service("glycoworkbench_import")
	public static String open(String data) throws JsonProcessingException {
		MsppManager manager = MsppManager.getInstance();
		PeakManager peakManager = PeakManager.getInstance();
		
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<GlycoWorkbenchData> typeRef = new TypeReference<GlycoWorkbenchData>() {};
		GlycoWorkbenchData glycoWorkbenchData = mapper.readValue(data, typeRef);
				
		List<Annotation> annotations = glycoWorkbenchData.getAnnotations();
		PeakList peakList = new PeakList();
		Map<Integer, Peak> peakMap = new HashMap<Integer, Peak>();
		
		for(Annotation annotation: annotations) {
            double mz = annotation.getMass();
            double intensity = annotation.getIntensity();                       
            String base64 = annotation.getImage();
            
            int index = (int)Math.round(mz / 0.01);
            Peak peak = peakMap.get(index);
			if (peak == null) {
				peak = new Peak(mz, intensity, mz, mz);
				peakMap.put(index, peak);
			}

            Image image = null;
            if(!base64.isEmpty()) {
            	try {
            		byte[] bytes = Base64.getDecoder().decode(base64);
            		InputStream stream = new ByteArrayInputStream(bytes);
            		image = ImageIO.read(stream);
            		stream.close();
            		
            		Image orgImage = peak.getImage();
            		if(orgImage != null) {
            			image = mergeImage(orgImage, image);
            		}
            		peak.setImage(image);
            	}
				catch (Exception e) {
					e.printStackTrace();
				}
            }

            peakList.add(peak);
		}
		
		int index = glycoWorkbenchData.getCurrentIndex();
		Sample sample = Scan.createSample(glycoWorkbenchData.getScans());		
		
		for(Spectrum spectrum : sample.getSpectra()) {
            peakManager.setPeaks(spectrum, peakList);
		}
		
		Spectrum spectrum = sample.getSpectra().get(index);		
		
		manager.invoke(OnOpenSample.class, sample);
		manager.invoke(OnSelectSample.class, sample);
		manager.invoke(OnSelectSpectrum.class, spectrum);
		
		Map<String, String> result = new HashMap<String, String>();
		result.put("success",  "true");
		String response = mapper.writeValueAsString(result);
		return response;
	}
	
	private static Image mergeImage(Image image1, Image image2) {
		int width = Math.max(image1.getWidth(null), image2.getWidth(null));
		int height = image1.getHeight(null) + image2.getHeight(null);
		
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		
		g.setComposite(AlphaComposite.Clear);
		g.fillRect(0, 0, width, height);
		g.setComposite(AlphaComposite.SrcOver);
		
		g.drawImage(image1, 0, 0, null);
		g.drawImage(image2, 0, image1.getHeight(null), null);
		g.dispose();
		
		return image;
	}
	
	@Service("glycoworkbench_spectra_count")
	public static String getSpectraCount(String request) throws JsonProcessingException {
		MsppManager manager = MsppManager.getInstance();
		
		Sample sample = manager.getActiveSample();
		
		Map<String, String> map = new HashMap<String, String>();
		map.put("count", Integer.toString(sample.getSpectra().size()));
		
		ObjectMapper mapper = new ObjectMapper();
		String response = mapper.writeValueAsString(map);
		return response;
	}
	
	@Service("glycoworkbench_current_index")
	public static String getCurrentIndex(String request) throws JsonProcessingException {
		MsppManager manager = MsppManager.getInstance();

		Sample sample = manager.getActiveSample();		
		Spectrum activeSpectrum = manager.getActiveSpectrum();
		int currentIndex = 0;
		int index = -1;
		
		if(sample != null) {
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
	
	@Service("glycoworkbench_spectrum")
	public static String getSpectrum(String request) throws JsonProcessingException {
		MsppManager manager = MsppManager.getInstance();
		
		ObjectMapper requestMapper = new ObjectMapper();
		String unescapedJson = requestMapper.readValue(request, String.class);
		
		Map<String, String> requestMap = requestMapper.readValue(unescapedJson, new TypeReference<Map<String, String>>() {});
		
		int index = Integer.parseInt(requestMap.get("index"));
		
		Sample sample = manager.getActiveSample();
		Spectrum spectrum = sample.getSpectra().get(index);
		
		Scan scan = new Scan();
		List<ScanPoint> scanPoints = new ArrayList<ScanPoint>();
		DataPoints points = spectrum.readDataPoints();
		for(Point point : points) {
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
