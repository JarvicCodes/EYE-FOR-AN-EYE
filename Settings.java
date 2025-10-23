import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class Settings extends JPanel {
    public Settings(ActionListener onClick) {
        setPreferredSize(new Dimension(960, 540));
        setLayout(new BorderLayout());
        Color neon = new Color(185, 150, 255);

        // --- Back button ---
        JButton back = new JButton("BACK");
        back.setFocusPainted(false);
        back.setFont(back.getFont().deriveFont(Font.BOLD, 18f));
        back.addActionListener(onClick);
        back.setOpaque(true);
        back.setBackground(new Color(25, 25, 25));
        back.setForeground(Color.WHITE);
        back.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(160, 130, 255)),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));

        // --- Center text ---
        JLabel label = new JLabel("<html>Roll Until 21 to reach the endgame. <br/>Then try to keep your score as close to 21 to win.</html>",
                SwingConstants.CENTER);
        label.setFont(new Font(Font.SERIF, Font.BOLD, 24));
        label.setForeground(neon);

        add(label, BorderLayout.CENTER);
        add(back, BorderLayout.SOUTH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();

        // Background gradient
        GradientPaint gp = new GradientPaint(0, 0, new Color(10, 10, 12),
                0, h, new Color(15, 5, 25));
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }
}
