package cn.hamster3.currency.core;

import cn.hamster3.currency.HamsterCurrency;
import cn.hamster3.currency.data.CurrencyType;
import cn.hamster3.currency.data.PlayerData;
import cn.hamster3.service.bukkit.api.ServiceMessageAPI;
import com.google.gson.JsonParser;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static cn.hamster3.currency.HamsterCurrency.getLogUtils;

public class SQLDataManager implements IDataManager {
    private final HamsterCurrency plugin;
    private final JsonParser parser;

    private final String database;
    private final HikariDataSource dataSource;

    private final HashSet<PlayerData> playerData;
    private final HashSet<CurrencyType> currencyTypes;

    public SQLDataManager(HamsterCurrency plugin) throws SQLException, ClassNotFoundException {
        this.plugin = plugin;
        parser = new JsonParser();
        playerData = new HashSet<>();
        currencyTypes = new HashSet<>();

        ConfigurationSection datasourceConfig = FileManager.getPluginConfig().getConfigurationSection("datasource");
        database = datasourceConfig.getString("database");
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setDriverClassName(datasourceConfig.getString("driver"));
        hikariConfig.setJdbcUrl(datasourceConfig.getString("url"));
        hikariConfig.setUsername(datasourceConfig.getString("user"));
        hikariConfig.setPassword(datasourceConfig.getString("password"));

        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(1);

        dataSource = new HikariDataSource(hikariConfig);
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        statement.execute("CREATE TABLE IF NOT EXISTS " + database + ".hamster_currency_player_data(" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "data TEXT" +
                ") CHARACTER SET = utf8mb4;");
        statement.execute("CREATE TABLE IF NOT EXISTS " + database + ".hamster_currency_settings(" +
                "title VARCHAR(64) PRIMARY KEY," +
                "data TEXT" +
                ") CHARACTER SET = utf8mb4;");

        statement.close();
        connection.close();
    }

    public void uploadConfigToSQL() {
        getLogUtils().info("??????????????????...");
        FileManager.reload(plugin);
        FileConfiguration config = FileManager.getPluginConfig();
        getLogUtils().info("????????????????????????!");
        try {
            getLogUtils().info("?????????????????????????????????...");
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            String data = Base64.getEncoder().encodeToString(config.saveToString().getBytes(StandardCharsets.UTF_8));
            statement.executeUpdate(String.format(
                    "REPLACE INTO " + database + ".hamster_currency_settings VALUES('%s', '%s');",
                    "pluginConfig",
                    data
            ));
            statement.close();
            connection.close();
            getLogUtils().info("????????????????????????!");
        } catch (SQLException e) {
            getLogUtils().error(e, "???????????? pluginConfig ????????????????????????????????????: ");
        }
        loadConfig(config);
        ServiceMessageAPI.sendServiceMessage("HamsterCurrency", "uploadConfigToSQL");
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public void loadConfigFromSQL() {
        try {
            getLogUtils().info("?????????????????????????????????...");
            Connection connection = dataSource.getConnection();
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
                            getLogUtils().error(e, "???????????? %s ????????????????????????: ", title);
                        }
                        loadConfig(config);
                    }
                }
            }
            statement.close();
            connection.close();
            getLogUtils().info("????????????????????????!");
        } catch (SQLException e) {
            getLogUtils().error(e, "??????????????????????????? pluginConfig ????????????????????????: ");
        }
    }

    private void loadConfig(FileConfiguration config) {
        getLogUtils().info("??????????????????...");
        currencyTypes.clear();
        ConfigurationSection currencyTypesConfig = config.getConfigurationSection("currencyTypes");
        for (String key : currencyTypesConfig.getKeys(false)) {
            try {
                currencyTypes.add(new CurrencyType(currencyTypesConfig.getConfigurationSection(key)));
                getLogUtils().warning("?????????????????????: %s", key);
            } catch (Exception e) {
                getLogUtils().error(e, "?????????????????? %s ????????????????????????: ", key);
            }
        }
        FileManager.setPluginConfig(config);
        getLogUtils().info("????????????????????????!");
    }

    public void importFromOtherPluginData(String database, String table, String uuidCol, String nameCol, String moneyCol, String currencyType) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            getLogUtils().info("??????????????????????????????????????????????????????: ");
            getLogUtils().info("????????????: %s", database);
            getLogUtils().info("????????????: %s", table);
            getLogUtils().info("??????uuid??????: %s", uuidCol);
            getLogUtils().info("??????????????????: %s", nameCol);
            getLogUtils().info("??????????????????: %s", moneyCol);
            getLogUtils().info("?????????????????????: %s", currencyType);
            try {
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet set = statement.executeQuery(String.format("SELECT * FROM %s.%s;", database, table));
                synchronized (playerData) {
                    while (set.next()) {
                        try {
                            UUID uuid = UUID.fromString(set.getString(uuidCol));
                            String name = set.getString(nameCol);
                            double money = set.getDouble(moneyCol);
                            PlayerData data = getPlayerData(uuid);
                            if (data == null) {
                                data = new PlayerData(uuid, name);
                                playerData.add(data);
                            }
                            data.setPlayerCurrency(currencyType, money);
                            getLogUtils().info("???????????????????????????????????? %s ???????????????.", data.getUuid());
                        } catch (Exception e) {
                            getLogUtils().error(e, "?????????????????????????????????????????????: ");
                        }
                    }
                }
                for (PlayerData data : playerData) {
                    statement.executeUpdate(String.format(
                            "REPLACE INTO " + database + ".hamster_currency_player_data VALUES('%s', '%s');",
                            data.getUuid().toString(),
                            data.saveToJson().toString()
                    ));
                    getLogUtils().info("??????????????? %s ???????????????.", data.getUuid());
                    ServiceMessageAPI.sendServiceMessage("HamsterCurrency", "savedPlayerData", data.getUuid().toString());
                }
                statement.close();
                connection.close();
            } catch (SQLException e) {
                getLogUtils().error(e, "??????????????????????????????????????????????????????:");
            }
        });
    }

    @Override
    public void onEnable() {
        getLogUtils().info("?????????????????????????????????...");
        try {
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet set = statement.executeQuery("SELECT * FROM " + database + ".hamster_currency_player_data;");
            synchronized (playerData) {
                while (set.next()) {
                    String uuid = set.getString("uuid");
                    String string = set.getString("data");
                    try {
                        PlayerData data = new PlayerData(parser.parse(string).getAsJsonObject());
                        playerData.add(data);
                    } catch (Exception e) {
                        getLogUtils().error(e, "??????????????????????????? %s ?????????( %s )????????????????????????: ", uuid, string);
                    }
                }
            }
            set.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            getLogUtils().error(e, "?????????????????????????????????????????????????????????:");
        }
        getLogUtils().info("???????????????????????????????????????!");
    }

    @Override
    public void onDisable() {
        // ??????SQL????????????HamsterService??????
        // ?????????????????????????????????
        // ???????????????????????????????????????
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
            Connection connection = dataSource.getConnection();
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
                    getLogUtils().error(e, "??????????????????????????? %s ?????????( %s )????????????????????????: ", uuid, string);
                    statement.close();
                    return;
                }
            } else {
                data = new PlayerData(uuid);
                getLogUtils().info("??????????????? %s ???????????????.", data.getUuid());
            }
            synchronized (playerData) {
                playerData.remove(data);
                playerData.add(data);
            }
            set.close();
            statement.close();
            connection.close();
            getLogUtils().info("??????????????? %s ???????????????.", data.getUuid());
        } catch (SQLException e) {
            getLogUtils().error(e, "???????????? %s ????????????????????????!", uuid);
        }
    }

    @Override
    public void savePlayerData(PlayerData data) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                statement.executeUpdate(String.format(
                        "REPLACE INTO " + database + ".hamster_currency_player_data VALUES('%s', '%s');",
                        data.getUuid().toString(),
                        data.saveToJson().toString()
                ));
                statement.close();
                connection.close();
            } catch (SQLException e) {
                getLogUtils().error(e, "???????????? %s ????????????????????????!", data.getUuid());
            }
            getLogUtils().info("??????????????? %s ???????????????.", data.getUuid());
            ServiceMessageAPI.sendServiceMessage(
                    "HamsterCurrency",
                    "savedPlayerData",
                    data.getUuid().toString()
            );
        });
    }

    @Override
    public PlayerData getPlayerData(UUID uuid) {
        synchronized (playerData) {
            for (PlayerData data : playerData) {
                if (uuid.equals(data.getUuid())) {
                    return data;
                }
            }
        }
        return null;
    }

    @Override
    public PlayerData getPlayerData(String name) {
        synchronized (playerData) {
            for (PlayerData data : playerData) {
                if (name.equalsIgnoreCase(data.getPlayerName())) {
                    return data;
                }
            }
        }
        return null;
    }

    @Override
    public ArrayList<PlayerData> getPlayerData() {
        synchronized (playerData) {
            return new ArrayList<>(playerData);
        }
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
