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
    public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        if (ACT.equals(i.getAction())) Pet.press(c, i.getStringExtra("b"));
        else if (REF.equals(i.getAction())) refresh(c);
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

    static int[] frames(Pet.St s, long now) {
        int rx = (now - s.msgT < Pet.SHOW) ? s.rx : 0;
        int one = 0;
        switch (rx) {
            case 1: one = R.drawable.w_eat; break;
            case 2: one = R.drawable.w_full; break;
            case 3: one = R.drawable.w_refuse; break;
            case 4: one = R.drawable.w_love; break;
            case 5: one = R.drawable.w_confused; break;
            case 6: one = R.drawable.w_workout; break;
            case 7: one = R.drawable.w_question; break;
            default: break;
        }
        int[] f = new int[8];
        if (one == 0) {
            String m = Pet.mood(s);
            if (m.equals("Asleep")) one = R.drawable.w_sleep;
            else if (m.equals("Sick")) one = R.drawable.w_hide;
            else if (m.equals("Angry")) one = R.drawable.w_angry;
            else if (m.equals("Hungry")) one = R.drawable.w_hungry;
            else if (m.equals("Messy")) one = R.drawable.w_poop;
            else if (m.equals("Sad")) one = R.drawable.w_sad;
            else if (m.equals("Sleepy")) one = R.drawable.w_blink;
            else if (m.equals("Happy")) {
                return new int[]{R.drawable.w_bounce1, R.drawable.w_bounce2,
                    R.drawable.w_bounce3, R.drawable.w_bounce4,
                    R.drawable.w_bounce5, R.drawable.w_bounce6,
                    R.drawable.w_bounce7, R.drawable.w_bounce8};
            } else {
                for (int i = 0; i < 8; i++) f[i] = R.drawable.w_idle;
                f[5] = R.drawable.w_blink;
                return f;
            }
        }
        for (int i = 0; i < 8; i++) f[i] = one;
        return f;
    }

    static RemoteViews build(Context c) {
        Pet.St s = Pet.current(c);
        long now = System.currentTimeMillis();
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        int[] ids = {R.id.f1, R.id.f2, R.id.f3, R.id.f4,
            R.id.f5, R.id.f6, R.id.f7, R.id.f8};
        int[] f = frames(s, now);
        for (int i = 0; i < 8; i++) v.setImageViewResource(ids[i], f[i]);
        v.setViewVisibility(R.id.menu_tag,
            s.mode == 1 ? View.VISIBLE : View.GONE);
        if (s.mode == 1) {
            v.setTextViewText(R.id.menu_tag, "> " + Pet.MENU[s.menu]);
        }
        v.setTextViewText(R.id.panel, Pet.panel(s, now));
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
