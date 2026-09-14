package ninja.mspp.operation.jpost.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A file entry returned by the jPOST repository {@code /_api/file} endpoint.
 *
 * <p>{@code fileSize} arrives as a numeric string; {@link #getSizeBytes()} exposes
 * it as a {@code long} for size formatting and table sorting. The {@code elements}
 * array carries the experimental presets (sample / fractionation / enzyme / MS mode)
 * shown in the file detail pane.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JpostFile {
	@JsonProperty("fileId")
	private String fileId;
	@JsonProperty("fileName")
	private String fileName;
	@JsonProperty("fileSize")
	private String fileSize;
	@JsonProperty("fileType")
	private String fileType;
	@JsonProperty("status")
	private String status;
	@JsonProperty("checksum")
	private String checksum;
	@JsonProperty("projectId")
	private String projectId;
	@JsonProperty("location")
	private String location;
	@JsonProperty("isArchived")
	private boolean isArchived;
	@JsonProperty("total")
	private int total;
	@JsonProperty("totalSize")
	private long totalSize;
	@JsonProperty("elements")
	private List<Preset> elements;

	public String getFileId() { return fileId; }
	public String getFileName() { return fileName; }
	public String getFileSize() { return fileSize; }
	public String getFileType() { return fileType; }
	public String getStatus() { return status; }
	public String getChecksum() { return checksum; }
	public String getProjectId() { return projectId; }
	public String getLocation() { return location; }
	public boolean isArchived() { return isArchived; }
	public int getTotal() { return total; }
	public long getTotalSize() { return totalSize; }
	public List<Preset> getElements() {
		return elements == null ? Collections.<Preset>emptyList() : elements;
	}

	public long getSizeBytes() {
		if(this.fileSize == null || this.fileSize.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(this.fileSize.trim());
		}
		catch(NumberFormatException e) {
			return 0L;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Preset {
		@JsonProperty("software")
		private String software;
		@JsonProperty("format")
		private String format;
		@JsonProperty("sampleTitle")
		private String sampleTitle;
		@JsonProperty("fractionationTitle")
		private String fractionationTitle;
		@JsonProperty("enzymeModTitle")
		private String enzymeModTitle;
		@JsonProperty("msModeTitle")
		private String msModeTitle;
		@JsonProperty("labelText")
		private String labelText;

		public String getSoftware() { return software; }
		public String getFormat() { return format; }
		public String getSampleTitle() { return sampleTitle; }
		public String getFractionationTitle() { return fractionationTitle; }
		public String getEnzymeModTitle() { return enzymeModTitle; }
		public String getMsModeTitle() { return msModeTitle; }
		public String getLabelText() { return labelText; }
	}
}
