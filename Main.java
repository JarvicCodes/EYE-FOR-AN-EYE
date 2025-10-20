import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("House Always Wins");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            StartScreen start = new StartScreen(
                e -> JOptionPane.showMessageDialog(f, "Start game!"),   // Play
                e -> JOptionPane.showMessageDialog(f, "Settings…"),     // Settings
                e -> JOptionPane.showMessageDialog(f, "Credits…")       // Credits
            );

            f.setContentPane(start);
            f.pack();                   // sizes window to panel’s preferred size
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}