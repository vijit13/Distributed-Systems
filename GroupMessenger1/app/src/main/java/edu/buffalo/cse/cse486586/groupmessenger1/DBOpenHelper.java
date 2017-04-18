package edu.buffalo.cse.cse486586.groupmessenger1;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by vijit on 2/19/17.
 */

public class DBOpenHelper extends SQLiteOpenHelper {

        private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME="messenger";
    public static final String DICTIONARY_TABLE_NAME = "dictionary";
    public static final String KEY="key";
    public static final String VALUE="value";
    public static final String[] ALL_COLUMNS={KEY,VALUE};
    public static final String DICTIONARY_TABLE_CREATE =
                "CREATE TABLE " + DICTIONARY_TABLE_NAME + " (" +
                        KEY + " TEXT PRIMARY KEY , " +
                       VALUE + " TEXT);";

        DBOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DICTIONARY_TABLE_CREATE);
        }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DATABASE_NAME);
        onCreate(db);
    }
}



