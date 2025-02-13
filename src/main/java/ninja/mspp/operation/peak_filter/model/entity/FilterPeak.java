package ninja.mspp.operation.peak_filter.model.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class FilterPeak {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private Long setId;
	private String name;
	private Double mz;
	private String color;
	private Boolean neutralLoss;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getSetId() {
		return setId;
	}
	public void setSetId(Long setId) {
		this.setId = setId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Double getMz() {
		return mz;
	}
	public void setMz(Double mz) {
		this.mz = mz;
	}
	public String getColor() {
		return color;
	}
	public void setColor(String color) {
		this.color = color;
	}
	public Boolean getNeutralLoss() {
		return neutralLoss;
	}
	public void setNeutralLoss(Boolean neutralLoss) {
		this.neutralLoss = neutralLoss;
	}
}
