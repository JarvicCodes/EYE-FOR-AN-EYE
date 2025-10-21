import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("House Always Wins");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // create StartScreen
            StartScreen start = new StartScreen(
                e -> { // Play button → swap to GameClass
                    GameClass game = new GameClass();
                    f.setContentPane(game);
                    f.revalidate();
                    f.repaint();
                },
                e -> JOptionPane.showMessageDialog(f, "Settings…"),
                e -> JOptionPane.showMessageDialog(f, "Credits…")
            );

            f.setContentPane(start);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
