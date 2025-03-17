package ninja.mspp.core.view;

import javafx.scene.Parent;

public class ViewInfo<T> {
	private Parent window;
	private T controller;
	
	public ViewInfo(Parent window, T controller) {
		this.window = window;
		this.controller = controller;
	}
	
	public Parent getWindow() {
		return this.window;
	}
	
	public T getController() {
		return this.controller;
	}
}