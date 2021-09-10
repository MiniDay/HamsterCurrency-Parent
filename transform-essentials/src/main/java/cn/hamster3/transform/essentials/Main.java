package cn.hamster3.transform.essentials;

import cn.hamster3.transform.essentials.data.PlayerData;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.yaml.YamlFormat;
import com.electronwill.nightconfig.yaml.YamlParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    private static final YamlParser parser = new YamlParser(YamlFormat.defaultInstance());
    private static final HashSet<PlayerData> playerData = new HashSet<>();

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
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

        File file = new File(System.getProperty("user.dir"));
        File[] files = file.listFiles();
        if (files == null) {
            return;
        }
        for (File subFile : files) {
            System.out.println("开始扫描文件夹: " + subFile.getAbsolutePath());
            if (!subFile.isDirectory()) {
                continue;
            }
            scanServer(subFile);
            System.out.println("文件夹扫描完成: " + subFile.getAbsolutePath());
        }
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

    private static void scanServer(File folder) {
        if (!folder.isDirectory()) {
            System.out.println(folder.getAbsolutePath() + " 不是一个文件夹. 跳过扫描.");
            return;
        }
        File pluginFolder = new File(folder, "plugins");
        if (!pluginFolder.isDirectory()) {
            System.out.println(pluginFolder.getAbsolutePath() + " 不是一个文件夹. 跳过扫描.");
            return;
        }
        File essentialFolder = new File(pluginFolder, "Essentials");
        if (!essentialFolder.isDirectory()) {
            System.out.println(essentialFolder.getAbsolutePath() + " 不是一个文件夹. 跳过扫描.");
            return;
        }
        File userdataFolder = new File(essentialFolder, "userdata");
        if (!userdataFolder.isDirectory()) {
            System.out.println(userdataFolder.getAbsolutePath() + " 不是一个文件夹. 跳过扫描.");
            return;
        }
        File[] files = userdataFolder.listFiles();
        if (files == null) {
            return;
        }
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                scanUserData(file);
            } catch (Exception e) {
                System.out.println("读取文件 " + file.getAbsolutePath() + " 时出现了一个异常:");
                e.printStackTrace();
            }
            if (i % 100 == 0) {
                System.out.println("已完成: (" + i + "/" + files.length + ")");
            }
        }
    }

    private static void scanUserData(File file) {
        UUID uuid = UUID.fromString(file.getName().substring(0, 36));
        Config config = parser.parse(file, FileNotFoundAction.THROW_ERROR, StandardCharsets.UTF_8);

        if (config.contains("npc")) {
            boolean isNPC = config.get("npc");
            if (isNPC) {
                return;
            }
        }
        if (!config.contains("lastAccountName")) {
            return;
        }
        if (!config.contains("money")) {
            return;
        }
        String lastAccountName = config.get("lastAccountName");
        double money = Double.parseDouble(config.get("money"));
        PlayerData data = getPlayerData(uuid, lastAccountName);
        if (data.getPlayerCurrency("金币") >= money) {
            return;
        }
        data.setPlayerCurrency("金币", money);
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
