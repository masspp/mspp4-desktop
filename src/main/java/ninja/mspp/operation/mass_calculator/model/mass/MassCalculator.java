package ninja.mspp.operation.mass_calculator.model.mass;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.glycoinfo.ms.GlycanMassUtility.om.IMassElement;
import org.glycoinfo.ms.GlycanMassUtility.om.IonCloud;
import org.glycoinfo.ms.GlycanMassUtility.om.Molecule;
import org.glycoinfo.ms.GlycanMassUtility.utils.MassElementCreator;
import org.glycoinfo.ms.GlycanMassUtility.utils.MassUtils;

public class MassCalculator {

	private List<IMassElement> compounds;
	private IMassElement complex;

	public MassCalculator() {
		this.compounds = new ArrayList<>();
		this.complex = null;
	}

	public void addCompound(IMassElement compound) {
		if (compound != null)
			this.compounds.add(compound);
		this.complex = MassElementCreator.createComplexMassElement(this.compounds);
	}

	public double computeMass() {
		return this.complex.getMass().doubleValue();
	}

	/**
	 * Computes the m/z value of the compounds with the given ion and neutral loss.
	 * @param ion IonCloud of the ion
	 * @param neutralLoss Molecule of the neutral loss, or {@code null} for no neutral loss
	 * @return m/z value, or {@code null} if the total charge of the ion is 0.
	 * The value is positive also for a negative ion, as the m/z values of spectra are.
	 */
	public Double computeMz(IonCloud ion, Molecule neutralLoss) {
		Molecule mol = new Molecule();
		if (neutralLoss != null)
			mol = mol.addMolecules(neutralLoss, -1);

		BigDecimal mz = MassUtils.computeMz(this.complex, ion, mol);
		if (mz == null)
			return null;
		// MassUtils returns a negative m/z for a negative ion
		return mz.abs().doubleValue();
	}
}
