package com.DMHelper.fx;

import javafx.scene.Scene;

import java.net.URL;

public final class FxThemes {
    private static final String MAIN_STYLESHEET = "/com/DMHelper/basic/javafx/main-menu.css";

    private FxThemes() {
    }

    public static void apply(Scene scene) {
        URL resource = FxThemes.class.getResource(MAIN_STYLESHEET);
        if (resource == null) {
            return;
        }
        String stylesheet = resource.toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }
}
