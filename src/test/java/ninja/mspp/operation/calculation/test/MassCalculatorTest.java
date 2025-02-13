package ninja.mspp.operation.calculation.test;

import static org.junit.Assert.assertEquals;

import org.glycoinfo.ms.GlycanMassUtility.om.IMassElement;
import org.glycoinfo.ms.GlycanMassUtility.om.IonCloud;
import org.glycoinfo.ms.GlycanMassUtility.om.Molecule;
import org.junit.Test;

import ninja.mspp.operation.mass_calculator.model.mass.CompoundCreator;
import ninja.mspp.operation.mass_calculator.model.mass.CompoundType;
import ninja.mspp.operation.mass_calculator.model.mass.MassCalculator;

public class MassCalculatorTest {

	@Test
	public void testGlycanComposition() {
		CompoundType type = CompoundType.GLYCAN_COMPOSITION;
		assertComputeMass(type, "Hex", 180.063388);
		assertComputeMass(type, "HexNAc", 221.089937);
		assertComputeMass(type, "Hex(3)HexNAc(2)", 910.327780);

		assertComputeMz(type, "Hex", "H", null, 181.070664);
		assertComputeMz(type, "Hex", "Na", null, 203.052609);
		assertComputeMz(type, "Hex", "2Na", null, 113.020915);
		assertComputeMz(type, "Hex", "H", "H2O", 163.060100);
		assertComputeMz(type, "Hex", "Na", "H2O", 185.042044);
		assertComputeMz(type, "Hex", "2Na", "H2O", 104.015633);
		assertComputeMz(type, "Hex(3)HexNAc(2)", "H", null, 911.335056);
		assertComputeMz(type, "Hex(3)HexNAc(2)", "Na", null, 933.317001);
		assertComputeMz(type, "Hex(3)HexNAc(2)", "2Na", null, 478.153111);
	}

	@Test
	public void testPeptide() {
		CompoundType type = CompoundType.PEPTIDE;
		assertComputeMass(type, "A", 89.047678);
		assertComputeMass(type, "C", 121.019749);
		assertComputeMass(type, "D", 133.037508);
		assertComputeMass(type, "PROTEIN", 965.529429);
		assertComputeMass(type, "PEPTIDE", 799.359964);

		assertComputeMz(type, "A", "H", null, 90.054955);
		assertComputeMz(type, "C", "H", null, 122.027026);
		assertComputeMz(type, "D", "H", null, 134.044784);
		assertComputeMz(type, "PROTEIN", "H", null, 966.536706);
		assertComputeMz(type, "PEPTIDE", "H", null, 800.367240);

		assertComputeMz(type, "HKTDSFV", "H", null, 833.415194);
	}

	@Test
	public void testChemicalComposition() {
		CompoundType type = CompoundType.CHEMICAL_COMPOSITION;
		assertComputeMass(type, "C6H12O6", 180.063388);
		assertComputeMz(type, "C6H12O6", "H", null, 181.070664);
	}


	private void assertComputeMass(CompoundType type, String name, double mass) {
		assertEquals(mass, getCalculator(type, name).computeMass(), 0.000001);
	}

	private void assertComputeMz(CompoundType type, String name, String ion, String neutralLoss, double mz) {
		IonCloud ionCloud = IonCloud.parse(ion);
		Molecule neutralLossMol = (neutralLoss == null)? new Molecule() : Molecule.parse(neutralLoss);
		assertEquals(mz, getCalculator(type, name).computeMz(ionCloud, neutralLossMol), 0.000001);
	}

	private MassCalculator getCalculator(CompoundType type, String name) {
		IMassElement compound = CompoundCreator.create(name, type);
		MassCalculator calc = new MassCalculator();
		calc.addCompound(compound);
		return calc;
	}
}
