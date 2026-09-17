package org.example;


import de.gurkenlabs.litiengine.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

final class RealmMap {

    private static final int HEADER_SIZE = 28;

    private static final int BLOCK_SIZE = 40;

    // 20bytes for block header.
    // block id(16 bytes), changedCount(4 bytes)
    // cells[1600 * 12 bytes]
    private static final int MAP_BLOCK_DATA_SIZE = 20 + BLOCK_SIZE * BLOCK_SIZE * 12;

    private static final int MAP_CELL_SIZE = 12;

    private final byte[][] movableMask;

    public final int height;
    public final int width;

    private final String mapFile;
    private final String resource;

    private final MapCell[][] cells;
    private static final Logger log = LoggerFactory.getLogger(RealmMap.class);

    private RealmMap(byte[][] movableMask, String name, String resource, MapCell[][] cells) {
        Objects.requireNonNull(name);
        if (movableMask.length == 0) {
            throw new IllegalArgumentException();
        }
        if (movableMask[0].length == 0) {
            throw new IllegalArgumentException();
        }
        this.mapFile = name;
        this.movableMask = movableMask;
        this.height = movableMask.length;
        this.width = movableMask[0].length;
        this.resource = resource;
        this.cells = cells;
    }


    public static class MapCell {
        public short TileId;
        public byte TileNumber;
        public short TileOverId;
        public byte TileOverNumber;
        public short ObjectId;
        public byte ObjectNumber;
        public short RoofId;
        public byte BoMove;

        public boolean isMovable() {
            return ((BoMove & 0x1) == 0) && ((BoMove & 0x2) == 0);
        }
    }


    public MapCell get(int x, int y) {
        return cells[y][x];
    }


    private static class Header {
        private final String idString;
        private final int blockSize;
        private final int width;
        private final int height;

        private Header(String idString, int blockSize, int width, int height) {
            this.idString = idString;
            this.blockSize = blockSize;
            this.width = width;
            this.height = height;
        }

        private static Header parse(ByteBuffer headerBuffer) {
            byte[] idbytes = new byte[16];
            headerBuffer.get(idbytes);
            String idString = new String(idbytes);
            if ("ATZMAP2".equals(idString)) {
                throw new IllegalArgumentException("Invalid idString.");
            }
            return new Header(idString, headerBuffer.getInt(), headerBuffer.getInt(), headerBuffer.getInt());
        }
    }


    public static Optional<RealmMap> read(String name, String resource) {
        String mapName = name.endsWith(".map") ? name : name + ".map";
        if (!mapName.startsWith("/maps/")) {
            mapName = "/maps/" + mapName;
        }
        try (InputStream is = Main.class.getResourceAsStream(mapName)) {
            if (is == null) {
                log.error("Map {} does not exist.", mapName);
                return Optional.empty();
            }
            ByteBuffer headerBinary = ByteBuffer.allocate(HEADER_SIZE);
            headerBinary.order(ByteOrder.LITTLE_ENDIAN);
            if (is.read(headerBinary.array(), 0, headerBinary.capacity()) != headerBinary.capacity()) {
                log.error("Map {} contains invalid headerBinary.", mapName);
                return Optional.empty();
            }
            Header header = Header.parse(headerBinary);
            byte[][] cellMasks = new byte[header.height][header.width];
            MapCell[][] cells = new MapCell[header.height][header.width];
            byte[] cellBinary = new byte[MAP_CELL_SIZE];
            ByteBuffer blockData = ByteBuffer.allocate(MAP_BLOCK_DATA_SIZE);
            blockData.order(ByteOrder.LITTLE_ENDIAN);
            for (int h = 0; h < header.height / BLOCK_SIZE; h++) {
                for (int w = 0; w < header.width / BLOCK_SIZE; w++) {
                    if (is.read(blockData.array()) != MAP_BLOCK_DATA_SIZE) {
                        log.error("Failed to read map block for map {}.", name);
                        return Optional.empty();
                    }
                    for (int y = 0; y < BLOCK_SIZE; y++) {
                        for (int x = 0; x < BLOCK_SIZE; x++) {
                            int off = 0;
                            MapCell cell = new MapCell();
                            cell.TileId = blockData.getShort(20 + (y * BLOCK_SIZE + x) * MAP_CELL_SIZE + off);
                            off += 2;

                            cell.TileNumber = blockData.get(20 + (y * BLOCK_SIZE + x) * MAP_CELL_SIZE + off);
                            off += 1;

                            cell.TileOverId = blockData.getShort(20 + (y * BLOCK_SIZE + x) * MAP_CELL_SIZE + off);
                            off += 2;

                            cell.TileOverNumber = blockData.get(20 + (y * BLOCK_SIZE + x) * MAP_CELL_SIZE + off);
                            off += 1;

                            cell.ObjectId = blockData.getShort(20 + (y * BLOCK_SIZE + x) * MAP_CELL_SIZE + off);
                            off += 2;

                            cell.ObjectNumber = blockData.get(20 + (y * BLOCK_SIZE + x) * MAP_CELL_SIZE + off);
                            off += 1;

                            cell.RoofId = blockData.getShort(20 + (y * BLOCK_SIZE + x) * MAP_CELL_SIZE + off);
                            off += 2;

                            var movable = blockData.get(20 + (y * BLOCK_SIZE + x) * MAP_CELL_SIZE + off);
                            cell.BoMove = movable;

                            cellMasks[h *  BLOCK_SIZE + y][w * BLOCK_SIZE + x] = movable;
                            cells[h *  BLOCK_SIZE + y][w * BLOCK_SIZE + x] = cell;
                        }
                    }
                }
            }
            return Optional.of(new RealmMap(cellMasks, name, resource, cells));
        } catch (Exception e) {
            log.error("Failed to read map {}.", mapName, e);
        }
        return Optional.empty();
    }

    public boolean inRange(Index index) {
        return index.x() >= 0 && index.x() < width &&
                index.y() >= 0 && index.y() < height;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        RealmMap map = RealmMap.read("start", "test")
                .orElseThrow(IllegalArgumentException::new);
        for (int i = 0; i < map.width; i++) {
            for (int j = 0; j < map.height; j++) {
                MapCell cell = map.get(i, j);
                var str = String.format("%3d-%2d", cell.TileId, cell.TileNumber);
                System.out.print(str);
                System.out.print(",");
            }
            System.out.println();
        }
        Set<Index> tiles = new HashSet<>();
        Set<Index> overTiles = new HashSet<>();
        for (int i = 0; i < map.width; i++) {
            for (int j = 0; j < map.height; j++) {
                MapCell cell = map.get(i, j);
                if (cell.TileId > 0) {
                    tiles.add(new Index(cell.TileId, cell.TileNumber));
                }
                if (cell.TileOverId > 0) {
                    overTiles.add(new Index(cell.TileOverId, cell.TileOverNumber));
                }
            }
        }
        log.info("Width {}, height {}, tile {}, tileOver {}, movable {}.", map.width, map.height, tiles.size(), overTiles.size(), 0);
    }
}
