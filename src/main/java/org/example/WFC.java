package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
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
                grids[i][j] = new Grid(movable);
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


    private record Index(int x, int y) {

        public Index left() {
            return new Index(x -1, y);
        }

        public Index right() {
            return new Index(x + 1, y);
        }

        public Index up() {
            return new Index(x, y -1);
        }

        public Index down() {
            return new Index(x, y + 1);
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

        if (index.x < 0 || index.x >= grids.length)
            return false;
        if (index.y < 0 || index.y >= grids[0].length)
            return false;
        return true;
    }

    private void computeEntropy(Index justCollapsedIndex, Direction direction) {
        if (!inRange(justCollapsedIndex)) {
            return;
        }
        Grid justCollapsed = grids[justCollapsedIndex.x][justCollapsedIndex.y];
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
        Grid target = grids[index.x][index.y];
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
        log.info("Collapse {}.", index);
        Grid grid = grids[index.x][index.y];
        boolean ret = grid.collapseRandom();
        if (!ret)
            return false;
        computeEntropy(index, Direction.Up);
        computeEntropy(index, Direction.Down);
        computeEntropy(index, Direction.Left);
        computeEntropy(index, Direction.Right);
        return true;
    }

    public void run() {
        //collapse(new Index(10, 10));
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
        log.debug("Done run");
    }

    public void draw() {
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
