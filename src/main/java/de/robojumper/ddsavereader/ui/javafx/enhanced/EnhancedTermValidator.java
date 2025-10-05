package de.robojumper.ddsavereader.ui.javafx.enhanced;

import de.robojumper.ddsavereader.util.GameTermExtractor;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 增强的术语验证系统
 * 直接使用GameTermExtractor获取术语数据，无需文件
 */
public class EnhancedTermValidator {
    
    private static EnhancedTermValidator instance;
    private GameTermExtractor termExtractor;
    
    public static EnhancedTermValidator getInstance() {
        if (instance == null) {
            instance = new EnhancedTermValidator();
        }
        return instance;
    }
    
    private EnhancedTermValidator() {
        termExtractor = GameTermExtractor.getInstance();
    }
    
    /**
     * 验证术语是否有效
     */
    public boolean validateTerm(String category, String term) {
        if (term == null || term.trim().isEmpty()) return false;
        Set<String> terms = termExtractor.getTerms(category);
        return terms != null && terms.contains(term.toLowerCase());
    }
    
    /**
     * 获取术语建议列表
     */
    public List<String> getSuggestions(String category, String partial) {
        if (partial == null || partial.trim().isEmpty()) return new ArrayList<>();
        
        Set<String> terms = termExtractor.getTerms(category);
        if (terms == null) return new ArrayList<>();
        
        String lowerPartial = partial.toLowerCase();
        return terms.stream()
            .filter(term -> term.toLowerCase().contains(lowerPartial))
            .sorted()
            .limit(10)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取所有术语列表
     */
    public ObservableList<String> getAllTerms(String category) {
        Set<String> terms = termExtractor.getTerms(category);
        if (terms == null) return FXCollections.observableArrayList();
        
        List<String> sortedTerms = new ArrayList<>(terms);
        Collections.sort(sortedTerms);
        return FXCollections.observableArrayList(sortedTerms);
    }
    
    /**
     * 创建带自动完成功能的ComboBox
     */
    public ComboBox<String> createSmartComboBox(String category) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setEditable(true);
        comboBox.setItems(getAllTerms(category));
        
        // 添加自动完成功能
        comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                List<String> suggestions = getSuggestions(category, newValue);
                if (!suggestions.isEmpty()) {
                    ObservableList<String> items = FXCollections.observableArrayList(suggestions);
                    comboBox.setItems(items);
                    if (!comboBox.isShowing()) {
                        comboBox.show();
                    }
                }
            }
        });
        
        return comboBox;
    }
    
    /**
     * 创建带验证的TextField
     */
    public TextField createValidatedTextField(String category) {
        TextField textField = new TextField();
        
        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.trim().isEmpty()) {
                if (validateTerm(category, newValue)) {
                    textField.setStyle("-fx-text-fill: green; -fx-border-color: green;");
                } else {
                    textField.setStyle("-fx-text-fill: red; -fx-border-color: red;");
                }
            } else {
                textField.setStyle("");
            }
        });
        
        return textField;
    }
    
    /**
     * 获取随机术语
     */
    public String getRandomTerm(String category) {
        Set<String> terms = termExtractor.getTerms(category);
        if (terms == null || terms.isEmpty()) return "";
        
        List<String> termList = new ArrayList<>(terms);
        Random random = new Random();
        return termList.get(random.nextInt(termList.size()));
    }
    
    /**
     * 获取术语统计信息
     */
    public Map<String, Integer> getTermStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        
        String[] categories = {
            GameTermExtractor.HEROES, GameTermExtractor.SKILLS, 
            GameTermExtractor.QUIRKS, GameTermExtractor.ITEMS,
            GameTermExtractor.TRINKETS, GameTermExtractor.MONSTERS,
            GameTermExtractor.DUNGEONS, GameTermExtractor.BUILDINGS,
            GameTermExtractor.ACTIVITIES, GameTermExtractor.CURIOS,
            GameTermExtractor.EVENTS, GameTermExtractor.QUESTS,
            GameTermExtractor.EFFECTS, GameTermExtractor.OTHERS
        };
        
        for (String category : categories) {
            Set<String> terms = termExtractor.getTerms(category);
            stats.put(category, terms != null ? terms.size() : 0);
        }
        
        return stats;
    }
    
    /**
     * 获取总术语数量
     */
    public int getTotalTermCount() {
        return getTermStatistics().values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * 搜索术语
     */
    public Map<String, List<String>> searchTerms(String keyword) {
        return termExtractor.searchTerms(keyword).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> new ArrayList<>(entry.getValue())
            ));
    }
    
    /**
     * 获取类别的中文显示名称
     */
    public String getCategoryDisplayName(String category) {
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
    
    /**
     * 检查术语提取器是否可用
     */
    public boolean isAvailable() {
        return termExtractor != null;
    }
}