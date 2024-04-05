package cn.hamster3.currency.core;

import cn.hamster3.api.HamsterAPI;
import cn.hamster3.currency.HamsterCurrency;
import cn.hamster3.currency.data.CurrencyLog;
import cn.hamster3.currency.data.CurrencyType;
import cn.hamster3.currency.data.PlayerData;
import cn.hamster3.service.bukkit.api.ServiceMessageAPI;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static cn.hamster3.currency.HamsterCurrency.getLogUtils;

public class SQLDataManager implements IDataManager {
    private final HamsterCurrency plugin;
    private final JsonParser parser;

    private final String database;
    private final DataSource datasource;

    private final Map<UUID, PlayerData> playerData;
    private final HashSet<CurrencyType> currencyTypes;

    public SQLDataManager(HamsterCurrency plugin) throws SQLException, ClassNotFoundException {
        this.plugin = plugin;
        parser = new JsonParser();
        playerData = new ConcurrentHashMap<>();
        currencyTypes = new HashSet<>();

        ConfigurationSection datasourceConfig = FileManager.getPluginConfig().getConfigurationSection("datasource");
        database = datasourceConfig.getString("database");
        datasource = HamsterAPI.getHikariDataSource(datasourceConfig);

        Connection connection = datasource.getConnection();
        Statement statement = connection.createStatement();
        statement.execute("CREATE TABLE IF NOT EXISTS " + database + ".hamster_currency_player_data(" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "data TEXT" +
                ") CHARACTER SET = utf8mb4;");
        statement.execute("CREATE TABLE IF NOT EXISTS " + database + ".hamster_currency_logs(" +
                "uuid VARCHAR(36) NOT NULL," +
                "player_name VARCHAR(36) NOT NULL," +
                "type VARCHAR(36) NOT NULL," +
                "action VARCHAR(36) NOT NULL," +
                "amount DOUBLE NOT NULL," +
                "balance DOUBLE NOT NULL," +
                "time DATETIME NOT NULL DEFAULT NOW()," +
                "INDEX idx_uuid(uuid)," +
                "INDEX idx_name(player_name)" +
                ") CHARACTER SET = utf8mb4;");
        statement.execute("CREATE TABLE IF NOT EXISTS " + database + ".hamster_currency_settings(" +
                "title VARCHAR(64) PRIMARY KEY," +
                "data TEXT" +
                ") CHARACTER SET = utf8mb4;");

        statement.close();
        connection.close();
    }

    public void uploadConfigToSQL() {
        getLogUtils().info("重载配置文件...");
        FileManager.reload(plugin);
        FileConfiguration config = FileManager.getPluginConfig();
        getLogUtils().info("配置文件重载完成!");
        try {
            getLogUtils().info("将配置文件上传至数据库...");
            Connection connection = datasource.getConnection();
            Statement statement = connection.createStatement();
            String data = Base64.getEncoder().encodeToString(config.saveToString().getBytes(StandardCharsets.UTF_8));
            statement.executeUpdate(String.format(
                    "REPLACE INTO " + database + ".hamster_currency_settings VALUES('%s', '%s');",
                    "pluginConfig",
                    data
            ));
            statement.close();
            connection.close();
            getLogUtils().info("配置文件上传完成!");
        } catch (SQLException e) {
            getLogUtils().error(e, "插件上传 pluginConfig 至数据库时遇到了一个异常: ");
        }
        loadConfig(config);
        ServiceMessageAPI.sendServiceMessage("HamsterCurrency", "uploadConfigToSQL");
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public void loadConfigFromSQL() {
        try {
            getLogUtils().info("从数据库中下载配置文件...");
            Connection connection = datasource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet set = statement.executeQuery("SELECT * FROM " + database + ".hamster_currency_settings;");
            while (set.next()) {
                String title = set.getString("title");
                String data = new String(Base64.getDecoder().decode(set.getString("data")), StandardCharsets.UTF_8);
                switch (title) {
                    case "pluginConfig": {
                        YamlConfiguration config = new YamlConfiguration();
                        try {
                            config.loadFromString(data);
                        } catch (InvalidConfigurationException e) {
                            getLogUtils().error(e, "插件加载 %s 时遇到了一个异常: ", title);
                        }
                        loadConfig(config);
                    }
                }
            }
            statement.close();
            connection.close();
            getLogUtils().info("配置文件下载完成!");
        } catch (SQLException e) {
            getLogUtils().error(e, "插件从数据库中下载 pluginConfig 时遇到了一个异常: ");
        }
    }

    private void loadConfig(FileConfiguration config) {
        getLogUtils().info("加载配置文件...");
        currencyTypes.clear();
        ConfigurationSection currencyTypesConfig = config.getConfigurationSection("currencyTypes");
        for (String key : currencyTypesConfig.getKeys(false)) {
            try {
                currencyTypes.add(new CurrencyType(currencyTypesConfig.getConfigurationSection(key)));
                getLogUtils().warning("已加载货币类型: %s", key);
            } catch (Exception e) {
                getLogUtils().error(e, "加载货币类型 %s 时出现了一个错误: ", key);
            }
        }
        FileManager.setPluginConfig(config);
        getLogUtils().info("配置文件加载完成!");
    }

    public void importFromOtherPluginData(String database, String table, String uuidCol, String nameCol, String moneyCol, String currencyType) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            getLogUtils().info("开始从其他插件的数据库存档中导入数据: ");
            getLogUtils().info("数据库名: %s", database);
            getLogUtils().info("数据表名: %s", table);
            getLogUtils().info("玩家uuid列名: %s", uuidCol);
            getLogUtils().info("玩家名称列名: %s", nameCol);
            getLogUtils().info("玩家经济列名: %s", moneyCol);
            getLogUtils().info("导入至经济类型: %s", currencyType);
            try {
                Connection connection = datasource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet set = statement.executeQuery(String.format("SELECT * FROM %s.%s;", database, table));
                while (set.next()) {
                    try {
                        UUID uuid = UUID.fromString(set.getString(uuidCol));
                        String name = set.getString(nameCol);
                        double money = set.getDouble(moneyCol);
                        PlayerData data = getPlayerData(uuid);
                        if (data == null) {
                            data = new PlayerData(uuid, name);
                            playerData.put(data.getUuid(), data);
                        }
                        data.setPlayerCurrency(currencyType, money);
                        getLogUtils().info("已从其他插件中加载了玩家 %s 的存档数据.", data.getUuid());
                    } catch (Exception e) {
                        getLogUtils().error(e, "导入某一条数据时发生了一个错误: ");
                    }
                }
                for (PlayerData data : playerData.values()) {
                    statement.executeUpdate(String.format(
                            "REPLACE INTO " + database + ".hamster_currency_player_data VALUES('%s', '%s');",
                            data.getUuid().toString(),
                            data.saveToJson().toString()
                    ));
                    getLogUtils().info("已保存玩家 %s 的存档数据.", data.getUuid());
                    ServiceMessageAPI.sendServiceMessage("HamsterCurrency", "savedPlayerData", data.getUuid().toString());
                }
                statement.close();
                connection.close();
            } catch (SQLException e) {
                getLogUtils().error(e, "从其他插件中导入数据时发生了一个异常:");
            }
        });
    }

    @Override
    public void onEnable() {
        getLogUtils().info("从数据库中读取玩家数据...");
        try {
            Connection connection = datasource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet set = statement.executeQuery("SELECT * FROM " + database + ".hamster_currency_player_data;");
            while (set.next()) {
                String uuid = set.getString("uuid");
                String string = set.getString("data");
                try {
                    PlayerData data = new PlayerData(parser.parse(string).getAsJsonObject());
                    playerData.put(data.getUuid(), data);
                } catch (Exception e) {
                    getLogUtils().error(e, "从数据库中读取玩家 %s 的存档( %s )时出现了一个异常: ", uuid, string);
                }
            }
            set.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            getLogUtils().error(e, "从数据库中读取玩家数据时出现了一个异常:");
        }
        getLogUtils().info("从数据库中读取玩家数据完成!");
    }

    @Override
    public void onDisable() {
        // 因为SQL模式使用HamsterService前置
        // 服务器之间数据实时同步
        // 所以关服时无需保存任何数据
    }

    @Override
    public void loadConfig() {
        if (FileManager.isMainServer()) {
            uploadConfigToSQL();
        } else {
            loadConfigFromSQL();
        }
    }

    @Override
    public void reloadConfig() {
        ServiceMessageAPI.sendServiceMessage("HamsterCurrency", "reload");
    }

    @Override
    public void loadPlayerData(UUID uuid) {
        try {
            Connection connection = datasource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet set = statement.executeQuery(String.format(
                    "SELECT * FROM " + database + ".hamster_currency_player_data WHERE uuid='%s';",
                    uuid
            ));
            PlayerData data;
            if (set.next()) {
                String string = set.getString("data");
                try {
                    data = new PlayerData(parser.parse(string).getAsJsonObject());
                } catch (Exception e) {
                    getLogUtils().error(e, "从数据库中读取玩家 %s 的存档( %s )时出现了一个异常: ", uuid, string);
                    statement.close();
                    return;
                }
            } else {
                data = new PlayerData(uuid);
                getLogUtils().info("初始化玩家 %s 的存档数据.", data.getUuid());
            }
            playerData.remove(data.getUuid());
            playerData.put(data.getUuid(), data);
            set.close();
            statement.close();
            connection.close();
            getLogUtils().info("已加载玩家 %s 的存档数据.", data.getUuid());
        } catch (SQLException e) {
            getLogUtils().error(e, "加载玩家 %s 的存档数据时出错!", uuid);
        }
    }

    @Override
    public void savePlayerData(PlayerData data) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Connection connection = datasource.getConnection();
                Statement statement = connection.createStatement();
                statement.executeUpdate(String.format(
                        "REPLACE INTO " + database + ".hamster_currency_player_data VALUES('%s', '%s');",
                        data.getUuid().toString(),
                        data.saveToJson().toString()
                ));
                statement.close();
                connection.close();
            } catch (SQLException e) {
                getLogUtils().error(e, "保存玩家 %s 的存档数据时出错!", data.getUuid());
            }
            getLogUtils().info("已保存玩家 %s 的存档数据.", data.getUuid());
            ServiceMessageAPI.sendServiceMessage(
                    "HamsterCurrency",
                    "savedPlayerData",
                    data.getUuid().toString()
            );
        });
    }

    @Override
    public void insertLog(CurrencyLog log) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = datasource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO " + database + ".hamster_currency_logs VALUES(?, ?, ?, ?, ?, ?, DEFAULT);"
                )) {
                    statement.setString(1, log.getUuid().toString());
                    statement.setString(2, log.getPlayerName());
                    statement.setString(3, log.getType());
                    statement.setString(4, log.getAction());
                    statement.setDouble(5, log.getAmount());
                    statement.setDouble(6, log.getBalance());
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                getLogUtils().error(e);
            }
        });
    }

    @Override
    public PlayerData getPlayerData(UUID uuid) {
        return playerData.get(uuid);
    }

    @Override
    public PlayerData getPlayerData(String name) {
        for (PlayerData data : playerData.values()) {
            if (name.equalsIgnoreCase(data.getPlayerName())) {
                return data;
            }
        }
        return null;
    }

    @Override
    public ArrayList<PlayerData> getPlayerData() {
        return new ArrayList<>(playerData.values());
    }

    @Override
    public CurrencyType getCurrencyType(String id) {
        for (CurrencyType type : currencyTypes) {
            if (type.getId().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public Set<CurrencyType> getCurrencyTypes() {
        return currencyTypes;
    }
}
