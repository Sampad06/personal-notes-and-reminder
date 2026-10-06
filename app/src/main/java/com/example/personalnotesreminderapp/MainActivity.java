package com.example.personalnotesreminderapp;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    EditText etTitle, etNote;
    Button btnDate, btnTime, btnSave;
    LinearLayout notesContainer;

    Calendar reminderCalendar = Calendar.getInstance();

    SharedPreferences preferences;

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> notes = new ArrayList<>();
    ArrayList<String> reminders = new ArrayList<>();

    private static final String PREF_NAME = "PersonalNotes";
    private static final String CHANNEL_ID = "ReminderChannel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        etTitle = findViewById(R.id.etTitle);
        etNote = findViewById(R.id.etNote);

        btnDate = findViewById(R.id.btnDate);
        btnTime = findViewById(R.id.btnTime);
        btnSave = findViewById(R.id.btnSave);

        notesContainer = findViewById(R.id.notesContainer);

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        100
                );
            }
        }

        loadNotes();

        btnDate.setOnClickListener(v -> selectDate());

        btnTime.setOnClickListener(v -> selectTime());

        btnSave.setOnClickListener(v -> saveNote());
    }

    private void selectDate() {

        Calendar today = Calendar.getInstance();

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {

                            reminderCalendar.set(
                                    Calendar.YEAR,
                                    year
                            );

                            reminderCalendar.set(
                                    Calendar.MONTH,
                                    month
                            );

                            reminderCalendar.set(
                                    Calendar.DAY_OF_MONTH,
                                    dayOfMonth
                            );

                            btnDate.setText(
                                    String.format(
                                            Locale.getDefault(),
                                            "%02d/%02d/%04d",
                                            dayOfMonth,
                                            month + 1,
                                            year
                                    )
                            );

                        },
                        today.get(Calendar.YEAR),
                        today.get(Calendar.MONTH),
                        today.get(Calendar.DAY_OF_MONTH)
                );

        datePickerDialog.show();
    }

    private void selectTime() {

        Calendar now = Calendar.getInstance();

        TimePickerDialog timePickerDialog =
                new TimePickerDialog(
                        this,
                        (view, hourOfDay, minute) -> {

                            reminderCalendar.set(
                                    Calendar.HOUR_OF_DAY,
                                    hourOfDay
                            );

                            reminderCalendar.set(
                                    Calendar.MINUTE,
                                    minute
                            );

                            reminderCalendar.set(
                                    Calendar.SECOND,
                                    0
                            );

                            btnTime.setText(
                                    String.format(
                                            Locale.getDefault(),
                                            "%02d:%02d",
                                            hourOfDay,
                                            minute
                                    )
                            );

                        },
                        now.get(Calendar.HOUR_OF_DAY),
                        now.get(Calendar.MINUTE),
                        true
                );

        timePickerDialog.show();
    }

    private void saveNote() {

        String title = etTitle.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Enter a title");
            return;
        }

        if (note.isEmpty()) {
            etNote.setError("Enter a note");
            return;
        }

        String reminderTime = new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
        ).format(reminderCalendar.getTime());

        titles.add(title);
        notes.add(note);
        reminders.add(reminderTime);

        saveNotes();

        scheduleReminder(
                title,
                note,
                reminderCalendar.getTimeInMillis(),
                titles.size() - 1
        );

        etTitle.setText("");
        etNote.setText("");

        btnDate.setText("Select Reminder Date");
        btnTime.setText("Select Reminder Time");

        Toast.makeText(
                this,
                "Note saved successfully!",
                Toast.LENGTH_SHORT
        ).show();

        displayNotes();
    }

    private void saveNotes() {

        SharedPreferences.Editor editor = preferences.edit();

        editor.putInt("count", titles.size());

        for (int i = 0; i < titles.size(); i++) {

            editor.putString(
                    "title_" + i,
                    titles.get(i)
            );

            editor.putString(
                    "note_" + i,
                    notes.get(i)
            );

            editor.putString(
                    "reminder_" + i,
                    reminders.get(i)
            );
        }

        editor.apply();
    }

    private void loadNotes() {

        int count = preferences.getInt("count", 0);

        titles.clear();
        notes.clear();
        reminders.clear();

        for (int i = 0; i < count; i++) {

            titles.add(
                    preferences.getString(
                            "title_" + i,
                            ""
                    )
            );

            notes.add(
                    preferences.getString(
                            "note_" + i,
                            ""
                    )
            );

            reminders.add(
                    preferences.getString(
                            "reminder_" + i,
                            ""
                    )
            );
        }

        displayNotes();
    }

    private void displayNotes() {

        notesContainer.removeAllViews();

        for (int i = 0; i < titles.size(); i++) {

            final int position = i;

            LinearLayout card = new LinearLayout(this);

            card.setOrientation(
                    LinearLayout.VERTICAL
            );

            card.setPadding(
                    20,
                    20,
                    20,
                    20
            );

            TextView titleText = new TextView(this);

            titleText.setText(
                    "📌 " + titles.get(i)
            );

            titleText.setTextSize(19);
            titleText.setTextColor(
                    getResources().getColor(
                            android.R.color.black
                    )
            );

            TextView noteText = new TextView(this);

            noteText.setText(
                    notes.get(i)
            );

            noteText.setTextSize(16);

            TextView reminderText = new TextView(this);

            reminderText.setText(
                    "⏰ Reminder: " + reminders.get(i)
            );

            reminderText.setTextSize(14);

            Button deleteButton = new Button(this);

            deleteButton.setText("Delete");

            deleteButton.setOnClickListener(
                    v -> deleteNote(position)
            );

            card.addView(titleText);
            card.addView(noteText);
            card.addView(reminderText);
            card.addView(deleteButton);

            notesContainer.addView(card);

            TextView separator = new TextView(this);

            separator.setText(
                    "--------------------------------"
            );

            notesContainer.addView(separator);
        }
    }

    private void deleteNote(int position) {

        titles.remove(position);
        notes.remove(position);
        reminders.remove(position);

        saveNotes();

        displayNotes();

        Toast.makeText(
                this,
                "Note deleted",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void scheduleReminder(
            String title,
            String message,
            long time,
            int requestCode
    ) {

        if (time <= System.currentTimeMillis()) {

            Toast.makeText(
                    this,
                    "Reminder time is in the past",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        AlarmManager alarmManager =
                (AlarmManager) getSystemService(
                        Context.ALARM_SERVICE
                );

        Intent intent =
                new Intent(
                        this,
                        ReminderReceiver.class
                );

        intent.putExtra(
                "title",
                title
        );

        intent.putExtra(
                "message",
                message
        );

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                );

        if (alarmManager != null) {

            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    time,
                    pendingIntent
            );
        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Personal Note Reminders",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "Notifications for personal notes and reminders"
            );

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            manager.createNotificationChannel(channel);
        }
    }
}