package com.itsamsung.traintracks;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ThemeStoreActivity extends Activity {
    private TextView coinsView;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private int tapCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_store);
        coinsView = findViewById(R.id.coins);
        listView = findViewById(R.id.theme_list);
        coinsView.setOnClickListener(v -> {
            tapCount++;
            if (tapCount >= 10) {
                ThemeManager.setCoins(this, 1000000);
                updateCoins();
                Toast.makeText(this, "Секрет активирован", Toast.LENGTH_SHORT).show();
            }
        });
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, buildItems());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> onItem(position));
        updateCoins();
    }

    private void updateCoins() {
        coinsView.setText("Монеты: " + ThemeManager.getCoins(this) + " \uD83E\uDE99");
        adapter.clear();
        adapter.addAll(buildItems());
        adapter.notifyDataSetChanged();
    }

    private List<String> buildItems() {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < ThemeManager.themeCount(); i++) {
            String name = ThemeManager.getName(i);
            if (ThemeManager.isOwned(this, i)) {
                if (ThemeManager.getTheme(this) == i) name += " (выбрано)";
                else name += " (куплено)";
            } else {
                name += " - " + ThemeManager.getCost(i) + " \uD83E\uDE99";
            }
            items.add(name);
        }
        return items;
    }

    private void onItem(int position) {
        if (ThemeManager.isOwned(this, position)) {
            ThemeManager.setTheme(this, position);
            updateCoins();
            finish();
        } else {
            int cost = ThemeManager.getCost(position);
            int coins = ThemeManager.getCoins(this);
            if (coins >= cost) {
                new AlertDialog.Builder(this)
                        .setTitle("Купить тему?")
                        .setMessage("Стоимость: " + cost + " \uD83E\uDE99")
                        .setPositiveButton("Купить", (d, w) -> {
                            ThemeManager.addCoins(this, -cost);
                            ThemeManager.unlock(this, position);
                            ThemeManager.setTheme(this, position);
                            updateCoins();
                            finish();
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            } else {
                Toast.makeText(this, "Недостаточно монет", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
