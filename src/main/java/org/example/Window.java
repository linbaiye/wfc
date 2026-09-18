package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class Window {

    private static final Logger log = LoggerFactory.getLogger(Window.class);
    private JFrame jFrame;

    private BufferedImage combinedImage;

    private Graphics2D graphics2D;

    private JButton yes;
    private JButton no;

    public Window(int w, int h) {
        jFrame = new JFrame("Java 2D Map");
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setSize(w * 32 + 60, h * 24 );
        jFrame.setLocationRelativeTo(null);
        yes = new JButton("y");
        yes.setVisible(true);
        yes.setSize(30, 20);
        no = new JButton("n");
        no.setSize(30, 20);
        no.setVisible(true);
        jFrame.add(yes, BorderLayout.WEST);
        jFrame.add(no, BorderLayout.EAST);
        combinedImage = new BufferedImage(w * 32,h * 24, BufferedImage.TYPE_INT_ARGB);
        graphics2D = combinedImage.createGraphics();
    }

    public void draw(int x, int y, int tileId, int tileNumber) {
        try (InputStream is = Main.class.getResourceAsStream("/tile/" + tileId + ".png")) {
            BufferedImage read = ImageIO.read(is);
            graphics2D.drawImage(read, x * 32, y * 24 , (x+1)*32, (y+1) * 24, tileNumber * 32, 0, (tileNumber + 1) * 32, 24, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void fillBlack(int x, int y) {
        BufferedImage blackImage = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);
        graphics2D.drawImage(blackImage, x * 32, y * 24, (x+1)*32, (y+1) * 24, 0, 0,  32, 24, null);
    }

    public void drawObject(int x, int y, int objId, int objNumber) {
        try (InputStream is = getClass().getResourceAsStream("/obj/" + objId + "/" + objNumber + ".png")) {
            BufferedImage read = ImageIO.read(is);
            graphics2D.drawImage(read, x * 32, y * 24, (x+1)*32, (y+1) * 24, objNumber * 32, 0, (objNumber + 1) * 32, 24, null);
        } catch (IOException e) {
            throw new RuntimeException("Cant open " + objId);
        }
    }

    public void display() {
        JLabel jLabel = new JLabel();
        jLabel.setIcon(new ImageIcon(combinedImage));
        jFrame.add(jLabel);
        jFrame.pack();
        jFrame.setVisible(true);
    }

    public void close() {
        jFrame.setVisible(false);
        jFrame.dispose();
    }

    public void callback(Consumer<String> consumer) {
        yes.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                consumer.accept("yes");
            }
        });
        no.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                consumer.accept("no");
            }
        });
    }

    public void write() {
        try {
            File output_file = new File("map.png");
            ImageIO.write(combinedImage, "png", output_file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
