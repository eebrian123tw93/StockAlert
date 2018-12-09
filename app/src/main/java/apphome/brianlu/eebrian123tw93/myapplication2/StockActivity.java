package apphome.brianlu.eebrian123tw93.myapplication2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.unknownreality.dataframe.DataFrame;
import quickml.data.PredictionMap;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

public class StockActivity extends AppCompatActivity {
    private TextView textViewMinutePrice, textViewLow, textViewOpen, textViewHigh, textViewClose, textViewDate, textViewCode, textViewName, textViewPrediction;
    private StockCodesDBHelper helper;
    private static final long PERIOD_MINUTE = 60 * 1000;
    String code;
    LineChart lineChart;
    CandleStickChart candleStickChart;
    List<MinuteStockPrice> prices;
    List<HistoricalQuote> historicalQuotes;
    String valueLimit;
    Thread historicalQuotesThread;
    @SuppressLint("SimpleDateFormat")
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd");
    List<CandleEntry> ceList;
    DecimalFormat df = new DecimalFormat("##.00");
    List<Calendar> dateTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock);
        textViewMinutePrice = findViewById(R.id.textView_minute_price);
        textViewLow = findViewById(R.id.textView_history_price_low);
        textViewHigh = findViewById(R.id.textView_history_price_high);
        textViewOpen = findViewById(R.id.textView_history_price_open);
        textViewClose = findViewById(R.id.textView_history_price_close);
        textViewDate = findViewById(R.id.text_view_date);
        textViewCode = findViewById(R.id.text_view_code);
        textViewName = findViewById(R.id.text_view_name);
        textViewPrediction = findViewById(R.id.text_view_prediction);

        lineChart = findViewById(R.id.line_chart_minute_price);
        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setAxisMinValue(0.0f);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getAxisRight().setDrawGridLines(false);
        lineChart.getAxisLeft().setDrawGridLines(false);
        lineChart.getXAxis().setLabelCount(8);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.setNoDataText("No Data");
        lineChart.setNoDataTextColor(Color.BLACK);
        lineChart.setScaleEnabled(false);

        candleStickChart = findViewById(R.id.candle_stick_chart_history);
        candleStickChart.setDoubleTapToZoomEnabled(false);
        candleStickChart.getDescription().setEnabled(false);
        candleStickChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        candleStickChart.getXAxis().setDrawGridLines(false);
        candleStickChart.getAxisRight().setDrawGridLines(false);
        candleStickChart.getAxisLeft().setDrawGridLines(false);
        candleStickChart.getXAxis().setLabelCount(5);
        candleStickChart.getAxisRight().setEnabled(false);
        candleStickChart.setNoDataText("No Data");
        candleStickChart.setNoDataTextColor(Color.BLACK);
        candleStickChart.setScaleXEnabled(true);
        candleStickChart.setScaleYEnabled(false);


        Intent intent = getIntent();
        code = intent.getExtras().getString(StockCodesDBEntry.COLUMN_NAME_CODE);
        textViewCode.setText(code);
        textViewCode.setTextSize(50f);
        textViewName.setText(intent.getExtras().getString(StockCodesDBEntry.COLUMN_NAME_NAME, ""));
        textViewName.setTextSize(50f);
        textViewPrediction.setTextSize(30f);

        MinuteStockPrice.CODE = code;
        valueLimit = intent.getExtras().getString(StockCodesDBEntry.COLUMN_NAME_PRICE, "");

        helper = new StockCodesDBHelper(this);


        updateMinuteStockPrice();
        updateLineChart();
        updateHistoricalQuote();


        Handler handler = new Handler();
        if (ListenerStockReceiver.isMarketOpen()) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateMinuteStockPrice();
                    updateLineChart();
                    candleStickChart.invalidate();

                }
            }, PERIOD_MINUTE);
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    historicalQuotesThread.join();
                    candleStickChart.animateX(2500);
                    candleStickChart.setVisibleXRange(0, 40);
                    candleStickChart.moveViewToX(ceList.size() - 39);
                    candleStickChart.invalidate();
                    int index = ceList.size() - 1;
                    String dateformat = simpleDateFormat.format(new Date(dateTimes.get(index).getTimeInMillis()));
                    textViewDate.setText(dateformat);
                    textViewLow.setText("Low: " + df.format(ceList.get(index).getLow()));
                    textViewHigh.setText("High: " + df.format(ceList.get(index).getHigh()));
                    textViewOpen.setText("Open: " + df.format(ceList.get(index).getOpen()));
                    textViewClose.setText("Close: " + df.format(ceList.get(index).getClose()));

                    if (ceList.get(index).getClose() > ceList.get(index).getOpen()) {
                        textViewClose.setTextColor(Color.RED);
                        textViewOpen.setTextColor(Color.RED);
                    } else {
                        textViewClose.setTextColor(Color.GREEN);
                        textViewOpen.setTextColor(Color.GREEN);
                    }
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                randomForestPrediction();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateMinuteStockPrice() {
        prices = new ArrayList<>();
        SQLiteDatabase readableDatabase = helper.getReadableDatabase();
        @SuppressLint("Recycle") Cursor c = readableDatabase.query(StockCodesDBEntry.TABLE_NAME_DAY, null, StockCodesDBEntry.COLUMN_NAME_CODE + " = ? ", new String[]{code}, null, null, StockCodesDBEntry.COLUMN_NAME_MINUTE + " ASC", null);
        if (c.getCount() > 0) {
            c.moveToFirst();

            do {
                String dateTime = c.getString(c.getColumnIndex(StockCodesDBEntry.COLUMN_NAME_MINUTE)).split(" ")[1];
                String price = c.getString(c.getColumnIndex(StockCodesDBEntry.COLUMN_NAME_PRICE));
                prices.add(new MinuteStockPrice(dateTime, Float.parseFloat(price)));
            } while (c.moveToNext());
        }

    }

    private void updateLineChart() {
        if (prices.size() == 0) return;
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            entries.add(new Entry(i, prices.get(i).getPrice()));
            labels.add(prices.get(i).getDateTime());
        }


        LineDataSet dataSet = new LineDataSet(entries, "Price");
        dataSet.setDrawCircles(true);
        dataSet.setCircleRadius(1f);
        dataSet.setLineWidth(2f);
        dataSet.setDrawHorizontalHighlightIndicator(false);
        dataSet.setHighlightLineWidth(1.5f);
        dataSet.setHighLightColor(Color.BLUE);
        dataSet.setValueTextSize(0f);
        LineData lineData = new LineData(dataSet);

        lineChart.animateX(1000);
        lineChart.setData(lineData);
        lineChart.getXAxis().setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                if (value < 0 || value > (labels.size() - 1)) {//使得两侧柱子完全显示
                    return "";
                }
                return labels.get((int) value);
            }
        });
        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                textViewMinutePrice.setText(labels.get((int) e.getX()) + "\t\t\t\t" + e.getY());
            }

            @Override
            public void onNothingSelected() {

            }
        });


        if (valueLimit.length() != 0) {
            LimitLine limitLine = new LimitLine(Float.parseFloat(valueLimit), valueLimit);
            limitLine.setLineColor(Color.RED);
            limitLine.setTypeface(Typeface.DEFAULT_BOLD);
            limitLine.setTextSize(10f);

            lineChart.getAxisLeft().addLimitLine(limitLine);
            lineChart.getAxisLeft().resetAxisMaximum();
            lineChart.getAxisLeft().resetAxisMinimum();
            float pr = prices.get(prices.size() - 1).getPrice();
            lineChart.getAxisLeft().setAxisMaximum(pr * 1.1f);
            lineChart.getAxisLeft().setAxisMinimum(pr * 0.9f);
        }

        lineChart.invalidate();
    }

    private void updateHistoricalQuote() {
        historicalQuotesThread = new Thread() {
            @Override
            public void run() {
                try {
                    Calendar from = Calendar.getInstance();
                    Calendar to = Calendar.getInstance();
                    from.add(Calendar.MONTH, -6);
                    Stock stock = YahooFinance.get(code, from, to, Interval.DAILY);
                    historicalQuotes = stock.getHistory();
                    updateCandleStickChart();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        historicalQuotesThread.start();
    }

    private void randomForestPrediction() throws IOException {
        DataFrame dataFrame = StockCore.createIndicatorTable(code, historicalQuotes);
        PredictionMap map = StockCore.randomForestPredict(dataFrame);
        String increase = ((int) (map.get("漲").doubleValue() * 100)) + "%";
        String decrease = ((int) (map.get("跌").doubleValue() * 100)) + "%";
        textViewPrediction.setText("  {漲=" + increase + "," + "跌=" + decrease + "}");

    }

    private void updateCandleStickChart() {
        ceList = new ArrayList<>();
        dateTimes = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < historicalQuotes.size(); i++) {
            HistoricalQuote quote = historicalQuotes.get(i);
            if (quote.getClose() == null || quote.getOpen() == null || quote.getHigh() == null || quote.getLow() == null)
                continue;
            ceList.add(new CandleEntry((float) j++, quote.getHigh().floatValue(), quote.getLow().floatValue(), quote.getOpen().floatValue(), quote.getClose().floatValue()));
            dateTimes.add(quote.getDate());
        }
        System.out.println(historicalQuotes.size());
        System.out.println(ceList.size());
        CandleDataSet cds = new CandleDataSet(ceList, "Entries");
        cds.setColor(Color.rgb(80, 80, 80));
        cds.setShadowColor(Color.DKGRAY);
        cds.setShadowWidth(0.7f);
        cds.setDecreasingColor(Color.GREEN);
        cds.setDecreasingPaintStyle(Paint.Style.FILL);
        cds.setIncreasingColor(Color.RED);
        cds.setIncreasingPaintStyle(Paint.Style.FILL);
        cds.setNeutralColor(Color.BLUE);
        cds.setValueTextColor(Color.WHITE);
        cds.setHighlightLineWidth(1.5f);

        CandleData cd = new CandleData(cds);
        candleStickChart.setData(cd);
        candleStickChart.getXAxis().setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                if (value < 0 || value > (dateTimes.size() - 1)) {//使得两侧柱子完全显示
                    return "";
                }
                return simpleDateFormat.format(new Date(dateTimes.get((int) value).getTimeInMillis()));
            }
        });
        candleStickChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                try {
                    System.out.println(e.getX());

                    int index = (int) Math.floor(e.getX());
                    System.out.println(index);
                    textViewLow.setText("Low: " + df.format(ceList.get(index).getLow()));
                    textViewHigh.setText("High: " + df.format(ceList.get(index).getHigh()));
                    textViewOpen.setText("Open: " + df.format(ceList.get(index).getOpen()));
                    textViewClose.setText("Close: " + df.format(ceList.get(index).getClose()));
                    if (ceList.get(index).getClose() > ceList.get(index).getOpen()) {
                        textViewClose.setTextColor(Color.RED);
                        textViewOpen.setTextColor(Color.RED);
                    } else {
                        textViewClose.setTextColor(Color.GREEN);
                        textViewOpen.setTextColor(Color.GREEN);
                    }
                    String dateformat = simpleDateFormat.format(new Date(dateTimes.get(index).getTimeInMillis()));
                    textViewDate.setText(dateformat);
                } catch (Exception es) {
                    es.printStackTrace();
                }

            }

            @Override
            public void onNothingSelected() {

            }
        });


    }

}

class MinuteStockPrice implements Serializable {
    public static String CODE;
    private float price;
    private String dateTime;

    public MinuteStockPrice(String dateTime, float price) {
        this.dateTime = dateTime;
        this.price = price;
    }

    public static String getCODE() {
        return CODE;
    }


    public float getPrice() {
        return price;
    }


    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
}
