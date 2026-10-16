# 墨金藏书 1.1.2 原生页面验收

final result: passed

范围：本轮墨金首页视觉还原、真实生产组件及分类筛选模型回归。截图不是浏览器/网页模拟，也不代表真机触控、输入法、启动性能或完整端到端验收。

## 视觉基准与状态

- 源图：`docs/design/guofeng/ink-gold-user-target.png`，用户本次给出的 1024×1536 px 双栏原稿。上一版 `03-ink-gold-v2.png` 是纵向变体，本轮不再拿它替代原稿。
- 实现：`app/src/test/snapshots/images/com.example.bookkeeping.ui.home_InkGoldReferenceSnapshotTest_referenceRatio.png`，600×900 px/dp，160 dpi，fontScale=1。使用 Android Layoutlib / Paparazzi 1.3.5 渲染生产 InkGoldHomeContent、LedgerBillRow、AppBackground、记账按钮和导航。
- 原图等比例缩至 600×900，左参考/右实现合成 1200×900；无拉伸，没有把原稿当作可交互页面背景。原稿与实现都不包含系统状态栏、手机边框或手势栏；这是原生 dp 视口，不是 CSS 页面。
- 状态同为 2024 年 10 月，支出 2346.80、收入 5800.00、结余 3453.20；10月12日五笔，10月11日三笔。主标题/金额与原稿相同。原稿的二级分类标签不是现有数据字段，因此副行展示实际分类，不虚构新分类数据。
- 手机补充视口 393×850；窄屏 320×720、200% 字号、超长金额；另外覆盖原青玉首页、反向明暗、130% 字号、缩短可用高度与两套生产控件板。
- 照片补充使用用户原始人物图 `ink-gold-photo-fixture.png`，复用生产 AppBackgroundContent 绘制和原 0.55 黑色蒙层。仅测试用例同步解码以保证确定性，应用仍在 IO 线程解码。用户背景选择未改动、未迁移。

## 最终证据

完整并排：`docs/design/guofeng/qa/ink-gold-v112-comparison.png`。

放大核对：`ink-gold-v112-top.png`（1200×320），`ink-gold-v112-list.png`（1200×580），与完整对照使用同一坐标裁切，不修改页面元素。顶部核对字体、金额/单位、金线与双栏；下部核对账目密度、图标、CTA 和导航。

手机背景并排：`docs/design/guofeng/qa/ink-gold-v112-backgrounds.png`（786×850，左主题默认、右保留照片）。

交互状态原生截图：`app/src/test/snapshots/images/com.example.bookkeeping.ui.home_InkGoldReferenceSnapshotTest_` 前缀下的 `categoryAndSearch.png`、`emptySearch.png`、`largeFont.png`。

## 修正记录

1. [P1] 旧实现使用纵向通用首页：品牌、月份、支出、收支顺次堆叠，收支三选栏和悬浮按钮与本次原稿不符。已独立为 InkGoldHomeContent：双栏品牌/月份搜索，左右概览，真实横向分类，备注主标题与分类副标题，底部通栏 CTA。
2. [P1] 字体仅含源码汉字，用户备注回落为系统字体。正文补入基本汉字、中文标点及全角字符；动态备注也保持宋体。标题仍使用 600 字重，正文/金额为 400 字重。
3. [P2] 首次渲染暴露 SubcomposeLayout 与 intrinsic 测量冲突，页面不能绘制。删除 intrinsic 依赖，改由 Box 测量正文后匹配竖线；后续全部原生渲染成功。
4. [P2] 首轮有效截图中概览金线错位、旧算盘缩小不可辨识、默认纹理过强。金线统一顶部基线，使用已有清楚算盘资源，默认布纹叠加降低为 0.42 alpha；照片背景保持原设置。对应初轮截图留在忽略的 `app/build/visual-qa/ink-iteration-1/`。
5. [P2] 金额单位与数值分离，长金额时单位过小。改为同一富文本基线，单位最小 10sp，整体按可用宽度测量缩放。超窄/大字号下头部和概览纵排，内容仍可滚动，CTA/导航独立可见。
6. [P2] 同比例视口末条记录未完整出现在首屏。进一步减少宽视口概览/日期留白，账单保持至少48dp；最终对照中八条记录完整显示在 CTA 之上。
7. 提高墨金辅助文字亮度，保持原照片背景蒙层时也更清楚。最终照片对照已检查，未把照片背景视为错误或强制重置。

## 五项视觉检查

- 字体：嵌入 OFL Noto Serif SC 400/600；标题、备注、金额层级明确；主金额单位共用基线；200% 字体完整显示长金额，必要时副行金额允许换行。
- 排版：原稿双栏、横向分类、连续细线账目、通栏 CTA、横向选中导航已落实。手机分类可横滑，窄屏大字体纵向布局，整体账页滚动。
- 配色：深墨绿、暖白文字、淡金交互；主题默认布面纹理与用户照片均保留。浅色变体继续使用纸白/古铜；配色单测覆盖文字对比度。
- 素材：实际本地算盘和生成布面，无 UI 截图贴图；标准图标使用既有 Material 图标库。
- 内容：源图金额和八条示例账目同态；生产版本消费真实分类、账目与月份状态，月概览不受搜索/分类筛选改变。筛选、重置与“全部”接原有 ViewModel/Repository。

## P3 与验证边界

- 原稿生成字形和细线图标并无可直接提取的字体/图标源文件，当前用合法 Noto 字体及最接近的 Material 图标，算盘更重以适应小尺寸。
- 手机尺寸较原稿更窄，保留最小触控尺寸、字号和横向分类滚动，首屏记录数随系统字号和状态栏可用高度改变；不把原稿当作固定像素大图缩放。
- 日期使用应用既有「周六」文字，副标题仅含现有分类；没有添加原稿中的虚构二级标签。
- `adb devices -l` 无设备，真机输入法、滑动删除手势、返回性能、厂商系统栏及覆盖安装仍需设备验证。截图只验证生产组件的原生视觉与指定状态，未声称自动点击这些流程。

## 复现与检查

- 记录：`gradlew.bat :app:recordPaparazziDebug -PvisualQa --tests '*InkGoldReferenceSnapshotTest*' --tests '*Ledger*SnapshotTest*' --offline --max-workers=2`。
- 回归：同筛选执行 `verifyPaparazziDebug`，15 项通过，日志 `.tools/ink-gold-verify-delivery.log`。
- 对照：`python tools/compare-ink-gold.py`。
- 单测、lint、preview 打包：`gradlew.bat :app:packageXiaozhuheng :app:testDebugUnitTest :app:lintDebug --offline --max-workers=2`，结果详见 `.tools/ink-gold-build-delivery.log`。

## 最终构建结果

- 49 项业务/配色单测通过，0失败/错误/跳过；含横向分类“全部”清除收支类型且保留关键词、跨收支分类切换用例。
- 15 项原生截图基准回归通过；最终记录日志 `.tools/ink-gold-record-delivery.log`，回归日志 `.tools/ink-gold-verify-delivery.log`。
- Android lint：0错误、7项既有警告、1项提示；preview 构建成功且不可调试。
- APK：`app/build/outputs/distribution/xiaozhuheng-v1.1.2.apk`，24,801,087 bytes，versionName=1.1.2 / versionCode=7。
- SHA-256：`c1c3afda240004d017218e0d46c0bc9bd704f9c2db85ed0ddf069efc01acb701`。
- apksigner 验证通过；证书 SHA-256：`113b1e3be65ad53f6d05a6e0b199c9b0574f5481aed41ee750331b5b3263c145`，与原安装包一致，包名仍为 `com.example.bookkeeping`，应用名称「小珠衡」。
