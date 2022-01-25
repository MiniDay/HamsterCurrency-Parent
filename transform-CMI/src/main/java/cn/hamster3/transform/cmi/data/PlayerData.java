package cn.hamster3.transform.cmi.data;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private final String playerName;
    private final HashMap<String, Double> playerCurrencies;

    public PlayerData(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        playerCurrencies = new HashMap<>();
    }


    public JsonObject saveToJson() {
        JsonObject object = new JsonObject();
        object.addProperty("uuid", uuid.toString());
        object.addProperty("playerName", playerName);
        JsonObject playerCurrenciesJson = new JsonObject();
        for (Map.Entry<String, Double> entry : playerCurrencies.entrySet()) {
            playerCurrenciesJson.addProperty(entry.getKey(), entry.getValue());
        }
        object.add("playerCurrencies", playerCurrenciesJson);
        return object;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerCurrency(String type, double amount) {
        playerCurrencies.put(type, amount);
    }

    public double getPlayerCurrency(String type) {
        return playerCurrencies.getOrDefault(type, 0D);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayerData that = (PlayerData) o;

        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
