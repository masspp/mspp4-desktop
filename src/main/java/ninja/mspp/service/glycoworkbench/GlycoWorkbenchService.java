package ninja.mspp.service.glycoworkbench;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.OnOpenSample;
import ninja.mspp.core.annotation.method.OnSelectSample;
import ninja.mspp.core.annotation.method.OnSelectSpectrum;
import ninja.mspp.core.annotation.method.Service;
import ninja.mspp.core.api.glycoworkbench.model.GlycoWorkbenchData;
import ninja.mspp.core.api.glycoworkbench.model.Scan;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.core.model.ms.Spectrum;

@Listener("GlycoWorkbench Service")
public class GlycoWorkbenchService {
	@Service("glycoworkbench")
	public static String open(String data) throws JsonProcessingException {
		MsppManager manager = MsppManager.getInstance();
		
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<GlycoWorkbenchData> typeRef = new TypeReference<GlycoWorkbenchData>() {};
		GlycoWorkbenchData glycoWorkbenchData = mapper.readValue(data, typeRef);
		
		int index = glycoWorkbenchData.getCurrentIndex();
		Sample sample = Scan.createSample(glycoWorkbenchData.getScans());
		Spectrum spectrum = sample.getSpectra().get(index);
		
		manager.invoke(OnOpenSample.class, sample);
		manager.invoke(OnSelectSample.class, sample);
		manager.invoke(OnSelectSpectrum.class, spectrum);
		
		Map<String, String> result = new HashMap<String, String>();
		result.put("success",  "true");
		String response = mapper.writeValueAsString(result);
		return response;
	}
}
