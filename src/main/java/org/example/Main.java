package org.example;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    /*private static JFrame create() {
        JFrame frame = new JFrame("Java 2D drawImage Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(640, 480);
        frame.setLocationRelativeTo(null);
        return frame;
    }*/

    static List<TileCell> buildCells() {

        RealmMap realmMap = RealmMap.read("start", "test").orElseThrow(RuntimeException::new);
        TileCell[][] cells = new TileCell[realmMap.width][realmMap.height];
        List<TileCell> cellList = new LinkedList<>();
        for (int i = 0; i < realmMap.width; i++) {
            for (int j = 0; j < realmMap.height; j++) {
                RealmMap.MapCell mapCell = realmMap.get(i, j);
                TileCell tileCell = cellList.stream().filter(c -> c.has(mapCell.TileId, mapCell.TileNumber)).findFirst()
                        .orElseGet(() -> {
                            TileCell c = new TileCell(mapCell.TileId, mapCell.TileNumber, mapCell.isMovable());
                            cellList.add(c);
                            return c;
                        });
                cells[i][j] = tileCell;
            }
        }
        for (int i = 0; i < realmMap.width; i++) {
            for (int j = 0; j < realmMap.height; j++) {
                TileCell tileCell = cells[i][j];
                if (j + 1 < realmMap.height)
                    tileCell.addNeibhour(cells[i][j+1], Direction.Down);
                if (i + 1 < realmMap.width)
                    tileCell.addNeibhour(cells[i+1][j], Direction.Right);
                if (i - 1 >= 0)
                    tileCell.addNeibhour(cells[i-1][j], Direction.Left);
                if (j - 1 >= 0)
                    tileCell.addNeibhour(cells[i][j-1], Direction.Up);
            }
        }
        return cellList;
    }

    private record Coordinate(int x, int y) {

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Coordinate that = (Coordinate) o;
            return x == that.x && y == that.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        public Coordinate left() {
            return new Coordinate(x -1, y);
        }

        public Coordinate right() {
            return new Coordinate(x + 1, y);
        }

        public Coordinate up() {
            return new Coordinate(x, y -1);
        }
        public Coordinate down() {
            return new Coordinate(x, y + 1);
        }
    }


    static Set<Coordinate> visisted;


    static void generate(Window window, TileCell toTile, int x, int y) {
        if (x < 0 || x > 20)
            return;
        if (y < 0 || y > 20)
            return;
        if (visisted.contains(new Coordinate(x, y)))
            return;
        window.draw(x, y, toTile.id, toTile.number);
        visisted.add(new Coordinate(x, y));
        toTile.randomChoice(Direction.Up).ifPresent(c -> generate(window, c, x, y-1));
        toTile.randomChoice(Direction.Down).ifPresent(c -> generate(window, c, x, y+1));
        toTile.randomChoice(Direction.Left).ifPresent(c -> generate(window, c, x-1, y));
        toTile.randomChoice(Direction.Right).ifPresent(c -> generate(window, c, x+1, y));
    }


    static Optional<TileCell> findBest(TileCell left, TileCell right, TileCell up, TileCell down) {
        List<TileCell> leftOptions = left != null ? left.candidates(Direction.Right) : Collections.emptyList();
        List<TileCell> rightOptions = right != null ? right.candidates(Direction.Left) : Collections.emptyList();
        List<TileCell> upOptions = up != null ? up.candidates(Direction.Down) : Collections.emptyList();
        List<TileCell> downOptions = down != null ? down.candidates(Direction.Up) : Collections.emptyList();
        Map<TileCell, Integer> counter = new HashMap<>();
        Consumer<TileCell> count =  t -> {
            if (!counter.containsKey(t))
                counter.put(t, 1);
            else
                counter.put(t, counter.get(t) + 1);
        };
        leftOptions.forEach(count);
        rightOptions.forEach(count);
        upOptions.forEach(count);
        downOptions.forEach(count);
        int max = -1;
        TileCell ret = null;
        for (TileCell tileCell : counter.keySet()) {
            Integer i = counter.get(tileCell);
            if (max < i) {
                ret = tileCell;
                max = i;
            }
        }
        return Optional.ofNullable(ret);
    }

    static void generateBFS(Window window, TileCell origin, int x, int y) {
        final Deque<Coordinate> deque = new ArrayDeque<>();
        Map<Coordinate, TileCell> drawn = new HashMap<>();

        window.draw(x, y, origin.id, origin.number);

        Consumer<Coordinate> pushNeighbours = (c) -> {
            if (c.x() - 1 > 0) {
                deque.add(c.left());
            }
            if (c.x + 1 < 20) {
                deque.add(c.right());
            }
            if (c.y - 1 > 0) {
                deque.add(c.up());
            }
            if (c.y + 1 < 20) {
                deque.add(c.down());
            }
        };
        Coordinate c = new Coordinate(x, y);

        drawn.put(c, origin);
        visisted.add(c);
        pushNeighbours.accept(c);

        while (!deque.isEmpty()) {
            Coordinate coor = deque.poll();
            if (visisted.contains(coor))
                continue;
            findBest(drawn.get(coor.left()), drawn.get(coor.right()), drawn.get(coor.up()), drawn.get(coor.down()))
                    .ifPresent(t ->  {
                        window.draw(coor.x, coor.y, t.id, t.number);
                        drawn.put(coor, t);
                    });
            visisted.add(coor);
            pushNeighbours.accept(coor);
        }
    }


    static void generate() {
        Window window = new Window(20, 20);
        List<TileCell> cellList = buildCells();
        TileCell tileCell = cellList.stream()
                .filter(TileCell::isMovable)
                .max(Comparator.comparingInt(TileCell::maxPossiblility))
                .orElseThrow(RuntimeException::new);
        visisted = new HashSet<>();
        //generate(window, tileCell, 10, 10);
        generateBFS(window, tileCell, 10, 10);
        //window.draw(10, 10, tileCell.id, tileCell.number);
        window.display();
    }


    static void collapseMap() {
        List<TileCell> cellList = buildCells();
        WFC wfc = new WFC(30, 30, cellList);
        wfc.run();
        wfc.draw();
    }

    static void collapseDemo() {
        List<TileCell> tileCells = new ArrayList<>(5);
        var t1 = new TileCell(1, 0, true);
        var t2 = new TileCell(2, 0, true);
        var t3 = new TileCell(3, 0, true);
        var t4 = new TileCell(4, 0, true);
        var t5 = new TileCell(5, 0, true);
        tileCells.add(t1);
        tileCells.add(t2);
        tileCells.add(t3);
        tileCells.add(t4);
        tileCells.add(t5);

        t1.addNeibhour(Direction.Up, t1, t5);
        t1.addNeibhour(Direction.Right, t1, t4);
        t1.addNeibhour(Direction.Down, t1, t2);
        t1.addNeibhour(Direction.Left, t1, t3);

        t2.addNeibhour(Direction.Up, t1, t5);
        t2.addNeibhour(Direction.Right, t2, t3, t5);
        t2.addNeibhour(Direction.Down, t3, t4, t5);
        t2.addNeibhour(Direction.Left, t2, t4, t5);

        t3.addNeibhour(Direction.Up, t2, t3, t4);
        t3.addNeibhour(Direction.Right, t1, t4);
        t3.addNeibhour(Direction.Down, t3, t4, t5);
        t3.addNeibhour(Direction.Left, t2, t4, t5);

        t4.addNeibhour(Direction.Up, t2, t3, t4);
        t4.addNeibhour(Direction.Right, t2, t3, t5);
        t4.addNeibhour(Direction.Down, t3, t4, t5);
        t4.addNeibhour(Direction.Left, t1, t3);

        t5.addNeibhour(Direction.Up, t2, t3, t4);
        t5.addNeibhour(Direction.Right, t2, t3, t5);
        t5.addNeibhour(Direction.Down, t1, t2);
        t5.addNeibhour(Direction.Left, t2, t4, t5);
        WFC wfc = new WFC(20, 20, tileCells);
        wfc.run();
        wfc.drawDemo();
    }

    static void main() throws Exception {
        //collapseDemo();
        collapseMap();
        //generate();
        /*for (int i = 0, x = 100; i < 20; i++) {
            for (int j = 0, y = 100; j < 20; j++) {
                RealmMap.MapCell mapCell = realmMap.get(x + i, y + j);
                window.draw(i, j, mapCell.TileId, mapCell.TileNumber);
            }
        }*/
        //window.display();
        /*var frame = create();
        frame.add(ImageCell.of(1, 0, 0, 0));
        frame.add(ImageCell.of(1, 1, 1, 0));
        frame.setVisible(true);
        try (InputStream is = Main.class.getResourceAsStream("/tile/1.png")) {
            BufferedImage read = ImageIO.read(is);
            var combinedImage = new BufferedImage(640,480, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = combinedImage.createGraphics();
            g.drawImage(read, 0, 0, 32, 24, 0, 0, 32, 24, null);
            g.drawImage(read, 32, 0, 64, 24, 32, 0, 64, 24, null);
            JLabel jLabel = new JLabel();
            jLabel.setIcon(new ImageIcon(combinedImage));
            frame.add(jLabel);
        }
        frame.pack();
        frame.setVisible(true);*/
    }
}
