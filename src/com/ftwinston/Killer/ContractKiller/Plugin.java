package com.ftwinston.Killer.ContractKiller;

import org.bukkit.plugin.java.JavaPlugin;

import com.ftwinston.Killer.Killer;

public class Plugin extends JavaPlugin
{
	public void onEnable()
	{
		Killer.registerGameMode(new ContractKiller());
	}
}