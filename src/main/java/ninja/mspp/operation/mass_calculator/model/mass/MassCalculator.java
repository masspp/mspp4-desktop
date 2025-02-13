package ninja.mspp.operation.mass_calculator.model.mass;

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

	public double computeMz(IonCloud ion, Molecule neutralLoss) {
		Molecule mol = new Molecule();
		if (neutralLoss != null)
			mol = mol.addMolecules(neutralLoss, -1);

		return MassUtils.computeMz(this.complex, ion, mol).doubleValue();
	}
}
