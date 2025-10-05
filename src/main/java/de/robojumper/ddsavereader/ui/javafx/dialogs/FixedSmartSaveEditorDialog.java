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
import de.robojumper.ddsavereader.i18n.Messages;

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
    
    // 资源管理控件
    private TextField goldField;
    private TextField bustField;
    private TextField portraitField;
    private TextField deedField;
    private TextField crestField;
    private TextField shardField;
    private TextField memoryField;
    private TextField blueprintField;
    
    // 英雄管理控件
    private ComboBox<String> heroSelector;
    private TextField heroNameField;
    private TextField heroHpField;
    private TextField heroStressField;
    private Spinner<Integer> weaponLevelSpinner;
    private Spinner<Integer> armorLevelSpinner;
    private ComboBox<String> heroClassSelector;
    
    // 庄园管理控件
    private TextField estateNameField;
    
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
        
        // 英雄选择器
        HBox heroSelectorBox = new HBox(10);
        heroSelectorBox.getChildren().addAll(new Label(Messages.getString("saveEditor.heroes.selectHero")), 
            heroSelector = new ComboBox<>());
        heroSelector.setOnAction(e -> loadSelectedHero());
        
        // 英雄属性编辑
        GridPane heroGrid = new GridPane();
        heroGrid.setHgap(10);
        heroGrid.setVgap(5);
        
        heroNameField = new TextField();
        heroHpField = new TextField();
        heroStressField = new TextField();
        weaponLevelSpinner = new Spinner<>(0, 4, 0);
        armorLevelSpinner = new Spinner<>(0, 4, 0);
        heroClassSelector = new ComboBox<>();
        
        heroGrid.add(new Label(Messages.getString("saveEditor.heroes.name")), 0, 0);
        heroGrid.add(heroNameField, 1, 0);
        heroGrid.add(new Label(Messages.getString("saveEditor.heroes.health")), 0, 1);
        heroGrid.add(heroHpField, 1, 1);
        heroGrid.add(new Label(Messages.getString("saveEditor.heroes.stress")), 0, 2);
        heroGrid.add(heroStressField, 1, 2);
        heroGrid.add(new Label(Messages.getString("saveEditor.heroes.weaponLevel")), 0, 3);
        heroGrid.add(weaponLevelSpinner, 1, 3);
        heroGrid.add(new Label(Messages.getString("saveEditor.heroes.armorLevel")), 0, 4);
        heroGrid.add(armorLevelSpinner, 1, 4);
        heroGrid.add(new Label(Messages.getString("saveEditor.heroes.class")), 0, 5);
        heroGrid.add(heroClassSelector, 1, 5);
        
        content.getChildren().addAll(heroSelectorBox, heroGrid);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    private Tab createEstateTab() {
        Tab tab = new Tab(Messages.getString("saveEditor.tab.estate"));
        tab.setClosable(false);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // 庄园名称
        HBox estateNameBox = new HBox(10);
        estateNameField = new TextField();
        estateNameBox.getChildren().addAll(new Label(Messages.getString("saveEditor.estate.name")), estateNameField);
        
        content.getChildren().add(estateNameBox);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    // =============================================
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
            String estateName = root.path("base_root").path("estatename").asText("未知庄园");
            
            Platform.runLater(() -> {
                estateNameField.setText(estateName);
            });
            
            System.out.println("已加载庄园数据: " + estateName);
        } catch (Exception e) {
            System.err.println("加载庄园数据时出错: " + e.getMessage());
        }
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
                        heroNameField.setText(name);
                        heroHpField.setText(String.valueOf(heroData.path("actor").path("current_hp").asDouble(0)));
                        heroStressField.setText(String.valueOf(heroData.path("m_Stress").asDouble(0)));
                        weaponLevelSpinner.getValueFactory().setValue(heroData.path("weapon_rank").asInt(0));
                        armorLevelSpinner.getValueFactory().setValue(heroData.path("armour_rank").asInt(0));
                        
                        String heroClass = heroData.path("heroClass").asText("");
                        heroClassSelector.getSelectionModel().select(heroClass);
                    });
                }
            });
        } catch (Exception e) {
            System.err.println("加载选中英雄数据时出错: " + e.getMessage());
        }
    }
    
    private void loadGameTerms() {
        // 加载英雄职业列表
        List<String> heroClasses = Arrays.asList(
            "abomination", "antiquarian", "arbalest", "bounty_hunter", "crusader",
            "grave_robber", "hellion", "highwayman", "hound_master", "jester",
            "leper", "man_at_arms", "occultist", "plague_doctor", "vestal"
        );
        
        Platform.runLater(() -> {
            heroClassSelector.setItems(FXCollections.observableArrayList(heroClasses));
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
}