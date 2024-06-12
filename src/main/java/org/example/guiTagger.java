package org.example;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import static org.example.Tagger.PATH_TO_SONGS;
import static org.example.Tagger.getAllFiles;

public class guiTagger extends JFrame {
    private JButton tagAllFilesInButton;
    private JPanel MainPanel;
    private JCheckBox fileRename;
    private JTextField songPlaylistURLTextField;
    private JButton downloadAndTagSongButton;
    private JTextPane loadingText;
    private static final JTextField artistNameInput = new JTextField(10);
    private static final JTextField songNameInput = new JTextField(10);
    private final Tagger tagger;
    private final Downloader downloader;

    // TODO: bring back individual tagging (turned out more useful than thought before)

    public guiTagger() {
        setContentPane(MainPanel);
        setTitle("Auto-Tagger by Quinn Caris");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);
        setVisible(true);
        tagAllFilesInButton.addActionListener(e -> invokeTagAllFiles());
        downloadAndTagSongButton.addActionListener(e -> downloadAndTag());

        new Logger(this);
        this.tagger = new Tagger();
        this.downloader = new Downloader();
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

    private class tagAllFilesWorker extends SwingWorker<Void, Void> {
        @Override
        protected Void doInBackground() {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            displayText("Starting tagging...");
            tagAllFiles();
            return null;
        }

        @Override
        protected void done() {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            displayText("Tagging complete!");
        }
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
                            song.renameTo(new File(PATH_TO_SONGS + artistNameInput.getText() + " - " + songNameInput.getText() + ".mp3"));
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
        } catch (InvalidDataException | UnsupportedTagException | IOException | URISyntaxException |
                 InterruptedException | NotSupportedException ex) {
            JOptionPane.showMessageDialog(guiTagger.this, "Something went wrong, sowwy");
            throw new RuntimeException(ex);
        } catch (NoSongFoundException exc) {
            JOptionPane.showMessageDialog(guiTagger.this, "No songs found in Downloads folder!");
        } catch (VideoIdEmptyException exce) {
            JOptionPane.showMessageDialog(guiTagger.this, "No song online found that corresponds with these fields!");
        }
    }

    private void invokeTagAllFiles() {
        new tagAllFilesWorker().execute();
    }

    private void downloadAndTag() {
        new DownloadAndTagWorker().execute();
    }

    private class DownloadAndTagWorker extends SwingWorker<Void, Void> {
        @Override
        protected Void doInBackground() {
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                displayText("Starting download...\n");
                downloader.downloadSongs(songPlaylistURLTextField.getText());
                displayText("Download complete. Starting tagging...\n");
                tagAllFiles();
            } catch (IOException | InterruptedException e) {
                JOptionPane.showMessageDialog(guiTagger.this, "Something went wrong, please contact the developer");
            }
            return null;
        }

        @Override
        protected void done() {
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                get();
                displayText("Tagging complete!\n");
            } catch (InterruptedException | ExecutionException e) {
                JOptionPane.showMessageDialog(guiTagger.this, "Something went wrong, please contact the developer");
            }
        }
    }

    public void displayText(String string) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = loadingText.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), string, null);
            } catch (BadLocationException exc) {
                JOptionPane.showMessageDialog(guiTagger.this, "Something went wrong, please contact the developer");
            }
        });
    }
}
