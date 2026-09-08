# 每日打卡 - Android 版

基于 Android + Material Design + Room 的每日打卡应用。

## 技术栈

- Kotlin/Java + Android SDK 34
- Material Components 1.11.0
- Room 2.6.1 (本地数据库)
- Navigation 2.7.7 (底部导航)
- ViewBinding

## 核心功能

- **打卡管理**: 按日期添加/删除/勾选任务，连续打卡天数统计
- **学习计时**: 番茄钟模式，目标设置，震动提醒，前台通知
- **收支记账**: 收入/支出记录，每日结余，消费上限提醒
- **统计可视化**: 月历/周历/饼图三模式切换，柱状图
- **快捷记录**: 打卡页集成学习和消费快捷入口
- **每日目标**: 学习目标(分钟) + 消费上限(元)

## 构建

```bash
# 确保 local.properties 中配置了 sdk.dir
./gradlew assembleDebug
```

输出: `app/build/outputs/apk/debug/app-debug.apk`

## 最低要求

- Android 7.0 (API 24)
- 编译 SDK 34
