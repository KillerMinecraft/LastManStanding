package com.ftwinston.KillerMinecraft.Modules.LastManStanding;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.ftwinston.KillerMinecraft.GameMode;
import com.ftwinston.KillerMinecraft.Helper;
import com.ftwinston.KillerMinecraft.Option;
import com.ftwinston.KillerMinecraft.PlayerFilter;
import com.ftwinston.KillerMinecraft.Configuration.NumericOption;
import com.ftwinston.KillerMinecraft.Configuration.TeamInfo;
import com.ftwinston.KillerMinecraft.Configuration.ToggleOption;

public class LastManStanding extends GameMode
{
	ToggleOption useTeams, centralizedSpawns;
	NumericOption numTeams, numLives;
	
	static final long centralSpawnImmobilizationDelay = 200;
	long centralSpawnImmobilizationEnd;
	
	abstract class LMSTeamInfo extends TeamInfo
	{
		public LMSTeamInfo(int num) { teamNum = num; }
		public Score lives;
		public int teamNum;
	}
	
	LMSTeamInfo[] teams = new LMSTeamInfo[0];
	
	@Override
	public int getMinPlayers() { return 2; }
	
	@Override
	public Option[] setupOptions()
	{
		numTeams = new NumericOption("Number of teams", 2, 4, Material.CHEST, 2) {
			@Override
			protected void changed() 
			{
				LMSTeamInfo[] newTeams = new LMSTeamInfo[numTeams == null ? 2 : numTeams.getValue()];
				int i;
				for ( i=0; i<teams.length && i<newTeams.length; i++ )
					newTeams[i] = teams[i];
				for ( ; i<newTeams.length; i++ )
					switch ( i )
					{
						case 0:
							newTeams[i] = new LMSTeamInfo(i) {
								@Override
								public String getName() { return "red team"; }
								@Override
								public ChatColor getChatColor() { return ChatColor.RED; }
								@Override
								public DyeColor getDyeColor() { return DyeColor.RED; }
							}; break;
						case 1:
							newTeams[i] = new LMSTeamInfo(i) {
								@Override
								public String getName() { return "blue team"; }
								@Override
								public ChatColor getChatColor() { return ChatColor.BLUE; }
								@Override
								public DyeColor getDyeColor() { return DyeColor.BLUE; }
							}; break;
						case 2:
							newTeams[i] = new LMSTeamInfo(i) {
								@Override
								public String getName() { return "yellow team"; }
								@Override
								public ChatColor getChatColor() { return ChatColor.YELLOW; }
								@Override
								public DyeColor getDyeColor() { return DyeColor.YELLOW; }
							}; break;
						case 3:
							newTeams[i] = new LMSTeamInfo(i) {
								@Override
								public String getName() { return "green team"; }
								@Override
								public ChatColor getChatColor() { return ChatColor.GREEN; }
								@Override
								public DyeColor getDyeColor() { return DyeColor.GREEN; }
							}; break;
					}
				teams = newTeams;
				setTeams(teams);
			}
		};
		numTeams.setHidden(true);
		
		numLives = new NumericOption("Number of lives", 1, 7, Material.APPLE, 1, "Players can die this many times", "before they're out of the game.", "In a team game, players share", "lives with their teammates.");
		
		centralizedSpawns = new ToggleOption("Centralized spawns", true, "When enabled, players spawn in", "a circle around a chest full of", "equipment. When disabled, players", "spawn spread out in the world");
		
		useTeams = new ToggleOption("Use Teams", false, "Allows players to be divided", "into separate teams.") {
			@Override
			public void changed()
			{
				numTeams.setHidden(!isEnabled()); // can only set team count when teams enabled
				
				if ( isEnabled() )
				{
					// can only use centralized spawns when teams disabled
					centralizedSpawns.setHidden(true);
					
					if ( centralizedSpawns.isEnabled() )
						centralizedSpawns.toggle();
					setTeams(teams);
				}
				else
				{
					centralizedSpawns.setHidden(false);
					setTeams(new TeamInfo[0]);
				}
			}
		};
		
		return new Option[] { useTeams, numTeams, centralizedSpawns, numLives };
	}
	
	@Override
	public List<String> getHelpMessages(TeamInfo team)
	{
		LinkedList<String> messages = new LinkedList<String>();
		
		if ( useTeams.isEnabled() )
		{
			messages.add("Players have been split into " + teams.length + " teams. Each time you die, your team's lives decreases by 1.");
			messages.add("When a team has no lives left, they cannot respawn. The last team standing wins the game!");
			messages.add("Your compass will point to the nearest player on another team.");
		}
		else
		{
			messages.add("Each time you die, your lives decreases by 1.");
			messages.add("When you have no lives left, you cannot respawn. The last player standing wins the game!");
			messages.add("Your compass will point to the nearest player apart from yourself.");
		}
		return messages;
	}
	
	Objective playerLives;
	
	@Override
	public Scoreboard createScoreboard()
	{
		Scoreboard scoreboard;
		
		if ( useTeams.isEnabled() )
		{
			scoreboard = super.createScoreboard();
			for (Team team : scoreboard.getTeams())
				team.setAllowFriendlyFire(false);
		}
		else
			scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		
		playerLives = scoreboard.registerNewObjective("lives", "dummy");
		playerLives.setDisplaySlot(DisplaySlot.SIDEBAR);
		playerLives.setDisplayName("Lives remaining");
		
		return scoreboard;
	}
	
	@Override
	public Environment[] getWorldsToGenerate() { return new Environment[] { Environment.NORMAL }; }
		
	@Override
	public boolean isLocationProtected(Location l, Player p) { return inWarmup && centralizedSpawns.isEnabled(); }
	
	@Override
	public Location getSpawnLocation(Player player)
	{
		if ( useTeams.isEnabled() )
		{
			LMSTeamInfo lmsTeam = (LMSTeamInfo)getTeam(player);
			
			Location spawn = getCircleSpawnLocation(lmsTeam.teamNum, teamSeparation);
			Location spawnPoint = Helper.randomizeLocation(spawn, 0, 0, 0, 8, 0, 8);
			return Helper.getSafeSpawnLocationNear(spawnPoint);
		}
		else
		{
			int playerNumber;
			
			if (inWarmup) // at the start, players spawn in sequence
				playerNumber = nextPlayerNumber ++;
			else // at the end, they spawn within the previously-allotted area
				playerNumber = new Random().nextInt(nextPlayerNumber);
		
			if ( centralizedSpawns.isEnabled() )	
				return getCircleSpawnLocation(playerNumber, playerSeparation);
			else
				return getSpreadOutSpawn(playerNumber);
		}
	}
	
	final double teamSeparation = 100, playerSeparation = 12;
	double angularSeparation, spawnCircleRadius;

	private Location getCircleSpawnLocation(int spawnNumber, double separation)
	{
		// spawns are spread out in a circle around the center.
		// the radius of this circle is dependent on the number of players/teams, such that they will always
		// be a fixed distance from their nearest neighbours, regardless of how many players/teams we have.
		
		Location spawn = getWorld(0).getSpawnLocation();
		double angle = angularSeparation * spawnNumber;
		
		while (angle >= Math.PI * 2)
			angle -= Math.PI * 2;
		
		double x, z;
		if ( angle < Math.PI / 2)
		{
			x = separation * Math.cos(angle);
			z = separation * Math.sin(angle);
		}
		else if ( angle < Math.PI )
		{
			x = -separation * Math.cos(Math.PI - angle);
			z = separation * Math.sin(Math.PI - angle);
		}
		else if ( angle < 3 * Math.PI / 2)
		{
			x = -separation * Math.cos(angle - Math.PI);
			z = -separation * Math.sin(angle - Math.PI);
		}
		else
		{
			x = separation * Math.cos(2 * Math.PI - angle);
			z = -separation * Math.sin(2 * Math.PI - angle);
		}
		
		spawn = spawn.add(x + 0.5, 0, z + 0.5);
		spawn.setY(Helper.getHighestBlockYAt(spawn.getChunk(), (int)x, (int)z)+1);
		
		spawn.setYaw((float)(180 / Math.PI * angle + 90));
		return spawn;
	}

	private Location getSpreadOutSpawn(int playerNumber)
	{
		Location worldSpawn = getWorld(0).getSpawnLocation();		
		// ok, we're going to spawn one player at each vertex of a "square spiral," moving outward from the center.
		// This shape is called an Ulam spiral, and it might be a bit OTT to use this, but there you go.
		
		int x = 0, z = 0;
		int side = 0, number = 0, sideLength = 0;
		int dir = 0; // 0 = +x, 1 = +z, 2 = -x, 3 = -z
		
		while ( true )
		{
			side++;
			for (int k = 0; k < sideLength; k++)
			{
				// move forward
				switch ( dir )
				{
					case 0:
						x++; break;
					case 1:
						z++; break;
					case 2:
						x--; break;
					case 3:
						z--; break;
				}
				number++;
				
				if ( number == playerNumber )
					return Helper.getSafeSpawnLocationNear(worldSpawn.add(x * 32, 0, z * 32));
			}
			
			// turn left
			if ( dir >= 3 )
				dir = 0;
			else
				dir++;
				
			if (side % 2 == 0)
				sideLength++;
		}
	}

	boolean inWarmup = true;
	int nextPlayerNumber = 1;
	
	@Override
	public void gameStarted()
	{
		List<Player> players = getOnlinePlayers();

		if ( !useTeams.isEnabled() )
		{
			angularSeparation = 2 * Math.PI / teams.length;
			spawnCircleRadius = 0.5 * teamSeparation / Math.sin(angularSeparation / 2);
			
			for ( LMSTeamInfo team : teams )			
			{
				int num = getOnlinePlayers(new PlayerFilter().team(team)).size();
				team.lives.setScore(num * numLives.getValue());
			}
		}
		else
		{
			for ( Player player : players )
			{
				Score score = playerLives.getScore(player.getName());
				score.setScore(numLives.getValue());
			}
			
			if ( centralizedSpawns.isEnabled() )
			{
				angularSeparation = 2 * Math.PI / players.size();
				inWarmup = true;
				
				getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
					@Override
					public void run() {
						inWarmup = false;
						broadcastMessage(ChatColor.RED + "Go!");
					}
				}, centralSpawnImmobilizationDelay);
				centralSpawnImmobilizationEnd = getWorld(0).getFullTime();
				
				for (Player player : players)
					immobilizePlayer(player, (int)centralSpawnImmobilizationDelay);

				createCentralSpawnItems();
			}

			nextPlayerNumber = 1; // ensure that the player placement logic starts over again
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onWorldInit(WorldInitEvent event)
	{
		if (!useTeams.isEnabled() && centralizedSpawns.isEnabled())
		{
			angularSeparation = 2 * Math.PI / getOnlinePlayers().size();
			spawnCircleRadius = 0.5 * playerSeparation / Math.sin(angularSeparation / 2);
			
			SpawnPopulator populator = new SpawnPopulator(event.getWorld().getSpawnLocation(), (int)spawnCircleRadius + 2, 14, 10);
			event.getWorld().getPopulators().add(populator);
		}
	}
		
	private void createCentralSpawnItems()
	{
		World world = getWorld(0);
		Location center = world.getSpawnLocation();
		
		// the area around the central spawn items should be clear of trees etc
		for (int x = center.getBlockX() - 2; x <= center.getBlockX() + 2; x++)
		{
			for (int z = center.getBlockZ() - 2; z <= center.getBlockZ() + 2; z++)
			{
				Block b = world.getHighestBlockAt(x, z);
				while (!isGround(b.getType()))
				{
					b.setType(Material.AIR);
					b = b.getRelative(BlockFace.DOWN);
				}
			}
		}
		
		// create some central items (couple of chests, enchanting table
		Block b = world.getHighestBlockAt(center.getBlockX(),  center.getBlockZ());
		b.setType(Material.ENCHANTMENT_TABLE);
		
		b = world.getHighestBlockAt(center.getBlockX() + 1,  center.getBlockZ() + 1);
		populateRandomChest(b);
		
		b = world.getHighestBlockAt(center.getBlockX() + 1,  center.getBlockZ() - 1);
		populateRandomChest(b);
		
		b = world.getHighestBlockAt(center.getBlockX() - 1,  center.getBlockZ() + 1);
		populateRandomChest(b);
		
		b = world.getHighestBlockAt(center.getBlockX() - 1,  center.getBlockZ() - 1);
		populateRandomChest(b);
	}

	private boolean isGround(Material type)
	{
		switch (type)
		{
		case GRASS:
		case DIRT:
		case STONE:
		case COBBLESTONE:
		case SAND:
		case GRAVEL:
		case SANDSTONE:
		case STATIONARY_WATER:
		case STATIONARY_LAVA:
			return true;
		default:
			return false;
		}
	}

	private void populateRandomChest(Block b)
	{
		// TODO Auto-generated method stub
		b.setType(Material.TRAPPED_CHEST);
		
		Inventory inv = ((Chest)b.getState()).getBlockInventory();
		Random r = new Random();
		
		int numItems = r.nextInt(8) + 4;
		for (int i=0; i<numItems; i++)
			inv.setItem(r.nextInt(36), selectRandomItem(r));
	}

	private ItemStack selectRandomItem(Random r)
	{
		switch (r.nextInt(32))
		{
		case 0:
		case 1:
			return new ItemStack(Material.STONE_SWORD);
		case 2:
			return new ItemStack(Material.IRON_SWORD);
		case 3:
		case 4:
			return new ItemStack(Material.BOW);
		case 5:
		case 6:
			return new ItemStack(Material.ARROW, r.nextInt(8) + 2);
		case 7:
		case 8:
		case 9:
			return new ItemStack(Material.STONE_PICKAXE);
		case 10:
			return new ItemStack(Material.IRON_PICKAXE);
		case 11:
			return new ItemStack(Material.IRON_HELMET);
		case 12:
			return new ItemStack(Material.IRON_CHESTPLATE);
		case 13:
			return new ItemStack(Material.IRON_LEGGINGS);
		case 14:
			return new ItemStack(Material.IRON_BOOTS);
		case 15:
			return new ItemStack(Material.ENDER_PEARL, r.nextInt(3) + 1);
		case 16:
			return new ItemStack(Material.TNT, r.nextInt(4) + 1);
		case 17:
			return new ItemStack(Material.DIAMOND, 2);
		case 18:
			return new ItemStack(Material.BUCKET);
		case 19:
			return new ItemStack(Material.REDSTONE, r.nextInt(5) + 4);
		case 20:
			return new ItemStack(Material.COAL, r.nextInt(8) + 8);
		case 21:
			return createPotion(PotionType.SPEED, 1);
		case 22:
			return createPotion(PotionType.STRENGTH, 1);
		case 23:
			return createPotion(PotionType.INSTANT_HEAL, 2);
		case 24:
			return createPotion(PotionType.JUMP, 2);
		case 25:
			return createPotion(PotionType.NIGHT_VISION, 1);
		case 26:
			return createPotion(PotionType.REGEN, 1);
		case 27:
			return createPotion(PotionType.WATER_BREATHING, 1);
		case 28:
			return createPotion(PotionType.FIRE_RESISTANCE, 1);
			
		default:
			return new ItemStack(Material.APPLE, r.nextInt(5) + 1);
		}
	}

	private ItemStack createPotion(PotionType type, int level)
	{
		ItemStack stack = new ItemStack(Material.POTION);
		Potion potion = new Potion(type);
		potion.setLevel(level);
		potion.apply(stack);
		return stack;
	}
	
	private void immobilizePlayer(Player player, int duration)
	{
		player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 50, false, false));
		player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 50, false, false));
		player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, duration, 50, false, false));
	}

	@Override
	public void playerJoinedLate(Player player)
	{
		if ( !useTeams.isEnabled() )
		{
			Score score = playerLives.getScore(player.getName());
			score.setScore(numLives.getValue());
		}
		
		if (inWarmup)
		{// prevent slightly-late joiners from having free reign, or being immoblized longer than everyone else
			long duration = centralSpawnImmobilizationEnd - getWorld(0).getFullTime();
			immobilizePlayer(player, (int)duration);			
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerKilled(PlayerDeathEvent event)
	{		
		if ( useTeams.isEnabled() )
		{
			LMSTeamInfo team = (LMSTeamInfo)getTeam(event.getEntity());
			if ( team.lives.getScore() > 0 )
				team.lives.setScore(team.lives.getScore() - 1);
			else
			{
				// make every player on this team a spectator
				for (Player player : getOnlinePlayers(new PlayerFilter().team(team)))
					Helper.makeSpectator(getGame(), player);
			}
		}
		else
		{
			Score score = playerLives.getScore(event.getEntity().getName());
			if ( score.getScore() > 0 )
				score.setScore(score.getScore()-1);
			else
				Helper.makeSpectator(getGame(), event.getEntity());
		}
	}
	
	@Override
	public void playerQuit(OfflinePlayer player)
	{
		if ( hasGameFinished() )
			return;
		
		if ( useTeams.isEnabled() )
		{
			// if only one team has players left, that team wins. Otherwise, continue.
			
			LMSTeamInfo lastTeam = null;
			
			for ( LMSTeamInfo team : teams )
				if ( getOnlinePlayers(new PlayerFilter().team(team)).size() > 0 )
				{
					if ( lastTeam != null )
						return; // multiple teams have players left
					else
						lastTeam = team;
				}
			
			if ( lastTeam == null )
				broadcastMessage("All players dead, game drawn");
			else
				broadcastMessage("The " + lastTeam.getChatColor() + lastTeam.getName() + ChatColor.RESET + " is the last team standing. The " + lastTeam.getName() + " wins!");
				
			finishGame();
		}
		else
		{
			List<Player> survivors = getOnlinePlayers(new PlayerFilter());
			
			if ( survivors.size() == 1 )
			{
				Player survivor = survivors.get(0);
				broadcastMessage(new PlayerFilter().exclude(survivor), survivor.getName() + " is the last man standing, and wins the game!");
				survivor.sendMessage("You are the last man standing: you win the game!");
			}
			else if ( survivors.size() == 0 )
				broadcastMessage("All players died, nobody wins!");
			else
				return; // multiple people left in the game
			
			finishGame();
		}
	}
	
	@Override
	public Location getCompassTarget(Player player)
	{
		if ( useTeams.isEnabled() )
		{
			TeamInfo team = getTeam(player);
			return Helper.getNearestPlayerTo(player, getOnlinePlayers(new PlayerFilter().notTeam(team))); // points in a random direction if no players are found
		}
		else
			return Helper.getNearestPlayerTo(player, getOnlinePlayers(new PlayerFilter())); // points in a random direction if no players are found
	}
}
