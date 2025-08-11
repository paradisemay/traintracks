package com.itsamsung.traintracks;

import android.util.Log;
import android.util.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Random;


public class Field {
    private static final String TAG = "Field";

    private final int height;
    private final int width;
    private int villageA;
    private int villageB;
    private CellType[][] field;
    private Integer[] sumsX;
    private Integer[] sumsY;
    private int state = 0;

    private Random rnd = new Random();
    private ArrayList<Pair<Integer, Integer>> solutionPath = new ArrayList<>();
    private final Deque<Move> undoStack = new ArrayDeque<>();
    private final Deque<Move> redoStack = new ArrayDeque<>();

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void putSumsX(int index, int value) {
        sumsX[index] = value;
    }

    public void putSumsY(int index, int value) {
        sumsY[index] = value;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getVillageA() {
        return villageA;
    }

    public int getVillageB() {
        return villageB;
    }

    public void setVillages(int a, int b) {
        villageA = a;
        villageB = b;
    }

    public void randomizeVillages() {
        villageA = rnd.nextInt(height - 2) + 1;
        villageB = rnd.nextInt(width - 2) + 1;
    }

    public Integer[] getSumsX() { return sumsX; }

    public Integer[] getSumsY() { return sumsY; }

    public ArrayList<Pair<Integer, Integer>> getSolutionPath() { return solutionPath; }

    public Field(int height, int width) {
        this.height = height;
        this.width = width;
        randomizeVillages();
        field = new CellType[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                field[x][y] = CellType.EMPTY;
            }
        }
        sumsX = new Integer[width];
        sumsY = new Integer[height];
        for (int col = 0; col < width; col++) {
            sumsX[col] = 0;
        }
        for (int line = 0; line < height; line++) {
            sumsY[line] = 0;
        }
    }

    private int getRailsCount() {
        int res = 0;
        for (int col = 0; col < width; col++) {
            for (int line = 0; line < height; line++) {
                if (getCell(col, line) == CellType.RAIL) {
                    res++;
                }
            }
        }

        return res;
    }

    public void clear() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                field[x][y] = CellType.EMPTY;
            }
        }
        undoStack.clear();
        redoStack.clear();
    }

    public boolean check(){
        ArrayDeque<Pair<Integer, Integer>> flow = new ArrayDeque<Pair<Integer, Integer>>();
        ArrayList<Pair<Integer, Integer>> stepped = new ArrayList<Pair<Integer, Integer>>();
        flow.addLast(new Pair<>(0, villageA));

        boolean finished = false;
        while (true) {
            if (flow.size() == 0) break;
            Pair<Integer, Integer> current = flow.removeFirst();
            if (current.equals(new Pair<>(villageB, height - 1))) {
                finished = true;
            }
            if (getCell(current.first, current.second) == CellType.EMPTY) {
                continue;
            }
            if (stepped.contains(current)) {
                continue;
            }
            stepped.add(current);

            ArrayList<Pair<Integer, Integer>> var = new ArrayList<Pair<Integer, Integer>>();
            var.add(new Pair<Integer, Integer>(current.first - 1, current.second));
            var.add(new Pair<Integer, Integer>(current.first, current.second - 1));
            var.add(new Pair<Integer, Integer>(current.first + 1, current.second));
            var.add(new Pair<Integer, Integer>(current.first, current.second + 1));

            for (int i = 0; i < var.size(); i++) {
                if (inField(var.get(i)) && !stepped.contains(var.get(i))) {
                    flow.add(var.get(i));
                }
            }
        }
        if (finished) {
            return stepped.size() == getRailsCount() && checkSums();
        } else {
            return false;
        }
    }

    private boolean checkSums() {
        Log.v(TAG, "checkSums");
        boolean flagX = true;
        for (int col = 0; col < width; col++) {
            int res = 0;
            for (int line = 0; line < height; line++) {
                if (getCell(col, line).equals(CellType.RAIL)) {
                    res++;
                }
            }
            Log.v(TAG, "res=" + res);
            flagX = flagX && (res == sumsX[col]);
        }

        Log.v(TAG, "XY");
        boolean flagY = true;
        for (int line = 0; line < height; line++) {
            int res = 0;
            for (int col = 0; col < width; col++) {
                if (getCell(col, line).equals(CellType.RAIL)) {
                    res++;
                }
            }
            Log.v(TAG, "res=" + res);
            flagY = flagY && (res == sumsY[line]);
        }
        Log.v(TAG, "flagx" + flagX);
        Log.v(TAG, "flagy" + flagY);
        return flagX && flagY;
    }

    private boolean inField(Pair<Integer, Integer> cell) {
        return 0 <= cell.first && cell.first < width && 0 <= cell.second && cell.second < height;
    }

    public void putCell(Pair<Integer, Integer> cell, boolean isRail) {
        if (!exists(cell)) {
            throw new IndexOutOfBoundsException();
        }
        if (isRail) field[cell.first][cell.second] = CellType.RAIL;
        else field[cell.first][cell.second] = CellType.EMPTY;
    }

    private boolean exists(Pair<Integer, Integer> cell) {
        return inField(cell);
    }

    public void putCell(int x, int y, CellType value) {
        field[x][y] = value;
    }

    public void applyMove(int x, int y, CellType value) {
        CellType prev = field[x][y];
        if (prev == value) return;
        field[x][y] = value;
        undoStack.push(new Move(x, y, prev, value));
        redoStack.clear();
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        Move m = undoStack.pop();
        field[m.x][m.y] = m.from;
        redoStack.push(m);
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        Move m = redoStack.pop();
        field[m.x][m.y] = m.to;
        undoStack.push(m);
    }


    public CellType getCell(int x, int y) {
        return field[x][y];
    }

    public void generatePath() {
        clear();
        ArrayList<Pair<Integer, Integer>> path = new ArrayList<>();
        boolean[][] visited = new boolean[width][height];
        Pair<Integer, Integer> start = new Pair<>(0, villageA);
        Pair<Integer, Integer> goal = new Pair<>(villageB, height - 1);
        path.add(start);
        visited[start.first][start.second] = true;
        dfs(start.first, start.second, goal.first, goal.second, visited, path);
        solutionPath.clear();
        solutionPath.addAll(path);
        fillByPath(path);
    }

    private boolean dfs(int x, int y, int gx, int gy, boolean[][] visited,
                        ArrayList<Pair<Integer, Integer>> path) {
        if (x == gx && y == gy) {
            return true;
        }
        ArrayList<Pair<Integer, Integer>> dirs = new ArrayList<>();
        dirs.add(new Pair<>(1, 0));
        dirs.add(new Pair<>(-1, 0));
        dirs.add(new Pair<>(0, 1));
        dirs.add(new Pair<>(0, -1));
        Collections.shuffle(dirs, rnd);
        for (Pair<Integer, Integer> d : dirs) {
            int nx = x + d.first;
            int ny = y + d.second;
            if (0 <= nx && nx < width && 0 <= ny && ny < height && !visited[nx][ny]) {
                visited[nx][ny] = true;
                path.add(new Pair<>(nx, ny));
                if (dfs(nx, ny, gx, gy, visited, path)) {
                    return true;
                }
                path.remove(path.size() - 1);
                visited[nx][ny] = false;
            }
        }
        return false;
    }
    private void fillByPath(ArrayList<Pair<Integer, Integer>> path) {
        for (Pair<Integer, Integer> cell :
                path) {
            putCell(cell, true);
        }
        updateSums();
    }

    public Pair<Integer, Integer> hint() {
        for (Pair<Integer, Integer> cell : solutionPath) {
            if (getCell(cell.first, cell.second) != CellType.RAIL) {
                applyMove(cell.first, cell.second, CellType.RAIL);
                return cell;
            }
        }
        return null;
    }

    public void updateSums() {
        for (int col = 0; col < width; col++) {
            int summa = 0;
            for (int line = 0; line < height; line++) {
                if (getCell(col, line) == CellType.RAIL) {
                    summa++;
                }
            }
            sumsX[col] = summa;
        }
        for (int line = 0; line < height; line++) {
            int summa = 0;
            for (int col = 0; col < width; col++) {
                if (getCell(col, line) == CellType.RAIL) {
                    summa++;
                }
            }
            sumsY[line] = summa;
        }
    }

    private static class Move {
        final int x, y;
        final CellType from, to;

        Move(int x, int y, CellType from, CellType to) {
            this.x = x;
            this.y = y;
            this.from = from;
            this.to = to;
        }
    }
}
