package org.tensorflow.strokechange.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DBManager {

    private DatabaseHelper dbHelper;

    private Context context;

    private SQLiteDatabase database;

    public DBManager(Context c) {
        context = c;
    }

    public DBManager open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        dbHelper.close();
    }

    public void insert(double eyeSeverity, double mouthSeverity, String fileName) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strDate = sdf.format(new Date());

        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.EyeSeverity, eyeSeverity);
        contentValue.put(DatabaseHelper.MouthSeverity, mouthSeverity);
        contentValue.put(DatabaseHelper.DateTime,strDate);
        contentValue.put(DatabaseHelper.ImageFileName,fileName);
        database.insert(DatabaseHelper.TABLE_NAME, null, contentValue);
    }

    public Cursor fetch() {
        String[] columns = new String[] { DatabaseHelper._ID, DatabaseHelper.DateTime, DatabaseHelper.EyeSeverity, DatabaseHelper.MouthSeverity, DatabaseHelper.ImageFileName };
        Cursor cursor = database.query(DatabaseHelper.TABLE_NAME, columns, null, null, null, null, DatabaseHelper.DateTime);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public int update(Timestamp datetime, String name, String desc) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.EyeSeverity, name);
        contentValues.put(DatabaseHelper.MouthSeverity, desc);
        int i = database.update(DatabaseHelper.TABLE_NAME, contentValues, DatabaseHelper.DateTime + " = " + datetime, null);
        return i;
    }

    public void delete(long _id) {
        database.delete(DatabaseHelper.TABLE_NAME, DatabaseHelper._ID + "=" + _id, null);
    }

}