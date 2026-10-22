package pl.moneycode.eventowki;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;
import java.util.*;

public final class MoneyCodeEventowki extends JavaPlugin implements Listener {
    private NamespacedKey key;
    private final Map<UUID,Map<String,Long>> cooldowns = new HashMap<>();
    private final Map<UUID,String> projectiles = new HashMap<>();
    private final Map<Location,Material> cages = new HashMap<>();
    private final Map<UUID,Long> trapped = new HashMap<>();
    private final Map<UUID,Set<Location>> activeCages = new HashMap<>();
    private final Set<UUID> menuUsers = new HashSet<>();
    private static final String TITLE = "§5§lMoneyCode §8• §dEventówki";
    private final String[] ids = {"axe","pear","launcher","hourglass","hook"};
    private final Material[] bases = {Material.NETHERITE_AXE,Material.APPLE,Material.BLAZE_ROD,Material.CLOCK,Material.CARROT_ON_A_STICK};
    private final String[] names = {"§b⚡ Topór Gromu","§a🍐 Gruszka","§5✦ Wyrzutnia Otchłani","§e⌛ Klepsydra Czasu","§d⛓ Hak Otchłani"};
    @Override public void onEnable() {
        key = new NamespacedKey(this,"event_item");
        getServer().getPluginManager().registerEvents(this,this);
        Objects.requireNonNull(getCommand("mceventowki")).setExecutor((sender,command,label,args)->{
            if (!(sender instanceof Player p)) { sender.sendMessage("Komenda tylko dla gracza."); return true; }
            if (!p.isOp() && !p.hasPermission("moneycode.eventowki.admin")) { p.sendMessage("§cBrak uprawnień."); return true; }
            if (args.length==1 && args[0].equalsIgnoreCase("reload")) { reloadConfig(); p.sendMessage("§aPrzeładowano."); return true; }
            Inventory gui=Bukkit.createInventory(null,27,TITLE);
            int[] slots={10,11,13,15,16};
            for(int i=0;i<ids.length;i++) gui.setItem(slots[i],makeItem(i));
            p.openInventory(gui); menuUsers.add(p.getUniqueId()); return true;
        });
        getLogger().info("MoneyCode Eventowki 1.0 enabled. WorldGuard required.");
    }
    @Override public void onDisable(){
        for(var entry:new HashMap<>(cages).entrySet()) entry.getKey().getBlock().setType(entry.getValue(),false);
        cages.clear();
    }
    private ItemStack makeItem(int i){
        ItemStack stack=new ItemStack(bases[i]); ItemMeta meta=stack.getItemMeta();
        meta.setDisplayName(names[i]); meta.setCustomModelData(12001+i);
        meta.getPersistentDataContainer().set(key,PersistentDataType.STRING,ids[i]);
        meta.setLore(List.of("§7MoneyCode • autorska eventówka", "§8Działa wyłącznie na regionach PvP"));
        stack.setItemMeta(meta); return stack;
    }
    private String id(ItemStack item){
        if(item==null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING);
    }
    @EventHandler public void menu(InventoryClickEvent e){
        if (!TITLE.equals(e.getView().getTitle())) return;
        e.setCancelled(true);
        if(e.getClickedInventory()==null || e.getClickedInventory()!=e.getView().getTopInventory()) return;
        if(!(e.getWhoClicked() instanceof Player p) || (!p.isOp()&&!p.hasPermission("moneycode.eventowki.admin"))) return;
        String itemId=id(e.getCurrentItem()); if(itemId==null) return;
        for(int i=0;i<ids.length;i++) if(ids[i].equals(itemId)) {
            HashMap<Integer,ItemStack> left=p.getInventory().addItem(makeItem(i));
            if(!left.isEmpty()) p.sendMessage("§cBrak miejsca w ekwipunku."); else p.sendMessage("§aOtrzymano: "+names[i]);
            break;
        }
    }
    private boolean allowed(Location loc){
        if(loc.getWorld()==null || Bukkit.getPluginManager().getPlugin("WorldGuard")==null) return false;
        var manager=WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(loc.getWorld()));
        if(manager==null)return false;
        ApplicableRegionSet regions=manager.getApplicableRegions(BlockVector3.at(loc.getBlockX(),loc.getBlockY(),loc.getBlockZ()));
        boolean pvp=false;
        for(ProtectedRegion r:regions){ String n=r.getId().toLowerCase(Locale.ROOT);
            if(n.contains("spawn")) return false;
            if(n.contains("pvp")) pvp=true;
        }
        return pvp;
    }
    private boolean ready(Player p,String id,int seconds){
        long now=System.currentTimeMillis(); long next=cooldowns.computeIfAbsent(p.getUniqueId(),k->new HashMap<>()).getOrDefault(id,0L);
        if(now<next){p.sendMessage("§cOdczekaj jeszcze "+((next-now+999)/1000)+" s.");return false;}
        cooldowns.get(p.getUniqueId()).put(id,now+seconds*1000L); return true;
    }
    @EventHandler(priority=EventPriority.HIGH) public void interact(PlayerInteractEvent e){
        if(e.getHand()!=EquipmentSlot.HAND || (e.getAction()!=Action.RIGHT_CLICK_AIR && e.getAction()!=Action.RIGHT_CLICK_BLOCK))return;
        Player p=e.getPlayer(); String item=id(e.getItem()); if(item==null || item.equals("pear")) return;
        e.setCancelled(true);
        if(!allowed(p.getLocation())){p.sendMessage("§cEventówki działają tylko w regionach PvP (nie spawn).");return;}
        switch(item){
            case "axe" -> {
                if(!ready(p,item,50))return;
                p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,p.getLocation(),70,3,1,3,0.15);
                for(Entity entity:p.getNearbyEntities(5,5,5)) if(entity instanceof Player victim && victim!=p && allowed(victim.getLocation()) && p.hasLineOfSight(victim)){
                    victim.damage(6.0,p); victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,60,1));
                }
            }
            case "launcher" -> {
                if(!ready(p,item,120))return;
                Snowball ball=p.launchProjectile(Snowball.class);ball.setVelocity(p.getEyeLocation().getDirection().multiply(1.7));
                projectiles.put(ball.getUniqueId(),"launcher");
            }
            case "hourglass" -> {
                if(!ready(p,item,80))return;
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,100,1));
                p.getWorld().spawnParticle(Particle.ENCHANT,p.getLocation(),80,3,1,3,0.1);
                for(Entity entity:p.getNearbyEntities(6,6,6)) if(entity instanceof Player victim && victim!=p && allowed(victim.getLocation()) && p.hasLineOfSight(victim))
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,100,2));
            }
            case "hook" -> {
                if(!ready(p,item,45))return;
                Snowball ball=p.launchProjectile(Snowball.class);ball.setVelocity(p.getEyeLocation().getDirection().multiply(2.0));
                projectiles.put(ball.getUniqueId(),"hook");
            }
        }
    }
    @EventHandler public void eat(PlayerItemConsumeEvent e){
        if(!"pear".equals(id(e.getItem())))return;
        if(!allowed(e.getPlayer().getLocation())){e.setCancelled(true);e.getPlayer().sendMessage("§cGruszka działa tylko na PvP.");return;}
        Bukkit.getScheduler().runTask(this,()->e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,60,4)));
    }
    @EventHandler public void projectile(ProjectileHitEvent e){
        String type=projectiles.remove(e.getEntity().getUniqueId()); if(type==null)return;
        if(!(e.getEntity().getShooter() instanceof Player shooter))return;
        Location hit=e.getHitEntity()!=null?e.getHitEntity().getLocation():e.getEntity().getLocation();
        if(!allowed(hit) || !allowed(shooter.getLocation()))return;
        if(type.equals("hook")){
            if(e.getHitEntity() instanceof Player victim && victim!=shooter && allowed(victim.getLocation())){
                Vector toward=shooter.getLocation().toVector().subtract(victim.getLocation().toVector());
                if(toward.length()<=12 && toward.length()>0.1)victim.setVelocity(toward.normalize().multiply(1.5).setY(0.4));
            }
        } else createCage(hit);
    }
    private void createCage(Location hit){
        World w=hit.getWorld(); int x=hit.getBlockX(), y=hit.getBlockY(), z=hit.getBlockZ();
        // Hollow 5x5x5 shell; requires all shell blocks to be air and allowed PvP.
        List<Block> shell=new ArrayList<>();
        for(int dx=-2;dx<=2;dx++)for(int dy=-1;dy<=3;dy++)for(int dz=-2;dz<=2;dz++){
            if(Math.abs(dx)!=2 && dy!=-1 && dy!=3 && Math.abs(dz)!=2)continue;
            Block b=w.getBlockAt(x+dx,y+dy,z+dz);
            if(!b.getType().isAir() || !allowed(b.getLocation()) || cages.containsKey(b.getLocation()))return;
            shell.add(b);
        }
        // Also require the cage interior to be empty so no existing construction is enclosed.
        for(int dx=-1;dx<=1;dx++)for(int dy=0;dy<=2;dy++)for(int dz=-1;dz<=1;dz++){
            Block b=w.getBlockAt(x+dx,y+dy,z+dz);
            if(!b.getType().isAir() || !allowed(b.getLocation()))return;
        }
        List<Location> locations=new ArrayList<>();
        for(Block b:shell){Location loc=b.getLocation();cages.put(loc,b.getType());locations.add(loc);b.setType(Material.TINTED_GLASS,false);}
        w.spawnParticle(Particle.PORTAL,hit,100,2,2,2,0.3);
        Bukkit.getScheduler().runTaskLater(this,()->{
            for(Location loc:locations){Material previous=cages.remove(loc);if(previous!=null && loc.getBlock().getType()==Material.TINTED_GLASS)loc.getBlock().setType(previous,false);}
        },300L);
    }
    private boolean cage(Block b){return cages.containsKey(b.getLocation());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false) public void bucket(PlayerBucketEmptyEvent e){if(cage(e.getBlockClicked()) || cage(e.getBlockClicked().getRelative(e.getBlockFace())))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false) public void bucketFill(PlayerBucketFillEvent e){if(cage(e.getBlockClicked()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false) public void cageInteract(PlayerInteractEvent e){if(e.getClickedBlock()!=null && cage(e.getClickedBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false) public void teleport(PlayerTeleportEvent e){
        if(e.getTo()==null || e.getCause()!=PlayerTeleportEvent.TeleportCause.ENDER_PEARL)return;
        if(isInsideCage(e.getFrom()) || isInsideCage(e.getTo()))e.setCancelled(true);
    }
    private boolean isInsideCage(Location loc){
        if(loc.getWorld()==null)return false;
        for(Location shell:cages.keySet()){
            if(!shell.getWorld().equals(loc.getWorld()))continue;
            if(Math.abs(shell.getBlockX()-loc.getBlockX())<=4 && Math.abs(shell.getBlockY()-loc.getBlockY())<=4 && Math.abs(shell.getBlockZ()-loc.getBlockZ())<=4){
                // Check for a complete nearby enclosure (center inferred from its shell). Conservative check.
                for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
                    Location floor=loc.getWorld().getBlockAt(loc.getBlockX()+dx,loc.getBlockY()-1,loc.getBlockZ()+dz).getLocation();
                    if(cages.containsKey(floor))return true;
                }
            }
        }
        return false;
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false) public void breakBlock(BlockBreakEvent e){if(cage(e.getBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false) public void placeBlock(BlockPlaceEvent e){if(cage(e.getBlockPlaced())||cage(e.getBlockAgainst()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void explode(EntityExplodeEvent e){e.blockList().removeIf(this::cage);}
    @EventHandler(priority=EventPriority.HIGHEST) public void blockExplode(BlockExplodeEvent e){e.blockList().removeIf(this::cage);}
    @EventHandler(priority=EventPriority.HIGHEST) public void pistonExtend(BlockPistonExtendEvent e){if(e.getBlocks().stream().anyMatch(this::cage))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void pistonRetract(BlockPistonRetractEvent e){if(e.getBlocks().stream().anyMatch(this::cage))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false) public void ignite(BlockIgniteEvent e){if(cage(e.getBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false) public void physics(BlockPhysicsEvent e){if(cage(e.getBlock()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void flow(BlockFromToEvent e){if(cage(e.getToBlock()))e.setCancelled(true);}
}
