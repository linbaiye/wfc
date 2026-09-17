package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

public class CombineObj {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombineObj.class);


    static Set<Index> visited = new HashSet<>();


    private static class IndexTile {
        private final Index index;
        private final RealmMap.MapCell mapCell;

        private IndexTile(Index index, RealmMap.MapCell mapCell) {
            this.index = index;
            this.mapCell = mapCell;
        }

        @Override
        public String toString() {
            return "[" + index.x() + ", " + index.y() + "] -> " + "[" + mapCell.ObjectId + ", " + mapCell.ObjectNumber + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            IndexTile indexTile = (IndexTile) o;
            return index.equals(indexTile.index);
        }

        @Override
        public int hashCode() {
            return Objects.hash(index);
        }
    }

    static List<IndexTile> search(RealmMap realmMap, Index index) {
        Predicate<Index> isObject = i -> realmMap.inRange(i) && realmMap.get(i.x(), i.y()).ObjectId > 0;
        if (!isObject.test(index))
            return Collections.emptyList();
        List<IndexTile> result = new ArrayList<>();
        //Set<IndexTile> result = new HashSet<>();
        Deque<Index> deque = new ArrayDeque<>();
        deque.add(index);

        Set<Index> thisVisited = new HashSet<>();
        while (!deque.isEmpty()) {
            Index current = deque.poll();
            result.add(new IndexTile(current, realmMap.get(current.x(), current.y())));
            thisVisited.add(current);
            visited.add(current);
            if (isObject.test(current.up()) && !thisVisited.contains(current.up()))
                deque.add(current.up());
            if (isObject.test(current.right()) && !thisVisited.contains(current.right()))
                deque.add(current.right());
            if (isObject.test(current.down()) && !thisVisited.contains(current.down()))
                deque.add(current.down());
            if (isObject.test(current.left()) && !thisVisited.contains(current.left()))
                deque.add(current.left());
        }
        return new ArrayList<>(result);
    }


    static List<List<IndexTile>> searchObjects(RealmMap realmMap) {
        List<List<IndexTile>> objects = new ArrayList<>();
        for (int i = 0; i < realmMap.width; i++) {
            for (int j = 0; j < realmMap.height; j++) {
                Index index = new Index(i, j);
                if (visited.contains(index))
                    continue;
                RealmMap.MapCell mapCell = realmMap.get(i, j);
                if (mapCell.ObjectId > 0) {
                    var ret = search(realmMap, index);
                    if (!ret.isEmpty())
                        objects.add(ret);
                }
            }
        }
        return objects;
    }


    static List<IndexTile> transformCoordinates(List<IndexTile> ob) {
        var minX = ob.stream().min(Comparator.comparing(i -> i.index.x())).map(it -> it.index.x()).orElse(0);
        var minY = ob.stream().min(Comparator.comparing(i -> i.index.y())).map(it -> it.index.y()).orElse(0);
        return ob.stream().map(it -> new IndexTile(new Index(it.index.x() - minX, it.index.y() - minY), it.mapCell))
        .sorted((o1, o2) -> {
            if (o1.index.x() == o2.index.x())
                return o1.index.y() - o2.index.y();
            return o1.index.x() - o2.index.x();
        }).toList();
    }

    static Window draw(List<IndexTile> list) {
        var maxX = list.stream().max(Comparator.comparing(i -> i.index.x())).map(it -> it.index.x()).orElse(0) + 1;
        var maxY = list.stream().max(Comparator.comparing(i -> i.index.y())).map(it -> it.index.y()).orElse(0) + 1;
        Window window = new Window(maxX, maxY);
        list.forEach(it -> {
            window.drawObject(it.index.x(), it.index.y(), it.mapCell.ObjectId, it.mapCell.ObjectNumber);
        });
        window.display();
        return window;
    }


    static void main() {
        RealmMap realmMap = RealmMap.read("start", "").orElseThrow(IllegalArgumentException::new);
        List<List<IndexTile>> objects = searchObjects(realmMap);
        /*List<IndexTile> list = objects.get(0)
                .stream().sorted((o1, o2) -> {
                    if (o1.index.x() == o2.index.x())
                        return o1.index.y() - o2.index.y();
                    return o1.index.x() - o2.index.x();
                }).toList();
        list.forEach(System.out::println);*/
        System.out.println("---------------------------------");
        objects.forEach(o -> {
            if (o.stream().anyMatch(i -> i.mapCell.ObjectId == 101)) {
                //List<IndexTile> indexTiles = transformCoordinates(o);
                List<IndexTile> indexTiles = o;
                indexTiles.forEach(System.out::println);
                var w = draw(indexTiles);
                try {
                    System.in.read();
                    w.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
