package com.itsamsung.traintracks;

import android.content.Context;
import android.os.SystemClock;
import android.util.Pair;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Chronometer;

public class DrawView extends FrameLayout implements SurfaceHolder.Callback {
    private static final int MAX_HINTS = 3;
    private final Field field;
    private final SurfaceView surfaceView;
    private DrawThread drawThread;
    private final Button hintButton;
    private final Button undoButton;
    private final Button redoButton;
    private final Chronometer chronometer;
    private int hintsLeft;
    private final GameListener listener;

    public DrawView(Context context, Field field, int hintsLeft, GameListener listener) {
        super(context);
        this.field = field;
        this.hintsLeft = hintsLeft;
        this.listener = listener;
        surfaceView = new SurfaceView(context);
        surfaceView.getHolder().addCallback(this);
        addView(surfaceView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        hintButton = new Button(context);
        hintButton.setText("Подсказка: " + hintsLeft);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        addView(hintButton, lp);

        undoButton = new Button(context);
        undoButton.setText("↩");
        FrameLayout.LayoutParams up = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        up.gravity = Gravity.BOTTOM | Gravity.START;
        addView(undoButton, up);

        redoButton = new Button(context);
        redoButton.setText("↪");
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        rp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        addView(redoButton, rp);

        chronometer = new Chronometer(context);
        chronometer.setBase(SystemClock.elapsedRealtime());
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        cp.gravity = Gravity.TOP | Gravity.START;
        addView(chronometer, cp);

        surfaceView.setOnTouchListener((v, event) -> {
            if (drawThread != null) {
                drawThread.setTowardPoint((int) event.getX(), (int) event.getY());
            }
            return false;
        });

        hintButton.setOnClickListener(v -> {
            if (hintsLeft <= 0) return;
            if (field.getState() == 0) {
                field.generatePath();
                field.updateSums();
                field.clear();
                field.setState(1);
            }
            Pair<Integer, Integer> cell = field.hint();
            if (cell != null && drawThread != null) {
                drawThread.highlightHint(cell);
                if (listener != null) listener.onFirstRail();
                hintsLeft--;
                hintButton.setText("Подсказка: " + hintsLeft);
                if (hintsLeft == 0) hintButton.setEnabled(false);
                if (field.check()) {
                    field.setState(2);
                    if (listener != null) listener.onVictory();
                }
            }
        });

        undoButton.setOnClickListener(v -> field.undo());
        redoButton.setOnClickListener(v -> field.redo());
    }

    public int getHintsLeft() {
        return hintsLeft;
    }

    public Chronometer getChronometer() {
        return chronometer;
    }

    public void toggleSolution() {
        if (drawThread != null) {
            drawThread.toggleSolution();
        }
    }

    public interface GameListener {
        void onFirstRail();
        void onVictory();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        drawThread = new DrawThread(getContext(), holder);
        drawThread.giveField(field);
        drawThread.giveContext(getContext());
        drawThread.setGameListener(listener);
        drawThread.setTheme(ThemeManager.getTheme(getContext()));
        drawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        drawThread.requestStop();
        boolean retry = true;
        while (retry) {
            try {
                drawThread.join();
                retry = false;
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    public void applyTheme() {
        if (drawThread != null) {
            drawThread.setTheme(ThemeManager.getTheme(getContext()));
        }
    }
}
