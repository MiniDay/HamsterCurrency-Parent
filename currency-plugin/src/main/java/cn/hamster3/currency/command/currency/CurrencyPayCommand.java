package cn.hamster3.currency.command.currency;

import cn.hamster3.api.HamsterAPI;
import cn.hamster3.api.command.CommandExecutor;
import cn.hamster3.currency.core.FileManager;
import cn.hamster3.currency.core.IDataManager;
import cn.hamster3.currency.core.Message;
import cn.hamster3.currency.data.CurrencyLog;
import cn.hamster3.currency.data.CurrencyType;
import cn.hamster3.currency.data.PlayerData;
import cn.hamster3.service.bukkit.api.ServiceMessageAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class CurrencyPayCommand extends CommandExecutor {
    private final IDataManager dataManager;

    public CurrencyPayCommand(IDataManager dataManager) {
        super(
                "pay",
                "向其他玩家转账",
                "currency.pay",
                Message.notHasPermission.toString(),
                new String[]{"玩家", "货币类型", "数额"}
        );
        this.dataManager = dataManager;
    }

    @Override
    public boolean isPlayerCommand() {
        return true;
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Message.notInputPlayerName.toString());
            return true;
        }
        PlayerData toData = dataManager.getPlayerData(args[1]);
        if (toData == null) {
            sender.sendMessage(Message.playerNotFound.toString());
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(Message.notInputCurrencyType.toString());
            return true;
        }
        CurrencyType type = dataManager.getCurrencyType(args[2]);
        if (type == null) {
            sender.sendMessage(Message.currencyTypeNotFound.toString());
            return true;
        }
        if (!type.isCanTransfer()) {
            sender.sendMessage(Message.currencyTypeCantTransfer.toString().replace("%type%", type.getId()));
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(Message.notInputPayAmount.toString());
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Message.amountNumberError.toString());
            return true;
        }
        if (amount <= 0) {
            sender.sendMessage(Message.amountNumberError.toString());
            return true;
        }
        Player player = (Player) sender;
        PlayerData fromData = dataManager.getPlayerData(player.getUniqueId());
        if (fromData.getPlayerCurrency(type.getId()) < amount) {
            sender.sendMessage(
                    Message.currencyNotEnough.toString()
                            .replace("%type%", type.getId())
            );
            return true;
        }
        double fromBalance = fromData.getPlayerCurrency(type.getId()) - amount;
        fromData.setPlayerCurrency(type.getId(), fromBalance);
        double toBalance = toData.getPlayerCurrency(type.getId()) + amount;
        toData.setPlayerCurrency(type.getId(), toBalance);
        dataManager.savePlayerData(fromData);
        dataManager.savePlayerData(toData);
        dataManager.insertLog(new CurrencyLog(fromData.getUuid(), fromData.getPlayerName(), type.getId(), "payOut", amount, fromBalance));
        dataManager.insertLog(new CurrencyLog(toData.getUuid(), toData.getPlayerName(), type.getId(), "payIn", amount, toBalance));
        sender.sendMessage(
                Message.paySuccess.toString()
                        .replace("%player%", toData.getPlayerName())
                        .replace("%type%", type.getId())
                        .replace("%amount%", String.format("%.2f", amount))
        );
        if (FileManager.isUseBC()) {
            ServiceMessageAPI.sendPlayerMessage(
                    toData.getUuid(),
                    Message.receivePay.toString()
                            .replace("%player%", player.getName())
                            .replace("%type%", type.getId())
                            .replace("%amount%", String.format("%.2f", amount))
            );
        } else {
            Player toPlayer = Bukkit.getPlayer(toData.getUuid());
            if (toPlayer != null) {
                toPlayer.sendMessage(
                        Message.receivePay.toString()
                                .replace("%player%", player.getName())
                                .replace("%type%", type.getId())
                                .replace("%amount%", String.format("%.2f", amount))
                );
            }
        }
        return true;
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        switch (args.length) {
            case 2: {
                return HamsterAPI.getOnlinePlayersName(args[1]);
            }
            case 3: {
                List<String> types = dataManager.getCurrencyTypes().stream().map(CurrencyType::getId).collect(Collectors.toList());
                types = HamsterAPI.startWithIgnoreCase(types, args[2]);
                if (types.size() > 10) {
                    types = types.subList(0, 9);
                }
                return types;
            }
        }
        return null;
    }
}
