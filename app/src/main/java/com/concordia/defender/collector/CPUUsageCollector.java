package com.concordia.defender.collector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.content.Context;
import android.nfc.Tag;
import android.util.Log;

import com.concordia.defender.defender.DefenderDBHelper;
import com.concordia.defender.defender.DefenderTask;
import com.concordia.defender.model.CPUUsage;
public class CPUUsageCollector extends DefenderTask {
    Pattern pidPattern = Pattern.compile("^\\s*(\\d+)\\s*");
    Pattern cpuUsagePattern = Pattern.compile("(\\d+)%");

    public CPUUsageCollector(){
        this.RunEvery = 5;
    }

    public void doWork(Context context) {
        DefenderDBHelper defDBHelper = DefenderDBHelper.getInstance(context);
        //AIDSDBHelper aidsDBHelper = AIDSDBHelper.getInstance(context);

        // get cpu usage for processes
        try {

            Process topProcess = Runtime.getRuntime().exec("top -n 1");
           //Process topProcess = Runtime.getRuntime().exec("top -n 1 -d 0");

            BufferedReader bufferedStream = new BufferedReader(
                    new InputStreamReader(topProcess.getInputStream()));
            String tLine;
            bufferedStream.readLine(); // skip over 1st empty line
            bufferedStream.readLine(); // second empty line
            bufferedStream.readLine(); // skip over USER and SYSTEM CPU
            bufferedStream.readLine(); // skip over USER and NICE
            //bufferedStream.readLine(); // skip over column titles

/*Antes: ini
            while ((tLine = bufferedStream.readLine()) != null) {

                Log.d("HORROR", "1.- bufferedStream.readLine() != null");

                Matcher pidMatcher = pidPattern.matcher(tLine);
                Matcher cpuMatcher = cpuUsagePattern.matcher(tLine);

                Log.d("HORROR", "2.- YA GUARDO pidMatcher cpuMatcher");

                if (!pidMatcher.find() || !cpuMatcher.find()) {
                    // can't find pid or cpu usage, probably line we're not
                    // interested in
                    Log.d("HORROR", "3.- YA entro aca !pidMatcher.find() || !cpuMatcher.find()");
                    continue;
                }

                Log.d("HORROR", "4.- va guardar  pidmatcher.group y cpumatcher.grouo");
                String pid = pidMatcher.group();
                String cpuUsage = cpuMatcher.group();

                Log.d("HORROR", "4.- YA GUARDO pid y cpuUsage");

                if (tLine.contains("root")) {
                    continue;
                }

                // int pidInt = Integer.parseInt(pid.replaceAll("\\s", ""));

                CPUUsage cu = new CPUUsage();
                cu.TimeStamp = System.currentTimeMillis();
                cu.Pid = pid.replaceAll("\\s", "");
                cu.CPUUsage = cpuUsage.substring(0, cpuUsage.length() - 1); //cpu usage minus the % char

                Log.d("HORROR", "LLEGA A defDBHelper.insertCPUUsage(cu)");
                defDBHelper.insertCPUUsage(cu);
                Log.d("HORROR", "EJECUTO A defDBHelper.insertCPUUsage(cu)");
            }
Antes: fin*/

            boolean cabe=false;
            while ((tLine = bufferedStream.readLine()) != null) {

                if (tLine.contains("CPU")){
                    cabe=true;
                    continue;
                }

                if (cabe){
                    String[] lista =tLine.split(" ");
                    List<String> lis=new ArrayList<>();
                    for (int i=0;i<lista.length;i++){
                        if (!lista[i].equals("") && lista[i] !=null && !lista[i].equals("R") && !lista[i].contains("[1m")){ //modifiq aca
                            lis.add(lista[i]);
                            Log.d("HORROR1","lista["+i+"]= "+ lista[i]);
                        }
                    }
                    String cpuUsage=lis.get(8)+"%";
                    String cpuUsa=lis.get(8);
                    String pid = lis.get(0);

                  // if((Long.parseLong(pid) > 1 && Float.parseFloat(cpuUsa)>0.0)){ // borrar ini
                        Log.d("HORROR", "cpu Usage: " + cpuUsage);
                        Log.d("HORROR", "pid: " + pid);
                        CPUUsage cu = new CPUUsage();
                        cu.TimeStamp = System.currentTimeMillis();
                        cu.Pid = pid;
                        cu.CPUUsage = cpuUsage;
                        Log.e("HORROR", "esteee cu.toString() " + cu.toString());

                        defDBHelper.insertCPUUsage(cu);
                        cabe = false;
                   // }else{ cabe=true;} //borrar end
                }
            }


        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.d("HORROR", "ESTE ES EL CATCH");
            e.printStackTrace();
        }
    }

}
