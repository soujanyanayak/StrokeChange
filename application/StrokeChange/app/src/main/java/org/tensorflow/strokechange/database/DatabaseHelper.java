package org.tensorflow.strokechange.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.sql.Date;
import java.sql.Timestamp;

public class DatabaseHelper extends SQLiteOpenHelper {
    // Table Name
    public static final String TABLE_NAME = "Report";

    // Table columns
    public static final String _ID = "_id";
    public static final String DateTime = "datetime";
    public static final String EyeSeverity = "eyeSeverity";
    public static final String MouthSeverity = "mouthSeverity";
    public static final String ImageFileName = "imageFile";

    // Database Information
    static final String DB_NAME = "StrokeDetails.DB";

    // database version
    static final int DB_VERSION = 1;

    // Creating table query
    private static final String CREATE_TABLE = "create table " + TABLE_NAME + "(" + _ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DateTime +" DATETIME DEFAULT CURRENT_TIMESTAMP, " + EyeSeverity + " DOUBLE," + MouthSeverity + " DOUBLE, "+
            ImageFileName + " TEXT);";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
