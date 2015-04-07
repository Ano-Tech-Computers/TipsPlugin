package no.atc.floyd.bukkit.tips;


import java.io.*;

//import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
//import org.bukkit.Location;
//import org.bukkit.Server;
//import org.bukkit.event.Event.Priority;
//import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.PluginDescriptionFile;
//import org.bukkit.plugin.Plugin;
//import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

import java.util.Date;
import java.util.Random;
import java.util.Scanner;

//import com.nijikokun.bukkit.Permissions.Permissions;

/**
* Tips plugin for Bukkit
*
* @author FloydATC
*/
public class TipsPlugin extends JavaPlugin implements Listener {
    //private final TipsPlayerListener playerListener = new TipsPlayerListener(this);

    private CopyOnWriteArrayList<String> tips = new CopyOnWriteArrayList<String>();
    
    //public static Permissions Permissions = null;
	public static final Logger logger = Logger.getLogger("Minecraft.TipsPlugin");
	public static Random rng = new Random((new Date()).getTime());
	private Long timer = (new Date()).getTime() / 1000;
	
//    public TipsPlugin(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
//        super(pluginLoader, instance, desc, folder, plugin, cLoader);
//        // TODO: Place any custom initialization code here
//
//        // NOTE: Event registration should be done in onEnable not here as all events are unregistered when a plugin is disabled
//    }

    public void onDisable() {
        // TODO: Place any custom disable code here

        // NOTE: All registered events are automatically unregistered when a plugin is disabled
    	
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
    	PluginDescriptionFile pdfFile = this.getDescription();
    	logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!" );
    }

    public void onEnable() {
        // TODO: Place any custom enable code here including the registration of any events
    	
    	//setupPermissions();
    	loadTips();
    	
        // Register our events
        PluginManager pm = getServer().getPluginManager();
        //pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
        //pm.registerEvent(Event.Type.PLAYER_CHAT, playerListener, Priority.Normal, this);
        pm.registerEvents((Listener) this, this);

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
        logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args ) {
//    	if (Permissions == null) {
//			logger.info( "Permission system not enabled." );
//    		return false;
//    	}
    	String cmdname = cmd.getName().toLowerCase();
        Player player = null;
        if (sender instanceof Player) {
        	player = (Player)sender;
        }
        
    	if (cmdname.equalsIgnoreCase("tip")) {
    		if (args.length > 0 && (player == null || player.hasPermission("tipsplugin.tip") )) {
    			tips.add(join(" ", args));
    			saveTips();
    			respond(player, "[Tips]§b Tip added");
    		} else {
    			respond(player, "[Tips]§b " + getRandomTip());
    		}
    		return true;
    	}
    	return false;
	}

    @EventHandler
    public void onPlayerChat( PlayerChatEvent event ) {
    	if (timerFired()) {
    		announce(getRandomTip());
    	}
    }

    @EventHandler
    public void onPlayerMove( PlayerMoveEvent event ) {
    	if (timerFired()) {
    		announce(getRandomTip());
    	}
    }
    
    private void announce(String tip) {
    	if (tip != null) {
	    	for (Player p : getServer().getOnlinePlayers()) {
	    		p.sendMessage("[Tips]§b " + tip);
	    	}
    	}
    }
   
//    public void setupPermissions() {
//    	Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");
//
//    	if(this.Permissions == null) {
//    	    if(test != null) {
//    	    	this.Permissions = (Permissions)test;
//    	    } else {
//    	    	logger.info( "[Tips] Permission system not enabled. Disabling plugin." );
//    	    	this.getServer().getPluginManager().disablePlugin(this);
//    	    }
//    	}
//    }
    
    private synchronized void loadTips() {
    	String fname = "plugins/TipsPlugin/tips.txt";
    	try {
    		Scanner scanner = new Scanner(new FileInputStream(fname), "ISO8859_1");
    		String line = null;
    		String newline = System.getProperty("line.separator");
    		while (scanner.hasNextLine()){
    			line = scanner.nextLine() + newline;
    			if (!line.matches("^#.*") && !line.matches("")) {
    				tips.add(line);
    			}
    		}		

//    		BufferedReader input =  new BufferedReader(new FileReader(fname));
//    		String line = null;
//    		while (( line = input.readLine()) != null) {
//    			line = line.trim();
//    			if (!line.matches("^#.*") && !line.matches("")) {
//    				tips.add(line);
//    			}
//    		}
//    		input.close();
    	}
    	catch (FileNotFoundException e) {
    		logger.warning("[Tips] File not found: " + fname);
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    }

	private synchronized void saveTips() {
    	String fname = "plugins/TipsPlugin/tips.txt";
   		BufferedWriter output;
   		String newline = System.getProperty("line.separator");
   	    try {
       		output = new BufferedWriter(new FileWriter(fname));
       		for (String tip : tips) {
           		output.write( tip + newline );
       		}
       		output.close();
   	    }
   	    catch (Exception e) {
    		e.printStackTrace();
   	    }
	}

	public String getRandomTip() {
		if (tips.size() == 0) {
			return "TipsPlugin/tips.txt is empty or does not exist";
		}
		return tips.get(rng.nextInt(tips.size()));
	}
	
	public synchronized boolean timerFired() {
		Long now = (new Date()).getTime() / 1000;
		if (timer + 300 < now) {
			timer = now;
			return true;
		} else {
			return false;
		}
	}
	
    private String join(String glue, String[] parts) {
    	String buf = "";
    	for (String part : parts) {
    		if (buf.length() > 0) {
    			buf = buf.concat(glue);
    		}
    		buf = buf.concat(part);
    	}
    	return buf;
    }

    private void respond(Player player, String message) {
    	if (player == null) {
    		System.out.println(message);
    	} else {
    		player.sendMessage(message);
    	}
    }

}
