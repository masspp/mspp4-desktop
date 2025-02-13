package ninja.mspp.core.model.obo.test;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import ninja.mspp.core.model.obo.Obo;
import ninja.mspp.core.model.obo.OboManager;

public class OboTest {

	@Test
	public void test() throws IOException {
		OboManager manager = OboManager.getInstance();
		List<Obo> list = manager.getOboList();
		for (Obo obo : list) {
			System.out.println(obo.getId());
			System.out.println("    Name: " + obo.getName());
			System.out.println("    Def: " + obo.getDef());
			List<Obo> parents = obo.getParents();
			if (parents != null) {
				for (Obo parent : parents) {
					System.out.println("    Parent: " + parent.getId());
				}
			}
		}
	}
}
