package org.example;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class DemoWindow {

    private JFrame jFrame;

    private BufferedImage combinedImage;

    private Graphics2D graphics2D;

    public DemoWindow(int w, int h) {
        jFrame = new JFrame("Java 2D Map");
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setSize(w * 50, h * 50);
        jFrame.setLocationRelativeTo(null);
        combinedImage = new BufferedImage(w * 50,h * 50, BufferedImage.TYPE_INT_ARGB);
        graphics2D = combinedImage.createGraphics();
    }

    public void draw(int x, int y, int tileId, int tileNumber) {
        try (InputStream is = Main.class.getResourceAsStream("/demo/" + tileId + ".png")) {
            BufferedImage read = ImageIO.read(is);
            graphics2D.drawImage(read, x * 50, y * 50, (x+1)*50, (y+1) * 50, tileNumber * 50, 0, (tileNumber + 1) * 50, 50, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void fillBlack(int x, int y) {
        BufferedImage blackImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        graphics2D.drawImage(blackImage, x * 50, y * 50, (x+1)*50, (y+1) * 50, 0, 0,  50, 50, null);
    }

    public void display() {
        JLabel jLabel = new JLabel();
        jLabel.setIcon(new ImageIcon(combinedImage));
        jFrame.add(jLabel);
        jFrame.pack();
        jFrame.setVisible(true);
    }
}
