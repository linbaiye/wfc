package org.example;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class ImageCell extends JPanel {

    private BufferedImage image;

    private final int x;
    private final int y;
    private final int tileNumber;

    public ImageCell(int x, int y, int tileId, int tileNumber) {
        this.x = x;
        this.y = y;
        this.tileNumber = tileNumber;
        try (InputStream is = Main.class.getResourceAsStream("/tile/" + tileId + ".png")) {
            image = ImageIO.read(is);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ImageCell() {
        this.x = 0;
        this.y = 0;
        this.tileNumber = 0;
        try (InputStream is = Main.class.getResourceAsStream("/tile/1.png")) {
            image = ImageIO.read(is);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Always call super to clear previous paint cycles

        if (image == null) return;

        // Cast to Graphics2D to unlock advanced features
        Graphics2D g2d = (Graphics2D) g;

        // Optional: Enable high-quality rendering options (Bilinear interpolation for scaled images)
        //g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // 1. Draw image at actual size at coordinates (10, 10)

        //g2d.drawImage(image, 0, 0, 32, 24, null);

        // 3. Draw a cropped section (e.g., take a 50x50 patch from top-left of source
        //    and scale it to a 100x100 region on screen)
        g2d.drawImage(image,
                x * 32, y * 24, (x+1) * 32, (y+1) * 24,  // Destination top-left (350,10) to bottom-right (450,110)
                32 * tileNumber, 0, 32 * (tileNumber + 1), 24,       // Source top-left (0,0) to bottom-right (50,50)
                null
        );
    }

    public static ImageCell of(int tileId, int number, int x, int y) {
        return new ImageCell(x, y, tileId, number);
    }
}
