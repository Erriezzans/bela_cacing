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

public class WormWidget extends AppWidgetProvider {
    static final String ACT = "com.erriezzans.belacacing.ACTION";
    static final String REF = "com.erriezzans.belacacing.REFRESH";

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        refresh(c);
    }

    @Override
    public void onReceive(final Context c, Intent i) {
        super.onReceive(c, i);
        String a = i.getAction();
        if (ACT.equals(a)) {
            final String b = i.getStringExtra("b");
            final PendingResult pr = goAsync();
            new Thread(() -> {
                try {
                    boolean react = Pet.press(c, b);
                    if (react) {
                        Thread.sleep(Pet.SHOW + 300);
                        refresh(c);
                    }
                } catch (Exception e) {
                    // ignore
                } finally {
                    pr.finish();
                }
            }).start();
        } else if (REF.equals(a)) {
            refresh(c);
        }
    }

    static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        ComponentName cn = new ComponentName(c, WormWidget.class);
        for (int id : m.getAppWidgetIds(cn)) {
            m.updateAppWidget(id, build(c));
        }
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

    static int[] seq(Context c, String m) {
        int[] f = new int[8];
        for (int i = 0; i < 8; i++) f[i] = rid(c, "an_" + m + "_" + (i % 4));
        return f;
    }

    static int[] frames(Context c, Pet.St s, long now) {
        String[] r = {"", "eat", "full", "refuse", "love",
            "confused", "workout", "question"};
        if (now - s.msgT < Pet.SHOW && s.rx > 0 && s.rx < r.length) {
            return seq(c, r[s.rx]);
        }
        String m = Pet.mood(s);
        if (m.equals("Asleep")) return seq(c, "sleep");
        if (m.equals("Sick")) return seq(c, "hide");
        if (m.equals("Angry")) return seq(c, "angry");
        if (m.equals("Hungry")) return seq(c, "hungry");
        if (m.equals("Messy")) return seq(c, "poop");
        if (m.equals("Sad")) return seq(c, "sad");
        if (m.equals("Happy")) {
            int[] f = new int[8];
            for (int i = 0; i < 8; i++) f[i] = rid(c, "w_bounce" + (i + 1));
            return f;
        }
        int[] f = seq(c, "idle");
        f[5] = rid(c, "w_blink");
        return f;
    }

    static RemoteViews build(Context c) {
        Pet.St s = Pet.current(c);
        long now = System.currentTimeMillis();
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        int[] ids = {R.id.f1, R.id.f2, R.id.f3, R.id.f4,
            R.id.f5, R.id.f6, R.id.f7, R.id.f8};
        int[] f = frames(c, s, now);
        for (int i = 0; i < 8; i++) v.setImageViewResource(ids[i], f[i]);
        v.setViewVisibility(R.id.menu_tag,
            s.mode == 1 ? View.VISIBLE : View.GONE);
        if (s.mode == 1) {
            v.setTextViewText(R.id.menu_tag, "> " + Pet.MENU[s.menu]);
        }
        v.setTextViewText(R.id.stat_h, Pet.col("Hunger", s.hung));
        v.setTextViewText(R.id.stat_j, Pet.col("Joy", s.joy));
        v.setTextViewText(R.id.stat_s, Pet.col("Str", s.str));
        v.setTextViewText(R.id.info, Pet.info(s, now));
        v.setTextViewText(R.id.note, Pet.note(s, now));
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
