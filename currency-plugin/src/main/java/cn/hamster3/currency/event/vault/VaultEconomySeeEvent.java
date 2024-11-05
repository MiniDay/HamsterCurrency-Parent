package cn.hamster3.currency.event.vault;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class VaultEconomySeeEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();

    @NotNull
    private final UUID uuid;
    @NotNull
    private final String currencyID;
    private double result;

    public VaultEconomySeeEvent(@NotNull UUID uuid, @NotNull String currencyID, double result) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.uuid = uuid;
        this.currencyID = currencyID;
        this.result = result;
    }

    public @NotNull UUID getUuid() {
        return uuid;
    }

    public @NotNull String getCurrencyID() {
        return currencyID;
    }

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
        this.result = result;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
