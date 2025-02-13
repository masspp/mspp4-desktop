package ninja.mspp.service;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.OnSelectSpectrum;
import ninja.mspp.core.annotation.method.Service;
import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;

@Listener("Profile Service")
public class ProfileService {
	@Service("open_spectrum")
	public static String openSpectrum(Map<String, String> parameters) throws JsonProcessingException {
		MsppManager manager = MsppManager.getInstance();
		
		String data = parameters.get("data");
		DataPoints points = DataPoints.fromString(data);
		Sample sample = new ProfileSample();
		Spectrum spectrum = new ProfileSpectrum(sample, points);
		
		manager.invoke(OnSelectSpectrum.class, spectrum);
		
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> map = new HashMap<String, String>();
		map.put("result", "success");
		
		String json = mapper.writeValueAsString(map);
		return json;
	}
}
