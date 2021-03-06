package tw.mics.spigot.plugin.nomoreesp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class XRayDetect {
    static HashMap<UUID, LinkedHashMap<Block, HashSet<Block>>> value_block_count_block_set;
    static HashMap<UUID, HashSet<Block>> player_breaked_block;
    static HashMap<UUID, Double> player_vl;
    static HashMap<Material, Double> config_block_value;
    public static void initData(){
        value_block_count_block_set = new HashMap<UUID, LinkedHashMap<Block,HashSet<Block>>>();
        player_breaked_block = new HashMap<UUID, HashSet<Block>>();
        player_vl = new HashMap<UUID, Double>();
        config_block_value = new HashMap<Material, Double>();
        
        for(String str : Config.XRAY_DETECT_ADD_VL_BLOCK_AND_NUMBER.getStringList()){
            String[] str_split = str.split(":");
            String material = str_split[0];
            String value = str_split[1];
            config_block_value.put(Material.valueOf(material), Double.valueOf(value));
        }
    }

    public static void checkUUIDDataExist(UUID player){
        int maxEntries = 20; //每個玩家記錄多少個方塊價值列表
        if(!value_block_count_block_set.containsKey(player)){
            value_block_count_block_set.put(player,new LinkedHashMap<Block,HashSet<Block>>(maxEntries*10/7, 0.7f, true){
                private static final long serialVersionUID = 7122398289557675273L;
                @Override
                protected boolean removeEldestEntry(Map.Entry<Block, HashSet<Block>> eldest) {
                    return size() > maxEntries;
                }
            });
        }
        if(!player_breaked_block.containsKey(player)){
            player_breaked_block.put(player,new HashSet<Block>());
        }
        if(!player_vl.containsKey(player)){
            player_vl.put(player, 0.0);
        }
    }
    
    public static void removeUUID(UUID player){
        value_block_count_block_set.remove(player);
        player_breaked_block.remove(player);
        player_vl.remove(player);
    }
    
    public static LinkedHashMap<Block, HashSet<Block>> getValueBlockCountBlockSet(UUID player){
        return value_block_count_block_set.get(player);
    }
    
    public static Double getBlockValue(Material m){
        return config_block_value.get(m);
    }

    public static void playerBreakBlock(UUID player, Block block, Material block_type) {
        new Thread(new Runnable(){
            @Override
            public void run() {
                //初始化方塊相關變數
                Double block_value = getBlockValue(block_type);
                String block_location_string = block.getX() + " " + block.getY() + " " + block.getZ();

                //如果已經是最近挖過的方塊 不計算
                HashSet<Block> breaked_block = player_breaked_block.get(player);
                if(breaked_block.contains(block)) {
                    NoMoreESP.getInstance().logDebug( "%s mining block(%s) mined before, skip change VL.", Bukkit.getPlayer(player).getName(), block_location_string);
                    return;
                }
                breaked_block.add(block);
                
                //如果挖的是無價值方塊
                if(block_value == null){
                    if(player_vl.get(player) > 0) {
                        NoMoreESP.getInstance().logDebug("%s mining block(%s) is non-valuable, VL minus %.3f", Bukkit.getPlayer(player).getName(), block_location_string, Config.XRAY_MINUX_VL.getDouble());
                        player_vl.put(player, player_vl.get(player) - Config.XRAY_MINUX_VL.getDouble());
                    }
                    return;
                }

                //不是無價值方塊 計算 block_count 數量來加成 vl
                LinkedHashMap<Block, HashSet<Block>> vl_bouns_block = value_block_count_block_set.get(player);
                int block_count = 0;
                if(vl_bouns_block.get(block) != null){
                    Iterator<Block> itr = vl_bouns_block.get(block).iterator();
                    while(itr.hasNext()){
                        if(breaked_block.contains(itr.next())){
                            block_count++;
                        }
                    }
                }

                //計算 vl
                Double vl = block_value * (block_count);

                //特殊計算 (黃金)
                if(block_type == Material.GOLD_ORE){
                    switch(block.getBiome()){
                    case BADLANDS:
                    case ERODED_BADLANDS:
                    case WOODED_BADLANDS_PLATEAU:
                    case MODIFIED_BADLANDS_PLATEAU:
                    case MODIFIED_WOODED_BADLANDS_PLATEAU:
                    case BADLANDS_PLATEAU:
                        vl /= Config.XRAY_DETECT_GOLD_VL_DIVIDED_NUMBER_IN_MESA.getDouble();
                    default:
                        break;
                    }
                }
                
                NoMoreESP.getInstance().log( "%s mining block(%s) is valuable, VL add %.3f (value: %f, count: %d)", Bukkit.getPlayer(player).getName(), block_location_string, vl, block_value, block_count);
                player_vl.put(player, player_vl.get(player) + vl);
                
                //reach vl and run command
                if(player_vl.get(player) > Config.XRAY_DETECT_RUN_COMMAND_VL.getInt()){
                    
                    //run command
                    String str = Config.XRAY_DETECT_RUN_COMMAND.getString().replace("%PLAYER%", Bukkit.getPlayer(player).getName());
                    if(!str.isEmpty()){

                        //log
                        String msg = "%s is reach command VL (now VL is %.3f)";
                        NoMoreESP.getInstance().log(msg, Bukkit.getPlayer(player).getName(), player_vl.get(player));

                        Bukkit.getScheduler().scheduleSyncDelayedTask(NoMoreESP.getInstance(), new Runnable(){
                            @Override
                            public void run() {
                                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), str);
                            }
                        });

                        player_vl.put(player, 0.0); //reset VL
                    }
                }
                
                //debug message
                NoMoreESP.getInstance().log( "%s's VL is now %.3f",Bukkit.getPlayer(player).getName(), player_vl.get(player));
            }
        }).start();
    }
}
