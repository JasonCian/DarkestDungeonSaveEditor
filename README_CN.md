# 黑暗地牢存档编辑器 - 中文翻译

这个项目为 [Darkest Dungeon Save Editor](https://github.com/robojumper/DarkestDungeonSaveEditor) 添加了中文语言支持。

## 功能特性

- 🌏 **多语言支持**: 英文和简体中文界面
- 🔄 **智能语言检测**: 自动根据系统语言选择界面语言
- ⚙️ **语言切换**: 通过菜单随时切换语言
- 💾 **偏好保存**: 记住用户的语言选择

## 使用说明

### 第一次使用

1. 启动应用程序时，会自动检测您的系统语言
   - 如果系统语言是中文，界面将显示为中文
   - 否则将显示为英文

### 切换语言

1. 在主窗口菜单栏中选择 **帮助** → **Language / 语言**
2. 在弹出的对话框中选择您想要的语言
3. 点击 **OK / 确定** 保存设置
4. 重启应用程序以应用新的语言设置

### 支持的语言

- **English** - 英文 (默认)
- **中文 (简体)** - 简体中文

## 实现细节

### 文件结构

```
src/main/java/de/robojumper/ddsavereader/
├── i18n/
│   └── Messages.java                     # 国际化消息管理类
└── ui/
    └── LanguageSelectionDialog.java      # 语言选择对话框

src/main/resources/de/robojumper/ddsavereader/i18n/
├── messages.properties                   # 默认消息 (英文)
├── messages_en.properties               # 英文消息
└── messages_zh_CN.properties            # 中文消息
```

### 主要组件

1. **Messages.java**: 国际化消息管理类
   - 自动检测系统语言
   - 提供消息获取和格式化功能
   - 支持运行时语言切换

2. **LanguageSelectionDialog.java**: 语言选择对话框
   - 用户友好的语言选择界面
   - 保存用户偏好设置
   - 初始化语言设置

3. **资源文件**: 包含所有界面文本的翻译
   - 菜单项翻译
   - 对话框文本翻译
   - 按钮和标签翻译
   - 错误和状态消息翻译

### 翻译覆盖范围

- ✅ 主菜单 (文件、工具、帮助)
- ✅ 主窗口按钮和标签
- ✅ 对话框和消息框
- ✅ 数据路径配置对话框
- ✅ 错误和状态消息
- ✅ 确认对话框

## 为开发者

### 添加新的翻译文本

1. 在资源文件中添加新的键值对
2. 在代码中使用 `Messages.getString("key")` 获取文本
3. 确保在所有语言文件中都添加相应的翻译

### 添加新语言

1. 创建新的资源文件，如 `messages_fr.properties` (法语)
2. 在 `LanguageSelectionDialog.java` 中添加新的语言选项
3. 在 `Messages.java` 中添加语言处理逻辑

### 测试

1. 构建项目: `./gradlew build`
2. 运行应用程序: `java -jar build/libs/DDSaveEditor.jar`
3. 测试语言切换功能
4. 验证所有界面元素的翻译

## 技术说明

- 使用 Java 标准的 `ResourceBundle` 进行国际化
- 使用 `Preferences` API 保存用户语言偏好
- 支持运行时语言切换（重启后生效）
- 自动检测系统语言环境

## 贡献

欢迎提交翻译改进或添加新语言的 Pull Request！

### 翻译指南

1. 保持简洁明了的翻译
2. 确保专业术语的一致性
3. 考虑界面布局限制
4. 测试翻译在实际界面中的效果

## 许可证

本项目遵循原项目的 MIT 许可证。