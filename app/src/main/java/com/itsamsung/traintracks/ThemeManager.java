package com.itsamsung.traintracks;

import android.content.Context;
import android.content.SharedPreferences;

public class ThemeManager {
    private static final String PREFS = "prefs";
    private static final String COINS = "coins";
    private static final String THEME = "theme";
    private static final String OWN_PREFIX = "theme_owned_";

    public static final int THEME_DARK = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_RED = 2;
    public static final int THEME_BLUE = 3;
    public static final int THEME_GREEN = 4;
    public static final int THEME_YELLOW = 5;
    public static final int THEME_CYAN = 6;
    public static final int THEME_STRIPES = 7;
    public static final int THEME_CHECKER = 8;
    public static final int THEME_DYNAMIC = 9;

    private static final String[] NAMES = {
            "Тёмная", "Белая", "Красная", "Синяя", "Зелёная",
            "Жёлтая", "Голубая", "Полоски", "Шахматы", "Динамическая"
    };

    private static final int[] COSTS = {0,0,3,3,4,4,5,6,7,10};

    public static int themeCount() { return NAMES.length; }
    public static String getName(int id) { return NAMES[id]; }
    public static int getCost(int id) { return COSTS[id]; }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int getCoins(Context ctx) {
        return prefs(ctx).getInt(COINS, 0);
    }

    public static void addCoins(Context ctx, int delta) {
        int c = getCoins(ctx) + delta;
        if (c < 0) c = 0;
        prefs(ctx).edit().putInt(COINS, c).apply();
    }

    public static void setCoins(Context ctx, int value) {
        prefs(ctx).edit().putInt(COINS, value).apply();
    }

    public static int getTheme(Context ctx) {
        return prefs(ctx).getInt(THEME, THEME_DARK);
    }

    public static void setTheme(Context ctx, int id) {
        prefs(ctx).edit().putInt(THEME, id).apply();
    }

    public static boolean isOwned(Context ctx, int id) {
        if (id <= THEME_LIGHT) return true; // первые две бесплатные
        return prefs(ctx).getBoolean(OWN_PREFIX + id, false);
    }

    public static void unlock(Context ctx, int id) {
        prefs(ctx).edit().putBoolean(OWN_PREFIX + id, true).apply();
    }
}
