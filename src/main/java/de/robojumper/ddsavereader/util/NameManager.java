package de.robojumper.ddsavereader.util;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 名字管理器 - 简化版本，主要用于与 GameTermExtractor 协作
 * 提供快速访问游戏术语的接口
 */
public class NameManager {
    
    private static NameManager instance;
    private GameTermExtractor termExtractor;
    
    // 默认英雄名字，用作后备
    private static final List<String> DEFAULT_HERO_NAMES = Arrays.asList(
        "雷纳德", "迪斯马斯", "维纳", "塔斯卡", "奥德丽", "埃尔芙", 
        "巴拉克", "弗格斯", "莫里根", "塞拉芬", "加斯顿", "伊莎贝拉",
        "马库斯", "露娜", "凯撒", "亚瑟", "兰斯洛特", "高文"
    );
    
    public static NameManager getInstance() {
        if (instance == null) {
            instance = new NameManager();
        }
        return instance;
    }
    
    private NameManager() {
        termExtractor = GameTermExtractor.getInstance();
    }
    
    /**
     * 异步生成游戏术语
     */
    public CompletableFuture<GameTermExtractor.ExtractionResult> generateGameDataAsync(String gameDir, String modsDir) {
        return termExtractor.extractTermsAsync(gameDir, modsDir);
    }
    
    /**
     * 获取随机英雄名字
     */
    public String getRandomHeroName() {
        Set<String> heroNames = termExtractor.getTerms(GameTermExtractor.HEROES);
        
        if (heroNames.isEmpty()) {
            // 使用默认名字
            Random random = new Random();
            return DEFAULT_HERO_NAMES.get(random.nextInt(DEFAULT_HERO_NAMES.size()));
        }
        
        List<String> nameList = new ArrayList<>(heroNames);
        Random random = new Random();
        return nameList.get(random.nextInt(nameList.size()));
    }
    
    /**
     * 获取随机技能名
     */
    public String getRandomSkillName() {
        Set<String> skillNames = termExtractor.getTerms(GameTermExtractor.SKILLS);
        
        if (skillNames.isEmpty()) {
            return "skill_" + new Random().nextInt(1000);
        }
        
        List<String> nameList = new ArrayList<>(skillNames);
        Random random = new Random();
        return nameList.get(random.nextInt(nameList.size()));
    }
    
    /**
     * 获取随机物品名
     */
    public String getRandomItemName() {
        Set<String> itemNames = termExtractor.getTerms(GameTermExtractor.ITEMS);
        
        if (itemNames.isEmpty()) {
            return "item_" + new Random().nextInt(1000);
        }
        
        List<String> nameList = new ArrayList<>(itemNames);
        Random random = new Random();
        return nameList.get(random.nextInt(nameList.size()));
    }
    
    /**
     * 获取指定类别的所有术语
     */
    public Set<String> getTermsByCategory(String category) {
        return termExtractor.getTerms(category);
    }
    
    /**
     * 搜索术语
     */
    public Map<String, Set<String>> searchTerms(String keyword) {
        return termExtractor.searchTerms(keyword);
    }
    
    /**
     * 获取所有可用的类别
     */
    public Set<String> getAvailableCategories() {
        return termExtractor.getCategories();
    }
    
    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return termExtractor.isInitialized();
    }
    
    /**
     * 获取游戏数据统计
     */
    public Map<String, Integer> getDataStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        Map<String, Set<String>> allTerms = termExtractor.getAllTerms();
        
        for (Map.Entry<String, Set<String>> entry : allTerms.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().size());
        }
        
        return stats;
    }
    
    /**
     * 关闭资源
     */
    public void shutdown() {
        if (termExtractor != null) {
            termExtractor.shutdown();
        }
    }
}