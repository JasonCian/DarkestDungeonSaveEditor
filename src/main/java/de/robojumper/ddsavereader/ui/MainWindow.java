package de.robojumper.ddsavereader.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

import de.robojumper.ddsavereader.BuildConfig;
import de.robojumper.ddsavereader.i18n.Messages;
import de.robojumper.ddsavereader.spreadsheets.SpreadsheetsService;
import de.robojumper.ddsavereader.spreadsheets.SpreadsheetsService.SheetUpdater;
import de.robojumper.ddsavereader.ui.LanguageSelectionDialog;
import de.robojumper.ddsavereader.ui.State.SaveFile;
import de.robojumper.ddsavereader.ui.State.Status;
import de.robojumper.ddsavereader.updatechecker.UpdateChecker;
import de.robojumper.ddsavereader.updatechecker.UpdateChecker.Release;
import de.robojumper.ddsavereader.updatechecker.Version;

public class MainWindow {

    private JFrame frame;
    private JTextField savePathBox;
    private JLabel saveFileStatus;
    private JTabbedPane tabbedPane;
    private JLabel saveStatus, errorLabel;
    private JButton saveButton;
    private JButton makeBackupButton, restoreBackupButton;
    private JButton discardChangesButton, reloadButton;
    private JMenuItem mntmSpreadsheets, mntmNames;
    private JButton btnNewUpdateAvailable;

    private static volatile Release latestRelease;

    private State state = new State();

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainWindow window = new MainWindow();

                    window.state.init(window::onStateChange);
                    window.initSettings();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public MainWindow() {
        initialize();
    }

    private void onStateChange(String fname) {
        updateSaveStatus();
        updateTabStatus(fname);
    }

    private void initSettings() {
        if (!state.sawGameDataPopup()) {
            new DataPathsDialog(frame, state.getGameDir(), state.getModsDir(), state, true);
            state.setSawGameDataPopup(true);
        }
        DataPathsDialog.updateFromDataFile();
        updateSaveDir();
        updateFiles();
        updateSaveStatus();
        checkForUpdates();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e1) {
            e1.printStackTrace();
        }

        frame = new JFrame();
        frame.setBounds(100, 100, 800, 500);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                attemptExit();
            }
        });
        frame.setTitle(BuildConfig.DISPLAY_NAME + "/" + BuildConfig.VERSION);

        JMenuBar menuBar = new JMenuBar();
        frame.getContentPane().add(menuBar, BorderLayout.NORTH);

        JMenu fileMenu = new JMenu(Messages.getString("menu.file"));
        menuBar.add(fileMenu);

        JMenuItem mntmExit = new JMenuItem(Messages.getString("menu.file.exit"));
        mntmExit.addActionListener(e -> {
            attemptExit();
        });

        JMenuItem mntmOpenBackupDirectory = new JMenuItem(Messages.getString("menu.file.openBackupDirectory"));
        mntmOpenBackupDirectory.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File(state.getBackupPath()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        fileMenu.add(mntmOpenBackupDirectory);
        fileMenu.add(mntmExit);

        JMenu mnTools = new JMenu(Messages.getString("menu.tools"));
        menuBar.add(mnTools);

        mntmNames = new JMenuItem(Messages.getString("menu.tools.generateNameFile"));
        mntmNames.addActionListener(e -> {
            if (confirmLoseChanges()) {
                new DataPathsDialog(frame, state.getGameDir(), state.getModsDir(), state, false);
                DataPathsDialog.updateFromDataFile();
                state.loadFiles();
                updateFiles();
            }
        });
        mnTools.add(mntmNames);

        mntmSpreadsheets = new JMenuItem(Messages.getString("menu.tools.spreadsheets"));
        mntmSpreadsheets.setEnabled(false);
        mntmSpreadsheets.addActionListener(e -> {
            if (state.getSaveStatus() != Status.ERROR) {
                SheetUpdater sheetUpdater;
                try {
                    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                    Credential cred = SpreadsheetsService.getCredentials(HTTP_TRANSPORT);
                    if (cred == null) {
                        Object[] options = { Messages.getString("button.ok"), Messages.getString("button.goToReadme") };
                        int openInstructions = JOptionPane.showOptionDialog(frame,
                                Messages.getString("error.spreadsheetNotSetup"),
                                Messages.getString("error.errorTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
                                options[0]);
                        switch (openInstructions) {
                        case 0:
                            break;
                        case 1:
                            try {
                                Desktop.getDesktop().browse(
                                        new URI(BuildConfig.GITHUB_URL + "/blob/master/README.md#spreadsheets"));
                            } catch (IOException | URISyntaxException e1) {
                                e1.printStackTrace();
                            }
                        }
                        return;
                    }
                    String sheetID = JOptionPane.showInputDialog(frame, Messages.getString("dialog.setSpreadsheetId"), state.getLastSheetID());
                    if (sheetID != null) {
                        state.setLastSheetID(sheetID);
                        sheetUpdater = SpreadsheetsService.makeUpdaterRunnable(sheetID, state.getSaveDir(), cred,
                                HTTP_TRANSPORT);
                    } else {
                        return;
                    }
                } catch (IOException | GeneralSecurityException e1) {
                    JOptionPane.showMessageDialog(null, Messages.getString("error.unknownError"), Messages.getString("menu.tools.spreadsheets"),
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

                JLabel runningLabel = new JLabel(Messages.getString("status.running"));
                ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(new Runnable() {

                    @Override
                    public void run() {
                        if (sheetUpdater.isRunning()) {
                            sheetUpdater.run();
                        } else {
                            runningLabel.setText(Messages.getString("status.stopped"));
                        }
                    }
                }, 3, 120, TimeUnit.SECONDS);

                JOptionPane.showConfirmDialog(null, runningLabel, "Spreadsheets", JOptionPane.DEFAULT_OPTION,
                        JOptionPane.PLAIN_MESSAGE);

                if (sheetUpdater.isRunning()) {
                    sheetUpdater.cancel();
                }
                future.cancel(false);
            }
        });
        mnTools.add(mntmSpreadsheets);

        JMenu mnHelp = new JMenu(Messages.getString("menu.help"));
        menuBar.add(mnHelp);

        JMenuItem mntmLanguage = new JMenuItem("Language / 语言");
        mntmLanguage.addActionListener(e -> {
            LanguageSelectionDialog langDialog = new LanguageSelectionDialog(frame);
            if (langDialog.showDialog()) {
                langDialog.saveLanguagePreference();
                JOptionPane.showMessageDialog(frame, 
                    "Language will be applied after restart.\n语言将在重启后生效。",
                    "Language / 语言", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        mnHelp.add(mntmLanguage);

        JMenuItem mntmAbout = new JMenuItem(Messages.getString("menu.help.about"));
        mntmAbout.addActionListener(e -> {
            Object[] options = { Messages.getString("button.ok"), Messages.getString("button.goToGitHub") };
            int result = JOptionPane.showOptionDialog(frame,
                    Messages.getString("dialog.aboutMessage", BuildConfig.DISPLAY_NAME, BuildConfig.VERSION, BuildConfig.GITHUB_URL),
                    Messages.getString("dialog.aboutTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            switch (result) {
            case 0:
                break;
            case 1:
                try {
                    Desktop.getDesktop().browse(new URI(BuildConfig.GITHUB_URL));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
        });
        mnHelp.add(mntmAbout);

        Component horizontalGlue_1 = Box.createHorizontalGlue();
        menuBar.add(horizontalGlue_1);

        btnNewUpdateAvailable = new JButton(Messages.getString("button.newUpdateAvailable"));
        btnNewUpdateAvailable.setVisible(false);
        btnNewUpdateAvailable.setEnabled(false);
        btnNewUpdateAvailable.addActionListener(e -> {
            if (latestRelease != null) {
                Version curr = new Version(BuildConfig.VERSION);
                Object[] options = { Messages.getString("button.ok"), Messages.getString("button.goToReleases") };
                int openInstructions = JOptionPane.showOptionDialog(frame,
                        Messages.getString("dialog.updateAvailableMessage", curr.toString(), latestRelease.version.toString()),
                        Messages.getString("dialog.updateAvailableTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
                        options[1]);
                switch (openInstructions) {
                case 0:
                    break;
                case 1:
                    try {
                        Desktop.getDesktop().browse(new URI(latestRelease.htmlUrl));
                    } catch (IOException | URISyntaxException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        menuBar.add(btnNewUpdateAvailable);

        JPanel contentPanel = new JPanel();
        frame.getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new BorderLayout(0, 0));

        JPanel inputPanel = new JPanel();
        inputPanel.setBorder(null);
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.PAGE_AXIS));

        JPanel savePathPanel = new JPanel();
        savePathPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), Messages.getString("panel.saveFileDirectory"),
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        inputPanel.add(savePathPanel);
        savePathPanel.setLayout(new BoxLayout(savePathPanel, BoxLayout.LINE_AXIS));

        saveFileStatus = new JLabel("");
        saveFileStatus.setIcon(Resources.WARNING_ICON);
        savePathPanel.add(saveFileStatus);

        savePathBox = new JTextField();
        savePathBox.setEditable(false);
        savePathPanel.add(savePathBox);
        savePathBox.setColumns(10);

        JButton chooseSavePathButton = new JButton(Messages.getString("button.browse"));
        chooseSavePathButton.addActionListener(e -> {
            if (confirmLoseChanges()) {
                directoryChooser(state.getSaveDir(), s -> state.setSaveDir(s));
                updateSaveDir();
                updateFiles();
            }
        });

        makeBackupButton = new JButton(Messages.getString("button.makeBackup"));
        makeBackupButton.setEnabled(false);
        makeBackupButton.addActionListener(e -> {
            if (state.getSaveStatus() != Status.ERROR) {
                String result = JOptionPane.showInputDialog(frame, Messages.getString("dialog.chooseBackupName"),
                        new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()));
                if (result == null) {
                    return;
                } else {
                    result = result.replaceAll("[:\\\\/*?|<>]", "_");
                    if (state.hasBackup(result)) {
                        int confirmed = JOptionPane.showConfirmDialog(frame,
                                Messages.getString("dialog.backupAlreadyExists", result), Messages.getString("dialog.backupAlreadyExistsTitle"),
                                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                        switch (confirmed) {
                        case JOptionPane.YES_OPTION:
                            break;
                        case JOptionPane.NO_OPTION:
                            return;
                        }
                    }
                    state.makeBackup(result);
                    updateBackupButtons();
                }
            }
        });
        savePathPanel.add(makeBackupButton);

        restoreBackupButton = new JButton(Messages.getString("button.loadBackup"));
        restoreBackupButton.addActionListener(e -> {
            if (state.getSaveStatus() != Status.ERROR && state.hasAnyBackups() && confirmLoseChanges()) {
                String[] backups = state.getBackupNames().toArray(new String[0]);
                Object result = JOptionPane.showInputDialog(frame, Messages.getString("dialog.restoreBackup"), Messages.getString("dialog.restoreBackupTitle"),
                        JOptionPane.OK_CANCEL_OPTION, null, backups, backups[0]);
                if (result != null) {
                    state.restoreBackup((String) result);
                    state.loadFiles();
                    updateFiles();
                }
            }
        });
        restoreBackupButton.setEnabled(false);
        savePathPanel.add(restoreBackupButton);
        savePathPanel.add(chooseSavePathButton);

        contentPanel.add(inputPanel, BorderLayout.NORTH);

        JPanel panel = new JPanel();
        contentPanel.add(panel, BorderLayout.CENTER);
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.addChangeListener(e -> {
            updateSaveStatus();
        });
        panel.add(tabbedPane);

        JPanel buttonPanel = new JPanel();
        frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

        discardChangesButton = new JButton(Messages.getString("button.discardFileChanges"));
        discardChangesButton.addActionListener(e -> {
            Component c = tabbedPane.getSelectedComponent();
            if (c != null) {
                String fileName = ((Tab) c).fileName;
                SaveFile s = state.getSaveFile(fileName);
                Tab t = (Tab) tabbedPane.getSelectedComponent();
                t.area.setText(s.originalContents);
                t.area.setCaretPosition(0);
            }
        });
        buttonPanel.add(discardChangesButton);

        Component horizontalGlue = Box.createHorizontalGlue();
        buttonPanel.add(horizontalGlue);

        errorLabel = new JLabel("Optional Error Text");
        buttonPanel.add(errorLabel);

        Component horizontalStrut = Box.createHorizontalStrut(20);
        buttonPanel.add(horizontalStrut);

        saveStatus = new JLabel("");
        saveStatus.setIcon(Resources.OK_ICON);
        buttonPanel.add(saveStatus);

        saveButton = new JButton(Messages.getString("button.saveAllChanges"));
        saveButton.addActionListener(e -> {
            if (state.canSave()) {
                state.saveChanges();
                state.loadFiles();
                updateFiles();
            }
        });
        buttonPanel.add(saveButton);

        reloadButton = new JButton(Messages.getString("button.reloadAll"));
        reloadButton.addActionListener(e -> {
            if (confirmLoseChanges()) {
                state.loadFiles();
                updateFiles();
            }
        });
        buttonPanel.add(reloadButton);
    }

    protected static final void directoryChooser(String def, Consumer<String> onSuccess) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(def));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.putClientProperty("JFileChooser.appBundleIsTraversable", "always");
        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            onSuccess.accept(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void updateSaveDir() {
        savePathBox.setText(state.getSaveDir());
        saveFileStatus.setIcon(state.getSaveStatus().icon);
        reloadButton.setEnabled(state.getSaveStatus() != Status.ERROR);
        mntmSpreadsheets.setEnabled(state.getSaveStatus() != Status.ERROR);
        updateBackupButtons();
    }

    private void updateBackupButtons() {
        makeBackupButton.setEnabled(state.getSaveStatus() != Status.ERROR);
        restoreBackupButton.setEnabled(state.hasAnyBackups());
    }

    private void updateFiles() {
        tabbedPane.removeAll();
        for (SaveFile f : state.getSaveFiles()) {
            Tab compPanel = new Tab();
            compPanel.fileName = f.name;
            compPanel.setLayout(new BoxLayout(compPanel, BoxLayout.LINE_AXIS));
            RSyntaxTextArea a = new RSyntaxTextArea(f.contents);
            a.setCodeFoldingEnabled(true);
            a.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            compPanel.area = a;
            a.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void removeUpdate(DocumentEvent e) {
                    update(e);
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    update(e);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    update(e);
                }

                private void update(DocumentEvent e) {
                    try {
                        state.changeFile(f.name, e.getDocument().getText(0, e.getDocument().getLength()));
                        updateSaveStatus();
                    } catch (BadLocationException e1) {
                        e1.printStackTrace();
                    }
                    updateFile(compPanel);
                }
            });
            RTextScrollPane sp = new RTextScrollPane(a);
            compPanel.add(sp);
            tabbedPane.addTab((f.changed() ? "*" : "") + f.name, compPanel);
            tabbedPane.setForegroundAt(tabbedPane.indexOfComponent(compPanel), colorFor(f));
        }
    }

    private void updateFile(Tab t) {
        SaveFile f = state.getSaveFile(t.fileName);
        tabbedPane.setTitleAt(tabbedPane.indexOfComponent(t), (f.changed() ? "*" : "") + f.name);
        // tabbedPane.setIconAt(tabbedPane.indexOfComponent(t), iconFor(f));
        tabbedPane.setForegroundAt(tabbedPane.indexOfComponent(t), colorFor(f));
        t.area.getHighlighter().removeAllHighlights();
        if (!f.canSave() && !state.isBusy()) {
            Highlighter.HighlightPainter redPainter = new DefaultHighlighter.DefaultHighlightPainter(
                    new Color(255, 127, 127));
            int[] errorLine = f.getErrorLine();
            try {
                t.area.getHighlighter().addHighlight(errorLine[0], errorLine[1], redPainter);
            } catch (BadLocationException e) {
            }
        }
        discardChangesButton.setEnabled(f.changed());
    }

    private void updateTabStatus(String fname) {
        int totalTabs = tabbedPane.getTabCount();
        for (int i = 0; i < totalTabs; i++) {
            Tab t = (Tab) tabbedPane.getComponentAt(i);
            if (t.fileName.equals(fname)) {
                tabbedPane.setForegroundAt(i, colorFor(state.getSaveFile(fname)));
                updateFile(t);
            }
        }
    }

    private Color colorFor(SaveFile f) {
        if (state.isBusy()) {
            return Color.ORANGE;
        } else if (f.changed()) {
            return f.canSave() ? new Color(50, 131, 50) : Color.RED;
        } else {
            return f.canSave() ? Color.BLACK : Color.ORANGE;
        }
    }

    private void updateSaveStatus() {
        saveStatus.setIcon(
                !state.isBusy() && state.anyChanges() ? (state.canSave() ? Status.OK.icon : Status.ERROR.icon) : null);
        saveButton.setEnabled(state.canSave());
        Component tab = tabbedPane.getSelectedComponent();
        if (tab != null) {
            SaveFile f = state.getSaveFile(((Tab) tab).fileName);
            errorLabel.setText(f.canSave() ? "" : f.errorReason);
            discardChangesButton.setEnabled(f.changed());
        } else {
            errorLabel.setText("");
            discardChangesButton.setEnabled(false);
        }
    }

    protected void attemptExit() {
        if (confirmLoseChanges()) {
            state.save();
            System.exit(0);
        }
    }

    protected boolean confirmLoseChanges() {
        if (state.getNumUnsavedChanges() > 0) {
            int result = JOptionPane.showConfirmDialog(frame,
                    Messages.getString("message.confirmLoseChanges"),
                    Messages.getString("message.confirmLoseChangesTitle"), JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.NO_OPTION || result == JOptionPane.CLOSED_OPTION) {
                return false;
            }
        }
        return true;
    }

    private void checkForUpdates() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Release r = UpdateChecker.getLatestRelease();
                    Version curr = new Version(BuildConfig.VERSION);

                    if (r.version.compareTo(curr) > 0) {
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                latestRelease = r;
                                btnNewUpdateAvailable.setVisible(true);
                                btnNewUpdateAvailable.setEnabled(true);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Update check failed.");
                }
            }

        }).start();
    }

    private class Tab extends JPanel {
        private static final long serialVersionUID = 7066962308849880236L;
        private String fileName;
        private RSyntaxTextArea area;
    }
}
