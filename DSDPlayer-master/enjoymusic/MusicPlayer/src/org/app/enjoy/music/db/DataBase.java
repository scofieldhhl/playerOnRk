package org.app.enjoy.music.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import org.app.enjoy.music.tool.Contsant;

/**
 * Created by victor on 2015/12/25.
 */
public class DataBase extends SQLiteOpenHelper{

    public DataBase(Context context, String name, SQLiteDatabase.CursorFactory factory,
                    int version) {
        super(context, Contsant.DbConfig.DB_NAME, null, Contsant.DbConfig.DB_VERSION);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createCategoryTb(db);
        createMusicTb(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	if (tabbleIsExist(Contsant.TB.CATEGORY)) {
    		db.execSQL("drop table " + Contsant.TB.CATEGORY);
    	}
    	if (tabbleIsExist(Contsant.TB.MUSIC)) {
    		db.execSQL("drop table " + Contsant.TB.MUSIC);
    	}

        onCreate(db);
    }

    private void createCategoryTb(SQLiteDatabase db) {
        String sysSql = "create table if not exists " + Contsant.TB.CATEGORY +
                "(_id integer primary key autoincrement,category text)";

        db.execSQL(sysSql);
    }
    private void createMusicTb(SQLiteDatabase db) {
        String sysSql = "create table if not exists " + Contsant.TB.MUSIC +
                "(_id integer primary key autoincrement,title text,duration long," +
                "artist text,id integer,displayName text,data text,path text," +
                "albumId text,album text,size text,category_type text)";//category_type 分类标示

        db.execSQL(sysSql);
    }

    /**
     * 判断某张表是否存在
     * @param tableName 表名
     * @return
     */
    public boolean tabbleIsExist(String tableName){
        boolean result = false;
        if(TextUtils.isEmpty(tableName)){
                return false;
        }
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase();
            String sql = "select count(1) from "+"sqlite_master "+" where type ='table' and name ='"+tableName.trim()+"' ";
            cursor = db.rawQuery(sql, null);
            if(cursor.moveToNext()){
                int count = cursor.getInt(0);
                if(count>0){
                        result = true;
                }
            }

        } catch (Exception e) {
                // TODO: handle exception
        } finally {
        	if (cursor != null) {
        		cursor.close();
        	}
        }
        return result;
    }
}
