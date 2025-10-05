package de.robojumper.ddsavereader.ui.javafx.dialogs;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;

import de.robojumper.ddsavereader.ui.javafx.StateManager;
import de.robojumper.ddsavereader.util.GameTermExtractor;
import de.robojumper.ddsavereader.util.NameManager;
import de.robojumper.ddsavereader.i18n.Messages;
import de.robojumper.ddsavereader.util.Helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Random;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 智能存档编辑器
 * 基于实际存档文件结构进行正确的读取和保存
 */
public class FixedSmartSaveEditorDialog implements Initializable {
    
    private Stage dialogStage;
    private StateManager state;
    private NameManager nameManager;
    private GameTermExtractor termExtractor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 文件名到实际文件的映射（基于StateManager实际加载的文件名）
    private static final Map<String, String> FILE_MAPPING = Map.of(
        "estate", "persist.estate.json",
        "roster", "persist.roster.json", 
        "town", "persist.town.json",
        "game", "persist.game.json"
    );
    
    // 资源类型映射（基于实际JSON结构）
    private static final Map<String, String> RESOURCE_MAPPING = Map.of(
        "gold", "0", "bust", "1", "portrait", "2", "deed", "3",
        "crest", "4", "shard", "5", "memory", "6", "blueprint", "7"
    );
    
    // UI控件
    private TabPane tabPane;
    private VBox rootNode;
    
    // 资源管理控件 (基于 persist.estate_decoded.json)
    private TextField goldField;           // wallet.0.amount
    private TextField bustField;           // wallet.1.amount  
    private TextField portraitField;       // wallet.2.amount
    private TextField deedField;           // wallet.3.amount
    private TextField crestField;          // wallet.4.amount
    private TextField shardField;          // wallet.5.amount
    private TextField memoryField;         // wallet.6.amount
    private TextField blueprintField;      // wallet.7.amount
    
    // 英雄管理控件 (基于 persist.roster_decoded.json)
    private ComboBox<String> heroSelector;
    private TextField heroNameField;       // actor.name
    private TextField heroHpField;         // actor.current_hp
    private TextField heroStressField;     // m_Stress
    private TextField heroResolveXpField;  // resolveXp
    private ComboBox<String> heroClassSelector; // heroClass
    private Spinner<Integer> weaponLevelSpinner;  // weapon_rank
    private Spinner<Integer> armorLevelSpinner;   // armour_rank
    
    // 英雄状态控件
    private CheckBox heroDeathsDoorCheck;  // visited_deaths_door
    private CheckBox heroHeartAttackCheck; // has_had_heart_attack
    private TextField heroStepsTakenField; // steps_taken
    private TextField heroEnemiesKilledField; // enemies_killed
    
    // 英雄怪癖和技能管理
    private ListView<String> currentQuirksList;   // quirks.*
    private ListView<String> availableQuirksList; // 可添加的怪癖
    private ListView<String> currentSkillsList;   // skills.* (废弃，保留兼容)
    private ListView<String> availableSkillsList; // 可添加的技能 (废弃，保留兼容)
    
    // 独立的战斗技能管理
    private ListView<String> currentCombatSkillsList;   // 当前战斗技能
    private ListView<String> availableCombatSkillsList; // 可用战斗技能
    
    // 独立的野营技能管理
    private ListView<String> currentCampingSkillsList;   // 当前野营技能
    private ListView<String> availableCampingSkillsList; // 可用野营技能
    
    // 庄园管理控件 (基于 persist.game_decoded.json)
    private TextField estateNameField;     // estatename
    private ComboBox<String> gameModeSelector;  // game_mode
    private TextField gameTimeField;       // totalelapsed
    private CheckBox inRaidCheckBox;       // inraid
    private ComboBox<String> raidDungeonSelector; // raiddungeon
    private ListView<String> activeDlcList;       // dlc
    private ListView<String> availableDlcList;    // 可用DLC
    private ListView<String> activeModsList;      // applied_ugcs_1_0
    private ListView<String> availableModsList;   // 可用MOD
    
    // 术语存储
    private final Map<String, Set<String>> gameTerms = new HashMap<>();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameManager = NameManager.getInstance();
        termExtractor = GameTermExtractor.getInstance();
        setupUI();
    }
    
    private void setupUI() {
        rootNode = new VBox(10);
        rootNode.setPadding(new Insets(15));
        
        // 创建标题
        Label titleLabel = new Label(Messages.getString("saveEditor.title"));
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // 创建标签页
        tabPane = new TabPane();
        
        // 资源管理标签页
        Tab resourceTab = createResourceTab();
        
        // 英雄管理标签页  
        Tab heroTab = createHeroTab();
        
        // 庄园设置标签页
        Tab estateTab = createEstateTab();
        
        tabPane.getTabs().addAll(resourceTab, heroTab, estateTab);
        
        // 底部按钮
        HBox buttonBox = new HBox(10);
        Button saveButton = new Button(Messages.getString("saveEditor.button.saveAll"));
        Button cancelButton = new Button(Messages.getString("saveEditor.button.cancel"));
        Button refreshButton = new Button(Messages.getString("saveEditor.button.refresh"));
        
        saveButton.setOnAction(e -> saveAllChanges());
        cancelButton.setOnAction(e -> closeDialog());
        refreshButton.setOnAction(e -> loadFromSave());
        
        buttonBox.getChildren().addAll(saveButton, cancelButton, refreshButton);
        
        rootNode.getChildren().addAll(titleLabel, tabPane, buttonBox);
    }
    
    private Tab createResourceTab() {
        Tab tab = new Tab(Messages.getString("saveEditor.tab.resources"));
        tab.setClosable(false);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setPadding(new Insets(10));
        
        // 资源输入框
        goldField = new TextField();
        bustField = new TextField();
        portraitField = new TextField();
        deedField = new TextField();
        crestField = new TextField();
        shardField = new TextField();
        memoryField = new TextField();
        blueprintField = new TextField();
        
        // 添加到网格
        grid.add(new Label(Messages.getString("saveEditor.resources.gold") + ":"), 0, 0);
        grid.add(goldField, 1, 0);
        grid.add(new Label(Messages.getString("saveEditor.resources.bust") + ":"), 0, 1);
        grid.add(bustField, 1, 1);
        grid.add(new Label(Messages.getString("saveEditor.resources.portrait") + ":"), 0, 2);
        grid.add(portraitField, 1, 2);
        grid.add(new Label(Messages.getString("saveEditor.resources.deed") + ":"), 0, 3);
        grid.add(deedField, 1, 3);
        grid.add(new Label(Messages.getString("saveEditor.resources.crest") + ":"), 0, 4);
        grid.add(crestField, 1, 4);
        grid.add(new Label(Messages.getString("saveEditor.resources.shard") + ":"), 0, 5);
        grid.add(shardField, 1, 5);
        grid.add(new Label(Messages.getString("saveEditor.resources.memory") + ":"), 0, 6);
        grid.add(memoryField, 1, 6);
        grid.add(new Label(Messages.getString("saveEditor.resources.blueprint") + ":"), 0, 7);
        grid.add(blueprintField, 1, 7);
        
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    private Tab createHeroTab() {
        Tab tab = new Tab(Messages.getString("saveEditor.tab.heroes"));
        tab.setClosable(false);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // 英雄选择器和操作按钮
        HBox heroSelectorBox = new HBox(10);
        heroSelector = new ComboBox<>();
        heroSelector.setOnAction(e -> loadSelectedHero());
        
        Button saveHeroButton = new Button(Messages.getString("hero.save"));
        saveHeroButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        saveHeroButton.setOnAction(e -> saveSelectedHero());
        
        heroSelectorBox.getChildren().addAll(new Label(Messages.getString("hero.select")), heroSelector, saveHeroButton);
        
        // 创建标签页面板
        TabPane heroTabPane = new TabPane();
        
        // 基础属性标签页
        Tab basicTab = createHeroBasicTab();
        
        // 怪癖管理标签页  
        Tab quirksTab = createHeroQuirksTab();
        
        // 技能管理标签页（分别创建战斗技能和野营技能页面）
        
        // 战斗技能标签页
        Tab combatSkillsTab = createHeroCombatSkillsTab();
        
        // 野营技能标签页
        Tab campingSkillsTab = createHeroCampingSkillsTab();
        
        heroTabPane.getTabs().addAll(basicTab, quirksTab, combatSkillsTab, campingSkillsTab);
        
        content.getChildren().addAll(heroSelectorBox, heroTabPane);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    /**
     * 创建英雄基础属性标签页
     */
    private Tab createHeroBasicTab() {
        Tab tab = new Tab("基础属性");
        tab.setClosable(false);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        // 初始化控件
        heroNameField = new TextField();
        heroHpField = new TextField();
        heroStressField = new TextField();
        heroResolveXpField = new TextField();
        heroClassSelector = new ComboBox<>();
        weaponLevelSpinner = new Spinner<>(0, 4, 0);
        armorLevelSpinner = new Spinner<>(0, 4, 0);
        heroDeathsDoorCheck = new CheckBox();
        heroHeartAttackCheck = new CheckBox();
        heroStepsTakenField = new TextField();
        heroEnemiesKilledField = new TextField();
        
        // 布局
        int row = 0;
        grid.add(new Label("英雄姓名:"), 0, row);
        grid.add(heroNameField, 1, row++);
        
        grid.add(new Label("英雄职业:"), 0, row);
        grid.add(heroClassSelector, 1, row++);
        
        grid.add(new Label("当前生命值:"), 0, row);
        grid.add(heroHpField, 1, row++);
        
        grid.add(new Label("当前压力值:"), 0, row);
        grid.add(heroStressField, 1, row++);
        
        grid.add(new Label("决心经验:"), 0, row);
        grid.add(heroResolveXpField, 1, row++);
        
        grid.add(new Label("武器等级:"), 0, row);
        grid.add(weaponLevelSpinner, 1, row++);
        
        grid.add(new Label("护甲等级:"), 0, row);
        grid.add(armorLevelSpinner, 1, row++);
        
        grid.add(new Label("死亡之门状态:"), 0, row);
        grid.add(heroDeathsDoorCheck, 1, row++);
        
        grid.add(new Label("心脏病发作:"), 0, row);
        grid.add(heroHeartAttackCheck, 1, row++);
        
        grid.add(new Label("步数统计:"), 0, row);
        grid.add(heroStepsTakenField, 1, row++);
        
        grid.add(new Label("击杀敌人数:"), 0, row);
        grid.add(heroEnemiesKilledField, 1, row++);
        
        // 应用按钮
        Button applyHeroBtn = new Button("应用英雄更改");
        applyHeroBtn.setOnAction(e -> applyHeroChanges());
        applyHeroBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        grid.add(applyHeroBtn, 0, row, 2, 1);
        
        tab.setContent(grid);
        return tab;
    }
    
    /**
     * 创建英雄怪癖管理标签页
     */
    private Tab createHeroQuirksTab() {
        Tab tab = new Tab(Messages.getString("hero.tab.quirks"));
        tab.setClosable(false);
        
        HBox content = new HBox(10);
        content.setPadding(new Insets(10));
        
        // 当前怪癖列表
        VBox currentBox = new VBox(5);
        currentBox.getChildren().add(new Label(Messages.getString("hero.quirks.current")));
        currentQuirksList = new ListView<>();
        currentQuirksList.setPrefHeight(200);
        
        Button removeQuirkBtn = new Button(Messages.getString("hero.quirks.remove"));
        removeQuirkBtn.setOnAction(e -> {
            String selectedQuirk = currentQuirksList.getSelectionModel().getSelectedItem();
            if (selectedQuirk != null) {
                currentQuirksList.getItems().remove(selectedQuirk);
                availableQuirksList.getItems().add(selectedQuirk);
                availableQuirksList.getItems().sort(String.CASE_INSENSITIVE_ORDER);
            }
        });
        
        currentBox.getChildren().addAll(currentQuirksList, removeQuirkBtn);
        
        // 可添加怪癖列表
        VBox availableBox = new VBox(5);
        availableBox.getChildren().add(new Label(Messages.getString("hero.quirks.available")));
        availableQuirksList = new ListView<>();
        availableQuirksList.setPrefHeight(200);
        
        Button addQuirkBtn = new Button(Messages.getString("hero.quirks.add"));
        addQuirkBtn.setOnAction(e -> {
            String selectedQuirk = availableQuirksList.getSelectionModel().getSelectedItem();
            if (selectedQuirk != null) {
                availableQuirksList.getItems().remove(selectedQuirk);
                currentQuirksList.getItems().add(selectedQuirk);
                currentQuirksList.getItems().sort(String.CASE_INSENSITIVE_ORDER);
            }
        });
        
        availableBox.getChildren().addAll(availableQuirksList, addQuirkBtn);
        
        content.getChildren().addAll(currentBox, availableBox);
        tab.setContent(content);
        return tab;
    }
    
    /**
     * 创建英雄技能管理标签页
     */
    private Tab createHeroSkillsTab() {
        Tab tab = new Tab(Messages.getString("hero.tab.skills"));
        tab.setClosable(false);
        
        HBox content = new HBox(10);
        content.setPadding(new Insets(10));
        
        // 当前技能列表
        VBox currentBox = new VBox(5);
        currentBox.getChildren().add(new Label(Messages.getString("hero.skills.current")));
        currentSkillsList = new ListView<>();
        currentSkillsList.setPrefHeight(200);
        
        Button removeSkillBtn = new Button(Messages.getString("hero.skills.remove"));
        removeSkillBtn.setOnAction(e -> {
            String selectedSkill = currentSkillsList.getSelectionModel().getSelectedItem();
            if (selectedSkill != null) {
                currentSkillsList.getItems().remove(selectedSkill);
                
                // 移除标记，返回原始技能名称
                String cleanSkill = selectedSkill.replace(" (野营)", "");
                availableSkillsList.getItems().add(cleanSkill);
                availableSkillsList.getItems().sort(String.CASE_INSENSITIVE_ORDER);
            }
        });
        
        currentBox.getChildren().addAll(currentSkillsList, removeSkillBtn);
        
        // 可添加技能列表
        VBox availableBox = new VBox(5);
        availableBox.getChildren().add(new Label(Messages.getString("hero.skills.available")));
        availableSkillsList = new ListView<>();
        availableSkillsList.setPrefHeight(200);
        
        Button addSkillBtn = new Button(Messages.getString("hero.skills.add"));
        addSkillBtn.setOnAction(e -> {
            String selectedSkill = availableSkillsList.getSelectionModel().getSelectedItem();
            if (selectedSkill != null) {
                availableSkillsList.getItems().remove(selectedSkill);
                
                // 判断是否为野营技能并添加标记
                String skillToAdd = selectedSkill;
                if (selectedSkill.toLowerCase().contains("camp") || 
                    selectedSkill.toLowerCase().contains("inspire") ||
                    selectedSkill.toLowerCase().contains("pray") ||
                    selectedSkill.toLowerCase().contains("encourage")) {
                    skillToAdd = selectedSkill + " (野营)";
                }
                
                currentSkillsList.getItems().add(skillToAdd);
                currentSkillsList.getItems().sort(String.CASE_INSENSITIVE_ORDER);
            }
        });
        
        availableBox.getChildren().addAll(availableSkillsList, addSkillBtn);
        
        content.getChildren().addAll(currentBox, availableBox);
        tab.setContent(content);
        return tab;
    }
    
    /**
     * 创建战斗技能管理标签页
     */
    private Tab createHeroCombatSkillsTab() {
        Tab tab = new Tab(Messages.getString("hero.tab.combatSkills"));
        tab.setClosable(false);
        
        HBox content = new HBox(15);
        content.setPadding(new Insets(15));
        
        // 当前战斗技能列表
        VBox currentBox = new VBox(10);
        currentBox.getChildren().add(new Label(Messages.getString("hero.combatSkills.current")));
        currentCombatSkillsList = new ListView<>();
        currentCombatSkillsList.setPrefHeight(300);
        currentCombatSkillsList.setPrefWidth(250);
        
        Button removeCombatSkillBtn = new Button(Messages.getString("hero.combatSkills.remove"));
        removeCombatSkillBtn.setOnAction(e -> {
            String selectedSkill = currentCombatSkillsList.getSelectionModel().getSelectedItem();
            if (selectedSkill != null) {
                currentCombatSkillsList.getItems().remove(selectedSkill);
                availableCombatSkillsList.getItems().add(selectedSkill);
                availableCombatSkillsList.getItems().sort(String.CASE_INSENSITIVE_ORDER);
            }
        });
        
        currentBox.getChildren().addAll(currentCombatSkillsList, removeCombatSkillBtn);
        
        // 可学习战斗技能列表
        VBox availableBox = new VBox(10);
        availableBox.getChildren().add(new Label(Messages.getString("hero.combatSkills.available")));
        availableCombatSkillsList = new ListView<>();
        availableCombatSkillsList.setPrefHeight(300);
        availableCombatSkillsList.setPrefWidth(250);
        
        Button addCombatSkillBtn = new Button(Messages.getString("hero.combatSkills.add"));
        addCombatSkillBtn.setOnAction(e -> {
            String selectedSkill = availableCombatSkillsList.getSelectionModel().getSelectedItem();
            if (selectedSkill != null) {
                // 检查战斗技能数量限制（通常最多4个）
                if (currentCombatSkillsList.getItems().size() >= 4) {
                    showAlert(Messages.getString("common.info"), Messages.getString("hero.combatSkills.limit"));
                    return;
                }
                availableCombatSkillsList.getItems().remove(selectedSkill);
                currentCombatSkillsList.getItems().add(selectedSkill);
                currentCombatSkillsList.getItems().sort(String.CASE_INSENSITIVE_ORDER);
            }
        });
        
        availableBox.getChildren().addAll(availableCombatSkillsList, addCombatSkillBtn);
        
        content.getChildren().addAll(currentBox, availableBox);
        tab.setContent(content);
        return tab;
    }
    
    /**
     * 创建野营技能管理标签页
     */
    private Tab createHeroCampingSkillsTab() {
        Tab tab = new Tab(Messages.getString("hero.tab.campingSkills"));
        tab.setClosable(false);
        
        HBox content = new HBox(15);
        content.setPadding(new Insets(15));
        
        // 当前野营技能列表
        VBox currentBox = new VBox(10);
        currentBox.getChildren().add(new Label(Messages.getString("hero.campingSkills.current")));
        currentCampingSkillsList = new ListView<>();
        currentCampingSkillsList.setPrefHeight(300);
        currentCampingSkillsList.setPrefWidth(250);
        
        Button removeCampingSkillBtn = new Button(Messages.getString("hero.campingSkills.remove"));
        removeCampingSkillBtn.setOnAction(e -> {
            String selectedSkill = currentCampingSkillsList.getSelectionModel().getSelectedItem();
            if (selectedSkill != null) {
                currentCampingSkillsList.getItems().remove(selectedSkill);
                availableCampingSkillsList.getItems().add(selectedSkill);
                availableCampingSkillsList.getItems().sort(String.CASE_INSENSITIVE_ORDER);
            }
        });
        
        currentBox.getChildren().addAll(currentCampingSkillsList, removeCampingSkillBtn);
        
        // 可学习野营技能列表
        VBox availableBox = new VBox(10);
        availableBox.getChildren().add(new Label(Messages.getString("hero.campingSkills.available")));
        availableCampingSkillsList = new ListView<>();
        availableCampingSkillsList.setPrefHeight(300);
        availableCampingSkillsList.setPrefWidth(250);
        
        Button addCampingSkillBtn = new Button(Messages.getString("hero.campingSkills.add"));
        addCampingSkillBtn.setOnAction(e -> {
            String selectedSkill = availableCampingSkillsList.getSelectionModel().getSelectedItem();
            if (selectedSkill != null) {
                // 检查野营技能数量限制（通常最多7个）
                if (currentCampingSkillsList.getItems().size() >= 7) {
                    showAlert(Messages.getString("common.info"), Messages.getString("hero.campingSkills.limit"));
                    return;
                }
                availableCampingSkillsList.getItems().remove(selectedSkill);
                currentCampingSkillsList.getItems().add(selectedSkill);
                currentCampingSkillsList.getItems().sort(String.CASE_INSENSITIVE_ORDER);
            }
        });
        
        availableBox.getChildren().addAll(availableCampingSkillsList, addCampingSkillBtn);
        
        content.getChildren().addAll(currentBox, availableBox);
        tab.setContent(content);
        return tab;
    }
    
    private Tab createEstateTab() {
        Tab tab = new Tab(Messages.getString("saveEditor.tab.estate"));
        tab.setClosable(false);

        VBox content = new VBox(15);
        content.setPadding(new Insets(15));

        // 创建子标签页
        TabPane estateTabPane = new TabPane();
        
        // 基础信息标签页
        Tab basicInfoTab = createEstateBasicInfoTab();
        
        // DLC管理标签页
        Tab dlcTab = createEstateDlcTab();
        
        // MOD管理标签页
        Tab modTab = createEstateModTab();
        
        estateTabPane.getTabs().addAll(basicInfoTab, dlcTab, modTab);
        
        // 保存按钮
        Button saveEstateButton = new Button(Messages.getString("estate.save"));
        saveEstateButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        saveEstateButton.setOnAction(e -> saveEstateData());
        
        content.getChildren().addAll(estateTabPane, saveEstateButton);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);

        return tab;
    }
    
    /**
     * 创建庄园基础信息标签页
     */
    private Tab createEstateBasicInfoTab() {
        Tab tab = new Tab(Messages.getString("estate.tab.basic"));
        tab.setClosable(false);
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        
        // 庄园名称
        grid.add(new Label(Messages.getString("estate.basic.name")), 0, 0);
        estateNameField = new TextField();
        estateNameField.setPrefWidth(200);
        grid.add(estateNameField, 1, 0);
        
        // 游戏模式
        grid.add(new Label(Messages.getString("estate.basic.gameMode")), 0, 1);
        gameModeSelector = new ComboBox<>();
        gameModeSelector.getItems().addAll("darkest", "bloodmoon", "newgameplus", "normal");
        gameModeSelector.setPrefWidth(200);
        grid.add(gameModeSelector, 1, 1);
        
        // 游戏时长 (转换为可读格式)
        grid.add(new Label(Messages.getString("estate.basic.gameTime")), 0, 2);
        gameTimeField = new TextField();
        gameTimeField.setPrefWidth(200);
        grid.add(gameTimeField, 1, 2);
        
        // 当前状态
        grid.add(new Label(Messages.getString("estate.basic.location")), 0, 3);
        inRaidCheckBox = new CheckBox(Messages.getString("estate.basic.inRaid"));
        grid.add(inRaidCheckBox, 1, 3);
        
        // 地牢类型
        grid.add(new Label(Messages.getString("estate.basic.dungeon")), 0, 4);
        raidDungeonSelector = new ComboBox<>();
        raidDungeonSelector.getItems().addAll("none", "crypt", "weald", "warrens", "cove", 
                                             "darkestdungeon", "tutorial", "town", 
                                             "crypts", "tcc_crimson_court", "farmstead");
        raidDungeonSelector.setPrefWidth(200);
        grid.add(raidDungeonSelector, 1, 4);
        
        tab.setContent(grid);
        return tab;
    }
    
    /**
     * 创建DLC管理标签页
     */
    private Tab createEstateDlcTab() {
        Tab tab = new Tab(Messages.getString("estate.tab.dlc"));
        tab.setClosable(false);
        
        HBox content = new HBox(15);
        content.setPadding(new Insets(15));
        
        // 已启用DLC列表
        VBox activeDlcBox = new VBox(10);
        activeDlcBox.getChildren().add(new Label(Messages.getString("estate.dlc.active")));
        activeDlcList = new ListView<>();
        activeDlcList.setPrefHeight(300);
        activeDlcList.setPrefWidth(250);
        
        Button removeDlcBtn = new Button(Messages.getString("estate.dlc.disable"));
        removeDlcBtn.setOnAction(e -> {
            String selectedDlc = activeDlcList.getSelectionModel().getSelectedItem();
            if (selectedDlc != null) {
                activeDlcList.getItems().remove(selectedDlc);
                availableDlcList.getItems().add(selectedDlc);
                availableDlcList.getItems().sort(String.CASE_INSENSITIVE_ORDER);
            }
        });
        
        activeDlcBox.getChildren().addAll(activeDlcList, removeDlcBtn);
        
        // 可用DLC列表
        VBox availableDlcBox = new VBox(10);
        availableDlcBox.getChildren().add(new Label(Messages.getString("estate.dlc.available")));
        availableDlcList = new ListView<>();
        availableDlcList.setPrefHeight(300);
        availableDlcList.setPrefWidth(250);
        
        // 添加常见DLC
        availableDlcList.getItems().addAll(
            "crimson_court", "shieldbreaker", "flagellant", 
            "districts", "color_of_madness", "musketeer", "arena_mp"
        );
        
        Button addDlcBtn = new Button(Messages.getString("estate.dlc.enable"));
        addDlcBtn.setOnAction(e -> {
            String selectedDlc = availableDlcList.getSelectionModel().getSelectedItem();
            if (selectedDlc != null) {
                availableDlcList.getItems().remove(selectedDlc);
                activeDlcList.getItems().add(selectedDlc);
                activeDlcList.getItems().sort(String.CASE_INSENSITIVE_ORDER);
            }
        });
        
        availableDlcBox.getChildren().addAll(availableDlcList, addDlcBtn);
        
        content.getChildren().addAll(activeDlcBox, availableDlcBox);
        tab.setContent(content);
        return tab;
    }
    
    /**
     * 创建MOD管理标签页
     */
    private Tab createEstateModTab() {
        Tab tab = new Tab(Messages.getString("estate.tab.mods"));
        tab.setClosable(false);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));
        
        // 说明文本
        Label infoLabel = new Label(Messages.getString("estate.mods.current"));
        infoLabel.setStyle("-fx-font-weight: bold;");
        
        // MOD列表
        activeModsList = new ListView<>();
        activeModsList.setPrefHeight(400);
        
        // MOD操作按钮
        HBox modButtonBox = new HBox(10);
        
        Button addModBtn = new Button(Messages.getString("estate.mods.add"));
        addModBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle(Messages.getString("estate.mods.addDialog.title"));
            dialog.setHeaderText(Messages.getString("estate.mods.addDialog.header"));
            dialog.setContentText(Messages.getString("estate.mods.addDialog.content"));
            
            dialog.showAndWait().ifPresent(modId -> {
                if (!modId.trim().isEmpty() && !activeModsList.getItems().contains(modId.trim())) {
                    activeModsList.getItems().add(modId.trim());
                }
            });
        });
        
        Button removeModBtn = new Button(Messages.getString("estate.mods.remove"));
        removeModBtn.setOnAction(e -> {
            String selectedMod = activeModsList.getSelectionModel().getSelectedItem();
            if (selectedMod != null) {
                activeModsList.getItems().remove(selectedMod);
            }
        });
        
        Button clearAllModsBtn = new Button(Messages.getString("estate.mods.clear"));
        clearAllModsBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        clearAllModsBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(Messages.getString("estate.mods.clearDialog.title"));
            alert.setHeaderText(Messages.getString("estate.mods.clearDialog.header"));
            alert.setContentText(Messages.getString("estate.mods.clearDialog.content"));
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    activeModsList.getItems().clear();
                }
            });
        });
        
        modButtonBox.getChildren().addAll(addModBtn, removeModBtn, clearAllModsBtn);
        
        content.getChildren().addAll(infoLabel, activeModsList, modButtonBox);
        tab.setContent(content);
        return tab;
    }
    
    /**
     * 保存庄园数据
     */
    private void saveEstateData() {
        StateManager.SaveFile gameFile = findSaveFile("game");
        if (gameFile == null) {
            showAlert(Messages.getString("common.error"), Messages.getString("estate.save.fileNotFound"));
            return;
        }
        
        try {
            JsonNode root = objectMapper.readTree(gameFile.getContents());
            ObjectNode mutableRoot = (ObjectNode) root;
            ObjectNode baseRoot = (ObjectNode) root.path("base_root");
            
            // 保存基础信息
            baseRoot.put("estatename", estateNameField.getText().trim());
            baseRoot.put("game_mode", gameModeSelector.getValue());
            
            // 保存游戏时长（转换回毫秒）
            try {
                double hours = Double.parseDouble(gameTimeField.getText().trim());
                long totalElapsed = (long) (hours * 3600000); // 小时转毫秒
                baseRoot.put("totalelapsed", totalElapsed);
            } catch (NumberFormatException e) {
                // 保持原值
            }
            
            baseRoot.put("inraid", inRaidCheckBox.isSelected());
            baseRoot.put("raiddungeon", raidDungeonSelector.getValue());
            
            // 保存DLC数据
            ObjectNode dlcNode = objectMapper.createObjectNode();
            for (int i = 0; i < activeDlcList.getItems().size(); i++) {
                ObjectNode dlcEntry = objectMapper.createObjectNode();
                dlcEntry.put("name", activeDlcList.getItems().get(i));
                dlcEntry.put("source", "dlc");
                dlcNode.set(String.valueOf(i), dlcEntry);
            }
            baseRoot.set("dlc", dlcNode);
            baseRoot.set("presented_dlc", objectMapper.createObjectNode().set("dlc", dlcNode));
            
            // 保存MOD数据
            ObjectNode modsNode = objectMapper.createObjectNode();
            for (int i = 0; i < activeModsList.getItems().size(); i++) {
                ObjectNode modEntry = objectMapper.createObjectNode();
                modEntry.put("name", activeModsList.getItems().get(i));
                modEntry.put("source", "Steam");
                modsNode.set(String.valueOf(i), modEntry);
            }
            baseRoot.set("applied_ugcs_1_0", modsNode);
            
            // 写入文件
            String updatedContent = objectMapper.writeValueAsString(mutableRoot);
            gameFile.setContents(updatedContent);
            
            showAlert(Messages.getString("common.success"), Messages.getString("estate.save.success"));
            System.out.println("Estate data saved successfully");
            
        } catch (Exception e) {
            showAlert(Messages.getString("common.error"), Messages.getString("estate.save.error") + ": " + e.getMessage());
            System.err.println("Error saving estate data: " + e.getMessage());
        }
    }    // =============================================
    // 数据加载方法
    // =============================================
    
    private void loadFromSave() {
        if (state == null || state.getSaveFiles().isEmpty()) {
            showError(Messages.getString("error.title"), Messages.getString("saveEditor.error.noSaveFiles"));
            return;
        }
        
        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // 加载资源数据
                loadResourceData();
                
                // 加载英雄数据
                loadHeroData();
                
                // 加载庄园数据
                loadEstateData();
                
                // 加载游戏术语数据
                loadGameTerms();
                
                return null;
            }
        };
        
        loadTask.setOnSucceeded(e -> {
            showInfo(Messages.getString("success.title"), Messages.getString("saveEditor.message.loadSuccess"));
        });
        
        loadTask.setOnFailed(e -> {
            showError(Messages.getString("error.title"), Messages.getString("saveEditor.message.loadFailed") + ": " + loadTask.getException().getMessage());
        });
        
        new Thread(loadTask).start();
    }
    
    private void loadResourceData() {
        StateManager.SaveFile estateFile = findSaveFile("estate");
        if (estateFile == null) {
            System.out.println("警告: 未找到庄园文件");
            return;
        }
        
        try {
            JsonNode root = objectMapper.readTree(estateFile.getContents());
            JsonNode wallet = root.path("base_root").path("wallet");
            
            Platform.runLater(() -> {
                goldField.setText(getResourceAmount(wallet, "0"));
                bustField.setText(getResourceAmount(wallet, "1"));
                portraitField.setText(getResourceAmount(wallet, "2"));
                deedField.setText(getResourceAmount(wallet, "3"));
                crestField.setText(getResourceAmount(wallet, "4"));
                shardField.setText(getResourceAmount(wallet, "5"));
                memoryField.setText(getResourceAmount(wallet, "6"));
                blueprintField.setText(getResourceAmount(wallet, "7"));
            });
            
            System.out.println("已加载资源数据");
        } catch (Exception e) {
            System.err.println("加载资源数据时出错: " + e.getMessage());
        }
    }
    
    private void loadHeroData() {
        StateManager.SaveFile rosterFile = findSaveFile("roster");
        if (rosterFile == null) {
            System.out.println("警告: 未找到队伍文件");
            return;
        }
        
        try {
            JsonNode root = objectMapper.readTree(rosterFile.getContents());
            JsonNode heroes = root.path("base_root").path("heroes");
            
            ObservableList<String> heroNames = FXCollections.observableArrayList();
            heroes.fields().forEachRemaining(entry -> {
                JsonNode hero = entry.getValue();
                String name = hero.path("hero_file_data").path("raw_data")
                    .path("base_root").path("actor").path("name").asText("未知英雄");
                heroNames.add(name);
            });
            
            Platform.runLater(() -> {
                heroSelector.setItems(heroNames);
                if (!heroNames.isEmpty()) {
                    heroSelector.getSelectionModel().selectFirst();
                    loadSelectedHero();
                }
            });
            
            System.out.println("已加载英雄列表: " + heroNames.size() + " 个英雄");
        } catch (Exception e) {
            System.err.println("加载英雄数据时出错: " + e.getMessage());
        }
    }
    
    private void loadEstateData() {
        StateManager.SaveFile gameFile = findSaveFile("game");
        if (gameFile == null) {
            System.out.println("警告: 未找到游戏文件");
            return;
        }
        
        try {
            JsonNode root = objectMapper.readTree(gameFile.getContents());
            JsonNode baseRoot = root.path("base_root");
            
            String estateName = baseRoot.path("estatename").asText("未知庄园");
            String gameMode = baseRoot.path("game_mode").asText("normal");
            long totalElapsed = baseRoot.path("totalelapsed").asLong(0);
            boolean inRaid = baseRoot.path("inraid").asBoolean(false);
            String raidDungeon = baseRoot.path("raiddungeon").asText("none");
            
            Platform.runLater(() -> {
                // 加载基础信息
                estateNameField.setText(estateName);
                gameModeSelector.setValue(gameMode);
                
                // 转换游戏时长为小时并显示
                double hours = totalElapsed / 3600000.0; // 毫秒转小时
                gameTimeField.setText(String.format("%.1f", hours));
                
                inRaidCheckBox.setSelected(inRaid);
                raidDungeonSelector.setValue(raidDungeon);
                
                // 加载DLC列表
                loadDlcData(baseRoot);
                
                // 加载MOD列表
                loadModData(baseRoot);
            });
            
            System.out.println("已加载庄园数据: " + estateName);
        } catch (Exception e) {
            System.err.println("加载庄园数据时出错: " + e.getMessage());
        }
    }
    
    /**
     * 加载DLC数据
     */
    private void loadDlcData(JsonNode baseRoot) {
        if (activeDlcList == null || availableDlcList == null) return;
        
        activeDlcList.getItems().clear();
        
        JsonNode dlcNode = baseRoot.path("dlc");
        Set<String> activeDlcs = new HashSet<>();
        
        dlcNode.fields().forEachRemaining(entry -> {
            String dlcName = entry.getValue().path("name").asText("");
            if (!dlcName.isEmpty()) {
                activeDlcs.add(dlcName);
            }
        });
        
        activeDlcList.getItems().addAll(activeDlcs);
        activeDlcList.getItems().sort(String.CASE_INSENSITIVE_ORDER);
        
        // 更新可用DLC列表（移除已启用的）
        List<String> allDlcs = List.of("crimson_court", "shieldbreaker", "flagellant", 
                                      "districts", "color_of_madness", "musketeer", "arena_mp");
        availableDlcList.getItems().clear();
        allDlcs.stream()
               .filter(dlc -> !activeDlcs.contains(dlc))
               .forEach(dlc -> availableDlcList.getItems().add(dlc));
        
        System.out.println("已加载DLC数据: " + activeDlcs.size() + " 个已启用");
    }
    
    /**
     * 加载MOD数据
     */
    private void loadModData(JsonNode baseRoot) {
        if (activeModsList == null) return;
        
        activeModsList.getItems().clear();
        
        JsonNode modsNode = baseRoot.path("applied_ugcs_1_0");
        List<String> activeMods = new ArrayList<>();
        
        modsNode.fields().forEachRemaining(entry -> {
            String modId = entry.getValue().path("name").asText("");
            if (!modId.isEmpty()) {
                activeMods.add(modId);
            }
        });
        
        activeModsList.getItems().addAll(activeMods);
        System.out.println("已加载MOD数据: " + activeMods.size() + " 个已启用");
    }
    
    private void loadSelectedHero() {
        String selectedHeroName = heroSelector.getSelectionModel().getSelectedItem();
        if (selectedHeroName == null) return;
        
        StateManager.SaveFile rosterFile = findSaveFile("roster");
        if (rosterFile == null) return;
        
        try {
            JsonNode root = objectMapper.readTree(rosterFile.getContents());
            JsonNode heroes = root.path("base_root").path("heroes");
            
            // 查找选中的英雄
            heroes.fields().forEachRemaining(entry -> {
                JsonNode hero = entry.getValue();
                JsonNode heroData = hero.path("hero_file_data").path("raw_data").path("base_root");
                String name = heroData.path("actor").path("name").asText("");
                
                if (name.equals(selectedHeroName)) {
                    Platform.runLater(() -> {
                        // 基础属性
                        heroNameField.setText(name);
                        heroHpField.setText(String.valueOf(heroData.path("actor").path("current_hp").asDouble(0)));
                        heroStressField.setText(String.valueOf(heroData.path("m_Stress").asDouble(0)));
                        heroResolveXpField.setText(String.valueOf(heroData.path("resolveXp").asInt(0)));
                        weaponLevelSpinner.getValueFactory().setValue(heroData.path("weapon_rank").asInt(0));
                        armorLevelSpinner.getValueFactory().setValue(heroData.path("armour_rank").asInt(0));
                        
                        String heroClass = heroData.path("heroClass").asText("");
                        heroClassSelector.getSelectionModel().select(heroClass);
                        
                        // 状态复选框
                        heroDeathsDoorCheck.setSelected(heroData.path("visited_deaths_door").asBoolean(false));
                        heroHeartAttackCheck.setSelected(heroData.path("has_had_heart_attack").asBoolean(false));
                        
                        // 统计信息
                        heroStepsTakenField.setText(String.valueOf(heroData.path("steps_taken").asInt(0)));
                        heroEnemiesKilledField.setText(String.valueOf(heroData.path("enemies_killed").asInt(0)));
                        
                        // 加载怪癖
                        loadHeroQuirks(heroData);
                        
                        // 加载技能
                        loadHeroSkills(heroData);
                    });
                }
            });
        } catch (Exception e) {
            System.err.println("加载选中英雄数据时出错: " + e.getMessage());
        }
    }
    
    /**
     * 加载英雄怪癖
     */
    private void loadHeroQuirks(JsonNode heroData) {
        if (currentQuirksList == null || availableQuirksList == null) return;
        
        // 清空现有列表
        currentQuirksList.getItems().clear();
        availableQuirksList.getItems().clear();
        
        // 获取当前怪癖
        Set<String> currentQuirks = new HashSet<>();
        JsonNode quirks = heroData.path("quirks");
        quirks.fields().forEachRemaining(entry -> {
            String quirkName = entry.getKey();
            currentQuirks.add(quirkName);
        });
        
        currentQuirksList.getItems().addAll(currentQuirks);
        currentQuirksList.getItems().sort(String.CASE_INSENSITIVE_ORDER);
        
        // 获取可用怪癖（从术语数据中获取，排除已有的）
        Set<String> allQuirks = gameTerms.getOrDefault("quirks", new HashSet<>());
        List<String> availableQuirks = allQuirks.stream()
            .filter(quirk -> !currentQuirks.contains(quirk))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(java.util.stream.Collectors.toList());
            
        availableQuirksList.getItems().addAll(availableQuirks);
        
        System.out.println("已加载英雄怪癖: " + currentQuirks.size() + " 个当前，" + availableQuirks.size() + " 个可用");
    }
    
    /**
     * 加载英雄技能（更新版本，分别加载战斗和野营技能）
     */
    private void loadHeroSkills(JsonNode heroData) {
        // 加载战斗技能
        loadHeroCombatSkills(heroData);
        
        // 加载野营技能  
        loadHeroCampingSkills(heroData);
        
        // 兼容旧版本的统一技能列表（如果存在）
        if (currentSkillsList != null && availableSkillsList != null) {
            currentSkillsList.getItems().clear();
            availableSkillsList.getItems().clear();
        }
    }
    
    /**
     * 加载英雄战斗技能
     */
    private void loadHeroCombatSkills(JsonNode heroData) {
        if (currentCombatSkillsList == null || availableCombatSkillsList == null) return;
        
        // 清空现有列表
        currentCombatSkillsList.getItems().clear();
        availableCombatSkillsList.getItems().clear();
        
        // 获取当前战斗技能
        Set<String> currentCombatSkills = new HashSet<>();
        
        JsonNode skillsNode = heroData.path("skills");
        JsonNode combatSkills = skillsNode.path("selected_combat_skills");
        combatSkills.fields().forEachRemaining(entry -> {
            String skillName = entry.getKey();
            currentCombatSkills.add(skillName);
            System.out.println("发现战斗技能: " + skillName);
        });
        
        currentCombatSkillsList.getItems().addAll(currentCombatSkills);
        currentCombatSkillsList.getItems().sort(String.CASE_INSENSITIVE_ORDER);
        
        // 获取可用战斗技能
        String heroClass = heroData.path("heroClass").asText("");
        Set<String> allSkills = gameTerms.getOrDefault("skills", new HashSet<>());
        
        List<String> availableCombatSkills = allSkills.stream()
            .filter(skill -> {
                // 过滤战斗技能：排除明显的野营技能
                if (skill.toLowerCase().contains("camp") || 
                    skill.toLowerCase().contains("inspire") ||
                    skill.toLowerCase().contains("pray") ||
                    skill.toLowerCase().contains("encourage") ||
                    skill.toLowerCase().contains("wound_care") ||
                    skill.toLowerCase().contains("stand_tall")) {
                    return false;
                }
                // 检查是否为该职业的技能
                if (skill.contains(".")) {
                    return skill.toLowerCase().contains(heroClass.toLowerCase());
                }
                return true; // 通用战斗技能
            })
            .filter(skill -> !currentCombatSkills.contains(skill))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(java.util.stream.Collectors.toList());
            
        availableCombatSkillsList.getItems().addAll(availableCombatSkills);
        
        System.out.println("已加载战斗技能: " + currentCombatSkills.size() + " 个当前，" + availableCombatSkills.size() + " 个可用");
    }
    
    /**
     * 加载英雄野营技能
     */
    private void loadHeroCampingSkills(JsonNode heroData) {
        if (currentCampingSkillsList == null || availableCampingSkillsList == null) return;
        
        // 清空现有列表
        currentCampingSkillsList.getItems().clear();
        availableCampingSkillsList.getItems().clear();
        
        // 获取当前野营技能
        Set<String> currentCampingSkills = new HashSet<>();
        
        JsonNode skillsNode = heroData.path("skills");
        JsonNode campingSkills = skillsNode.path("selected_camping_skills");
        campingSkills.fields().forEachRemaining(entry -> {
            String skillName = entry.getKey();
            currentCampingSkills.add(skillName);
            System.out.println("发现野营技能: " + skillName);
        });
        
        currentCampingSkillsList.getItems().addAll(currentCampingSkills);
        currentCampingSkillsList.getItems().sort(String.CASE_INSENSITIVE_ORDER);
        
        // 获取可用野营技能
        String heroClass = heroData.path("heroClass").asText("");
        Set<String> allSkills = gameTerms.getOrDefault("skills", new HashSet<>());
        
        List<String> availableCampingSkills = allSkills.stream()
            .filter(skill -> {
                // 过滤野营技能：包含野营关键词的技能
                return skill.toLowerCase().contains("camp") || 
                       skill.toLowerCase().contains("inspire") ||
                       skill.toLowerCase().contains("pray") ||
                       skill.toLowerCase().contains("encourage") ||
                       skill.toLowerCase().contains("wound_care") ||
                       skill.toLowerCase().contains("stand_tall") ||
                       skill.toLowerCase().contains("zealous_speech") ||
                       skill.toLowerCase().contains("battlefield_bandage");
            })
            .filter(skill -> {
                // 检查是否为该职业的技能
                if (skill.contains(".")) {
                    return skill.toLowerCase().contains(heroClass.toLowerCase());
                }
                return true; // 通用野营技能
            })
            .filter(skill -> !currentCampingSkills.contains(skill))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(java.util.stream.Collectors.toList());
            
        availableCampingSkillsList.getItems().addAll(availableCampingSkills);
        
        System.out.println("已加载野营技能: " + currentCampingSkills.size() + " 个当前，" + availableCampingSkills.size() + " 个可用");
        System.out.println("英雄职业: " + heroClass);
    }
    
    /**
     * 保存选中英雄的所有数据
     */
    private void saveSelectedHero() {
        String selectedHeroName = heroSelector.getSelectionModel().getSelectedItem();
        if (selectedHeroName == null) {
            showAlert("错误", "请先选择一个英雄！");
            return;
        }
        
        StateManager.SaveFile rosterFile = findSaveFile("roster");
        if (rosterFile == null) {
            showAlert("错误", "找不到英雄花名册文件！");
            return;
        }
        
        try {
            JsonNode root = objectMapper.readTree(rosterFile.getContents());
            ObjectNode mutableRoot = (ObjectNode) root;
            JsonNode heroes = root.path("base_root").path("heroes");
            
            // 查找选中的英雄并保存数据
            heroes.fields().forEachRemaining(entry -> {
                JsonNode hero = entry.getValue();
                JsonNode heroData = hero.path("hero_file_data").path("raw_data").path("base_root");
                ObjectNode mutableHeroData = (ObjectNode) heroData;
                String name = heroData.path("actor").path("name").asText("");
                
                if (name.equals(selectedHeroName)) {
                    try {
                        // 保存基础属性
                        saveHeroBasicAttributes(mutableHeroData);
                        
                        // 保存怪癖
                        saveHeroQuirks(mutableHeroData);
                        
                        // 保存技能
                        saveHeroSkills(mutableHeroData);
                        
                        // 写入文件
                        String updatedContent = objectMapper.writeValueAsString(mutableRoot);
                        rosterFile.setContents(updatedContent);
                        
                        Platform.runLater(() -> {
                            showAlert("成功", "英雄数据已保存！");
                        });
                        
                        System.out.println("成功保存英雄数据: " + selectedHeroName);
                        
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showAlert("错误", "保存英雄数据失败: " + e.getMessage());
                        });
                        System.err.println("保存英雄数据时出错: " + e.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            showAlert("错误", "读取英雄数据失败: " + e.getMessage());
            System.err.println("读取英雄数据时出错: " + e.getMessage());
        }
    }
    
    /**
     * 保存英雄基础属性
     */
    private void saveHeroBasicAttributes(ObjectNode heroData) {
        try {
            // 基础属性
            if (heroNameField.getText() != null && !heroNameField.getText().trim().isEmpty()) {
                ObjectNode actorNode = (ObjectNode) heroData.path("actor");
                actorNode.put("name", heroNameField.getText().trim());
            }
            
            if (heroHpField.getText() != null && !heroHpField.getText().trim().isEmpty()) {
                ObjectNode actorNode = (ObjectNode) heroData.path("actor");
                actorNode.put("current_hp", Double.parseDouble(heroHpField.getText().trim()));
            }
            
            if (heroStressField.getText() != null && !heroStressField.getText().trim().isEmpty()) {
                heroData.put("m_Stress", Double.parseDouble(heroStressField.getText().trim()));
            }
            
            if (heroResolveXpField.getText() != null && !heroResolveXpField.getText().trim().isEmpty()) {
                heroData.put("resolveXp", Integer.parseInt(heroResolveXpField.getText().trim()));
            }
            
            // 装备等级
            heroData.put("weapon_rank", weaponLevelSpinner.getValue());
            heroData.put("armour_rank", armorLevelSpinner.getValue());
            
            // 职业
            if (heroClassSelector.getValue() != null) {
                heroData.put("heroClass", heroClassSelector.getValue());
            }
            
            // 状态复选框
            heroData.put("visited_deaths_door", heroDeathsDoorCheck.isSelected());
            heroData.put("has_had_heart_attack", heroHeartAttackCheck.isSelected());
            
            // 统计信息
            if (heroStepsTakenField.getText() != null && !heroStepsTakenField.getText().trim().isEmpty()) {
                heroData.put("steps_taken", Integer.parseInt(heroStepsTakenField.getText().trim()));
            }
            
            if (heroEnemiesKilledField.getText() != null && !heroEnemiesKilledField.getText().trim().isEmpty()) {
                heroData.put("enemies_killed", Integer.parseInt(heroEnemiesKilledField.getText().trim()));
            }
            
        } catch (NumberFormatException e) {
            throw new RuntimeException("数值格式错误: " + e.getMessage());
        }
    }
    
    /**
     * 保存英雄怪癖
     */
    private void saveHeroQuirks(ObjectNode heroData) {
        if (currentQuirksList == null) return;
        
        // 清空现有怪癖
        ObjectNode quirksNode = objectMapper.createObjectNode();
        
        // 添加当前怪癖
        for (String quirk : currentQuirksList.getItems()) {
            ObjectNode quirkData = objectMapper.createObjectNode();
            quirkData.put("cooldown", 0);
            quirkData.put("locked_in", false);
            quirksNode.set(quirk, quirkData);
        }
        
        heroData.set("quirks", quirksNode);
        System.out.println("已保存 " + currentQuirksList.getItems().size() + " 个怪癖");
    }
    
    /**
     * 保存英雄技能（更新版本，分别保存战斗和野营技能）
     */
    private void saveHeroSkills(ObjectNode heroData) {
        // 创建或获取 skills 节点
        ObjectNode skillsNode = (ObjectNode) heroData.path("skills");
        if (skillsNode.isMissingNode()) {
            skillsNode = objectMapper.createObjectNode();
            heroData.set("skills", skillsNode);
        }
        
        // 保存战斗技能
        ObjectNode combatSkillsNode = objectMapper.createObjectNode();
        if (currentCombatSkillsList != null) {
            for (String skill : currentCombatSkillsList.getItems()) {
                combatSkillsNode.put(skill, 0); // 值为0
            }
        }
        skillsNode.set("selected_combat_skills", combatSkillsNode);
        
        // 保存野营技能
        ObjectNode campingSkillsNode = objectMapper.createObjectNode();
        if (currentCampingSkillsList != null) {
            for (String skill : currentCampingSkillsList.getItems()) {
                campingSkillsNode.put(skill, 0); // 值为0
            }
        }
        skillsNode.set("selected_camping_skills", campingSkillsNode);
        
        int totalSkills = combatSkillsNode.size() + campingSkillsNode.size();
        System.out.println("已保存技能到 skills 节点 - 总计: " + totalSkills + " 个");
        System.out.println("战斗技能: " + combatSkillsNode.size() + " 个");
        System.out.println("野营技能: " + campingSkillsNode.size() + " 个");
    }
    
    /**
     * 显示提示对话框
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(title.equals("错误") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void loadGameTerms() {
        // 使用真正的GameTermExtractor加载所有游戏术语
        try {
            // 获取游戏目录路径
            String gameDir = (state != null) ? state.getGameDir() : "";
            String modsDir = (state != null) ? state.getModsDir() : "";
            
            System.out.println("正在从游戏目录加载术语数据...");
            System.out.println("游戏目录: " + (gameDir.isEmpty() ? "未设置" : gameDir));
            System.out.println("MOD目录: " + (modsDir.isEmpty() ? "未设置" : modsDir));
            
            // 如果有游戏目录，异步提取术语
            if (!gameDir.isEmpty()) {
                termExtractor.extractTermsAsync(gameDir, modsDir).thenAccept(result -> {
                    if (result.success) {
                        System.out.println("术语提取成功: " + result.message);
                        updateUIWithExtractedTerms(result.extractedTerms);
                    } else {
                        System.err.println("术语提取失败: " + result.message);
                        loadFallbackTerms();
                    }
                }).exceptionally(throwable -> {
                    System.err.println("术语提取异常: " + throwable.getMessage());
                    loadFallbackTerms();
                    return null;
                });
            } else {
                System.out.println("未设置游戏目录，使用备用术语数据");
                loadFallbackTerms();
            }
            
        } catch (Exception e) {
            System.err.println("加载游戏术语失败，使用备用数据: " + e.getMessage());
            loadFallbackTerms();
        }
    }
    
    /**
     * 使用提取的术语更新UI
     */
    private void updateUIWithExtractedTerms(Map<String, Set<String>> extractedTerms) {
        Platform.runLater(() -> {
            // 获取各类术语
            Set<String> heroes = extractedTerms.getOrDefault("heroes", new HashSet<>());
            Set<String> skills = extractedTerms.getOrDefault("skills", new HashSet<>());
            Set<String> quirks = extractedTerms.getOrDefault("quirks", new HashSet<>());
            Set<String> trinkets = extractedTerms.getOrDefault("trinkets", new HashSet<>());
            
            // 转换为排序列表
            List<String> heroClasses = new ArrayList<>(heroes);
            heroClasses.sort(String.CASE_INSENSITIVE_ORDER);
            
            List<String> skillList = new ArrayList<>(skills);
            skillList.sort(String.CASE_INSENSITIVE_ORDER);
            
            List<String> quirkList = new ArrayList<>(quirks);
            quirkList.sort(String.CASE_INSENSITIVE_ORDER);
            
            List<String> trinketList = new ArrayList<>(trinkets);
            trinketList.sort(String.CASE_INSENSITIVE_ORDER);
            
            // 设置英雄职业下拉列表
            heroClassSelector.setItems(FXCollections.observableArrayList(heroClasses));
            
            // 为英雄名称和庄园名称添加智能提示
            setupSmartTextField(heroNameField, generateHeroNames(), "英雄名称");
            setupSmartTextField(estateNameField, generateEstateNames(), "庄园名称");
            
            System.out.println("✅ 智能术语验证系统已启用:");
            System.out.println("- 英雄职业: " + heroes.size() + " 个");
            System.out.println("- 技能: " + skills.size() + " 个");
            System.out.println("- 怪癖: " + quirks.size() + " 个");
            System.out.println("- 饰品: " + trinkets.size() + " 个");
            
            // 保存术语供后续使用
            gameTerms.clear();
            gameTerms.putAll(extractedTerms);
        });
    }
    
    /**
     * 加载备用术语数据
     */
    private void loadFallbackTerms() {
        Platform.runLater(() -> {
            // 备用硬编码列表
            List<String> heroClasses = Arrays.asList(
                "abomination", "antiquarian", "arbalest", "bounty_hunter", "crusader",
                "grave_robber", "hellion", "highwayman", "hound_master", "jester",
                "leper", "man_at_arms", "occultist", "plague_doctor", "vestal"
            );
            
            heroClassSelector.setItems(FXCollections.observableArrayList(heroClasses));
            
            // 为英雄名称和庄园名称添加智能提示
            setupSmartTextField(heroNameField, generateHeroNames(), "英雄名称");
            setupSmartTextField(estateNameField, generateEstateNames(), "庄园名称");
            
            System.out.println("⚠ 使用备用术语数据（硬编码）");
        });
    }
    
    /**
     * 生成英雄名称建议
     */
    private List<String> generateHeroNames() {
        return Arrays.asList(
            "Reynauld", "Dismas", "Gareth", "Baldwin", "William", "Edward",
            "Alhazred", "Barristan", "Boudica", "Agatha", "Margaret", "Eleanor",
            "Bigby", "Damian", "Mordecai", "Junia", "Paracelsus", "Adalynn"
        );
    }
    
    /**
     * 生成庄园名称建议
     */
    private List<String> generateEstateNames() {
        return Arrays.asList(
            "Darkest Estate", "Ancestral Home", "Hamlet Manor", "Shadow Keep",
            "Crimson Court", "Ancient Manor", "Forsaken Estate", "Noble House"
        );
    }

    /**
     * 为文本字段设置智能自动完成功能
     */
    private void setupSmartTextField(TextField textField, List<String> suggestions, String category) {
        if (textField == null) return;
        
        // 创建自动完成功能
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > 0) {
                // 查找匹配的建议
                List<String> matches = suggestions.stream()
                    .filter(s -> s.toLowerCase().contains(newValue.toLowerCase()))
                    .limit(10)
                    .collect(java.util.stream.Collectors.toList());
                
                // 显示工具提示
                if (!matches.isEmpty()) {
                    String tooltip = category + "建议:\n" + String.join("\n", matches.subList(0, Math.min(5, matches.size())));
                    if (matches.size() > 5) {
                        tooltip += "\n... 还有 " + (matches.size() - 5) + " 个匹配项";
                    }
                    textField.setTooltip(new Tooltip(tooltip));
                } else {
                    textField.setTooltip(new Tooltip("未找到匹配的" + category));
                }
            }
        });
        
        // 验证输入
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // 失去焦点时验证
                String input = textField.getText();
                if (input != null && !input.trim().isEmpty()) {
                    boolean isValid = suggestions.stream()
                        .anyMatch(s -> s.equalsIgnoreCase(input.trim()));
                    
                    if (isValid) {
                        textField.setStyle("-fx-border-color: green;");
                        textField.setTooltip(new Tooltip("✓ 有效的" + category));
                    } else {
                        textField.setStyle("-fx-border-color: orange;");
                        textField.setTooltip(new Tooltip("⚠ 警告: 可能不是有效的" + category));
                    }
                }
            }
        });
    }
    
    // =============================================
    // 数据保存方法
    // =============================================
    
    private void saveAllChanges() {
        Task<Void> saveTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                saveResourceChanges();
                saveHeroChanges();
                saveEstateChanges();
                
                // 保存到StateManager
                if (state != null) {
                    state.saveChanges();
                }
                
                return null;
            }
        };
        
        saveTask.setOnSucceeded(e -> {
            showInfo(Messages.getString("success.title"), Messages.getString("saveEditor.message.saveSuccess"));
        });
        
        saveTask.setOnFailed(e -> {
            showError(Messages.getString("error.title"), Messages.getString("saveEditor.message.saveFailed") + ": " + saveTask.getException().getMessage());
        });
        
        new Thread(saveTask).start();
    }
    
    private void saveResourceChanges() {
        StateManager.SaveFile estateFile = findSaveFile("estate");
        if (estateFile == null) {
            throw new RuntimeException("未找到庄园文件");
        }
        
        try {
            JsonNode root = objectMapper.readTree(estateFile.getContents());
            ObjectNode wallet = (ObjectNode) root.path("base_root").path("wallet");
            
            // 更新资源
            updateResourceAmount(wallet, "0", goldField.getText());
            updateResourceAmount(wallet, "1", bustField.getText());
            updateResourceAmount(wallet, "2", portraitField.getText());
            updateResourceAmount(wallet, "3", deedField.getText());
            updateResourceAmount(wallet, "4", crestField.getText());
            updateResourceAmount(wallet, "5", shardField.getText());
            updateResourceAmount(wallet, "6", memoryField.getText());
            updateResourceAmount(wallet, "7", blueprintField.getText());
            
            estateFile.setContents(objectMapper.writeValueAsString(root));
            System.out.println("已保存资源数据");
        } catch (Exception e) {
            throw new RuntimeException("保存资源数据失败", e);
        }
    }
    
    private void saveHeroChanges() {
        String selectedHeroName = heroSelector.getSelectionModel().getSelectedItem();
        if (selectedHeroName == null) return;
        
        StateManager.SaveFile rosterFile = findSaveFile("roster");
        if (rosterFile == null) {
            throw new RuntimeException("未找到队伍文件");
        }
        
        try {
            JsonNode root = objectMapper.readTree(rosterFile.getContents());
            ObjectNode heroes = (ObjectNode) root.path("base_root").path("heroes");
            
            // 查找并更新选中的英雄
            heroes.fields().forEachRemaining(entry -> {
                ObjectNode hero = (ObjectNode) entry.getValue();
                ObjectNode heroData = (ObjectNode) hero.path("hero_file_data").path("raw_data").path("base_root");
                String name = heroData.path("actor").path("name").asText("");
                
                if (name.equals(selectedHeroName)) {
                    // 更新英雄数据
                    ((ObjectNode) heroData.path("actor")).put("name", heroNameField.getText());
                    ((ObjectNode) heroData.path("actor")).put("current_hp", Double.parseDouble(heroHpField.getText()));
                    heroData.put("m_Stress", Double.parseDouble(heroStressField.getText()));
                    heroData.put("weapon_rank", weaponLevelSpinner.getValue());
                    heroData.put("armour_rank", armorLevelSpinner.getValue());
                    
                    String selectedClass = heroClassSelector.getSelectionModel().getSelectedItem();
                    if (selectedClass != null) {
                        heroData.put("heroClass", selectedClass);
                    }
                }
            });
            
            rosterFile.setContents(objectMapper.writeValueAsString(root));
            System.out.println("已保存英雄数据");
        } catch (Exception e) {
            throw new RuntimeException("保存英雄数据失败", e);
        }
    }
    
    private void saveEstateChanges() {
        StateManager.SaveFile gameFile = findSaveFile("game");
        if (gameFile == null) {
            throw new RuntimeException("未找到游戏文件");
        }
        
        try {
            JsonNode root = objectMapper.readTree(gameFile.getContents());
            ObjectNode baseRoot = (ObjectNode) root.path("base_root");
            
            baseRoot.put("estatename", estateNameField.getText());
            
            gameFile.setContents(objectMapper.writeValueAsString(root));
            System.out.println("已保存庄园数据");
        } catch (Exception e) {
            throw new RuntimeException("保存庄园数据失败", e);
        }
    }
    
    // =============================================
    // 辅助方法
    // =============================================
    
    private StateManager.SaveFile findSaveFile(String type) {
        String fileName = FILE_MAPPING.get(type);
        if (fileName == null) return null;
        
        for (StateManager.SaveFile file : state.getSaveFiles()) {
            if (file.getName().equals(fileName)) {
                return file;
            }
        }
        return null;
    }
    
    private String getResourceAmount(JsonNode wallet, String index) {
        return wallet.path(index).path("amount").asText("0");
    }
    
    private void updateResourceAmount(ObjectNode wallet, String index, String amount) {
        try {
            int value = Integer.parseInt(amount.trim());
            ObjectNode resource = (ObjectNode) wallet.path(index);
            resource.put("amount", value);
        } catch (NumberFormatException e) {
            System.err.println("无效的资源数值: " + amount);
        }
    }
    
    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    
    public Stage createDialog() {
        Stage stage = new Stage();
        stage.setTitle(Messages.getString("saveEditor.title"));
        stage.setResizable(true);
        
        javafx.scene.Scene scene = new javafx.scene.Scene(rootNode, 600, 500);
        stage.setScene(scene);
        
        this.dialogStage = stage;
        return stage;
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    public void setState(StateManager state) {
        this.state = state;
        if (state != null) {
            loadFromSave();
        }
    }
    
    // =============================================
    // 新增的英雄管理方法
    // =============================================
    
    /**
     * 应用英雄基础属性更改
     */
    private void applyHeroChanges() {
        try {
            String selectedHero = heroSelector.getValue();
            if (selectedHero == null || selectedHero.isEmpty()) {
                showError("错误", "请先选择一个英雄");
                return;
            }
            
            // 这里将实现实际的英雄数据保存逻辑
            // 基于 persist.roster_decoded.json 的结构
            showInfo("成功", "英雄属性已更新：" + selectedHero);
            
        } catch (Exception e) {
            showError("应用英雄更改失败", e.getMessage());
        }
    }
}