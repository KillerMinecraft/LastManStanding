package com.ftwinston.KillerMinecraft.Modules.LastManStanding;

import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;

class SpawnPopulator extends BlockPopulator
{
	public SpawnPopulator(Location center, int radius, int clearAboveY, int fillBelowY)
	{
		this.center = center;
		this.radius = radius;
		this.clearAboveY = clearAboveY;
		this.fillBelowY = fillBelowY;
		
		surfaceY = center.getBlockY() - 1;
		
		minX = center.getBlockX() - radius;
		maxX = center.getBlockX() + radius;
		minZ = center.getBlockZ() - radius;
		maxZ = center.getBlockZ() + radius;
		
		cMinX = minX >> 4; cMaxX = maxX >> 4;
		cMinZ = minZ >> 4; cMaxZ = maxZ >> 4;
	}
	
	int minX, maxX, minZ, maxZ;
	int cMinX, cMaxX, cMinZ, cMaxZ;
	int radius, surfaceY, clearAboveY, fillBelowY;
	Location center;
	
	@Override
	public void populate(World w, Random r, Chunk c)
	{
		int cx = c.getX(), cz = c.getZ();
		if ( cx < cMinX || cx > cMaxX || cz < cMinZ || cz > cMaxZ)
			return;
		
		int cMinX = c.getX() << 4, cMinZ = c.getZ() << 4;
		
		for (int x = minX - cMinX; x < 16 && x + cMinX <= maxX; x++)
			for (int z = minZ - cMinZ; z < 16 && z + cMinZ <= maxZ; z++)
			{
				c.getBlock(x, surfaceY, z).setType(Material.GRASS);
				
				for (int y=surfaceY + 1; y <= surfaceY + clearAboveY; y++)
					c.getBlock(x, surfaceY, z).setType(Material.AIR);
				
				for (int y=surfaceY - 1; y >= surfaceY - fillBelowY; y--)
				{
					Block b = c.getBlock(x, surfaceY, z);
					
					if (isGround(b.getType()))
						break;
					
					b.setType(Material.DIRT);
				}
			}
		
	}
	
	private boolean isGround(Material type)
	{
		switch(type)
		{
		case GRASS:
		case DIRT:
		case STONE:
		case SAND:
		case GRAVEL:
		case BEDROCK:
			return true;
		
		default:
			return false;
		}
	}
}