package cn.hamster3.currency.command.vault;

import cn.hamster3.currency.core.IDataManager;
import cn.hamster3.currency.data.CurrencyLog;
import cn.hamster3.currency.data.CurrencyType;
import cn.hamster3.currency.data.PlayerData;

public class VaultTakeCommand extends VaultAdminSetCommand {
    public VaultTakeCommand(IDataManager dataManager) {
        super(
                dataManager,
                "take",
                "扣除玩家金币",
                "currency.take"
        );
    }

    @Override
    public void doSet(PlayerData data, CurrencyType type, double amount) {
        double balance = data.getPlayerCurrency(type.getId()) - amount;
        data.setPlayerCurrency(type.getId(), balance);
        dataManager.insertLog(new CurrencyLog(data.getUuid(), data.getPlayerName(), type.getId(), "take", amount, balance));
    }
}
