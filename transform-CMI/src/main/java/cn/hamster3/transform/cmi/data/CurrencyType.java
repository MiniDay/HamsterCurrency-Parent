package cn.hamster3.transform.cmi.data;

/**
 * 货币类型
 */
public class CurrencyType {
    /**
     * 货币识别符
     */
    private final String id;
    /**
     * 是否允许转账
     */
    private final boolean canTransfer;

    public CurrencyType(String id, boolean canTransfer) {
        this.id = id;
        this.canTransfer = canTransfer;
    }

    public String getId() {
        return id;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isCanTransfer() {
        return canTransfer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CurrencyType that = (CurrencyType) o;

        return id.equalsIgnoreCase(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
