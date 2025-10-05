# 游戏术语提取器 (GameTermExtractor) 使用指南

## 概述

GameTermExtractor 是一个强大的游戏数据分析工具，用于从《最黑暗的地牢》游戏文件中提取和分类各种游戏术语。它解决了原来名字生成功能导致应用卡死的问题，并提供了更丰富的游戏数据分析功能。

## 主要功能

### 1. 分类术语提取
- **英雄 (Heroes)**: 英雄职业名称
- **技能 (Skills)**: 技能和能力名称
- **怪癖 (Quirks)**: 英雄特质和怪癖
- **物品 (Items)**: 游戏中的各种物品
- **饰品 (Trinkets)**: 装备饰品
- **怪物 (Monsters)**: 敌人和怪物
- **地牢 (Dungeons)**: 地牢类型
- **建筑 (Buildings)**: 城镇建筑
- **活动 (Activities)**: 建筑活动
- **古玩 (Curios)**: 地牢中的古玩
- **事件 (Events)**: 游戏事件
- **任务 (Quests)**: 任务类型
- **效果 (Effects)**: 状态效果
- **其他 (Others)**: 其他游戏元素

### 2. 异步处理
- 使用 CompletableFuture 避免界面卡死
- 支持后台处理大量游戏文件
- 提供处理进度反馈

### 3. 数据持久化
- 自动保存提取的术语到分类文件
- 生成汇总统计文件
- 支持增量更新

### 4. 搜索和查询
- 按类别查询术语
- 关键词搜索功能
- 统计信息获取

## 使用方法

### 基本使用

```java
// 获取提取器实例
GameTermExtractor extractor = GameTermExtractor.getInstance();

// 异步提取游戏术语
CompletableFuture<GameTermExtractor.ExtractionResult> future = 
    extractor.extractTermsAsync(gameDir, modsDir);

// 处理结果
future.thenAccept(result -> {
    if (result.success) {
        System.out.println("提取成功: " + result.message);
        Map<String, Set<String>> allTerms = result.extractedTerms;
        // 处理提取的术语...
    } else {
        System.err.println("提取失败: " + result.message);
    }
});
```

### 与简易修改器集成

```java
// 获取特定类别的术语
Set<String> heroNames = extractor.getTerms(GameTermExtractor.HEROES);
Set<String> skillNames = extractor.getTerms(GameTermExtractor.SKILLS);

// 搜索相关术语
Map<String, Set<String>> healingSkills = extractor.searchTerms("heal");

// 获取统计信息
Map<String, Set<String>> allTerms = extractor.getAllTerms();
```

### 使用 NameManager 简化接口

```java
NameManager nameManager = NameManager.getInstance();

// 生成游戏数据
CompletableFuture<GameTermExtractor.ExtractionResult> future = 
    nameManager.generateGameDataAsync(gameDir, modsDir);

// 获取随机术语
String randomHero = nameManager.getRandomHeroName();
String randomSkill = nameManager.getRandomSkillName();
String randomItem = nameManager.getRandomItemName();

// 获取数据统计
Map<String, Integer> stats = nameManager.getDataStatistics();
```

## 输出文件

提取器会在数据目录中生成以下文件：

- `heroes_terms.txt` - 英雄术语
- `skills_terms.txt` - 技能术语
- `quirks_terms.txt` - 怪癖术语
- `items_terms.txt` - 物品术语
- `trinkets_terms.txt` - 饰品术语
- `monsters_terms.txt` - 怪物术语
- `dungeons_terms.txt` - 地牢术语
- `buildings_terms.txt` - 建筑术语
- `activities_terms.txt` - 活动术语
- `curios_terms.txt` - 古玩术语
- `events_terms.txt` - 事件术语
- `quests_terms.txt` - 任务术语
- `effects_terms.txt` - 效果术语
- `others_terms.txt` - 其他术语
- `game_data_summary.json` - 数据汇总

## 支持的文件类型

提取器能够解析以下游戏文件类型：

- `.info.darkest` - 英雄/怪物信息文件
- `.upgrades.json` - 技能升级文件
- `.camping_skills.json` - 野营技能文件
- `.quirk_library.json` - 怪癖库文件
- `.inventory.*.darkest` - 物品文件
- `.trinkets.json` - 饰品文件
- `.building.json` - 建筑文件
- `.dungeon.json` - 地牢文件
- `.events.json` - 事件文件
- `curio_props.csv` - 古玩属性文件
- `obstacle_definitions.json` - 障碍物定义文件
- `.types.json` - 任务类型文件
- `tutorial_popup.*.png` - 教程文件

## 性能特点

- **异步处理**: 避免界面卡死
- **内存优化**: 使用流式处理大文件
- **错误恢复**: 单个文件解析失败不影响整体进程
- **增量更新**: 支持增量添加新的游戏内容

## 故障排除

### 常见问题

1. **提取失败**
   - 检查游戏目录路径是否正确
   - 确认目录中包含 `svn_revision.txt` 文件
   - 检查 mod 目录是否以 `262060` 结尾

2. **术语数量较少**
   - 确认游戏安装完整
   - 检查是否包含了 DLC 内容
   - 验证 mod 目录是否正确

3. **处理时间过长**
   - 大型 mod 集合可能需要较长时间
   - 可以分批处理不同目录
   - 使用异步处理避免界面卡死

## 扩展和定制

### 添加新的文件类型支持

```java
// 在 GameTermExtractor 中添加新的处理器
private void processCustomFile(Path filePath) throws Exception {
    // 自定义文件解析逻辑
}
```

### 自定义术语分类

```java
// 添加新的术语类别
public static final String CUSTOM_CATEGORY = "custom";

// 在 initializeCategories() 中添加
gameTerms.put(CUSTOM_CATEGORY, new HashSet<>());
```

## 应用场景

1. **简易修改器数据源**: 为修改器提供准确的游戏术语
2. **游戏内容分析**: 分析游戏和 mod 的内容构成
3. **本地化支持**: 提取术语用于翻译和本地化
4. **开发工具**: 帮助 mod 开发者了解游戏结构
5. **数据挖掘**: 研究游戏平衡性和内容分布

## 注意事项

- 提取的术语基于游戏文件结构，可能包含技术性名称
- 某些术语可能是内部标识符，不一定是用户可见的名称
- mod 内容可能会覆盖或扩展基础游戏内容
- 建议定期更新术语数据以包含最新的游戏内容