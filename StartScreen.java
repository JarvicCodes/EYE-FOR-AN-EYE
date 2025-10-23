import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class StartScreen extends JPanel {

    // ---- filenames (JPG or PNG both fine) ----
    // change names here if yours differ
    private static final String BG_NAME       = "titlescreennodice.png";
    private static final String PLAY_NAME     = "dice_play.png";   // play die (transparent)
    private static final String SETTINGS_NAME = "dice_settings.png";   // settings die (transparent)
    private static final String CREDITS_NAME  = "dice_credits.png";   // credits die (transparent)

    // ---- your slot fractions (x, y, w, h) in 0..1 ----
    // these came from your message
    private static final RectF SLOT_PLAY_F     = new RectF(0.18f, 0.74f, 0.13f, 0.18f);
    private static final RectF SLOT_SETTINGS_F = new RectF(0.44f, 0.74f, 0.13f, 0.18f);
    private static final RectF SLOT_CREDITS_F  = new RectF(0.68f, 0.74f, 0.13f, 0.18f);

    // padding inside each slot (pixels, after converting from fractions)
    private static final int SLOT_PAD = 8;

    // turn on briefly to see outlines while tuning
    private static final boolean DEBUG_SLOTS = false;

    private BufferedImage bg, playDie, settingsDie, creditsDie;

    // invisible buttons that sit on top of the dice
    private final JButton playBtn     = new JButton();
    private final JButton settingsBtn = new JButton();
    private final JButton creditsBtn  = new JButton();

    public StartScreen(ActionListener onPlay, ActionListener onSettings, ActionListener onCredits) {
        setLayout(null);
        setOpaque(true);

        // load images (classpath first, then file fallback)
        bg          = loadImage(BG_NAME);
        playDie     = loadImage(PLAY_NAME);
        settingsDie = loadImage(SETTINGS_NAME);
        creditsDie  = loadImage(CREDITS_NAME);

        // make buttons invisible but clickable
        makeInvisible(playBtn);
        makeInvisible(settingsBtn);
        makeInvisible(creditsBtn);

        // wire actions
        if (onPlay     != null) playBtn.addActionListener(onPlay);
        if (onSettings != null) settingsBtn.addActionListener(onSettings);
        if (onCredits  != null) creditsBtn.addActionListener(onCredits);

        // add to panel (add LAST = on TOP)
        add(playBtn);
        add(settingsBtn);
        add(creditsBtn);

        // ensure Z-order (0 is topmost in Swing)
        setComponentZOrder(playBtn, 0);
        setComponentZOrder(settingsBtn, 0);
        setComponentZOrder(creditsBtn, 0);

        // set initial bounds + keep in sync on resize
        updateHotspots();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                updateHotspots();
                revalidate();
                repaint();
            }
        });
    }

    private void updateHotspots() {
        Rectangle p  = toPixels(SLOT_PLAY_F);
        Rectangle s  = toPixels(SLOT_SETTINGS_F);
        Rectangle c  = toPixels(SLOT_CREDITS_F);
        playBtn.setBounds(p);
        settingsBtn.setBounds(s);
        creditsBtn.setBounds(c);
    }

    private void makeInvisible(AbstractButton b) {
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (DEBUG_SLOTS) {
            // temporary visual aid while tuning positions
            b.setContentAreaFilled(true);
            b.setOpaque(true);
            b.setBackground(new Color(0, 180, 255, 60));
            b.setBorder(BorderFactory.createLineBorder(new Color(0, 80, 160, 160)));
        }
    }

    // convert fractional rect to pixels for current panel size
    private Rectangle toPixels(RectF rf) {
        int pw = Math.max(1, getWidth());
        int ph = Math.max(1, getHeight());
        int x = Math.round(rf.x * pw);
        int y = Math.round(rf.y * ph);
        int w = Math.round(rf.w * pw);
        int h = Math.round(rf.h * ph);
        return new Rectangle(x, y, w, h);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // draw background stretched to panel
        if (bg != null) g2.drawImage(bg, 0, 0, getWidth(), getHeight(), null);

        // draw dice centered within their slots (with padding)
        drawCentered(g2, playDie,     shrink(toPixels(SLOT_PLAY_F),     SLOT_PAD));
        drawCentered(g2, settingsDie, shrink(toPixels(SLOT_SETTINGS_F), SLOT_PAD));
        drawCentered(g2, creditsDie,  shrink(toPixels(SLOT_CREDITS_F),  SLOT_PAD));

        if (DEBUG_SLOTS) {
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(0, 255, 180, 140)); g2.draw(toPixels(SLOT_PLAY_F));
            g2.setColor(new Color(255, 210, 0, 140)); g2.draw(toPixels(SLOT_SETTINGS_F));
            g2.setColor(new Color(255, 80, 120, 140)); g2.draw(toPixels(SLOT_CREDITS_F));
        }

        g2.dispose();
    }

    private static Rectangle shrink(Rectangle r, int pad) {
        return new Rectangle(r.x + pad, r.y + pad,
                Math.max(1, r.width - 2 * pad), Math.max(1, r.height - 2 * pad));
    }

    private static void drawCentered(Graphics2D g2, Image img, Rectangle slot) {
        if (img == null) return;
        int iw = img.getWidth(null), ih = img.getHeight(null);
        if (iw <= 0 || ih <= 0) return;

        double sx = slot.width  / (double) iw;
        double sy = slot.height / (double) ih;
        double s  = Math.min(sx, sy);

        int w = (int) Math.round(iw * s);
        int h = (int) Math.round(ih * s);
        int x = slot.x + (slot.width  - w) / 2;
        int y = slot.y + (slot.height - h) / 2;

        g2.drawImage(img, x, y, w, h, null);
    }

    // robust loader: classpath first, then file fallback
    private static BufferedImage loadImage(String name) {
        try {
            try (InputStream in = StartScreen.class.getResourceAsStream("/" + name)) {
                if (in != null) return ImageIO.read(in);
            }
            try (InputStream in = StartScreen.class.getResourceAsStream(name)) {
                if (in != null) return ImageIO.read(in);
            }
            File f = new File(name);
            if (f.exists()) return ImageIO.read(f);
            System.err.println("[StartScreen] Image not found: " + name);
        } catch (Exception ex) {
            System.err.println("[StartScreen] Failed to load " + name + " -> " + ex);
        }
        return null;
    }

    // fractional rect holder
    private static class RectF {
        final float x, y, w, h;
        RectF(float x, float y, float w, float h) { this.x = x; this.y = y; this.w = w; this.h = h; }
    }
}
