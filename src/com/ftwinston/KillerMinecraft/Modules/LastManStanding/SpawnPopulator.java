package com.ftwinston.KillerMinecraft.Modules.LastManStanding;

import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;

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
				for (int y=surfaceY + clearAboveY; y > surfaceY; y--)
					c.getBlock(x, y, z).setType(Material.AIR);

				c.getBlock(x, surfaceY, z).setType(Material.GRASS);
				
				for (int y=surfaceY - 1; y >= surfaceY - fillBelowY; y--)
				{
					Block b = c.getBlock(x, y, z);
					
					if (isGround(b.getType()))
						break;
					
					b.setType(Material.DIRT);
				}
			}
	}
	
	private static boolean isGround(Material type)
	{
		switch(type)
		{
		case GRASS:
		case DIRT:
		case STONE:
		case COBBLESTONE:
		case GRAVEL:
		case SAND:
		case SANDSTONE:
		case BEDROCK:
			return true;
		
		default:
			return false;
		}
	}

	public static void createCentralSpawnItems(World world)
	{
		Location center = world.getSpawnLocation();
		
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
	
	private static void populateRandomChest(Block b)
	{
		// TODO Auto-generated method stub
		b.setType(Material.TRAPPED_CHEST);
		
		Inventory inv = ((Chest)b.getState()).getBlockInventory();
		Random r = new Random();
		
		int numItems = r.nextInt(8) + 4;
		for (int i=0; i<numItems; i++)
			inv.setItem(r.nextInt(27), selectRandomItem(r));
	}

	private static ItemStack selectRandomItem(Random r)
	{
		switch (r.nextInt(38))
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
		case 29:
		case 30:
		case 31:
		case 32:
		case 33:
			return new ItemStack(Material.COMPASS);
		default:
			return new ItemStack(Material.APPLE, r.nextInt(5) + 1);
		}
	}

	private static ItemStack createPotion(PotionType type, int level)
	{
		ItemStack stack = new ItemStack(Material.POTION);
		Potion potion = new Potion(type);
		potion.setLevel(level);
		potion.apply(stack);
		return stack;
	}
}