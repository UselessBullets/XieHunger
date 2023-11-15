package useless.xiehunger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.EntityPlayerSP;
import net.minecraft.core.entity.player.EntityPlayer;
import net.minecraft.core.item.ItemFood;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import turniplabs.halplibe.util.ConfigHandler;
import turniplabs.halplibe.util.TomlConfigHandler;
import turniplabs.halplibe.util.toml.Toml;

import java.io.*;
import java.util.Properties;


public class XieHunger implements ModInitializer {
    public static final String MOD_ID = "xiehunger";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
		FoodLists.init();
        LOGGER.info("XieHunger initialItemStacked.");
    }
	static final String modName = "Xie Hunger";
	static final String version = "1.7b";
	static final String settingsFile = "hunger.ini";
	static final String settingsPath = "/mods/Xie/";
	static final String hungerSaveFilePath = "/mods/Xie/Hunger";
	static final String iconFile = "/Xie/img/xiehunger.png";
	public static int hungerRate = 60;
	public static boolean passiveRegen = false;
	public static int hungerMax = 20;
	private static int hungerStateFactor = 5;
	public static int thirstRate = 20;
	public static boolean waterDrinkable = true;
	public static int thirstMax = 20;
	private static int thirstStateFactor = 5;
	public static int fatigue = 0;
	public static int fatigueMax = 1000;
	public static int fatigueScaledMax = 80;
	public static int fatigueScaled = 0;
	public static int fatigueStateFactor;
	public static int fatigueState;
	private static int fatigueHeartScaleFactor;
	public static int[] fatigueRate;
	public static int hungerClockTickRate;
	private static long timeOfLastTick;
	public static boolean hungerEnabled;
	public static boolean thirstEnabled;
	public static boolean fatigueEnabled;
	public static boolean useBars;
	public static int hunger;
	public static int thirst;
	public static int hungerState;
	public static int thirstState;
	public static boolean dying;
	private static boolean gryllsEnabled;
	private static int tickCounter;
	private String currentWorld = "";
	private int saveStateTick = 0;
	private final int ticksBetweenSaves = 40;
	private static int hungerTicks;
	private static int thirstTicks;
	private int fatigueOverflow = 0;
	private static int gryllsTick;
	public static final int CONSTANT = 0;
	public static final int WALKING = 1;
	public static final int JUMPING = 2;
	public static final int SWIMMING = 3;
	public static final int SWINGING = 4;
	public static final int SNEAKING = 5;
	public static final int RESTING = 6;
	public static final int SLEEPING = 7;

	public XieHunger() {
		FoodLists.init();
//		ModLoader.SetInGameHook(this, true, true); Commands sent to the risugamis modloader
//		ModLoader.SetInGUIHook(this, true, true);
	}

	public void ModsLoaded() { // For integrations with Xie's other mods
//		if (ModLoader.isModLoaded("mod_XieFarming")) {
//			FoodLists.juiceList.add(XieMod.lemonade);
//			FoodLists.juiceList.add(XieMod.orangeJuice);
//			FoodLists.addItemsToList(new gm[]{XieMod.orange, XieMod.lemon, XieMod.lettuce, XieMod.tomato, XieMod.watermelonPiece, XieMod.fruitSalad}, FoodLists.juicyList);
//			gryllsEnabled = true;
//		}
//
//		if (ModLoader.isModLoaded("mod_XieCooking")) {
//			FoodLists.juicyList.add(XieMod.soup);
//			FoodLists.juicyList.add(XieMod.stew);
//		}

	}

	public boolean OnTickInGame(Minecraft game) {
		this.gameTick(game);
		return true;
	}

	public void OnTickInGui(Minecraft game) {
		this.gameTick(game);
	}

	public void gameTick(Minecraft game) {
		if (!this.currentWorld.equals(game.theWorld.getLevelData().getWorldName())) {
//			loadStateFromFile(game);
			resetState(); // TODO replace with proper save/load system
			this.currentWorld = game.theWorld.getLevelData().getWorldName();
		}

		if (++tickCounter >= hungerClockTickRate) {
			tickCounter = 0;
			this.hungerClockTick(game);
		}

//		if (++this.saveStateTick >= 40) {
//			this.saveStateTick = 0;
//			saveStateToFile(game);
//		}

	}

	public void hungerClockTick(Minecraft game) {
		if (timeOfLastTick == 0L) {
			timeOfLastTick = game.theWorld.getWorldTime();
		}

		long scalingFactor = 1L;
		long dt = game.theWorld.getWorldTime() - timeOfLastTick;
		if (dt > (long)hungerClockTickRate) {
			scalingFactor = dt / (long)hungerClockTickRate;
		}

		timeOfLastTick = game.theWorld.getWorldTime();
		if (passiveRegen && this.inTheGreen()) {
			this.healPlayer(game, scalingFactor);
		}

		if (hungerEnabled) {
			this.doHunger(game, scalingFactor);
		}

		if (thirstEnabled) {
			this.doThirst(game, scalingFactor);
		}

		if (fatigueEnabled) {
			this.doFatigue(game, scalingFactor, dt);
		}

//		if (gryllsEnabled) {
//			doGrylls(game);
//		}

		stateUpdate();
	}

	private static void stateUpdate() {
		if (hunger < 0) {
			hunger = 0;
		}

		if (hunger > hungerMax) {
			hunger = hungerMax;
		}

		if (thirst < 0) {
			thirst = 0;
		}

		if (thirst > thirstMax) {
			thirst = thirstMax;
		}

		if (fatigue < 0) {
			fatigue = 0;
		}

		if (fatigue > fatigueMax) {
			fatigue = fatigueMax;
		}

		dying = hunger >= hungerMax || thirst >= thirstMax || fatigue >= fatigueMax;

		hungerState = (int)Math.floor((double) hunger / hungerStateFactor);
		thirstState = (int)Math.floor((double) thirst / thirstStateFactor);
		fatigueScaled = fatigue * fatigueScaledMax / fatigueMax;
		fatigueState = (int)Math.floor((double)fatigue / fatigueStateFactor);

	}

	private void doHunger(Minecraft game, long scale) {
		hungerTicks = (int)((long)hungerTicks + scale);
		if (hungerTicks >= hungerRate) {
			int amt = hungerTicks / hungerRate;
			hungerTicks = 0;
			hunger += amt;
			if (hunger >= hungerMax) {
				dying = true;
				this.hurtPlayer(game, hunger - hungerMax);
				hunger = hungerMax;
			}
		}

	}

	private void doThirst(Minecraft game, long scale) {
		thirstTicks = (int)((long)thirstTicks + scale);
		if (thirstTicks >= thirstRate) {
			int amt = thirstTicks / thirstRate;
			thirstTicks = 0;
			thirst += amt;
			if (thirst >= thirstMax) {
				dying = true;
				this.hurtPlayer(game, thirst - thirstMax);
				thirst = thirstMax;
			}
		}

		if (waterDrinkable && game.thePlayer.isInWater() && game.thePlayer.isSneaking() && game.thePlayer.input.moveStrafe  == 0.0F && game.thePlayer.input.moveForward == 0.0F) {
			thirst -= hungerClockTickRate * thirstMax / 100;
		}

	}

	private void doFatigue(Minecraft game, long scale, long dt) {
		fatigue = (int)((long)fatigue + (long)fatigueRate[0] * scale);
		boolean resting = true;
		if (dt >= 1200L) {
			fatigue = (int)((long)fatigue + (long)fatigueRate[7] * scale);
			if (fatigue < 0) {
				fatigue = 0;
			}

			resting = false;
		} else {
			if (game.thePlayer.input.moveStrafe  != 0.0F || game.thePlayer.input.moveForward != 0.0F) {
				if (game.thePlayer.isInWater()) {
					fatigue = (int)((long)fatigue + (long)fatigueRate[3] * scale);
				} else {
					fatigue = (int)((long)fatigue + (long)fatigueRate[1] * scale);
				}

				resting = false;
			}

			if (game.thePlayer.input.jump) {
				resting = false;
				fatigue = (int)((long)fatigue + (long)fatigueRate[2] * scale);
			}

			if (game.thePlayer.isSwinging) {
				resting = false;
				fatigue = (int)((long)fatigue + (long)fatigueRate[4] * scale);
			}

			if (game.thePlayer.isSneaking()) {
				resting = false;
				fatigue = (int)((long)fatigue + (long)fatigueRate[5] * scale);
			}

			if (resting) {
				fatigue = (int)((long)fatigue + (long)fatigueRate[6] * scale);
			}
		}

		if (fatigue >= fatigueMax) {
			dying = true;
			this.fatigueOverflow += fatigue - fatigueMax;
			int amt = this.fatigueOverflow / fatigueHeartScaleFactor;
			if (amt >= 1) {
				if (this.hurtPlayer(game, (long)amt)) {
					fatigue = fatigueMax;
				}

				this.fatigueOverflow = 0;
			}
		}

	}

//	private static void doGrylls(Minecraft game) {
//		if (thirst >= thirstMax && game.thePlayer.t() && !game.thePlayer.isInWater() && game.thePlayer.input.moveStrafe  == 0.0F && game.thePlayer.input.moveForward == 0.0F) {
//			if (game.thePlayer.c.b() != null) {
//				if (game.thePlayer.c.b().c == uu.N.bn && game.thePlayer.c.b().a == 1 && thirst >= thirstMax) {
//					if (gryllsTick++ >= 3) {
//						game.thePlayer.c.a[game.thePlayer.c.c] = new iz(XieMod.lemonade);
//					}
//				} else {
//					gryllsTick = 0;
//				}
//			} else {
//				gryllsTick = 0;
//			}
//		} else {
//			gryllsTick = 0;
//		}
//	}

	public static void feed(int amount) {
		hunger -= amount;
		stateUpdate();
	}

	public static void feed(int amount, ItemFood food) {
		int hungerRelief = amount;
		int thirstRelief = 0;
		if (FoodLists.juicyList.contains(food)) {
			thirstRelief = amount / 2;
		} else if (FoodLists.juiceList.contains(food)) {
			hungerRelief = amount / 2;
			thirstRelief = amount;
		} else if (FoodLists.waterList.contains(food)) {
			hungerRelief = 0;
			thirstRelief = amount;
		}

		hunger -= hungerRelief;
		thirst -= thirstRelief;
		stateUpdate();
	}

	private boolean hurtPlayer(Minecraft game, long scale) {
		if ((long)game.thePlayer.health <= scale) {
			resetState();
			game.thePlayer.outOfWorld();
			return false;
		} else {
			EntityPlayerSP player = game.thePlayer;
			player.health = (int)((long)player.health - scale);
			return true;
		}
	}

	private static void resetState() {
		hunger = 0;
		hungerState = 0;
		thirst = 0;
		thirstState = 0;
		fatigue = 0;
		fatigueState = 0;
		hungerTicks = 0;
		thirstTicks = 0;
		tickCounter = 0;
		timeOfLastTick = 0L;
	}

	private void healPlayer(Minecraft game, long scale) {
		if (game.thePlayer.health < 20) {
			game.thePlayer.heal((int) scale);
		}

	}

	public boolean inTheGreen() {
		return hungerState <= 1 && thirstState <= 1 && fatigueState <= 1;
	}
	static {
		fatigueStateFactor = fatigueMax / 4;
		fatigueState = 0;
		fatigueHeartScaleFactor = fatigueMax / 20;
		fatigueRate = new int[]{0, 2, 5, 5, 5, 5, -1, -3};
		hungerClockTickRate = 20;
		timeOfLastTick = 0L;
		hungerEnabled = true;
		thirstEnabled = true;
		fatigueEnabled = true;
		useBars = true;
		hunger = 0;
		thirst = 0;
		hungerState = 0;
		thirstState = 0;
		dying = false;
		gryllsEnabled = false;
		tickCounter = 0;
		hungerTicks = 0;
		thirstTicks = 0;
		gryllsTick = 0;
	}

	public static TomlConfigHandler cfg;
	static {
		ModContainer thisMod = FabricLoader.getInstance().getModContainer("xiehunger").get();
		Toml defaults = new Toml(thisMod.getMetadata().getName() + " " + thisMod.getMetadata().getVersion() + " settings");
		defaults.addCategory("Toggles")
			.addEntry("enableHunger", hungerEnabled)
			.addEntry("enableThirst", thirstEnabled)
			.addEntry("enableFatigue", fatigueEnabled)
			.addEntry("Water drinkable by crounching", "waterDrinkable", waterDrinkable);
		defaults.addCategory("Timings")
			.addEntry("Game ticks per mod tick", "clockRate", hungerClockTickRate)
			.addEntry("Ticks between increasing hunger or thirst", "hungerRate", hungerRate)
			.addEntry("thirstRate", thirstRate);
		defaults.addCategory("Saturated Benefits")
			.addEntry("passiveRegen", passiveRegen);
		defaults.addCategory("GUI")
			.addEntry("useBars", useBars);
		defaults.addCategory("Fatigue Rates")
			.addEntry("constantRate", fatigueRate[0])
			.addEntry("walkingRate", fatigueRate[1])
			.addEntry("jumpingRate", fatigueRate[2])
			.addEntry("swimmingRate", fatigueRate[3])
			.addEntry("swingingRate", fatigueRate[4])
			.addEntry("sneakingRate", fatigueRate[5])
			.addEntry("restingRate", fatigueRate[6])
			.addEntry("sleepingRate", fatigueRate[7]);

		cfg = new TomlConfigHandler(MOD_ID, defaults);
		hungerEnabled = cfg.getBoolean("Toggles.enableHunger");
		thirstEnabled = cfg.getBoolean("Toggles.enableThirst");
		fatigueEnabled = cfg.getBoolean("Toggles.enableFatigue");
		waterDrinkable = cfg.getBoolean("Toggles.waterDrinkable");
		hungerClockTickRate = cfg.getInt("Timings.clockRate");
		hungerRate = cfg.getInt("Timings.hungerRate");
		thirstRate = cfg.getInt("Timings.thirstRate");
		passiveRegen = cfg.getBoolean("Saturated Benefits.passiveRegen");
		useBars = cfg.getBoolean("GUI.useBars");

		fatigueRate[0] = cfg.getInt("Fatigue Rates.constantRate");
		fatigueRate[1] = cfg.getInt("Fatigue Rates.constantRate");
		fatigueRate[2] = cfg.getInt("Fatigue Rates.walkingRate");
		fatigueRate[3] = cfg.getInt("Fatigue Rates.jumpingRate");
		fatigueRate[4] = cfg.getInt("Fatigue Rates.swimmingRate");
		fatigueRate[5] = cfg.getInt("Fatigue Rates.swingingRate");
		fatigueRate[6] = cfg.getInt("Fatigue Rates.sneakingRate");
		fatigueRate[7] = cfg.getInt("Fatigue Rates.restingRate");
	}

// TODO Properly save data to level instead of seperate file
//	public static void saveStateToFile(Minecraft game) {
//		File dir = new File(Minecraft.b() + "/mods/Xie/Hunger");
//		if (!dir.exists()) {
//			dir.mkdirs();
//		}
//
//		String filename = game.k.b + ".hunger";
//		File f = new File(dir + "/" + filename);
//		Properties props = new Properties();
//		String world;
//		if (game.l()) {
//			world = game.z.C;
//		} else {
//			world = game.theWorld.getLevelData().j();
//		}
//
//		String keyHunger = world + "@hunger";
//		String keyThirst = world + "@thirst";
//		String keyFatigue = world + "@fatigue";
//		String keyClockTick = world + "@clockTick";
//		String lastTickTime = world + "@lastTickTime";
//
//		try {
//			if (!f.exists()) {
//				f.createNewFile();
//			} else {
//				FileReader fr = new FileReader(f);
//				props.load(fr);
//			}
//
//			props.put(keyHunger, "" + hunger);
//			props.put(keyThirst, "" + thirst);
//			props.put(keyFatigue, "" + fatigue);
//			props.put(keyClockTick, "" + tickCounter);
//			props.put(lastTickTime, "" + timeOfLastTick);
//			FileWriter fw = new FileWriter(f);
//			props.store(fw, (String)null);
//			fw.close();
//		} catch (IOException var12) {
//			var12.printStackTrace();
//		}
//
//	}
//
//	public static void loadStateFromFile(Minecraft mc) {
//		resetState();
//		File dir = new File(Minecraft.b() + "/mods/Xie/Hunger");
//		String filename = mc.k.b + ".hunger";
//		File f = new File(dir + "/" + filename);
//		Properties props = new Properties();
//		String world;
//		if (mc.l()) {
//			world = mc.z.C;
//		} else {
//			world = mc.f.x.j();
//		}
//
//		String keyHunger = world + "@hunger";
//		String keyThirst = world + "@thirst";
//		String keyFatigue = world + "@fatigue";
//		String keyClockTick = world + "@clockTick";
//		String lastTickTime = world + "@lastTickTime";
//
//		try {
//			if (!f.exists()) {
//				System.out.println("Couldn't find hunger file " + f.getCanonicalPath());
//			} else {
//				FileReader fr = new FileReader(f);
//				props.load(fr);
//				if (props.containsKey(keyHunger)) {
//					hunger = Integer.parseInt(props.getProperty(keyHunger));
//				} else {
//					System.out.println("Hunger information for world/server " + world + " not found.");
//				}
//
//				if (props.containsKey(keyThirst)) {
//					thirst = Integer.parseInt(props.getProperty(keyThirst));
//				} else {
//					System.out.println("Thirst information for world/server " + world + " not found.");
//				}
//
//				if (props.containsKey(keyFatigue)) {
//					fatigue = Integer.parseInt(props.getProperty(keyFatigue));
//				} else {
//					System.out.println("Fatigue information for world/server " + world + " not found.");
//				}
//
//				if (props.containsKey(keyClockTick)) {
//					tickCounter = Integer.parseInt(props.getProperty(keyClockTick));
//				}
//
//				if (props.containsKey(lastTickTime)) {
//					timeOfLastTick = Long.parseLong(props.getProperty(lastTickTime));
//				}
//			}
//		} catch (IOException var12) {
//			var12.printStackTrace();
//		}
//
//	}
}