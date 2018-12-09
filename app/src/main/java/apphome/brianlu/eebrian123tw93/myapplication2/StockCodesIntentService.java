package apphome.brianlu.eebrian123tw93.myapplication2;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by eebrian123tw93 on 2018/2/25.
 */

public class StockCodesIntentService extends IntentService {
    private static final long PERIOD_DAY = 24 * 60 * 60 * 1000;
    public static final String DOWNLOAD_STOCK_CODES = "downloadStockCodes";
    public static final String TASK = "Task";

    private StockCodesDBHelper dbHelper;
    private SQLiteDatabase database;

    public StockCodesIntentService() {
        super("IntentService");
        dbHelper = new StockCodesDBHelper(this);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String taskType = null;
        if (intent != null) {
            taskType = intent.getExtras().getString(TASK);
        }
        if (taskType != null) {
            switch (taskType) {
                case DOWNLOAD_STOCK_CODES:
                    database = dbHelper.getWritableDatabase();

                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 19);
                    calendar.set(Calendar.SECOND, 30);

                    Date date = calendar.getTime();
                    if (date.before(new Date())) {
                        date = addDay(date);
                    }

                    Timer timer = new Timer();
                    TimerTask timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            database = dbHelper.getWritableDatabase();
                            StockCore stockCore = new StockCore(StockCodesIntentService.this, database);
                            stockCore.getTWStockCode();
                            stockCore.getTWOStockCode();
                        }
                    };

                    timer.schedule(timerTask, date, PERIOD_DAY);
                    break;
            }
        }
    }

    private static Date addDay(Date date) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, 1);
        return startDT.getTime();
    }
}

