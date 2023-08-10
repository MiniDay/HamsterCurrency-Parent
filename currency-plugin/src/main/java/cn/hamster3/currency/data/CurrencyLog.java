package cn.hamster3.currency.data;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class CurrencyLog {
    @NotNull
    private final UUID uuid;
    @NotNull
    private final String playerName;
    /**
     * 货币类型
     */
    @NotNull
    private final String type;
    /**
     * 执行的操作
     */
    @NotNull
    private final String action;
    /**
     * 执行操作的数额
     */
    private final double amount;
    /**
     * 执行操作后的余额
     */
    private final double balance;

    public CurrencyLog(@NotNull UUID uuid, @NotNull String playerName, @NotNull String type, @NotNull String action, double amount, double balance) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.type = type;
        this.action = action;
        this.amount = amount;
        this.balance = balance;
    }

    public @NotNull UUID getUuid() {
        return uuid;
    }

    public @NotNull String getPlayerName() {
        return playerName;
    }

    public @NotNull String getType() {
        return type;
    }

    public @NotNull String getAction() {
        return action;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalance() {
        return balance;
    }
}
