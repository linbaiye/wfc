package org.example;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class Window {

    private JFrame jFrame;

    private BufferedImage combinedImage;

    private Graphics2D graphics2D;

    public Window(int w, int h) {
        jFrame = new JFrame("Java 2D Map");
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setSize(w * 32, h * 24);
        jFrame.setLocationRelativeTo(null);
        combinedImage = new BufferedImage(w * 32,h * 24, BufferedImage.TYPE_INT_ARGB);
        graphics2D = combinedImage.createGraphics();
    }

    public void draw(int x, int y, int tileId, int tileNumber) {
        try (InputStream is = Main.class.getResourceAsStream("/tile/" + tileId + ".png")) {
            BufferedImage read = ImageIO.read(is);
            graphics2D.drawImage(read, x * 32, y * 24, (x+1)*32, (y+1) * 24, tileNumber * 32, 0, (tileNumber + 1) * 32, 24, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void fillBlack(int x, int y) {
        BufferedImage blackImage = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);
        graphics2D.drawImage(blackImage, x * 32, y * 24, (x+1)*32, (y+1) * 24, 0, 0,  32, 24, null);
    }

    public void display() {
        JLabel jLabel = new JLabel();
        jLabel.setIcon(new ImageIcon(combinedImage));
        jFrame.add(jLabel);
        jFrame.pack();
        jFrame.setVisible(true);
    }
}
