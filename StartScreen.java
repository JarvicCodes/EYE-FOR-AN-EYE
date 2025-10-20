import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/** A self-painted title screen with Play / Settings / Credits buttons. */
public class StartScreen extends JPanel {

    public StartScreen(ActionListener onPlay,
                       ActionListener onSettings,
                       ActionListener onCredits) {
        setPreferredSize(new Dimension(960, 540));
        setLayout(new GridBagLayout());   // lets us center the button row
        setOpaque(true);

        // --- Buttons row -----------------------------------------------------
        JButton play     = makeBtn("Play", onPlay);
        JButton settings = makeBtn("Settings", onSettings);
        JButton credits  = makeBtn("Credits", onCredits);

        JPanel row = new JPanel(new GridLayout(1, 3, 20, 0));
        row.setOpaque(false);
        row.add(play);
        row.add(settings);
        row.add(credits);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.insets = new Insets(0, 0, 40, 0);
        add(row, gbc);

        // --- Key shortcuts: Enter=Play, S=Settings, C=Credits, Esc=Exit ------
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "play");
        getActionMap().put("play", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { play.doClick(); }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('S'), "settings");
        getActionMap().put("settings", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { settings.doClick(); }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('C'), "credits");
        getActionMap().put("credits", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { credits.doClick(); }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "quit");
        getActionMap().put("quit", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { System.exit(0); }
        });
    }

    private JButton makeBtn(String text, ActionListener onClick) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 18f));
        b.addActionListener(onClick);
        b.setOpaque(true);
        b.setBackground(new Color(25, 25, 25));
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(160,130,255)),
                BorderFactory.createEmptyBorder(10,18,10,18)
        ));
        return b;
    }

    // Paint a gradient background and a neon-style title
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();

        // Background gradient
        GradientPaint gp = new GradientPaint(0, 0, new Color(10,10,12),
                                             0, h, new Color(15,5,25));
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);

        // Neon title "House Always Wins"
        String line1 = "House";
        String line2 = "Always Wins";
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font titleFont = new Font(Font.SERIF, Font.BOLD, Math.max(36, h/10));
        g2.setFont(titleFont);

        FontMetrics fm = g2.getFontMetrics();
        int y1 = h/3;
        int x1 = (w - fm.stringWidth(line1)) / 2;

        // fake glow: draw several translucent passes
        Color neon = new Color(185, 150, 255);
        for (int r=8; r>=1; r--) {
            g2.setColor(new Color(neon.getRed(), neon.getGreen(), neon.getBlue(), 15));
            g2.drawString(line1, x1, y1);
        }
        g2.setColor(neon);
        g2.drawString(line1, x1, y1);

        // second line a bit bigger
        Font titleFont2 = titleFont.deriveFont(titleFont.getSize2D() * 1.2f);
        g2.setFont(titleFont2);
        fm = g2.getFontMetrics();
        int y2 = y1 + fm.getAscent() + 20;
        int x2 = (w - fm.stringWidth(line2)) / 2;
        for (int r=8; r>=1; r--) {
            g2.setColor(new Color(neon.getRed(), neon.getGreen(), neon.getBlue(), 15));
            g2.drawString(line2, x2, y2);
        }
        g2.setColor(neon);
        g2.drawString(line2, x2, y2);

        g2.dispose();
    }
}
