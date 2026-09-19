package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WFC {

    private static final Logger log = LoggerFactory.getLogger(WFC.class);

    private final List<TileCell> cells;

    private final Grid[][] grids;

    public WFC(int w, int h, List<TileCell> cellList) {
        this.grids = new Grid[w][h];
        this.cells = cellList;
        var movable = cellList.stream().filter(TileCell::isMovable).toList();
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                grids[i][j] = new Grid(cells);
            }
        }
    }

    private static class Rules {
        private final List<TileCell> cells;

        private Rules(List<TileCell> cells) {
            this.cells = cells;
        }
    }

    private static class Grid {
        private boolean collapsed;
        private Set<TileCell> cellSet;
        private TileCell collapsedCell;
        public Grid(Collection<TileCell> cells) {
            collapsed = false;
            cellSet = new HashSet<>(cells);
        }

        public void setCollapsedCell(TileCell cell) {
            collapsedCell = cell;
            collapsed = true;
        }

        public void changeSet(Collection<TileCell> newSet) {
            cellSet = new HashSet<>(newSet);
        }

        public int entropy() {
            /*int count = 0;
            for (TileCell tileCell : cellSet) {
                count += tileCell.maxPossiblility();
            }
            return count;*/
            return cellSet.size();
        }


        public TileCell random() {
            if (cellSet.isEmpty())
                return null;
            ArrayList<TileCell> tileCells = new ArrayList<>(cellSet);
            int i = ThreadLocalRandom.current().nextInt(0, tileCells.size());
            return tileCells.get(i);
        }


        public boolean collapseMostPossibility() {
            if (collapsed)
                return false;
            if (cellSet.isEmpty()) {
                collapsed = true;
                return true;
            }
            List<TileCell> reversed = cellSet.stream().sorted(Comparator.comparing(TileCell::maxPossiblility)).toList().reversed();
            if (reversed.size() < 2) {
                setCollapsedCell(reversed.getFirst());
            } else {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                setCollapsedCell(reversed.get(random.nextInt(0, 2)));
            }
            cellSet.clear();
            collapsed= true;
            return true;
        }

        public boolean collapseRandom() {
            if (collapsed)
                return false;
            TileCell random = random();
            if (random == null) {
                collapsed = true;
                return true;
            }
            setCollapsedCell(random);
            cellSet.clear();
            return true;
        }
    }


    private List<Index> findLeastEntropy() {
        List<Index> result = new ArrayList<>();
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < grids.length; i++) {
            for (int j = 0; j < grids[0].length; j++) {
                if (!grids[i][j].collapsed && min > grids[i][j].entropy())
                    min = grids[i][j].entropy();
            }
        }
        if (min == Integer.MAX_VALUE)
            return result;
        for (int i = 0; i < grids.length; i++) {
            for (int j = 0; j < grids[0].length; j++) {
                if (!grids[i][j].collapsed && min == grids[i][j].entropy()) {
                    result.add(new Index(i, j));
                }
            }
        }
        return result;
    }

    private boolean inRange(Index index) {

        if (index.x() < 0 || index.x() >= grids.length)
            return false;
        if (index.y() < 0 || index.y() >= grids[0].length)
            return false;
        return true;
    }

    private void computeEntropy(Index justCollapsedIndex, Direction direction) {
        if (!inRange(justCollapsedIndex)) {
            return;
        }
        Grid justCollapsed = grids[justCollapsedIndex.x()][justCollapsedIndex.y()];
        if (justCollapsed.collapsedCell == null)
            return;
        /*if (direction == Direction.Left) {
            Index left = justCollapsedIndex.left();
            if (!inRange(left)) {
                return;
            }
            Grid leftGrid = grids[left.x][left.y];
            if (leftGrid.collapsed) {
                return;
            }
            leftGrid.cellSet.removeIf(c -> !justCollapsed.collapsedCell.canConnect(c, Direction.Left));
        }*/
        Index index = null;
        if (direction == Direction.Left) {
            index = justCollapsedIndex.left();
        } else if (direction == Direction.Right) {
            index = justCollapsedIndex.right();
        } else if (direction == Direction.Up) {
            index = justCollapsedIndex.up();
        } else if (direction == Direction.Down) {
            index = justCollapsedIndex.down();
        }
        if (index == null || !inRange(index))
            return;
        Grid target = grids[index.x()][index.y()];
        if (target.collapsed) {
            return;
        }
        Set<TileCell> collect = target.cellSet
                .stream()
                .filter(c -> c.canConnect(justCollapsed.collapsedCell, direction.opposite()))
                .collect(Collectors.toSet());
        target.changeSet(collect);
        /*target.cellSet.forEach(c -> {
            if (justCollapsed.collapsedCell.canConnect(c, direction))
                log.debug("can connect to {}.", c);
        });
        target.cellSet.removeIf(c -> !justCollapsed.collapsedCell.canConnect(c, direction));*/
    }



    private boolean collapse(Index index) {
        //log.info("Collapse {}.", index);
        Grid grid = grids[index.x()][index.y()];
        boolean ret = grid.collapseRandom();
        if (!ret)
            return false;
        computeEntropy(index, Direction.Up);
        computeEntropy(index, Direction.Down);
        computeEntropy(index, Direction.Left);
        computeEntropy(index, Direction.Right);
        return true;
    }

    private boolean collapseBest(Index index) {
        //log.info("Collapse {}.", index);
        Grid grid = grids[index.x()][index.y()];
        boolean ret = grid.collapseMostPossibility();
        if (!ret)
            return false;
        computeEntropy(index, Direction.Up);
        computeEntropy(index, Direction.Down);
        computeEntropy(index, Direction.Left);
        computeEntropy(index, Direction.Right);
        return true;
    }

    private TileCell find(Index i) {
        if (!inRange(i))
            return null;
        if (grids[i.x()][i.y()].collapsed && grids[i.x()][i.y()].collapsedCell != null) {
            return grids[i.x()][i.y()].collapsedCell;
        }
        return null;
    }


    private TileCell findBest(Index black, List<TileCell> all) {
        var upCell = find(black.up());
        if (upCell != null) {
            var upCells = all.stream().filter(t -> upCell.canConnect(t, Direction.Down)).toList();
            if (upCells.size() > 0)
                return upCells.getFirst();
        }
        var left = black.left();
        var leftCell = find(left);
        if (leftCell != null) {
            var leftCells = all.stream().filter(t -> leftCell.canConnect(t, Direction.Right)).toList();
            if (leftCells.size() > 0)
                return leftCells.getFirst();
        }
        var rightCell = find(black.right());
        if (rightCell != null) {
            var rightCells = all.stream().filter(t -> rightCell.canConnect(t, Direction.Left)).toList();
            if (rightCells.size() > 0)
                return rightCells.getFirst();
        }
        var downCell = find(black.down());
        if (downCell != null) {
            var upCells = all.stream().filter(t -> downCell.canConnect(t, Direction.Up)).toList();
            if (upCells.size() > 0)
                return upCells.getFirst();
        }
        return null;
    }

    public void fillBlack(Window window, List<TileCell> all) {
        for (int i = 0; i < grids.length; i++) {
            for (int j = 0; j < grids[0].length; j++) {
                Grid grid = grids[i][j];
                if (grid.collapsed && grid.collapsedCell != null) {
                    continue;
                }
                Index index = new Index(i, j);
                TileCell tileCell = findBest(index, all);
                if (tileCell != null) {
                    window.draw(i, j, tileCell.id, tileCell.number);
                    log.info("Filled black at {}.", index);
                }
            }
        }
    }

    public float checkBlacks() {
        int count = 0;
        for (int i = 0; i < grids.length; i++) {
            for (int j = 0; j < grids[0].length; j++) {
                if (grids[i][j].collapsed && grids[i][j].collapsedCell != null) {
                } else {
                    count++;
                }
            }
        }
        float rate = (float) count / (grids.length * grids[0].length);
        log.info("Backs {}, percent {}.", count, rate);
        return rate;
    }

    public void run() {
        //collapseBest(new Index(10, 10));
        while (true) {
            List<Index> leastEntropy = findLeastEntropy();
            if (leastEntropy.isEmpty())
                break;
            //int i = ThreadLocalRandom.current().nextInt(0, leastEntropy.size());
            boolean changed = false;
            for (Index i : leastEntropy) {
                if (collapse(i)) {
                    changed = true;
                    break;
                }
            }
            if (!changed)
                break;
        }
        //log.debug("Done run");
    }

    public boolean hasContradiction() {
        for (int i = 0; i < grids.length; i++) {
            for (int j = 0; j < grids[0].length; j++) {
                if (grids[i][j].collapsed && grids[i][j].collapsedCell != null) {
                    continue;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public Window draw() {
        Window window = new Window(grids.length, grids[0].length);
        for (int i = 0; i < grids.length; i++) {
            for (int j = 0; j < grids[0].length; j++) {
                if (grids[i][j].collapsed && grids[i][j].collapsedCell != null) {
                    window.draw(i, j, grids[i][j].collapsedCell.id, grids[i][j].collapsedCell.number);
                } else {
                    window.fillBlack(i, j);
                }
            }
        }
        window.display();
        try {
            System.in.read();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            window.close();
        }
        return window;
    }


    private void dumpBlacks() {
        Function<Index, TileCell> indexToCell = i -> {
            if (!inRange(i))
                return null;
            if (grids[i.x()][i.y()].collapsed && grids[i.x()][i.y()].collapsedCell != null) {
                return grids[i.x()][i.y()].collapsedCell;
            }
            return null;
        };
        for (int i = 0; i < grids.length; i++) {
            for (int j = 0; j < grids[0].length; j++) {
                if (grids[i][j].collapsed && grids[i][j].collapsedCell != null) {

                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    Index index = new Index(i, j);
                    Index up = index.up();
                    if (inRange(up)) {
                        TileCell cell = indexToCell.apply(up);
                        if (cell != null)
                            stringBuilder.append("up: [").append(cell.id).append(",").append(cell.number).append("]");
                        else
                            stringBuilder.append("up: []");
                    } else {
                        stringBuilder.append("up: []");
                    }
                    stringBuilder.append(", ");
                    Index right = index.right();
                    if (inRange(right)) {
                        TileCell cell = indexToCell.apply(right);
                        if (cell != null)
                            stringBuilder.append("right: [").append(cell.id).append(",").append(cell.number).append("]");
                        else
                            stringBuilder.append("right: []");
                    } else {
                        stringBuilder.append("right: []");
                    }
                    stringBuilder.append(", ");
                    Index down = index.down();
                    if (inRange(down)) {
                        TileCell cell = indexToCell.apply(down);
                        if (cell != null)
                            stringBuilder.append("down: [").append(cell.id).append(",").append(cell.number).append("]");
                        else
                            stringBuilder.append("down: []");
                    } else {
                        stringBuilder.append("down: []");
                    }

                    stringBuilder.append(", ");
                    Index left = index.left();
                    if (inRange(left)) {
                        TileCell cell = indexToCell.apply(left);
                        if (cell != null)
                            stringBuilder.append("left: [").append(cell.id).append(",").append(cell.number).append("]");
                        else
                            stringBuilder.append("left: []");
                    } else {
                        stringBuilder.append("left: []");
                    }
                    log.info("({}, {}), {}.", i, j, stringBuilder);
                }
            }
        }
    }

    public void drawDemo() {
        DemoWindow window = new DemoWindow(grids.length, grids[0].length);
        for (int i = 0; i < grids.length; i++) {
            for (int j = 0; j < grids[0].length; j++) {
                if (grids[i][j].collapsed && grids[i][j].collapsedCell != null) {
                    window.draw(i, j, grids[i][j].collapsedCell.id, grids[i][j].collapsedCell.number);
                } else {
                    window.fillBlack(i, j);
                }
            }
        }
        window.display();
    }
}
