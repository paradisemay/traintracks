package com.itsamsung.traintracks;

import android.support.annotation.NonNull;

public enum CellType {
    RAIL, EMPTY, MARKED;

    public int toInt(){
        switch (this) {
            case RAIL: return 1;
            case EMPTY: return 0;
            case MARKED: return 2;
            default: return -1;
        }
    }
}
