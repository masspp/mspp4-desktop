package ninja.mspp.service.io;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import ninja.mspp.core.api.io.model.Annotation;
import ninja.mspp.core.api.io.model.Scan;
import ninja.mspp.core.model.ms.Peak;
import ninja.mspp.core.model.ms.PeakList;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.service.io.model.IOSample;
import ninja.mspp.service.io.model.IOSpectrum;

public class IOServiceManager {
	private static IOServiceManager instance;
	
	private Map<String, IOSample> sampleMap;
	private Map<String, PeakList> peakMap;
	
	private IOServiceManager() {
		this.sampleMap = new HashMap<>();
		this.peakMap = new HashMap<>();
	}
	
	public Sample getSample(String id) {
		IOSample sample = sampleMap.get(id);
		sample.sortScans();
		
		return sample;
	}
	
	public Sample createSample() {
		IOSample sample = new IOSample();
		sampleMap.put(sample.getId(), sample);
		return sample;
	}
	
	public void addScan(String id, Scan scan) {
		Sample sample = sampleMap.get(id);
		
		Spectrum spectrum = new IOSpectrum(sample, sample.getSpectra().size() + 1, scan);
		sample.getSpectra().add(spectrum);
	}
	
	public PeakList getPeakList(String id) {
		return peakMap.get(id);
	}
	
	public void addAnnotation(String id, Annotation annotation) {
		PeakList peakList = peakMap.get(id);
		if (peakList == null) {
			peakList = new PeakList();
			peakMap.put(id, peakList);
		}
		
		double mz = annotation.getMass();
		double intensity = annotation.getIntensity();
		String base64 = annotation.getImage();
		int index = (int)Math.round(mz / 0.01);
		
		Peak peak = null;
		for(Peak current : peakList) {
			int currentIndex = (int)Math.round(current.getX() / 0.01);
			if (currentIndex == index) {
				peak = current;
			}
		}
		
		if(peak == null) {
			peak = new Peak(mz, intensity, mz, mz);
			peakList.add(peak);
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
	
	
	public static IOServiceManager getInstance() {
		if(instance == null) {
			instance = new IOServiceManager();
		}
		return instance;
	}
}
