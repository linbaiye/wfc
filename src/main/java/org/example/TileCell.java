package org.example;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class TileCell {
    public final int id;
    public final int number;
    private boolean movable;

    private final Map<Direction, Set<TileCell>> neibhours;

    public TileCell(int id, int number, boolean movable) {
        this.id = id;
        this.number = number;
        this.neibhours = new HashMap<>();
        this.movable = movable;
    }

    public void changeMove(boolean m) {
        this.movable = m;
    }

    public boolean has(int tileId, int tileNumber) {
        return id == tileId && number == tileNumber;
    }


    public void addNeibhour(TileCell tileCell, Direction direction) {
        neibhours.computeIfAbsent(direction, k -> new HashSet<>())
                .add(tileCell);
    }

    public void addNeibhour(Direction direction, TileCell ... tiles) {
        for (TileCell tile : tiles) {
            addNeibhour(tile, direction);
        }
    }

    public boolean canConnect(TileCell another, Direction direction) {
        return neibhours.getOrDefault(direction, Collections.emptySet())
                .contains(another);
    }


    public Optional<TileCell> randomChoice(Direction direction) {
        Set<TileCell> tileCells = neibhours.get(direction);
        if (tileCells == null)
            return Optional.empty();
        List<TileCell> list = tileCells.stream().filter(TileCell::isMovable)
                .sorted(Comparator.comparing(TileCell::movablePossibility))
                .toList().reversed();
        if (!list.isEmpty()) {
            ThreadLocalRandom localRandom = ThreadLocalRandom.current();
            var idx = localRandom.nextInt(0, list.size());
            return Optional.of(list.get(idx));
        }
        list = tileCells.stream()
                .sorted(Comparator.comparing(TileCell::movablePossibility))
                .toList()
                .reversed();
        if (list.isEmpty())
            return Optional.empty();
        ThreadLocalRandom localRandom = ThreadLocalRandom.current();
        var idx = localRandom.nextInt(0, list.size());
        return Optional.of(list.get(idx));
    }


    public Optional<TileCell> bestChoice(Direction direction) {
        Set<TileCell> tileCells = neibhours.get(direction);
        if (tileCells == null)
            return Optional.empty();
        List<TileCell> list = tileCells.stream().filter(TileCell::isMovable)
                .sorted(Comparator.comparing(TileCell::movablePossibility))
                .toList().reversed();
        if (!list.isEmpty())
            return Optional.of(list.getFirst());
        list = tileCells.stream()
                .sorted(Comparator.comparing(TileCell::movablePossibility))
                .toList()
                .reversed();
        if (list.isEmpty())
            return Optional.empty();
        return Optional.of(list.getFirst());
    }

    private int movablePossibility() {
        Function<Direction, Integer> f = d -> neibhours.getOrDefault(d, Collections.emptySet())
                .stream().filter(TileCell::isMovable).toList().size();
        return f.apply(Direction.Left) + f.apply(Direction.Right) + f.apply(Direction.Up) + f.apply(Direction.Down);
    }

    public int possibility(Direction direction) {
        return neibhours.get(direction) != null ?
                neibhours.get(direction).size() : 0;
    }

    public int maxPossiblility() {
        return possibility(Direction.Left) + possibility(Direction.Right)
                + possibility(Direction.Up) + possibility(Direction.Down);
    }

    public boolean isMovable() {
        return movable;
    }

    public List<TileCell> candidates(Direction direction) {
        return new ArrayList<>(neibhours.getOrDefault(direction, Collections.emptySet()));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TileCell tileCell = (TileCell) o;
        return id == tileCell.id && number == tileCell.number;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, number);
    }

    @Override
    public String toString() {
        return "TileCell{" +
                "id=" + id +
                ", number=" + number +
                '}';
    }
}
