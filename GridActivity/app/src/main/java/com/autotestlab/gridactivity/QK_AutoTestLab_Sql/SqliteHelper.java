package com.autotestlab.gridactivity.QK_AutoTestLab_Sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Administrator on 2016/8/1.
 */
public class SqliteHelper extends SQLiteOpenHelper {

    public static final int version = 1;
    public static final String TB_NAME = "QK_TAB";
    public static final String DB_NAME = "QK_DB";

    public SqliteHelper(Context context) {

        super(context,DB_NAME,null,version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL( "DROP TABLE IF EXISTS " + TB_NAME );

        Log.e("sqlitehelper","delete table  if exiset ");

        db.execSQL("CREATE TABLE IF NOT EXISTS " +
                TB_NAME + "(" +
                TabInfo.ID + " integer primary key," +
                TabInfo.PHONENAME + " varchar," +
                TabInfo.VERSION + " varchar," +
                TabInfo.STRESSTOOLSNAME + " VARCHAR(50)," +
                TabInfo.TIME + " varchar," +
                TabInfo.RESULT + " varchar " +
                ")"
        );
        Log.e(getClass().getSimpleName(), "onCreate");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL( "DROP TABLE IF EXISTS " + TB_NAME );
        onCreate(db);
        Log. e(getClass().getSimpleName(),"onUpgrade" );
    }


     public  void  deletetab(SQLiteDatabase db)
     {

         db.execSQL( "DROP TABLE IF EXISTS " + TB_NAME );

     }

}
