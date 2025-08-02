// de/bjusystems/vdrmanager/data/Timer.java
package de.bjusystems.vdrmanager.ng.data;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.content.Context;
import android.text.TextUtils;
import de.bjusystems.vdrmanager.ng.StringUtils;
import de.bjusystems.vdrmanager.ng.app.C;
import de.bjusystems.vdrmanager.ng.gui.Utils;
import de.bjusystems.vdrmanager.ng.data.Preferences;

/**
 * Class for timer data
 */
public class Timer extends Event implements Timerable {

    private static final int ACTIVE = 1;
    private static final int INSTANT = 2;
    private static final int VPS = 4;
    private static final int RECORDING = 8;

    private int number;
    private int flags;
    private int priority;
    private int lifetime;
    private String searchtimer = "";	
    private String weekdays = "-------";
    private boolean conflict;
    private long vps; // VPS-Zeit in ms

    public Timer() {
        // Parameterloser Konstruktor für neue Timer
        this.flags = ACTIVE;
        this.priority = 5;
        this.lifetime = 99;
        this.start = new Date();
        this.stop = new Date(start.getTime() + 3600000); // +1 Stunde
        this.weekdays = "-------";
    }

    /**
     * Constructs a timer from SvdrpHelper result line
     */
    public Timer(final String timerData) {
        final String[] values = StringUtils.splitPreserveAllTokens(timerData, C.DATA_SEPARATOR);

        this.number = Integer.valueOf(values[0].substring(1));
        this.flags = Integer.valueOf(values[1]);
        this.channelNumber = Long.valueOf(values[2]);
        this.channelName = Utils.mapSpecialChars(values[3]);

        this.start = new Date(Long.parseLong(values[4]) * 1000);
        this.stop = new Date(Long.parseLong(values[5]) * 1000);

        this.priority = Integer.valueOf(values[6]);
        this.lifetime = Integer.valueOf(values[7]);

        this.title = Utils.mapSpecialChars(values[8]);

        this.searchtimer = Utils.mapSpecialChars(values[9]);
        if (this.searchtimer.split("<searchtimer>").length > 1) {
            String extracted = this.searchtimer.split("<searchtimer>")[1].split("</searchtimer>")[0].trim();
            this.searchtimer = extracted.isEmpty() ? null : extracted;
        }

        this.description = values.length > 9 ? values[9] : "";
        this.shortText = values.length > 10 ? Utils.mapSpecialChars(values[10]) : "";

        if (values.length > 11) {
            this.description = values[11];
        }
        if (values.length > 12) {
            this.channelId = values[12];
        }
        if (values.length > 13) {
            this.weekdays = values[13];
        }
        if (values.length > 14) {
            this.conflict = values[14].equals("1");
        }

        description = Utils.mapSpecialChars(description);

        if (values.length > 15 && !TextUtils.isEmpty(values[15])) {
            this.vps = Long.valueOf(values[15]) * 1000;
        } else if (isVps()) {
            this.vps = start.getTime();
        }
    }

    public Timer(final Event event) {
        this(); // Nutzt den parameterlosen Konstruktor
        this.channelNumber = event.getChannelNumber();
        this.channelName = event.getChannelName();
        this.channelId = event.getChannelId();
        this.start = new Date(event.getStart().getTime());
        this.stop = new Date(event.getStop().getTime());
        this.title = event.getTitle();
        if (Utils.isSerie(event.getContent())) {
            if (!TextUtils.isEmpty(event.getShortText())) {
                this.title += "~" + event.getShortText();
            }
        }
        this.description = event.getDescription();
        this.vps = event.getVPS();
    }

    public Timer copy() {
        Timer t = new Timer();
        t.number = this.number;
        t.flags = this.flags;
        t.channelNumber = this.channelNumber;
        t.channelName = this.channelName;
        t.channelId = this.channelId;
        t.start = new Date(this.start.getTime());
        t.stop = new Date(this.stop.getTime());
        t.priority = this.priority;
        t.lifetime = this.lifetime;
        t.title = this.title;
        t.description = this.description;
        t.shortText = this.shortText;
        t.searchtimer = this.searchtimer;
        t.weekdays = this.weekdays;
        t.conflict = this.conflict;
        t.vps = this.vps;
        return t;
    }

    public static Timer createEmpty() {
        return new Timer();
    }
    
    /**
     * Gibt den aktuellen Zustand des Timers zurück (Active, Inactive, Recording).
     *
     * @return der TimerState
     */
    public TimerState getTimerState() {
        if (isEnabled()) {
            if (isRecording()) {
                return TimerState.Recording;
            }
            return TimerState.Active;
        }
        return TimerState.Inactive;
    }

    // Getter & Setter
    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getLifetime() { return lifetime; }
    public void setLifetime(int lifetime) { this.lifetime = lifetime; }

    public String getSearchtimer() { return searchtimer; }
    public void setSearchtimer(String searchtimer) { this.searchtimer = searchtimer; }

    public String getWeekdays() { return weekdays; }
    public void setWeekdays(String weekdays) { this.weekdays = weekdays; }

    public boolean isRecurring() { return !weekdays.equals("-------"); }

    public boolean isEnabled() { return (flags & ACTIVE) == ACTIVE; }
    public void setEnabled(boolean enabled) {
        if (enabled) flags |= ACTIVE;
        else flags &= ~ACTIVE;
    }

    public boolean isInstant() { return (flags & INSTANT) == INSTANT; }
    public boolean isVps() { return (flags & VPS) == VPS; }
    public void setVps(boolean useVps) {
        if (useVps) flags |= VPS;
        else flags &= ~VPS;
    }

    public boolean isRecording() { return (flags & RECORDING) == RECORDING; }
    public boolean isConflict() { return conflict; }

    public void setStart(Date start) { this.start = start; }
    public void setStop(Date stop) { this.stop = stop; }

    public Timer getTimer() { return this; }
    public Timer createTimer() { return new Timer(this); }

    @Override
    public TimerMatch getTimerMatch() {
        return isConflict() ? TimerMatch.Conflict : TimerMatch.Full;
    }

    public String toCommandLine() {
        final StringBuilder line = new StringBuilder();
        line.append(flags).append(":");
        line.append(channelNumber).append(":");

        Calendar cal = Calendar.getInstance();
        cal.setTime(isVps() ? new Date(vps) : start);
        cal.setTimeZone(TimeZone.getTimeZone(Preferences.get().getCurrentVdr().getServerTimeZone()));

        line.append((!weekdays.equals("-------") ? weekdays + "@" : ""))
            .append(String.format("%04d-%02d-%02d:", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)))
            .append(String.format("%02d%02d:", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));

        cal.setTime(stop);
        line.append(String.format("%02d%02d:", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)))
            .append(priority).append(":")
            .append(lifetime).append(":")
            .append(Utils.unMapSpecialChars(title));
        return line.toString();
    }
}

