package hadeel.semulatorui;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.TableRow;
import javafx.util.Duration;

public class AnimationHelper {
    private static boolean animationsEnabled = true;

    public static void setAnimationsEnabled(boolean enabled) {
        animationsEnabled = enabled;
    }

    public static boolean isAnimationsEnabled() {
        return animationsEnabled;
    }

    // Fade in animation for dialogs and new content
    public static void fadeIn(Node node) {
        if (!animationsEnabled) {
            node.setOpacity(1);
            return;
        }

        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    // Pulse animation for highlighting important changes
    public static void pulse(Node node) {
        if (!animationsEnabled) return;

        ScaleTransition st = new ScaleTransition(Duration.millis(300), node);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.05);
        st.setToY(1.05);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.play();
    }

    // Highlight animation for current debug line
    public static void highlightDebugLine(Node node) {
        if (!animationsEnabled) return;

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(node.opacityProperty(), 0.7)),
            new KeyFrame(Duration.millis(500),
                new KeyValue(node.opacityProperty(), 1.0))
        );
        timeline.setCycleCount(2);
        timeline.setAutoReverse(true);
        timeline.play();
    }

    // Progress bar animation
    public static void animateProgress(Node progressBar, double fromValue, double toValue) {
        if (!animationsEnabled) return;

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(progressBar.scaleXProperty(), fromValue)),
            new KeyFrame(Duration.millis(1000),
                new KeyValue(progressBar.scaleXProperty(), toValue))
        );
        timeline.play();
    }
}