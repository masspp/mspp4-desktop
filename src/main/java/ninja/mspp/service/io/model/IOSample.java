package ninja.mspp.service.io.model;

import java.util.UUID;

import ninja.mspp.core.model.ms.Sample;

public class IOSample extends Sample {
	private static int counter = 1;
	
	public IOSample() {
		super(
			UUID.randomUUID().toString(),
			"External Data " + (counter++)				
		);
	}
	
	public void sortScans() {
		this.getSpectra().sort(
			(s1, s2) -> {
				int scan1 = s1.getScanNumber();
				int scan2 = s2.getScanNumber();
				
				return (scan1 - scan2);
			}
		);
	}
}
