package com.ftwinston.KillerMinecraft.Modules.LastManStanding;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ftwinston.KillerMinecraft.GameMode;
import com.ftwinston.KillerMinecraft.Helper;
import com.ftwinston.KillerMinecraft.Option;
import com.ftwinston.KillerMinecraft.PlayerFilter;
import com.ftwinston.KillerMinecraft.Configuration.NumericOption;
import com.ftwinston.KillerMinecraft.Configuration.TeamInfo;
import com.ftwinston.KillerMinecraft.Configuration.ToggleOption;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Score;
import org.bukkit.Material;

public class LastManStanding extends GameMode
{
	static final long warmupDelayTicks = 200L; // 10 seconds
	
	ToggleOption useTeams, friendlyFire, centralizedSpawns, contractKills;
	NumericOption numTeams, numLives;
	
	abstract class LMSTeamInfo extends TeamInfo
	{
		public LMSTeamInfo(int num) { teamNum = num; }
		public Score lives;
		public int teamNum;
	}
	
	LMSTeamInfo[] teams = new LMSTeamInfo[0];
	
	@Override
	public int getMinPlayers() { return 4; }
	
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
								public byte getWoolColor() { return (byte)0xE; }
								@Override
								public Color getArmorColor() { return Color.RED; }
							}; break;
						case 1:
							newTeams[i] = new LMSTeamInfo(i) {
								@Override
								public String getName() { return "blue team"; }
								@Override
								public ChatColor getChatColor() { return ChatColor.BLUE; }
								@Override
								public byte getWoolColor() { return (byte)0xB; }
								@Override
								public Color getArmorColor() { return Color.fromRGB(0x0066FF); }
							}; break;
						case 2:
							newTeams[i] = new LMSTeamInfo(i) {
								@Override
								public String getName() { return "yellow team"; }
								@Override
								public ChatColor getChatColor() { return ChatColor.YELLOW; }
								@Override
								public byte getWoolColor() { return (byte)0x4; }
								@Override
								public Color getArmorColor() { return Color.YELLOW; }
							}; break;
						case 3:
							newTeams[i] = new LMSTeamInfo(i) {
								@Override
								public String getName() { return "green team"; }
								@Override
								public ChatColor getChatColor() { return ChatColor.GREEN; }
								@Override
								public byte getWoolColor() { return (byte)0x5; }
								@Override
								public Color getArmorColor() { return Color.GREEN; }
							}; break;
					}
				teams = newTeams;
				setTeams(teams);
			}
		};
		numTeams.setHidden(true);
		
		numLives = new NumericOption("Number of lives", 1, 7, Material.APPLE, 1, "Players can die this many times", "before they're out of the game.", "In a team game, players share", "lives with their teammates.");
		
		friendlyFire = new ToggleOption("Friendly fire", true, "When enabled, players can hurt", "their teammates with weapons.");
		friendlyFire.setHidden(true);
		
		centralizedSpawns = new ToggleOption("Centralized spawns", true, "When enabled, players spawn in", "a circle around a chest full of", "equipment. When disabled, players", "spawn spread out in the world");
		
		contractKills = new ToggleOption("Contract Kills", false, "When enabled, each player is given", "the name of another. They're only", "allowed to kill this target,", "or the player hunting them.", "Trying to hurt anyone else", "will damage yourself instead.");
		
		useTeams = new ToggleOption("Use Teams", false, "Allows players to be divided", "into separate teams.") {
			@Override
			public void changed()
			{
				numTeams.setHidden(!isEnabled()); // can only set team count when teams enabled
				
				if ( isEnabled() )
				{
					friendlyFire.setHidden(false);
					
					// can only use contract killer & centralized spawns when teams disabled
					contractKills.setHidden(true);
					centralizedSpawns.setHidden(true);
					
					if ( contractKills.isEnabled() )
						contractKills.toggle();
					if ( centralizedSpawns.isEnabled() )
						centralizedSpawns.toggle();
					setTeams(teams);
				}
				else
				{
					contractKills.setHidden(false);
					centralizedSpawns.setHidden(false);
					setTeams(new TeamInfo[0]);
					
					friendlyFire.setHidden(true);
					if ( !friendlyFire.isEnabled() )
						friendlyFire.toggle();
				}
			}
		};
		
		return new Option[] { useTeams, numTeams, centralizedSpawns, contractKills, numLives };
	}
	
	@Override
	public String getHelpMessage(int num, TeamInfo team)
	{
		switch ( num )
		{
			case 0:
				if ( inWarmup )
					return "Every player will soon be assigned a target to kill, which they must do without being seen by anyone else.";
				else
					return "Every player has been assigned a target to kill, which they must do without being seen by anyone else.";
			
			case 1:
				return "Your compass points towards your victim, and if anyone sees you kill them, you will die instead of them.";
			
			case 2:
				return "Remember that someone else is hunting you! If you kill anyone other than your target or your hunter, you will die instead of them.";
			
			case 3:
				return "When you kill your target, you are assigned their target, and the game continues until only one player remains alive.";
			
			default:
				return null;
		}
	}
	
	@Override
	public Environment[] getWorldsToGenerate() { return new Environment[] { Environment.NORMAL }; }
		
	@Override
	public boolean isLocationProtected(Location l, Player p) { return inWarmup && centralizedSpawns.isEnabled(); }
	
	@Override
	public boolean isAllowedToRespawn(Player player) { return false; }

	@Override
	public boolean useDiscreetDeathMessages() { return false; }
	
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
		else if ( centralizedSpawns.isEnabled() )
		{
			int playerNumber = nextPlayerNumber ++; // get a number for this player, somehow.
			return getCircleSpawnLocation(playerNumber, playerSeparation);
		}
		else
		{
			int playerNumber = nextPlayerNumber ++;
			return getSpreadOutSpawn(playerNumber);
		}
	}
	
	final double teamSeparation = 100, playerSeparation = 8;
	double angularSeparation, spawnCircleRadius;

	private Location getCircleSpawnLocation(int spawnNumber, double separation)
	{
		// spawns are spread out in a circle around the center.
		// the radius of this circle is dependent on the number of players/teams, such that they will always
		// be a fixed distance from their nearest neighbours, regardless of how many players/teams we have.
		
		Location spawn = getWorld(0).getSpawnLocation();
		double angle = angularSeparation * spawnNumber;
		
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
	int allocationProcessID = -1;
	
	@Override
	public void gameStarted()
	{
		if ( useTeams.isEnabled() )
		{
			angularSeparation = 2 * Math.PI / teams.length;
			spawnCircleRadius = 0.5 * teamSeparation / Math.sin(angularSeparation / 2);
		}
		else if ( centralizedSpawns.isEnabled() )
		{
			angularSeparation = 2 * Math.PI / getOnlinePlayers().size();
			spawnCircleRadius = 0.5 * playerSeparation / Math.sin(angularSeparation / 2);
		}
		
		inWarmup = true;
		nextPlayerNumber = 1; // ensure that the player placement logic starts over again
		
		// allocation doesn't happen right away, there's 30 seconds of "scrabbling" first
		allocationProcessID = getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
			public void run()
			{
				allocateTargets();
				allocationProcessID = -1;
			}
		}, warmupDelayTicks);
	}
	
	private void allocateTargets()
	{
		// give everyone a target, make them be someone else's target
		List<Player> players = getOnlinePlayers(new PlayerFilter().alive());
		
		if ( players.size() < getMinPlayers() )
		{
			broadcastMessage("Cannot start game: insufficient players to assign targets. A minimum of " + getMinPlayers() + " players are required.");
			return;
		}
		
		Player firstOne = players.remove(random.nextInt(players.size()));
		Player prevOne = firstOne;
		
		while ( players.size() > 0 )
		{
			
			Player current = players.remove(random.nextInt(players.size()));
			Helper.setTargetOf(getGame(), prevOne, current);
			
			prevOne.sendMessage("Your target is: " +  ChatColor.YELLOW + current.getName() + ChatColor.RESET + "!");
			prevOne.getInventory().addItem(new ItemStack(Material.COMPASS, 1));
			prevOne = current;
		}
		
		Helper.setTargetOf(getGame(), prevOne, firstOne);
		prevOne.sendMessage("Your target is: " +  ChatColor.YELLOW + firstOne.getName() + ChatColor.RESET + "!");
		prevOne.getInventory().addItem(new ItemStack(Material.COMPASS, 1));
		
		broadcastMessage("All players have been allocated a target to kill");
		inWarmup = false;
	}
	
	@Override
	public void gameFinished()
	{
		victimWarningTimes.clear();
		nextPlayerNumber = 1;
		
		if ( allocationProcessID != -1 )
		{
			getPlugin().getServer().getScheduler().cancelTask(allocationProcessID);
			allocationProcessID = -1;
		}
	}
	
	@Override
	public void playerJoinedLate(Player player, boolean isNewPlayer)
	{
		if ( !isNewPlayer )
		{
			Player target = Helper.getTargetOf(getGame(), player);
			if ( target != null )
				player.sendMessage("Your target is: " +  ChatColor.YELLOW + target.getName() + ChatColor.RESET + "!");
			else
				player.sendMessage("You don't seem to have a target... sorry!");
			return;
		}
		
		List<Player> players = getOnlinePlayers(new PlayerFilter().alive());
		if ( players.size() < 2 )
			return;
		
		// pick a player to be this player's hunter. This player's victim will be the hunter's victim.
		int hunterIndex = random.nextInt(players.size()-1), i = 0;
		for ( Player hunter : players )
			if ( hunter == player )
				continue; // ignore self
			else if ( i == hunterIndex )
			{
				Player target = Helper.getTargetOf(getGame(), hunter);
				Helper.setTargetOf(getGame(), player, target);
				Helper.setTargetOf(getGame(), hunter, player);
				
				hunter.sendMessage("Your target has changed, and is now: " +  ChatColor.YELLOW + player.getName() + ChatColor.RESET + "!");
				
				player.sendMessage("Your target is: " +  ChatColor.YELLOW + target.getName() + ChatColor.RESET + "!");
				player.getInventory().addItem(new ItemStack(Material.COMPASS, 1));
				break;
			}
			else
				i++;
	}
	
	@Override
	public void playerQuit(OfflinePlayer player)
	{
		if ( hasGameFinished() )
			return;
		
		List<Player> survivors = getOnlinePlayers(new PlayerFilter().alive());
		
		if ( survivors.size() > 1 ) 
		{// find this player's hunter ... change their target to this player's target
			for ( Player survivor : survivors )
				if ( player == Helper.getTargetOf(getGame(), survivor) )
				{
					Player target = Helper.getTargetOf(getGame(), player);
					Helper.setTargetOf(getGame(), survivor, target);
					
					survivor.sendMessage("Your target has changed, and is now: " +  ChatColor.YELLOW + target.getName() + ChatColor.RESET + "!");
					break;
				}
		}
		Helper.setTargetOf(getGame(), player, null);
		
		if ( survivors.size() == 1 )
		{
			Player survivor = survivors.get(0);
			broadcastMessage(new PlayerFilter().exclude(survivor), survivor.getName() + " is the last man standing, and wins the game!");
			survivor.sendMessage("You are the last man standing: you win the game!");
		}
		else if ( survivors.size() == 0 )
			broadcastMessage("All players died, nobody wins!");
		else if ( survivors.size() == 3 )
		{
			broadcastMessage("Three players remain: everyone is now a legitimate target!");
			return;
		}
		else
			return; // multiple people left in the game
		
		finishGame();
	}
	
	@Override
	public Location getCompassTarget(Player player)
	{
		Player target = Helper.getTargetOf(getGame(), player);
		if ( target != null )
			return target.getLocation();
		
		return null;
	}
	
	private static final double maxObservationRangeSq = 60 * 60;
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void entityDamaged(EntityDamageEvent event)
	{
		if ( useTeams.isEnabled() )
		{
			if ( friendlyFire.isEnabled() )
				return;
			
			Player victim = (Player)event.getEntity();
			if ( victim == null )
				return;
			
			Player attacker = Helper.getAttacker(event);
			if ( attacker == null )
				return;
			
			if ( getTeam(victim) == getTeam(attacker) )
				event.setCancelled(true);
		}
		
		else if ( !contractKills.isEnabled() )
			return;
		
		Player victim = (Player)event.getEntity();
		if ( victim == null )
			return;
		
		Player attacker = Helper.getAttacker(event);
		if ( attacker == null )
			return;
		
		Player victimTarget = Helper.getTargetOf(getGame(), victim);
		Player attackerTarget = Helper.getTargetOf(getGame(), attacker);

		// armour is a problem. looks like its handled in EntityHuman.b(DamageSource damagesource, int i) - can replicate the code ... technically also account for enchantments
		if ( event.getDamage() >= victim.getHealth() )
			if ( attackerTarget == victim || victimTarget == attacker )
			{// this interaction was allowed ... should still check if they were observed!
				List<Player> survivors = getOnlinePlayers(new PlayerFilter().alive());
				
				for ( Player observer : survivors )
				{
					 if ( observer == victim || observer == attacker )
						 continue;
					 
					 if ( Helper.playerCanSeeOther(observer, attacker, maxObservationRangeSq) )
					 {
						 attacker.damage(50);
						 
						 attacker.sendMessage("You were observed trying to kill " + victim.getName() + " by " + observer.getName() + ", so you've been killed instead.");
						 victim.sendMessage(attacker.getName() + " tried to kill you, but was observed doing so by " + observer.getName() + " - so " + attacker.getName() + " has been killed instead.");
						 observer.sendMessage("You observed " + attacker.getName() + " trying to kill " + victim.getName() + ", so " + attacker.getName() + " was killed instead.");
						 
						 event.setCancelled(true);
						 return;
					 }
				}
				
				if ( victimTarget == attacker && survivors.size() > 1)
					victim.sendMessage("You killed your hunter - but someone else is hunting you now!");
			}
			else
			{
				// this wasn't a valid kill target, and was a killing blow
				attacker.damage(50);
				
				attacker.sendMessage(victim.getName() + " was neither your target nor your hunter, so you've been killed for trying to kill them!");
				victim.sendMessage(attacker.getName() + " tried to kill you - they've been killed instead.");
				
				event.setCancelled(true);
				return;
			}
		else if ( attackerTarget == victim )
		{
			if ( shouldSendVictimMessage(victim.getName(), attacker.getName(), "H") )
				victim.sendMessage(attacker.getName() + " is your hunter - " + ChatColor.RED + "they can kill you!");
		}
		else if ( victimTarget == attacker )
		{
			if ( shouldSendVictimMessage(victim.getName(), attacker.getName(), "V") )
				victim.sendMessage(attacker.getName() + " is your victim - " + ChatColor.RED + "they can kill you!");
		}
		else
		{
			if ( shouldSendVictimMessage(victim.getName(), attacker.getName(), "-") )
				victim.sendMessage(attacker.getName() + " is neither your hunter nor your victim - they cannot kill you, and will die if they try!");
		}
	}
	
	private static final long victimWarningRepeatInterval = 200;
	private Map<String, Long> victimWarningTimes = new LinkedHashMap<String, Long>();
	
	private boolean shouldSendVictimMessage(String victim, String attacker, String relationship)
	{
		// if there's a value saved for this player pair/relationship, see if it was saved within the last 10 secs - if so, don't send.
		String key = victim + "|" + attacker + "|" + relationship;
		long currentTime = getWorld(0).getTime();
		
		if ( victimWarningTimes.containsKey(key) )
		{
			long warnedTime = victimWarningTimes.get(key);
			if ( currentTime - warnedTime <= victimWarningRepeatInterval )
				return false; // they were warned about this attacker IN THIS RELATIONSHIP within the last 10 secs. Don't warn again.
		}
		
		// save this off, so they don't get this same message again in the next 10 secs
		victimWarningTimes.put(key, currentTime);
		return true;
	}
}
