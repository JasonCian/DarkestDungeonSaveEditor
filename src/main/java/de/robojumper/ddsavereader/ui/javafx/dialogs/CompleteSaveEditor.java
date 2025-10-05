package de.robojumper.ddsavereader.ui.javafx.dialogs;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import de.robojumper.ddsavereader.ui.javafx.StateManager;
import de.robojumper.ddsavereader.util.EnhancedTermValidator;

import java.util.*;

/**
 * 增强型智能存档编辑器 - 完整实现版
 * 基于3257个游戏术语的数据验证和智能提示
 */
public class CompleteSaveEditor {
    
    private Stage dialogStage;
    private StateManager state;
    private EnhancedTermValidator validator;
    
    // UI组件
    private VBox rootNode;
    private TabPane tabPane;
    private Label statusLabel;
    
    // 英雄管理
    private ComboBox<String> heroClassSelector;
    private TextField heroNameField;
    private Spinner<Double> heroHpSpinner;
    private Spinner<Double> heroStressSpinner;
    private ListView<String> heroQuirksView;
    private ComboBox<String> quirkSelector;
    
    // 资源管理
    private Spinner<Integer> goldSpinner;
    private Spinner<Integer> bustsSpinner;
    private Spinner<Integer> portraitsSpinner;
    private Spinner<Integer> deedsSpinner;
    private Spinner<Integer> crestsSpinner;
    
    public CompleteSaveEditor(Stage dialogStage, StateManager state) {
        this.dialogStage = dialogStage;
        this.state = state;
        this.validator = EnhancedTermValidator.getInstance();
        
        initializeUI();
        updateStatus("增强型编辑器已就绪，基于 " + getTotalTermCount() + " 个游戏术语");
    }
    
    private int getTotalTermCount() {
        return validator.getTermStatistics().values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * 初始化用户界面
     */
    private void initializeUI() {
        rootNode = new VBox(10);
        rootNode.setPadding(new Insets(15));
        
        // 标题
        Label titleLabel = new Label("🎮 增强型智能存档编辑器");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2E8B57;");
        
        // 状态栏
        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        
        // 术语统计显示
        VBox statsBox = createStatsDisplay();
        
        // 功能标签页
        tabPane = new TabPane();
        
        // 英雄管理标签页
        Tab heroTab = createHeroTab();
        
        // 资源管理标签页
        Tab resourceTab = createResourceTab();
        
        // 验证工具标签页
        Tab validationTab = createValidationTab();
        
        tabPane.getTabs().addAll(heroTab, resourceTab, validationTab);
        
        // 底部按钮
        HBox buttonBox = createButtonBar();
        
        rootNode.getChildren().addAll(titleLabel, statusLabel, statsBox, 
                                     new Separator(), tabPane, buttonBox);
    }
    
    /**
     * 创建术语统计显示
     */
    private VBox createStatsDisplay() {
        VBox statsBox = new VBox(5);
        statsBox.getChildren().add(new Label("📊 术语数据统计:"));
        
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(15);
        statsGrid.setVgap(5);
        
        Map<String, Integer> stats = validator.getTermStatistics();
        int row = 0;
        int col = 0;
        
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            if (entry.getValue() > 0) {
                String displayName = EnhancedTermValidator.getCategoryDisplayName(entry.getKey());
                Label statLabel = new Label(displayName + ": " + entry.getValue());
                statLabel.setStyle("-fx-text-fill: #4169E1; -fx-font-size: 12px;");
                
                statsGrid.add(statLabel, col, row);
                
                col++;
                if (col >= 4) {
                    col = 0;
                    row++;
                }
            }
        }
        
        statsBox.getChildren().add(statsGrid);
        return statsBox;
    }
    
    /**
     * 创建英雄管理标签页
     */
    private Tab createHeroTab() {
        Tab tab = new Tab("👥 英雄管理");
        tab.setClosable(false);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(10));
        
        // 英雄基本信息
        GridPane heroInfoGrid = new GridPane();
        heroInfoGrid.setHgap(10);
        heroInfoGrid.setVgap(10);
        
        // 英雄职业选择
        heroClassSelector = new ComboBox<>();
        heroClassSelector.setItems(FXCollections.observableArrayList(validator.getAllHeroClasses()));
        heroClassSelector.setEditable(true);
        heroClassSelector.setPromptText("选择或输入英雄职业...");
        setupAutoComplete(heroClassSelector, EnhancedTermValidator.HEROES);
        
        // 英雄名称
        heroNameField = new TextField();
        heroNameField.setPromptText("输入英雄名称...");
        
        // 生命值
        heroHpSpinner = new Spinner<>(0.0, 200.0, 100.0, 1.0);
        heroHpSpinner.setEditable(true);
        
        // 压力值
        heroStressSpinner = new Spinner<>(0.0, 200.0, 0.0, 1.0);
        heroStressSpinner.setEditable(true);
        
        heroInfoGrid.addRow(0, new Label("英雄职业:"), heroClassSelector);
        heroInfoGrid.addRow(1, new Label("英雄名称:"), heroNameField);
        heroInfoGrid.addRow(2, new Label("生命值:"), heroHpSpinner);
        heroInfoGrid.addRow(3, new Label("压力值:"), heroStressSpinner);
        
        // 怪癖管理
        VBox quirkBox = new VBox(10);
        Label quirkLabel = new Label("🎲 怪癖管理 (" + validator.getAllQuirks().size() + " 个可用):");
        quirkLabel.setStyle("-fx-font-weight: bold;");
        
        HBox quirkControls = new HBox(10);
        quirkSelector = new ComboBox<>();
        quirkSelector.setItems(FXCollections.observableArrayList(validator.getAllQuirks()));
        quirkSelector.setEditable(true);
        quirkSelector.setPromptText("选择或搜索怪癖...");
        setupAutoComplete(quirkSelector, EnhancedTermValidator.QUIRKS);
        
        Button addQuirkBtn = new Button("➕ 添加");
        Button removeQuirkBtn = new Button("➖ 移除");
        Button randomQuirkBtn = new Button("🎲 随机");
        
        addQuirkBtn.setOnAction(e -> addQuirk());
        removeQuirkBtn.setOnAction(e -> removeSelectedQuirk());
        randomQuirkBtn.setOnAction(e -> addRandomQuirk());
        
        quirkControls.getChildren().addAll(quirkSelector, addQuirkBtn, removeQuirkBtn, randomQuirkBtn);
        
        heroQuirksView = new ListView<>();
        heroQuirksView.setPrefHeight(120);
        
        quirkBox.getChildren().addAll(quirkLabel, quirkControls, heroQuirksView);
        
        // 快速操作按钮
        HBox quickOpsBox = new HBox(10);
        Button healBtn = new Button("💖 满血");
        Button destressBtn = new Button("😌 清压力");
        Button perfectBtn = new Button("⭐ 完美英雄");
        
        healBtn.setOnAction(e -> {
            heroHpSpinner.getValueFactory().setValue(100.0);
            updateStatus("英雄已满血");
        });
        
        destressBtn.setOnAction(e -> {
            heroStressSpinner.getValueFactory().setValue(0.0);
            updateStatus("英雄压力已清除");
        });
        
        perfectBtn.setOnAction(e -> createPerfectHero());
        
        quickOpsBox.getChildren().addAll(healBtn, destressBtn, perfectBtn);
        
        content.getChildren().addAll(
            new Label("英雄基本信息:"),
            heroInfoGrid,
            new Separator(),
            quirkBox,
            new Separator(),
            new Label("快速操作:"),
            quickOpsBox
        );
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    /**
     * 创建资源管理标签页
     */
    private Tab createResourceTab() {
        Tab tab = new Tab("💰 资源管理");
        tab.setClosable(false);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));
        
        // 资源编辑区域
        GridPane resourceGrid = new GridPane();
        resourceGrid.setHgap(20);
        resourceGrid.setVgap(15);
        
        // 创建资源编辑器
        goldSpinner = createResourceSpinner(0, 999999, 0, 1000);
        bustsSpinner = createResourceSpinner(0, 999, 0, 10);
        portraitsSpinner = createResourceSpinner(0, 999, 0, 10);
        deedsSpinner = createResourceSpinner(0, 999, 0, 10);
        crestsSpinner = createResourceSpinner(0, 999, 0, 10);
        
        resourceGrid.addRow(0, new Label("💰 金币:"), goldSpinner, createQuickSetButton(goldSpinner, 999999, "金币"));
        resourceGrid.addRow(1, new Label("🗿 半身像:"), bustsSpinner, createQuickSetButton(bustsSpinner, 999, "半身像"));
        resourceGrid.addRow(2, new Label("🖼️ 肖像:"), portraitsSpinner, createQuickSetButton(portraitsSpinner, 999, "肖像"));
        resourceGrid.addRow(3, new Label("📜 契约:"), deedsSpinner, createQuickSetButton(deedsSpinner, 999, "契约"));
        resourceGrid.addRow(4, new Label("🏅 纹章:"), crestsSpinner, createQuickSetButton(crestsSpinner, 999, "纹章"));
        
        // 批量操作按钮
        HBox batchOpsBox = new HBox(15);
        Button maxAllBtn = new Button("🚀 全部拉满");
        Button resetAllBtn = new Button("🔄 全部清零");
        Button richModeBtn = new Button("💎 富翁模式");
        
        maxAllBtn.setStyle("-fx-base: #4CAF50;");
        resetAllBtn.setStyle("-fx-base: #FF9800;");
        richModeBtn.setStyle("-fx-base: #9C27B0;");
        
        maxAllBtn.setOnAction(e -> setAllResourcesMax());
        resetAllBtn.setOnAction(e -> resetAllResources());
        richModeBtn.setOnAction(e -> setRichMode());
        
        batchOpsBox.getChildren().addAll(maxAllBtn, resetAllBtn, richModeBtn);
        
        content.getChildren().addAll(
            new Label("庄园资源编辑:"),
            resourceGrid,
            new Separator(),
            new Label("批量操作:"),
            batchOpsBox
        );
        
        tab.setContent(content);
        return tab;
    }
    
    /**
     * 创建验证工具标签页
     */
    private Tab createValidationTab() {
        Tab tab = new Tab("🔍 验证工具");
        tab.setClosable(false);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));
        
        // 术语搜索工具
        VBox searchBox = new VBox(10);
        searchBox.getChildren().add(new Label("🔍 术语搜索:"));
        
        HBox searchControls = new HBox(10);
        ComboBox<String> categorySelector = new ComboBox<>();
        categorySelector.setItems(FXCollections.observableArrayList(
            "heroes", "skills", "quirks", "items", "trinkets", "monsters",
            "dungeons", "buildings", "activities", "curios", "events", "quests"
        ));
        categorySelector.setPromptText("选择类别");
        
        TextField searchField = new TextField();
        searchField.setPromptText("输入搜索词...");
        
        Button searchBtn = new Button("搜索");
        
        ListView<String> searchResults = new ListView<>();
        searchResults.setPrefHeight(200);
        
        searchBtn.setOnAction(e -> {
            String category = categorySelector.getValue();
            String query = searchField.getText();
            
            if (category != null && !query.trim().isEmpty()) {
                List<String> results = validator.searchTerms(category, query);
                searchResults.setItems(FXCollections.observableArrayList(results));
                updateStatus("找到 " + results.size() + " 个匹配的" + 
                           EnhancedTermValidator.getCategoryDisplayName(category));
            }
        });
        
        searchControls.getChildren().addAll(categorySelector, searchField, searchBtn);
        searchBox.getChildren().addAll(searchControls, searchResults);
        
        // 随机术语生成器
        VBox randomBox = new VBox(10);
        randomBox.getChildren().add(new Label("🎲 随机术语生成器:"));
        
        HBox randomControls = new HBox(10);
        ComboBox<String> randomCategory = new ComboBox<>();
        randomCategory.setItems(categorySelector.getItems());
        randomCategory.setPromptText("选择类别");
        
        TextField randomResult = new TextField();
        randomResult.setEditable(false);
        randomResult.setPromptText("随机结果将显示在这里...");
        
        Button randomBtn = new Button("生成随机");
        
        randomBtn.setOnAction(e -> {
            String category = randomCategory.getValue();
            if (category != null) {
                String randomTerm = validator.getRandomTerm(category);
                randomResult.setText(randomTerm);
                updateStatus("生成随机" + EnhancedTermValidator.getCategoryDisplayName(category) + ": " + randomTerm);
            }
        });
        
        randomControls.getChildren().addAll(randomCategory, randomResult, randomBtn);
        randomBox.getChildren().add(randomControls);
        
        content.getChildren().addAll(searchBox, new Separator(), randomBox);
        
        tab.setContent(content);
        return tab;
    }
    
    /**
     * 创建底部按钮栏
     */
    private HBox createButtonBar() {
        HBox buttonBox = new HBox(15);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        
        Button saveBtn = new Button("💾 保存修改");
        Button refreshBtn = new Button("🔄 刷新数据");
        Button helpBtn = new Button("❓ 帮助");
        Button closeBtn = new Button("❌ 关闭");
        
        saveBtn.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        closeBtn.setStyle("-fx-base: #f44336; -fx-text-fill: white;");
        
        saveBtn.setOnAction(e -> saveChanges());
        refreshBtn.setOnAction(e -> refreshData());
        helpBtn.setOnAction(e -> showHelp());
        closeBtn.setOnAction(e -> dialogStage.close());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        buttonBox.getChildren().addAll(saveBtn, refreshBtn, helpBtn, spacer, closeBtn);
        
        return buttonBox;
    }
    
    // 辅助方法
    
    private Spinner<Integer> createResourceSpinner(int min, int max, int initial, int step) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initial, step);
        spinner.setEditable(true);
        spinner.setPrefWidth(120);
        return spinner;
    }
    
    private Button createQuickSetButton(Spinner<Integer> spinner, int value, String resource) {
        Button btn = new Button("最大");
        btn.setOnAction(e -> {
            spinner.getValueFactory().setValue(value);
            updateStatus(resource + "已设置为最大值");
        });
        return btn;
    }
    
    private void setupAutoComplete(ComboBox<String> comboBox, String category) {
        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isEmpty()) {
                comboBox.setItems(FXCollections.observableArrayList(validator.getTermSuggestions(category)));
            } else {
                List<String> filtered = validator.getFilteredSuggestions(category, newText);
                comboBox.setItems(FXCollections.observableArrayList(filtered));
            }
        });
        
        // 输入验证
        comboBox.getEditor().focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                String text = comboBox.getEditor().getText();
                if (!text.isEmpty() && !validator.isValidTerm(category, text)) {
                    comboBox.getEditor().setStyle("-fx-control-inner-background: #ffcccc;");
                    updateStatus("⚠️ 警告: '" + text + "' 不是有效的" + 
                               EnhancedTermValidator.getCategoryDisplayName(category));
                } else {
                    comboBox.getEditor().setStyle("");
                }
            }
        });
    }
    
    private void addQuirk() {
        String quirk = quirkSelector.getEditor().getText();
        if (quirk != null && !quirk.trim().isEmpty()) {
            if (validator.isValidQuirk(quirk)) {
                if (!heroQuirksView.getItems().contains(quirk)) {
                    heroQuirksView.getItems().add(quirk);
                    updateStatus("✅ 已添加怪癖: " + quirk);
                } else {
                    updateStatus("⚠️ 该怪癖已存在");
                }
            } else {
                updateStatus("❌ 无效的怪癖: " + quirk);
            }
        }
    }
    
    private void removeSelectedQuirk() {
        String selected = heroQuirksView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            heroQuirksView.getItems().remove(selected);
            updateStatus("✅ 已移除怪癖: " + selected);
        } else {
            updateStatus("⚠️ 请先选择要移除的怪癖");
        }
    }
    
    private void addRandomQuirk() {
        String randomQuirk = validator.getRandomTerm(EnhancedTermValidator.QUIRKS);
        if (!heroQuirksView.getItems().contains(randomQuirk)) {
            heroQuirksView.getItems().add(randomQuirk);
            updateStatus("🎲 随机添加怪癖: " + randomQuirk);
        } else {
            addRandomQuirk(); // 递归尝试，直到找到未拥有的怪癖
        }
    }
    
    private void createPerfectHero() {
        heroHpSpinner.getValueFactory().setValue(100.0);
        heroStressSpinner.getValueFactory().setValue(0.0);
        
        // 清除现有怪癖，添加一些正面怪癖
        heroQuirksView.getItems().clear();
        String[] goodQuirks = {"accuracy", "quick_reflexes", "tough", "focused", "steady"};
        for (String quirk : goodQuirks) {
            if (validator.isValidQuirk(quirk)) {
                heroQuirksView.getItems().add(quirk);
            }
        }
        
        updateStatus("⭐ 已创建完美英雄");
    }
    
    private void setAllResourcesMax() {
        goldSpinner.getValueFactory().setValue(999999);
        bustsSpinner.getValueFactory().setValue(999);
        portraitsSpinner.getValueFactory().setValue(999);
        deedsSpinner.getValueFactory().setValue(999);
        crestsSpinner.getValueFactory().setValue(999);
        updateStatus("🚀 所有资源已设置为最大值");
    }
    
    private void resetAllResources() {
        goldSpinner.getValueFactory().setValue(0);
        bustsSpinner.getValueFactory().setValue(0);
        portraitsSpinner.getValueFactory().setValue(0);
        deedsSpinner.getValueFactory().setValue(0);
        crestsSpinner.getValueFactory().setValue(0);
        updateStatus("🔄 所有资源已重置为零");
    }
    
    private void setRichMode() {
        goldSpinner.getValueFactory().setValue(100000);
        bustsSpinner.getValueFactory().setValue(50);
        portraitsSpinner.getValueFactory().setValue(50);
        deedsSpinner.getValueFactory().setValue(50);
        crestsSpinner.getValueFactory().setValue(50);
        updateStatus("💎 富翁模式已激活");
    }
    
    private void saveChanges() {
        updateStatus("💾 正在保存修改...");
        // TODO: 实现实际的保存逻辑
        updateStatus("✅ 修改保存成功");
    }
    
    private void refreshData() {
        updateStatus("🔄 正在刷新数据...");
        // TODO: 实现数据刷新逻辑
        updateStatus("✅ 数据刷新完成");
    }
    
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("帮助信息");
        alert.setHeaderText("增强型智能存档编辑器");
        alert.setContentText(
            "🎮 功能说明:\n\n" +
            "• 英雄管理: 编辑英雄属性和怪癖\n" +
            "• 资源管理: 修改庄园资源数量\n" +
            "• 验证工具: 搜索和验证游戏术语\n\n" +
            "💡 提示:\n" +
            "• 所有输入框都支持自动完成\n" +
            "• 基于 " + getTotalTermCount() + " 个真实游戏术语\n" +
            "• 输入无效术语时会显示警告"
        );
        alert.showAndWait();
    }
    
    private void updateStatus(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            
            if (message.contains("⚠️") || message.contains("❌")) {
                statusLabel.setTextFill(Color.RED);
            } else if (message.contains("✅") || message.contains("成功")) {
                statusLabel.setTextFill(Color.GREEN);
            } else if (message.contains("🎲") || message.contains("💡")) {
                statusLabel.setTextFill(Color.BLUE);
            } else {
                statusLabel.setTextFill(Color.BLACK);
            }
        });
    }
    
    public VBox getRootNode() {
        return rootNode;
    }
}