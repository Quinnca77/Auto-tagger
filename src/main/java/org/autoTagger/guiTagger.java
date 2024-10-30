package org.autoTagger;

import com.mpatric.mp3agic.NotSupportedException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.autoTagger.Tagger.PATH_TO_SONGS;
import static org.autoTagger.Tagger.getAllFiles;

// TODO document this class
public class guiTagger extends JFrame {
    private JButton tagAllFilesInButton;
    private JPanel MainPanel;
    private JCheckBox fileRename;
    private JTextField songPlaylistURLTextField;
    private JButton downloadAndTagSongButton;
    private JTextPane loadingText;
    private JTextField filePathSong;
    private JTextField vIdThumbnail;
    private JButton addCoverForIndividualButton;
    private static final JTextField artistNameInput = new JTextField(10);
    private static final JTextField songNameInput = new JTextField(10);
    private final Logger logger;
    private final Tagger tagger;
    private final Downloader downloader;

    public guiTagger() {
        setContentPane(MainPanel);
        setTitle("Auto-Tagger by Quinn Caris");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);

        URL iconURL = getClass().getResource("/icon.png");
        if (iconURL != null) {
            ImageIcon icon = new ImageIcon(iconURL);
            this.setIconImage(icon.getImage());
        }

        setVisible(true);
        tagAllFilesInButton.addActionListener(e -> invokeTagAllFiles());
        downloadAndTagSongButton.addActionListener(e -> invokeDownloadAndTag());
        addCoverForIndividualButton.addActionListener(e -> invokeIndividualTag());

        new Logger(this);
        this.logger = Logger.getLogger();
        this.tagger = new Tagger();
        this.downloader = new Downloader();
    }

    private void addCoverForIndividualFile() {
        String filePath = filePathSong.getText().replaceAll("\"", "");
        if (filePath.isEmpty()) {
            JOptionPane.showMessageDialog(guiTagger.this, "Please put in a file path when using this option");
            return;
        }
        String vId = vIdThumbnail.getText();
        if (fileRename.isSelected()) {
            File song = new File(filePath);
            JPanel fields = getFields(song.getName());

            int result = JOptionPane.showConfirmDialog(guiTagger.this, fields, "Rename file", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            switch (result) {
                case JOptionPane.OK_OPTION:
                    if (!(artistNameInput.getText().isEmpty() && songNameInput.getText().isEmpty())) {
                        filePath = PATH_TO_SONGS + artistNameInput.getText() + " - " + songNameInput.getText() + ".mp3";
                        if (!song.renameTo(new File(filePath))) {
                            this.logger.println("Renaming song failed!");
                        }
                    }
                    break;

                case JOptionPane.CANCEL_OPTION:
                    break;
            }
        }
        try {
            tagger.tagIndividualFile(filePath, vId);
            JOptionPane.showMessageDialog(guiTagger.this, "Tagging successful!");
        } catch (IOException | NotSupportedException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
            JOptionPane.showMessageDialog(guiTagger.this, "Something went wrong, please contact the developer.\nError code 01");
            throw new RuntimeException(e);
        }
    }

    private static @NotNull JPanel getFields(String fileName) {
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel title = new JPanel();
        JPanel fields = new JPanel(new GridLayout(2, 2));
        JLabel artistName = new JLabel("Artist name:");
        JLabel songName = new JLabel("Song name:");
        JTextField song = new JTextField(fileName);
        song.setBorder(null);
        song.setOpaque(false);
        song.setEditable(false);

        title.add(song);
        fields.add(artistName);
        fields.add(artistNameInput);
        fields.add(songName);
        fields.add(songNameInput);
        mainPanel.add(title, BorderLayout.NORTH);
        mainPanel.add(fields, BorderLayout.CENTER);
        mainPanel.setPreferredSize(new Dimension(800,60));
        return mainPanel;
    }

    private void tagAllFiles() {
        if (fileRename.isSelected()) {
            File[] songs = getAllFiles();
            for (File song : songs) {
                JPanel fields = getFields(song.getName());

                int result = JOptionPane.showConfirmDialog(guiTagger.this, fields, "Rename file", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                switch (result) {
                    case JOptionPane.OK_OPTION:
                        if (!(artistNameInput.getText().isEmpty() && songNameInput.getText().isEmpty())) {
                            if (!song.renameTo(new File(PATH_TO_SONGS + artistNameInput.getText() + " - " + songNameInput.getText() + ".mp3"))) {
                                this.logger.println("Renaming song failed!");
                            }
                        }
                        break;

                    case JOptionPane.CANCEL_OPTION:
                        break;
                }
            }
        }
        try {
            this.tagger.tagAllFiles();
            JOptionPane.showMessageDialog(guiTagger.this, "Tagging successful!");
        } catch (IOException |
                 InterruptedException | NotSupportedException e) {
            ErrorLogger.runtimeExceptionOccurred(e);
            JOptionPane.showMessageDialog(guiTagger.this, "Something went wrong, please contact the developer.\nError code 02");
            throw new RuntimeException(e);
        } catch (NoSongFoundException e) {
            JOptionPane.showMessageDialog(guiTagger.this, "No songs found in Downloads folder!");
        } catch (VideoIdEmptyException e) {
            JOptionPane.showMessageDialog(guiTagger.this, "No song online found that corresponds with these fields!");
        }
    }

    private void invokeTagAllFiles() {
        new AbstractWorker(this) {
            @Override
            protected void beginTask() {
                displayText("Starting tagging...");
            }
            @Override
            protected void executeTask() {
                tagAllFiles();
            }
            @Override
            protected void taskCompleted() {
                displayText("Tagging complete!");
            }
        }.execute();
    }

    private void invokeDownloadAndTag() {
        new AbstractWorker(this) {
            @Override
            protected void beginTask() {
                displayText("Starting download...\n");
            }
            @Override
            protected void executeTask() {
                try {
                    downloader.downloadSongs(songPlaylistURLTextField.getText());
                } catch (IOException | InterruptedException e) {
                    JOptionPane.showMessageDialog(guiTagger.this,
                            "Something went wrong, please contact the developer.\nError code 03");
                }
            }
            @Override
            protected void taskCompleted() {
                displayText("Download complete. Starting tagging...\n");
                invokeTagAllFiles();
            }
        }.execute();
    }

    private void invokeIndividualTag() {
        new AbstractWorker(this) {
            @Override
            protected void beginTask() {
                displayText("Starting tagging...");
            }
            @Override
            protected void executeTask() {
                addCoverForIndividualFile();
            }
            @Override
            protected void taskCompleted() {
                displayText("Tagging complete!");
            }
        }.execute();
    }

    public void displayText(String string) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = loadingText.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), string, null);
            } catch (BadLocationException exc) {
                JOptionPane.showMessageDialog(guiTagger.this, "Something went wrong, please contact the developer.\nError code 04");
            }
        });
    }
}