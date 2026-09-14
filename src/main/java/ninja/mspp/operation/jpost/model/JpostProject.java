package ninja.mspp.operation.jpost.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A project entry returned by the jPOST repository {@code /_api/project} endpoint.
 *
 * <p>Unlike MB-POST, jPOST keeps the descriptive metadata (title, PI, keywords...)
 * in a generic {@code elements} array keyed by {@code elementCategory}; the
 * convenience getters below pull individual values out of that array so they can
 * be bound from FXML via {@code PropertyValueFactory}.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JpostProject {
	@JsonProperty("jpostId")
	private String jpostId;
	@JsonProperty("projectId")
	private String projectId;
	@JsonProperty("location")
	private String location;
	@JsonProperty("userName")
	private String userName;
	@JsonProperty("revision")
	private String revision;
	@JsonProperty("currentRevision")
	private String currentRevision;
	@JsonProperty("status")
	private String status;
	@JsonProperty("type")
	private String type;
	@JsonProperty("mode")
	private String mode;
	@JsonProperty("pxid")
	private String pxid;
	@JsonProperty("announcementDate")
	private String announcementDate;
	@JsonProperty("createdDate")
	private String createdDate;
	@JsonProperty("modifiedDate")
	private String modifiedDate;
	@JsonProperty("total")
	private int total;
	@JsonProperty("elements")
	private List<Element> elements;

	public String getJpostId() { return jpostId; }
	public String getProjectId() { return projectId; }
	public String getLocation() { return location; }
	public String getUserName() { return userName; }
	public String getSubmitter() { return userName; }
	public String getRevision() { return revision; }
	public String getCurrentRevision() { return currentRevision; }
	public String getStatus() { return status; }
	public String getType() { return type; }
	public String getMode() { return mode; }
	public String getPxid() { return pxid; }
	public String getAnnouncementDate() { return announcementDate; }
	public String getCreatedDate() { return createdDate; }
	public String getModifiedDate() { return modifiedDate; }
	public int getTotal() { return total; }
	public List<Element> getElements() {
		return elements == null ? Collections.<Element>emptyList() : elements;
	}

	// ---- values extracted from the generic elements array ----

	public String getTitle() { return element("title"); }
	public String getKeywords() { return element("keywords"); }
	public String getDescription() { return element("description"); }
	public String getPrincipalInvestigator() { return element("pi"); }
	public String getAffiliation() { return element("affiliation"); }
	public String getNote() { return element("note"); }
	public String getPmid() { return element("pmid"); }

	private String element(String category) {
		for(Element e : this.getElements()) {
			if(category.equals(e.getElementCategory())) {
				return e.getValueName();
			}
		}
		return null;
	}

	public boolean matchesKeyword(String keyword) {
		if(keyword == null || keyword.isEmpty()) {
			return true;
		}
		String k = keyword.toLowerCase();
		return contains(this.jpostId, k)
			|| contains(this.pxid, k)
			|| contains(this.userName, k)
			|| contains(this.getTitle(), k)
			|| contains(this.getKeywords(), k)
			|| contains(this.getDescription(), k)
			|| contains(this.getPrincipalInvestigator(), k)
			|| contains(this.getAffiliation(), k);
	}

	private static boolean contains(String s, String lowerNeedle) {
		return s != null && s.toLowerCase().contains(lowerNeedle);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Element {
		@JsonProperty("elementId")
		private String elementId;
		@JsonProperty("elementCategory")
		private String elementCategory;
		@JsonProperty("valueId")
		private String valueId;
		@JsonProperty("valueName")
		private String valueName;

		public String getElementId() { return elementId; }
		public String getElementCategory() { return elementCategory; }
		public String getValueId() { return valueId; }
		public String getValueName() { return valueName; }
	}
}
