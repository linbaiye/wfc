package org.example;

public enum Direction {

    Left,
    Right,
    Up,
    Down,

    ;
    public Direction opposite() {
        if (this == Direction.Left)
            return Direction.Right;
        if (this == Direction.Right)
            return Direction.Left;
        if (this == Direction.Up)
            return Direction.Down;
        return Direction.Up;
    }


}
