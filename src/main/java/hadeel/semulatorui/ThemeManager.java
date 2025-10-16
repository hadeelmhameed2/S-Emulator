package hadeel.semulatorui;

import javafx.scene.Scene;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    private static Scene currentScene;
    private static String currentTheme = "Default";

    public enum Theme {
        DEFAULT("Default", "/hadeel/semulatorui/themes/default.css"),
        DARK("Dark", "/hadeel/semulatorui/themes/dark.css"),
        BLUE("Blue Professional", "/hadeel/semulatorui/themes/blue.css");

        private final String displayName;
        private final String cssPath;

        Theme(String displayName, String cssPath) {
            this.displayName = displayName;
            this.cssPath = cssPath;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssPath() {
            return cssPath;
        }
    }

    public static void setScene(Scene scene) {
        currentScene = scene;
    }

    public static void applyTheme(Theme theme) {
        if (currentScene == null) return;

        // Clear existing stylesheets
        currentScene.getStylesheets().clear();

        // Add new theme
        try {
            String cssPath = theme.getCssPath();
            currentScene.getStylesheets().add(ThemeManager.class.getResource(cssPath).toExternalForm());
            currentTheme = theme.getDisplayName();
        } catch (Exception e) {
            System.err.println("Failed to load theme: " + theme.getDisplayName());
            e.printStackTrace();
        }
    }

    public static List<String> getAvailableThemes() {
        List<String> themes = new ArrayList<>();
        for (Theme theme : Theme.values()) {
            themes.add(theme.getDisplayName());
        }
        return themes;
    }

    public static Theme getThemeByName(String name) {
        for (Theme theme : Theme.values()) {
            if (theme.getDisplayName().equals(name)) {
                return theme;
            }
        }
        return Theme.DEFAULT;
    }

    public static String getCurrentTheme() {
        return currentTheme;
    }
}