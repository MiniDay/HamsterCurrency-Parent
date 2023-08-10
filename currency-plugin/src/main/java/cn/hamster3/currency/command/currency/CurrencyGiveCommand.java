package cn.hamster3.currency.command.currency;

import cn.hamster3.currency.core.IDataManager;
import cn.hamster3.currency.data.CurrencyLog;
import cn.hamster3.currency.data.CurrencyType;
import cn.hamster3.currency.data.PlayerData;

public class CurrencyGiveCommand extends CurrencyAdminSetCommand {
    public CurrencyGiveCommand(IDataManager dataManager) {
        super(
                dataManager,
                "give",
                "为玩家货币添加余额",
                "currency.give"
        );
    }

    @Override
    protected void doSet(PlayerData data, CurrencyType type, double amount) {
        double balance = data.getPlayerCurrency(type.getId()) + amount;
        data.setPlayerCurrency(type.getId(), balance);
        dataManager.insertLog(new CurrencyLog(data.getUuid(), data.getPlayerName(), type.getId(), "add", amount, balance));
    }

}
