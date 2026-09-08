# 每日打卡 - Windows 桌面版

基于 JavaFX 17 + SQLite 的桌面打卡应用，功能与安卓版对等。

## 技术栈

- Java 17
- JavaFX 17.0.2
- SQLite 3.45.1.0 (sqlite-jdbc)
- Maven 构建
- jpackage 打包为 EXE

## 项目结构

```
src/main/java/com/dailycheck/
├── Main.java                    应用入口
├── DatabaseHelper.java          SQLite 数据库
├── models/                      数据模型
│   ├── Task.java
│   ├── StudyRecord.java
│   └── ExpenseRecord.java
├── controllers/                 界面控制器
│   ├── TasksController.java     打卡
│   ├── StudyController.java     学习
│   ├── ExpenseController.java   记账
│   ├── StatsController.java     统计
│   └── SettingsController.java  设置
├── charts/                      自绘图表
│   ├── SimpleBarChart.java
│   └── SimplePieChart.java
└── utils/                       工具类
    ├── DateUtils.java
    └── PrefsManager.java

src/main/resources/
├── fxml/                        界面布局
└── style.css                    样式
```

## 构建与运行

```bash
# 运行（需 JDK 17+）
mvn javafx:run

# 打包 JAR
mvn package -DskipTests -Djpackage.skip=true

# 构建 EXE（Windows 环境）
mvn package
```

## 数据存储

- 数据库: `%APPDATA%/dailycheck/dailycheck.db`
- 偏好设置: Java Preferences API (注册表)
