# 主题字体

`res/font/song_headings.ttf` 是 Noto Serif SC 的 600 字重静态标题子集。
`res/font/ledger_text.ttf` 是同字体的 400 字重静态正文子集。
来源：https://github.com/google/fonts/tree/main/ofl/notoserifsc 。
按 SIL Open Font License 1.1 分发，许可见 `NotoSerifSC-OFL.txt`。
修改后的字体族名称分别为 `Xiaozhuheng Song Headings` 和 `Xiaozhuheng Ledger Text`。

古风首页的标题、正文和金额使用这两个子集；输入数字键盘继续使用系统数字字体。
标题子集包含当前界面文案；正文子集包含界面文案、ASCII、CJK 基本汉字区 U+4E00–U+9FFF、中文标点和全角字符。动态备注与自定义分类不再仅因不在源码文案中而回落为黑体；扩展区生僻字、其他语言仍由 Android 字体回退处理。
正文子集生成脚本见 `tools/subset-ledger-font.py`。
