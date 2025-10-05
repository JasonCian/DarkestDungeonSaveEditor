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

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Random;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 智能简易存档编辑器
 * 利用GameTermExtractor和NameManager提供的数据来增强编辑功能
 */
public class SmartSaveEditorDialog implements Initializable {
    
    private Stage dialogStage;
    private StateManager state;
    private NameManager nameManager;
    private GameTermExtractor termExtractor;
    
    // UI控件
    private TabPane tabPane;
    private VBox rootNode;
    
    // 资源管理控件
    private TextField goldField;
    private TextField bustField;
    private TextField portraitField;
    private TextField deedField;
    private TextField crestField;
    private TextField shardField;
    
    // 英雄管理控件
    private ComboBox<String> heroSelector;
    private TextField heroNameField;
    private TextField heroHpField;
    private TextField heroStressField;
    private Spinner<Integer> weaponLevelSpinner;
    private Spinner<Integer> armorLevelSpinner;
    private ComboBox<String> heroClassSelector;
    private ListView<String> heroQuirksListView;
    private ListView<String> availableQuirksListView;
    
    // 物品管理控件
    private ListView<String> trinketListView;
    private ComboBox<String> trinketSelector;
    
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
        Label titleLabel = new Label("智能存档编辑器");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // 创建标签页
        tabPane = new TabPane();
        
        // 资源管理标签页
        Tab resourceTab = createResourceTab();
        
        // 英雄管理标签页  
        Tab heroTab = createHeroTab();
        
        // 物品管理标签页
        Tab itemTab = createItemTab();
        
        tabPane.getTabs().addAll(resourceTab, heroTab, itemTab);
        
        // 底部按钮
        HBox buttonBox = new HBox(10);
        Button saveButton = new Button("保存所有修改");
        Button cancelButton = new Button("取消");
        Button refreshButton = new Button("刷新数据");
        Button infoButton = new Button("文件信息");
        
        saveButton.setOnAction(e -> saveAllChanges());
        cancelButton.setOnAction(e -> dialogStage.close());
        refreshButton.setOnAction(e -> refreshGameData());
        infoButton.setOnAction(e -> showLoadedFilesInfo());
        
        buttonBox.getChildren().addAll(saveButton, cancelButton, refreshButton, infoButton);
        
        rootNode.getChildren().addAll(titleLabel, tabPane, buttonBox);
    }
    
    public VBox getRootNode() {
        return rootNode;
    }
    
    private Tab createResourceTab() {
        Tab tab = new Tab("💰 资源管理");
        tab.setClosable(false);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // 标题
        Label titleLabel = new Label("游戏资源");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // 创建资源输入控件
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        
        // 金钱
        gridPane.add(new Label("金钱:"), 0, 0);
        goldField = new TextField();
        gridPane.add(goldField, 1, 0);
        Button maxGoldBtn = new Button("最大值");
        maxGoldBtn.setOnAction(e -> goldField.setText("999999"));
        gridPane.add(maxGoldBtn, 2, 0);
        
        // 胸像
        gridPane.add(new Label("胸像:"), 0, 1);
        bustField = new TextField();
        gridPane.add(bustField, 1, 1);
        Button maxBustBtn = new Button("最大值");
        maxBustBtn.setOnAction(e -> bustField.setText("999"));
        gridPane.add(maxBustBtn, 2, 1);
        
        // 肖像
        gridPane.add(new Label("肖像:"), 0, 2);
        portraitField = new TextField();
        gridPane.add(portraitField, 1, 2);
        Button maxPortraitBtn = new Button("最大值");
        maxPortraitBtn.setOnAction(e -> portraitField.setText("999"));
        gridPane.add(maxPortraitBtn, 2, 2);
        
        // 契约
        gridPane.add(new Label("契约:"), 0, 3);
        deedField = new TextField();
        gridPane.add(deedField, 1, 3);
        Button maxDeedBtn = new Button("最大值");
        maxDeedBtn.setOnAction(e -> deedField.setText("999"));
        gridPane.add(maxDeedBtn, 2, 3);
        
        // 纹章
        gridPane.add(new Label("纹章:"), 0, 4);
        crestField = new TextField();
        gridPane.add(crestField, 1, 4);
        Button maxCrestBtn = new Button("最大值");
        maxCrestBtn.setOnAction(e -> crestField.setText("999"));
        gridPane.add(maxCrestBtn, 2, 4);
        
        // 碎片
        gridPane.add(new Label("碎片:"), 0, 5);
        shardField = new TextField();
        gridPane.add(shardField, 1, 5);
        Button maxShardBtn = new Button("最大值");
        maxShardBtn.setOnAction(e -> shardField.setText("999"));
        gridPane.add(maxShardBtn, 2, 5);
        
        // 快捷按钮
        HBox quickButtons = new HBox(10);
        Button richModeBtn = new Button("土豪模式");
        Button reasonableBtn = new Button("合理配置");
        Button clearBtn = new Button("清空");
        
        richModeBtn.setOnAction(e -> setRichMode());
        reasonableBtn.setOnAction(e -> setReasonableResources());
        clearBtn.setOnAction(e -> clearResources());
        
        quickButtons.getChildren().addAll(richModeBtn, reasonableBtn, clearBtn);
        
        content.getChildren().addAll(titleLabel, gridPane, quickButtons);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    private Tab createHeroTab() {
        Tab tab = new Tab("⚔️ 英雄管理");
        tab.setClosable(false);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // 英雄选择
        HBox heroSelectBox = new HBox(10);
        heroSelectBox.getChildren().addAll(
            new Label("选择英雄:"),
            heroSelector = new ComboBox<>()
        );
        heroSelector.setOnAction(e -> onHeroSelected());
        
        // 英雄基本信息
        GridPane heroInfoGrid = new GridPane();
        heroInfoGrid.setHgap(10);
        heroInfoGrid.setVgap(10);
        
        heroInfoGrid.add(new Label("名字:"), 0, 0);
        heroNameField = new TextField();
        heroInfoGrid.add(heroNameField, 1, 0);
        Button randomNameBtn = new Button("随机名字");
        randomNameBtn.setOnAction(e -> generateRandomHeroName());
        heroInfoGrid.add(randomNameBtn, 2, 0);
        
        heroInfoGrid.add(new Label("职业:"), 0, 1);
        heroClassSelector = new ComboBox<>();
        heroInfoGrid.add(heroClassSelector, 1, 1);
        
        heroInfoGrid.add(new Label("生命值:"), 0, 2);
        heroHpField = new TextField();
        heroInfoGrid.add(heroHpField, 1, 2);
        Button fullHealBtn = new Button("完全治愈");
        fullHealBtn.setOnAction(e -> fullHeal());
        heroInfoGrid.add(fullHealBtn, 2, 2);
        
        heroInfoGrid.add(new Label("压力值:"), 0, 3);
        heroStressField = new TextField();
        heroInfoGrid.add(heroStressField, 1, 3);
        Button clearStressBtn = new Button("清除压力");
        clearStressBtn.setOnAction(e -> clearStress());
        heroInfoGrid.add(clearStressBtn, 2, 3);
        
        heroInfoGrid.add(new Label("武器等级:"), 0, 4);
        weaponLevelSpinner = new Spinner<>(0, 5, 0);
        heroInfoGrid.add(weaponLevelSpinner, 1, 4);
        Button maxWeaponBtn = new Button("最高等级");
        maxWeaponBtn.setOnAction(e -> weaponLevelSpinner.getValueFactory().setValue(5));
        heroInfoGrid.add(maxWeaponBtn, 2, 4);
        
        heroInfoGrid.add(new Label("护甲等级:"), 0, 5);
        armorLevelSpinner = new Spinner<>(0, 5, 0);
        heroInfoGrid.add(armorLevelSpinner, 1, 5);
        Button maxArmorBtn = new Button("最高等级");
        maxArmorBtn.setOnAction(e -> armorLevelSpinner.getValueFactory().setValue(5));
        heroInfoGrid.add(maxArmorBtn, 2, 5);
        
        // 癖好管理
        HBox quirksBox = new HBox(20);
        
        VBox currentQuirksBox = new VBox(10);
        currentQuirksBox.getChildren().addAll(
            new Label("当前癖好:"),
            heroQuirksListView = new ListView<>()
        );
        heroQuirksListView.setPrefHeight(150);
        
        VBox availableQuirksBox = new VBox(10);
        availableQuirksBox.getChildren().addAll(
            new Label("可用癖好:"),
            availableQuirksListView = new ListView<>()
        );
        availableQuirksListView.setPrefHeight(150);
        
        VBox quirksButtonsBox = new VBox(10);
        Button addQuirkBtn = new Button("添加 →");
        Button removeQuirkBtn = new Button("← 移除");
        Button clearAllQuirksBtn = new Button("清空所有");
        Button randomPositiveBtn = new Button("随机正面");
        
        addQuirkBtn.setOnAction(e -> addSelectedQuirk());
        removeQuirkBtn.setOnAction(e -> removeSelectedQuirk());
        clearAllQuirksBtn.setOnAction(e -> clearAllQuirks());
        randomPositiveBtn.setOnAction(e -> addRandomPositiveQuirk());
        
        quirksButtonsBox.getChildren().addAll(addQuirkBtn, removeQuirkBtn, clearAllQuirksBtn, randomPositiveBtn);
        
        quirksBox.getChildren().addAll(currentQuirksBox, quirksButtonsBox, availableQuirksBox);
        
        content.getChildren().addAll(heroSelectBox, heroInfoGrid, new Separator(), quirksBox);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    private Tab createItemTab() {
        Tab tab = new Tab("💍 物品管理");
        tab.setClosable(false);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // 饰品管理
        HBox trinketSelectBox = new HBox(10);
        trinketSelectBox.getChildren().addAll(
            new Label("添加饰品:"),
            trinketSelector = new ComboBox<>()
        );
        
        Button addTrinketBtn = new Button("添加到背包");
        addTrinketBtn.setOnAction(e -> addSelectedTrinket());
        trinketSelectBox.getChildren().add(addTrinketBtn);
        
        // 当前饰品列表
        VBox trinketListBox = new VBox(10);
        trinketListBox.getChildren().addAll(
            new Label("当前饰品:"),
            trinketListView = new ListView<>()
        );
        trinketListView.setPrefHeight(200);
        
        HBox trinketButtonsBox = new HBox(10);
        Button removeTrinketBtn = new Button("移除选中");
        Button clearTrinsketsBtn = new Button("清空所有");
        Button addRandomBtn = new Button("添加随机");
        
        removeTrinketBtn.setOnAction(e -> removeSelectedTrinket());
        clearTrinsketsBtn.setOnAction(e -> clearAllTrinkets());
        addRandomBtn.setOnAction(e -> addRandomTrinket());
        
        trinketButtonsBox.getChildren().addAll(removeTrinketBtn, clearTrinsketsBtn, addRandomBtn);
        
        content.getChildren().addAll(trinketSelectBox, trinketListBox, trinketButtonsBox);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    // =============================================
    // 数据操作方法
    // =============================================
    
    private void refreshGameData() {
        // 异步刷新游戏数据
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // 触发数据提取
                nameManager.generateGameDataAsync(state.getGameDir(), state.getModsDir());
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    loadGameTerms();
                    showInfo("数据刷新完成", "游戏术语数据已更新");
                });
            }
        };
        
        new Thread(task).start();
    }
    
    private void loadGameTerms() {
        // 加载英雄职业
        Set<String> heroClasses = termExtractor.getTerms(GameTermExtractor.HEROES);
        ObservableList<String> classItems = FXCollections.observableArrayList(heroClasses);
        heroClassSelector.setItems(classItems);
        
        // 加载癖好
        Set<String> quirks = termExtractor.getTerms(GameTermExtractor.QUIRKS);
        ObservableList<String> quirkItems = FXCollections.observableArrayList(quirks);
        availableQuirksListView.setItems(quirkItems);
        
        // 加载饰品
        Set<String> trinkets = termExtractor.getTerms(GameTermExtractor.TRINKETS);
        ObservableList<String> trinketItems = FXCollections.observableArrayList(trinkets);
        trinketSelector.setItems(trinketItems);
        
        System.out.println("游戏术语加载完成 - 英雄: " + heroClasses.size() + ", 癖好: " + quirks.size() + ", 饰品: " + trinkets.size());
    }
    
    private void showLoadedFilesInfo() {
        if (state == null || state.getSaveFiles().isEmpty()) {
            showError("无存档文件", "没有加载任何存档文件。请先打开存档目录。");
            return;
        }
        
        StringBuilder info = new StringBuilder("已加载的存档文件:\n");
        for (StateManager.SaveFile file : state.getSaveFiles()) {
            info.append("• ").append(file.getName()).append("\n");
        }
        info.append("\n存档目录: ").append(state.getSaveDir());
        
        showInfo("存档文件信息", info.toString());
    }
    
    // =============================================
    // 资源管理方法
    // =============================================
    
    private void setRichMode() {
        goldField.setText("500000");
        bustField.setText("100");
        portraitField.setText("100");
        deedField.setText("100");
        crestField.setText("100");
        shardField.setText("100");
    }
    
    private void setReasonableResources() {
        goldField.setText("50000");
        bustField.setText("20");
        portraitField.setText("20");
        deedField.setText("20");
        crestField.setText("20");
        shardField.setText("20");
    }
    
    private void clearResources() {
        goldField.setText("0");
        bustField.setText("0");
        portraitField.setText("0");
        deedField.setText("0");
        crestField.setText("0");
        shardField.setText("0");
    }
    
    // =============================================
    // 英雄管理方法
    // =============================================
    
    private void onHeroSelected() {
        // 加载选中英雄的数据
        String selectedHero = heroSelector.getSelectionModel().getSelectedItem();
        if (selectedHero != null) {
            loadHeroData(selectedHero);
        }
    }
    
    private void loadHeroData(String heroName) {
        // 这里应该从存档文件加载英雄数据
        // 现在先用示例数据
        heroNameField.setText(heroName);
        heroHpField.setText("100");
        heroStressField.setText("0");
        weaponLevelSpinner.getValueFactory().setValue(0);
        armorLevelSpinner.getValueFactory().setValue(0);
        
        // 清空当前癖好列表
        heroQuirksListView.getItems().clear();
    }
    
    private void generateRandomHeroName() {
        String randomName = nameManager.getRandomHeroName();
        heroNameField.setText(randomName);
    }
    
    private void fullHeal() {
        heroHpField.setText("100");
    }
    
    private void clearStress() {
        heroStressField.setText("0");
    }
    
    private void addSelectedQuirk() {
        String selectedQuirk = availableQuirksListView.getSelectionModel().getSelectedItem();
        if (selectedQuirk != null && !heroQuirksListView.getItems().contains(selectedQuirk)) {
            heroQuirksListView.getItems().add(selectedQuirk);
        }
    }
    
    private void removeSelectedQuirk() {
        String selectedQuirk = heroQuirksListView.getSelectionModel().getSelectedItem();
        if (selectedQuirk != null) {
            heroQuirksListView.getItems().remove(selectedQuirk);
        }
    }
    
    private void clearAllQuirks() {
        heroQuirksListView.getItems().clear();
    }
    
    private void addRandomPositiveQuirk() {
        Set<String> positiveQuirks = termExtractor.getPositiveQuirks();
        if (!positiveQuirks.isEmpty()) {
            List<String> quirkList = new ArrayList<>(positiveQuirks);
            Random random = new Random();
            String randomQuirk = quirkList.get(random.nextInt(quirkList.size()));
            
            if (!heroQuirksListView.getItems().contains(randomQuirk)) {
                heroQuirksListView.getItems().add(randomQuirk);
            }
        }
    }
    
    // =============================================
    // 物品管理方法
    // =============================================
    
    private void addSelectedTrinket() {
        String selectedTrinket = trinketSelector.getSelectionModel().getSelectedItem();
        if (selectedTrinket != null) {
            trinketListView.getItems().add(selectedTrinket);
        }
    }
    
    private void removeSelectedTrinket() {
        String selectedTrinket = trinketListView.getSelectionModel().getSelectedItem();
        if (selectedTrinket != null) {
            trinketListView.getItems().remove(selectedTrinket);
        }
    }
    
    private void clearAllTrinkets() {
        trinketListView.getItems().clear();
    }
    
    private void addRandomTrinket() {
        Set<String> trinkets = termExtractor.getTerms(GameTermExtractor.TRINKETS);
        if (!trinkets.isEmpty()) {
            List<String> trinketList = new ArrayList<>(trinkets);
            Random random = new Random();
            String randomTrinket = trinketList.get(random.nextInt(trinketList.size()));
            trinketListView.getItems().add(randomTrinket);
        }
    }
    
    // =============================================
    // 保存和加载方法
    // =============================================
    
    private void saveAllChanges() {
        if (state == null || state.getSaveFiles().isEmpty()) {
            showError("错误", "没有加载任何存档文件");
            return;
        }
        
        try {
            // 保存资源修改
            saveResourceChanges();
            
            // 保存英雄修改
            saveHeroChanges();
            
            // 保存物品修改
            saveItemChanges();
            
            // 调用StateManager保存所有更改
            state.saveChanges();
            
            showInfo("保存完成", "所有修改已保存到存档文件");
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("保存失败", "保存时发生错误: " + e.getMessage());
        }
    }
    
    private void saveResourceChanges() {
        // 查找estate.json文件（包含资源信息）
        for (StateManager.SaveFile saveFile : state.getSaveFiles()) {
            if (saveFile.getName().contains("estate") || saveFile.getName().contains("persist")) {
                String content = saveFile.getContents();
                
                // 更新金钱
                if (!goldField.getText().trim().isEmpty()) {
                    content = updateFieldInJson(content, "currency", "gold", goldField.getText().trim());
                }
                
                // 更新胸像
                if (!bustField.getText().trim().isEmpty()) {
                    content = updateFieldInJson(content, "currency", "bust", bustField.getText().trim());
                }
                
                // 更新肖像
                if (!portraitField.getText().trim().isEmpty()) {
                    content = updateFieldInJson(content, "currency", "portrait", portraitField.getText().trim());
                }
                
                // 更新契约
                if (!deedField.getText().trim().isEmpty()) {
                    content = updateFieldInJson(content, "currency", "deed", deedField.getText().trim());
                }
                
                // 更新纹章
                if (!crestField.getText().trim().isEmpty()) {
                    content = updateFieldInJson(content, "currency", "crest", crestField.getText().trim());
                }
                
                // 更新碎片
                if (!shardField.getText().trim().isEmpty()) {
                    content = updateFieldInJson(content, "currency", "shard", shardField.getText().trim());
                }
                
                saveFile.setContents(content);
                System.out.println("已更新资源数据在文件: " + saveFile.getName());
                break;
            }
        }
    }
    
    private void saveHeroChanges() {
        // 查找roster.json文件（包含英雄信息）
        for (StateManager.SaveFile saveFile : state.getSaveFiles()) {
            if (saveFile.getName().contains("roster")) {
                String content = saveFile.getContents();
                
                // 这里应该更新选中英雄的信息
                String selectedHero = heroSelector.getSelectionModel().getSelectedItem();
                if (selectedHero != null && !heroNameField.getText().trim().isEmpty()) {
                    // 更新英雄名字、生命值、压力等
                    content = updateHeroData(content, selectedHero, 
                        heroNameField.getText().trim(),
                        heroHpField.getText().trim(),
                        heroStressField.getText().trim(),
                        weaponLevelSpinner.getValue(),
                        armorLevelSpinner.getValue());
                }
                
                saveFile.setContents(content);
                System.out.println("已更新英雄数据在文件: " + saveFile.getName());
                break;
            }
        }
    }
    
    private void saveItemChanges() {
        // 查找inventory.json文件（包含物品信息）
        for (StateManager.SaveFile saveFile : state.getSaveFiles()) {
            if (saveFile.getName().contains("inventory") || saveFile.getName().contains("trinket")) {
                String content = saveFile.getContents();
                
                // 更新饰品列表
                content = updateTrinketInventory(content, trinketListView.getItems());
                
                saveFile.setContents(content);
                System.out.println("已更新物品数据在文件: " + saveFile.getName());
                break;
            }
        }
    }
    
    private void loadFromSave() {
        if (state == null || state.getSaveFiles().isEmpty()) {
            System.out.println("警告: 没有可用的存档文件");
            return;
        }
        
        // 加载资源数据
        loadResourceData();
        
        // 加载英雄列表
        loadHeroData();
        
        // 加载物品数据
        loadItemData();
    }
    
    private void loadResourceData() {
        // 查找包含资源信息的文件
        for (StateManager.SaveFile saveFile : state.getSaveFiles()) {
            if (saveFile.getName().contains("estate") || saveFile.getName().contains("persist")) {
                String content = saveFile.getContents();
                
                // 解析资源数据
                String gold = extractFieldFromJson(content, "currency", "gold");
                String bust = extractFieldFromJson(content, "currency", "bust");
                String portrait = extractFieldFromJson(content, "currency", "portrait");
                String deed = extractFieldFromJson(content, "currency", "deed");
                String crest = extractFieldFromJson(content, "currency", "crest");
                String shard = extractFieldFromJson(content, "currency", "shard");
                
                // 更新UI控件
                Platform.runLater(() -> {
                    goldField.setText(gold.isEmpty() ? "0" : gold);
                    bustField.setText(bust.isEmpty() ? "0" : bust);
                    portraitField.setText(portrait.isEmpty() ? "0" : portrait);
                    deedField.setText(deed.isEmpty() ? "0" : deed);
                    crestField.setText(crest.isEmpty() ? "0" : crest);
                    shardField.setText(shard.isEmpty() ? "0" : shard);
                });
                
                System.out.println("已从文件加载资源数据: " + saveFile.getName());
                break;
            }
        }
    }
    
    private void loadHeroData() {
        ObservableList<String> heroes = FXCollections.observableArrayList();
        
        // 查找包含英雄信息的文件
        for (StateManager.SaveFile saveFile : state.getSaveFiles()) {
            if (saveFile.getName().contains("roster")) {
                String content = saveFile.getContents();
                
                // 解析英雄列表
                List<String> heroNames = extractHeroNames(content);
                heroes.addAll(heroNames);
                
                System.out.println("已从文件加载英雄数据: " + saveFile.getName() + ", 英雄数量: " + heroNames.size());
                break;
            }
        }
        
        // 如果没有找到英雄，使用默认数据
        if (heroes.isEmpty()) {
            heroes.addAll(Arrays.asList("雷纳德", "迪斯马斯", "维纳", "塔斯卡"));
            System.out.println("使用默认英雄数据");
        }
        
        Platform.runLater(() -> {
            heroSelector.setItems(heroes);
            if (!heroes.isEmpty()) {
                heroSelector.getSelectionModel().selectFirst();
            }
        });
    }
    
    private void loadItemData() {
        ObservableList<String> trinkets = FXCollections.observableArrayList();
        
        // 查找包含物品信息的文件
        for (StateManager.SaveFile saveFile : state.getSaveFiles()) {
            if (saveFile.getName().contains("inventory") || saveFile.getName().contains("trinket")) {
                String content = saveFile.getContents();
                
                // 解析饰品列表
                List<String> trinketNames = extractTrinketNames(content);
                trinkets.addAll(trinketNames);
                
                System.out.println("已从文件加载物品数据: " + saveFile.getName() + ", 饰品数量: " + trinketNames.size());
                break;
            }
        }
        
        Platform.runLater(() -> {
            trinketListView.setItems(trinkets);
        });
    }
    
    // =============================================
    // JSON解析辅助方法
    // =============================================
    
    private String extractFieldFromJson(String content, String section, String field) {
        try {
            // 简单的正则表达式匹配
            String pattern = "\"" + field + "\"\\s*:\\s*(\\d+)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(content);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            System.err.println("解析字段 " + field + " 时出错: " + e.getMessage());
        }
        return "";
    }
    
    private String updateFieldInJson(String content, String section, String field, String newValue) {
        try {
            // 简单的正则表达式替换
            String pattern = "(\"" + field + "\"\\s*:\\s*)(\\d+)";
            String replacement = "$1" + newValue;
            return content.replaceFirst(pattern, replacement);
        } catch (Exception e) {
            System.err.println("更新字段 " + field + " 时出错: " + e.getMessage());
            return content;
        }
    }
    
    private List<String> extractHeroNames(String content) {
        List<String> names = new ArrayList<>();
        try {
            // 查找英雄名字的模式
            String pattern = "\"name\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(content);
            while (m.find()) {
                names.add(m.group(1));
            }
        } catch (Exception e) {
            System.err.println("解析英雄名字时出错: " + e.getMessage());
        }
        return names;
    }
    
    private List<String> extractTrinketNames(String content) {
        List<String> trinkets = new ArrayList<>();
        try {
            // 查找饰品的模式
            String pattern = "\"type\"\\s*:\\s*\"trinket\"[^}]*\"id\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(content);
            while (m.find()) {
                trinkets.add(m.group(1));
            }
        } catch (Exception e) {
            System.err.println("解析饰品时出错: " + e.getMessage());
        }
        return trinkets;
    }
    
    private String updateHeroData(String content, String heroName, String newName, String hp, String stress, int weaponLevel, int armorLevel) {
        try {
            // 这里应该实现复杂的英雄数据更新逻辑
            // 简化处理：只更新名字
            String pattern = "(\"name\"\\s*:\\s*\")" + java.util.regex.Pattern.quote(heroName) + "(\")";
            String replacement = "$1" + newName + "$2";
            return content.replaceFirst(pattern, replacement);
        } catch (Exception e) {
            System.err.println("更新英雄数据时出错: " + e.getMessage());
            return content;
        }
    }
    
    private String updateTrinketInventory(String content, ObservableList<String> trinkets) {
        try {
            // 这里应该实现饰品库存更新逻辑
            // 简化处理：返回原内容
            System.out.println("待实现：更新饰品库存，当前饰品数量: " + trinkets.size());
            return content;
        } catch (Exception e) {
            System.err.println("更新饰品库存时出错: " + e.getMessage());
            return content;
        }
    }
    
    // =============================================
    // 辅助方法
    // =============================================
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    public void setState(StateManager state) {
        this.state = state;
        if (state != null) {
            loadFromSave();
            loadGameTerms();
        }
    }
}