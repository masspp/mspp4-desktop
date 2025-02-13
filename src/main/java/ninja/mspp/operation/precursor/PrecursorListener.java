package ninja.mspp.operation.precursor;

import java.util.List;
import java.util.Optional;

import javafx.scene.control.ChoiceDialog;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.clazz.Listener;
import ninja.mspp.core.annotation.method.OnSelectSpectrum;
import ninja.mspp.core.annotation.method.SpectrumAction;
import ninja.mspp.core.model.ms.Spectrum;
import ninja.mspp.core.types.SpectrumCondition;
import ninja.mspp.core.view.SpectrumActionEvent;

@Listener("Precursor")
public class PrecursorListener {
	@SpectrumAction(value = "Precursor Spectrum", condition = SpectrumCondition.HAS_PRECURSOR)
	public void openPrecursor(SpectrumActionEvent event) {
		MsppManager manager = MsppManager.getInstance();
		
		Spectrum spectrum = event.getSpectrum();
		Spectrum precursor = spectrum.getPrecursor();
		
		manager.invoke(OnSelectSpectrum.class, precursor);
	}
	
	@SpectrumAction(value = "Product Spectrum ...", condition = SpectrumCondition.HAS_PRODUCTS)
	public void openProduct(SpectrumActionEvent event) {
		Spectrum spectrum = event.getSpectrum();
		List<Spectrum> products = spectrum.getProducts();
		
		ChoiceDialog<Spectrum> dialog = new ChoiceDialog<Spectrum>(products.get(0), products);
		dialog.setTitle("Select product spectrum");
		Optional<Spectrum> optional = dialog.showAndWait();
		
		if(optional.isPresent() ) {
			Spectrum product = optional.get();
			
			MsppManager manager = MsppManager.getInstance();
			manager.invoke(OnSelectSpectrum.class, product);
		}
	}
}
