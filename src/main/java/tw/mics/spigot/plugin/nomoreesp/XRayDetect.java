package tw.mics.spigot.plugin.nomoreesp;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class XRayDetect {
    static HashMap<Player, LinkedHashMap<Block,Double>> player_break_block_add_vl;
    static HashMap<Player, Double> player_vl;
    static HashMap<Material, Double> config_block_value;
    public static void initData(){
        player_break_block_add_vl = new HashMap<Player, LinkedHashMap<Block,Double>>();
        player_vl = new HashMap<Player, Double>();
        config_block_value = new HashMap<Material, Double>();
        
        for(String str : Config.XRAY_DETECT_ADD_VL_BLOCK_AND_NUMBER.getStringList()){
            String[] str_split = str.split(":");
            String material = str_split[0];
            String value = str_split[1];
            config_block_value.put(Material.valueOf(material), Double.valueOf(value));
        }
    }

    public static void checkPlayerDataExist(Player player){
        if(!player_break_block_add_vl.containsKey(player)){
            int maxEntries = 1000;
            player_break_block_add_vl.put(player,new LinkedHashMap<Block,Double>(maxEntries*10/7, 0.7f, true){
                private static final long serialVersionUID = 7122398289557675273L;
                @Override
                protected boolean removeEldestEntry(Map.Entry<Block, Double> eldest) {
                    return size() > maxEntries;
                }
            });
        }
        if(!player_vl.containsKey(player)){
            player_vl.put(player, 0.0);
        }
    }
    
    public static void removePlayer(Player player){
        player_break_block_add_vl.remove(player);
    }
    
    public static LinkedHashMap<Block, Double> getBreakAddVL(Player player){
        checkPlayerDataExist(player);
        return player_break_block_add_vl.get(player);
    }
    
    public static HashMap<Material, Double> getBlockValue(){
        return config_block_value;
    }

    public static void playerBreakBlock(Player player, Block block) {
        checkPlayerDataExist(player);
        Double vl = player_break_block_add_vl.get(player).get(block);
        if(vl != null){
            player_vl.put(player, player_vl.get(player) + vl);
            if(Config.DEBUG.getBoolean()){
                NoMoreESP.getInstance().log(player.getName() + "'s VL is now " + player_vl.get(player));
            }
            
            if(player_vl.get(player) > Config.XRAY_DETECT_RUN_COMMAND_VL.getInt()){
                String str = Config.XRAY_DETECT_RUN_COMMAND.getString().replace("%PLAYER%", player.getName());
                if(!str.isEmpty()) Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), str);
                NoMoreESP.getInstance().logInToFile("Player " + player.getName() +
                        " is reach command vl (now vl is" + player_vl.get(player) + ")" );
            }
        } else if(player_vl.get(player) > 0) {
            player_vl.put(player, player_vl.get(player) - 1);
        }
    }
}
