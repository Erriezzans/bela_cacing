package com.erriezzans.belacacing;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Pet {
    static final String[] MENU =
        {"FEED", "PLAY", "TRAIN", "CLEAN", "SLEEP", "HEAL"};
    static final long MIN = 60000L;
    static final long SHOW = 9000L;

    static class St {
        int hung = 4, joy = 4, str = 2, poop, heals, mode, menu, rx;
        boolean sleeping, tired, sick;
        long lt, born, sleepUntil, msgT;
        String msg = "";
    }

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("worm1", 0);
    }

    static St load(Context c) {
        SharedPreferences p = sp(c);
        St s = new St();
        s.hung = p.getInt("hung", 4);
        s.joy = p.getInt("joy", 4);
        s.str = p.getInt("str", 2);
        s.poop = p.getInt("poop", 0);
        s.heals = p.getInt("heals", 0);
        s.mode = p.getInt("mode", 0);
        s.menu = p.getInt("menu", 0);
        s.rx = p.getInt("rx", 0);
        s.sleeping = p.getBoolean("sleeping", false);
        s.tired = p.getBoolean("tired", false);
        s.sick = p.getBoolean("sick", false);
        s.lt = p.getLong("lt", 0);
        s.born = p.getLong("born", 0);
        s.sleepUntil = p.getLong("sleepUntil", 0);
        s.msgT = p.getLong("msgT", 0);
        s.msg = p.getString("msg", "");
        return s;
    }

    static void save(Context c, St s) {
        sp(c).edit()
            .putInt("hung", s.hung).putInt("joy", s.joy)
            .putInt("str", s.str).putInt("poop", s.poop)
            .putInt("heals", s.heals).putInt("mode", s.mode)
            .putInt("menu", s.menu).putInt("rx", s.rx)
            .putBoolean("sleeping", s.sleeping)
            .putBoolean("tired", s.tired)
            .putBoolean("sick", s.sick)
            .putLong("lt", s.lt).putLong("born", s.born)
            .putLong("sleepUntil", s.sleepUntil)
            .putLong("msgT", s.msgT).putString("msg", s.msg)
            .apply();
    }

    static St current(Context c) {
        St s = load(c);
        long now = System.currentTimeMillis();
        if (s.born == 0) { s.born = now; s.lt = now; }
        tick(s, now);
        save(c, s);
        return s;
    }

    static void say(St s, String m, int rx, long now) {
        s.msg = m;
        s.rx = rx;
        s.msgT = now;
    }

    static void tick(St s, long now) {
        if (s.lt == 0 || s.lt > now) { s.lt = now; return; }
        long gap = (now - s.lt) / MIN;
        long steps = Math.min(gap, 10080L);
        Calendar cal = Calendar.getInstance();
        for (long i = 1; i <= steps; i++) {
            long t = s.lt + i * MIN;
            cal.setTimeInMillis(t);
            int hr = cal.get(Calendar.HOUR_OF_DAY);
            int mn = cal.get(Calendar.MINUTE);
            boolean night = hr >= 21 || hr < 7;
            if (hr == 7 && mn == 0) {
                s.sleeping = false;
                s.tired = false;
                s.sleepUntil = 0;
            }
            if (s.sleepUntil > 0 && t >= s.sleepUntil) {
                s.sleepUntil = 0;
                if (!night) s.sleeping = false;
            }
            if (hr == 21 && mn == 0 && !s.sleeping) s.tired = true;
            if (s.sleeping) continue;
            long m = t / MIN;
            if (m % 45 == 0 && s.hung > 0) s.hung--;
            if (m % 50 == 0 && s.joy > 0) s.joy--;
            if (m % 90 == 0 && s.str > 0) s.str--;
            if (m % 120 == 0 && s.poop < 4) {
                s.poop++;
                if (s.poop == 4) { s.sick = true; s.heals = 0; }
            }
        }
        s.lt = gap > steps ? now : s.lt + steps * MIN;
    }

    static void press(Context c, String b) {
        St s = current(c);
        long now = System.currentTimeMillis();
        s.msg = "";
        s.rx = 0;
        if (s.mode == 0) {
            if (b.equals("A")) {
                s.mode = 1;
                s.menu = 0;
            } else if (b.equals("B")) {
                say(s, new SimpleDateFormat("HH:mm", Locale.US)
                    .format(new Date(now)), 0, now);
            }
        } else {
            if (b.equals("A")) s.menu = (s.menu + 1) % MENU.length;
            else if (b.equals("C")) s.mode = 0;
            else choose(s, now);
        }
        save(c, s);
        WormWidget.refresh(c);
        if (!s.msg.isEmpty()) WormWidget.later(c, SHOW + 500);
    }

    static void choose(St s, long now) {
        switch (s.menu) {
            case 0:
                if (s.sick) {
                    say(s, "No thanks", 3, now);
                } else if (s.hung >= 4) {
                    say(s, "Full!", 2, now);
                } else {
                    s.hung++;
                    say(s, "Yum!", 1, now);
                }
                break;
            case 1:
                if (s.sleeping) {
                    say(s, "Shh, asleep", 0, now);
                } else if (s.sick) {
                    say(s, "Not today", 3, now);
                } else {
                    boolean win = Math.random() < 0.5;
                    s.joy = Math.min(4, s.joy + (win ? 2 : 1));
                    say(s, win ? "Love it!" : "Hmm?", win ? 4 : 5, now);
                }
                break;
            case 2:
                if (s.sleeping || s.sick) {
                    say(s, "Not now", 3, now);
                } else if (s.hung < 1) {
                    say(s, "Too hungry", 3, now);
                } else {
                    s.hung--;
                    s.str = Math.min(4, s.str + 1);
                    say(s, "Gainz!", 6, now);
                }
                break;
            case 3:
                if (s.poop > 0) say(s, "Clean!", 4, now);
                else say(s, "Nothing", 7, now);
                s.poop = 0;
                break;
            case 4:
                if (s.sleeping) {
                    s.sleeping = false;
                    s.sleepUntil = 0;
                    say(s, "Awake", 0, now);
                } else {
                    int hr = Calendar.getInstance()
                        .get(Calendar.HOUR_OF_DAY);
                    boolean night = hr >= 21 || hr < 7;
                    s.sleeping = true;
                    s.tired = false;
                    s.sleepUntil = night ? 0 : now + 180 * MIN;
                    say(s, "Zzz...", 0, now);
                }
                break;
            default:
                if (!s.sick) {
                    say(s, "Healthy", 0, now);
                } else if (++s.heals >= 2) {
                    s.sick = false;
                    s.heals = 0;
                    say(s, "Healed!", 4, now);
                } else {
                    say(s, "Dose 1/2", 0, now);
                }
        }
        s.mode = 0;
    }

    static String hearts(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 4; i++) b.append(i < n ? "\u2665" : "\u2661");
        return b.toString();
    }

    static String mood(St s) {
        if (s.sleeping) return "Asleep";
        if (s.sick) return "Sick";
        if (s.hung == 0 || s.poop >= 3) return "Angry";
        if (s.hung <= 1) return "Hungry";
        if (s.poop > 0) return "Messy";
        if (s.joy <= 1) return "Sad";
        if (s.tired) return "Sleepy";
        if (s.hung >= 3 && s.joy >= 3) return "Happy";
        return "Okay";
    }

    static void add(StringBuilder b, String t) {
        if (b.length() > 0) b.append(", ");
        b.append(t);
    }

    static String needs(St s) {
        StringBuilder b = new StringBuilder();
        if (s.hung <= 1) add(b, "Apple");
        if (s.joy <= 1) add(b, "Play");
        if (s.str == 0) add(b, "Train");
        if (s.poop > 0) add(b, "Clean");
        if (s.sick) add(b, "Heal");
        if (s.tired && !s.sleeping) add(b, "Sleep");
        return b.toString();
    }

    static String panel(St s, long now) {
        long day = (now - s.born) / 86400000L + 1;
        String l1 = "Hunger " + hearts(s.hung) + "  Joy " + hearts(s.joy)
            + "  Str " + hearts(s.str);
        String l2 = "Day " + day + "    Mood: " + mood(s);
        String l3;
        if (s.mode != 0) {
            l3 = "A: next    B: pick    C: back";
        } else if (now - s.msgT < SHOW && !s.msg.isEmpty()) {
            l3 = s.msg;
        } else {
            String n = needs(s);
            l3 = n.isEmpty() ? "All good" : "Needs: " + n;
        }
        return l1 + "\n" + l2 + "\n" + l3;
    }

    static String detail(St s, long now) {
        return panel(s, now);
    }
}
