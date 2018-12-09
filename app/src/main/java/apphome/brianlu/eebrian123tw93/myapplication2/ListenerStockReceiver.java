package apphome.brianlu.eebrian123tw93.myapplication2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;

public class ListenerStockReceiver extends BroadcastReceiver {
    private Context context;
    DateFormat df = new SimpleDateFormat("YYYY-MM-dd HH:mm");

    @Override
    public void onReceive(final Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        this.context = context;
        Bundle bData = intent.getExtras();
        if (bData.get("msg").equals("play_hskay")) {
            //System.out.println("Start");
            if (ListenerStockReceiver.isMarketOpen()||true) {
                Bundle args = intent.getBundleExtra("BUNDLE");

                ArrayList<StockCode> stockCodes = (ArrayList<StockCode>) args.getSerializable("StockCodes");
                for (final StockCode stockCode : stockCodes) {
                    try {
                        Thread thread = new Thread() {
                            @Override
                            public void run() {
                                listener(stockCode);
                            }
                        };
                        thread.start();
                        thread.join();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
            updateStockCodes();
            deleteSQL();

        } else {

        }
    }

    public static boolean isMarketOpen() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) return false;

        if (9 <= hour && hour <= 13) {
            if (hour == 13 && minute > 30) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private void getStockPriceEveryMinute(List<StockCode> stockCodes) {

        try {
            Map<String, Stock> stockMap = new HashMap<>();

            List<ArrayList<String>> codes = new ArrayList<>();
            ArrayList<String> c = new ArrayList<>();
            for (int i = 0; i < stockCodes.size(); i++) {
                if (i % 10 == 0) {
                    if (c.size() != 0) {
                        codes.add(c);
                        c = new ArrayList<>();
                    }
                }
                c.add(stockCodes.get(i).getCode());
            }
            codes.add(c);

            Thread thread = new Thread() {
                @Override
                public void run() {


                    for (int i = 0; i < codes.size(); i++) {
                        try {

                            stockMap.putAll(YahooFinance.get(codes.get(i).toArray(new String[codes.get(i).size()])));
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                            List<String> s = codes.get(i);
                            Map<String, Stock> map = new HashMap<>();
                            for (int j = 0; j < s.size(); j++) {
                                try {
                                    map.put(s.get(j), YahooFinance.get(s.get(j)));
                                    Thread.sleep(1000);
                                } catch (Exception es) {
                                    es.printStackTrace();
                                }
                            }
                            stockMap.putAll(map);
                            System.out.println(map.size());
                        }
                    }


                }
            };
            thread.start();
            thread.join();

            StockCodesDBHelper helper = new StockCodesDBHelper(context);
            SQLiteDatabase database = helper.getWritableDatabase();
            Date date = new Date();
            DateFormat df = new SimpleDateFormat("YYYY-MM-dd HH:mm");
            for (StockCode stockCode : stockCodes) {
                String code = stockCode.getCode();
                double price = stockMap.get(code).getQuote().getPrice().doubleValue();
                ContentValues values = new ContentValues();
                values.put(StockCodesDBEntry.COLUMN_NAME_CODE, stockCode.getCode());
                values.put(StockCodesDBEntry.COLUMN_NAME_PRICE, price);
                values.put(StockCodesDBEntry.COLUMN_NAME_MINUTE, df.format(date));
                database.insert(StockCodesDBEntry.TABLE_NAME_DAY, null, values);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listener(StockCode stockCode) {


        try {
            String code = stockCode.getCode().split("\\.")[0];
            int codeInteger = Integer.valueOf(code);
            String url = "https://tw.stock.yahoo.com/q/q?s=" + code;
            Document document = Jsoup.connect(url).timeout(5000).get();
            Elements elements = document.select("body>center>table");
            String value = "0";
            for (Element element : elements) {
                if (element.text().indexOf(code) > 0) {
                    value = element.select("tbody > tr:nth-child(2) > td:nth-child(3) > b").first().text();
                    System.out.println(value);
                    break;
                }
            }

            StockCodesDBHelper helper = new StockCodesDBHelper(context);
            SQLiteDatabase database = helper.getWritableDatabase();
            Date date = new Date();


            ContentValues values = new ContentValues();
            values.put(StockCodesDBEntry.COLUMN_NAME_CODE, stockCode.getCode());
            values.put(StockCodesDBEntry.COLUMN_NAME_PRICE, value);
            values.put(StockCodesDBEntry.COLUMN_NAME_MINUTE, df.format(date));
            database.insert(StockCodesDBEntry.TABLE_NAME_DAY, null, values);


            System.out.println(stockCode.getStopPrice());


            if (Double.valueOf(stockCode.getStopPrice()) > Double.valueOf(value)) {
                System.out.println(Double.valueOf(stockCode.getStopPrice()) > Double.valueOf(value));
                Notification.Builder mBuilder = new Notification.Builder(context)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(stockCode.getCode() + "     " + stockCode.getName() + "    " + new SimpleDateFormat("HH:mm:ss").format(new Date()))
                        .setContentText(value + "\t\t\t" + stockCode.getStopPrice());



                Intent resultIntent = new Intent(context, StockActivity.class);
                String s = stockCode.getCode();
                resultIntent.putExtra(StockCodesDBEntry.COLUMN_NAME_CODE, stockCode.getCode());
                resultIntent.putExtra(StockCodesDBEntry.COLUMN_NAME_NAME, stockCode.getName());
                resultIntent.putExtra(StockCodesDBEntry.COLUMN_NAME_PRICE, stockCode.getStopPrice());
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addParentStack(StockActivity.class);
                stackBuilder.addNextIntent(resultIntent);

                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(
                                codeInteger,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                mBuilder.setContentIntent(resultPendingIntent);

                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                String channelId = code;
                String channelName =  channelId;
                int importance = NotificationManager.IMPORTANCE_HIGH;

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    NotificationChannel mChannel = new NotificationChannel(
                            channelId, channelName, importance);
                    mChannel.setDescription(value + "\t\t\t" + stockCode.getStopPrice());
                    mChannel.enableVibration(true);
                    mChannel.enableLights(true);
                    mBuilder.setChannelId(code);
                    mNotificationManager.createNotificationChannel(mChannel);
                }


                mNotificationManager.notify(codeInteger, mBuilder.build());
                System.out.println("notify");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();

        }

    }

    private  void deleteSQL() {
        Calendar calendar = Calendar.getInstance();
        if (calendar.get(Calendar.HOUR_OF_DAY) == 8 && (calendar.get(Calendar.MINUTE) == 59 || calendar.get(Calendar.MINUTE) == 58)) {
            StockCodesDBHelper helper = new StockCodesDBHelper(context);
            SQLiteDatabase database = helper.getWritableDatabase();
            database.delete(StockCodesDBEntry.TABLE_NAME_DAY, null, null);


            Notification.Builder mBuilder = new Notification.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Delete")
                    .setContentText("deleted today record")
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true);

            Intent resultIntent = new Intent(context, MainActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(Math.class);
            stackBuilder.addNextIntent(resultIntent);
            int id = new Random().nextInt(990);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            id,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

            mBuilder.setContentIntent(resultPendingIntent);

            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(id, mBuilder.build());
            Toast.makeText(context, "deleted today record", Toast.LENGTH_LONG).show();
        }
    }

    private void updateStockCodes() {
        Calendar calendar = Calendar.getInstance();
        if (calendar.get(Calendar.HOUR_OF_DAY) == 21 && (calendar.get(Calendar.MINUTE) == 59 || calendar.get(Calendar.MINUTE) == 58)) {
            new Thread() {
                @Override
                public void run() {
                    StockCodesDBHelper helper = new StockCodesDBHelper(context);
                    SQLiteDatabase database = helper.getWritableDatabase();
                    StockCore stockCore = new StockCore(context, database);
                    stockCore.getTWStockCode();
                    stockCore.getTWOStockCode();
                }
            }.start();
        }
    }
}
