package ninja.mspp.operation.mb_post.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MbPostFileDetail {
	@JsonProperty("id")
	private String id;
	@JsonProperty("name")
	private String name;
	@JsonProperty("size")
	private long size;
	@JsonProperty("type")
	private String type;
	@JsonProperty("checksum")
	private String checksum;
	@JsonProperty("is_on_server")
	private int isOnServer;
	@JsonProperty("presets")
	private List<PresetGroup> presets;

	public String getId() { return id; }
	public String getName() { return name; }
	public long getSize() { return size; }
	public String getType() { return type; }
	public String getChecksum() { return checksum; }
	public boolean isOnServer() { return isOnServer != 0; }
	public List<PresetGroup> getPresets() {
		return presets == null ? Collections.<PresetGroup>emptyList() : presets;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PresetGroup {
		@JsonProperty("id")
		private String id;
		@JsonProperty("category")
		private String category;
		@JsonProperty("presets")
		private List<PresetItem> presets;

		public String getId() { return id; }
		public String getCategory() { return category; }
		public List<PresetItem> getPresets() {
			return presets == null ? Collections.<PresetItem>emptyList() : presets;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PresetItem {
		@JsonProperty("key")
		private String key;
		@JsonProperty("label")
		private String label;
		@JsonProperty("value")
		private String value;
		@JsonProperty("ontologyValue")
		private String ontologyValue;

		public String getKey() { return key; }
		public String getLabel() { return label; }
		public String getValue() { return value; }
		public String getOntologyValue() { return ontologyValue; }
	}
}
