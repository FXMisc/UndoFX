package org.fxmisc.undo.demo;

import static org.reactfx.EventStreams.*;

import java.util.Objects;
import java.util.Optional;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;
import org.reactfx.Change;
import org.reactfx.EventStream;

public class CircleProperties extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private abstract class CircleChange<T> {
        protected final T oldValue;
        protected final T newValue;

        protected CircleChange(T oldValue, T newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        abstract void redo();
        abstract CircleChange<T> invert();

        Optional<CircleChange<?>> mergeWith(CircleChange<?> other) {
            // don't merge changes by default
            return Optional.empty();
        }

        @Override
        public int hashCode() {
            return Objects.hash(oldValue, newValue);
        }
    };

    private class ColorChange extends CircleChange<Color> {
        public ColorChange(Color oldValue, Color newValue) {
            super(oldValue, newValue);
        }
        public ColorChange(Change<Paint> c) {
            this((Color) c.getOldValue(), (Color) c.getNewValue());
        }
        @Override void redo() { colorPicker.setValue(newValue); }
        @Override ColorChange invert() { return new ColorChange(newValue, oldValue); }

        @Override
        public boolean equals(Object other) {
            if(other instanceof ColorChange) {
                ColorChange that = (ColorChange) other;
                return Objects.equals(this.oldValue, that.oldValue)
                    && Objects.equals(this.newValue, that.newValue);
            } else {
                return false;
            }
        }
    }

    private class RadiusChange extends CircleChange<Double> {
        public RadiusChange(Double oldValue, Double newValue) {
            super(oldValue, newValue);
        }
        public RadiusChange(Change<Number> c) {
            super(c.getOldValue().doubleValue(), c.getNewValue().doubleValue());
        }
        @Override void redo() { radius.setValue(newValue); }
        @Override RadiusChange invert() { return new RadiusChange(newValue, oldValue); }
        @Override Optional<CircleChange<?>> mergeWith(CircleChange<?> other) {
            if(other instanceof RadiusChange) {
                return Optional.of(new RadiusChange(oldValue, ((RadiusChange) other).newValue));
            } else {
                return Optional.empty();
            }
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof RadiusChange) {
                RadiusChange that = (RadiusChange) other;
                return Objects.equals(this.oldValue, that.oldValue)
                    && Objects.equals(this.newValue, that.newValue);
            } else {
                return false;
            }
        }
    }

    private class CenterXChange extends CircleChange<Double> {
        public CenterXChange(Double oldValue, Double newValue) {
            super(oldValue, newValue);
        }
        public CenterXChange(Change<Number> c) {
            super(c.getOldValue().doubleValue(), c.getNewValue().doubleValue());
        }
        @Override void redo() { centerX.setValue(newValue); }
        @Override CenterXChange invert() { return new CenterXChange(newValue, oldValue); }
        @Override Optional<CircleChange<?>> mergeWith(CircleChange<?> other) {
            if(other instanceof CenterXChange) {
                return Optional.of(new CenterXChange(oldValue, ((CenterXChange) other).newValue));
            } else {
                return Optional.empty();
            }
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof CenterXChange) {
                CenterXChange that = (CenterXChange) other;
                return Objects.equals(this.oldValue, that.oldValue)
                    && Objects.equals(this.newValue, that.newValue);
            } else {
                return false;
            }
        }
    }

    private class CenterYChange extends CircleChange<Double> {
        public CenterYChange(Double oldValue, Double newValue) {
            super(oldValue, newValue);
        }
        public CenterYChange(Change<Number> c) {
            super(c.getOldValue().doubleValue(), c.getNewValue().doubleValue());
        }
        @Override void redo() { centerY.setValue(newValue); }
        @Override CenterYChange invert() { return new CenterYChange(newValue, oldValue); }
        @Override Optional<CircleChange<?>> mergeWith(CircleChange<?> other) {
            if(other instanceof CenterYChange) {
                return Optional.of(new CenterYChange(oldValue, ((CenterYChange) other).newValue));
            } else {
                return Optional.empty();
            }
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof CenterYChange) {
                CenterYChange that = (CenterYChange) other;
                return Objects.equals(this.oldValue, that.oldValue)
                    && Objects.equals(this.newValue, that.newValue);
            } else {
                return false;
            }
        }
    }

    private final Circle circle = new Circle();
    private final ColorPicker colorPicker = new ColorPicker(Color.RED);
    private final Slider radius = new Slider(10, 200, 40);
    private final Slider centerX = new Slider(0, 400, 200);
    private final Slider centerY = new Slider(0, 400, 200);
    private final Button undoBtn = new Button("Undo");
    private final Button redoBtn = new Button("Redo");
    private final Button saveBtn = new Button("Save");
    private final EventStream<CircleChange<?>> changes;
    private final UndoManager<CircleChange<?>> undoManager;

    {
        circle.fillProperty().bind(colorPicker.valueProperty());
        circle.radiusProperty().bind(radius.valueProperty());
        circle.centerXProperty().bind(centerX.valueProperty());
        circle.centerYProperty().bind(centerY.valueProperty());

        EventStream<ColorChange> colorChanges = changesOf(circle.fillProperty()).map(c -> new ColorChange(c));
        EventStream<RadiusChange> radiusChanges = changesOf(circle.radiusProperty()).map(c -> new RadiusChange(c));
        EventStream<CenterXChange> centerXChanges = changesOf(circle.centerXProperty()).map(c -> new CenterXChange(c));
        EventStream<CenterYChange> centerYChanges = changesOf(circle.centerYProperty()).map(c -> new CenterYChange(c));
        changes = merge(colorChanges, radiusChanges, centerXChanges, centerYChanges);

        undoManager = UndoManagerFactory.unlimitedHistoryUndoManager(
                    changes, // stream of changes to observe
                    c -> c.invert(), // function to invert a change
                    c -> c.redo(), // function to undo a change
                    (c1, c2) -> c1.mergeWith(c2)); // function to merge two changes

        undoBtn.disableProperty().bind(undoManager.undoAvailableProperty().map(x -> !x));
        redoBtn.disableProperty().bind(undoManager.redoAvailableProperty().map(x -> !x));
        undoBtn.setOnAction(evt -> undoManager.undo());
        redoBtn.setOnAction(evt -> undoManager.redo());
        saveBtn.disableProperty().bind(undoManager.atMarkedPositionProperty());
        saveBtn.setOnAction(evt -> { save(); undoManager.mark(); });
    }

    private void save() {
        // implement save action here
    }

    @Override
    public void start(Stage primaryStage) {
        Pane pane = new Pane();
        pane.setPrefWidth(400);
        pane.setPrefHeight(400);
        pane.getChildren().add(circle);

        HBox undoPanel = new HBox(20.0, undoBtn, redoBtn, saveBtn);

        VBox root = new VBox(10.0,
                pane,
                labeled("Color", colorPicker),
                labeled("Radius", radius),
                labeled("X", centerX),
                labeled("Y", centerY),
                undoPanel);
        root.setAlignment(Pos.CENTER);
        root.setFillWidth(false);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static HBox labeled(String labelText, Node node) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold;");
        HBox hbox = new HBox(15, label, node);
        hbox.setAlignment(Pos.CENTER);
        return hbox;
    }
}
