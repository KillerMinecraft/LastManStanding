package com.ftwinston.Killer.ContractKiller;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.GameModePlugin;
import com.ftwinston.Killer.Killer;

public class Plugin extends GameModePlugin
{
	public void onEnable()
	{
		Killer.registerGameMode(this);
	}
	
	@Override
	public GameMode createInstance()
	{
		return new ContractKiller();
	}
	
	@Override
	public String[] getSignDescription()
	{
		return new String[] {
			"Each player is",
			"given a target,",
			"to kill without",
			"anyone seeing.",
			
			"You can only",
			"kill your own",
			"target, or your",
			"hunter.",
			
			"You take your",
			"victim's target",
			"when they die.",
			""
		};
	}
}