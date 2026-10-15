# 主题字体

`res/font/song_headings.ttf` 是 Noto Serif SC 的 600 字重静态标题子集。
`res/font/ledger_text.ttf` 是同字体的 400 字重静态正文子集。
来源：https://github.com/google/fonts/tree/main/ofl/notoserifsc 。
按 SIL Open Font License 1.1 分发，许可见 `NotoSerifSC-OFL.txt`。
修改后的字体族名称分别为 `Xiaozhuheng Song Headings` 和 `Xiaozhuheng Ledger Text`。

古风首页的标题、正文和金额使用这两个子集；输入数字键盘继续使用系统数字字体。
子集包含当前 Kotlin 页面文案中的字符和 ASCII，其他字符由 Android 字体回退处理。
正文子集生成脚本见 `tools/subset-ledger-font.py`。
