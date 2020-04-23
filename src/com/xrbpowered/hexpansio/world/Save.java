package com.xrbpowered.hexpansio.world;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.xrbpowered.hexpansio.world.city.City;
import com.xrbpowered.hexpansio.world.city.build.BuildImprovement;
import com.xrbpowered.hexpansio.world.city.build.BuildMigration;
import com.xrbpowered.hexpansio.world.city.build.BuildSettlement;
import com.xrbpowered.hexpansio.world.city.build.BuildingProgress;
import com.xrbpowered.hexpansio.world.city.build.FinishedBuilding;
import com.xrbpowered.hexpansio.world.city.build.RemoveImprovement;
import com.xrbpowered.hexpansio.world.resources.ResourcePile;
import com.xrbpowered.hexpansio.world.resources.TokenResource;
import com.xrbpowered.hexpansio.world.resources.Trade;
import com.xrbpowered.hexpansio.world.tile.TerrainGenerator;
import com.xrbpowered.hexpansio.world.tile.TerrainType;
import com.xrbpowered.hexpansio.world.tile.Tile;
import com.xrbpowered.hexpansio.world.tile.Tile.DummyTile;
import com.xrbpowered.hexpansio.world.tile.improv.Improvement;
import com.xrbpowered.hexpansio.world.tile.improv.ImprovementStack;

public class Save {

	public static final String savePath = "./save";
	public static final File saveDirectory = new File(savePath);
	public static final Save autosave = new Save(new File(saveDirectory, "autosave.save"));
	
	private static final int formatCode = 632016289;
	private static final int saveVersion = 10;
	private static final int minAcceptedVersion = saveVersion;

	public static class SaveInfo {
		public boolean validCode = false;
		public int version = 0;
		
		public long gameStarted;
		public long gameSaved;
		public int turn;
		public int numCities;
		public int population;
		
		public boolean isValid() {
			return validCode && version>=minAcceptedVersion;
		}
	}
	
	public final String name;
	public final File file;

	private SaveInfo info = null;
	public World world = null;
	
	protected ObjectIndex<TerrainType> convTerrainType = null;
	protected ObjectIndex<TokenResource> convResources = null;
	protected ObjectIndex<Improvement> convImprovement = null;
	
	public Save(File file) {
		this.file = file;
		String s = file.getName();
		this.name = s.substring(0, s.length()-".save".length());
	}
	
	public boolean exists() {
		return file.exists();
	}
	
	public boolean delete() {
		if(exists())
			return file.delete();
		else
			return true;
	}
	
	protected TerrainGenerator terrainGenerator() {
		return new TerrainGenerator();
	}

	protected void writeTile(DataOutputStream out, Tile tile) throws IOException {
		out.writeByte(convTerrainType.getIndex(tile.terrain.id));
		out.writeByte(tile.resource==null ? -1 : convResources.getIndex(tile.resource.id));
		out.writeByte(tile.workers);
		out.writeInt(tile.city==null ? -1 : tile.city.id);
		out.writeInt(tile.settlement==null ? -1 : tile.settlement.city.id);

		ImprovementStack.write(tile, convImprovement, out);
	}

	protected void readTile(DataInputStream in, Tile tile) throws IOException {
		tile.terrain = convTerrainType.get(in.readByte());
		if(tile.terrain==null)
			tile.terrain = TerrainType.defaultFallback;
		if(tile.terrain==TerrainType.voidStorm)
			tile.region.voidStorms++;
		tile.resource = convResources.get(in.readByte());
		tile.workers = in.readByte();
		int cityId = in.readInt();
		tile.city = tile.region.world.cityById(cityId);
		cityId = in.readInt();
		tile.settlement = cityId<0 ? null : (BuildSettlement)tile.region.world.cityById(cityId).buildingProgress;
		
		ImprovementStack.read(tile, convImprovement, in);
		if(tile.isCityCenter())
			tile.city.setTile(tile);
	}

	protected void writeRegion(DataOutputStream out, Region region) throws IOException {
		for(int x=0; x<=Region.size; x++)
			for(int y=0; y<=Region.size; y++) {
				Tile t = region.tiles[x][y]; 
				if(t.discovered) {
					out.writeByte(x);
					out.writeByte(y);
					writeTile(out, t);
				}
			}
		out.writeByte(-1);
	}
	
	protected void readRegion(DataInputStream in, Region region) throws IOException {
		for(;;) {
			int x = in.readByte();
			if(x<0)
				return;
			int y = in.readByte();
			Tile t = region.tiles[x][y];
			t.discovered = true;
			readTile(in, t);
		}
	}

	private static final int noBuildingProgress = -1;
	private static final int buildSettlement = -2;
	private static final int removeImprovement = -3;
	private static final int buildMigration = -4;
	
	protected void writeBuildingProgress(DataOutputStream out, BuildingProgress bp) throws IOException {
		if(bp==null)
			out.writeShort(noBuildingProgress);
		else {
			int x;
			if(bp instanceof BuildSettlement)
				x = buildSettlement;
			else if(bp instanceof BuildMigration)
				x = buildMigration;
			else if(bp instanceof RemoveImprovement)
				x = removeImprovement;
			else {
				Improvement imp = ((BuildImprovement) bp).improvement;
				x = convImprovement.getIndex(imp.id);
			}
			out.writeShort(x);
			
			out.writeInt(bp.progress);
			out.writeInt(bp.tile.wx);
			out.writeInt(bp.tile.wy);
		}
	}
	
	protected BuildingProgress readBuildingProgress(DataInputStream in, City city) throws IOException {
		int x = in.readShort();
		BuildingProgress bp;
		boolean skip = false;
		if(x==noBuildingProgress)
			return null;
		else if(x==buildSettlement)
			bp = new BuildSettlement(city, null);
		else if(x==buildMigration)
			bp = new BuildMigration(city, null);
		else if(x==removeImprovement)
			bp = new RemoveImprovement(null);
		else {
			Improvement imp = convImprovement.get(x);
			skip = imp==null;
			bp = new BuildImprovement(city, imp);
		}
		
		bp.progress = in.readInt();
		int wx = in.readInt();
		int wy = in.readInt();
		bp.tile = new Tile.DummyTile(wx, wy);
		return skip ? null : bp;
	}
	
	protected void writeTrade(DataOutputStream out, Trade trade) throws IOException {
		if(trade.city.id<trade.otherCity.id) {
			out.writeInt(trade.city.id);
			out.writeInt(trade.otherCity.id);
			ResourcePile.write(trade.out, convResources, out);
			ResourcePile.write(trade.in, convResources, out);
		}
	}
	
	protected boolean readTrade(DataInputStream in, World world) throws IOException {
		int cityId = in.readInt();
		if(cityId<0)
			return false;
		else {
			City city = world.cityById(cityId);
			City otherCity = world.cityById(in.readInt());
			ResourcePile resOut = new ResourcePile();
			ResourcePile.read(resOut, convResources, in);
			ResourcePile resIn = new ResourcePile();
			ResourcePile.read(resIn, convResources, in);
			city.trades.accept(new Trade(city, otherCity, resIn, resOut), false);
			return true;
		}
	}
	
	protected void writeCity(DataOutputStream out, City city) throws IOException {
		out.writeInt(city.population);
		out.writeInt(city.growth);
		writeBuildingProgress(out, city.buildingProgress);
		out.writeByte(city.availExpand ? 1 : 0);
		FinishedBuilding.write(out, city.finishedBuilding);
	}
	
	protected void readCity(DataInputStream in, City city) throws IOException {
		city.population = in.readInt();
		city.growth = in.readInt();
		city.buildingProgress = readBuildingProgress(in, city);
		city.availExpand = in.readByte()>0;
		city.finishedBuilding = FinishedBuilding.read(in);
	}
	
	protected void realiseCity(City city) {
		if(city.buildingProgress!=null)
			city.buildingProgress.setTile(((DummyTile) city.buildingProgress.tile).realise(city.world));
	}
	
	protected void writeMessages(DataOutputStream out, World world) throws IOException {
		out.writeByte(world.events.size());
		for(TurnEventMessage msg : world.events) {
			out.writeUTF(msg.city==null ? "" : msg.city);
			out.writeUTF(msg.message);
			out.writeByte(msg.focusTile==null ? 0 : 1);
			if(msg.focusTile!=null) {
				out.writeInt(msg.focusTile.wx);
				out.writeInt(msg.focusTile.wy);
			}
			out.writeInt(msg.color==null ? 0 : msg.color.getRGB());
		}
	}

	protected void readMessages(DataInputStream in, World world) throws IOException {
		int n = in.readByte();
		world.events.clear();
		for(int i=0; i<n; i++) {
			String city = in.readUTF();
			if(city.isEmpty())
				city = null;
			String message = in.readUTF();
			Tile tile;
			if(in.readByte()!=0) {
				int wx = in.readInt();
				int wy = in.readInt();
				tile = world.getTile(wx, wy);
			}
			else {
				tile = null;
			}
			int rgb = in.readInt();
			Color color = rgb==0 ? null : new Color(rgb);
			world.events.add(new TurnEventMessage(city, message, tile).setColor(color));
		}
	}
	
	protected void writeWorld(DataOutputStream out, World world) throws IOException {
		out.writeByte(world.cityNameBaseLength);
		out.writeByte(world.cheater ? 1 : 0);
		
		out.writeInt(world.turn);
		out.writeInt(world.poverty);
		out.writeInt(world.gold);
		out.writeInt(world.goods);
		out.writeInt(world.discoverThisTurn);

		out.writeInt(world.cities.size());
		for(City city : world.cities) {
			out.writeInt(city.id);
			out.writeUTF(city.name);
			writeCity(out, city);
		}

		for(City city : world.cities) {
			for(Trade trade : city.trades.getAll())
				writeTrade(out, trade);
		}
		out.writeInt(-1);
		
		ArrayList<Integer> regions = world.regionIds();
		out.writeInt(regions.size());
		for(Integer id : regions) {
			Region r = world.getRegionById(id);
			out.writeShort(r.rx);
			out.writeShort(r.ry);
			writeRegion(out, r);
		}
		
		writeMessages(out, world);
		world.history.write(out);
	}
	
	protected void readWorld(DataInputStream in, World world) throws IOException {
		world.cityNameBaseLength = in.readByte();
		world.cheater = in.readByte()!=0;
		
		world.turn = in.readInt();
		world.poverty = in.readInt();
		world.gold = in.readInt();
		world.goods = in.readInt();
		world.discoverThisTurn = in.readInt();

		int numCities = in.readInt();
		for(int i=0; i<numCities; i++) {
			int cityId = in.readInt();
			String name = in.readUTF();
			City city = new City(cityId, world, null, name);
			readCity(in, city);
		}
		
		while(readTrade(in, world)) {
		}
		
		int numRegions = in.readInt();
		for(int i=0; i<numRegions; i++) {
			int rx = in.readShort();
			int ry = in.readShort();
			Region r = world.discoverRegion(rx, ry);
			readRegion(in, r);
		}
		
		world.origin = world.getTile(World.originwx, World.originwy);
		for(City city : world.cities)
			realiseCity(city);
		
		readMessages(in, world);
		world.history.read(in);
	}

	protected void writeInfo(DataOutputStream out, World world) throws IOException {
		out.writeLong(world.gameStarted);
		out.writeInt(world.turn);
		out.writeInt(world.history.stats().numCities);
		out.writeInt(world.history.stats().population);
	}

	protected void readInfo(DataInputStream in, SaveInfo info) throws IOException {
		info.gameSaved = file.lastModified();
		info.gameStarted = in.readLong();
		info.turn = in.readInt();
		info.numCities = in.readInt();
		info.population = in.readInt();
	}

	public boolean write() {
		if(world==null)
			return false;
		
		try {
			ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file));
			zip.putNextEntry(new ZipEntry("savedata"));
			DataOutputStream out = new DataOutputStream(zip);
			
			out.writeInt(formatCode);
			out.writeInt(saveVersion);
			writeInfo(out, world);
			
			convTerrainType = TerrainType.objectIndex.write(out);
			convResources = TokenResource.objectIndex.write(out);
			convImprovement = Improvement.objectIndex.write(out);
			
			WorldSettings.write(out, world.settings);
			writeWorld(out, world);
			
			zip.closeEntry();
			zip.close();
			
			System.out.println("World saved");
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public World startNew(WorldSettings settings) {
		System.out.println("New world");
		this.world = new World(System.currentTimeMillis(), settings, terrainGenerator()).create();
		return this.world;
	}

	public World read() {
		return read(false);
	}

	public World read(boolean infoOnly) {
		World world;
		resetInfo();
		try {
			ZipInputStream zip = new ZipInputStream(new FileInputStream(file));
			zip.getNextEntry();
			DataInputStream in = new DataInputStream(zip);
			
			info = new SaveInfo();
			if(in.readInt()!=formatCode) {
				in.close();
				error(infoOnly, "Not a save file");
				return null;
			}
			info.validCode = true;
			
			info.version = in.readInt();
			if(info.version<minAcceptedVersion) {
				error(infoOnly, "Save version is not supported");
				in.close();
				return null;
			}
			else if(info.version!=saveVersion)
				error(infoOnly, "Save version is different");
			
			readInfo(in, info);
			if(infoOnly) {
				in.close();
				return null;
			}
			
			convTerrainType = TerrainType.objectIndex.read(in);
			convResources = TokenResource.objectIndex.read(in);
			convImprovement = Improvement.objectIndex.read(in);
			
			WorldSettings settings = WorldSettings.read(in);
			world = new World(info.gameStarted, settings, terrainGenerator());
			readWorld(in, world);

			zip.close();
		}
		catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		world.updateCities();
		world.updateWorldTotals();
		System.out.println("World loaded");
		
		this.world = world;
		return world;
	}

	private void error(boolean infoOnly, String msg) {
		if(!infoOnly)
			System.err.printf("%s: %s\n", name, msg);
	}
	
	public void resetInfo() {
		info = null;
	}
	
	public SaveInfo getInfo() {
		if(info==null)
			read(true);
		return info;
	}

	
	public static ArrayList<Save> allSaves(boolean showAutosave) {
		autosave.resetInfo();
		saveDirectory.mkdirs();
		File[] files = saveDirectory.listFiles();
		ArrayList<Save> saves = new ArrayList<>();
		for(int i=0; i<files.length; i++) {
			File f = files[i];
			if(f.getName().toLowerCase().endsWith(".save")) {
				Save s = new Save(f);
				if(showAutosave || !s.name.equalsIgnoreCase("autosave"))
					saves.add(s);
			}
		}
		saves.sort(new Comparator<Save>() {
			@Override
			public int compare(Save s1, Save s2) {
				return -Long.compare(s1.file.lastModified(), s2.file.lastModified());
			}
		});
		return saves;
	}
	
	public static Save latest() {
		ArrayList<Save> saves = allSaves(true);
		for(int i=0; i<saves.size(); i++) {
			Save s = saves.get(i);
			if(s.getInfo().isValid())
				return s;
		}
		return null;
	}

}
