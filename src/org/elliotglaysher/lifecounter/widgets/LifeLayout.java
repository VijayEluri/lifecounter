/*
 * Copyright (C) 2009 Elliot Glaysher.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.elliotglaysher.lifecounter.widgets;

import org.elliotglaysher.lifecounter.LifeModel;
import org.elliotglaysher.lifecounter.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LifeLayout extends LinearLayout {
    static private final int MIN_LIFE = -999;
    static private final int MAX_LIFE = 999;
    static private final int SPINNER_SPEED = 300;
    static private final int COMMIT_DELAY_MS = 2000;

    private final LifeView lifeHistory;
    private final NumberPicker spinner;
    private final Handler guiThread;
    private LifeModel life;
    private final TextView nameView;
    private final SharedPreferences sharedPreferences;

    public LifeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.life_layout, this, true);
        sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        guiThread = new Handler();

        // Read the name that's supposed to go on this column
        TypedArray array = context.obtainStyledAttributes(attrs,
                R.styleable.lifeLayout, 0, 0);
        nameView = (TextView) findViewById(R.id.player_name);

        int visibility = array.getBoolean(R.styleable.lifeLayout_nameShown,
                true) ? VISIBLE : GONE;
        nameView.setVisibility(visibility);

        lifeHistory = (LifeView) findViewById(R.id.life_widget);
        spinner = (NumberPicker) findViewById(R.id.spinner_widget);
        spinner.setRange(MIN_LIFE, MAX_LIFE);
        spinner.setSpeed(SPINNER_SPEED);
        spinner.setOnChangeListener(new LifeChangeListener());

        newGame();
    }

    public void newGame() {
        int starting_life = 20;
        try {
            starting_life = Integer.parseInt(sharedPreferences.getString(
                    "starting_life", "20"));
        } catch (Exception e) {
            // Do nothing. If we can't parse this, we already have the correct
            // value.
        }

        life = new LifeModel(starting_life);
        lifeHistory.setModel(life);
        spinner.setCurrent(starting_life);

        lifeHistory.onHistoryChanged();
    }

    public void commitPendingChanges() {
        guiThread.removeCallbacks(commitLifeScore);
        life.commitValue();
    }

    public void setTheme(String theme) {
        lifeHistory.setTheme(theme);
    }

    public void setName(String name) {
        nameView.setText(name);
    }

    public LifeModel getModelForSaving() {
        commitPendingChanges();
        return life;
    }

    public void setModelFromSave(LifeModel in) {
        commitPendingChanges();
        life = in;
        lifeHistory.setModel(life);
        spinner.setCurrent(life.getLife());

        lifeHistory.onHistoryChanged();
    }

    private class LifeChangeListener implements NumberPicker.OnChangedListener {
        public void onChanged(NumberPicker spinner, int oldVal, int newVal) {
            // The default behaviour of NumberPicker is to overflow; prevent
            // us from going over MAX_LIFE or going under MIN_LIFE.
            if (oldVal == MIN_LIFE && newVal == MAX_LIFE) {
                spinner.setCurrent(MIN_LIFE);
            } else if (oldVal == MAX_LIFE && newVal == MIN_LIFE) {
                spinner.setCurrent(MAX_LIFE);
            } else {
                handleNewValue(spinner.getId(), newVal);
            }
        }
    }

    private void handleNewValue(int id, int newVal) {
        life.setLife(newVal);
        lifeHistory.onHistoryChanged();

        guiThread.removeCallbacks(commitLifeScore);
        guiThread.postDelayed(commitLifeScore, COMMIT_DELAY_MS);
    }

    private final Runnable commitLifeScore = new Runnable() {
        public void run() {
            life.commitValue();
        }
    };
}
