package com.gargara.launcher;

import javax.swing.*;
import java.awt.*;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DebugConsole extends JFrame {
    
    private static DebugConsole instance;
    private JTextArea textArea;
    
    public static void initialize() {
        if (instance == null) {
            instance = new DebugConsole();
            redirectSystemStreams();
        }
    }
    
    public static void showConsole() {
        if (instance == null) {
            initialize();
        }
        instance.setVisible(true);
        instance.setState(Frame.NORMAL);
        instance.toFront();
        instance.requestFocus();
    }
    
    private DebugConsole() {
        setTitle("Gargara Hata Ayıklama (Debug)");
        setSize(800, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setBackground(Color.BLACK);
        textArea.setForeground(Color.GREEN);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("Temizle");
        clearButton.addActionListener(e -> textArea.setText(""));
        bottomPanel.add(clearButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void appendText(String text) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(text);
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }
    
    private static void redirectSystemStreams() {
        PrintStream out = new PrintStream(new CustomOutputStream(false), true);
        PrintStream err = new PrintStream(new CustomOutputStream(true), true);
        System.setOut(out);
        System.setErr(err);
    }
    
    private static class CustomOutputStream extends java.io.OutputStream {
        private boolean isError;
        public CustomOutputStream(boolean isError) {
            this.isError = isError;
        }
        
        @Override
        public void write(int b) throws IOException {
            if (instance != null) {
                instance.appendText(String.valueOf((char) b));
            }
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (instance != null) {
                instance.appendText(new String(b, off, len));
            }
        }
    }
}
