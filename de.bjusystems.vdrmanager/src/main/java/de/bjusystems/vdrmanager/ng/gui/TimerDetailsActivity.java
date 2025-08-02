// de/bjusystems/vdrmanager/gui/TimerDetailsActivity.java
package de.bjusystems.vdrmanager.ng.gui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import de.bjusystems.vdrmanager.ng.R;
import de.bjusystems.vdrmanager.ng.app.Intents;
import de.bjusystems.vdrmanager.ng.app.VdrManagerApp;
import de.bjusystems.vdrmanager.ng.data.EpgCache;
import de.bjusystems.vdrmanager.ng.data.EventFormatter;
import de.bjusystems.vdrmanager.ng.data.Preferences;
import de.bjusystems.vdrmanager.ng.data.Timer;
import de.bjusystems.vdrmanager.ng.tasks.CreateTimerTask;
import de.bjusystems.vdrmanager.ng.tasks.ModifyTimerTask;
import de.bjusystems.vdrmanager.ng.utils.VdrManagerExceptionHandler;
import de.bjusystems.vdrmanager.ng.utils.date.DateFormatter;
import de.bjusystems.vdrmanager.ng.utils.svdrp.SetTimerClient.TimerOperation;
import de.bjusystems.vdrmanager.ng.utils.svdrp.SvdrpEvent;

import de.bjusystems.vdrmanager.ng.databinding.TimerDetailBinding;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.CompoundButton;

/**
 * Activity zum Bearbeiten oder Erstellen eines Timers.
 */
public class TimerDetailsActivity extends AppCompatActivity
        implements View.OnClickListener, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    public static final int REQUEST_CODE_TIMER_MODIFIED = 34;
    public static final int REQUEST_CODE_TIMER_EDIT = 35;
    public static final int REQUEST_CODE_TIMER_ADD = 36;

    private CharSequence prevStart;
    private CharSequence prevEnd;
    private CharSequence prevDate;
    private boolean editStart;

    private Timer timer;
    private Timer original;

    // View Binding
    private TimerDetailBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(VdrManagerExceptionHandler.get(this,
                Thread.getDefaultUncaughtExceptionHandler()));

        // View Binding initialisieren
        binding = TimerDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // OnClickListener setzen
        binding.timerDetailsCancel.setOnClickListener(this);
        binding.timerDetailDay.setOnClickListener(this);
        binding.timerDetailStart.setOnClickListener(this);
        binding.timerDetailEnd.setOnClickListener(this);
        binding.timerDetailsSave.setOnClickListener(this);
        binding.timerDetailsModify.setOnClickListener(this);
        binding.timerDetailRepeat.setOnClickListener(this);

        // Aktuellen Timer holen
        Timer currentTimer = getApp().getCurrentTimer();
        if (currentTimer != null) {
            timer = currentTimer.copy();
            original = currentTimer.copy();
        } else {
            timer = Timer.createEmpty();
            original = Timer.createEmpty();
        }

        // Operation bestimmen
        int op = getIntent().getIntExtra(Intents.TIMER_OP, Intents.ADD_TIMER);
        if (op == Intents.ADD_TIMER) {
            setTitle(R.string.timer_details_add_title);
            add();
        } else if (op == Intents.EDIT_TIMER) {
            setTitle(R.string.timer_details_modify_title);
            modify();
        } else {
            finish();
            return;
        }

        // VPS-Block sichtbar/nicht sichtbar
        if (!timer.isVps() && !timer.hasVPS()) {
            binding.timerBlock.setVisibility(View.GONE);
        } else {
            binding.timerDetailVps.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    vpsChecked(false);
                } else {
                    vpsUnchecked();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Preferences.init(this);
    }

    protected VdrManagerApp getApp() {
        return (VdrManagerApp) getApplication();
    }

    public void add() {
        updateDisplay(TimerOperation.CREATE);
    }

    public void modify() {
        updateDisplay(TimerOperation.MODIFY);
    }

    private void updateDisplay(TimerOperation op) {
        updateDisplay();

        if (op == TimerOperation.CREATE) {
            binding.timerDetailsModify.setVisibility(View.GONE);
            binding.timerDetailsSave.setVisibility(View.VISIBLE);
            binding.timerDetailsSave.setText(R.string.timer_details_create_title);

            Preferences prefs = Preferences.get();
            binding.timerDetailPriority.setText(String.valueOf(prefs.getTimerDefaultPriority()));
            binding.timerDetailLifetime.setText(String.valueOf(prefs.getTimerDefaultLifetime()));

            Date start = new Date(timer.getStart().getTime() - prefs.getTimerPreMargin() * 60000);
            timer.setStart(start);
            Date end = new Date(timer.getStop().getTime() + prefs.getTimerPostMargin() * 60000);
            timer.setStop(end);
            updateDates(start, end);
        } else if (op == TimerOperation.MODIFY) {
            binding.timerDetailsSave.setVisibility(View.GONE);
            binding.timerDetailsModify.setVisibility(View.VISIBLE);
            binding.timerDetailsSave.setText(R.string.timer_details_save_title);
            binding.timerDetailPriority.setText(String.valueOf(timer.getPriority()));
            binding.timerDetailLifetime.setText(String.valueOf(timer.getLifetime()));

            if (timer.isVps()) {
                vpsChecked(true);
            } else {
                updateDates(timer.getStart(), timer.getStop());
            }
        } else {
            throw new RuntimeException("Unknown Operation: " + op);
        }
    }

    private void updateDisplay() {
        binding.timerDetailChannel.setText(timer.getChannelNumber() + " " + timer.getChannelName());
        EventFormatter f = new EventFormatter(timer, true);
        binding.timerDetailTitle.setText(f.getTitle());
        binding.timerDetailRepeat.setText(getSelectedItems().toString(this, true));
        EpgCache.CACHE.remove(timer.getChannelId());
        EpgCache.NEXT_REFRESH.remove(timer.getChannelId());
        binding.timerDetailVps.setChecked(timer.isVps());
    }

    private void updateDates(Date start, Date stop) {
        DateFormatter startF = new DateFormatter(start);
        DateFormatter endF = new DateFormatter(stop);
        binding.timerDetailStart.setText(startF.getTimeString());
        binding.timerDetailEnd.setText(endF.getTimeString());
        binding.timerDetailDay.setText(startF.getDateString());
    }

    private void vpsChecked(boolean initial) {
        if (!initial) {
            prevStart = binding.timerDetailStart.getText();
            prevEnd = binding.timerDetailEnd.getText();
            prevDate = binding.timerDetailDay.getText();
        }
        DateFormatter formatter = new DateFormatter(original.getStart());
        binding.timerDetailStart.setEnabled(false);
        binding.timerDetailStart.setText(formatter.getTimeString());
        timer.setStart(original.getStart());

        DateFormatter stopF = new DateFormatter(original.getStop());
        binding.timerDetailEnd.setEnabled(false);
        binding.timerDetailEnd.setText(stopF.getTimeString());
        timer.setStop(original.getStop());

        binding.timerDetailDay.setEnabled(false);
        binding.timerDetailDay.setText(formatter.getDateString());
    }

    private void vpsUnchecked() {
        if (prevStart != null) binding.timerDetailStart.setText(prevStart);
        binding.timerDetailStart.setEnabled(true);
        if (prevEnd != null) binding.timerDetailEnd.setText(prevEnd);
        binding.timerDetailEnd.setEnabled(true);
        if (prevDate != null) binding.timerDetailDay.setText(prevDate);
        binding.timerDetailDay.setEnabled(true);
    }

    @Override
    public void onClick(View view) {
        if (view == binding.timerDetailDay) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(timer.getStart());
            new DatePickerDialog(this, this, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        } else if (view == binding.timerDetailStart) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(timer.getStart());
            editStart = true;
            new TimePickerDialog(this, this, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        } else if (view == binding.timerDetailEnd) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(timer.getStop());
            editStart = false;
            new TimePickerDialog(this, this, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        } else if (view == binding.timerDetailsCancel) {
            finish();
        } else if (view == binding.timerDetailsModify) {
            timer.setTitle(binding.timerDetailTitle.getText().toString());
            timer.setVps(binding.timerDetailVps.isChecked());
            timer.setPriority(getIntOr0(binding.timerDetailPriority));
            timer.setLifetime(getIntOr0(binding.timerDetailLifetime));
            modifyTimer(timer);
        } else if (view == binding.timerDetailsSave) {
            timer.setTitle(binding.timerDetailTitle.getText().toString());
            timer.setVps(binding.timerDetailVps.isChecked());
            timer.setPriority(getIntOr0(binding.timerDetailPriority));
            timer.setLifetime(getIntOr0(binding.timerDetailLifetime));
            createTimer(timer);
        } else if (view == binding.timerDetailRepeat) {
            String[] weekdays = new DateFormatSymbols().getWeekdays();
            String[] values = {
                weekdays[Calendar.MONDAY],
                weekdays[Calendar.TUESDAY],
                weekdays[Calendar.WEDNESDAY],
                weekdays[Calendar.THURSDAY],
                weekdays[Calendar.FRIDAY],
                weekdays[Calendar.SATURDAY],
                weekdays[Calendar.SUNDAY],
            };

            DaysOfWeek mNewDaysOfWeek = new DaysOfWeek(getSelectedItems().mDays);

            new android.app.AlertDialog.Builder(this)
                .setMultiChoiceItems(values, getSelectedItems().getBooleanArray(), (dialog, which, isChecked) -> mNewDaysOfWeek.set(which, isChecked))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    StringBuilder sb = new StringBuilder(7);
                    for (int i = 0; i < 7; i++) {
                        sb.append(mNewDaysOfWeek.isSet(i) ? "MTWTFSS".charAt(i) : '-');
                    }
                    timer.setWeekdays(sb.toString());
                    binding.timerDetailRepeat.setText(mNewDaysOfWeek.toString(this, true));
                })
                .show();
        }
    }

    private DaysOfWeek getSelectedItems() {
        String str = timer.getWeekdays();
        DaysOfWeek dow = new DaysOfWeek(0);
        if (str == null || str.length() != 7) return dow;
        for (int i = 0; i < 7; i++) {
            dow.set(i, str.charAt(i) != '-');
        }
        return dow;
    }

    private int getIntOr0(android.widget.EditText text) {
        try {
            return TextUtils.isEmpty(text.getText()) ? 0 : Integer.parseInt(text.getText().toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    protected void say(int res) {
        Toast.makeText(this, res, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDateSet(android.widget.DatePicker view, int year, int month, int day) {
        timer.setStart(calculateDate(timer.getStart(), year, month, day));
        updateDates(timer.getStart(), timer.getStop());
    }

    @Override
    public void onTimeSet(android.widget.TimePicker view, int hour, int minute) {
        if (editStart) {
            timer.setStart(calculateTime(timer.getStart(), hour, minute, null));
        } else {
            timer.setStop(calculateTime(timer.getStop(), hour, minute, timer.getStart()));
        }
        updateDates(timer.getStart(), timer.getStop());
    }

    private Date calculateDate(Date oldDate, int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(oldDate);
        cal.set(year, month, day);
        return cal.getTime();
    }

    private Date calculateTime(Date oldTime, int hour, int minute, Date startTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(oldTime);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        if (startTime != null && cal.getTime().before(startTime)) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return cal.getTime();
    }

    private void createTimer(Timer timer) {
        new CreateTimerTask(this, timer) {
            @Override
            public void finished(SvdrpEvent event) {
                done();
            }
        }.start();
    }

    private void modifyTimer(Timer timer) {
        new ModifyTimerTask(this, timer, original) {
            @Override
            public void finished(SvdrpEvent event) {
                done();
            }
        }.start();
    }

    public void done() {
        setResult(RESULT_OK);
        finish();
    }

    // --- DaysOfWeek bleibt unverändert ---
    static final class DaysOfWeek {
        private static final int[] DAY_MAP = { Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY };

        private int mDays;

        DaysOfWeek(int days) { mDays = days; }

        public String toString(Context context, boolean showNever) {
            if (mDays == 0) return showNever ? context.getString(R.string.never) : "";
            if (mDays == 0x7f) return context.getString(R.string.every_day);
            StringBuilder ret = new StringBuilder();
            String[] dayList = new DateFormatSymbols().getShortWeekdays();
            for (int i = 0; i < 7; i++) {
                if ((mDays & (1 << i)) != 0) {
                    ret.append(dayList[DAY_MAP[i]]);
                    if (i < 6 && (mDays & ~(0xFF << (i+1))) != 0) {
                        ret.append(context.getString(R.string.day_concat));
                    }
                }
            }
            return ret.toString();
        }

        private boolean isSet(int day) { return (mDays & (1 << day)) != 0; }
        public void set(int day, boolean set) { mDays = set ? mDays | (1 << day) : mDays & ~(1 << day); }
        public void set(DaysOfWeek dow) { mDays = dow.mDays; }
        public int getCoded() { return mDays; }
        public boolean[] getBooleanArray() {
            boolean[] ret = new boolean[7];
            for (int i = 0; i < 7; i++) ret[i] = isSet(i);
            return ret;
        }
        public boolean isRepeatSet() { return mDays != 0; }
    }
}
