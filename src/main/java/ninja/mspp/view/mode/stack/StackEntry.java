package ninja.mspp.view.mode.stack;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ninja.mspp.view.mode.stack.StackViewManager.Kind;

/**
 * A single waveform row in the stack: title bar with [X] remove button at the
 * top, the canvas below. Sized to the manager's current entry height; canvas
 * fills the available width.
 */
public class StackEntry extends VBox {
	private final StackedCanvas canvas;
	private final Kind kind;

	StackEntry(Kind kind, String title, Runnable onRemove) {
		this.kind = kind;
		this.setPadding(new Insets(2));
		this.setSpacing(2);
		this.setStyle("-fx-border-color: #ccc; -fx-border-width: 0 0 1 0;");

		Label label = new Label(title);
		label.setStyle("-fx-font-weight: bold;");
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		Button close = new Button("X");
		close.setOnAction(e -> onRemove.run());
		HBox header = new HBox(6, label, spacer, close);
		header.setAlignment(Pos.CENTER_LEFT);
		header.setPadding(new Insets(2, 4, 2, 4));

		this.canvas = new StackedCanvas(kind);
		BorderPane canvasHolder = new BorderPane();
		canvasHolder.setCenter(this.canvas);
		canvasHolder.widthProperty().addListener((obs, oldV, newV) ->
			this.canvas.widthProperty().set(newV.doubleValue()));
		canvasHolder.heightProperty().addListener((obs, oldV, newV) ->
			this.canvas.heightProperty().set(newV.doubleValue()));
		VBox.setVgrow(canvasHolder, Priority.ALWAYS);

		this.getChildren().addAll(header, canvasHolder);
		this.applyHeight(StackViewManager.getInstance().getEntryHeight());
	}

	void applyHeight(double canvasHeight) {
		double total = canvasHeight + 26.0; // header + padding
		this.setMinHeight(total);
		this.setPrefHeight(total);
		this.setMaxHeight(total);
	}

	StackedCanvas getCanvas() {
		return this.canvas;
	}

	Kind getKind() {
		return this.kind;
	}
}
