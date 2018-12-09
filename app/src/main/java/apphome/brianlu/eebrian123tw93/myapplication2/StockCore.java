package apphome.brianlu.eebrian123tw93.myapplication2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Decimal;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.BooleanTransformIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.MaxPriceIndicator;
import org.ta4j.core.indicators.helpers.MinPriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import de.unknownreality.dataframe.DataFrame;
import quickml.data.AttributesMap;
import quickml.data.PredictionMap;
import quickml.data.instances.ClassifierInstance;
import quickml.supervised.ensembles.randomForest.randomDecisionForest.RandomDecisionForest;
import quickml.supervised.ensembles.randomForest.randomDecisionForest.RandomDecisionForestBuilder;
import quickml.supervised.tree.attributeIgnoringStrategies.IgnoreAttributesWithConstantProbability;
import quickml.supervised.tree.decisionTree.DecisionTreeBuilder;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

/**
 * Created by eebrian123tw93 on 2018/2/25.
 */

public class StockCore {
    private Context context;
    private SQLiteDatabase database;

    StockCore(Context context, SQLiteDatabase database) {
        this.context = context;
        this.database = database;
    }

    public List<StockCode> getTWStockCode() {
        database.delete(StockCodesDBEntry.TABLE_NAME_STOCKCODE, StockCodesDBEntry.COLUMN_NAME_CODE + " LIKE '%.TW' ", null);
        String url = "http://isin.twse.com.tw/isin/C_public.jsp?strMode=2";// http://isin.twse.com.tw/isin/C_public.jsp?strMode=4
        List<StockCode> stockCodes = new ArrayList<StockCode>();
        try {
            Document document = Jsoup.connect(url).get();
            Elements elements = document.select("tr");
            System.out.println("html get");

            for (int i = 0; i < elements.size(); i++) {
                // System.out.println(elements.get(i));
                Elements row = elements.get(i).select("td[bgcolor=#FAFAD2]");
                if (row.size() > 1 && row.get(0).text().split(Character.toString('\u3000'))[0].length() == 4) {
                    char space = row.get(0).text().charAt(4);
//                    System.out.println(Integer.toHexString((int) space));
//                    System.out.println(row.get(0).text().split(Character.toString('\u3000'))[0]);
//                    System.out.println(row.get(0).text().split(Character.toString('\u3000'))[1]);
                    String code = row.get(0).text().split(Character.toString('\u3000'))[0] + ".TW";
                    String name = row.get(0).text().split(Character.toString('\u3000'))[1];
                    String classname = row.get(4).text();
                    StockCode stockCode = new StockCode(code, name, classname);

                    ContentValues values = new ContentValues();
                    values.put(StockCodesDBEntry.COLUMN_NAME_CODE, code);
                    values.put(StockCodesDBEntry.COLUMN_NAME_NAME, name);
                    values.put(StockCodesDBEntry.COLUMN_NAME_CLASS, classname);

                    long a = database.insert(StockCodesDBEntry.TABLE_NAME_STOCKCODE, null, values);


                    stockCodes.add(stockCode);
                }

            }
            Notification.Builder builder = new Notification.Builder(context)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Update stock codes")
                    .setContentText("TW");

            Intent resultIntent = new Intent(context, MainActivity.class);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            builder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (mNotificationManager != null) {
                mNotificationManager.notify(new Random().nextInt(9999 - 1000) + 1000, builder.build());
            }


        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            return stockCodes;
        }
    }

    public List<StockCode> getTWOStockCode() {


        database.delete(StockCodesDBEntry.TABLE_NAME_STOCKCODE, StockCodesDBEntry.COLUMN_NAME_CODE + " LIKE '%.TWO' ", null);
        String urlStr = "http://dts.twse.com.tw/opendata/t187ap03_O.csv";
        String filename = "TWOStockCode.csv";
        BufferedReader br = null;
        String line = "";
        String sep = ",";
        List<StockCode> stockCodes = new ArrayList<StockCode>();
        try {
            URL url = new URL(urlStr);
//            File file=new File(context.getFilesDir(), filename);
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            rbc.close();


            int codeIndex = -1;
            int nameIndex = -1;
            int classIndex = -1;

            br = new BufferedReader(new InputStreamReader(context.openFileInput(filename), StandardCharsets.UTF_8));
            for (int i = 0; (line = br.readLine()) != null; i++) {
                System.out.println(line);
                if (i == 0) {
                    String array[] = line.split(sep);
                    for (int j = 0; j < array.length; j++) {
                        if (array[j].indexOf("代號") >= 0) {
                            codeIndex = j;
                        } else if (array[j].indexOf("公司名稱") >= 0) {
                            nameIndex = j;
                        } else if (array[j].indexOf("產業別") >= 0) {
                            classIndex = j;
                        }
                    }
                    continue;
                }
                line = line.replaceAll("\"", "");
                String array[] = line.split(sep);
                if (array[codeIndex].length() > 4 || codeIndex == -1 || nameIndex == -1 || classIndex == -1) {
                    continue;
                }
                //
                String sql = "INSERT INTO \"TWOStockCode\" (Code,Name,Class) VALUES (?, ?,?);";
                PreparedStatement pst = null;

                String code = array[codeIndex] + ".TWO";
                String name = array[nameIndex].substring(0, 3);
                String classname = array[classIndex];

                StockCode stockCode = new StockCode(code, name, classname);

                ContentValues values = new ContentValues();
                values.put(StockCodesDBEntry.COLUMN_NAME_CODE, code);
                values.put(StockCodesDBEntry.COLUMN_NAME_NAME, name);
                values.put(StockCodesDBEntry.COLUMN_NAME_CLASS, classname);

                database.insert(StockCodesDBEntry.TABLE_NAME_STOCKCODE, null, values);

                stockCodes.add(stockCode);

            }
            Notification.Builder builder = new Notification.Builder(context)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Update stock codes")
                    .setContentText("TWO");

            Intent resultIntent = new Intent(context, MainActivity.class);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            builder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (mNotificationManager != null) {
                mNotificationManager.notify(new Random().nextInt(9999 - 1000) + 1000, builder.build());
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return stockCodes;
        }

    }

    public static PredictionMap randomForestPredict(DataFrame dataFrame) throws IOException {
        Log.e("asd",System.currentTimeMillis()+"");
        dataFrame=dataFrame.getRows(dataFrame.size()-90,dataFrame.size()).toDataFrame();
        List<ClassifierInstance> stockIndicators = loadStockIndicator(dataFrame);
        Collections.shuffle(stockIndicators);
        int index = (int) (stockIndicators.size() * 0.8);
        Log.e("asd",System.currentTimeMillis()+"");
        List<ClassifierInstance> stockIndicatorsTrain = stockIndicators.subList(0, index);
        RandomDecisionForest randomForest = new RandomDecisionForestBuilder<>(new DecisionTreeBuilder<>()
                .attributeIgnoringStrategy(new IgnoreAttributesWithConstantProbability(0.2)))
                .numTrees(65)
                .executorThreadCount(8)
                .buildPredictiveModel(stockIndicators);
        Log.e("asd",System.currentTimeMillis()+"");
        AttributesMap attributes = new AttributesMap();
        String [] columns = dataFrame.getColumnNames().toArray(new String[dataFrame.getColumnNames().size()]);
        for (int i = 1; i < columns.length; i++) {
            if(columns[i].equals(Entry.LABEL))continue;
            Double value=(Double.valueOf( dataFrame.getStringColumn(columns[i]).get(dataFrame.size()-1)));
            attributes.put(columns[i], value);
        }
        System.out.println("Prediction: " + randomForest.predict(attributes));

        double correct = 0;
        for (ClassifierInstance instance : stockIndicatorsTrain) {
            String pre = randomForest.getClassificationByMaxProb(instance.getAttributes()).toString().trim();
            String label = instance.getLabel().toString().trim();
            if (pre.equals(label)) {
                correct++;
            }
//            System.out.println("classification: " + pre+"\t"+label);
        }
        System.out.println(correct / stockIndicatorsTrain.size());
        return randomForest.predict(attributes);
    }

    private static List<ClassifierInstance> loadStockIndicator(DataFrame dataFrame) {
        List<ClassifierInstance> instances = Lists.newLinkedList();

        String [] columns = dataFrame.getColumnNames().toArray(new String[dataFrame.getColumnNames().size()]);
        for (int i = 1; i < dataFrame.size() - 1; i++) {
            AttributesMap attributes = AttributesMap.newHashMap();
            for (int j = 1; j < columns.length; j++) {
                if(columns[j].equals(Entry.LABEL))continue;
                String s=dataFrame.getStringColumn(columns[j]).get(i);
                Double value=(Double.valueOf(s));
                attributes.put(columns[j], value);
            }
            double d=Double.valueOf((String) dataFrame.getStringColumn(Entry.LABEL).get( i));
            if (d == 0) {
                instances.add(new ClassifierInstance(attributes, "跌"));
            } else {
                instances.add(new ClassifierInstance(attributes, "漲"));
            }
        }

        return instances;
    }

    public static DataFrame createIndicatorTable(String symbol, List<HistoricalQuote> timeSeries) throws IOException {


        List<Integer> removeIndexs = new ArrayList<>();
        for (int i = 0; i < timeSeries.size(); i++) {
            if (timeSeries.get(i).getClose() == null) {
                removeIndexs.add(i);
            }
        }
        for (Integer i : removeIndexs) {
            timeSeries.remove((int) i);
        }

        BaseTimeSeries series = new BaseTimeSeries(symbol);


        for (int i = 0; i < timeSeries.size(); i++) {
            HistoricalQuote quote = timeSeries.get(i);
            Calendar calendar = quote.getDate();
            TimeZone tz = calendar.getTimeZone();
            ZoneId zid = tz == null ? ZoneId.systemDefault() : tz.toZoneId();
            ZonedDateTime zonedDateTime = ZonedDateTime.of(LocalDateTime.ofInstant(calendar.toInstant(), zid), ZoneId.of(tz.getID()));
            series.addBar(new BaseBar(zonedDateTime, quote.getOpen().doubleValue(), quote.getHigh().doubleValue(), quote.getLow().doubleValue(), quote.getClose().doubleValue(), quote.getVolume().doubleValue()));
        }


        //table test done
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        //sma 05,10,20
        SMAIndicator sma_05 = new SMAIndicator(closePrice, 5);
        SMAIndicator sma_10 = new SMAIndicator(closePrice, 10);
        SMAIndicator sma_20 = new SMAIndicator(closePrice, 20);
//        System.out.println(sma_05.getValue(timeSeries.size() - 2));
//        System.out.println(sma_10.getValue(timeSeries.size() - 2));
//        System.out.println(sma_20.getValue(timeSeries.size() - 2));
        //madc value
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator emaIndicator = new EMAIndicator(macd, 9);
//        System.out.println(emaIndicator.getValue(timeSeries.size() - 2));
        //RSI
        RSIIndicator rsi_05 = new RSIIndicator(closePrice, 5);
        RSIIndicator rsi_10 = new RSIIndicator(closePrice, 10);
//        System.out.println(rsi_05.getValue(timeSeries.size() - 2));
//        System.out.println(rsi_10.getValue(timeSeries.size() - 2));

        //volume
        VolumeIndicator volumeIndicator = new VolumeIndicator(series);
        SMAIndicator vol_05 = new SMAIndicator(volumeIndicator, 5);
        SMAIndicator vol_10 = new SMAIndicator(volumeIndicator, 10);
//        System.out.println(vol_05.getValue(timeSeries.size() - 2));
//        System.out.println(vol_10.getValue(timeSeries.size() - 2));


        // Standard deviation
        StandardDeviationIndicator sd = new StandardDeviationIndicator(closePrice, 5);
//        System.out.println(sd.getValue(timeSeries.size() - 2));

        //k,d
        MaxPriceIndicator maxPrice = new MaxPriceIndicator(series);
        MinPriceIndicator minPrice = new MinPriceIndicator(series);
        StochasticOscillatorKIndicator rsv = new StochasticOscillatorKIndicator(closePrice, 9, maxPrice, minPrice);
        MyStochasticOscillatorKIndicator stochK = new MyStochasticOscillatorKIndicator(rsv, 3);
        MyStochasticOscillatorDIndicator stochD = new MyStochasticOscillatorDIndicator(stochK, 3);
//        System.out.println(stochK.getValue(timeSeries.size() - 2));
//        System.out.println(stochD.getValue(timeSeries.size() - 2));

        //bias
        BiasIndicator bias = new BiasIndicator(sma_05, closePrice);
//        System.out.println(bias.getValue(timeSeries.size() - 2));

        //opop
        OpopIndicator opop = new OpopIndicator(closePrice);
//        System.out.println(opop.getValue(timeSeries.size() - 5));

        // BooleanTransform
        BooleanTransformIndicator booleanTransformGrater = new BooleanTransformIndicator(sma_20, Decimal.valueOf(2), BooleanTransformIndicator.BooleanTransformType.isGreaterThan);
        BooleanTransformIndicator booleanTransformLess = new BooleanTransformIndicator(sma_20, Decimal.valueOf(2), BooleanTransformIndicator.BooleanTransformType.isLessThan);
//        System.out.println(booleanTransformGrater.getValue(timeSeries.size() - 2));
//        System.out.println(booleanTransformLess.getValue(timeSeries.size() - 2));

        //label
        LabelIndicator label = new LabelIndicator(opop);
        DataFrame dataFrame = DataFrame.create(symbol).
                addStringColumn(Entry.DATE).
                addStringColumn(Entry.SMA05).
                addStringColumn(Entry.SMA10).
                addStringColumn(Entry.SMA20).
                addStringColumn(Entry.MACD).
                addStringColumn(Entry.RSI05).
                addStringColumn(Entry.RSI10).
                addStringColumn(Entry.SD).
                addStringColumn(Entry.K9).
                addStringColumn(Entry.D9).
                addStringColumn(Entry.VOL05).
                addStringColumn(Entry.VOL10).
                addStringColumn(Entry.BAIS).
                addStringColumn(Entry.OPOP).
                addStringColumn(Entry.BOOLEAN_GRATER).
                addStringColumn(Entry.BOOLEAN_LESS).
                addStringColumn(Entry.LABEL);
        for (int i = 0; i < timeSeries.size(); i++) {
            HistoricalQuote quote = timeSeries.get(i);
            dataFrame.getStringColumn(Entry.DATE).set(i, quote.getDate().toString());
            dataFrame.append(
                    quote.getDate().toString(),
                    sma_05.getValue(i).doubleValue() + "",
                    sma_10.getValue(i).doubleValue() + "",
                    sma_20.getValue(i).doubleValue() + "",
                    emaIndicator.getValue(i).doubleValue() + "",
                    rsi_05.getValue(i).doubleValue() + "",
                    rsi_10.getValue(i).doubleValue() + "",
                    sd.getValue(i).doubleValue() + "",
                    stochK.getValue(i).doubleValue() + "",
                    stochD.getValue(i).doubleValue() + "",
                    vol_05.getValue(i).doubleValue() + "",
                    vol_10.getValue(i).doubleValue() + "",
                    bias.getValue(i).doubleValue() + "",
                    opop.getValue(i).doubleValue() + "",
                    booleanTransformGrater.getValue(i).booleanValue() ? 1 + "" : 0 + "",
                    booleanTransformLess.getValue(i).booleanValue() ? 1 + "" : 0 + "",
                    label.getValue(i).doubleValue() > 0 ? 1 + "" : 0 + ""
            );
        }
        System.out.println();
        return dataFrame;
    }
}

class StockCode implements Serializable {
    private String code;
    private String name;
    private String className;
    private String stopPrice;

    StockCode(String code, String name, String className) {
        this.code = code;
        this.name = name;
        this.className = className;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public String getStopPrice() {
        return stopPrice;
    }

    public void setStopPrice(String stopPrice) {
        this.stopPrice = stopPrice;
    }
}

class MyStochasticOscillatorKIndicator extends CachedIndicator<Decimal> {
    private Indicator<Decimal> rsv;
    private int timeFrame;

    MyStochasticOscillatorKIndicator(Indicator<Decimal> rsv, int timeFrame) {
        super(rsv);
        this.rsv = rsv;
        this.timeFrame = timeFrame;
    }

    @Override
    protected Decimal calculate(int index) {
        if (index == 0) return Decimal.valueOf(0);
        Decimal rsvValue = rsv.getValue(index);
        if (this.rsv.getValue(index).isNaN()) {
            rsvValue = Decimal.ZERO;
        }
        //        System.out.println(this.rsv.getValue(index));

        return ((Decimal) rsvValue.dividedBy(Decimal.valueOf(timeFrame)).plus(this.getValue(index - 1).multipliedBy(Decimal.valueOf(2)).dividedBy(Decimal.valueOf(timeFrame))));
    }
}

class MyStochasticOscillatorDIndicator extends CachedIndicator<Decimal> {
    private Indicator<Decimal> k;
    private int timeFrame;

     MyStochasticOscillatorDIndicator(Indicator<Decimal> k, int timeFrame) {
        super(k);
        this.k = k;
        this.timeFrame = timeFrame;
    }

    @Override
    protected Decimal calculate(int index) {
        if (index == 0) return Decimal.valueOf(0);
        return ((Decimal) this.k.getValue(index).dividedBy(Decimal.valueOf(timeFrame)).plus(this.getValue(index - 1).multipliedBy(Decimal.valueOf(2)).dividedBy(Decimal.valueOf(timeFrame))));
    }
}

class BiasIndicator extends CachedIndicator<Decimal> {
    private Indicator<Decimal> ma;
    private Indicator<Decimal> close;

    BiasIndicator(Indicator<Decimal> ma, Indicator<Decimal> close) {
        super(ma);
        this.ma = ma;
        this.close = close;
    }

    @Override
    protected Decimal calculate(int index) {
        return close.getValue(index).minus(ma.getValue(index)).multipliedBy(Decimal.valueOf(100)).dividedBy(ma.getValue(index));
    }
}

class OpopIndicator extends CachedIndicator<Decimal> {
    private Indicator<Decimal> close;

    OpopIndicator(Indicator<Decimal> close) {
        super(close);
        this.close = close;
    }

    protected Decimal calculate(int index) {
        if (index == 0) return Decimal.ZERO;
        return close.getValue(index).minus(close.getValue(index - 1)).dividedBy(close.getValue(index - 1)).multipliedBy(Decimal.HUNDRED);
    }
}

class LabelIndicator extends CachedIndicator<Decimal> {
    private Indicator<Decimal> opop;

    LabelIndicator(Indicator<Decimal> opop) {
        super(opop);
        this.opop = opop;
    }

    protected Decimal calculate(int index) {
        if (index == opop.getTimeSeries().getBarCount() - 1) return Decimal.ZERO;
        if (opop.getValue(index + 1).doubleValue() > 0) return Decimal.ONE;
        else return Decimal.ZERO;
    }
}

class Entry {
    public static final String DATE = "DATE";
    public static final String OPEN = "OPEN";
    public static final String CLOSE = "CLOSE";
    public static final String LOW = "LOW";
    public static final String HIGH = "HIGH";

    public static final String SMA05 = "SMA05";
    public static final String SMA10 = "SMA10";
    public static final String SMA20 = "SMA20";
    public static final String MACD = "MACD";
    public static final String RSI05 = "RSI05";
    public static final String RSI10 = "RSI10";
    public static final String SD = "SD";
    public static final String VOL05 = "VOL05";
    public static final String VOL10 = "VOL10";
    public static final String BAIS = "BAIS";
    public static final String OPOP = "OPOP";
    public static final String K9 = "K9";
    public static final String D9 = "D9";
    public static final String BOOLEAN_GRATER = "BOOLEAN_GRATER";
    public static final String BOOLEAN_LESS = "BOOLEAN_LESS";
    public static final String LABEL = "LABEL";


}

