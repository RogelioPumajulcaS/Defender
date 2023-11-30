package com.concordia.defender.defender;


import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import com.concordia.defender.model.Event;
import com.concordia.defender.model.Process;
import com.concordia.defender.model.CPUUsage;
import com.concordia.defender.model.IEModel;
public class DefenderDBHelper extends SQLiteOpenHelper {
    private final String TAG = DefenderDBHelper.class.getName();

    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_NAME = "stats.db";

    // process table, collecting process name and timestamp
    private static final String PROCESS_TABLE_NAME = "process";
    private static final String ID = "id";
    private static final String TIMESTAMP = "timestamp";

    private static final String PROCESS_UID = "process_uid";
    private static final String PROCESS_PID = "process_pid";
    private static final String PROCESS_NAME = "process_name";

    private static final String PROCESS_TABLE_CREATE = "CREATE TABLE "
            + PROCESS_TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY,"
            + TIMESTAMP + " int," + PROCESS_UID + " TEXT, " + PROCESS_PID
            + " TEXT, " + PROCESS_NAME + " TEXT " + ");";






    // cpu usage table, collecting cpu usage and pid
    private static final String CPUUSAGE_TABLE_NAME = "cpuusage";
    private static final String CPUUSAGE_CPU = "cpu";

    private static final String CPUUSAGE_TABLE_CREATE = "CREATE TABLE "
            + CPUUSAGE_TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY,"
            + TIMESTAMP + " int," + PROCESS_PID + " TEXT, " + CPUUSAGE_CPU
            + " TEXT " + ");";


    // table for IEModel, to hold cumulative resources
    // fromts, tots, processname, lowcpu, midcpu, highcpu
    private static final String IEMODEL_TABLE_NAME = "iemodel";
    private static final String IEMODEL_FROM_TS = "fromts";
    private static final String IEMODEL_TO_TS = "tots";
    private static final String IEMODEL_AGE = "age";
    private static final String IEMODEL_CPU_LOW = "cpu_low";
    private static final String IEMODEL_CPU_MID = "cpu_mid";
    private static final String IEMODEL_CPU_HIGH = "cpu_high";
    private static final String IEMODEL_CPU_COUNTER = "cpu_counter";

    private static final String IEMODEL_TABLE_CREATE = "CREATE TABLE "
            + IEMODEL_TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY,"
            + IEMODEL_FROM_TS + " int," + IEMODEL_TO_TS + " int, "
            + PROCESS_NAME + " TEXT, " + IEMODEL_CPU_LOW + " int, "
            + IEMODEL_CPU_MID + " int, " + IEMODEL_CPU_HIGH + " int, "
            + IEMODEL_CPU_COUNTER + " int, " + IEMODEL_AGE + " int "
            + ");";


    // events table, collecting platform events such as screen on/off toggle and
    // app install plus extra metadata depending on event type
    private static final String EVENT_TABLE_NAME = "event";
    private static final String EVENT_TYPE = "type";
    private static final String EVENT_MORE = "more";

    private static final String EVENT_TABLE_CREATE = "CREATE TABLE "
            + EVENT_TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY,"
            + TIMESTAMP + " int," + EVENT_TYPE + " int, " + EVENT_MORE
            + " TEXT " + ");";



    private static final String[] tables = new String[] { PROCESS_TABLE_NAME,
            CPUUSAGE_TABLE_NAME, EVENT_TABLE_NAME};

    private DefenderDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PROCESS_TABLE_CREATE);
        db.execSQL(CPUUSAGE_TABLE_CREATE);
        db.execSQL(EVENT_TABLE_CREATE);
        db.execSQL(IEMODEL_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // this will be reconsidered but on upgrade conserve old data just in
        // case...
        for (String tabname : tables) {
            try {
                db.execSQL(String.format("ALTER TABLE %s RENAME TO %s",
                        tabname, tabname + oldVersion));
            } catch (Exception e) {
                // table probably doesnt exist
                continue;
            }
        }

        onCreate(db);
    }

    private static DefenderDBHelper mDBHelper;

    public synchronized static DefenderDBHelper getInstance(Context context) {

        if (mDBHelper == null) {
            mDBHelper = new DefenderDBHelper(context);
        }

        return mDBHelper;
    }

    public boolean insertProcess(Process p) {
        SQLiteDatabase defenderDB = this.getWritableDatabase();

        long insertedID = 0;
        ContentValues values = new ContentValues();

        values.put(TIMESTAMP, p.TimeStamp);
        values.put(PROCESS_PID, p.Pid);
        values.put(PROCESS_NAME, p.Name);
        values.put(PROCESS_UID, p.Uid);

        try {
            defenderDB.beginTransaction();
            insertedID = defenderDB.insert(PROCESS_TABLE_NAME, null, values);
            defenderDB.setTransactionSuccessful();
        } finally {
            defenderDB.endTransaction();
        }

        if (insertedID == -1) {
            return false;
        }

        return true;
    }

    public boolean insertCPUUsage(CPUUsage cu) {
        SQLiteDatabase defenderDB = this.getWritableDatabase();

        long insertedID = 0;
        ContentValues values = new ContentValues();

        values.put(TIMESTAMP, cu.TimeStamp);
        values.put(PROCESS_PID, cu.Pid);
        values.put(CPUUSAGE_CPU, cu.CPUUsage);

        try {
            defenderDB.beginTransaction();
            insertedID = defenderDB.insert(CPUUSAGE_TABLE_NAME, null, values);
            defenderDB.setTransactionSuccessful();
        } finally {
            defenderDB.endTransaction();
        }

        if (insertedID == -1) {
            return false;
        }

        return true;
    }

    public boolean insertEvent(Event ev) {
        SQLiteDatabase aidsDB = this.getWritableDatabase();

        long insertedID = 0;
        ContentValues values = new ContentValues();

        values.put(TIMESTAMP, ev.TimeStamp);
        values.put(EVENT_TYPE, ev.Type.ordinal());
        values.put(EVENT_MORE, ev.More);

        try {
            aidsDB.beginTransaction();
            insertedID = aidsDB.insert(EVENT_TABLE_NAME, null, values);
            aidsDB.setTransactionSuccessful();
        } finally {
            aidsDB.endTransaction();
        }
        if (insertedID == -1) {
            return false;
        }

        return true;
    }



    public List<Process> getProcesses(long fromTS, long toTS) {
        SQLiteDatabase aidsDB = this.getReadableDatabase();
        ArrayList<Process> pList = new ArrayList<Process>();

        SQLiteCursor cursor = (SQLiteCursor) aidsDB.query(true,
                PROCESS_TABLE_NAME, new String[] { PROCESS_NAME, PROCESS_PID,
                        PROCESS_UID }, TIMESTAMP + " between ? and ?",
                new String[] { String.valueOf(fromTS), String.valueOf(toTS) },
                null, null, null, null);

        if (cursor.getCount() == 0) {
            return pList;
        }

        while (cursor.moveToNext()) {
            Process p = new Process();
            p.Name = cursor.getString(0);
            p.Pid = cursor.getString(1);
            p.Uid = cursor.getString(2);

            pList.add(p);
        }
        Log.d("myTag", "LISTO procesos");
        return pList;
    }


    public List<CPUUsage> getCPUUsage(String pid, long fromTS, long toTS) {
        SQLiteDatabase aidsDB = this.getReadableDatabase();
        ArrayList<CPUUsage> cpuList = new ArrayList<CPUUsage>();

        SQLiteCursor cursor = (SQLiteCursor) aidsDB.query(CPUUSAGE_TABLE_NAME,
                new String[] { CPUUSAGE_CPU }, TIMESTAMP
                        + " between ? and ? and " + PROCESS_PID + "=?",
                new String[] { String.valueOf(fromTS), String.valueOf(toTS),
                        pid }, null, null, null);

        if (cursor.getCount() == 0) {
            return cpuList;
        }

        while (cursor.moveToNext()) {
            CPUUsage cpu = new CPUUsage();
            cpu.CPUUsage = cursor.getString(0);

            cpuList.add(cpu);
            Log.d("myTag", "LISTO CPUs");
        }

        //ursor.close();

        return cpuList;
    }

    // get IEModel for process
    public IEModel getIEModel(String pName) {
        SQLiteDatabase aidsDB = this.getReadableDatabase();

        SQLiteCursor cursor = (SQLiteCursor) aidsDB.query(IEMODEL_TABLE_NAME,
                new String[] { IEMODEL_FROM_TS, IEMODEL_TO_TS, IEMODEL_CPU_LOW,
                        IEMODEL_CPU_MID, IEMODEL_CPU_HIGH, ID,
                        IEMODEL_CPU_COUNTER, IEMODEL_AGE
                         },
                PROCESS_NAME + "=?", new String[] { pName }, null, null, null);

        if (cursor.getCount() == 0) {
            return null;
        }

        cursor.moveToFirst(); // only one model for process is expected

        IEModel iem = new IEModel();
        iem.ProcessName = pName;
        iem.FromTimeStamp = cursor.getLong(0);
        iem.ToTimeStamp = cursor.getLong(1);
        iem.CPULow = cursor.getInt(2);
        iem.CPUMid = cursor.getInt(3);
        iem.CPUHigh = cursor.getInt(4);
        iem.ID = cursor.getInt(5);
        iem.CPUCounter = cursor.getInt(6);
        iem.Age = cursor.getInt(7);
        iem.RxBytes = cursor.getInt(8);
        iem.TxBytes = cursor.getInt(9);

        cursor.close();

        return iem;
    }

    // get all IEModels
    public List<IEModel> getIEModel() {
        SQLiteDatabase aidsDB = this.getReadableDatabase();
        ArrayList<IEModel> ieModelList = new ArrayList<IEModel>();

        SQLiteCursor cursor = (SQLiteCursor) aidsDB.query(IEMODEL_TABLE_NAME,
                new String[] { IEMODEL_FROM_TS, IEMODEL_TO_TS, IEMODEL_CPU_LOW,
                        IEMODEL_CPU_MID, IEMODEL_CPU_HIGH, ID,
                        IEMODEL_CPU_COUNTER, PROCESS_NAME, IEMODEL_AGE
                         }, null,
                null, null, null, null);

        if (cursor.getCount() == 0) {
            return ieModelList;
        }

        while (cursor.moveToNext()) {
            IEModel iem = new IEModel();
            iem.FromTimeStamp = cursor.getLong(0);
            iem.ToTimeStamp = cursor.getLong(1);
            iem.CPULow = cursor.getInt(2);
            iem.CPUMid = cursor.getInt(3);
            iem.CPUHigh = cursor.getInt(4);
            iem.ID = cursor.getInt(5);
            iem.CPUCounter = cursor.getInt(6);
            iem.ProcessName = cursor.getString(7);
            iem.Age = cursor.getInt(8);
            iem.RxBytes = cursor.getInt(9);
            iem.TxBytes = cursor.getInt(10);

            ieModelList.add(iem);
        }

        cursor.close();

        return ieModelList;
    }

    public boolean updateIEModel(IEModel iem) {
        SQLiteDatabase aidsDB = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(IEMODEL_FROM_TS, iem.FromTimeStamp);
        values.put(IEMODEL_TO_TS, iem.ToTimeStamp);
        values.put(PROCESS_NAME, iem.ProcessName);
        values.put(IEMODEL_CPU_LOW, iem.CPULow);
        values.put(IEMODEL_CPU_MID, iem.CPUMid);
        values.put(IEMODEL_CPU_HIGH, iem.CPUHigh);
        values.put(IEMODEL_CPU_COUNTER, iem.CPUCounter);
        values.put(IEMODEL_AGE, iem.Age);


        int affectedRows = aidsDB.update(IEMODEL_TABLE_NAME, values, ID + "=?",
                new String[] { String.valueOf(iem.ID) });

        if (affectedRows != 1) {
            return false;
        }

        return true;
    }

    public boolean insertIEModel(IEModel iem) {
        SQLiteDatabase aidsDB = this.getWritableDatabase();

        long insertedID = 0;
        ContentValues values = new ContentValues();

        values.put(IEMODEL_FROM_TS, iem.FromTimeStamp);
        values.put(IEMODEL_TO_TS, iem.ToTimeStamp);
        values.put(PROCESS_NAME, iem.ProcessName);
        values.put(IEMODEL_CPU_LOW, iem.CPULow);
        values.put(IEMODEL_CPU_MID, iem.CPUMid);
        values.put(IEMODEL_CPU_HIGH, iem.CPUHigh);
        values.put(IEMODEL_CPU_COUNTER, iem.CPUCounter);
        values.put(IEMODEL_AGE, iem.Age);


        try {
            aidsDB.beginTransaction();
            insertedID = aidsDB.insert(IEMODEL_TABLE_NAME, null, values);
            aidsDB.setTransactionSuccessful();
        } finally {
            aidsDB.endTransaction();
        }

        if (insertedID == -1) {
            return false;
        }

        return true;
    }


    public boolean resetAllData() {
        SQLiteDatabase aidsDB = this.getWritableDatabase();

        Log.i(TAG, "Resetting data based on user command");

        for (String tabname : tables) {
            aidsDB.delete(tabname, "1", null);
        }

        return true;
    }






}
