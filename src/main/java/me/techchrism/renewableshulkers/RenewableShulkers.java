package me.techchrism.renewableshulkers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Random;

public class RenewableShulkers extends JavaPlugin implements Listener
{
    private Method shulkerTeleport;
    private final Random shulkerRandom = new Random();
    
    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
        try
        {
            shulkerTeleport = getShulkerTeleportMethod();
            if(shulkerTeleport == null)
            {
                getLogger().severe("Could not find shulker teleportation method!");
                setEnabled(false);
            }
        }
        catch(ClassNotFoundException e)
        {
            getLogger().severe("Error getting shulker teleportation method:");
            e.printStackTrace();
            setEnabled(false);
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event)
    {
        if(event.getEntity().getType() != EntityType.SHULKER || event.getDamager().getType() != EntityType.SHULKER_BULLET)
        {
            return;
        }
    
        Shulker shulker = (Shulker) event.getEntity();
        try
        {
            // Ensure the shulker can teleport
            Method entityHandlerMethod = shulker.getClass().getMethod("getHandle");
            Object nmsEntity = entityHandlerMethod.invoke(shulker);
            Location previousLoc = shulker.getLocation();
            boolean success = (boolean) shulkerTeleport.invoke(nmsEntity);
            if(!success)
            {
                return;
            }
    
            // Make new shulkers less likely if there are nearby existing shulkers
            int nearbyShulkerCount = 0;
            for(Entity e : shulker.getNearbyEntities(8.0, 8.0, 8.0))
            {
                if(!e.isDead() && e.getType() == EntityType.SHULKER)
                {
                    nearbyShulkerCount++;
                }
            }
            if(shulkerRandom.nextFloat() < ((nearbyShulkerCount - 1) / 5.0F))
            {
                return;
            }
            
            previousLoc.getWorld().spawn(previousLoc, Shulker.class, newShulker -> newShulker.setColor(shulker.getColor()));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static Method getShulkerTeleportMethod() throws ClassNotFoundException
    {
        Class<?> shulkerClass = Class.forName("net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".EntityShulker");
        for(Method method : shulkerClass.getDeclaredMethods())
        {
            if(method.getReturnType() == boolean.class
                    && Modifier.isProtected(method.getModifiers())
                    && !method.getName().equals("playStepSound"))
            {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }
}
