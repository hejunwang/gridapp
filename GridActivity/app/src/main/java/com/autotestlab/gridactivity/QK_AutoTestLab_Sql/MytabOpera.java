package com.autotestlab.gridactivity.QK_AutoTestLab_Sql;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by Administrator on 2016/8/1.
 */
public class MytabOpera {

    private SQLiteDatabase db = null;
    private static final String TABNAME = "QK_tab";


    public MytabOpera(SQLiteDatabase sqLiteDatabase) {

        this.db = sqLiteDatabase;
    }


    /*insert*/
    public void insert(String phonename, String stresstoolsname,
                       String versionname, int time, String result) {
        ContentValues cv = new ContentValues();

        cv.put(TabInfo.PHONENAME, phonename);
        cv.put(TabInfo.STRESSTOOLSNAME, stresstoolsname);
        cv.put(TabInfo.VERSION, versionname);
        cv.put(TabInfo.TIME, time);
        cv.put(TabInfo.RESULT, result);

        this.db.insert(TABNAME, null, cv);
        this.db.close();

    }


    /**
     * 删除数据库
     *
     * @return
     */
    public void delettable() {
        db.delete(TABNAME, null, null);
        db.close();
    }


}
