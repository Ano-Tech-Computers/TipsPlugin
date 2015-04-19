/*
 * Copyright (c) 2015 osvein
 */
package no.atc.osvein.bukkit.tips;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Gives players helpful tips
 * 
 * @author osvein <osvein@users.noreply.github.com>
 */
public class TipsPlugin extends JavaPlugin {
    
    public static final String COPYRIGHT = "%1$s, Copyright (C) 2015 Ano-Tech Computers";
    public static final String WARRANTY = "%1$s comes with ABSOLUTELY NO WARRANTY; for details see the LICENSE file.";
    
    /**
     * The file in which tips are stored
     */
    public static final String FNAME_TIPS = "tips.txt";
    
    /**
     * The random number generator used to fetch random tips
     */
    public static final Random rand = new Random();
    
    private String prefix;
    private long interval;
    
    // tip buffer; thread safe, lightweight reading, heavyweight writing
    private CopyOnWriteArrayList<String> tips = new CopyOnWriteArrayList<String>();
    
    private BukkitTask task;
    
    private class Broadcaster extends BukkitRunnable {
	
	/**
	 * Broadcasts a random tip
	 */
	@Override
	public void run() {
	    TipsPlugin.this.getServer().broadcast(TipsPlugin.this.prefix + TipsPlugin.this.getTip(), "tipsplugin.get");
	}
    }
    
    @Override
    public void onEnable() {
	this.saveDefaultConfig();
	
	// read configuration
	this.prefix = this.getConfig().getString("prefix");
	this.interval = this.getConfig().getLong("interval");
	this.loadTips();
	
	// schedule broadcasts
	if (interval > 0)
	    this.task = this.new Broadcaster().runTaskTimer(this, 0L, interval);
	
	// log legal notices
	String fullName = this.getDescription().getFullName();
	this.getLogger().info(String.format(COPYRIGHT, fullName));
	this.getLogger().info(String.format(WARRANTY, fullName));
    }
    
    @Override
    public void onDisable() {
	// cancel broadcast schedule
	this.getServer().getScheduler().cancelTasks(this);
	
	// save configuration
	this.saveTips();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
	if (command.getName().equalsIgnoreCase("tip")) {
	    if (args.length > 0 && sender.hasPermission("tipsplugin.add")) {
		// join args
		StringBuilder joiner = new StringBuilder();
		for (String arg : args)
		    joiner.append(arg).append(' ');
		String joined = joiner.toString().trim();
		
		// add tip
		if (this.addTips(joined)) {
		    // success
		    sender.sendMessage(this.prefix + ChatColor.GREEN + "Tip added");
		    this.getLogger().fine(sender.getName() + " added tip \"" + joined + "\"");
		    
		    return true;
		}
		else {
		    // failure
		    sender.sendMessage(this.prefix + ChatColor.RED + "Failed to add tip");
		    this.getLogger().warning(sender.getName() + " failed to add tip \"" + joined + "\"");
		    
		    return false;
		}
	    }
	    else if (sender.hasPermission("tipsplugin.get")) {
		// get tip
		sender.sendMessage(this.prefix + this.getTip());
		
		return true;
	    }
	    return false;
	}
	return false;
    }
    
    /**
     * Adds the specified tips
     * 
     * @param tips
     *            tips to add
     * @return true if the tip buffer changed as a result of the call
     */
    public boolean addTips(String... tips) {
	if (this.tips.addAll(Arrays.asList(tips))) {
	    this.saveTips();
	    return true;
	}
	
	return false;
    }
    
    /**
     * Gets a random tip
     * 
     * @return a random tip
     */
    public String getTip() {
	if (tips.isEmpty())
	    return "Use " + ChatColor.ITALIC + "/tip <tip>" + ChatColor.ITALIC + " to add tips.";
	
	return this.tips.get(rand.nextInt(tips.size()));
    }
    
    /**
     * Gets the task responsible for broadcasting tips
     * 
     * @return the task responsible for broadcasting tips
     */
    public BukkitTask getBroadcasterTask() {
	return this.task;
    }
    
    /**
     * Loads the tip buffer from file
     * @return true if the tip buffer was loaded from file
     */
    public synchronized boolean loadTips() {
	File file = new File(this.getDataFolder(), FNAME_TIPS);
	this.getLogger().info("Loading tips from " + file);
	
	// buffer entire file for efficiency - writing the tip buffer once for
	// each line is inefficient
	Collection<String> buffer = new ArrayList<String>();
	
	BufferedReader reader = null;
	try {
	    reader = new BufferedReader(new FileReader(file));
	    
	    // read line by line into the file buffer
	    String line;
	    while ((line = reader.readLine()) != null) {
		line = line.trim();
		
		if (line.equals(""))
		    continue; // ignore empty lines
		if (line.startsWith("#"))
		    continue; // ignore comments
		    
		buffer.add(line);
	    }
	}
	catch (FileNotFoundException e) {
	    this.getLogger().warning("Tip file '" + file + "' not found.");
	    return false;
	}
	catch (IOException e) {
	    e.printStackTrace();
	    this.getLogger().warning("Failed to load tips from " + file);
	    return false;
	}
	finally {
	    if (reader != null) {
		try {
		    reader.close();
		}
		catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	    
	    // copy the file buffer into the tip buffer
	    this.tips = new CopyOnWriteArrayList<String>(buffer);
	}
	
	return true;
    }
    
    /**
     * Saves the tip buffer to file
     * @return true if the tip buffer was saved to file
     */
    public synchronized boolean saveTips() {
	File file = new File(this.getDataFolder(), FNAME_TIPS);
	this.getLogger().info("Saving tips to " + file);
	
	// create all parent directories
	File dir = file.getParentFile();
	if (!dir.isDirectory() && !dir.mkdirs()) {
	    this.getLogger().warning("Failed to create directory " + dir);
	    return false;
	}
	
	BufferedWriter writer = null;
	try {
	    writer = new BufferedWriter(new FileWriter(file));
	    
	    // write line by line
	    for (String line : this.tips) {
		writer.write(line);
		writer.newLine();
	    }
	}
	catch (IOException e) {
	    e.printStackTrace();
	    this.getLogger().warning("Failed to save tips to " + file);
	    return false;
	}
	finally {
	    if (writer != null) {
		try {
		    writer.close();
		}
		catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	}
	
	return true;
    }
    
}
