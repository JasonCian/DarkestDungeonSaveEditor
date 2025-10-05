package de.robojumper.ddsavereader.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 游戏术语提取器使用示例
 */
public class GameTermExtractorExample {
    
    public static void main(String[] args) {
        // 获取提取器实例
        GameTermExtractor extractor = GameTermExtractor.getInstance();
        NameManager nameManager = NameManager.getInstance();
        
        // 示例：异步提取游戏术语
        String gameDir = "C:\\Steam\\steamapps\\common\\DarkestDungeon";
        String modsDir = "C:\\Steam\\steamapps\\workshop\\content\\262060";
        
        System.out.println("开始提取游戏术语...");
        
        CompletableFuture<GameTermExtractor.ExtractionResult> future = 
            extractor.extractTermsAsync(gameDir, modsDir);
        
        // 等待提取完成
        future.thenAccept(result -> {
            if (result.success) {
                System.out.println("✓ " + result.message);
                
                // 显示提取的术语统计
                System.out.println("\n=== 游戏术语统计 ===");
                Map<String, Set<String>> allTerms = result.extractedTerms;
                
                for (Map.Entry<String, Set<String>> entry : allTerms.entrySet()) {
                    String category = entry.getKey();
                    int count = entry.getValue().size();
                    if (count > 0) {
                        System.out.printf("%-12s: %d 个术语\n", 
                            getCategoryDisplayName(category), count);
                    }
                }
                
                // 显示一些示例术语
                showExampleTerms(allTerms);
                
                // 演示搜索功能
                demonstrateSearch(extractor);
                
                // 演示名字管理器的新功能
                demonstrateNameManager(nameManager);
                
            } else {
                System.err.println("✗ 提取失败: " + result.message);
            }
        }).exceptionally(throwable -> {
            System.err.println("✗ 提取过程中出现异常: " + throwable.getMessage());
            return null;
        });
        
        // 保持程序运行直到异步操作完成
        try {
            Thread.sleep(10000); // 等待10秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        extractor.shutdown();
    }
    
    private static String getCategoryDisplayName(String category) {
        switch (category) {
            case GameTermExtractor.HEROES: return "英雄";
            case GameTermExtractor.SKILLS: return "技能";
            case GameTermExtractor.QUIRKS: return "怪癖";
            case GameTermExtractor.ITEMS: return "物品";
            case GameTermExtractor.TRINKETS: return "饰品";
            case GameTermExtractor.MONSTERS: return "怪物";
            case GameTermExtractor.DUNGEONS: return "地牢";
            case GameTermExtractor.BUILDINGS: return "建筑";
            case GameTermExtractor.ACTIVITIES: return "活动";
            case GameTermExtractor.CURIOS: return "古玩";
            case GameTermExtractor.EVENTS: return "事件";
            case GameTermExtractor.QUESTS: return "任务";
            case GameTermExtractor.EFFECTS: return "效果";
            case GameTermExtractor.OTHERS: return "其他";
            default: return category;
        }
    }
    
    private static void showExampleTerms(Map<String, Set<String>> allTerms) {
        System.out.println("\n=== 术语示例 ===");
        
        // 显示每个类别的前5个术语
        for (Map.Entry<String, Set<String>> entry : allTerms.entrySet()) {
            Set<String> terms = entry.getValue();
            if (!terms.isEmpty()) {
                System.out.print(getCategoryDisplayName(entry.getKey()) + ": ");
                terms.stream().limit(5).forEach(term -> System.out.print(term + " "));
                if (terms.size() > 5) {
                    System.out.print("...");
                }
                System.out.println();
            }
        }
    }
    
    private static void demonstrateSearch(GameTermExtractor extractor) {
        System.out.println("\n=== 搜索功能演示 ===");
        
        // 搜索包含特定关键词的术语
        String[] searchKeywords = {"heal", "stun", "plague", "guard"};
        
        for (String keyword : searchKeywords) {
            Map<String, Set<String>> searchResults = extractor.searchTerms(keyword);
            if (!searchResults.isEmpty()) {
                System.out.println("搜索 '" + keyword + "':");
                for (Map.Entry<String, Set<String>> entry : searchResults.entrySet()) {
                    System.out.printf("  %s: %s\n", 
                        getCategoryDisplayName(entry.getKey()), 
                        entry.getValue().toString());
                }
            }
        }
    }
    
    private static void demonstrateNameManager(NameManager nameManager) {
        System.out.println("\n=== 名字管理器功能演示 ===");
        
        if (nameManager.isInitialized()) {
            // 生成随机术语
            System.out.println("随机英雄名: " + nameManager.getRandomHeroName());
            System.out.println("随机技能名: " + nameManager.getRandomSkillName());
            System.out.println("随机物品名: " + nameManager.getRandomItemName());
            
            // 显示数据统计
            System.out.println("\n数据统计:");
            Map<String, Integer> stats = nameManager.getDataStatistics();
            for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                if (entry.getValue() > 0) {
                    System.out.printf("  %s: %d\n", 
                        getCategoryDisplayName(entry.getKey()), entry.getValue());
                }
            }
        } else {
            System.out.println("名字管理器尚未初始化");
        }
    }
}