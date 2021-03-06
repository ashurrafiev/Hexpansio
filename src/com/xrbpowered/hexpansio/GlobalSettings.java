package com.xrbpowered.hexpansio;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;

import com.xrbpowered.zoomui.swing.SwingWindowFactory;

public class GlobalSettings {

	public static final int minWindowWidth = 1400;
	public static final int minWindowHeight = 800;
	
	public static final String path = "./hexpansio.cfg";
	
	public int uiScaling = 0;
	public boolean windowed = false;
	public int windowWidth = minWindowWidth;
	public int windowHeight = minWindowHeight;
	public boolean hotkeyTooltips = true;
	public boolean tutorial = true;
	
	public boolean autosave = false;
	public boolean saveOnExit = false;
	
	public boolean explainNoAction = true;
	public boolean confirmHurry = true;
	public boolean openMessageLog = true;
	public boolean warnNextTurn = true;
	public boolean warnUnhappy = true;
	public boolean warnNoBuilding = false;

	public GlobalSettings copy() {
		GlobalSettings s = new GlobalSettings();
		s.uiScaling = uiScaling;
		s.windowed = windowed;
		s.windowWidth = windowWidth;
		s.windowHeight = windowHeight;
		s.hotkeyTooltips = hotkeyTooltips;
		s.tutorial = tutorial;
		s.autosave = autosave;
		s.saveOnExit = saveOnExit;
		s.explainNoAction = explainNoAction;
		s.confirmHurry = confirmHurry;
		s.openMessageLog = openMessageLog;
		s.warnNextTurn = warnNextTurn;
		s.warnUnhappy = warnUnhappy;
		s.warnNoBuilding = warnNoBuilding;
		return s;
	}
	
	public boolean apply(GlobalSettings prev) {
		float scale = uiScaling==0 ? SwingWindowFactory.getSystemScale() : uiScaling/100f;
		windowWidth = Math.max(windowWidth, minWindowWidth);
		windowHeight = Math.max(windowHeight, minWindowHeight);
		if(prev==null || windowed!=prev.windowed) {
			SwingWindowFactory.use().setBaseScale(scale);
			Hexpansio.createWindow();
			return true;
		}
		else if(uiScaling!=prev.uiScaling) {
			Hexpansio.instance.getBase().setBaseScale(scale);
		}
		if(Hexpansio.getWorld()!=null)
			Hexpansio.getWorld().updateWorldTotals();
		return false;
	}
	
	public static void saveValues(HashMap<String, String> values) {
		try {
			PrintWriter out = new PrintWriter(new File(path));
			for(Entry<String, String> entry : values.entrySet()) {
				out.printf("%s : %s\n", entry.getKey(), entry.getValue());
			}
			out.close();
		}
		catch(Exception e) {
		}
	}
	
	public static HashMap<String, String> loadValues() {
		try {
			HashMap<String, String> values = new HashMap<>();
			Scanner in = new Scanner(new File(path));
			while(in.hasNextLine()) {
				String[] s = in.nextLine().trim().split("\\s+:\\s+", 2);
				if(s.length==2)
					values.put(s[0], s[1]);
			}
			in.close();
			return values;
		}
		catch(Exception e) {
			return null;
		}
	}
	
	public static void save(GlobalSettings s) {
		HashMap<String, String> values = new HashMap<>();
		values.put("uiScaling", Integer.toString(s.uiScaling));
		values.put("windowed", Boolean.toString(s.windowed));
		values.put("windowWidth", Integer.toString(s.windowWidth));
		values.put("windowHeight", Integer.toString(s.windowHeight));
		values.put("hotkeyTooltips", Boolean.toString(s.hotkeyTooltips));
		values.put("tutorial", Boolean.toString(s.tutorial));
		values.put("autosave", Boolean.toString(s.autosave));
		values.put("saveOnExit", Boolean.toString(s.saveOnExit));
		values.put("explainNoAction", Boolean.toString(s.explainNoAction));
		values.put("confirmHurry", Boolean.toString(s.confirmHurry));
		values.put("openMessageLog", Boolean.toString(s.openMessageLog));
		values.put("warnNextTurn", Boolean.toString(s.warnNextTurn));
		values.put("warnUnhappy", Boolean.toString(s.warnUnhappy));
		values.put("warnNoBuilding", Boolean.toString(s.warnNoBuilding));

		saveValues(values);
	}

	private static int getInt(String value, int min, int max, int fallback) {
		if(value==null)
			return fallback;
		try {
			int n = Integer.parseInt(value);
			if(n<min || n>max)
				return fallback;
			return n;
		}
		catch(NumberFormatException e) {
			return fallback;
		}
	}
	
	private static boolean getBoolean(String value, boolean fallback) {
		if(value==null)
			return fallback;
		if(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"))
			return true;
		else if(value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no"))
			return false;
		else
			return fallback;
	}
	
	public static GlobalSettings load() {
		HashMap<String, String> values = loadValues();
		GlobalSettings s = new GlobalSettings();
		if(values==null)
			return s;
		s.uiScaling = getInt(values.get("uiScaling"), 0, 200, s.uiScaling);
		s.windowed = getBoolean(values.get("windowed"), s.windowed);
		s.windowWidth = getInt(values.get("windowWidth"), minWindowWidth, Integer.MAX_VALUE, s.windowWidth);
		s.windowHeight = getInt(values.get("windowHeight"), minWindowHeight, Integer.MAX_VALUE, s.windowHeight);
		s.hotkeyTooltips = getBoolean(values.get("hotkeyTooltips"), s.hotkeyTooltips);
		s.tutorial = getBoolean(values.get("tutorial"), s.tutorial);
		s.autosave = getBoolean(values.get("autosave"), s.autosave);
		s.saveOnExit = getBoolean(values.get("saveOnExit"), s.saveOnExit);
		s.explainNoAction = getBoolean(values.get("explainNoAction"), s.explainNoAction);
		s.confirmHurry = getBoolean(values.get("confirmHurry"), s.confirmHurry);
		s.openMessageLog = getBoolean(values.get("openMessageLog"), s.openMessageLog);
		s.warnNextTurn = getBoolean(values.get("warnNextTurn"), s.warnNextTurn);
		s.warnUnhappy = getBoolean(values.get("warnUnhappy"), s.warnUnhappy);
		s.warnNoBuilding = getBoolean(values.get("warnNoBuilding"), s.warnNoBuilding);
		return s;
	}
	
}
