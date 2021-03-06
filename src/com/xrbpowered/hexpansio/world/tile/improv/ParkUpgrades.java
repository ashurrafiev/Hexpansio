package com.xrbpowered.hexpansio.world.tile.improv;

import static com.xrbpowered.hexpansio.world.tile.improv.Improvement.*;

import com.xrbpowered.hexpansio.world.city.effect.CityEffect;
import com.xrbpowered.hexpansio.world.city.effect.EffectTarget;
import com.xrbpowered.hexpansio.world.tile.TerrainType.Feature;
import com.xrbpowered.hexpansio.world.tile.Tile;

public abstract class ParkUpgrades {

	public static void init() {
		new Improvement("park.garden", park, "Community Garden", 30, 1).yield(1, 0, 0, 1)
			.reject(Feature.values());
		new Improvement("park.monument", park, "Monument", 10, 0).maintenance(2).yield(0, 0, 0, 1);
		new Improvement("park.observatory", park, "Observatory", 80, 2).maintenance(3).yield(0, 0, 0, 3).cityUnique()
			.reject((Feature[])null).effects(
				CityEffect.add(EffectTarget.scouts, 1),
				new CityEffect() {
					@Override
					public float multiplyCityValue(EffectTarget key) {
						return key==EffectTarget.scoutCost ? 0.9f : 1f;
					}
					@Override
					public String getDescription() {
						return "-10% to Scout cost (diminishing)";
					}
				}
			);

		new Improvement("park.ski", park, "Ski Resort", 40, 1).yield(0, 0, 0, 2)
				.reject((Feature[])null).require(Feature.peak);

		final Improvement shrine = new Improvement("park.shrine", park, "Shrine", 20, 1).maintenance(1).yieldPerWorker(0, 0, 0, 1)
			.reject((Feature[])null);
		new Improvement("park.cathederal", shrine, "Cathedral", 100, 2).workplaces(1).maintenance(3).yield(0, 0, 0, 1).yieldPerWorker(0, 0, 1, 1)
			.reject(Feature.swamp, Feature.mountain, Feature.peak);
		
		final Improvement school = new Improvement("park.school", park, "School", 60, 1).workplaces(1).maintenance(2).yieldPerWorker(0, 1, 0, 1)
			.reject(Feature.forest, Feature.swamp, Feature.mountain, Feature.peak);
		new Improvement("park.library", school, "Library", 60, 1).maintenance(1).yieldPerWorker(0, 0, 0, 1);
		
		new Improvement("park.university", school, "University", 150, 2).cityUnique().maintenance(5).workplaces(1).yield(0, 0, 0, 2).cityPrerequisite(CityUpgrades.townHall)
			.effects(new CityEffect() {
				@Override
				public int tileEffect(EffectTarget key, Tile tile) {
					return (key==EffectTarget.workplaces &&
							(ImprovementStack.tileContains(tile, school) || ImprovementStack.tileContains(tile, MarketUpgrades.office))) ? 1 : 0;
				}
				@Override
				public String getDescription() {
					return "+1 Workplace to every School or Office";
				}
			});
	}
	
}
