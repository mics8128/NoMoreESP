package tw.mics.spigot.plugin.nomoreesp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import tw.mics.spigot.plugin.nomoreesp.schedule.CheckSchedule;

public class NoMoreESP extends JavaPlugin {
    private static NoMoreESP INSTANCE;
    private CheckSchedule checkschedule;

    @Override
    public void onEnable() {
        INSTANCE = this;
        Config.load();
        if(Config.HIDE_ENTITY_ENABLE.getBoolean()){
            checkschedule = new CheckSchedule(this);
        }
    }
    
    @Override
    public void onDisable() {
        this.logDebug("Unregister Listener!");
        HandlerList.unregisterAll();
        this.logDebug("Unregister Schedule tasks!");
        this.getServer().getScheduler().cancelTasks(this);
        checkschedule.removeRunnable();
    }

    public static NoMoreESP getInstance() {
        return INSTANCE;
    }
    
    //log system

    public void log(String str, Object... args) {
        String message = String.format(str, args);
        if(Config.LOG_IN_FILE.getBoolean()){
            logInToFile(message);
        }
        if (Config.LOG_IN_CONSOLE.getBoolean()) {
            getLogger().info(message);
        }
    }

    public void logDebug(String str, Object... args) {
        String message = String.format(str, args);
        if(Config.DEBUG_IN_FILE.getBoolean()){
            logDebugInToFile(message);
        }
        if (Config.DEBUG_IN_CONSOLE.getBoolean()) {
            getLogger().info("(DEBUG) " + message);
        }
    }
    
    private void logDebugInToFile(String msg){
        new Thread(() -> {
            try
            {
                File dataFolder = this.getDataFolder();
                if(!dataFolder.exists()){
                    dataFolder.mkdir();
                }
                File saveTo = new File(dataFolder, "debug.log");
                if (!saveTo.exists()){
                    saveTo.createNewFile();
                }
                FileWriter fw = new FileWriter(saveTo, true);
                PrintWriter pw = new PrintWriter(fw);
                DateFormat dateFormat = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss] ");
                Calendar cal = Calendar.getInstance();
                pw.println(dateFormat.format(cal.getTime()) + msg);
                pw.flush();
                pw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void logInToFile(String msg){
        new Thread(() -> {
            try
            {
                File dataFolder = this.getDataFolder();
                if(!dataFolder.exists()){
                    dataFolder.mkdir();
                }
                File saveTo = new File(dataFolder, "detect.log");
                if (!saveTo.exists()){
                    saveTo.createNewFile();
                }
                FileWriter fw = new FileWriter(saveTo, true);
                PrintWriter pw = new PrintWriter(fw);
                DateFormat dateFormat = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss] ");
                Calendar cal = Calendar.getInstance();
                pw.println(dateFormat.format(cal.getTime()) + msg);
                pw.flush();
                pw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

}
