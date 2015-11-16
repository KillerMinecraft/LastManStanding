package com.ftwinston.KillerMinecraft.Modules.LastManStanding;

import org.bukkit.Material;

import com.ftwinston.KillerMinecraft.GameMode;
import com.ftwinston.KillerMinecraft.GameModePlugin;

public class Plugin extends GameModePlugin
{
	@Override
	public Material getMenuIcon() { return Material.BOW; }
	
	@Override
	public String[] getDescriptionText() { return new String[] {"Kill or be killed, on your", "own or in a team."}; }
	
	@Override
	public GameMode createInstance()
	{
		return new LastManStanding();
	}
}