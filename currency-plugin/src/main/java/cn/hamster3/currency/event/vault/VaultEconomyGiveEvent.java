package cn.hamster3.currency.event.vault;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class VaultEconomyGiveEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();

    @NotNull
    private final UUID uuid;
    @NotNull
    private final String currencyID;
    private double amount;

    public VaultEconomyGiveEvent(@NotNull UUID uuid, @NotNull String currencyID, double amount) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.uuid = uuid;
        this.currencyID = currencyID;
        this.amount = amount;
    }

    public @NotNull UUID getUuid() {
        return uuid;
    }

    public @NotNull String getCurrencyID() {
        return currencyID;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
