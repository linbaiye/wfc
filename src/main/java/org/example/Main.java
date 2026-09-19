package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    /*private static JFrame create() {
        JFrame frame = new JFrame("Java 2D drawImage Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(640, 480);
        frame.setLocationRelativeTo(null);
        return frame;
    }*/
    static RealmMap REALM_MAP = RealmMap.read("start", "test").orElseThrow(RuntimeException::new);


    static List<TileCell> buildAllCells() {
        RealmMap realmMap = RealmMap.read("start", "test").orElseThrow(RuntimeException::new);
        TileCell[][] cells = new TileCell[realmMap.width][realmMap.height];
        TileCell[][] overCells = new TileCell[realmMap.width][realmMap.height];
        List<TileCell> tiles = new LinkedList<>();
        List<TileCell> overTiles = new LinkedList<>();
        for (int i = 0; i < realmMap.width; i++) {
            for (int j = 0; j < realmMap.height; j++) {
                RealmMap.MapCell mapCell = realmMap.get(i, j);
                TileCell tileCell = tiles.stream().filter(c -> c.has(mapCell.TileId, mapCell.TileNumber)).findFirst()
                        .orElseGet(() -> {
                            TileCell c = new TileCell(mapCell.TileId, mapCell.TileNumber, mapCell.isMovable());
                            tiles.add(c);
                            return c;
                        });
                cells[i][j] = tileCell;
                if (mapCell.TileOverId < 1)
                    continue;
                TileCell overTileCell = overTiles.stream().filter(c -> c.has(mapCell.TileOverId, mapCell.TileOverNumber)).findFirst()
                        .orElseGet(() -> {
                            TileCell c = new TileCell(mapCell.TileOverId, mapCell.TileOverNumber, mapCell.isMovable());
                            overTiles.add(c);
                            return c;
                        });
                overCells[i][j] = overTileCell;
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

                if (j + 1 < realmMap.height) {
                    TileCell overcell = overCells[i][j + 1];
                    if (overcell != null) {
                        tileCell.addNeibhour(overcell, Direction.Down);
                        overcell.addNeibhour(tileCell, Direction.Up);
                    }
                }
                if (i + 1 < realmMap.width) {
                    TileCell overcell = overCells[i+ 1][j];
                    if (overcell != null) {
                        tileCell.addNeibhour(overcell, Direction.Right);
                        overcell.addNeibhour(tileCell, Direction.Left);
                    }
                }
                if (i - 1 >= 0) {
                    TileCell overcell = overCells[i -1][j];
                    if (overcell != null) {
                        tileCell.addNeibhour(overcell, Direction.Left);
                        overcell.addNeibhour(tileCell, Direction.Right);
                    }
                }
                if (j - 1 >= 0) {
                    TileCell overcell = overCells[i][j - 1];
                    if (overcell != null) {
                        tileCell.addNeibhour(overcell, Direction.Up);
                        overcell.addNeibhour(tileCell, Direction.Down);
                    }
                }
            }
        }
        tiles.addAll(overTiles);
        return tiles;
    }

    static List<TileCell> findEdgeCells() {
        RealmMap realmMap = RealmMap.read("start", "test").orElseThrow(RuntimeException::new);
        for (int i = 0; i < realmMap.width; i++) {
            for (int j = 0; j < realmMap.height; j++) {
                RealmMap.MapCell mapCell = realmMap.get(i, j);
                if (mapCell.isMovable()) {

                }
            }
        }
        return null;
    }


    static void initLeft(List<TileCell> cellList) {
        for (int i = 8; i <= 17; i++) {
            try {
                BufferedImage image = ImageIO.read(Main.class.getResourceAsStream("/tile/" + i + ".png"));
                int w = image.getWidth() / 32;
                int h = w / 4;
                TileCell[][] cells = new TileCell[h][4];
                int n = 0;
                log.info("Height {}.", h);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < 4 ; x++) {
                        cells[y][x] = new TileCell(i, n++, true);
                        cellList.add(cells[y][x]);
                    }
                }
                addRulesByGrid(cells);
            } catch (Exception e) {
                log.error("Exception at {}.", i, e);
                break;
            }
        }
    }


    static TileCell find(List<TileCell> cells, int i, int n) {
        for (TileCell tileCell : cells) {
            if (tileCell.has(i, n)) {
                return tileCell;
            }
        }
        return null;
    }

    static void addLeftRightRules(List<TileCell> cellList, int i) throws IOException {
        BufferedImage image = ImageIO.read(Main.class.getResourceAsStream("/tile/" + i + ".png"));
        int w = image.getWidth() / 32;
        for (int n  = 0; n < w - 1; n++) {
            TileCell cell = find(cellList, i, n);
            if (cell == null) {
                cell = new TileCell(i, n, true);
                cellList.add(cell);
            }
            TileCell right = find(cellList, i, n+1);
            if (right == null) {
                right = new TileCell(i, n + 1, true);
                cellList.add(right);
            }
            cell.addNeibhour(right, Direction.Right);
            right.addNeibhour(cell, Direction.Left);
        }
    }

    static void addPerfectRule(List<TileCell> cellList, int i, int div) throws Exception {
        BufferedImage image = ImageIO.read(Main.class.getResourceAsStream("/tile/" + i + ".png"));
        int w = image.getWidth() / 32;
        int h = w / div;
        TileCell[][] cells = new TileCell[h][div];
        int n = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < div ; x++) {
                TileCell target = find(cellList, i, n);
                if (target == null) {
                    cells[y][x] = new TileCell(i, n, true);
                    cellList.add(cells[y][x]);
                } else {
                    cells[y][x] = target;
                }
                n++;
            }
        }
        addRulesByGrid(cells);
    }

    static void addHorizontalRules(List<TileCell> cellList, Set<Integer> ids, int div) {
        ids.forEach(i -> {
            try {
                addHorizontalRule(cellList, i, div);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    static void addHorizontalRule(List<TileCell> cellList, int i, int div) throws Exception {
        BufferedImage image = ImageIO.read(Main.class.getResourceAsStream("/tile/" + i + ".png"));
        int w = image.getWidth() / 32;
        int h = w / div;
        TileCell[][] cells = new TileCell[h][div];
        int n = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < div; x++) {
                TileCell target = find(cellList, i, n);
                if (target == null) {
                    cells[y][x] = new TileCell(i, n, true);
                    cellList.add(cells[y][x]);
                } else {
                    cells[y][x] = target;
                }
                n++;
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < div; x++) {
                TileCell tileCell = cells[y][x];
                if (x + 1 < cells[0].length)
                    tileCell.addNeibhour(cells[y][x+1], Direction.Right);
                if (x - 1 >= 0)
                    tileCell.addNeibhour(cells[y][x-1], Direction.Left);
            }
        }
    }


    static void initFixedRule(List<TileCell> cellList) {
        for (int i = 1; i <= 7; i++) {
            try {
                BufferedImage image = ImageIO.read(Main.class.getResourceAsStream("/tile/" + i + ".png"));
                int w = image.getWidth() / 32;
                for (int n  = 0; n < w - 1; n++) {
                    TileCell cell = new TileCell(i, n, true);
                    TileCell right = new TileCell(i, n + 1, true);
                    cell.addNeibhour(right, Direction.Right);
                    right.addNeibhour(cell, Direction.Left);
                    cellList.add(cell);
                    cellList.add(right);
                }
            } catch (Exception e) {
                log.error("Exception ", e);
                return;
            }
        }
        initLeft(cellList);
    }

    static void addRulesByGrid(TileCell[][] cells) {
        for (int y = 0; y < cells.length; y++) {
            for (int x = 0; x < cells[0].length; x++) {
                TileCell tileCell = cells[y][x];
                if (x + 1 < cells[0].length)
                    tileCell.addNeibhour(cells[y][x+1], Direction.Right);
                if (x - 1 >= 0)
                    tileCell.addNeibhour(cells[y][x-1], Direction.Left);
                if (y + 1 < cells.length)
                    tileCell.addNeibhour(cells[y+1][x], Direction.Down);
                if (y - 1 >= 0)
                    tileCell.addNeibhour(cells[y-1][x], Direction.Up);
            }
        }
    }


    static void addStaticRules(List<TileCell> cells) {
        for (int i = 1; i < PERFECT.size(); i++) {
            Set<Integer> integers = PERFECT.get(i);
            if (integers.isEmpty())
                continue;
            addPerfectRules(cells, integers, i);
        }

        addSingleLineRules(cells, SINGLE_LINE);

        addPatchRules(cells, FOUR_CELL_PATCH, 4, 4);
    }

    static List<TileCell> buildCells() {
        RealmMap realmMap = RealmMap.read("start", "test").orElseThrow(RuntimeException::new);
        TileCell[][] cells = new TileCell[realmMap.width][realmMap.height];
        List<TileCell> cellList = new LinkedList<>();
        addStaticRules(cellList);
        //initFixedRule(cellList);
        for (int i = 0; i < realmMap.width; i++) {
            for (int j = 0; j < realmMap.height; j++) {
                RealmMap.MapCell mapCell = realmMap.get(i, j);
                TileCell tileCell = cellList.stream().filter(c -> c.has(mapCell.TileId, mapCell.TileNumber)).findFirst()
                        .orElseGet(() -> {
                            TileCell c = new TileCell(mapCell.TileId, mapCell.TileNumber, mapCell.isMovable());
                            cellList.add(c);
                            return c;
                        });
                tileCell.changeMove(mapCell.isMovable());
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
        //fix(cellList);
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

    static void hasIntersection() {
        RealmMap realmMap = RealmMap.read("start", "test").orElseThrow(RuntimeException::new);
        Set<Integer> tiles = new HashSet<>();
        Set<Integer> overTiles = new HashSet<>();
        for (int i = 0; i < realmMap.width; i++) {
            for (int j = 0; j < realmMap.height; j++) {
                RealmMap.MapCell mapCell = realmMap.get(i, j);
                if (mapCell.TileId > 0)
                    tiles.add((int)mapCell.TileId);
                if (mapCell.TileOverId > 0)
                    overTiles.add((int)mapCell.TileOverId);
            }
        }
        tiles.stream().filter(overTiles::contains).forEach(e -> {
            log.info("Tile {} intersected.", e);
        });
        overTiles.stream().filter(tiles::contains).forEach(e -> {
            log.info("Overtile {} intersected.", e);
        });
        log.info("Clear.");
    }

    static void checkIntersection(int n) {
        RealmMap realmMap = RealmMap.read("start", "test").orElseThrow(RuntimeException::new);
        for (int i = 0; i < realmMap.width; i++) {
            for (int j = 0; j < realmMap.height; j++) {
                RealmMap.MapCell mapCell = realmMap.get(i, j);
                if (mapCell.TileId == n) {
                    //log.info("Tile number {} at {} {}", mapCell.TileNumber, i, j);
                }
                if (mapCell.TileOverId == n) {
                    log.info("Tile over number {} at {} {}", mapCell.TileOverNumber, i, j);
                }
            }
        }
    }


    static List<TileCell> staticCells() {
        List<TileCell> cellList = new ArrayList<>();
        addStaticRules(cellList);
        return cellList;
    }

    static void collapseMap() throws IOException {
        List<TileCell> allCells = buildAllCells();
        List<TileCell> cellList = buildCells();
        List<TileCell> staticList = staticCells();
        while (true) {
            WFC wfc = new WFC(50, 50, cellList);
            wfc.run();
            if (wfc.checkBlacks() <= 0.10f) {
                var w = wfc.draw();
                //wfc.fillBlack(w, allCells);
                System.in.read();
                w.close();
            }
            /*if (!wfc.hasContradiction()) {
                var w = wfc.draw();
                w.write();
                break;
            }*/
        }
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


    static void fixRight(List<TileCell> cells, int id) {
        List<TileCell> list = cells.stream().filter(e -> e.id == id).toList();
        Map<Index, TileCell> maps = new HashMap<>();
        AtomicInteger max = new AtomicInteger(Integer.MIN_VALUE);
        list.forEach(t -> {
            Index key = new Index(t.id, t.number);
            if (max.get() < t.number)
                max.set(t.number);
            if (maps.containsKey(key)) {
                log.warn("Duplicated {}.", t);
                return;
            }
            maps.put(key, t);
        });
        if (id == 6) {
            System.out.println("6");
        }
        for (int i = 0; i < max.get() - 1; i++) {
            log.info("Fix number {} {}.", id, i);
            TileCell right = maps.get(new Index(id, i + 1));
            if (right == null)
                continue;
            TileCell tileCell = maps.get(new Index(id, i));
            if (tileCell == null)
                continue;
            tileCell.addNeibhour(right, Direction.Right);
            right.addNeibhour(tileCell, Direction.Left);
        }
    }

    static void fix(List<TileCell> cells) {
        for (int i = 1; i <= 7; i++) {
            fixRight(cells, i);
        }
    }

    static void dumpAdj() {
        List<TileCell> tileCells = buildCells();
        tileCells.stream().filter(t -> t.id == 1)
                .sorted(Comparator.comparingInt(t -> t.number))
                .toList()
                //.reversed()
                .forEach(e -> {
                    log.info("{} -> {}.", e.number, e.candidates(Direction.Right));
                });
    }

    static void dumpAdj(List<TileCell>cells, int id) {
        cells.stream().filter(t -> t.id == id)
                .sorted(Comparator.comparingInt(t -> t.number))
                .toList()
                //.reversed()
                .forEach(e -> {
                    log.info("{} -> up: {}.", e.number, e.candidates(Direction.Up));
                    log.info("{} -> right: {}.", e.number, e.candidates(Direction.Right));
                    log.info("{} -> down: {}.", e.number, e.candidates(Direction.Down));
                    log.info("{} -> left: {}.", e.number, e.candidates(Direction.Left));
                });
    }


    static Window draw(int id, int length) throws Exception {
        InputStream resourceAsStream = Main.class.getResourceAsStream("/tile/" + id + ".png");
        if (resourceAsStream == null)
            return null;
        BufferedImage image = ImageIO.read(resourceAsStream);
        int width = image.getWidth()/ 32;
        if (width % length != 0) {
            log.error("Bad image ");
            return null;
        }
        Window window = new Window(length, width / length);
        int count = 0;
        for (int y = 0; y < width / length; y++) {
            for (int x = 0; x < length; x++) {
                window.draw(x, y, id, count++);
            }
        }
        window.display();
        return window;
    }

    static void addPerfectRules(List<TileCell> cells, Set<Integer> ids, int div) {
        ids.forEach(i -> {
            try {
                addPerfectRule(cells, i, div);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    static void addPatchRules(List<TileCell> cellList, Set<Integer> ids, int div, int r) {
        ids.forEach(i  -> {
            try {
                addPatchRule(cellList, div, i, r);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    static void addPatchRule(List<TileCell> cellList, int div, int i, int r) throws Exception{
        BufferedImage image = ImageIO.read(Main.class.getResourceAsStream("/tile/" + i + ".png"));
        int w = image.getWidth() / 32;
        int h = w / div;
        for (int k = 0; k < (h / r); k++) {
            TileCell[][] cells = new TileCell[r][div];
            int n = 0;
            for (int y = 0; y < r; y++) {
                for (int x = 0; x < div; x++) {
                    TileCell target = find(cellList, i, n);
                    if (target == null) {
                        cells[y][x] = new TileCell(i, n, true);
                        cellList.add(cells[y][x]);
                    } else {
                        cells[y][x] = target;
                    }
                    n++;
                }
            }
            addRulesByGrid(cells);
        }
    }



    static void addSingleLineRules(List<TileCell> cells, Set<Integer> ids) {
        ids.forEach(i -> {
            try {
                addLeftRightRules(cells, i);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /*
            1: 54, 901, 902, 903, 904, 905, 906, 907, 908, 909, 910, 911, 915, 916, 917, 918, 919, 920, 921, 922, 923, 924, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012, 1013, 1014, 1015, 1016, 1017, 1018, 1019, 1020, 1021, 1022, 1023
            2: 1, 3, 7, 53, 105, 108, 381, 382, 401, 402, 403, 404, 421, 422, 713, 761, 912
            3: 46, 47, 102, 103, 106, 106, 109, 122
            4: 2, 4, 5, 6, 9, 10, 13, 14, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 88, 90, 91, 92, 94, 98, 99, 100, 201, 202, 203, 204, 205, 206, 207, 208, 209, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 241, 242, 243, 244, 245, 246, 247, 248, 251, 252, 253, 254, 261, 262, 263, 264, 265, 271, 272, 273, 274, 275, 276, 277, 278, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360, 361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372, 383, 384, 385, 386, 387, 388, 389, 390, 391,  405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 418, 419, 423, 424, 425, 426, 427, 428, 711, 712, 713, 761
            5: 48, 120, 121
            6: 3, 49, 50, 51
            7: 44, 123, 715, 716, 717, 718
            8: 45
            9: 760
    partial ----------------
            8, 11, 12, 15, 16, 17
    notsure ----------------
            19, 20, 111,*/
    static List<Set<Integer>> PERFECT = Arrays.asList(
            new HashSet<>(), // 0 occupied.
            new HashSet<>(Arrays.asList(54, 901, 902, 903, 904, 905, 906, 907, 908, 909, 910, 911, 915, 916, 917, 918, 919, 920, 921, 922, 923, 924, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012, 1013, 1014, 1015, 1016, 1017, 1018, 1019, 1020, 1021, 1022, 1023)),
            new HashSet<>(Arrays.asList(1, 3, 7, 53, 105, 108, 381, 382, 401, 402, 403, 404, 421, 422, 713, 761, 912)),
            new HashSet<>(Arrays.asList(46, 47, 102, 103, 106, 106, 109, 122)),
            new HashSet<>(Arrays.asList(2, 4, 5, 6, 9, 10, 13, 14, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 88, 90, 91, 92, 94, 98, 99, 100, 201, 202, 203, 204, 205, 206, 207, 208, 209, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 241, 242, 243, 244, 245, 246, 247, 248, 251, 252, 253, 254, 261, 262, 263, 264, 265, 271, 272, 273, 274, 275, 276, 277, 278, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360, 361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372, 383, 384, 385, 386, 387, 388, 389, 390, 391,  405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 418, 419, 423, 424, 425, 426, 427, 428, 711, 712, 713, 761)),
            new HashSet<>(Arrays.asList(48, 120, 121)),
            new HashSet<>(Arrays.asList(3, 49, 50, 51)),
            new HashSet<>(Arrays.asList(44, 123, 715, 716, 717, 718)),
            new HashSet<>(Arrays.asList(45)),
            new HashSet<>(Arrays.asList(760))
    );


    static Set<Integer> SINGLE_LINE = Set.of(
            98, 99, 92, 91, 88, 713, 761, 215, 1, 2, 3, 4, 5, 6, 7
    );

    static Set<Integer> FOUR = new HashSet<>(Arrays.asList(new Integer[]{
            19, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 88, 90, 91, 92, 94, 98, 99, 100, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 241, 242, 243, 244, 245, 246, 247, 248, 251, 252, 253, 254, 261, 262, 263, 264, 265, 271, 272, 273, 274, 275, 276, 277, 278, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360, 361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372, 383, 384, 385, 386, 387, 388, 389, 390, 391, 405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 418, 419, 423, 424, 425, 426, 427, 428, 711, 712, 713, 761
    }));

    static Set<Integer> PERFECT_FOUR = new HashSet<>(Arrays.asList(new Integer[]{
            1, 2, 4,
            5, 6,
            9, 10, 13, 14, 19, 20, 21, 22,
            23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42,
            53, 54, 55, 56, 57, 58, 59, 60, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83,
            84, 85, 86,
            88, 90, 91, 92, 94, 98, 99, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 241, 242, 243, 244, 245, 246, 247, 248, 251, 252, 253, 254, 261, 262, 263, 264, 265, 271, 272, 273, 274, 275, 276, 277, 278, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360, 361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372, 383, 384, 385, 386, 387, 388, 389, 390, 391, 405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 418, 419, 423, 424, 425, 426, 427, 428, 711, 712, 713, 761
    }));

    static Set<Integer> PERFECT_TWO = new HashSet<>(Arrays.asList(new Integer[] {
            1, 3,
            //4,
            7, 19, 20, 105, 108, 381, 382, 401, 402, 403, 404, 421, 422, 713, 761, 912,
            //over
            51,

            63, 65
    }));

    static Set<Integer> PERFECT_ONE = new HashSet<>(Arrays.asList(new Integer[] {
            54, 915, 916, 1008, 1009, 902, 903, 904, 905, 906, 907, 908, 909, 910, 911, 917, 918, 919, 920, 921, 922, 923, 924, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1010, 1011, 1012, 1013, 1014, 1015, 1016, 1017, 1018, 1019, 1020, 1021, 1022, 1023, 111, 901,
            54, 713, 761, 19, 20, 713,
    }));

    static Set<Integer> SUSPICIOUS_TWO = new HashSet<>(Arrays.asList(new Integer[]
            {
                120, 715, 717, 760
            }));
    static Set<Integer> THREE = Set.of(121);

    static Set<Integer> PERFECT_THREE = Set.of(
            4, 102, 106, 109, 46, 103, 122, 45, 61, 62, 64, 66, 70
    );
    static Set<Integer> FIVE = Set.of(123);

    static Set<Integer> PERFECT_FIVE = Set.of(47, 68, 69);

    static Set<Integer> PERFECT_SIX = Set.of(
            6, 47, 49, 50, 48
    );
    static Set<Integer> PERFECT_SEVEN = Set.of(
            43, 44, 71, 715, 717, 716, 718)
            ;
    static Set<Integer> PERFECT_EIGHT = Set.of(44);

    static Set<Integer> FOUR_CELL_PATCH = Set.of(8, 11, 12, 15, 16, 17);


    static Set<Integer> SINGLE_CELL_OBJ = Set.of(52);


    public enum CellType {
        ;
        private final Set<Integer> fileIds;
        private final int type;

        CellType(Set<Integer> fileIds, int type) {
            this.fileIds = fileIds;
            this.type = type;
        }
    }




    static void manualCheck() throws Exception {
        final List<Integer> perfectTow = Collections.synchronizedList(new ArrayList<>());
        /*Set<Integer> all = new HashSet<>(FOUR);
        all.addAll(PERFECT_TWO);
        all.addAll(PERFECT_FOUR);
        all.addAll(PERFECT_ONE);
        all.addAll(SUSPICIOUS_TWO);
        all.addAll(THREE);
        all.addAll(FIVE);
        all.addAll(PERFECT_SIX);
        all.addAll(PERFECT_THREE);
        all.addAll(PERFECT_SEVEN);*/
        for (int i = 1; i < 1024; i++) {
            if (!Set.of(7).contains(i))
                continue;
            var w = draw(i, 2);
            if (w == null)
                continue;
            AtomicInteger current = new AtomicInteger(i);
            CountDownLatch latch = new CountDownLatch(1);
            w.callback(answer -> {
                if ("yes".equalsIgnoreCase(answer))
                    perfectTow.add(current.get());
                latch.countDown();
            });
            latch.await();
            w.close();
        }
        System.out.println(perfectTow);
    }

    static void main() throws Exception {
        //collapseDemo();
        collapseMap();
        //generate();
        //checkIntersection(88);
        //List<TileCell> list = new ArrayList<>();
        //addPatchRule(list, 4, 8, 4);
        //initLeft(list);
        // manualCheck();
    }
}
