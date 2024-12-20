package cn.hamster3.currency.hook;

import cn.hamster3.currency.core.FileManager;
import cn.hamster3.currency.core.IDataManager;
import cn.hamster3.currency.core.Message;
import cn.hamster3.currency.data.CurrencyLog;
import cn.hamster3.currency.data.PlayerData;
import cn.hamster3.currency.event.vault.VaultEconomyGiveEvent;
import cn.hamster3.currency.event.vault.VaultEconomySeeEvent;
import cn.hamster3.currency.event.vault.VaultEconomyTakeEvent;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;

public class VaultEconomyHook extends AbstractEconomy {
    private static final EconomyResponse NOT_IMPLEMENTED_RESPONSE = new EconomyResponse(
            0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "HamsterCurrency未实现该功能~"
    );
    private final IDataManager dataManager;

    public VaultEconomyHook(IDataManager dataManager) {
        this.dataManager = dataManager;
    }

    protected EconomyResponse depositPlayer(PlayerData data, double amount) {
        if (data == null) {
            return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.FAILURE, "玩家账户不存在");
        }
        String type = FileManager.getVaultCurrencyType();
        VaultEconomyGiveEvent event = new VaultEconomyGiveEvent(data.getUuid(), type, amount);
        Bukkit.getPluginManager().callEvent(event);
        amount = event.getAmount();
        if (amount == 0) {
            return new EconomyResponse(amount, data.getPlayerCurrency(type), EconomyResponse.ResponseType.SUCCESS, null);
        }
        double balance = data.getPlayerCurrency(type) + amount;
        if (data.getPlayerCurrency(type) > 0 && balance < 0) {
            return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.FAILURE, "玩家金额超出上限");
        }
        data.setPlayerCurrency(type, balance);
        dataManager.savePlayerData(data);
        dataManager.insertLog(new CurrencyLog(data.getUuid(), data.getPlayerName(), type, "add", amount, balance));
        return new EconomyResponse(amount, data.getPlayerCurrency(type), EconomyResponse.ResponseType.SUCCESS, null);
    }

    protected EconomyResponse withdrawPlayer(PlayerData data, double amount) {
        if (data == null) {
            return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.FAILURE, "玩家账户不存在");
        }
        String type = FileManager.getVaultCurrencyType();
        VaultEconomyTakeEvent event = new VaultEconomyTakeEvent(data.getUuid(), type, amount);
        Bukkit.getPluginManager().callEvent(event);
        amount = event.getAmount();
        if (amount == 0) {
            return new EconomyResponse(amount, data.getPlayerCurrency(type), EconomyResponse.ResponseType.SUCCESS, null);
        }
        if (data.getPlayerCurrency(type) < amount) {
            return new EconomyResponse(amount, data.getPlayerCurrency(type), EconomyResponse.ResponseType.FAILURE, "余额不足");
        }
        double balance = data.getPlayerCurrency(type) - amount;
        data.setPlayerCurrency(type, balance);
        dataManager.savePlayerData(data);
        dataManager.insertLog(new CurrencyLog(data.getUuid(), data.getPlayerName(), type, "take", amount, balance));
        return new EconomyResponse(amount, data.getPlayerCurrency(type), EconomyResponse.ResponseType.SUCCESS, null);
    }

    private boolean has(PlayerData data, double amount) {
        if (data == null) {
            return false;
        }
        return getBalance(data) >= amount;
    }

    private double getBalance(PlayerData data) {
        if (data == null) {
            return 0;
        }
        double currency = data.getPlayerCurrency(FileManager.getVaultCurrencyType());
        VaultEconomySeeEvent event = new VaultEconomySeeEvent(data.getUuid(), FileManager.getVaultCurrencyType(), currency);
        Bukkit.getPluginManager().callEvent(event);
        return event.getResult();
    }

    @Override
    public boolean isEnabled() {
        return FileManager.isVaultHook();
    }

    @Override
    public String getName() {
        return "HamsterCurrency";
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return String.format("%.2f", amount);
    }

    @Override
    public String currencyNamePlural() {
        return Message.currencyNamePlural.getMessage();
    }

    @Override
    public String currencyNameSingular() {
        return Message.currencyNameSingular.getMessage();
    }

    @Override
    public double getBalance(String playerName) {
        return getBalance(dataManager.getPlayerData(playerName));
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return getBalance(dataManager.getPlayerData(player.getUniqueId()));
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(dataManager.getPlayerData(playerName));
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(dataManager.getPlayerData(player.getUniqueId()));
    }

    @Override
    public boolean has(String playerName, double amount) {
        return has(dataManager.getPlayerData(playerName), amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return has(dataManager.getPlayerData(player.getUniqueId()), amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(dataManager.getPlayerData(playerName), amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(dataManager.getPlayerData(player.getUniqueId()), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(dataManager.getPlayerData(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        return withdrawPlayer(dataManager.getPlayerData(player.getUniqueId()), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(dataManager.getPlayerData(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(dataManager.getPlayerData(player.getUniqueId()), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(dataManager.getPlayerData(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        return depositPlayer(dataManager.getPlayerData(player.getUniqueId()), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(dataManager.getPlayerData(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(dataManager.getPlayerData(player.getUniqueId()), amount);
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public boolean hasAccount(String playerName) {
        return true;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return NOT_IMPLEMENTED_RESPONSE;
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return NOT_IMPLEMENTED_RESPONSE;
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return NOT_IMPLEMENTED_RESPONSE;
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return NOT_IMPLEMENTED_RESPONSE;
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return NOT_IMPLEMENTED_RESPONSE;
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return NOT_IMPLEMENTED_RESPONSE;
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return NOT_IMPLEMENTED_RESPONSE;
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return NOT_IMPLEMENTED_RESPONSE;
    }

    @Override
    public List<String> getBanks() {
        return new ArrayList<>();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return true;
    }
}
