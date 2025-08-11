package com.itsamsung.traintracks;

import static java.lang.System.nanoTime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.widget.Toast;
import android.os.SystemClock;

import java.util.Random;

public class DrawThread extends Thread {
    private static final String TAG = "DrawThread";
    private final Bitmap villageBitmap;
    private final Bitmap railsBitmap;
    private Bitmap scaledVillage;
    private Bitmap scaledRails;
    private int lastCellSize = -1;
    private final SurfaceHolder surfaceHolder;

    private final Paint netPaint = new Paint();
    private final Paint bgPaint = new Paint();
    private final Paint digitPaint = new Paint();
    private final Paint TextPaint = new Paint();
    private final Paint hintPaint = new Paint();
    private final Paint solutionPaint = new Paint();
    private volatile boolean running = true;
    private Field field;
    private int canvasWidth, canvasHeight;
    private Context context;
    private long clickTime = 0L;
    private Pair<Integer, Integer> hintCell;
    private DrawView.GameListener gameListener;
    private boolean gameStarted = false;
    private boolean showSolution = false;
    private int themeId = ThemeManager.THEME_DARK;

    {
        bgPaint.setColor(Color.WHITE);
        bgPaint.setStyle(Paint.Style.FILL);
        netPaint.setColor(Color.BLACK);
        netPaint.setStyle(Paint.Style.STROKE);
        digitPaint.setColor(Color.BLACK);
        digitPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        TextPaint.setColor(Color.BLACK);
        TextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        TextPaint.setTextSize(20);
        TextPaint.setAntiAlias(true);
        hintPaint.setColor(Color.argb(128, 255, 255, 0));
        solutionPaint.setColor(Color.argb(128, 0, 0, 255));
    }

    private static void setTextSizeForWidth(Paint paint, float desiredWidth, String text) {
        final float testTextSize = 48f;
        paint.setTextSize(testTextSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        float desiredTextSize = testTextSize * desiredWidth / bounds.width();
        paint.setTextSize(desiredTextSize);
    }

    private static void setTextSize(Paint paint, float desiredSize, String text) {
        final float testTextSize = 48f;
        paint.setTextSize(testTextSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        float desiredTextSize = Math.min(testTextSize * desiredSize / bounds.width(), testTextSize * desiredSize / bounds.height());
        paint.setTextSize(desiredTextSize);
    }

    public void giveField(Field otherField) {
        field = otherField;
    }

    public void giveContext(Context otherContext) {
        context = otherContext;
    }

    public void highlightHint(Pair<Integer, Integer> cell) {
        hintCell = cell;
    }

    public void setGameListener(DrawView.GameListener listener) {
        this.gameListener = listener;
    }

    public void toggleSolution() {
        showSolution = !showSolution;
    }

    public void setTheme(int id) {
        themeId = id;
        applyThemeColors();
    }

    private void applyThemeColors() {
        switch (themeId) {
            case ThemeManager.THEME_DARK:
                bgPaint.setColor(Color.BLACK);
                netPaint.setColor(Color.WHITE);
                digitPaint.setColor(Color.WHITE);
                TextPaint.setColor(Color.WHITE);
                break;
            case ThemeManager.THEME_LIGHT:
                bgPaint.setColor(Color.WHITE);
                netPaint.setColor(Color.BLACK);
                digitPaint.setColor(Color.BLACK);
                TextPaint.setColor(Color.BLACK);
                break;
            case ThemeManager.THEME_RED:
                bgPaint.setColor(Color.RED);
                netPaint.setColor(Color.BLACK);
                digitPaint.setColor(Color.BLACK);
                TextPaint.setColor(Color.BLACK);
                break;
            case ThemeManager.THEME_BLUE:
                bgPaint.setColor(Color.BLUE);
                netPaint.setColor(Color.WHITE);
                digitPaint.setColor(Color.WHITE);
                TextPaint.setColor(Color.WHITE);
                break;
            case ThemeManager.THEME_GREEN:
                bgPaint.setColor(Color.GREEN);
                netPaint.setColor(Color.BLACK);
                digitPaint.setColor(Color.BLACK);
                TextPaint.setColor(Color.BLACK);
                break;
            case ThemeManager.THEME_YELLOW:
                bgPaint.setColor(Color.YELLOW);
                netPaint.setColor(Color.BLACK);
                digitPaint.setColor(Color.BLACK);
                TextPaint.setColor(Color.BLACK);
                break;
            case ThemeManager.THEME_CYAN:
                bgPaint.setColor(Color.CYAN);
                netPaint.setColor(Color.BLACK);
                digitPaint.setColor(Color.BLACK);
                TextPaint.setColor(Color.BLACK);
                break;
            case ThemeManager.THEME_STRIPES:
            case ThemeManager.THEME_CHECKER:
            case ThemeManager.THEME_DYNAMIC:
                // базовые цвета для узоров
                bgPaint.setColor(Color.WHITE);
                netPaint.setColor(Color.BLACK);
                digitPaint.setColor(Color.BLACK);
                TextPaint.setColor(Color.BLACK);
                break;
        }
    }

    private static class Geometry {
        final int cellSize;
        final int sx;
        final int sy;
        Geometry(int cellSize, int sx, int sy) {
            this.cellSize = cellSize;
            this.sx = sx;
            this.sy = sy;
        }
    }

    private Geometry calcGeometry(Canvas canvas) {
        int dw = canvas.getWidth();
        int dh = canvas.getHeight();
        int shift = Math.min(dw, dh) * 10 / 100;
        int cellSize = Math.min((dw - 2 * shift) / field.getWidth(),
                (dh - 2 * shift) / field.getHeight());
        int fieldSizeX = cellSize * field.getWidth();
        int fieldSizeY = cellSize * field.getHeight();
        int sx;
        int sy;
        if (fieldSizeX == fieldSizeY) {
            sx = (dw - fieldSizeX) / 2;
            sy = (dh - fieldSizeY) / 2;
        } else if (fieldSizeX < fieldSizeY) {
            sx = (dw - fieldSizeX) / 2;
            sy = shift;
        } else {
            sx = shift;
            sy = (dh - fieldSizeY) / 2;
        }
        if (cellSize != lastCellSize) {
            scaledRails = Bitmap.createScaledBitmap(railsBitmap, cellSize, cellSize, true);
            scaledVillage = Bitmap.createScaledBitmap(villageBitmap, cellSize, cellSize, true);
            lastCellSize = cellSize;
        }
        return new Geometry(cellSize, sx, sy);
    }

    public DrawThread(Context context, SurfaceHolder surfaceHolder) {
        villageBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.train_tracks_village);
        railsBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.train_tracks_rails);
        this.surfaceHolder = surfaceHolder;
    }

    public void setTowardPoint(int x, int y) {
        int dw = canvasWidth;
        int dh = canvasHeight;
        int shift = Math.min(dw, dh) * 10 / 100;

        int cellSize = Math.min((dw - 2 * shift) / field.getWidth(), (dh - 2 * shift) / field.getHeight());

        int fieldSizeX = cellSize * field.getWidth();
        int fieldSizeY = cellSize * field.getHeight();

        int sx, sy;
        if (fieldSizeX == fieldSizeY) {

            sx = (dw - fieldSizeX) / 2;
            sy = (dh - fieldSizeY) / 2;
        } else if (fieldSizeX < fieldSizeY) {
            sx = (dw - fieldSizeX) / 2;
            sy = shift;
        } else {
            int sd = fieldSizeX - fieldSizeY;
            sx = shift;
            sy = (dh - fieldSizeY) / 2;
        }

        Log.v(TAG, "state=" + field.getState());

        if (field.getState() == 0) {
            field.generatePath();
            field.updateSums();
            field.clear();
            field.setState(1);
        } else if (field.getState() == 1) {
            if (sx <= x && x < sx + fieldSizeX && sy <= y && y < sy + fieldSizeY) {
                x = x - sx;
                y = y - sy;
                int col = x / cellSize;
                int line = y / cellSize;
                CellType current = field.getCell(col, line);
                CellType next;
                switch (current) {
                    case EMPTY: next = CellType.RAIL; break;
                    case RAIL: next = CellType.MARKED; break;
                    case MARKED: default: next = CellType.EMPTY; break;
                }
                field.applyMove(col, line, next);
                if (!gameStarted && next == CellType.RAIL && gameListener != null) {
                    gameListener.onFirstRail();
                    gameStarted = true;
                }
            } else {
                long clickNow = nanoTime();
                long clickDur = 500000000; // 1/2 second
                if (clickNow - clickTime < clickDur) {
                    field.clear();
                    field.updateSums();
                    field.randomizeVillages();
                    field.setState(0);
                    gameStarted = false;
                }
                clickTime = clickNow;
            }
            if (field.check()) {
                field.setState(2);
                if (gameListener != null) {
                    gameListener.onVictory();
                }
            }
        } else {
            field.setState(0);
            field.clear();
            field.updateSums();
            field.randomizeVillages();
            gameStarted = false;
        }
    }

    public void requestStop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                try {
                    canvasWidth = canvas.getWidth();
                    canvasHeight = canvas.getHeight();
                    drawBackground(canvas);

                    if (field.getState() == 2) {
                        drawCong(canvas);
                    } else {
                        Geometry g = calcGeometry(canvas);
                        drawNet(canvas, g);
                        drawVillage(canvas, g);
                        drawField(canvas, g);
                        drawDigits(canvas, g);
                    }

                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private void drawBackground(Canvas canvas) {
        switch (themeId) {
            case ThemeManager.THEME_STRIPES:
                int stripe = 40;
                for (int x = 0; x < canvas.getWidth(); x += stripe) {
                    bgPaint.setColor((x / stripe) % 2 == 0 ? Color.LTGRAY : Color.DKGRAY);
                    canvas.drawRect(x, 0, x + stripe, canvas.getHeight(), bgPaint);
                }
                break;
            case ThemeManager.THEME_CHECKER:
                int size = 40;
                for (int y = 0; y < canvas.getHeight(); y += size) {
                    for (int x = 0; x < canvas.getWidth(); x += size) {
                        bgPaint.setColor(((x + y) / size) % 2 == 0 ? Color.WHITE : Color.LTGRAY);
                        canvas.drawRect(x, y, x + size, y + size, bgPaint);
                    }
                }
                break;
            case ThemeManager.THEME_DYNAMIC:
                long t = SystemClock.elapsedRealtime() / 1000;
                int c;
                switch ((int) (t % 3)) {
                    case 0: c = Color.RED; break;
                    case 1: c = Color.GREEN; break;
                    default: c = Color.BLUE; break;
                }
                bgPaint.setColor(c);
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), bgPaint);
                break;
            default:
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), bgPaint);
                break;
        }
    }

    private void drawCong(Canvas canvas) {
        int dw = canvas.getWidth();
        int dh = canvas.getHeight();
        int shift = Math.min(dw, dh) * 10 / 100;

        int cellSize = Math.min((dw - 2 * shift) / field.getWidth(), (dh - 2 * shift) / field.getHeight());

        int fieldSizeX = cellSize * field.getWidth();
        int fieldSizeY = cellSize * field.getHeight();

        int sx, sy;
        if (fieldSizeX == fieldSizeY) {

            sx = (dw - fieldSizeX) / 2;
            sy = (dh - fieldSizeY) / 2;
        } else if (fieldSizeX < fieldSizeY) {
            sx = (dw - fieldSizeX) / 2;
            sy = shift;
        } else {
            int sd = fieldSizeX - fieldSizeY;
            sx = shift;
            sy = (dh - fieldSizeY) / 2;
        }

        int textBox = (int) (Math.min(dw, dh) / 3);

        String congText = "You win!";
        setTextSize(TextPaint, textBox, congText);
        canvas.drawText(congText, (int) ((dw - textBox) / 2), (int) ((dh - textBox) / 2), TextPaint);

    }

    private void drawDigits(Canvas canvas, Geometry g) {
        int cellSize = g.cellSize;
        int sx = g.sx;
        int sy = g.sy;
        int fieldSizeX = cellSize * field.getWidth();
        int textBox = cellSize * 7 / 10;
        for (int col = 0; col < field.getWidth(); col++) {
            int res = 0;
            for (int line = 0; line < field.getHeight(); line++) {
                if (field.getCell(col, line).equals(CellType.RAIL)) {
                    res++;
                }
            }
            Integer value = field.getSumsX()[col];
            if (res == value) {
                digitPaint.setColor(Color.parseColor("#006400"));
            } else if (res > value) {
                digitPaint.setColor(Color.parseColor("#800000"));
            } else {
                digitPaint.setColor(Color.BLACK);
            }
            setTextSize(digitPaint, textBox, value.toString());
            canvas.drawText(value.toString(), sx + col * cellSize + (int) ((cellSize - textBox) / 2), sy - 5, digitPaint);
        }

        for (int line = 0; line < field.getHeight(); line++) {
            int res = 0;
            for (int col = 0; col < field.getWidth(); col++) {
                if (field.getCell(col, line).equals(CellType.RAIL)) {
                    res++;
                }
            }
            Integer value = field.getSumsY()[line];
            if (res == value) {
                digitPaint.setColor(Color.parseColor("#006400"));
            } else if (res > value) {
                digitPaint.setColor(Color.parseColor("#800000"));
            } else {
                digitPaint.setColor(Color.BLACK);
            }
            setTextSize(digitPaint, textBox, value.toString());
            canvas.drawText(value.toString(), sx + fieldSizeX + 5, sy + cellSize * (line + 1) - (int) ((cellSize - textBox) / 2), digitPaint);
        }
    }

    private void drawField(Canvas canvas, Geometry g) {
        int cellSize = g.cellSize;
        int sx = g.sx;
        int sy = g.sy;
        for (int line = 0; line < field.getHeight(); line++) {
            for (int col = 0; col < field.getWidth(); col++) {
                CellType ct = field.getCell(col, line);
                if (ct == CellType.RAIL) {
                    canvas.drawBitmap(scaledRails, sx + cellSize * col, sy + cellSize * line, bgPaint);
                } else if (ct == CellType.MARKED) {
                    int left = sx + cellSize * col;
                    int top = sy + cellSize * line;
                    canvas.drawLine(left, top, left + cellSize, top + cellSize, netPaint);
                    canvas.drawLine(left, top + cellSize, left + cellSize, top, netPaint);
                }
                if (hintCell != null && hintCell.first == col && hintCell.second == line) {
                    canvas.drawRect(sx + cellSize * col, sy + cellSize * line,
                            sx + cellSize * (col + 1), sy + cellSize * (line + 1), hintPaint);
                }
            }
        }
        if (showSolution) {
            for (Pair<Integer, Integer> cell : field.getSolutionPath()) {
                canvas.drawRect(sx + cellSize * cell.first, sy + cellSize * cell.second,
                        sx + cellSize * (cell.first + 1), sy + cellSize * (cell.second + 1), solutionPaint);
            }
        }
    }

    private void drawVillage(Canvas canvas, Geometry g) {
        int cellSize = g.cellSize;
        int sx = g.sx;
        int sy = g.sy;
        int fieldSizeY = cellSize * field.getHeight();
        canvas.drawBitmap(scaledVillage, sx - cellSize - 1, sy + cellSize * field.getVillageA(), bgPaint);
        canvas.drawBitmap(scaledVillage, sx + cellSize * field.getVillageB(), sy + fieldSizeY + 1, bgPaint);

    }

    private void drawNet(Canvas canvas, Geometry g) {
        int cellSize = g.cellSize;
        int sx = g.sx;
        int sy = g.sy;
        int fieldSizeX = cellSize * field.getWidth();
        int fieldSizeY = cellSize * field.getHeight();

        for (int line = 0; line <= field.getHeight(); line++) {
            canvas.drawLine(sx, sy + line * cellSize, sx + fieldSizeX, sy + line * cellSize, netPaint);
        }
        for (int col = 0; col <= field.getWidth(); col++) {
            canvas.drawLine(sx + col * cellSize, sy, sx + col * cellSize, sy + fieldSizeY, netPaint);
        }
    }
}
