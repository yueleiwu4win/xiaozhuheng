# 小珠衡（Android 离线记账 App）

纯本地、离线、无账号的个人记账 App。数据只保存在本机 SQLite 数据库，`AndroidManifest`
刻意**不声明 INTERNET 权限**，从系统层面保证不联网。

> 产品设计见 [docs/产品设计文档.md](docs/产品设计文档.md)。

当前基础版本为 **1.0**（versionCode 3），以 Git 标签 `v1.0` 固定保存。后续体验优化可以从此版本继续开发；版本记录见 [CHANGELOG.md](CHANGELOG.md)。

当前 feature 分支提供 **1.1.4 主题体验版**：设置 → 外观 → 界面风格，可切换经典、绢本山水、青玉雅集与墨金藏书。宋式已重设计为独立山水账页，采用浅绢纸面、棕墨操作、朱红算盘印章与笔搁。选择「背景 → 主题默认」可看到山水效果；原人物背景仍可选择。青玉与墨金使用独立首页布局，分别采用玉色概览与通栏账页；青玉推荐浅色，墨金推荐深色。墨金首页按新的双栏原稿重排，横向分类、紧凑账目和底部通栏记账按钮；相册照片背景继续保留，升级不重置背景选择。原稿纹理可在「背景 → 主题默认」查看。开发与截图验证说明见 [主题系统与扩展说明](docs/主题系统与扩展说明.md)。

## 功能（V1.0）

- **启动**：使用用户提供的汉服女子算盘图片作为桌面、安装和启动图标；只显示一个图标启动窗口，必要数据就绪后直接进入首页，取消后续全屏封面；分类持续缓存，新记账同步显示分类与默认选中项。
- **记一笔**：四行四列金额键盘、可滚动分类网格、日期到天、备注（≤50 字）
  - 金额上方明确切换**支出 / 收入**，分类网格随类型切换；数字键盘只用于输入金额
  - 打开时日期默认为**今天**；上次使用的分类跨重启记忆，方便连续记账
  - 首页右下角 **记一笔** 进入记账页；编辑时隐藏底部导航
  - 金额与收支固定在上方，分类独立滚动；键盘右列为日期、备注、再记、保存
  - 日期支持今天 / 昨天 / 日历；备注在独立弹窗编辑，显示是否已填写
  - **再记**：保存后清空金额与备注、保留收支类型、分类和日期；编辑时同位置为**清零**，只重置输入金额
  - 保存与修改成功不弹提示；已有修改时返回会提醒，记账页进入和返回直接切换，退出时保持全屏尺寸与按钮外观
- **分类管理**：内置 13 个支出分类 + 5 个收入分类；收入 / 支出切换，自定义新增 / 改名 / 换图标（40 个）/ 换颜色（12 色）/ 长按拖动排序 / 归档
  - 有账单引用的分类禁止硬删除，自动改为归档（历史统计不受影响）
  - 低频操作收进更多菜单，提供上移 / 下移替代拖动；已归档分类默认折叠
- **账单明细**：支出 + 收入混排（收入带 `+` 号、绿色区分）；按日分组、按天显示当日支出合计
  - 月度概览独立展示支出 / 收入 / 结余（收入减支出），不受筛选影响；筛选结果另列合计
  - **筛选**：全部 / 支出 / 收入 + 分类；收支类型与分类联动；**搜索备注关键字**入口固定在顶部
  - 日分组明确展示对应支出 / 收入合计，分类与备注分行显示
  - 月份左右切换，**点击月份文字直接打开月份选择器**
  - **左滑任意账单露出「删除」按钮**，二次确认后删除，成功直接刷新列表，不弹成功提示或撤销入口
  - 编辑页删除也先确认，成功后直接返回原列表
- **统计**（收入 / 支出切换，实时 SQL 聚合；年份/月份可点击直接选择）
  - 默认月度：总额、环比、日均、最大单笔、每日趋势与分类占比 / 排行
  - 年度分析：年度 / 历年 / 分类三页，12 个月折线或柱状图、同比、月度排行、历年趋势与类别 TOP5 对比
  - 点击统计分类可查看对应月份或全年账单（含归档分类），点击账单可编辑；月度排行可跳回对应月份分析
- **底部导航**：明细 - 统计 - 设置；记账与分类管理页隐藏导航
- **界面**：暖白墨绿主题、统一卡片与数字层级；背景蒙层跟随应用选择的深浅主题
- **数据导出 / 导入**
  - CSV：UTF-8 BOM + RFC 4180 转义 + 公式注入中和，Excel 直接打开不乱码
  - JSON：完整备份（formatVersion 2：分类含 `isPreset`/`createdAt`、账单含**整数分** `amountCents`、并含**设置项**）
  - 两种途径：SAF 保存到用户选定的位置 / 系统分享面板（微信、邮件、网盘）
  - **恢复备份**：先选文件并校验，预览账单 / 分类数量、日期范围、无效数据；确认前可备份当前数据
  - 确认后按已预览的数据快照恢复，分类与账单为**单事务**替换；兼容 v1 旧备份，设置恢复情况单独提示
  - 另开启系统级自动备份规则（API 31+ `dataExtractionRules` / API ≤30 `fullBackupContent`：数据库整域 + 设置 + 背景图）
- **设置**：外观 / 分类 / 数据 / 存储 / 高级分组；显示币种（CNY/USD/JPY/EUR/GBP 仅符号）、主题、背景（默认 / 4 种渐变 / 相册图片）均通过选项面板调整；缓存清理与清空数据均确认后执行

## 技术栈

| 层 | 选型 |
| --- | --- |
| 语言 / UI | Kotlin 2.0.20 + Jetpack Compose (Material 3) |
| 构建 | AGP 8.5.2 + Gradle 8.7（JDK 17） |
| 数据库 | Room 2.6.1（SQLite，kapt） |
| 设置 | DataStore Preferences |
| 图表 | 自绘 Compose Canvas（折线 / 多折线 / 柱状 / 环形），无第三方图表库 |
| 导出 | Storage Access Framework + FileProvider（无存储权限） |
| DI | 轻量 AppContainer + viewModelFactory（无 Hilt） |
| 最低版本 | minSdk 26（Android 8.0），targetSdk 34 |

### 关键设计

- **金额**：全程以「分」为单位的 `Long` 运算，展示层才格式化，杜绝浮点误差
- **日期**：存 `yyyy-MM-dd` 字符串（精确到天，不受时区影响），冗余 `year`/`month` 列 + 索引，
  年/月聚合走覆盖索引
- **无网络**：Manifest 无 INTERNET 权限；导出/分享由用户主动触发，走系统 SAF / Intent

## 构建

### 方式一：Android Studio（推荐）

1. 用 Android Studio（Hedgehog 或更新）打开本目录
2. 等待 Gradle Sync（首次会从网络下载依赖；国内网络已在 `settings.gradle.kts` 配置阿里云镜像）
3. Run `app` 到模拟器 / 手机（USB 调试）

### 方式二：命令行

需要 JDK 17 与 Android SDK（含 `platforms;android-34`、`build-tools;34.0.0`）：

```bat
set ANDROID_HOME=C:\Users\<you>\AppData\Local\Android\Sdk
gradlew.bat :app:assembleDebug
```

产物：`app\build\outputs\apk\debug\app-debug.apk`

打包当前体验版：

```bat
gradlew.bat :app:packageXiaozhuheng
```

产物：`app\build\outputs\distribution\xiaozhuheng-v1.1.3.apk`。`preview` 使用非调试运行模式，沿用本机 debug 签名；其他电脑生成的 debug 签名不同，不能直接覆盖原安装包。签名文件不提交到仓库。

验证代码：

```bat
gradlew.bat :app:testDebugUnitTest :app:lintDebug
```

可选原生截图验收：`gradlew.bat :app:recordPaparazziDebug -PvisualQa --tests "*Ledger*SnapshotTest*"`。
只在带 `-PvisualQa` 时加载 Paparazzi；不增加 APK 的运行依赖。已接受的截图位于 `app/src/test/snapshots/images`，图文对照见 [design-qa.md](design-qa.md)。

> `local.properties`（SDK 路径）已加入 `.gitignore`，由 Studio 自动创建或手工写入：
> `sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`

## 目录结构

```
app/src/main/java/com/example/bookkeeping/
├── BookkeepingApp.kt            # Application + 依赖容器
├── AppContainer.kt              # Room/DataStore/Repository 装配
├── AppViewModelProvider.kt      # viewModelFactory（无 Hilt）
├── MainActivity.kt
├── data/
│   ├── db/                      # 实体 / DAO / AppDatabase / 统计 POJO
│   ├── export/                  # CSV / JSON 导出器
│   ├── prefs/                   # DataStore 设置
│   ├── repo/                    # 业务规则层（分类/账单）
│   └── seed/                    # 内置分类
├── ui/
│   ├── chart/                   # 自绘图表组件
│   ├── common/                  # 通用 UI 组件
│   ├── home/                    # 明细页
│   ├── edit/                    # 记一笔（金额键盘状态机）
│   ├── stats/                   # 统计页（总览/年/月/类别）
│   ├── category/                # 分类管理
│   ├── settings/                # 设置 + 导出
│   ├── nav/                     # 底部导航
│   └── theme/                   # 主题
└── util/                        # 金额格式化 / 日期 / 图标表
```

## 版本规划

- V1.0（本仓库）：见上
- V1.1：预算 / 超支提醒（收入统计与收入分类管理已实现）
- V1.2：桌面小组件 / 通知栏快捷记账、账单回收站
- V2.0：应用锁、多币种、PDF 年度报告

## 已知待办（本轮代码审查发现、暂未实施）

| 事项 | 原因 / 说明 |
| --- | --- |
| 账单表加 `(type, year, month)` 索引 | 需要 `version 2` + Migration；当前无真机可验证迁移，贸然改 schema 有启动校验失败风险 |
| `exportSchema = true` + `schemas/` + `room-testing` 迁移测试 | 需要测试基础设施；建议与上面一条一起做 |
| 扩展单测（`ImportManager` 往返、`DateUtils`、`formatSize`） | 已新增金额输入、收支合计、筛选联动与未保存修改判断的回归测试；其余逻辑待补 |
| 文案国际化（`strings.xml` 目前只有 `app_name`） | 全项目硬编码中文；抽出资源工作量较大 |
| 超大备份流式导出（`JsonWriter`） | 10 万条以上才需要，暂不优化 |
| 周起始日设置暂未生效 | 无周维度统计，属预留项 |

内置背景：设置 → 外观 → 背景 →「静心拨珠」，可直接选择汉服女子算盘插画，无需相册文件，浅深色模式自动调整蒙层。
