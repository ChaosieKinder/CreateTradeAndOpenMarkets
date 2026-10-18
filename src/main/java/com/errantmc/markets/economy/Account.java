package com.errantmc.markets.economy;

/** Money held by someone in the world, in copper bits: a villager's till or a village treasury. */
public interface Account {
    long balance();

    /** Replaces the balance. Callers check the result is not negative. */
    void set(long copperBits);
}
