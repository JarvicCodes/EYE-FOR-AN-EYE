import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("House Always Wins");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setSize(1920, 1080);
            f.setLocationRelativeTo(null);

            // Create a CardLayout and main panel
            CardLayout cards = new CardLayout();
            JPanel container = new JPanel(cards);

            // --- Create screens ---
            StartScreen start = new StartScreen(
                e -> cards.show(container, "game"),
                e -> cards.show(container, "settings"),
                e -> cards.show(container, "credits")
            );

            GameClass game = new GameClass();
            Settings settings = new Settings(e -> cards.show(container, "start"));
            Credits credits = new Credits(e -> cards.show(container, "start"));

            // --- Add them to the card container ---
            container.add(start, "start");
            container.add(game, "game");
            container.add(settings, "settings");
            container.add(credits, "credits");

            f.setContentPane(container);
            f.setVisible(true);

            // Start with the start screen
            cards.show(container, "start");
        });
    }
}
