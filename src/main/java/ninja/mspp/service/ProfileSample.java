package ninja.mspp.service;

import java.util.UUID;

import ninja.mspp.core.model.ms.Sample;

public class ProfileSample extends Sample {
	public ProfileSample() {
		super(UUID.randomUUID().toString(), "dummy");
	}
}
