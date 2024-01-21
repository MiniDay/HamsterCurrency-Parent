package cn.hamster3.currency.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@SuppressWarnings("unused")
public class CurrencyChangeEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();
    @NotNull
    private final UUID playerUUID;
    @NotNull
    private final String currencyID;
    private final double oldAmount;
    private final double newAmount;

    public CurrencyChangeEvent(@NotNull UUID playerUUID, @NotNull String currencyID, double oldAmount, double newAmount) {
        super(true);
        this.playerUUID = playerUUID;
        this.currencyID = currencyID;
        this.oldAmount = oldAmount;
        this.newAmount = newAmount;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @NotNull
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    @NotNull
    public String getCurrencyID() {
        return currencyID;
    }

    public double getOldAmount() {
        return oldAmount;
    }

    public double getNewAmount() {
        return newAmount;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
