package de.robojumper.ddsavereader.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 增强型术语验证器
 * 基于已提取的3257个游戏术语进行数据验证和智能提示
 */
public class EnhancedTermValidator {
    
    private static EnhancedTermValidator instance;
    private final Map<String, Set<String>> gameTerms = new HashMap<>();
    private final Map<String, List<String>> termSuggestions = new HashMap<>();
    private boolean initialized = false;
    
    // 术语类别常量
    public static final String HEROES = "heroes";
    public static final String SKILLS = "skills";
    public static final String QUIRKS = "quirks";
    public static final String ITEMS = "items";
    public static final String TRINKETS = "trinkets";
    public static final String MONSTERS = "monsters";
    public static final String DUNGEONS = "dungeons";
    public static final String BUILDINGS = "buildings";
    public static final String ACTIVITIES = "activities";
    public static final String CURIOS = "curios";
    public static final String EVENTS = "events";
    public static final String QUESTS = "quests";
    public static final String EFFECTS = "effects";
    public static final String OTHERS = "others";
    
    public static EnhancedTermValidator getInstance() {
        if (instance == null) {
            instance = new EnhancedTermValidator();
        }
        return instance;
    }
    
    private EnhancedTermValidator() {
        initializeTerms();
    }
    
    /**
     * 从term_analysis目录加载已提取的术语数据
     */
    private void initializeTerms() {
        String[] categories = {
            HEROES, SKILLS, QUIRKS, ITEMS, TRINKETS, MONSTERS,
            DUNGEONS, BUILDINGS, ACTIVITIES, CURIOS, EVENTS, QUESTS, OTHERS
        };
        
        Path termAnalysisDir = Paths.get("term_analysis");
        
        if (!Files.exists(termAnalysisDir)) {
            System.err.println("警告: 未找到术语分析目录，使用备用数据");
            initializeFallbackTerms();
            return;
        }
        
        for (String category : categories) {
            Set<String> terms = new HashSet<>();
            Path termFile = termAnalysisDir.resolve(category + "_terms.txt");
            
            if (Files.exists(termFile)) {
                try (BufferedReader reader = Files.newBufferedReader(termFile)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        // 跳过注释行和空行
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            terms.add(line.toLowerCase());
                        }
                    }
                    System.out.println("加载 " + category + ": " + terms.size() + " 个术语");
                } catch (IOException e) {
                    System.err.println("加载术语文件失败: " + termFile + " - " + e.getMessage());
                }
            }
            
            gameTerms.put(category, terms);
            // 预生成建议列表（按字母顺序排序）
            termSuggestions.put(category, new ArrayList<>(terms));
            termSuggestions.get(category).sort(String::compareToIgnoreCase);
        }
        
        initialized = !gameTerms.isEmpty();
        if (initialized) {
            int totalTerms = gameTerms.values().stream().mapToInt(Set::size).sum();
            System.out.println("术语验证器初始化完成，共加载 " + totalTerms + " 个术语");
        }
    }
    
    /**
     * 备用术语数据（如果无法加载文件时使用）
     */
    private void initializeFallbackTerms() {
        // 使用核心术语作为备用
        gameTerms.put(HEROES, new HashSet<>(Arrays.asList(
            "crusader", "highwayman", "plague_doctor", "vestal", "hellion",
            "bounty_hunter", "occultist", "grave_robber", "man_at_arms", "jester",
            "leper", "abomination", "arbalest", "hound_master", "antiquarian"
        )));
        
        gameTerms.put(QUIRKS, new HashSet<>(Arrays.asList(
            "accuracy", "quick_reflexes", "tough", "hard_skinned", "steady",
            "focused", "natural", "robust", "god_fearing", "beast_hater"
        )));
        
        // 为每个类别生成建议列表
        for (Map.Entry<String, Set<String>> entry : gameTerms.entrySet()) {
            termSuggestions.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        
        initialized = true;
        System.out.println("使用备用术语数据初始化完成");
    }
    
    /**
     * 验证术语是否有效
     */
    public boolean isValidTerm(String category, String term) {
        if (!initialized || term == null || term.trim().isEmpty()) {
            return false;
        }
        
        Set<String> terms = gameTerms.get(category);
        return terms != null && terms.contains(term.toLowerCase().trim());
    }
    
    /**
     * 获取术语建议列表
     */
    public List<String> getTermSuggestions(String category) {
        if (!initialized) return new ArrayList<>();
        return new ArrayList<>(termSuggestions.getOrDefault(category, new ArrayList<>()));
    }
    
    /**
     * 根据前缀过滤术语建议
     */
    public List<String> getFilteredSuggestions(String category, String prefix) {
        if (!initialized || prefix == null) {
            return getTermSuggestions(category);
        }
        
        String lowerPrefix = prefix.toLowerCase().trim();
        return termSuggestions.getOrDefault(category, new ArrayList<>()).stream()
            .filter(term -> term.toLowerCase().startsWith(lowerPrefix))
            .limit(20) // 限制建议数量
            .collect(Collectors.toList());
    }
    
    /**
     * 模糊搜索术语
     */
    public List<String> searchTerms(String category, String query) {
        if (!initialized || query == null || query.trim().isEmpty()) {
            return getTermSuggestions(category);
        }
        
        String lowerQuery = query.toLowerCase().trim();
        return termSuggestions.getOrDefault(category, new ArrayList<>()).stream()
            .filter(term -> term.toLowerCase().contains(lowerQuery))
            .limit(20)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取随机术语
     */
    public String getRandomTerm(String category) {
        List<String> suggestions = getTermSuggestions(category);
        if (suggestions.isEmpty()) return "";
        
        Random random = new Random();
        return suggestions.get(random.nextInt(suggestions.size()));
    }
    
    /**
     * 验证英雄职业
     */
    public boolean isValidHeroClass(String heroClass) {
        return isValidTerm(HEROES, heroClass);
    }
    
    /**
     * 验证技能名称
     */
    public boolean isValidSkill(String skill) {
        return isValidTerm(SKILLS, skill);
    }
    
    /**
     * 验证怪癖名称
     */
    public boolean isValidQuirk(String quirk) {
        return isValidTerm(QUIRKS, quirk);
    }
    
    /**
     * 验证饰品名称
     */
    public boolean isValidTrinket(String trinket) {
        return isValidTerm(TRINKETS, trinket);
    }
    
    /**
     * 验证物品名称
     */
    public boolean isValidItem(String item) {
        return isValidTerm(ITEMS, item);
    }
    
    /**
     * 获取所有英雄职业
     */
    public List<String> getAllHeroClasses() {
        return getTermSuggestions(HEROES);
    }
    
    /**
     * 获取所有怪癖
     */
    public List<String> getAllQuirks() {
        return getTermSuggestions(QUIRKS);
    }
    
    /**
     * 获取所有技能
     */
    public List<String> getAllSkills() {
        return getTermSuggestions(SKILLS);
    }
    
    /**
     * 获取所有饰品
     */
    public List<String> getAllTrinkets() {
        return getTermSuggestions(TRINKETS);
    }
    
    /**
     * 获取所有物品
     */
    public List<String> getAllItems() {
        return getTermSuggestions(ITEMS);
    }
    
    /**
     * 获取术语统计信息
     */
    public Map<String, Integer> getTermStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : gameTerms.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().size());
        }
        return stats;
    }
    
    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 获取分类显示名称
     */
    public static String getCategoryDisplayName(String category) {
        switch (category) {
            case HEROES: return "英雄";
            case SKILLS: return "技能";
            case QUIRKS: return "怪癖";
            case ITEMS: return "物品";
            case TRINKETS: return "饰品";
            case MONSTERS: return "怪物";
            case DUNGEONS: return "地牢";
            case BUILDINGS: return "建筑";
            case ACTIVITIES: return "活动";
            case CURIOS: return "古玩";
            case EVENTS: return "事件";
            case QUESTS: return "任务";
            case EFFECTS: return "效果";
            case OTHERS: return "其他";
            default: return category;
        }
    }
}