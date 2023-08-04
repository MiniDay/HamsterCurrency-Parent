package cn.hamster3.currency.data;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class CurrencyLog {
    @NotNull
    private final UUID uuid;
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

    public CurrencyLog(@NotNull UUID uuid, @NotNull String type, @NotNull String action, double amount, double balance) {
        this.uuid = uuid;
        this.type = type;
        this.action = action;
        this.amount = amount;
        this.balance = balance;
    }

    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    @NotNull
    public String getType() {
        return type;
    }

    @NotNull
    public String getAction() {
        return action;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalance() {
        return balance;
    }
}
