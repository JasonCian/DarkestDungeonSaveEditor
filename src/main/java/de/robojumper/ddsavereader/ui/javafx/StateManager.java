package de.robojumper.ddsavereader.ui.javafx;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.application.Platform;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import de.robojumper.ddsavereader.file.DsonFile;
import de.robojumper.ddsavereader.file.DsonFile.UnhashBehavior;
import de.robojumper.ddsavereader.file.DsonWriter;
import de.robojumper.ddsavereader.util.Helpers;

/**
 * JavaFX版本的UI状态管理类
 * 完全移植自Swing版本，使用JavaFX Task替代SwingWorker
 */
public class StateManager {

    private static final File SETTINGS_FILE = new File(Helpers.DATA_DIR, "uisettings.properties");
    private static final File BACKUP_DIR = new File(Helpers.DATA_DIR, "/backups");

    public enum Status {
        OK("/icons/checkmark.png"), 
        WARNING("/icons/warning.png"), 
        ERROR("/icons/error.png"),
        PENDING("/icons/warning.png");

        public final String iconPath;
        private Image cachedImage;

        Status(String iconPath) {
            this.iconPath = iconPath;
        }
        
        public Image getImage() {
            if (cachedImage == null) {
                try {
                    cachedImage = new Image(getClass().getResourceAsStream(iconPath));
                } catch (Exception e) {
                    System.err.println("无法加载图标: " + iconPath);
                }
            }
            return cachedImage;
        }
    }

    public enum Saveability {
        YES, NO, PENDING,
    }

    /**
     * 保存文件类 - 完全移植自Swing版本
     */
    public class SaveFile {
        String name;
        String contents;
        String originalContents;
        int errorPos;
        String errorReason;
        Task<CheckResult> worker;
        private Saveability saveability = Saveability.YES;

        boolean changed() {
            return !Objects.equals(contents, originalContents);
        }

        boolean canSave() {
            return saveability == Saveability.YES;
        }
        
        // 公共访问器方法
        public String getName() { return name; }
        public String getContents() { return contents; }
        public void setContents(String contents) { this.contents = contents; }
        public String getOriginalContents() { return originalContents; }
        public void setOriginalContents(String originalContents) { this.originalContents = originalContents; }
        public String getErrorReason() { return errorReason; }
        public boolean isChanged() { return changed(); }
        public boolean canSaveFile() { return canSave(); }
        public Saveability getSaveability() { return saveability; }
        public int getErrorPos() { return errorPos; }

        int[] getErrorLine() {
            int[] ret = new int[2];
            for (int i = Math.max(errorPos - 1, 0); i < contents.length(); i++) {
                if (contents.charAt(i) == '\n') {
                    ret[1] = i;
                    break;
                }
            }
            for (int i = Math.min(errorPos - 1, contents.length() - 1); i >= 0; i--) {
                if (contents.charAt(i) == '\n') {
                    ret[0] = i;
                    break;
                }
            }
            return ret;
        }
    }

    /**
     * 检查结果类
     */
    public static class CheckResult {
        public final boolean isOk;
        public final String errorReason;
        public final int errorPos;

        public CheckResult(boolean isOk, String errorReason, int errorPos) {
            this.isOk = isOk;
            this.errorReason = errorReason;
            this.errorPos = errorPos;
        }
    }

    /**
     * 后台检查任务 - 替代SwingWorker
     */
    private class CheckInBackground extends Task<CheckResult> {
        private String contents;

        public CheckInBackground(String contents) {
            this.contents = contents;
        }

        @Override
        protected CheckResult call() throws Exception {
            try {
                // Parse the save file to validate its format
                @SuppressWarnings("unused")
                DsonFile save = new DsonFile(contents.getBytes(StandardCharsets.UTF_8), UnhashBehavior.POUNDUNHASH);
                return new CheckResult(true, null, -1);
            } catch (Exception e) {
                return new CheckResult(false, e.getMessage(), extractErrorPosition(e.getMessage()));
            }
        }
        
        private int extractErrorPosition(String errorMessage) {
            // 尝试从错误消息中提取位置信息
            if (errorMessage != null && errorMessage.contains("position")) {
                try {
                    String[] parts = errorMessage.split("position");
                    if (parts.length > 1) {
                        String posStr = parts[1].trim().split("\\s+")[0];
                        return Integer.parseInt(posStr);
                    }
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
            return -1;
        }
    }

    // 状态字段
    private String saveDir = "", gameDir = "", modsDir = "";
    private String profileString;
    private Status saveStatus = Status.ERROR, gameStatus = Status.WARNING, modsStatus = Status.WARNING;
    private boolean sawGameDataPopup;

    private Map<String, SaveFile> files = new TreeMap<>();

    private String lastSheetID = "";
    private ScheduledExecutorService spreadsheetScheduler;
    private ScheduledFuture<?> spreadsheetFuture;

    private Consumer<String> saveStatusChangeCB;

    /**
     * 初始化状态管理器
     */
    public void init(Consumer<String> saveStatusChangeCB) {
        try {
            Properties prop = new Properties();
            if (!SETTINGS_FILE.exists()) {
                SETTINGS_FILE.getParentFile().mkdirs();
                SETTINGS_FILE.createNewFile();
            }
            prop.load(new FileInputStream(SETTINGS_FILE));
            setGameDir((String) prop.getOrDefault("gameDir", ""));
            setModsDir((String) prop.getOrDefault("modsDir", ""));
            setSaveDir((String) prop.getOrDefault("saveDir", ""));
            lastSheetID = (String) prop.getOrDefault("sheetId", "");
            sawGameDataPopup = Boolean.parseBoolean((String) prop.getOrDefault("sawGameDataPopup", ""));
            this.saveStatusChangeCB = saveStatusChangeCB;
        } catch (IOException | ClassCastException e) {
            System.err.println("初始化设置时出错: " + e.getMessage());
        }
    }

    /**
     * 保存设置到文件
     */
    public void save() {
        try {
            Properties prop = new Properties();
            if (!SETTINGS_FILE.exists()) {
                SETTINGS_FILE.getParentFile().mkdirs();
                SETTINGS_FILE.createNewFile();
            }
            Helpers.hideDataDir();
            prop.setProperty("saveDir", saveDir);
            prop.setProperty("gameDir", gameDir);
            prop.setProperty("modsDir", modsDir);
            prop.setProperty("sheetId", lastSheetID);
            prop.setProperty("sawGameDataPopup", String.valueOf(sawGameDataPopup));
            prop.store(new FileOutputStream(SETTINGS_FILE), "DD Save Editor Settings");
        } catch (IOException e) {
            System.err.println("保存设置时出错: " + e.getMessage());
        }
    }

    /**
     * 加载存档文件
     */
    public void loadFiles() {
        files.clear();
        if (saveDir == null || saveDir.isEmpty()) {
            updateSaveStatus();
            return;
        }
        
        File dir = new File(saveDir);
        if (!dir.exists()) {
            updateSaveStatus();
            return;
        }

        File[] fileList = dir.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (Helpers.isSaveFileName(file.getName())) {
                    String content;
                    try {
                        // 使用DsonFile解析二进制存档文件
                        content = new DsonFile(Files.readAllBytes(file.toPath()), UnhashBehavior.POUNDUNHASH).toString() + "\n";
                    } catch (Exception e) {
                        content = "Error reading: " + e.getMessage();
                    }
                    
                    SaveFile saveFile = new SaveFile();
                    saveFile.name = file.getName();
                    saveFile.contents = content;
                    saveFile.originalContents = content;
                    files.put(file.getName(), saveFile);
                }
            }
        }
        updateSaveStatus();
    }

    /**
     * 更新保存状态
     */
    private void updateSaveStatus() {
        if (files.isEmpty()) {
            saveStatus = Status.ERROR;
        } else {
            boolean hasErrors = files.values().stream().anyMatch(f -> !f.canSave());
            boolean hasChanges = files.values().stream().anyMatch(SaveFile::changed);
            
            if (hasErrors) {
                saveStatus = Status.ERROR;
            } else if (hasChanges) {
                saveStatus = Status.WARNING;
            } else {
                saveStatus = Status.OK;
            }
        }
        
        if (saveStatusChangeCB != null) {
            Platform.runLater(() -> saveStatusChangeCB.accept(saveStatus.name()));
        }
    }

    /**
     * 检查文件内容
     */
    public void checkFile(String fileName) {
        SaveFile file = files.get(fileName);
        if (file == null) return;

        // 取消之前的检查任务
        if (file.worker != null && !file.worker.isDone()) {
            file.worker.cancel();
        }

        file.saveability = Saveability.PENDING;
        
        CheckInBackground task = new CheckInBackground(file.contents);
        file.worker = task;
        
        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                CheckResult result = task.getValue();
                if (result.isOk) {
                    file.saveability = Saveability.YES;
                    file.errorReason = null;
                    file.errorPos = -1;
                } else {
                    file.saveability = Saveability.NO;
                    file.errorReason = result.errorReason;
                    file.errorPos = result.errorPos;
                }
                updateSaveStatus();
            });
        });
        
        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                file.saveability = Saveability.NO;
                file.errorReason = "检查失败: " + task.getException().getMessage();
                file.errorPos = -1;
                updateSaveStatus();
            });
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 保存文件
     */
    public void saveFile(String fileName) throws IOException {
        SaveFile file = files.get(fileName);
        if (file == null || !file.canSave()) {
            throw new IOException("文件无法保存: " + fileName);
        }

        File targetFile = new File(saveDir, profileString + "/" + fileName);
        Files.write(targetFile.toPath(), file.contents.getBytes(StandardCharsets.UTF_8));
        file.originalContents = file.contents;
        updateSaveStatus();
    }

    /**
     * 保存所有文件
     */
    public void saveAll() throws IOException {
        for (SaveFile file : files.values()) {
            if (file.changed() && file.canSave()) {
                saveFile(file.name);
            }
        }
    }

    /**
     * 修改文件内容
     */
    public void changeFile(String fileName, String newContents) {
        SaveFile file = files.get(fileName);
        if (file == null) return;
        
        file.contents = newContents;
        checkFile(fileName);
    }

    /**
     * 创建备份
     */
    public void createBackup(String backupName) throws IOException {
        if (!BACKUP_DIR.exists()) {
            BACKUP_DIR.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String finalBackupName = backupName.isEmpty() ? timestamp : backupName + "_" + timestamp;
        
        File backupFile = new File(BACKUP_DIR, profileString + "/" + finalBackupName + ".zip");
        backupFile.getParentFile().mkdirs();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile))) {
            File saveFolder = new File(saveDir, profileString);
            File[] saveFiles = saveFolder.listFiles((dir, name) -> name.endsWith(".json"));
            
            if (saveFiles != null) {
                for (File file : saveFiles) {
                    ZipEntry entry = new ZipEntry(file.getName());
                    zos.putNextEntry(entry);
                    Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                }
            }
        }
    }

    /**
     * 恢复备份
     */
    public void restoreBackup(String backupName) throws IOException {
        File backupFile = new File(BACKUP_DIR, profileString + "/" + backupName + ".zip");
        if (!backupFile.exists()) {
            throw new IOException("备份文件不存在: " + backupName);
        }

        File saveFolder = new File(saveDir, profileString);
        
        try (ZipFile zipFile = new ZipFile(backupFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File targetFile = new File(saveFolder, entry.getName());
                
                try (InputStream is = zipFile.getInputStream(entry);
                     FileOutputStream fos = new FileOutputStream(targetFile)) {
                    is.transferTo(fos);
                }
            }
        }
        
        loadFiles();
    }

    // Getter和Setter方法
    public String getSaveDir() { return saveDir; }
    public String getGameDir() { return gameDir; }
    public String getModsDir() { return modsDir; }
    public String getProfileString() { return profileString; }
    public Status getSaveStatus() { return saveStatus; }
    public Status getGameStatus() { return gameStatus; }
    public Status getModsStatus() { return modsStatus; }
    public boolean sawGameDataPopup() { return sawGameDataPopup; }
    public Collection<SaveFile> getSaveFiles() { return files.values(); }
    public SaveFile getSaveFile(String fileName) { return files.get(fileName); }
    public String getLastSheetID() { return lastSheetID; }

    public void setSaveDir(String saveDir) {
        if (!Objects.equals(saveDir, this.saveDir)) {
            this.saveDir = saveDir;
            if (saveDir != null && !saveDir.isEmpty() && new File(saveDir).exists()) {
                profileString = Paths.get(saveDir).toFile().getName();
                new File(BACKUP_DIR, profileString).mkdirs();
                saveStatus = saveDir.matches(".*profile_[0-9]*/?$") ? Status.OK : Status.WARNING;
                loadFiles();
            } else {
                saveStatus = Status.ERROR;
                profileString = null;
            }
        }
        updateSaveStatus();
    }
    
    public void setGameDir(String gameDir) {
        this.gameDir = gameDir;
        gameStatus = gameDir.isEmpty() ? Status.WARNING : Status.OK;
    }
    
    public void setModsDir(String modsDir) {
        this.modsDir = modsDir;
        modsStatus = modsDir.isEmpty() ? Status.WARNING : Status.OK;
    }
    
    public void setProfileString(String profileString) {
        this.profileString = profileString;
        loadFiles();
    }
    
    public void setSawGameDataPopup(boolean sawGameDataPopup) {
        this.sawGameDataPopup = sawGameDataPopup;
    }
    
    public void setLastSheetID(String lastSheetID) {
        this.lastSheetID = lastSheetID;
    }
    
    public void setSpreadsheetScheduler(ScheduledExecutorService scheduler) {
        this.spreadsheetScheduler = scheduler;
    }

    /**
     * 关闭状态管理器
     */
    public void shutdown() {
        if (spreadsheetFuture != null) {
            spreadsheetFuture.cancel(true);
        }
        if (spreadsheetScheduler != null) {
            spreadsheetScheduler.shutdown();
        }
        
        // 取消所有正在进行的检查任务
        files.values().forEach(file -> {
            if (file.worker != null && !file.worker.isDone()) {
                file.worker.cancel();
            }
        });
        
        save();
    }
    
    // Backup management methods
    public boolean hasAnyBackups() {
        if (profileString != null) {
            File backupDir = new File(BACKUP_DIR, profileString);
            if (backupDir.exists()) {
                String[] files = backupDir.list((dir, name) -> name.endsWith(".zip"));
                return files != null && files.length > 0;
            }
        }
        return false;
    }
    
    public boolean hasBackup(String name) {
        if (profileString != null) {
            return Paths.get(BACKUP_DIR.getAbsolutePath(), profileString, name + ".zip").toFile().exists();
        }
        return false;
    }
    
    public Collection<String> getBackupNames() {
        if (profileString != null) {
            File backupDir = new File(BACKUP_DIR, profileString);
            if (backupDir.exists()) {
                File[] files = backupDir.listFiles();
                if (files != null) {
                    return Arrays.stream(files)
                            .filter((f) -> f.getName().endsWith(".zip"))
                            .sorted((a, b) -> (int) (b.lastModified() - a.lastModified()))
                            .map(s -> s.getName().replaceAll("\\.zip", "")).collect(Collectors.toList());
                }
            }
        }
        return new ArrayList<>();
    }
    
    public boolean makeBackup(String name) {
        // Most simple Box type I could find
        AtomicBoolean success = new AtomicBoolean(true);
        try (FileOutputStream fos = new FileOutputStream(
                Paths.get(BACKUP_DIR.getAbsolutePath(), profileString, name + ".zip").toFile());
                ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {

            Arrays.stream(new File(saveDir).listFiles()).forEach(f -> {
                if (Helpers.isSaveFileName(f.getName())) {
                    try {
                        ZipEntry z = new ZipEntry(f.getName());
                        zos.putNextEntry(z);
                        zos.write(Files.readAllBytes(f.toPath()));
                        zos.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                        success.set(false);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            success.set(false);
        }
        return success.get();
    }
    
    public String getBackupPath() {
        if (profileString != null) {
            return new File(BACKUP_DIR, profileString).getAbsolutePath();
        } else {
            return BACKUP_DIR.getAbsolutePath();
        }
    }
    
    private void clear(File f, boolean deleteSelf) {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                clear(c, true);
        }
        if (deleteSelf) {
            f.delete();
        }
    }

    private void clear(File f) {
        clear(f, true);
    }
    
    // Change tracking and save methods
    public boolean canSave() {
        return files.values().stream().filter(s -> s.isChanged() && !s.canSaveFile()).count() == 0 && anyChanges();
    }

    public boolean isBusy() {
        return files.values().stream().anyMatch(s -> s.getSaveability() == Saveability.PENDING);
    }

    public boolean anyChanges() {
        return files.values().stream().filter(s -> s.isChanged()).count() > 0;
    }

    public int getNumUnsavedChanges() {
        return (int) files.values().stream().filter(f -> f.isChanged()).count();
    }
    
    public void saveChanges() {
        for (SaveFile file : files.values()) {
            if (file.isChanged() && file.canSaveFile()) {
                try {
                    // 使用DsonWriter将JSON内容转换回二进制格式
                    Files.write(Paths.get(saveDir, file.getName()), new DsonWriter(file.getContents()).bytes(),
                            StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                    file.setOriginalContents(file.getContents());
                } catch (Exception e) {
                    System.err.println("保存文件失败: " + file.getName() + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}