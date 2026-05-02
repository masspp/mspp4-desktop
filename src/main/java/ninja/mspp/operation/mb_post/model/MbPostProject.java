package ninja.mspp.operation.mb_post.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MbPostProject {
	@JsonProperty("mbpostId")
	private String mbpostId;
	@JsonProperty("location")
	private String location;
	@JsonProperty("title")
	private String title;
	@JsonProperty("keywords")
	private String keywords;
	@JsonProperty("description")
	private String description;
	@JsonProperty("principalInvestigator")
	private String principalInvestigator;
	@JsonProperty("affiliation")
	private String affiliation;
	@JsonProperty("submitter")
	private String submitter;
	@JsonProperty("announcementDate")
	private String announcementDate;
	@JsonProperty("createdAt")
	private String createdAt;
	@JsonProperty("modifiedAt")
	private String modifiedAt;
	@JsonProperty("fileCount")
	private int fileCount;
	@JsonProperty("revision")
	private int revision;
	@JsonProperty("pubmedId")
	private String pubmedId;
	@JsonProperty("publicationType")
	private String publicationType;
	@JsonProperty("note")
	private String note;
	@JsonProperty("status")
	private String status;

	public String getMbpostId() { return mbpostId; }
	public String getLocation() { return location; }
	public String getTitle() { return title; }
	public String getKeywords() { return keywords; }
	public String getDescription() { return description; }
	public String getPrincipalInvestigator() { return principalInvestigator; }
	public String getAffiliation() { return affiliation; }
	public String getSubmitter() { return submitter; }
	public String getAnnouncementDate() { return announcementDate; }
	public String getCreatedAt() { return createdAt; }
	public String getModifiedAt() { return modifiedAt; }
	public int getFileCount() { return fileCount; }
	public int getRevision() { return revision; }
	public String getPubmedId() { return pubmedId; }
	public String getPublicationType() { return publicationType; }
	public String getNote() { return note; }
	public String getStatus() { return status; }

	public boolean matchesKeyword(String keyword) {
		if(keyword == null || keyword.isEmpty()) {
			return true;
		}
		String k = keyword.toLowerCase();
		return contains(this.mbpostId, k)
			|| contains(this.title, k)
			|| contains(this.keywords, k)
			|| contains(this.description, k)
			|| contains(this.principalInvestigator, k)
			|| contains(this.affiliation, k)
			|| contains(this.submitter, k);
	}

	private static boolean contains(String s, String lowerNeedle) {
		return s != null && s.toLowerCase().contains(lowerNeedle);
	}
}
