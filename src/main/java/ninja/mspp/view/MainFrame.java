package ninja.mspp.view;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import ninja.mspp.MsppManager;
import ninja.mspp.core.annotation.method.MenuAction;
import ninja.mspp.core.annotation.method.ViewMode;
import ninja.mspp.core.model.listener.ListenerMethod;

public class MainFrame implements Initializable {
	private StringProperty VIEW_MODE = null;
	
	@FXML
	private MenuBar menuBar;
		
	@FXML
	private BorderPane pane;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.createMenu(this.menuBar);
	}
		
	private void createMenu(MenuBar menuBar) {
		Menu fileMenu = new Menu("File");
		menuBar.getMenus().add(fileMenu);
		
		Menu openMenu = new Menu("Open");
		fileMenu.getItems().add(openMenu);
		
		Menu viewMenu = new Menu("View");
		menuBar.getMenus().add(viewMenu);
		
		Menu toolsMenu = new Menu("Tools");
		menuBar.getMenus().add(toolsMenu);
		
		Menu modeMenu = new Menu("View Mode");
		viewMenu.getItems().add(modeMenu);
	
		this.addModeMenus(modeMenu);
		this.addOtherMenus(menuBar);
	}
	
	private void addModeMenus(Menu modeMenu) {
		VIEW_MODE = new SimpleStringProperty("");
		MsppManager manager = MsppManager.getInstance();
		List<ListenerMethod<ViewMode>> methods = manager.getMethods(ViewMode.class);
		methods.sort(
			(a, b)-> {
				return a.getAnnotation().order() - b.getAnnotation().order();
			}
		);
		
		CheckMenuItem firstMenu = null; 
		for (ListenerMethod<ViewMode> method : methods) {
			ViewMode viewMode = method.getAnnotation();
			String name = viewMode.value();
			CheckMenuItem menuItem = new CheckMenuItem(name);
			menuItem.setOnAction((event) -> {
				try {
					Parent parent = (Parent)method.invoke(null);
					this.pane.setCenter(parent);
					this.pane.widthProperty().addListener(
						(ov, oldVal, newVal) -> {
							parent.prefWidth(newVal.doubleValue());
						}
					);
					this.pane.heightProperty().addListener(
						(ov, oldVal, newVal) -> {
							parent.prefHeight(newVal.doubleValue());
						}
					);
					VIEW_MODE.set(name);
					manager.setStatus("VIEW_MODE", name);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			});
			VIEW_MODE.addListener((observable, oldValue, newValue) -> {
				if (!newValue.equals(oldValue)) {
					menuItem.setSelected(newValue.equals(name));
				}
			});

			modeMenu.getItems().add(menuItem);
			if(firstMenu == null) {
                firstMenu = menuItem;
            }
		}
		firstMenu.fire();
	}
	
	private void addOtherMenus(MenuBar menuBar) {
		MsppManager manager = MsppManager.getInstance();
		List<ListenerMethod<MenuAction>> methods = manager.getMethods(MenuAction.class);
		methods.sort(
			(a, b)-> {
				return a.getAnnotation().order() - b.getAnnotation().order();
			}
		);

		for (ListenerMethod<MenuAction> method : methods) {
			MenuAction action = method.getAnnotation();
			String value = action.value();
			String[] items = value.split(">");
			
			Menu currentMenu = null;
			for(int i = 0; i < items.length; i++) {
				String name = items[i].trim();
				if (i == 0) {
					currentMenu = this.findMenu(menuBar, name);
				} 
				else if(i < items.length - 1){
					currentMenu = this.findMenu(currentMenu, name);
				}
				else {
					MenuItem menuItem = new MenuItem(name);
                    menuItem.setOnAction((event)->{
                        method.invoke();
                    });
                    currentMenu.getItems().add(menuItem);
				}
			}
		}
	}
	
	private Menu findMenu(MenuBar menuBar, String name) {
		for (Menu menu : menuBar.getMenus()) {
			if (menu.getText().equals(name)) {
				return menu;
			}
		}

		Menu menu = new Menu(name);
		menuBar.getMenus().add(menu);
		return menu;
	}
	
	private Menu findMenu(Menu menu, String name) {
        for (MenuItem subMenu : menu.getItems()) {
            if (subMenu.getText().equals(name)) {
                return (Menu)subMenu;
            }
        }
        
        Menu subMenu = new Menu(name);
        menu.getItems().add(subMenu);
        return subMenu;
    }
}
