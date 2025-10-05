package de.robojumper.ddsavereader.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 游戏术语提取器 - 从游戏文件中提取各种关键术语和数据
 * 支持异步处理，避免界面卡死，并将数据分类整理供简易修改器使用
 */
public class GameTermExtractor {
    
    private static GameTermExtractor instance;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private boolean initialized = false;
    
    // 分类存储的游戏术语
    private final Map<String, Set<String>> gameTerms = new HashMap<>();
    
    // 数据类别常量
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
    
    // 文件模式匹配器
    private static final Pattern ITEM_PATTERN = Pattern.compile("inventory_item:\\s?\\.type\\s?\"([a-z_]*)\"\\s*\\.id\\s*\"([a-z_]*)\".*");
    private static final Pattern VALID_ID_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");
    
    public static GameTermExtractor getInstance() {
        if (instance == null) {
            instance = new GameTermExtractor();
        }
        return instance;
    }
    
    private GameTermExtractor() {
        initializeCategories();
    }
    
    private void initializeCategories() {
        gameTerms.put(HEROES, new HashSet<>());
        gameTerms.put(SKILLS, new HashSet<>());
        gameTerms.put(QUIRKS, new HashSet<>());
        gameTerms.put(ITEMS, new HashSet<>());
        gameTerms.put(TRINKETS, new HashSet<>());
        gameTerms.put(MONSTERS, new HashSet<>());
        gameTerms.put(DUNGEONS, new HashSet<>());
        gameTerms.put(BUILDINGS, new HashSet<>());
        gameTerms.put(ACTIVITIES, new HashSet<>());
        gameTerms.put(CURIOS, new HashSet<>());
        gameTerms.put(EVENTS, new HashSet<>());
        gameTerms.put(QUESTS, new HashSet<>());
        gameTerms.put(EFFECTS, new HashSet<>());
        gameTerms.put(OTHERS, new HashSet<>());
    }
    
    /**
     * 异步提取游戏术语
     */
    public CompletableFuture<ExtractionResult> extractTermsAsync(String gameDir, String modsDir) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 清除旧数据
                for (Set<String> terms : gameTerms.values()) {
                    terms.clear();
                }
                initializeCategories();
                
                // 收集路径
                List<String> paths = new ArrayList<>();
                if (gameDir != null && !gameDir.trim().isEmpty()) {
                    paths.add(gameDir.trim());
                }
                if (modsDir != null && !modsDir.trim().isEmpty()) {
                    paths.add(modsDir.trim());
                }
                
                if (paths.isEmpty()) {
                    return new ExtractionResult(false, "没有指定有效的游戏目录", new HashMap<>());
                }
                
                // 使用增强的提取逻辑
                extractGameTerms(paths);
                
                // 保存结果到文件
                saveTermsToFiles();
                
                initialized = true;
                
                int totalTerms = gameTerms.values().stream().mapToInt(Set::size).sum();
                return new ExtractionResult(true, "成功提取 " + totalTerms + " 个游戏术语", new HashMap<>(gameTerms));
                
            } catch (Exception e) {
                e.printStackTrace();
                return new ExtractionResult(false, "提取术语时出错: " + e.getMessage(), new HashMap<>());
            }
        }, executor);
    }
    
    /**
     * 提取游戏术语的核心逻辑
     */
    private void extractGameTerms(List<String> paths) {
        for (String pathStr : paths) {
            Path rootPath = Paths.get(pathStr);
            if (Files.exists(rootPath) && Files.isDirectory(rootPath)) {
                try {
                    Files.walk(rootPath)
                        .filter(Files::isRegularFile)
                        .forEach(this::processGameFile);
                } catch (Exception e) {
                    System.err.println("处理目录时出错: " + pathStr + " - " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 处理单个游戏文件
     */
    private void processGameFile(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString().toLowerCase();
            String fullPath = filePath.toString().toLowerCase();
            
            // 根据文件类型和路径确定处理方式
            if (fileName.endsWith(".info.darkest")) {
                processInfoFile(filePath);
            } else if (fileName.endsWith(".upgrades.json")) {
                processUpgradesFile(filePath);
            } else if (fileName.endsWith(".camping_skills.json")) {
                processSkillsFile(filePath);
            } else if (fileName.endsWith(".quirk_library.json")) {
                processQuirksFile(filePath);
            } else if (fileName.endsWith(".inventory.items.darkest") || fileName.endsWith(".inventory.system_configs.darkest")) {
                processInventoryFile(filePath);
            } else if (fileName.endsWith(".trinkets.json")) {
                processTrinketsFile(filePath);
            } else if (fileName.endsWith(".building.json")) {
                processBuildingFile(filePath);
            } else if (fileName.endsWith(".dungeon.json")) {
                processDungeonFile(filePath);
            } else if (fileName.endsWith(".events.json")) {
                processEventsFile(filePath);
            } else if (fileName.equals("curio_props.csv")) {
                processCuriosFile(filePath);
            } else if (fileName.equals("obstacle_definitions.json")) {
                processObstaclesFile(filePath);
            } else if (fileName.endsWith(".types.json")) {
                processQuestTypesFile(filePath);
            } else if (fullPath.contains("tutorial_popup") && fileName.endsWith(".png")) {
                processTutorialFile(filePath);
            }
            
        } catch (Exception e) {
            System.err.println("处理文件时出错: " + filePath + " - " + e.getMessage());
        }
    }
    
    /**
     * 处理英雄/怪物信息文件
     */
    private void processInfoFile(Path filePath) throws Exception {
        String fileName = getBaseName(filePath);
        if (fileName.contains("monster") || filePath.toString().contains("monsters")) {
            gameTerms.get(MONSTERS).add(fileName);
        } else {
            gameTerms.get(HEROES).add(fileName);
        }
    }
    
    /**
     * 处理升级文件
     */
    private void processUpgradesFile(Path filePath) throws Exception {
        byte[] data = Files.readAllBytes(filePath);
        Set<String> ids = extractJSONArrayIDs(data, "trees", "id");
        for (String id : ids) {
            gameTerms.get(SKILLS).add(id);
            // 分解组合技能名
            String[] parts = id.split("\\.");
            if (parts.length == 2) {
                gameTerms.get(SKILLS).add(parts[1]);
            }
        }
    }
    
    /**
     * 处理技能文件
     */
    private void processSkillsFile(Path filePath) throws Exception {
        byte[] data = Files.readAllBytes(filePath);
        String jsonString = new String(data);
        JsonObject rootObject = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonArray skillsArray = rootObject.getAsJsonArray("skills");
        
        if (skillsArray != null) {
            for (JsonElement element : skillsArray) {
                JsonObject skill = element.getAsJsonObject();
                String skillId = skill.get("id").getAsString();
                gameTerms.get(SKILLS).add(skillId);
                
                // 添加职业特定技能
                JsonArray heroClasses = skill.getAsJsonArray("hero_classes");
                if (heroClasses != null) {
                    for (JsonElement classElement : heroClasses) {
                        String className = classElement.getAsString();
                        gameTerms.get(SKILLS).add(className + "." + skillId);
                    }
                }
            }
        }
    }
    
    /**
     * 处理怪癖文件
     */
    private void processQuirksFile(Path filePath) throws Exception {
        byte[] data = Files.readAllBytes(filePath);
        Set<String> quirkIds = extractJSONArrayIDs(data, "quirks", "id");
        gameTerms.get(QUIRKS).addAll(quirkIds);
    }
    
    /**
     * 处理物品文件
     */
    private void processInventoryFile(Path filePath) throws Exception {
        byte[] data = Files.readAllBytes(filePath);
        String content = new String(data);
        String[] lines = content.split("\\r?\\n");
        
        for (String line : lines) {
            Matcher matcher = ITEM_PATTERN.matcher(line);
            if (matcher.matches()) {
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    String group = matcher.group(i);
                    if (!group.isEmpty() && isValidTerm(group)) {
                        gameTerms.get(ITEMS).add(group);
                    }
                }
            }
        }
    }
    
    /**
     * 处理饰品文件
     */
    private void processTrinketsFile(Path filePath) throws Exception {
        byte[] data = Files.readAllBytes(filePath);
        Set<String> trinketIds = extractJSONArrayIDs(data, "entries", "id");
        gameTerms.get(TRINKETS).addAll(trinketIds);
    }
    
    /**
     * 处理建筑文件
     */
    private void processBuildingFile(Path filePath) throws Exception {
        String buildingName = getBaseName(filePath);
        gameTerms.get(BUILDINGS).add(buildingName);
        
        byte[] data = Files.readAllBytes(filePath);
        String jsonString = new String(data);
        JsonObject rootObject = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonObject dataObject = rootObject.getAsJsonObject("data");
        
        if (dataObject != null) {
            JsonArray activitiesArray = dataObject.getAsJsonArray("activities");
            if (activitiesArray != null) {
                for (JsonElement element : activitiesArray) {
                    String activityId = element.getAsJsonObject().get("id").getAsString();
                    gameTerms.get(ACTIVITIES).add(activityId);
                }
            }
        }
    }
    
    /**
     * 处理地牢文件
     */
    private void processDungeonFile(Path filePath) throws Exception {
        String dungeonName = getBaseName(filePath);
        gameTerms.get(DUNGEONS).add(dungeonName);
    }
    
    /**
     * 处理事件文件
     */
    private void processEventsFile(Path filePath) throws Exception {
        byte[] data = Files.readAllBytes(filePath);
        Set<String> eventIds = extractJSONArrayIDs(data, "events", "id");
        gameTerms.get(EVENTS).addAll(eventIds);
    }
    
    /**
     * 处理古玩文件
     */
    private void processCuriosFile(Path filePath) throws Exception {
        byte[] data = Files.readAllBytes(filePath);
        String content = new String(data);
        String[] lines = content.split("\\r?\\n");
        
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length > 0 && !parts[0].isEmpty() && isValidTerm(parts[0])) {
                gameTerms.get(CURIOS).add(parts[0]);
            }
        }
    }
    
    /**
     * 处理障碍物文件
     */
    private void processObstaclesFile(Path filePath) throws Exception {
        byte[] data = Files.readAllBytes(filePath);
        Set<String> obstacleIds = extractJSONArrayIDs(data, "props", "name");
        gameTerms.get(OTHERS).addAll(obstacleIds);
    }
    
    /**
     * 处理任务类型文件
     */
    private void processQuestTypesFile(Path filePath) throws Exception {
        byte[] data = Files.readAllBytes(filePath);
        Set<String> typeIds = extractJSONArrayIDs(data, "types", "id");
        Set<String> goalIds = extractJSONArrayIDs(data, "goals", "id");
        gameTerms.get(QUESTS).addAll(typeIds);
        gameTerms.get(QUESTS).addAll(goalIds);
    }
    
    /**
     * 处理教程文件
     */
    private void processTutorialFile(Path filePath) {
        Pattern tutorialPattern = Pattern.compile(".*tutorial_popup\\.([a-z_]*)\\.png");
        Matcher matcher = tutorialPattern.matcher(filePath.toString());
        if (matcher.matches()) {
            String tutorialId = matcher.group(1);
            if (isValidTerm(tutorialId)) {
                gameTerms.get(OTHERS).add(tutorialId);
            }
        }
    }
    
    /**
     * 从JSON数组中提取ID
     */
    private Set<String> extractJSONArrayIDs(byte[] data, String arrayName, String idField) {
        Set<String> ids = new HashSet<>();
        try {
            String jsonString = new String(data);
            JsonObject rootObject = JsonParser.parseString(jsonString).getAsJsonObject();
            JsonArray array = rootObject.getAsJsonArray(arrayName);
            
            if (array != null) {
                for (JsonElement element : array) {
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.has(idField)) {
                        String id = obj.get(idField).getAsString();
                        if (isValidTerm(id)) {
                            ids.add(id);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("解析JSON时出错: " + e.getMessage());
        }
        return ids;
    }
    
    /**
     * 获取文件基础名称
     */
    private String getBaseName(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.indexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }
    
    /**
     * 验证术语是否有效
     */
    private boolean isValidTerm(String term) {
        if (term == null || term.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = term.trim();
        
        // 长度检查
        if (trimmed.length() < 2 || trimmed.length() > 50) {
            return false;
        }
        
        // 格式检查
        return VALID_ID_PATTERN.matcher(trimmed).matches();
    }
    
    /**
     * 保存术语到文件
     */
    private void saveTermsToFiles() {
        try {
            Path dataDir = Paths.get(Helpers.DATA_DIR.getAbsolutePath());
            Files.createDirectories(dataDir);
            
            for (Map.Entry<String, Set<String>> entry : gameTerms.entrySet()) {
                String category = entry.getKey();
                Set<String> terms = entry.getValue();
                
                if (!terms.isEmpty()) {
                    Path categoryFile = dataDir.resolve(category + "_terms.txt");
                    List<String> sortedTerms = new ArrayList<>(terms);
                    Collections.sort(sortedTerms);
                    Files.write(categoryFile, sortedTerms);
                    System.out.println("保存了 " + terms.size() + " 个 " + category + " 术语");
                }
            }
            
            // 保存汇总文件
            saveGameDataSummary(dataDir);
            
        } catch (Exception e) {
            System.err.println("保存术语文件时出错: " + e.getMessage());
        }
    }
    
    /**
     * 保存游戏数据汇总
     */
    private void saveGameDataSummary(Path dataDir) throws Exception {
        Path summaryFile = dataDir.resolve("game_data_summary.json");
        
        JsonObject summary = new JsonObject();
        summary.addProperty("extraction_time", System.currentTimeMillis());
        summary.addProperty("total_categories", gameTerms.size());
        
        JsonObject categories = new JsonObject();
        for (Map.Entry<String, Set<String>> entry : gameTerms.entrySet()) {
            categories.addProperty(entry.getKey(), entry.getValue().size());
        }
        summary.add("categories", categories);
        
        Files.write(summaryFile, Collections.singletonList(summary.toString()));
        System.out.println("保存了游戏数据汇总文件");
    }
    
    /**
     * 获取指定类别的术语
     */
    public Set<String> getTerms(String category) {
        return new HashSet<>(gameTerms.getOrDefault(category, new HashSet<>()));
    }
    
    /**
     * 获取所有类别
     */
    public Set<String> getCategories() {
        return new HashSet<>(gameTerms.keySet());
    }
    
    /**
     * 获取所有术语
     */
    public Map<String, Set<String>> getAllTerms() {
        Map<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : gameTerms.entrySet()) {
            result.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return result;
    }
    
    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 搜索术语
     */
    public Map<String, Set<String>> searchTerms(String keyword) {
        Map<String, Set<String>> results = new HashMap<>();
        String lowerKeyword = keyword.toLowerCase();
        
        for (Map.Entry<String, Set<String>> entry : gameTerms.entrySet()) {
            Set<String> matchingTerms = new HashSet<>();
            for (String term : entry.getValue()) {
                if (term.toLowerCase().contains(lowerKeyword)) {
                    matchingTerms.add(term);
                }
            }
            if (!matchingTerms.isEmpty()) {
                results.put(entry.getKey(), matchingTerms);
            }
        }
        
        return results;
    }
    
    /**
     * 提取结果类
     */
    public static class ExtractionResult {
        public final boolean success;
        public final String message;
        public final Map<String, Set<String>> extractedTerms;
        
        public ExtractionResult(boolean success, String message, Map<String, Set<String>> extractedTerms) {
            this.success = success;
            this.message = message;
            this.extractedTerms = extractedTerms;
        }
    }
    
    /**
     * 获取正面癖好（基于常见的正面癖好名称模式）
     */
    public Set<String> getPositiveQuirks() {
        Set<String> positiveQuirks = new HashSet<>();
        Set<String> allQuirks = getTerms(QUIRKS);
        
        // 常见的正面癖好关键词
        String[] positiveKeywords = {
            "accurate", "tough", "hard_skinned", "eagle_eye", "quick_reflexes",
            "natural", "gifted", "steady", "focused", "calm", "precise",
            "blessed", "holy", "divine", "strong", "vigorous", "healthy",
            "brave", "courageous", "fearless", "determined", "resilient"
        };
        
        for (String quirk : allQuirks) {
            String lowerQuirk = quirk.toLowerCase();
            for (String keyword : positiveKeywords) {
                if (lowerQuirk.contains(keyword)) {
                    positiveQuirks.add(quirk);
                    break;
                }
            }
        }
        
        // 如果没有找到，返回前10个癖好作为示例
        if (positiveQuirks.isEmpty() && !allQuirks.isEmpty()) {
            List<String> quirkList = new ArrayList<>(allQuirks);
            int count = Math.min(10, quirkList.size());
            for (int i = 0; i < count; i++) {
                positiveQuirks.add(quirkList.get(i));
            }
        }
        
        return positiveQuirks;
    }
    
    /**
     * 关闭资源
     */
    public void shutdown() {
        executor.shutdown();
    }
}