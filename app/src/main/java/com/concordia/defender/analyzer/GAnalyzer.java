package com.concordia.defender.analyzer;



import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.concordia.defender.defender.DefenderDBHelper;
import com.concordia.defender.defender.DefenderTask;
import com.concordia.defender.model.CPUUsage;
import com.concordia.defender.model.Process;
import com.concordia.defender.model.IEModel;


/*
 * Resource usage global for entire device. Uses unique IEModel for that since
 * we follow the same structure.
 */
public class GAnalyzer extends DefenderTask {
    private final String TAG = GAnalyzer.class.getName();

    public GAnalyzer() {
        this.RunEvery = 60;
    }

    public void doWork(Context context) {
        DefenderDBHelper aidsDBHelper = DefenderDBHelper.getInstance(context);

        Calendar calendar = Calendar.getInstance();
        long currentTimeMillis = calendar.getTimeInMillis();
        Calendar prevCalendar = Calendar.getInstance();
        prevCalendar.add(Calendar.SECOND, -1 * RunEvery);
        long prevTimeMillis = prevCalendar.getTimeInMillis();

        String gModelName = "_global" + calendar.get(Calendar.HOUR_OF_DAY);


        // get global model
        IEModel gModel = aidsDBHelper.getIEModel(gModelName);

        if (gModel == null) {
            // first invocation
            gModel = new IEModel();
            gModel.ProcessName = gModelName;
            gModel.FromTimeStamp = prevTimeMillis;
            gModel.ToTimeStamp = currentTimeMillis;
            gModel.Age = 1;

            aidsDBHelper.insertIEModel(gModel);
        }

        // get IEModels of processes for past hour
        HashMap<String, IEModel> processMap = getIEModelsForProcesses(
                aidsDBHelper, prevTimeMillis, currentTimeMillis);

        for (String pName : processMap.keySet()) {
            IEModel newModel = processMap.get(pName);

            // update the model with current generation
            gModel.ToTimeStamp = currentTimeMillis;
            gModel.CPULow += newModel.CPULow;
            gModel.CPUMid += newModel.CPUMid;
            gModel.CPUHigh += newModel.CPUHigh;
            gModel.CPUCounter += newModel.CPUCounter;
            gModel.Age = gModel.Age + 1; // and increment the age

            aidsDBHelper.updateIEModel(gModel);
        }
    }

    // return hashmap of IEModel keyed by process name
    public static HashMap<String, IEModel> getIEModelsForProcesses(
            DefenderDBHelper aidsDBHelper, long fromTimeMillis, long toTimeMillis) {
        // hashmap keyed by process name and then attributes
        HashMap<String, IEModel> processMap = new HashMap<String, IEModel>();

        // get processes for specified period
        List<Process> processListForPeriod = aidsDBHelper.getProcesses(
                fromTimeMillis, toTimeMillis);
        // i have to iterate over them all because some could have different
        // PIDs
        // so i group them under process name
        for (Process p : processListForPeriod) {
            if (!processMap.containsKey(p.Name)) {
                processMap.put(p.Name, new IEModel());
            }

            IEModel pIEModel = processMap.get(p.Name);

            // get cpuusage for each process
            List<CPUUsage> cpuUsageForProcessList = aidsDBHelper.getCPUUsage(
                    p.Pid, fromTimeMillis, toTimeMillis);
            Log.i("hash", "" + cpuUsageForProcessList.size());
            for (CPUUsage cpu : cpuUsageForProcessList) {
                int cpuUsageInt = Integer.parseInt(cpu.CPUUsage);

                if (cpuUsageInt < 30) {
                    Log.d("HORROR4", "pid: " + cpuUsageInt);
                } else if (cpuUsageInt < 60) {
                    Log.d("HORROR4", "pid: " + cpuUsageInt);
                } else {
                    Log.d("HORROR4", "pid: " + cpuUsageInt);
                }

                pIEModel.CPUCounter = pIEModel.CPUCounter + 1;
            }

          }
            return processMap;
        }
    }
