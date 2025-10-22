package game.core;

import java.util.*;

public class GameEngine {

    public static final int TARGET = 21;

    public enum EffectKind {
        SELF_ADD_ROLL,        // GREEN  (+1 to your next roll)
        SELF_MULTIPLY_ROLL,   // BLUE   (x2 to your next roll)
        SELF_SUBTRACT_ROLL,   // PURPLE (-1 to your next roll, clamp ≥0)
        OPP_SUBTRACT_ROLL,    // YELLOW (-1 to opponent next roll, clamp ≥0)
        OPP_DIVIDE_TOTAL      // RED    (opponent total /= 2, immediate)
    }

    public static class PendingEffect {
        public final EffectKind kind;
        public final int magnitude;
        public PendingEffect(EffectKind kind, int magnitude) { this.kind = kind; this.magnitude = magnitude; }
        @Override public String toString() {
            return switch (kind) {
                case SELF_ADD_ROLL      -> "+" + magnitude;
                case SELF_MULTIPLY_ROLL -> "x" + magnitude;
                case SELF_SUBTRACT_ROLL -> "-" + magnitude;
                case OPP_SUBTRACT_ROLL  -> "Opponent -" + magnitude;
                case OPP_DIVIDE_TOTAL   -> "Opponent /" + magnitude;
            };
        }
    }

    public static class PlayerState {
        public final String name;
        public int total = 0;
        // instead of a single boolean, track a BAG of eyes (each eye already knows its effect)
        private final Deque<PendingEffect> eyes = new ArrayDeque<>();
        public PendingEffect nextEffect = null; // roll-time modifier
        public int lastBaseRoll = 0, lastFinalRoll = 0;
        public PlayerState(String name) { this.name = name; }
        // accessors for UI
        public List<PendingEffect> eyesView() { return Collections.unmodifiableList(new ArrayList<>(eyes)); }
        // private boolean hasEyes() { return !eyes.isEmpty(); }           // not needed
        private void addEye(PendingEffect e) { eyes.addLast(e); }          // enqueue
        private PendingEffect takeEye(int index) {                         // remove by index (0..)
            if (index < 0 || index >= eyes.size()) return null;
            Iterator<PendingEffect> it = eyes.iterator();
            int i = 0;
            while (it.hasNext()) {
                PendingEffect p = it.next();
                if (i == index) { it.remove(); return p; }
                i++;
            }
            return null;
        }
        private void clearEyes() { eyes.clear(); }
    }

    public interface Listener {
        default void onRoundStart(int roundIndex, PlayerState user, PlayerState cpu) {}
        default void onRoll(PlayerState who, int baseRoll, int finalRoll, PendingEffect consumed) {}
        // now includes WHICH effect was granted so UI can color the new eye
        default void onEyeGranted(PlayerState who, PendingEffect granted) {}
        default void onEffectChosen(PlayerState who, PendingEffect effect) {}
        default void onEndgameTriggered(PlayerState firstAtOrAboveTarget) {}
        default void onGameOver(String resultText, PlayerState user, PlayerState cpu) {}
    }

    private final Random rng;
    private final Listener listener;
    private final PlayerState user, cpu;

    private int roundIndex = 1;
    private boolean endgame = false;
    private PlayerState firstReached = null;

    public GameEngine(String userName, String cpuName, Listener listener, long seed) {
        this.rng = new Random(seed == 0 ? System.nanoTime() : seed);
        this.listener = (listener != null) ? listener : new Listener(){};
        this.user = new PlayerState(userName);
        this.cpu  = new PlayerState(cpuName);
    }

    public PlayerState user() { return user; }
    public PlayerState cpu()  { return cpu; }
    public boolean isEndgame(){ return endgame; }
    public int roundIndex()   { return roundIndex; }

    // ----- Eyes / effects -----

    /** 5 equiprobable effects with fixed magnitudes (equal odds). */
    public PendingEffect randomEffect() {
        return switch (rng.nextInt(5)) {
            case 0 -> new PendingEffect(EffectKind.OPP_DIVIDE_TOTAL,   2); // RED
            case 1 -> new PendingEffect(EffectKind.OPP_SUBTRACT_ROLL,  1); // YELLOW
            case 2 -> new PendingEffect(EffectKind.SELF_ADD_ROLL,      1); // GREEN
            case 3 -> new PendingEffect(EffectKind.SELF_MULTIPLY_ROLL, 2); // BLUE
            default -> new PendingEffect(EffectKind.SELF_SUBTRACT_ROLL,1); // PURPLE
        };
    }

    /** Use a specific eye (by index) that the owner currently has. Returns the effect used. */
    public PendingEffect useEye(PlayerState owner, int index) {
        if (endgame) return null;                    // no buffs in endgame
        PendingEffect eff = owner.takeEye(index);
        if (eff == null) return null;
        PlayerState opp = (owner == user) ? cpu : user;

        switch (eff.kind) {
            case OPP_DIVIDE_TOTAL -> {               // immediate
                opp.total = Math.max(0, opp.total / eff.magnitude);
                listener.onEffectChosen(owner, eff);
            }
            case OPP_SUBTRACT_ROLL -> {              // queue on opponent’s next roll
                opp.nextEffect = eff;
                listener.onEffectChosen(owner, eff);
            }
            default -> {                              // self roll-time effects
                owner.nextEffect = eff;
                listener.onEffectChosen(owner, eff);
            }
        }
        return eff;
    }

    /** CPU helper: use the first eye (FIFO). Returns effect or null. */
    public PendingEffect useFirstEye(PlayerState owner) { return useEye(owner, 0); }

    // ----- Game loop -----

    public boolean playRound() {
        if (endgame) {
            PlayerState chaser = (firstReached == user) ? cpu : user;
            int r = rollOnce(chaser);
            chaser.total += r;
            if (chaser.total >= TARGET) { checkForGameOver(); return false; }
            return true;
        }

        listener.onRoundStart(roundIndex, user, cpu);

        int ur = rollOnce(user);
        int cr = rollOnce(cpu);
        user.total += ur;
        cpu.total  += cr;

        // Grant eye to lower roll (tie = none); eye has its EFFECT at grant time
        if (ur < cr) {
            PendingEffect g = randomEffect();
            user.addEye(g);
            listener.onEyeGranted(user, g);
        } else if (cr < ur) {
            PendingEffect g = randomEffect();
            cpu.addEye(g);
            listener.onEyeGranted(cpu, g);
        }

        // Trigger endgame if someone crossed
        if (!endgame && (user.total >= TARGET || cpu.total >= TARGET)) {
            endgame = true;
            firstReached = (user.total >= TARGET && cpu.total >= TARGET)
                    ? (Math.abs(TARGET - user.total) <= Math.abs(TARGET - cpu.total) ? user : cpu)
                    : (user.total >= TARGET ? user : cpu);

            // wipe eyes & pending effects for both
            user.clearEyes(); cpu.clearEyes();
            user.nextEffect = cpu.nextEffect = null;

            listener.onEndgameTriggered(firstReached);

            if (user.total >= TARGET && cpu.total >= TARGET) { checkForGameOver(); return false; }
        }

        roundIndex++;
        return true;
    }

    private int rollOnce(PlayerState p) {
        int base = 1 + rng.nextInt(6);
        int out = base;
        PendingEffect used = p.nextEffect;
        p.nextEffect = null;

        if (used != null) {
            switch (used.kind) {
                case SELF_ADD_ROLL      -> out = base + used.magnitude;
                case SELF_MULTIPLY_ROLL -> out = base * used.magnitude;
                case SELF_SUBTRACT_ROLL -> out = Math.max(0, base - used.magnitude);
                case OPP_SUBTRACT_ROLL  -> out = Math.max(0, base - used.magnitude);
                default -> {}
            }
        }
        p.lastBaseRoll = base; p.lastFinalRoll = out;
        listener.onRoll(p, base, out, used);
        return out;
    }

    private void checkForGameOver() {
        int du = Math.abs(TARGET - user.total);
        int dc = Math.abs(TARGET - cpu.total);
        String result = (du < dc) ? "You win! (" + user.total + " vs " + cpu.total + ")"
                        : (dc < du) ? "CPU wins! (" + cpu.total + " vs " + user.total + ")"
                                    : "It's a tie at " + user.total + " and " + cpu.total + ".";
        listener.onGameOver(result, user, cpu);
    }

    public void reset() {
        user.total = cpu.total = 0;
        user.clearEyes(); cpu.clearEyes();
        user.nextEffect = cpu.nextEffect = null;
        user.lastBaseRoll = user.lastFinalRoll = 0;
        cpu.lastBaseRoll = cpu.lastFinalRoll = 0;
        roundIndex = 1; endgame = false; firstReached = null;
    }
}