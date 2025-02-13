package ninja.mspp.operation.mass_calculator.model.mass;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.expasy.mzjava.proteomics.mol.Peptide;
import org.glycoinfo.ms.GlycanMassUtility.om.Composition;
import org.glycoinfo.ms.GlycanMassUtility.om.IMassElement;
import org.glycoinfo.ms.GlycanMassUtility.om.Molecule;
import org.glycoinfo.ms.GlycanMassUtility.utils.MassElementCreator;

public class CompoundCreator {

	public static IMassElement create(String name, CompoundType type) throws IllegalArgumentException {
		switch (type) {
		case CHEMICAL_COMPOSITION:
			return createChemicalCompositionMassElement(name);
		case PEPTIDE:
			return createPeptideMassElement(name);
		case GLYCAN_COMPOSITION:
			return createGlycanCompositionMassElement(name);
		case GLYCOPEPTIDE:
			return createGlycopeptideMassElement(name);
		default:
			throw new IllegalArgumentException("Unsupported mass element type.");
		}
	}

	private static IMassElement createChemicalCompositionMassElement(String name) throws IllegalArgumentException {
		return Molecule.parse(name);
	}

	private static IMassElement createPeptideMassElement(String name) throws IllegalArgumentException {
		Peptide pep = Peptide.parse(name);
		double mass = pep.getMolecularMass();
		return MassElementCreator.createMassElement(name, new BigDecimal(mass), new BigDecimal(mass));
	}

	private static IMassElement createGlycanCompositionMassElement(String name) throws IllegalArgumentException {
		return Composition.parse(name);
	}

	private static IMassElement createGlycopeptideMassElement(String name) throws IllegalArgumentException {
		String[] parts = name.split("\\|");
		if (parts.length != 2)
			throw new IllegalArgumentException("Invalid glycopeptide format.");

		List<IMassElement> elements = new ArrayList<>();
		elements.add( createPeptideMassElement(parts[0]) );
		elements.add( createGlycanCompositionMassElement(parts[1]) );

		return MassElementCreator.createComplexMassElement(elements);
	}

}
