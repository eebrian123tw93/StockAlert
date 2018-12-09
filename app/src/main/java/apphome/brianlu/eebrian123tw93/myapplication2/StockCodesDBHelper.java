package apphome.brianlu.eebrian123tw93.myapplication2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by eebrian123tw93 on 2018/2/25.
 */

public class StockCodesDBHelper extends SQLiteOpenHelper {
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "StockCodes.db";
    public static final String SQL_CREATE_TABLE_STOCKCODE = " CREATE TABLE " + StockCodesDBEntry.TABLE_NAME_STOCKCODE +
            " (" + StockCodesDBEntry.COLUMN_NAME_CODE + TEXT_TYPE + " PRIMARY KEY," +
            StockCodesDBEntry.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
            StockCodesDBEntry.COLUMN_NAME_CLASS + TEXT_TYPE + " )";
    public static final String SQL_CREATE_TABLE_CANDIDATE = " CREATE TABLE " + StockCodesDBEntry.TABLE_NAME_CANDIDATE +
            " (" + StockCodesDBEntry.COLUMN_NAME_CODE + TEXT_TYPE + " PRIMARY KEY," +
            StockCodesDBEntry.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
            StockCodesDBEntry.COLUMN_NAME_CLASS + TEXT_TYPE + " )";

    public static final String SQL_CREATE_TABLE_DAY = " CREATE TABLE " + StockCodesDBEntry.TABLE_NAME_DAY +
            " (" + StockCodesDBEntry.COLUMN_NAME_CODE + TEXT_TYPE + COMMA_SEP +
            StockCodesDBEntry.COLUMN_NAME_MINUTE + COMMA_SEP +
            StockCodesDBEntry.COLUMN_NAME_PRICE + TEXT_TYPE + " )";

    public StockCodesDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_STOCKCODE);
        db.execSQL(SQL_CREATE_TABLE_CANDIDATE);
        db.execSQL(SQL_CREATE_TABLE_DAY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
