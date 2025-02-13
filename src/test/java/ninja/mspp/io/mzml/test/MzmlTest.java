package ninja.mspp.io.mzml.test;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import org.junit.Test;

import io.github.msdk.MSDKException;
import ninja.mspp.core.model.ms.Sample;
import ninja.mspp.io.mzml.MzmlReader;

public class MzmlTest {
    private String test_file_path;
	@Test
	public void test() throws MSDKException, UnsupportedEncodingException {
		String file = "data/small.pwiz.1.1.mzML";
		URL testFile = getClass().getClassLoader().getResource(file);
		assertNotNull(testFile);
		MzmlReader reader = new MzmlReader();
		test_file_path = URLDecoder.decode(testFile.getPath(), "UTF-8");
		Sample sample = reader.read(test_file_path);
	}

}
