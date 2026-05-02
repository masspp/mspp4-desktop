package ninja.mspp.operation.mb_post.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MbPostFile {
	@JsonProperty("id")
	private String id;
	@JsonProperty("name")
	private String name;
	@JsonProperty("size")
	private long size;
	@JsonProperty("type")
	private String type;
	@JsonProperty("status")
	private String status;
	@JsonProperty("checksum")
	private String checksum;

	public String getId() { return id; }
	public String getName() { return name; }
	public long getSize() { return size; }
	public String getType() { return type; }
	public String getStatus() { return status; }
	public String getChecksum() { return checksum; }
}
