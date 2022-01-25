package cn.hamster3.transform.cmi;

import cn.hamster3.transform.cmi.data.PlayerData;

import java.io.File;
import java.sql.*;
import java.util.HashSet;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    private static final HashSet<PlayerData> playerData = new HashSet<>();

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入 cmi 数据库路径:");
        String databasePath = scanner.nextLine();
        System.out.println("请输入数据库主机名: ");
        String host = scanner.nextLine();
        System.out.println("请输入数据库端口号: ");
        String port = scanner.nextLine();
        System.out.println("请输入数据库用户名: ");
        String user = scanner.nextLine();
        System.out.println("请输入数据库密码: ");
        String password = scanner.nextLine();
        System.out.println("请输入数据库库名: ");
        String database = scanner.nextLine();

        File databaseFile = new File(databasePath);
        System.out.println("开始读取 CMI 数据.");
        scanData(databaseFile);
        System.out.println("开始保存数据到 HamsterCurrency 中.");
        uploadData(host, port, user, password, database);
        System.out.printf("数据保存完成，共计转移了 %d 个玩家数据存档.%n", playerData.size());
    }

    private static void scanData(File database) throws ClassNotFoundException, SQLException {
        System.out.println("加载 sqlite 数据库驱动...");
        Class.forName("org.sqlite.JDBC");
        System.out.println("建立 sqlite 数据库连接...");
        Connection connection = DriverManager.getConnection("jdbc:sqlite://" + database.getAbsolutePath());
        Statement statement = connection.createStatement();
        ResultSet set = statement.executeQuery("SELECT player_uuid, username, Balance FROM users;");
        while (set.next()) {
            try {
                UUID uuid = UUID.fromString(set.getString("player_uuid"));
                String username = set.getString("username");
                PlayerData data = getPlayerData(uuid, username);
                double balance = set.getDouble("Balance");
                if (data.getPlayerCurrency("金币") >= balance) {
                    return;
                }
                data.setPlayerCurrency("金币", balance);
                System.out.printf("已加载 %s(%s) 的存档: %.2f金币%n", uuid, username, balance);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        statement.close();
        connection.close();
        System.out.printf("已加载 %d 个玩家的数据存档.%n", playerData.size());
    }

    private static void uploadData(String host, String port, String user, String password, String database) throws ClassNotFoundException, SQLException {
        System.out.println("加载 MySQL 数据库驱动...");
        Class.forName("com.mysql.cj.jdbc.Driver");
        System.out.println("建立 MySQL 数据库连接...");
        Connection connection = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8&autoReconnect=true&useSSL=false",
                user,
                password
        );
        Statement statement = connection.createStatement();
        System.out.println("切换至数据库...");
        statement.execute(String.format("CREATE DATABASE IF NOT EXISTS %s DEFAULT CHARACTER SET ='UTF8';", database));
        statement.execute(String.format("USE %s;", database));
        System.out.println("检查数据表...");
        statement.execute("CREATE TABLE IF NOT EXISTS hamster_currency_player_data(" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "data TEXT" +
                ");");
        System.out.println("开始更新数据库...");
        for (PlayerData data : playerData) {
            String sql = String.format(
                    "REPLACE INTO hamster_currency_player_data VALUES('%s', '%s');",
                    data.getUuid().toString(),
                    data.saveToJson().toString().replace("'", "\\'")
            );
            try {
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                System.out.println("执行 sql " + sql + " 时遇到了一个异常:");
                e.printStackTrace();
            }
        }
        statement.close();
        connection.close();
    }

    private static PlayerData getPlayerData(UUID uuid, String name) {
        for (PlayerData data : playerData) {
            if (data.getUuid().equals(uuid)) {
                return data;
            }
        }
        PlayerData data = new PlayerData(uuid, name);
        playerData.add(data);
        return data;
    }

}
