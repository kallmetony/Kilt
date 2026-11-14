package xyz.bluspring.kilt.helpers;

public class LivingDeathBridge {
    // Bridge flag: true when die() was called through LivingEntity.die() (Architectury mixin already fired).
    // When false, the Forge LivingDeathEvent came from a mod that overrides die() without super — needs manual Architectury bridge.
    public static final ThreadLocal<Boolean> DIE_HANDLED = ThreadLocal.withInitial(() -> false);
}
