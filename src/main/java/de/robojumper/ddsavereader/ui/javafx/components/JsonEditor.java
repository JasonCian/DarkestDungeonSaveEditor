package de.robojumper.ddsavereader.ui.javafx.components;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 现代化的JSON文件编辑器组件
 * 提供语法高亮、行号显示等功能
 */
public class JsonEditor extends ScrollPane {
    
    private static final String[] KEYWORDS = {
        "true", "false", "null"
    };
    
    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String NUMBER_PATTERN = "-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?";
    private static final String BRACE_PATTERN = "[\\{\\}\\[\\]]";
    private static final String COMMENT_PATTERN = "//[^\r\n]*|/\\*(.|\\R)*?\\*/";
    
    private static final Pattern PATTERN = Pattern.compile(
        "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
        + "|(?<STRING>" + STRING_PATTERN + ")"
        + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
        + "|(?<BRACE>" + BRACE_PATTERN + ")"
        + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );
    
    private final CodeArea codeArea;
    
    public JsonEditor() {
        this.codeArea = new CodeArea();
        setupEditor();
        setContent(codeArea);
        setFitToWidth(true);
        setFitToHeight(true);
    }
    
    private void setupEditor() {
        // 设置行号
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        
        // 设置语法高亮
        codeArea.multiPlainChanges()
            .successionEnds(java.time.Duration.ofMillis(500))
            .supplyTask(this::computeHighlightingAsync)
            .awaitLatest(codeArea.multiPlainChanges())
            .filterMap(t -> {
                if (t.isSuccess()) {
                    return java.util.Optional.of(t.get());
                } else {
                    t.getFailure().printStackTrace();
                    return java.util.Optional.empty();
                }
            })
            .subscribe(highlighting -> codeArea.setStyleSpans(0, highlighting));
        
        // 设置自动缩进
        codeArea.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleEnterKey();
            }
        });
        
        // 设置样式类
        codeArea.getStyleClass().add("code-area");
    }
    
    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        String text = codeArea.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
            @Override
            protected StyleSpans<Collection<String>> call() throws Exception {
                return computeHighlighting(text);
            }
        };
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return task;
    }
    
    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        
        while (matcher.find()) {
            String styleClass = null;
            if (matcher.group("KEYWORD") != null) {
                styleClass = "keyword";
            } else if (matcher.group("STRING") != null) {
                styleClass = "string";
            } else if (matcher.group("NUMBER") != null) {
                styleClass = "number";
            } else if (matcher.group("BRACE") != null) {
                styleClass = "brace";
            } else if (matcher.group("COMMENT") != null) {
                styleClass = "comment";
            }
            
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
    
    private void handleEnterKey() {
        int caretPos = codeArea.getCaretPosition();
        int currentParagraph = codeArea.getCurrentParagraph();
        String currentLine = codeArea.getParagraph(currentParagraph).getText();
        
        // 计算当前行的缩进
        int indent = 0;
        for (char c : currentLine.toCharArray()) {
            if (c == ' ') {
                indent++;
            } else if (c == '\t') {
                indent += 4; // 假设tab等于4个空格
            } else {
                break;
            }
        }
        
        // 如果当前行以{或[结尾，增加缩进
        String trimmedLine = currentLine.trim();
        if (trimmedLine.endsWith("{") || trimmedLine.endsWith("[")) {
            indent += 4;
        }
        
        // 插入换行和缩进
        StringBuilder indentStr = new StringBuilder("\n");
        for (int i = 0; i < indent; i++) {
            indentStr.append(" ");
        }
        
        codeArea.insertText(caretPos, indentStr.toString());
    }
    
    public void setText(String text) {
        codeArea.replaceText(text);
        codeArea.moveTo(0);
    }
    
    public String getText() {
        return codeArea.getText();
    }
    
    public CodeArea getCodeArea() {
        return codeArea;
    }
    
    public boolean isModified() {
        // 这里需要与原始内容比较
        return true; // 简化实现
    }
    
    /**
     * 格式化JSON文本
     */
    public void formatJson() {
        String text = getText();
        try {
            // 使用Gson格式化JSON
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                .setPrettyPrinting()
                .create();
            
            Object jsonObject = gson.fromJson(text, Object.class);
            String formattedText = gson.toJson(jsonObject);
            setText(formattedText);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Format Error");
            alert.setHeaderText("Failed to format JSON");
            alert.setContentText("The text is not valid JSON: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    /**
     * 验证JSON语法
     */
    public boolean validateJson() {
        String text = getText();
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            gson.fromJson(text, Object.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}