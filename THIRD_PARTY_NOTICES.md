# 许可范围与第三方说明

本项目自行编写的源代码、测试、工具及原创文字文档采用根目录 [MIT License](LICENSE)。第三方代码、字体，以及下面单独列出的图像资源不因根目录许可而改变其原有授权。

## Gradle Wrapper

`gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.jar` 来自 Gradle Wrapper，保留其原始版权声明，并遵循 [Apache License 2.0](licenses/Apache-2.0.txt)。

来源：https://github.com/gradle/gradle 。

## Noto Serif SC 字体

包含字体的主题版本使用 `song_headings.ttf`、`ledger_text.ttf`、`ledger_medium.ttf`，由 Noto Serif SC 分别实例化为600、400、500字重并进行子集化；修改后族名为 Xiaozhuheng Song Headings、Xiaozhuheng Ledger Text、Xiaozhuheng Ledger Medium。

字体继续遵循 [SIL Open Font License 1.1](licenses/NotoSerifSC-OFL.txt)，不按MIT重新授权。应用资源中也保留字体许可文件。

来源：https://github.com/google/fonts/tree/main/ofl/notoserifsc 。

## 插画、图标、设计参考及截图

`app/src/main/res/drawable-nodpi/` 中的位图、`docs/` 中的图像和 `app/src/test/snapshots/images/` 中的截图包含用户提供的插画、AI生成图像、设计参考或它们的衍生内容。本次MIT授权范围不包含这些图像，也不授予“小珠衡”名称、标志或品牌的商标权利。

其中汉服女子算盘插画由项目所有者提供；其来源说明为ChatGPT生成。部分主题素材的生成过程记录在设计文档中。这些来源说明不等于对所有图像权利的独立核验。复用或另行分发图像前，请向项目所有者确认具体素材的授权；不要仅依据根目录MIT许可推断其可任意复用。

## 构建依赖

AndroidX、Jetpack Compose、Material Icons、Room、DataStore、Kotlin、Paparazzi等依赖由Gradle获取，不属于本项目原创代码。它们继续适用各自版本携带的许可和版权声明；分发衍生安装包时应保留依赖要求的声明。依赖列表见相应分支的Gradle构建文件。
