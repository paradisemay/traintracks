package com.itsamsung.traintracks;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.content.Intent;


public class MainActivity extends AppCompatActivity implements DrawView.GameListener {
    private static final String LOG_TAG = "MainActivity";
    private int height = 15;
    private int width = 15;
    private Field field;
    private DrawView drawView;
    private int hintsLeft = 3;
    private SharedPreferences prefs;
    private boolean timerRunning = false;
    private String difficultyKey = "medium";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        if (savedInstanceState != null) {
            width = savedInstanceState.getInt("width");
            height = savedInstanceState.getInt("height");
            hintsLeft = savedInstanceState.getInt("hintsLeft", 3);
            field = new Field(height, width);
            drawView = new DrawView(this, field, hintsLeft, this);
            setContentView(drawView);
            assignDifficultyKey();
        } else {
            if (prefs.getBoolean("has_saved_game", false)) {
                new AlertDialog.Builder(this)
                        .setTitle("Продолжить игру?")
                        .setMessage("Найдена сохранённая партия. Восстановить её?")
                        .setPositiveButton("Да", (d, w) -> loadGame())
                        .setNegativeButton("Нет", (d, w) -> {
                            prefs.edit().putBoolean("has_saved_game", false).apply();
                            startNewGame();
                        })
                        .setCancelable(false)
                        .show();
            } else {
                startNewGame();
            }
        }
    }

    private void startNewGame() {
        boolean firstRun = prefs.getBoolean("isFirstRun", true);
        if (firstRun) {
            new AlertDialog.Builder(this)
                    .setTitle("Правила")
                    .setMessage("Соедините деревни единственной железной дорогой, используя подсказки по краям.")
                    .setPositiveButton("Понял", (d, w) -> {
                        prefs.edit().putBoolean("isFirstRun", false).apply();
                        showDifficultyDialog();
                    })
                    .setCancelable(false)
                    .show();
        } else {
            showDifficultyDialog();
        }
    }

    private void showDifficultyDialog() {
        final String[] levels = {"Лёгкий", "Средний", "Сложный"};
        new AlertDialog.Builder(this)
                .setTitle("Выбор сложности")
                .setItems(levels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                width = 10;
                                height = 10;
                                break;
                            case 1:
                                width = 15;
                                height = 15;
                                break;
                            case 2:
                                width = 20;
                                height = 20;
                                break;
                        }
                        hintsLeft = 3;
                        field = new Field(height, width);
                        drawView = new DrawView(MainActivity.this, field, hintsLeft, MainActivity.this);
                        setContentView(drawView);
                        assignDifficultyKey();
                        timerRunning = false;
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_change_difficulty) {
            if (field != null) {
                field.clear();
                hintsLeft = 3;
                drawView = new DrawView(this, field, hintsLeft, this);
                setContentView(drawView);
                assignDifficultyKey();
                timerRunning = false;
                drawView.applyTheme();
            }
            showDifficultyDialog();
            return true;
        } else if (item.getItemId() == R.id.menu_theme_store) {
            startActivity(new Intent(this, ThemeStoreActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (field == null) return;
        for (int line = 0; line < field.getHeight(); line++) {
            for (int col = 0; col < field.getWidth(); col++) {
                outState.putInt(String.format("Field%s%s", col, line), field.getCell(col, line).toInt());
            }
        }
        for (int col = 0; col < field.getWidth(); col++) {
            outState.putInt(String.format("SumsX%s", col), field.getSumsX()[col]);
        }
        for (int line = 0; line < field.getHeight(); line++) {
            outState.putInt(String.format("SumsY%s", line), field.getSumsY()[line]);
        }
        outState.putInt("villageA", field.getVillageA());
        outState.putInt("villageB", field.getVillageB());
        Log.v(LOG_TAG, "SaveState=" + field.getState());
        outState.putInt("state", field.getState());
        outState.putInt("width", field.getWidth());
        outState.putInt("height", field.getHeight());
        if (drawView != null) {
            outState.putInt("hintsLeft", drawView.getHintsLeft());
        }
        outState.putInt("path_size", field.getSolutionPath().size());
        for (int i = 0; i < field.getSolutionPath().size(); i++) {
            outState.putInt("pathX" + i, field.getSolutionPath().get(i).first);
            outState.putInt("pathY" + i, field.getSolutionPath().get(i).second);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        for (int line = 0; line < field.getHeight(); line++) {
            for (int col = 0; col < field.getWidth(); col++) {
                int val = savedInstanceState.getInt(String.format("Field%s%s", col, line));
                CellType ct;
                if (val == 0) {
                    ct = CellType.EMPTY;
                } else if (val == 2) {
                    ct = CellType.MARKED;
                } else {
                    ct = CellType.RAIL;
                }
                field.putCell(col, line, ct);
            }
        }
        for (int col = 0; col < field.getWidth(); col++) {
            field.putSumsX(col, savedInstanceState.getInt(String.format("SumsX%s", col)));
        }
        for (int line = 0; line < field.getHeight(); line++) {
            field.putSumsY(line, savedInstanceState.getInt(String.format("SumsY%s", line)));
        }
        field.setVillages(savedInstanceState.getInt("villageA"),
                savedInstanceState.getInt("villageB"));
        field.setState(savedInstanceState.getInt("state"));
        field.getSolutionPath().clear();
        int psize = savedInstanceState.getInt("path_size", 0);
        for (int i = 0; i < psize; i++) {
            int x = savedInstanceState.getInt("pathX" + i);
            int y = savedInstanceState.getInt("pathY" + i);
            field.getSolutionPath().add(new Pair<>(x, y));
        }
        Log.v(LOG_TAG, "state=" + field.getState());
    }

    private void assignDifficultyKey() {
        if (width == 10) difficultyKey = "easy";
        else if (width == 15) difficultyKey = "medium";
        else difficultyKey = "hard";
    }

    private void saveGame() {
        if (field == null || drawView == null) return;
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean("has_saved_game", true);
        ed.putInt("save_width", field.getWidth());
        ed.putInt("save_height", field.getHeight());
        ed.putInt("save_hints", drawView.getHintsLeft());
        for (int line = 0; line < field.getHeight(); line++) {
            for (int col = 0; col < field.getWidth(); col++) {
                ed.putInt(String.format("save_Field%s%s", col, line), field.getCell(col, line).toInt());
            }
        }
        for (int col = 0; col < field.getWidth(); col++) {
            ed.putInt(String.format("save_SumsX%s", col), field.getSumsX()[col]);
        }
        for (int line = 0; line < field.getHeight(); line++) {
            ed.putInt(String.format("save_SumsY%s", line), field.getSumsY()[line]);
        }
        ed.putInt("save_villageA", field.getVillageA());
        ed.putInt("save_villageB", field.getVillageB());
        ed.putInt("save_state", field.getState());
        ed.putInt("save_path_size", field.getSolutionPath().size());
        for (int i = 0; i < field.getSolutionPath().size(); i++) {
            ed.putInt("save_pathX" + i, field.getSolutionPath().get(i).first);
            ed.putInt("save_pathY" + i, field.getSolutionPath().get(i).second);
        }
        ed.putLong("save_timer_base", drawView.getChronometer().getBase());
        ed.putBoolean("save_timer_running", timerRunning);
        ed.apply();
    }

    private void loadGame() {
        width = prefs.getInt("save_width", 15);
        height = prefs.getInt("save_height", 15);
        hintsLeft = prefs.getInt("save_hints", 3);
        field = new Field(height, width);
        drawView = new DrawView(this, field, hintsLeft, this);
        setContentView(drawView);
        assignDifficultyKey();
        for (int line = 0; line < field.getHeight(); line++) {
            for (int col = 0; col < field.getWidth(); col++) {
                int val = prefs.getInt(String.format("save_Field%s%s", col, line), 0);
                CellType ct;
                if (val == 0) ct = CellType.EMPTY;
                else if (val == 2) ct = CellType.MARKED;
                else ct = CellType.RAIL;
                field.putCell(col, line, ct);
            }
        }
        for (int col = 0; col < field.getWidth(); col++) {
            field.putSumsX(col, prefs.getInt(String.format("save_SumsX%s", col), 0));
        }
        for (int line = 0; line < field.getHeight(); line++) {
            field.putSumsY(line, prefs.getInt(String.format("save_SumsY%s", line), 0));
        }
        field.setVillages(prefs.getInt("save_villageA", 0), prefs.getInt("save_villageB", 0));
        field.setState(prefs.getInt("save_state", 1));
        field.getSolutionPath().clear();
        int psize = prefs.getInt("save_path_size", 0);
        for (int i = 0; i < psize; i++) {
            int x = prefs.getInt("save_pathX" + i, 0);
            int y = prefs.getInt("save_pathY" + i, 0);
            field.getSolutionPath().add(new Pair<>(x, y));
        }
        long base = prefs.getLong("save_timer_base", SystemClock.elapsedRealtime());
        drawView.getChronometer().setBase(base);
        timerRunning = prefs.getBoolean("save_timer_running", false);
        if (timerRunning) {
            drawView.getChronometer().start();
        }
    }

    @Override
    public void onFirstRail() {
        if (!timerRunning && drawView != null) {
            drawView.getChronometer().setBase(SystemClock.elapsedRealtime());
            drawView.getChronometer().start();
            timerRunning = true;
        }
    }

    @Override
    public void onVictory() {
        if (timerRunning && drawView != null) {
            drawView.getChronometer().stop();
            long time = SystemClock.elapsedRealtime() - drawView.getChronometer().getBase();
            timerRunning = false;
            String key = "best_" + difficultyKey;
            long best = prefs.getLong(key, Long.MAX_VALUE);
            if (time < best) {
                prefs.edit().putLong(key, time).apply();
                best = time;
            }
            int reward = difficultyKey.equals("easy") ? 1 : difficultyKey.equals("medium") ? 2 : 3;
            ThemeManager.addCoins(this, reward);
            int total = ThemeManager.getCoins(this);
            String msg = String.format("Награда: +%d \uD83E\uDE99 (Всего: %d)\nВремя: %d сек\nРекорд: %d сек", reward, total, time / 1000, best / 1000);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Победа!")
                    .setMessage(msg)
                    .setPositiveButton("OK", null)
                    .setNegativeButton("Показать путь", null)
                    .show();
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> {
                drawView.toggleSolution();
                CharSequence text = dialog.getButton(DialogInterface.BUTTON_NEGATIVE).getText();
                if ("Показать путь".contentEquals(text)) {
                    dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setText("Скрыть путь");
                } else {
                    dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setText("Показать путь");
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (drawView != null) {
            drawView.applyTheme();
        }
    }
}