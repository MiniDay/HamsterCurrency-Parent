# [HamsterCurrency](https://github.com/MiniDay/HamsterCurrency-Parent)

[![](https://jitpack.io/v/cn.hamster3/HamsterCurrency.svg)](https://jitpack.io/#cn.hamster3/HamsterCurrency)  
仓鼠的多货币经济插件

# 依赖前置

- [HamsterAPI](https://github.com/MiniDay/HamsterAPI/releases)
- [HamsterService](https://github.com/MiniDay/HamsterService/releases) （仅跨服模式需要）
- [Vault](https://www.spigotmc.org/resources/vault.34315/) （仅开启Vault经济系统时需要）
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (非必须)

# 变量列表

| 变量               | 描述      |
|:-----------------|:--------|
| %Currency_<货币名称% | 玩家的货币余额 |

# 开发者

## 依赖导入

### Maven

添加仓库:

```xml

<repositories>
    <repository>
        <id>airgame-repo</id>
        <url>https://maven.airgame.net/maven-public/</url>
    </repository>
</repositories>
```

添加导入:

```xml

<dependency>
    <groupId>cn.hamster3</groupId>
    <artifactId>HamsterCurrency</artifactId>
    <version>2.2.2</version>
</dependency>
```

### Gradle

添加仓库:

```groovy
allprojects {
    repositories {
        maven { url 'https://maven.airgame.net/maven-public/' }
    }
}
```

添加导入:

```groovy
dependencies {
    implementation 'cn.hamster3:HamsterCurrency:2.2.2'
}
```

## API

请参考[CurrencyAPI](currency-plugin/src/main/java/cn/hamster3/currency/api/CurrencyAPI.java)
