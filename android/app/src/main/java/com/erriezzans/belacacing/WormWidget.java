package com.erriezzans.belacacing;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WormWidget extends AppWidgetProvider {
    static final String ACT = "com.erriezzans.belacacing.ACTION";
    static final String REF = "com.erriezzans.belacacing.REFRESH";
    static final ExecutorService PRESS = Executors.newSingleThreadExecutor();
    static final ScheduledExecutorService SCH =
        Executors.newSingleThreadScheduledExecutor();
    static volatile String lastKey = null;

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        lastKey = null;
        refresh(c);
    }

    @Override
    public void onReceive(final Context c, Intent i) {
        super.onReceive(c, i);
        String a = i.getAction();
        if (ACT.equals(a)) {
            final String b = i.getStringExtra("b");
            final PendingResult pr = goAsync();
            PRESS.execute(() -> {
                boolean react = false;
                try {
                    react = Pet.press(c, b);
                } catch (Exception e) {
                    // ignore
                }
                if (react) {
                    SCH.schedule(() -> {
                        try {
                            refresh(c);
                        } finally {
                            pr.finish();
                        }
                    }, Pet.SHOW + 300, TimeUnit.MILLISECONDS);
                } else {
                    pr.finish();
                }
            });
        } else if (REF.equals(a)) {
            refresh(c);
        }
    }

    static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        ComponentName cn = new ComponentName(c, WormWidget.class);
        int[] ids = m.getAppWidgetIds(cn);
        if (ids.length == 0) return;
        Pet.St s = Pet.current(c);
        long now = System.currentTimeMillis();
        int[] f = frames(c, s, now);
        String key = Arrays.toString(f);
        boolean same = key.equals(lastKey);
        RemoteViews v = build(c, s, now, f, !same);
        for (int id : ids) {
            if (same) m.partiallyUpdateAppWidget(id, v);
            else m.updateAppWidget(id, v);
        }
        lastKey = key;
    }

    static void later(Context c, long delayMs) {
        AlarmManager am =
            (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(c, WormWidget.class).setAction(REF);
        PendingIntent pi = PendingIntent.getBroadcast(c, 9, i,
            PendingIntent.FLAG_UPDATE_CURRENT
            | PendingIntent.FLAG_IMMUTABLE);
        am.set(AlarmManager.RTC, System.currentTimeMillis() + delayMs, pi);
    }

    static int rid(Context c, String name) {
        return c.getResources()
            .getIdentifier(name, "drawable", c.getPackageName());
    }

    static int[] same(int id) {
        int[] f = new int[8];
        Arrays.fill(f, id);
        return f;
    }

    static int[] mix(Context c, String m, boolean fast) {
        int ex = rid(c, "ex_" + m);
        int[] a = new int[4];
        for (int i = 0; i < 4; i++) a[i] = rid(c, "an_" + m + "_" + i);
        int[] f = new int[8];
        if (ex == 0) {
            for (int i = 0; i < 8; i++) f[i] = a[i % 4];
        } else if (fast) {
            for (int i = 0; i < 8; i++) {
                f[i] = (i % 2 == 0) ? a[(i / 2) % 4] : ex;
            }
        } else {
            f = new int[]{a[0], a[1], a[2], ex, ex, a[3], a[0], a[1]};
        }
        return f;
    }

    static int[] frames(Context c, Pet.St s, long now) {
        if (s.mode == 4) return same(rid(c, "ex_rps_ready"));
        String[] r = {"", "eat", "full", "refuse", "love", "confused",
            "workout", "question", "rock", "paper", "scissor"};
        if (now - s.msgT < Pet.SHOW && s.rx > 0 && s.rx < r.length) {
            if (s.rx >= 8) return same(rid(c, "ex_" + r[s.rx]));
            return mix(c, r[s.rx], true);
        }
        String m = Pet.mood(s);
        if (m.equals("Asleep")) return mix(c, "sleep", false);
        if (m.equals("Sick")) return mix(c, "hide", false);
        if (m.equals("Angry")) return mix(c, "angry", true);
        if (m.equals("Hungry")) return mix(c, "hungry", true);
        if (m.equals("Messy")) return mix(c, "poop", false);
        if (m.equals("Sad")) return mix(c, "sad", false);
        if (m.equals("Happy")) {
            int[] f = new int[8];
            for (int i = 0; i < 8; i++) f[i] = rid(c, "w_bounce" + (i + 1));
            return f;
        }
        int[] f = mix(c, "idle", false);
        f[5] = rid(c, "w_blink");
        return f;
    }

    static RemoteViews build(Context c, Pet.St s, long now,
                             int[] f, boolean withFrames) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        if (withFrames) {
            int[] ids = {R.id.f1, R.id.f2, R.id.f3, R.id.f4,
                R.id.f5, R.id.f6, R.id.f7, R.id.f8};
            for (int i = 0; i < 8; i++) v.setImageViewResource(ids[i], f[i]);
        }
        boolean tag = s.mode == 1 || s.mode == 4;
        v.setViewVisibility(R.id.menu_tag, tag ? View.VISIBLE : View.GONE);
        if (s.mode == 1) {
            v.setTextViewText(R.id.menu_tag, "> " + Pet.MENU[s.menu]);
        } else if (s.mode == 4) {
            v.setTextViewText(R.id.menu_tag, "Pick one!");
        }
        v.setTextViewText(R.id.stat_h, Pet.col("Hunger", s.hung));
        v.setTextViewText(R.id.stat_j, Pet.col("Joy", s.joy));
        v.setTextViewText(R.id.stat_s, Pet.col("Str", s.str));
        v.setTextViewText(R.id.info, Pet.info(s, now));
        v.setTextViewText(R.id.note, Pet.note(s, now));
        String[] lb = Pet.labels(s);
        v.setTextViewText(R.id.btn_a, lb[0]);
        v.setTextViewText(R.id.btn_b, lb[1]);
        v.setTextViewText(R.id.btn_c, lb[2]);
        v.setOnClickPendingIntent(R.id.btn_a, btn(c, "A", 1));
        v.setOnClickPendingIntent(R.id.btn_b, btn(c, "B", 2));
        v.setOnClickPendingIntent(R.id.btn_c, btn(c, "C", 3));
        v.setOnClickPendingIntent(R.id.flip, PendingIntent.getActivity(c, 0,
            new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE));
        return v;
    }

    static PendingIntent btn(Context c, String b, int code) {
        Intent i = new Intent(c, WormWidget.class)
            .setAction(ACT).putExtra("b", b);
        return PendingIntent.getBroadcast(c, code, i,
            PendingIntent.FLAG_UPDATE_CURRENT
            | PendingIntent.FLAG_IMMUTABLE);
    }
}
