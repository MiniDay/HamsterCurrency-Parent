package cn.hamster3.currency.command.currency;

import cn.hamster3.currency.core.IDataManager;
import cn.hamster3.currency.data.CurrencyLog;
import cn.hamster3.currency.data.CurrencyType;
import cn.hamster3.currency.data.PlayerData;

public class CurrencyTakeCommand extends CurrencyAdminSetCommand {
    public CurrencyTakeCommand(IDataManager dataManager) {
        super(
                dataManager,
                "take",
                "为玩家货币扣除余额",
                "currency.take"
        );
    }

    @Override
    protected void doSet(PlayerData data, CurrencyType type, double amount) {
        double balance = data.getPlayerCurrency(type.getId()) - amount;
        data.setPlayerCurrency(type.getId(), balance);
        dataManager.insertLog(new CurrencyLog(data.getUuid(), type.getId(), "take", amount, balance));
    }
}
