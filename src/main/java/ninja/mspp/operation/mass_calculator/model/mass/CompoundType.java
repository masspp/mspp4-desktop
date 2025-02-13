package ninja.mspp.operation.mass_calculator.model.mass;

public enum CompoundType {
	CHEMICAL_COMPOSITION("Chemical composition", "C6H12O6"),
	PEPTIDE("Peptide sequence", "PEPTIDE"),
	GLYCAN_COMPOSITION("Glycan composition", "HexNAc(2)Hex(3)"),
	GLYCOPEPTIDE("Glycopeptide format", "PEPTIDE|HexNAc(2)Hex(3)"),
	;

	private String name;
	private String eg;

	private CompoundType(String name, String eg) {
		this.name = name;
		this.eg = eg;
	}

	public String getName() {
		return this.name;
	}

	public String getEg() {
		return this.eg;
	}

	public static CompoundType fromName(String name) {
		for ( CompoundType type : CompoundType.values() ) {
			if ( type.getName().equals(name) ) {
				return type;
			}
		}
		return null;
	}
}
