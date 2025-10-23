import javax.swing.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.border.EmptyBorder;

import game.core.GameEngine;

public class GameClass extends JPanel {

    // ---- Engine ----
    private final GameEngine engine = new GameEngine("You", "CPU", new EngineListener(), 0);

    // ---- HUD ----
    private final JLabel roundLbl = new JLabel("Round 1");
    private final JLabel userLbl = new JLabel("You: 0");
    private final JLabel cpuLbl = new JLabel("CPU: 0");
    private final JTextArea log = new JTextArea(6, 36);
    private final JButton rollBtn = new JButton("Roll");
    private final JButton resetBtn = new JButton("Reset");

    // === Eye strip (8 user eyes, closed by default) ===
    private static final int NUM_EYES = 8;
    private final JButton[] userEyeBtns = new JButton[NUM_EYES];
    private final EyeAnimator eyeAnim = new EyeAnimator();

    // UI state for each eye slot
    private final GameEngine.EffectKind[] eyeSlotKind = new GameEngine.EffectKind[NUM_EYES];
    private final boolean[] eyeSlotOpen = new boolean[NUM_EYES];

    public GameClass() {
        setPreferredSize(new Dimension(1920, 1080)); // 1080p target
        setLayout(new BorderLayout());

        // ===== Background container (loads from assets/ when available) =====
        JLabel background = new JLabel(loadBackgroundIcon());
        // Use a layered pane so we can draw a composed background (with table) and
        // place HUD/HMD/bottom as an overlay on top (keeps them visible and
        // interactive).
        JLayeredPane layered = new JLayeredPane();
        layered.setLayout(null);
        add(layered, BorderLayout.CENTER);

        // Overlay holds HUD (north) + HMD center + bottom UI (south)
        JPanel overlay = new JPanel(new BorderLayout());
        overlay.setOpaque(false);

        // --- Center HMD image ---
        ImageIcon hmdIcon = loadHMDIcon("HMD_smile.png", 300, 1000);
        JLabel hmdLabel = new JLabel(hmdIcon);
        hmdLabel.setHorizontalAlignment(SwingConstants.CENTER);
        hmdLabel.setVerticalAlignment(SwingConstants.CENTER);
        final JPanel hmdContainer = new JPanel(new BorderLayout());
        hmdContainer.setOpaque(false);
        hmdContainer.setBorder(new EmptyBorder(0, 0, 0, 0));
        hmdContainer.add(hmdLabel, BorderLayout.CENTER);
        overlay.add(hmdContainer, BorderLayout.CENTER);

        // ===== Top HUD =====
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        top.setOpaque(false);
        for (JLabel l : new JLabel[] { roundLbl, userLbl, cpuLbl }) {
            l.setForeground(Color.WHITE);
        }
        top.add(roundLbl);
        top.add(userLbl);
        top.add(cpuLbl);
        overlay.add(top, BorderLayout.NORTH);

        // ===== Bottom stack: Eyes row (NORTH) -> Log (CENTER) -> Buttons (SOUTH) =====
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);

        // Eyes row (8 eyes)
        JPanel eyesRow = new JPanel(new GridLayout(1, NUM_EYES, 2, 0));
        eyesRow.setOpaque(false);
        for (int i = 0; i < NUM_EYES; i++) {
            JButton b = new JButton();
            styleEyeButton(b);
            b.setEnabled(false);
            // initially set a frame; the ComponentListener below will rescale it to fit
            b.setIcon(eyeAnim.frameWhite(0));
            final int idx = i;
            b.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    onClickUserEye(idx);
                }
            });
            // center the icon and use a fixed preferred size to avoid clipping
            b.setHorizontalAlignment(SwingConstants.CENTER);
            b.setVerticalAlignment(SwingConstants.CENTER);
            b.setPreferredSize(new Dimension(240, 240));
            b.setMinimumSize(new Dimension(240, 240));
            b.setMaximumSize(new Dimension(240, 240));
            userEyeBtns[i] = b;
            eyesRow.add(b);
        }
        bottom.add(eyesRow, BorderLayout.NORTH);

        // Log (bottom-center)
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        JScrollPane sc = new JScrollPane(log);
        sc.setOpaque(false);
        sc.getViewport().setOpaque(false);
        bottom.add(sc, BorderLayout.CENTER);

        // Buttons (bottom-most)
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        btns.setOpaque(false);
        btns.add(rollBtn);
        btns.add(resetBtn);
        bottom.add(btns, BorderLayout.SOUTH);

        // bottom UI goes into overlay south
        overlay.add(bottom, BorderLayout.SOUTH);

        // add both layers
        layered.add(background, JLayeredPane.DEFAULT_LAYER);
        layered.add(overlay, JLayeredPane.PALETTE_LAYER);

        // Resize handler: recompute composed background (with table) and rescale HMD
        Runnable rescaleAll = () -> {
            int w = layered.getWidth();
            int h = layered.getHeight();
            if (w <= 0 || h <= 0)
                return;
            background.setBounds(0, 0, w, h);
            overlay.setBounds(0, 0, w, h);
            composeBackgroundWithTable(background);
            int size = (int) (Math.min(w, h));
            ImageIcon icon = loadHMDIcon("HMD_smile.png", size, size);
            hmdLabel.setIcon(icon);
            hmdContainer.setBorder(new EmptyBorder(0, 0, 0, 0));
        };
        layered.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                rescaleAll.run();
            }
        });
        // initial
        layered.setBounds(0, 0, getWidth(), getHeight());
        background.setBounds(0, 0, getWidth(), getHeight());
        overlay.setBounds(0, 0, getWidth(), getHeight());
        composeBackgroundWithTable(background);
        rescaleAll.run();

        // ===== Wiring =====
        rollBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cpuMaybeUseEyeBeforeRound(); // CPU may spend an eye (not shown)
                boolean cont = engine.playRound(); // play step
                refreshHUD();
                if (!cont)
                    rollBtn.setEnabled(false);
            }
        });

        resetBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                engine.reset();
                clearAllUserEyeUI();
                log.setText("");
                rollBtn.setEnabled(true);
                append("New game! First to reach " + GameEngine.TARGET + " triggers endgame.");
                roundLbl.setText("Round 1");
                refreshHUD();
            }
        });

        // Greet
        append("Welcome. Click Roll to begin.");
        refreshHUD();
    }

    // ----------------- Helpers -----------------

    private void refreshHUD() {
        userLbl.setText("You: " + engine.user().total + " (last " + engine.user().lastFinalRoll + ")");
        cpuLbl.setText("CPU: " + engine.cpu().total + " (last " + engine.cpu().lastFinalRoll + ")");
    }

    private void append(String s) {
        log.append(s + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    // Load background ImageIcon from assets/ if present, otherwise fall back to
    // project root.
    private ImageIcon loadBackgroundIcon() {
        String[] candidates = { "assets/casinobackground.png", "casinobackground.png" };
        for (String p : candidates) {
            File f = findExistingFile(p);
            if (f != null) {
                try {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null)
                        return new ImageIcon(img);
                } catch (IOException ignored) {
                }
            }
        }
        // fallback: empty icon
        return new ImageIcon();
    }

    // Scale an ImageIcon to fit within maxW x maxH, preserve aspect ratio, and
    // center it

    // Try multiple candidate prefixes so image lookups work whether the app is run
    // from
    // the project folder or the workspace root. Returns first existing File or
    // null.
    private static File findExistingFile(String rel) {
        String[] prefixes = { "", "Eye For An Eye/", "./Eye For An Eye/", "./" };
        for (String pfx : prefixes) {
            File f = new File(pfx + rel);
            if (f.exists())
                return f;
        }
        return null;
    }

    // Load an HMD image (from assets/HMD or fallback) and scale to maxW x maxH
    // preserving aspect ratio
    private ImageIcon loadHMDIcon(String fileName, int maxW, int maxH) {
        String rel = "assets/HMD/" + fileName;
        File f = findExistingFile(rel);
        if (f == null)
            f = findExistingFile(fileName);
        if (f == null)
            return new ImageIcon();
        try {
            BufferedImage img = ImageIO.read(f);
            if (img == null)
                return new ImageIcon();
            double scale = Math.min((double) maxW / img.getWidth(), (double) maxH / img.getHeight());
            int nw = Math.max(1, (int) Math.round(img.getWidth() * scale));
            int nh = Math.max(1, (int) Math.round(img.getHeight() * scale));
            Image scaled = img.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
            // center on a canvas the size of maxW x maxH
            BufferedImage canvas = new BufferedImage(maxW, maxH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            int x = (maxW - nw) / 2;
            int y = (maxH - nh);
            g.drawImage(scaled, x, y, null);
            g.dispose();
            return new ImageIcon(canvas);
        } catch (IOException e) {
            return new ImageIcon();
        }
    }

    // Compose the table image onto the background icon so it appears behind the UI
    private void composeBackgroundWithTable(JLabel background) {
        // find base background image
        File bf = findExistingFile("assets/casinobackground.png");
        if (bf == null)
            bf = findExistingFile("casinobackground.png");
        if (bf == null)
            return;
        try {
            BufferedImage base = ImageIO.read(bf);
            if (base == null)
                return;
            int bw = background.getWidth();
            int bh = background.getHeight();
            if (bw <= 0 || bh <= 0) {
                background.setIcon(new ImageIcon(base));
                return;
            }
            // scale base to fit background area
            Image baseScaled = base.getScaledInstance(bw, bh, Image.SCALE_SMOOTH);
            BufferedImage composed = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = composed.createGraphics();
            g.drawImage(baseScaled, 0, 0, null);

            // find table image and draw it near bottom
            File tf = findExistingFile("assets/table.png");
            if (tf == null)
                tf = findExistingFile("table.png");
            if (tf != null) {
                BufferedImage timg = ImageIO.read(tf);
                if (timg != null) {
                    double scale = (double) bw / (double) timg.getWidth();
                    int th = Math.max(1, (int) Math.round(timg.getHeight() * scale));
                    Image tScaled = timg.getScaledInstance(bw, th, Image.SCALE_SMOOTH);
                    int y = bh - th; // draw flush to bottom
                    g.drawImage(tScaled, 0, y, null);
                }
            }

            g.dispose();
            background.setIcon(new ImageIcon(composed));
        } catch (IOException ignored) {
        }
    }

    private void styleEyeButton(JButton b) {
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
    }

    private void cpuMaybeUseEyeBeforeRound() {
        if (engine.isEndgame())
            return;
        List<GameEngine.PendingEffect> list = engine.cpu().eyesView();
        if (!list.isEmpty() && Math.random() < 0.6) {
            var eff = engine.useFirstEye(engine.cpu());
            if (eff != null) {
                append("CPU used Eye: " + eff);
                if (eff.kind == GameEngine.EffectKind.OPP_DIVIDE_TOTAL)
                    refreshHUD(); // red is immediate
            }
        }
    }

    // When an eye in slot is clicked → consume the matching effect from engine
    private void onClickUserEye(int slot) {
        if (!eyeSlotOpen[slot])
            return;

        int idx = findMatchingEyeIndex(slot);
        if (idx < 0)
            return; // safety

        var eff = engine.useEye(engine.user(), idx);
        if (eff == null)
            return;

        append("You used Eye: " + eff);
        eyeAnim.flashEffect(userEyeBtns[slot], eff.kind);

        // Animate close in that color, then mark closed
        eyeAnim.playBlink(userEyeBtns[slot], colorFor(eff.kind));
        eyeSlotOpen[slot] = false;
        eyeSlotKind[slot] = null;
        userEyeBtns[slot].setEnabled(false);

        if (eff.kind == GameEngine.EffectKind.OPP_DIVIDE_TOTAL)
            refreshHUD(); // immediate effect
    }

    /**
     * Find the index of an eye with the same kind in the engine's queue.
     * We fill UI left→right as we grant, and the engine stores eyes FIFO.
     * If some earlier ones were spent, indices shift; so scan by kind.
     */
    private int findMatchingEyeIndex(int slot) {
        GameEngine.EffectKind want = eyeSlotKind[slot];
        if (want == null)
            return -1;
        List<GameEngine.PendingEffect> list = engine.user().eyesView();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).kind == want)
                return i;
        }
        return -1;
    }

    private void lightNextUserEye(GameEngine.EffectKind kind) {
        for (int i = 0; i < NUM_EYES; i++) {
            if (!eyeSlotOpen[i]) {
                eyeSlotOpen[i] = true;
                eyeSlotKind[i] = kind;
                userEyeBtns[i].setEnabled(true);
                // animate to open in the correct color and stay open
                userEyeBtns[i].setIcon(eyeAnim.playOpen(userEyeBtns[i], colorFor(kind)));
                eyeAnim.pulse(userEyeBtns[i], colorFor(kind), /* cycles */2, /* durationMs */700, /* amplitude */0.10);
                return;
            }
        }
        append("Your Eye bar is full (8). Consider using some!");
    }

    private void clearAllUserEyeUI() {
        for (int i = 0; i < NUM_EYES; i++) {
            eyeSlotOpen[i] = false;
            eyeSlotKind[i] = null;
            userEyeBtns[i].setEnabled(false);
            userEyeBtns[i].setIcon(eyeAnim.frameWhite(0));
        }
    }

    private EyeAnimator.ColorKey colorFor(GameEngine.EffectKind k) {
        return switch (k) {
            case OPP_DIVIDE_TOTAL -> EyeAnimator.ColorKey.RED;
            case OPP_SUBTRACT_ROLL -> EyeAnimator.ColorKey.YELLOW;
            case SELF_ADD_ROLL -> EyeAnimator.ColorKey.GREEN;
            case SELF_MULTIPLY_ROLL -> EyeAnimator.ColorKey.BLUE;
            case SELF_SUBTRACT_ROLL -> EyeAnimator.ColorKey.PURPLE;
        };
    }

    // ----------------- Engine listener -----------------

    private class EngineListener implements GameEngine.Listener {
        @Override
        public void onRoundStart(int r, GameEngine.PlayerState u, GameEngine.PlayerState c) {
            roundLbl.setText("Round " + r);
            append("— Round " + r + " —");
        }

        @Override
        public void onRoll(GameEngine.PlayerState who, int base, int fin, GameEngine.PendingEffect used) {
            String name = (who == engine.user()) ? "You" : "CPU";
            append(name + " rolled " + base + " → " + fin + (used != null ? (" [used " + used + "]") : ""));
        }

        @Override
        public void onEyeGranted(GameEngine.PlayerState who, GameEngine.PendingEffect granted) {
            if (who == engine.user()) {
                append("You rolled lower — Eye granted: " + granted);
                lightNextUserEye(granted.kind);
            } else {
                append("CPU gained an Eye.");
            }
        }

        @Override
        public void onEffectChosen(GameEngine.PlayerState who, GameEngine.PendingEffect eff) {
            if (eff.kind == GameEngine.EffectKind.OPP_DIVIDE_TOTAL)
                refreshHUD();
        }

        @Override
        public void onEndgameTriggered(GameEngine.PlayerState first) {
            append(((first == engine.user()) ? "You" : "CPU") + " hit " + GameEngine.TARGET +
                    ". Endgame: the other keeps rolling until they also reach it. No more buffs.");
            clearAllUserEyeUI();
        }

        @Override
        public void onGameOver(String result, GameEngine.PlayerState u, GameEngine.PlayerState c) {
            append("== " + result + " ==");
        }
    }

    // ========================================================================
    // EyeAnimator: loads eye frames (your files), tints per color, animates
    // ========================================================================
    private static class EyeAnimator {
        enum ColorKey {
            WHITE, RED, YELLOW, GREEN, BLUE, PURPLE
        }

        // Exact filenames provided via Google Drive
        private static final String[] FRAME_FILES = {
                "assets/eyes/IMG_3184.PNG",
                "assets/eyes/IMG_3185.PNG",
                "assets/eyes/IMG_3186.PNG",
                "assets/eyes/IMG_3187.PNG",
                "assets/eyes/IMG_3188.PNG",
                "assets/eyes/IMG_3189.PNG",
                "assets/eyes/IMG_3190.PNG"
        };

        // UI tuning
        private static final int TARGET_W = 240;
        private static final int TARGET_H = 240;
        private static final int FPS = 24;
        private static final float TINT_STRENGTH = 0.65f;

        private final Map<ColorKey, List<ImageIcon>> cache = new ConcurrentHashMap<>();

        EyeAnimator() {
            cache.put(ColorKey.WHITE, loadFrames(null)); // base frames
        }

        ImageIcon frameWhite(int idx) {
            List<ImageIcon> list = cache.get(ColorKey.WHITE);
            if (list.isEmpty())
                return new ImageIcon();
            idx = Math.max(0, Math.min(idx, list.size() - 1));
            return list.get(idx);
        }

        /** Animate open (closed → open). Leaves final frame set (stays open). */
        ImageIcon playOpen(JButton btn, ColorKey color) {
            List<ImageIcon> frames = frames(color);
            play(btn, frames, false);
            return frames.get(frames.size() - 1);
        }

        /** Blink (open then close) in given color. */
        void playBlink(JButton btn, ColorKey color) {
            List<ImageIcon> frames = frames(color);
            List<ImageIcon> seq = new ArrayList<>(frames);
            for (int i = frames.size() - 2; i >= 0; i--)
                seq.add(frames.get(i));
            play(btn, seq, true);
        }

        /** When an Eye is used, flash in the effect color (blink). */
        void flashEffect(JButton btn, GameEngine.EffectKind kind) {
            playBlink(btn, switch (kind) {
                case OPP_DIVIDE_TOTAL -> ColorKey.RED;
                case OPP_SUBTRACT_ROLL -> ColorKey.YELLOW;
                case SELF_ADD_ROLL -> ColorKey.GREEN;
                case SELF_MULTIPLY_ROLL -> ColorKey.BLUE;
                case SELF_SUBTRACT_ROLL -> ColorKey.PURPLE;
            });
        }

        /** Returns the fully-open (last) frame for the given color. */
        private ImageIcon openIcon(ColorKey color) {
            List<ImageIcon> frames = frames(color);
            return frames.get(frames.size() - 1);
        }

        /** Scale an icon by a factor (center-cropped to current target size). */
        private static ImageIcon scaleIcon(ImageIcon src, double factor) {
            int w = Math.max(1, (int) Math.round(src.getIconWidth() * factor));
            int h = Math.max(1, (int) Math.round(src.getIconHeight() * factor));
            Image scaled = src.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);

            // draw onto our target-size canvas so the button doesn’t jump
            BufferedImage canvas = new BufferedImage(TARGET_W, TARGET_H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            int x = (TARGET_W - w) / 2;
            int y = (TARGET_H - h) / 2;
            g.drawImage(scaled, x, y, null);
            g.dispose();
            return new ImageIcon(canvas);
        }

        /**
         * Gentle pulse animation (grow/shrink) for granted eyes.
         * cycles: how many in/out pulses; durationMs: total time; amplitude: 0.08..0.15
         * looks nice.
         */
        void pulse(JButton btn, ColorKey color, int cycles, int durationMs, double amplitude) {
            final ImageIcon base = openIcon(color); // fully-open icon at target size
            final int fps = Math.max(20, FPS); // keep it smooth
            final int steps = Math.max(1, (durationMs * fps) / 1000);
            final double twoPi = Math.PI * 2.0;
            final int[] i = { 0 };

            Timer t = new Timer(1000 / fps, e -> {
                double progress = (double) i[0] / (double) steps; // 0..1
                double wave = Math.sin(progress * twoPi * cycles); // -1..1
                double scale = 1.0 + amplitude * wave; // 1±amp
                btn.setIcon(scaleIcon(base, scale));
                i[0]++;
                if (i[0] > steps) {
                    ((Timer) e.getSource()).stop();
                    btn.setIcon(base); // snap back to clean open icon
                }
            });
            t.setRepeats(true);
            t.start();
        }

        // --- internals ---

        private List<ImageIcon> frames(ColorKey ck) {
            return cache.computeIfAbsent(ck, key -> {
                List<ImageIcon> base = cache.get(ColorKey.WHITE);
                if (key == ColorKey.WHITE)
                    return base;
                Color tint = switch (key) {
                    case RED -> Color.RED;
                    case YELLOW -> Color.YELLOW;
                    case GREEN -> Color.GREEN;
                    case BLUE -> Color.BLUE;
                    case PURPLE -> new Color(160, 32, 240);
                    default -> Color.WHITE;
                };
                List<ImageIcon> tinted = new ArrayList<>(base.size());
                for (ImageIcon icon : base)
                    tinted.add(tintIcon(icon, tint, TINT_STRENGTH));
                return tinted;
            });
        }

        private void play(JButton btn, List<ImageIcon> seq, boolean fast) {
            final int periodMs = fast ? Math.max(15, 1000 / FPS) : 1000 / FPS;
            final int[] i = { 0 };
            Timer t = new Timer(periodMs, e -> {
                btn.setIcon(seq.get(i[0]));
                i[0]++;
                if (i[0] >= seq.size())
                    ((Timer) e.getSource()).stop();
            });
            t.setRepeats(true);
            t.start();
        }

        private List<ImageIcon> loadFrames(Color tint) {
            List<ImageIcon> out = new ArrayList<>(FRAME_FILES.length);
            for (String name : FRAME_FILES) {
                try {
                    File f = findExistingFile(name);
                    BufferedImage img = null;
                    if (f != null && f.exists())
                        img = ImageIO.read(f);
                    if (img == null)
                        img = makePlaceholderFrame();
                    Image scaled = img.getScaledInstance(TARGET_W, TARGET_H, Image.SCALE_SMOOTH);
                    ImageIcon base = new ImageIcon(scaled);
                    out.add(tint == null ? base : tintIcon(base, tint, TINT_STRENGTH));
                } catch (Exception ex) {
                    BufferedImage ph = makePlaceholderFrame();
                    ImageIcon placeholder = new ImageIcon(ph);
                    out.add(tint == null ? placeholder : tintIcon(placeholder, tint, TINT_STRENGTH));
                }
            }
            return out;
        }

        private BufferedImage makePlaceholderFrame() {
            BufferedImage img = new BufferedImage(TARGET_W, TARGET_H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(40, 40, 40, 255));
            g.fillRect(0, 0, TARGET_W, TARGET_H);
            g.setColor(new Color(200, 200, 200, 200));
            int cx = TARGET_W / 2, cy = TARGET_H / 2, r = Math.min(TARGET_W, TARGET_H) / 4;
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
            g.dispose();
            return img;
        }

        private static ImageIcon tintIcon(ImageIcon src, Color tint, float strength) {
            int w = src.getIconWidth(), h = src.getIconHeight();
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.drawImage(src.getImage(), 0, 0, null);
            g.setComposite(AlphaComposite.SrcAtop.derive(Math.max(0f, Math.min(1f, strength))));
            g.setColor(tint);
            g.fillRect(0, 0, w, h);
            g.dispose();
            return new ImageIcon(out);
        }
    }
}