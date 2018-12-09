package apphome.brianlu.eebrian123tw93.myapplication2;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {

    private StockCodesDBHelper dbHelperelper;
    private StockCore stockCore;
    private SQLiteDatabase database;
    private Button addButtom, startButton, stopButton, updateButton;
    private EditText editText;
    private ListView listView;
    List<HashMap<String, String>> candidateCodes;
    private PowerManager.WakeLock wakeLock;
    private AlarmManager am;
    private PendingIntent pi;
    private ListAdapter listAdapter;
    List<StockCode> stockCodes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        dbHelperelper = new StockCodesDBHelper(this);
        database = dbHelperelper.getWritableDatabase();

        Intent intent = new Intent(this, StockCodesIntentService.class);
        intent.putExtra(StockCodesIntentService.TASK, StockCodesIntentService.DOWNLOAD_STOCK_CODES);
        startService(intent);

        addButtom = findViewById(R.id.button_add);
        startButton = findViewById(R.id.button_start);
        stopButton = findViewById(R.id.button_stop);
        updateButton = findViewById(R.id.button_update);
        editText = findViewById(R.id.editText);
        listView = findViewById(R.id.list_view_cadicate);
        listView.setClickable(false);

        addButtom.setOnClickListener(this);
        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        updateButton.setOnClickListener(this);
        listView.setOnItemLongClickListener(this);
        listView.setOnItemClickListener(this);

        firstStartApp();

        candidateCodes = new ArrayList<>();
        setListViewAdapter();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_add:
                addButtonFunction();
                break;
            case R.id.button_start:
                startButtonFunction();
                break;
            case R.id.button_stop:
                stopButtonFunction();
                break;
            case R.id.button_update:
                updateButtonFunction();
                break;
        }


    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("刪除 " + candidateCodes.get(position).get(StockCodesDBEntry.COLUMN_NAME_NAME) + "  (" + candidateCodes.get(position).get(StockCodesDBEntry.COLUMN_NAME_CODE) + ") ?")
                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                        String code = candidateCodes.get(position).get(StockCodesDBEntry.COLUMN_NAME_CODE);
                        database.delete(StockCodesDBEntry.TABLE_NAME_CANDIDATE, StockCodesDBEntry.COLUMN_NAME_CODE + "= ?", new String[]{code});
                        Toast.makeText(MainActivity.this, "刪除 " + candidateCodes.get(position).get(StockCodesDBEntry.COLUMN_NAME_NAME) + "  (" + candidateCodes.get(position).get(StockCodesDBEntry.COLUMN_NAME_CODE) + ") ", Toast.LENGTH_SHORT).show();
                        setListViewAdapter();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        builder.create().show();


        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        try {
            if (!listView.isClickable()) return;
            listView.setClickable(false);
            Intent intentStock = new Intent(MainActivity.this, StockActivity.class);
            intentStock.putExtra(StockCodesDBEntry.COLUMN_NAME_CODE, candidateCodes.get(position).get(StockCodesDBEntry.COLUMN_NAME_CODE));
            intentStock.putExtra(StockCodesDBEntry.COLUMN_NAME_NAME, candidateCodes.get(position).get(StockCodesDBEntry.COLUMN_NAME_NAME));
            intentStock.putExtra(StockCodesDBEntry.COLUMN_NAME_PRICE, stockCodes.get(position).getStopPrice());
            startActivity(intentStock);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            listView.setClickable(true);
        }


    }

    private void firstStartApp() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPreferences.getBoolean("firstStart", false)) {
            new Thread() {
                @Override
                public void run() {
                    StockCore stockCore = new StockCore(MainActivity.this, database);
                    stockCore.getTWStockCode();
                    stockCore.getTWOStockCode();
                    SharedPreferences.Editor sharedPreferencesEditor =
                            PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                    sharedPreferencesEditor.putBoolean(
                            "firstStart", true);
                    sharedPreferencesEditor.apply();
                }
            }.start();
        }

    }

    private void updateButtonFunction() {
        try {


            Thread t = new Thread() {
                @Override
                public void run() {
                    updateButton.setClickable(false);
                    StockCore stockCore = new StockCore(MainActivity.this, database);
                    stockCore.getTWStockCode();
                    stockCore.getTWOStockCode();
                    updateButton.setClickable(true);

                }
            };
            t.start();
            Toast.makeText(MainActivity.this, "start update stock codes", Toast.LENGTH_SHORT).show();
            t.join();

            Toast.makeText(MainActivity.this, "updated stock codes", Toast.LENGTH_SHORT).show();
            deleteSQL();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startButtonFunction() {
        stockCodes = new ArrayList<>();
        @SuppressLint("Recycle") Cursor c = database.query(StockCodesDBEntry.TABLE_NAME_CANDIDATE, null, null, null, null, null, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            do {
                String code = c.getString(c.getColumnIndex(StockCodesDBEntry.COLUMN_NAME_CODE));
                String name = c.getString(c.getColumnIndex(StockCodesDBEntry.COLUMN_NAME_NAME));
                String classname = c.getString(c.getColumnIndex(StockCodesDBEntry.COLUMN_NAME_CLASS));
                final String[] stopPrice = {"0"};


                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            Stock stock = YahooFinance.get(code);
                            Calendar from = Calendar.getInstance();
                            Calendar to = Calendar.getInstance();
                            to.add(Calendar.DATE, -1);
                            from.add(Calendar.MONTH, -2);
                            List<HistoricalQuote> historicalQuotes = stock.getHistory(from, to, Interval.DAILY);
                            double sum = 0;
                            for (int i = historicalQuotes.size() - 1; i > historicalQuotes.size() - 5; i--) {
                                sum += historicalQuotes.get(i).getClose().doubleValue();
                            }
                            DecimalFormat df = new DecimalFormat("##.00");
                            double d = Double.parseDouble(df.format(sum / 4));
                            stopPrice[0] = String.valueOf(d);
                            System.out.println(stopPrice[0]);
                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                StockCode stockCode = new StockCode(code, name, classname);
                stockCode.setStopPrice(stopPrice[0]);
                stockCodes.add(stockCode);

            } while (c.moveToNext());
            listView.setClickable(true);
        }
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
        Intent intent = new Intent(MainActivity.this, ListenerStockReceiver.class);
        intent.putExtra("msg", "play_hskay");
        Bundle args = new Bundle();
        args.putSerializable("StockCodes", (Serializable) stockCodes);
        intent.putExtra("BUNDLE", args);
        pi = PendingIntent.getBroadcast(MainActivity.this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 60000, pi);

    }

    private void addButtonFunction() {
        String text = editText.getText().toString();
        if (text.length() == 0) return;
        editText.setText("");
        @SuppressLint("Recycle") Cursor cursor = database.query(StockCodesDBEntry.TABLE_NAME_STOCKCODE, null, StockCodesDBEntry.COLUMN_NAME_CODE + " LIKE '%" + text + "%' ", null, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                String code = cursor.getString(cursor.getColumnIndex(StockCodesDBEntry.COLUMN_NAME_CODE));
                String name = cursor.getString(cursor.getColumnIndex(StockCodesDBEntry.COLUMN_NAME_NAME));
                String classname = cursor.getString(cursor.getColumnIndex(StockCodesDBEntry.COLUMN_NAME_CLASS));

                ContentValues contentValues = new ContentValues();
                contentValues.put(StockCodesDBEntry.COLUMN_NAME_CODE, code);
                contentValues.put(StockCodesDBEntry.COLUMN_NAME_NAME, name);
                contentValues.put(StockCodesDBEntry.COLUMN_NAME_CLASS, classname);
//                        contentValues.put(StockCodesDBEntry.COLUMN_NAME_PRICE,0);

                long count = database.insert(StockCodesDBEntry.TABLE_NAME_CANDIDATE, null, contentValues);
                if (count > 0) {
                    Toast.makeText(MainActivity.this, name + " " + code, Toast.LENGTH_SHORT).show();

                }
                setListViewAdapter();

            } while (cursor.moveToNext());
        }
    }

    private void stopButtonFunction() {
        try {
            Intent intent = new Intent(MainActivity.this, ListenerStockReceiver.class);
            intent.putExtra("msg", "play_hskay");
            pi = PendingIntent.getBroadcast(MainActivity.this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                am.cancel(pi);
            }
            wakeLock.release();
            Toast.makeText(MainActivity.this, "Stop alarm", Toast.LENGTH_LONG).show();
        } catch (Exception e) {

        }
    }

    private void deleteSQL() {

        StockCodesDBHelper helper = new StockCodesDBHelper(this);
        SQLiteDatabase database = helper.getWritableDatabase();
        database.delete(StockCodesDBEntry.TABLE_NAME_DAY, null, null);


        Notification.Builder mBuilder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Delete")
                .setContentText("deleted today record")
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true);

        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        int id = new Random().nextInt(990);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        id,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(id, mBuilder.build());
        Toast.makeText(this, "deleted today record", Toast.LENGTH_LONG).show();

    }

    private void setListViewAdapter() {
        candidateCodes.clear();
        @SuppressLint("Recycle") Cursor c = database.query(StockCodesDBEntry.TABLE_NAME_CANDIDATE, null, null, null, null, null, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            do {
                String code = c.getString(c.getColumnIndex(StockCodesDBEntry.COLUMN_NAME_CODE));
                String name = c.getString(c.getColumnIndex(StockCodesDBEntry.COLUMN_NAME_NAME));
                HashMap<String, String> map = new HashMap<>();

                map.put(StockCodesDBEntry.COLUMN_NAME_NAME, name);
                map.put(StockCodesDBEntry.COLUMN_NAME_CODE, code);
                candidateCodes.add(map);
            } while (c.moveToNext());
        }
        listAdapter = new SimpleAdapter(
                MainActivity.this,
                candidateCodes,
                android.R.layout.simple_list_item_2,
                new String[]{StockCodesDBEntry.COLUMN_NAME_NAME, StockCodesDBEntry.COLUMN_NAME_CODE},
                new int[]{android.R.id.text1, android.R.id.text2});


        listView.setAdapter(listAdapter);
    }

}
