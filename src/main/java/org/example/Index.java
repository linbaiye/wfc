package org.example;

import java.util.Objects;

public record Index(int x, int y) {

    public Index left() {
        return new Index(x - 1, y);
    }

    public Index right() {
        return new Index(x + 1, y);
    }

    public Index up() {
        return new Index(x, y - 1);
    }

    public Index down() {
        return new Index(x, y + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Index index = (Index) o;
        return x == index.x && y == index.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
