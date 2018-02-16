package grassmarlin.ui.common.dialogs.preferences;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.ui.common.controls.BinaryEditor;
import grassmarlin.ui.common.controls.ObjectField;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.BooleanExpression;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PreferenceDialog<TPrefs extends Cloneable> extends Dialog {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Field {
        String name() default "";
        String accessorName() default "";
        boolean nullable() default false;
        int rows() default 1;
    }

    private final RuntimeConfiguration config;
    private final TPrefs preferencesIn;
    private final TPrefs preferencesOut;
    private final GridPane grid;
    private final AtomicBoolean hasError;
    private final List<Runnable> onClosingTasks;

    public PreferenceDialog(final RuntimeConfiguration config, final TPrefs preferences) {
        this.config = config;
        this.preferencesIn = preferences;
        this.onClosingTasks = new LinkedList<>();

        TPrefs out = preferences;
        try {
            out = (TPrefs) preferences.getClass().getMethod("clone").invoke(preferences);
        } catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            Logger.log(Logger.Severity.WARNING, "There was an error preparing the preferences: %s", ex.getMessage());
        }
        this.preferencesOut = out;
        this.grid = new GridPane();
        this.hasError = new AtomicBoolean(false);
        this.setOnCloseRequest(event -> {
            for(Runnable task : onClosingTasks) {
                task.run();
            }
        });

        this.setPreferences(preferences);

        RuntimeConfiguration.setIcons(this);
        this.setTitle("Configure...");
        this.getDialogPane().setContent(this.grid);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
    }

    private static String properCase(final String source) {
        return source.replaceAll("(?!^)([A-Z])", " $1").replaceAll("^.", String.valueOf(Character.toUpperCase(source.charAt(0))));
    }

    public void setPreferences(final TPrefs preferencesNew) {
        this.grid.getChildren().clear();
        this.onClosingTasks.clear();
        this.hasError.set(false);

        // Identify the fields to add.
        final Class<?> clazz = this.preferencesIn.getClass();
        List<java.lang.reflect.Field> fields = new ArrayList<>();
        for(final java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            if(field.isAnnotationPresent(Field.class)) {
                fields.add(field);
            }
        }

        /*
        fields.sort((f1, f2) -> {
            final int cmpType = f1.getType().getName().compareTo(f2.getType().getName());
            if(cmpType == 0) {
                final String nameLeft = f1.getAnnotation(Field.class).name() == "" ? properCase(f1.getName()) : f1.getAnnotation(Field.class).name();
                final String nameRight = f2.getAnnotation(Field.class).name() == "" ? properCase(f2.getName()) : f2.getAnnotation(Field.class).name();

                return nameLeft.compareTo(nameRight);
            } else {
                return cmpType;
            }
        });
        */

        int idxRow = 0;
        for(final java.lang.reflect.Field field : fields) {
            boolean hasSetter;
            boolean hasGetter;
            final Field details = field.getAnnotation(Field.class);
            final String name = !details.accessorName().equals("") ? details.accessorName() : field.getName().replaceAll("^.", String.valueOf(Character.toUpperCase(field.getName().charAt(0))));
            final boolean nullable = details.nullable();
            try {
                hasGetter = clazz.getMethod("get" + name).getReturnType() == field.getType();
            } catch(NoSuchMethodException ex) {
                hasGetter = false;
            }
            try {
                hasSetter = clazz.getMethod("set" + name, field.getType()) != null;
            } catch(NoSuchMethodException ex) {
                hasSetter = false;
            }

            if(!hasGetter) {
                continue;
            }

            try {
                if (field.getType() == Long.class || field.getType() == Integer.class || field.getType() == Double.class) {
                    final String regex;
                    if(field.getType() == Long.class || field.getType() == Integer.class) {
                        //We're ignoring bounds
                        if(nullable) {
                            regex = "|-?\\d+";
                        } else {
                            regex = "-?\\d+";
                        }
                    } else if(field.getType() == Double.class) {
                        if(nullable) {
                            regex = "|-?\\d+(\\.\\d*)?";
                        } else {
                            regex = "-?\\d+(\\.\\d*)?";
                        }
                    } else {
                        //Default to integer
                        if(nullable) {
                            regex = "|-?\\d+";
                        } else {
                            regex = "-?\\d+";
                        }
                    }
                    final TextField control = new TextField();
                    control.setText(clazz.getMethod("get" + name).invoke(preferencesNew).toString());
                    if(!hasSetter) {
                        control.setDisable(true);
                    } else {
                        control.textProperty().addListener((observable, oldValue, newValue) -> {
                            try {
                                if(!newValue.matches(regex)) {
                                    control.setText(oldValue);
                                } else {
                                    clazz.getMethod("set" + name, field.getType()).invoke(this.preferencesOut, newValue.equals("") ? null : field.getType().getConstructor(String.class).newInstance(newValue));
                                }
                            } catch(InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                                PreferenceDialog.this.hasError.set(true);
                            }
                        });
                    }
                    this.grid.add(control, 1, idxRow);
                } else if (field.getType() == String.class) {
                    final TextInputControl control;
                    if(details.rows() <= 1) {
                        control = new TextField();
                    } else {
                        control = new TextArea();
                        ((TextArea)control).setPrefRowCount(details.rows());
                    }
                    control.setText((String) clazz.getMethod("get" + name).invoke(preferencesNew));
                    if (!hasSetter) {
                        control.setDisable(true);
                    } else {
                        control.textProperty().addListener((observable, oldValue, newValue) -> {
                            try {
                                clazz.getMethod("set" + name, String.class).invoke(this.preferencesOut, (newValue.equals("") && nullable) ? null : newValue);
                            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                                PreferenceDialog.this.hasError.set(true);
                            }
                        });
                    }
                    this.grid.add(control, 1, idxRow);
                } else if (field.getType() == Boolean.class) {
                    final CheckBox control = new CheckBox();
                    control.setSelected((Boolean)clazz.getMethod("get" + name).invoke(preferencesNew));
                    if(!hasSetter) {
                        control.setDisable(true);
                    } else {
                        control.selectedProperty().addListener((observable, oldValue, newValue) -> {
                            try {
                                clazz.getMethod("set" + name, Boolean.class).invoke(this.preferencesOut, newValue);
                            } catch(IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                                PreferenceDialog.this.hasError.set(true);
                            }
                        });
                    }
                    this.grid.add(control, 1, idxRow);
                } else if (field.getType() == Path.class) {
                    final TextInputControl control = new TextField();
                    control.autosize();
                    control.setText(((Path)clazz.getMethod("get" + name).invoke(preferencesNew)).toAbsolutePath().toString());
                    if(!hasSetter) {
                        control.setDisable(true);
                    } else {
                        control.textProperty().addListener((observable, oldValue, newValue) -> {
                            control.autosize();
                            try {
                                clazz.getMethod("set" + name, String.class).invoke(this.preferencesOut, (newValue.equals("") && nullable) ? null : newValue);
                            } catch(IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                                PreferenceDialog.this.hasError.set(true);
                            }
                        });
                    }
                    //TODO: Add metadata for filters, etc. when constructing path elements for preferences.
                    this.grid.add(control, 1, idxRow);
                    final Button btnBrowse = new Button("Browse...");
                    if(hasSetter) {
                        btnBrowse.setOnAction(event -> {
                            final FileChooser chooser = new FileChooser();
                            chooser.setInitialDirectory(new File(new File(control.getText()).getParent()));
                            final File choice = chooser.showOpenDialog(PreferenceDialog.this.getOwner());
                            if (choice != null) {
                                control.setText(choice.toPath().toAbsolutePath().toString());
                            }
                        });
                    } else {
                        btnBrowse.setDisable(true);
                    }
                    this.grid.add(btnBrowse, 2, idxRow);
                } else if (field.getType() == Color.class) {
                    //TODO: Color Picker
                } else if(field.getType().isEnum()) {
                    final ComboBox<Object> control = new ComboBox<>();
                    control.getItems().addAll(Arrays.asList(field.getType().getEnumConstants()));
                    control.getSelectionModel().select((clazz.getMethod("get" + name).invoke(preferencesNew)));
                    if (!hasSetter) {
                        control.setDisable(true);
                    } else {
                        onClosingTasks.add(() -> {
                            try {
                                clazz.getMethod("set" + name, field.getType()).invoke(this.preferencesOut, control.getSelectionModel().getSelectedItem());
                            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                                PreferenceDialog.this.hasError.set(true);
                            }
                        });
                    }
                    this.grid.add(control, 1, idxRow);
                } else if(field.getType() == byte[].class) {
                    final BinaryEditor control = new BinaryEditor();
                    control.setBytes((byte[])clazz.getMethod("get" + name).invoke(preferencesNew));
                    if (!hasSetter) {
                        control.setDisable(true);
                    } else {
                        onClosingTasks.add(() -> {
                            try {
                                clazz.getMethod("set" + name, field.getType()).invoke(this.preferencesOut, (Object)control.getBytes());
                            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                                PreferenceDialog.this.hasError.set(true);
                            }
                        });
                    }
                    this.grid.add(control, 1, idxRow);

                } else if(Serializable.class.isAssignableFrom(field.getType())) {
                    final ObjectField<Serializable> control = new ObjectField<>(config, (Class<Serializable>)field.getType());
                    control.setValue((Serializable)clazz.getMethod("get" + name).invoke(preferencesNew));
                    if (!hasSetter) {
                        control.setDisable(true);
                    } else {
                        onClosingTasks.add(() -> {
                            try {
                                clazz.getMethod("set" + name, field.getType()).invoke(this.preferencesOut, control.createInstanceFromText());
                            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                                PreferenceDialog.this.hasError.set(true);
                            }
                        });
                    }
                    this.grid.add(control, 1, idxRow);
                } else if(field.getType() == ZonedDateTime.class) {
                    //As with booleans, there is no good solution for nullable timestamps.
                    final ZonedDateTime dtInitial = Optional.ofNullable((ZonedDateTime)clazz.getMethod("get" + name).invoke(this.preferencesIn)).orElse(ZonedDateTime.now());

                    final HBox container = new HBox();
                    final DatePicker controlDate = new DatePicker(dtInitial.toLocalDate());
                    final Spinner<Integer> controlHour = new Spinner<>(0, 23, dtInitial.getHour());
                    final Spinner<Integer> controlMinute = new Spinner<>(0, 60, dtInitial.getMinute());
                    final Spinner<Integer> controlSecond = new Spinner<>(0, 60, dtInitial.getSecond());

                    if(hasSetter) {
                        final InvalidationListener listener = (observable) -> {
                            try {
                                if(controlDate.getValue() == null) {
                                    if(nullable) {
                                        clazz.getMethod("set" + name, ZonedDateTime.class).invoke(this.preferencesOut, (ZonedDateTime)null);
                                    } else {
                                        //Error; restore from preferencesOut
                                        controlDate.setValue(dtInitial.toLocalDate());
                                        //TODO: Restore time information as well.
                                    }
                                } else {
                                    clazz.getMethod("set" + name, ZonedDateTime.class).invoke(this.preferencesOut, controlDate.getValue().atTime(controlHour.getValue(), controlMinute.getValue(), controlSecond.getValue()).atZone(ZoneId.of("Z")));
                                }
                            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                                PreferenceDialog.this.hasError.set(true);
                            }
                        };
                        controlDate.valueProperty().addListener(listener);
                        controlHour.valueProperty().addListener(listener);
                        controlMinute.valueProperty().addListener(listener);
                        controlSecond.valueProperty().addListener(listener);

                        controlHour.setPrefWidth(72.0);
                        controlMinute.setPrefWidth(72.0);
                        controlSecond.setPrefWidth(72.0);

                        final BooleanExpression hasInvalidDate = controlDate.valueProperty().isNull();
                        controlHour.disableProperty().bind(hasInvalidDate);
                        controlMinute.disableProperty().bind(hasInvalidDate);
                        controlSecond.disableProperty().bind(hasInvalidDate);
                    } else {
                        controlDate.setDisable(true);
                        controlHour.setDisable(true);
                        controlMinute.setDisable(true);
                        controlSecond.setDisable(true);
                    }

                    container.getChildren().addAll(
                            controlDate,
                            new Label(" "),
                            controlHour,
                            new Label(":"),
                            controlMinute,
                            new Label(":"),
                            controlSecond,
                            new Label("Z")
                    );
                    this.grid.add(container, 1, idxRow);
                } else {
                    //TODO: Add support for date/time/other datetime classes
                    //TODO: Add support for enums
                    //If we still don't know how to render this, display an error
                    System.err.println("Unable to generate Preference row for type " + field.getType().getCanonicalName());
                }
                this.grid.add(new Label(details.name()), 0, idxRow);
                idxRow++;
            } catch(IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
                //From the user's perspective, this field doesn't exist.
                Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "Error adding field (%s) to preferences: %s", name, ex.getMessage());
            }
        }
    }
    public TPrefs getPreferences() {
        if(hasError.getAndSet(false)) {
            Logger.log(Logger.Severity.WARNING, "There were errors computing the new preferene values; the resulting values may not be correct.");
        }
        return this.preferencesOut;
    }
}