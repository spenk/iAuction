import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;


public class iAuctionListener extends PluginListener{
	Logger log = Logger.getLogger("Minecraft");
	int maxtime;
	int mintime;
	int time;
	double maxprice;
	double minprice;
	double price;
	boolean allowcreative;
	boolean adminallow;
	String disalloweditems;
	String currency = (String)etc.getLoader().callCustomHook("dCBalance", new Object[] { "MoneyName" });
	HashMap<String,Item> auction = new HashMap<String,Item>();
	HashMap<String,Double> bidders = new HashMap<String,Double>();
	List<Integer> disalloweditemslist;
	
	
	public void loadprops(){
		PropertiesFile props = new PropertiesFile("plugins/config/iAuction.properties");
		maxtime = props.getInt("Max-Auction-Time" , 100);
		mintime = props.getInt("Minimal-Auction-Time" , 10);
		minprice = props.getDouble("Minimal-Price",10);
	    maxprice = props.getDouble("Maximal-Price",10000);
		allowcreative = props.getBoolean("Allow-Creative-Auction",false);
		adminallow = props.getBoolean("Allow-Admins-To-Bid",false);
		disalloweditems = props.getString("Disallowed-Items","7,8,9,10,11");
	}
	
	
	public boolean onCommand(Player player, String[] split) {
		if (split[0].equalsIgnoreCase("/auction")&&player.canUseCommand("/auction")){//auction time price
			if (split.length <3 || split.length >3){
				player.sendMessage("§f[§aiAuction§f]§c The correct usage is '/auction <time> <price>'");
				return true;
			}
			
			if (!auction.isEmpty()){
				player.sendMessage("§f[§aiAuction§f]§c Please wait till the current auction is ended!");
				return true;
			}
			
			try{
				time = Integer.parseInt(split[1]);  
				price = Double.parseDouble(split[2]);
				}catch(NumberFormatException nfe){
					player.sendMessage("§f[§aiAuction§f]§c The correct usage is '/auction <time> <price>'"); 
					return true;
					}
			
			if (player.getItemStackInHand() == null){
				player.sendMessage("§f[§aiAuction§f]§c Please hold the item you want to auction!");
				return true;
			}
		       Item iih = player.getItemStackInHand();
		       if (disalloweditems.contains(iih.getItemId()+"")){
				player.sendMessage("§f[§aiAuction§f]§c You cant auction this item!");
				return true;
			}
			
			if (player.getCreativeMode() == 1){
				if (!allowcreative){
					player.sendMessage("§f[§aiAuction§f]§c You are not allowed to auction items in creative mode!");
					return true;
				}
			}
			
			if (maxtime != 0){
			if (time > maxtime){
				player.sendMessage("§f[§aiAuction§f]§c You exceeded the time limit of "+maxtime+" seconds!");
				return true;
			}
			}

			if (mintime != 0){
				if (time < mintime){
					player.sendMessage("§f[§aiAuction§f]§c The minimal time for an auction is "+mintime+" seconds!");
					return true;
				}
			}

			if (minprice != 0){
				if (price > maxprice){
					player.sendMessage("§f[§aiAuction§f]§c You exceeded the price limit of "+maxprice+" "+currency+"!");
					return true;
				}
			}

			if (maxprice != 0){
				if (price < minprice){
					player.sendMessage("§f[§aiAuction§f]§c The minimal price for an auction is "+minprice+" "+currency+"!");
					return true;
				}
			}
			auction.put(player.getName(), iih);
			bidders.put(player.getName(), price);
			runauction(time,player,iih);
			player.getInventory().removeItem(player.getItemStackInHand().getSlot());
			return true;
		}
		if (split[0].equalsIgnoreCase("/bid")&&player.canUseCommand("/auctionbid")){
			double bid;
			if (split.length <2 || split.length >2){
				player.sendMessage("§f[§aiAuction§f]§c The correct usage is '/bid <price>'");
				return true;
			}
			if (auction.isEmpty()){
				player.sendMessage("§f[§aiAuction§f]§c There is no auction running!");
				return true;
			}
			List<Double> list = new ArrayList<Double>(bidders.values());
			try{bid = Double.parseDouble(split[1]);}catch(NumberFormatException nfe){player.sendMessage("§f[§aiAuction§f]§c The correct usage is '/bid <price>'"); return true;}
			if (bid < list.get(0)){
				player.sendMessage("§f[§aiAuction§f]§c Your bid must be higher than "+list.get(0)+" "+currency);
				return true;
			}
			double balance =(Double)etc.getLoader().callCustomHook("dCBalance", new Object[] { "Player-Balance", player.getName()});
			
			if (balance < bid){
				player.sendMessage("§f[§aiAuction§f]§c You dont have enough money to pay this bid");
				return true;
			}
			
			List<String> ownerc = new ArrayList<String>(auction.keySet());
			if (ownerc.get(0).equals(player.getName())){
				player.sendMessage("§f[§aiAuction§f]§c You cant bid on your own auction!");
				return true;
			}
			
			List<String> list1 = new ArrayList<String>(bidders.keySet());
			if (list1.get(0).equals(player.getName())){
			   player.sendMessage("§f[§aiAuction§f]§c you've just offered in this auction");
			   return true;
			}
			if (player.isAdmin() && !adminallow){
				player.sendMessage("§f[§aiAuction§f]§c Admins are not allowed to bid!");
				return true;
			}
			setbid(player,bid);
			etc.getServer().messageAll("§f[§aiAuction§f]§3 5 §3"+player.getName()+" §1 Has just offered  §6"+bid+" §1"+currency);
			return true;
		}
		if (split[0].equalsIgnoreCase("/auctioninfo")&&player.canUseCommand("/auction")){
			if (auction.isEmpty()){
				player.sendMessage("§f[§aiAuction§f]§c There is no auction running!");
				return true;
			}
			List<String> list1 = new ArrayList<String>(auction.keySet());
			ArrayList<String> dat = itemdata(auction.get(list1.get(0)));
			List<String> bidderss = new ArrayList<String>(bidders.keySet());
			player.sendMessage("§f[§aiAuction§f]§1 Auction Host: §3"+list1.get(0));
			player.sendMessage("§f[§aiAuction§f]§1 Auction Item Name: §3"+dat.get(0));
			player.sendMessage("§f[§aiAuction§f]§1 Auction Item Id: §3"+dat.get(1));
			player.sendMessage("§f[§aiAuction§f]§1 Auction Item Amount: §3"+dat.get(2));
			player.sendMessage("§f[§aiAuction§f]§1 Auction Item Damage: §3"+dat.get(3));
			player.sendMessage("§f[§aiAuction§f]§1 Auction Item Enchantments: §3"+dat.get(4));
			player.sendMessage("§f[§aiAuction§f]§1 Auction Item Price: §3"+bidders.get(bidderss.get(0)));
			player.sendMessage("§1-----------------Bids info-----------------");
			if (bidderss.get(0).equals(list1.get(0))){
			player.sendMessage("§f[§aiAuction§f]§1 Auction Bidder: §3No bids");	
			player.sendMessage("§f[§aiAuction§f]§1 Auction Price: §3No bids");	
			return true;
			}else{
				player.sendMessage("§f[§aiAuction§f]§1 Auction Bidder: §3"+bidderss.get(0));	
				player.sendMessage("§f[§aiAuction§f]§1 Auction Price: §3"+bidders.get(bidderss.get(0)));
				return true;
			}
		}
		return false;
	}
	
	public void runauction(final int time, final Player player, final Item item){
		ArrayList<String> id = itemdata(item);
		if (item.getEnchantment() == null){
		etc.getServer().messageAll("§f[§1iAuction§f] - §3"+player.getName()+"§1 Is selling §3"+id.get(2)+ " " +id.get(0)+"§1 for §6"+ price +" §1"+currency+" (damage "+ id.get(3)+")");
		}else{
	    etc.getServer().messageAll("§f[§1iAuction§f] - §3"+player.getName() +"§1 Is selling §3"+id.get(2)+" "+ id.get(0)+"§1 With §3"+ id.get(4) +"§1 for §6"+ price +" §1"+currency+" (damage "+ id.get(3)+")");
		}
	    new Thread() {
	   	     public void run() {
	   	          try{
		             int run = (time-5)*1000;
	   	        	  Thread.sleep(run);
	   	        	etc.getServer().messageAll("§f[§aiAuction§f]§3 5 §1Seconds left to bid!");
	   	        	  Thread.sleep(1000);
	   	        	etc.getServer().messageAll("§f[§aiAuction§f]§3 4 §1Seconds left to bid!");
	   	        	  Thread.sleep(1000);
	   	        	etc.getServer().messageAll("§f[§aiAuction§f]§3 3 §1Seconds left to bid!");
	   	        	  Thread.sleep(1000);
	   	        	etc.getServer().messageAll("§f[§aiAuction§f]§3 2 §1Seconds left to bid!");
	   	        	  Thread.sleep(1000);
	   	        	etc.getServer().messageAll("§f[§aiAuction§f]§3 1 §1Seconds left to bid!");
	   	        	  Thread.sleep(1000);
	   	        	etc.getServer().messageAll("§f[§aiAuction§f] §1- §1Auction ended.");
	   	        	
	   	        	if (bidders.containsKey(player.getName())){
	   	        		etc.getServer().messageAll("§f[§aiAuction§f] §1--Auction ended with no bids--");
	   	        		returnitem(player,item);	
	   	        		dropdata();
	   	        	}else{
	   	        		if (checkscam()){return;}
	   	        		List<String> list = new ArrayList<String>(bidders.keySet());
	   	        		List<Double> prc = new ArrayList<Double>(bidders.values());
	   	        		etc.getServer().messageAll("§f[§aiAuction§f] §1AuctionWinner is §3"+list.get(0)+" §6"+prc.get(0)+" §1"+currency);
	   	        		sellitem(player,item);
	   	        		dropdata();
	   	        	}
	   	        }catch(InterruptedException e) {player.sendMessage("§f[§aiAuction§f] §c an error occured report it to an admin!");}
	   	     }
	    }.start();
	}
	
	public void returnitem(Player player ,Item item){
		int slot = player.getInventory().getEmptySlot();
		if (slot == -1 || slot >= 100){
			player.getWorld().dropItem(player.getLocation(), item);
			player.sendMessage("§f[§aiAuction§f] §1 No space in inventory found (dropping item)!");
		}else{
			player.getInventory().setSlot(item, slot);
			player.sendMessage("§f[§aiAuction§f] §1 item given!");
		}
	}
	
	public void sellitem(Player seller,Item item){
		List<String> list = new ArrayList<String>(bidders.keySet());
		Player buyer = etc.getServer().matchPlayer(list.get(0));
		double prc = bidders.get(buyer.getName());
	      etc.getLoader().callCustomHook("dCBalance", new Object[] { "Player-Charge", buyer.getName(), (Double) prc});
	      etc.getLoader().callCustomHook("dCBalance", new Object[] { "Player-Pay", seller.getName(), (Double) prc});
	      returnitem(buyer,item);
	}
	
	public void dropdata(){
		if (!auction.isEmpty()){
		auction.clear();
		if (!bidders.isEmpty()){
			bidders.clear();
		}
		}else{
		if (!bidders.isEmpty()){
			bidders.clear();
		}
		}
	}
	
	public void setbid(Player player,double price) {
		if (!bidders.isEmpty()){
			bidders.clear();
			bidders.put(player.getName(), price);
		}else{
		bidders.put(player.getName(), price);
		}
	}
	
	public void tolist() throws Exception {
		if (disalloweditems == null || disalloweditems.equals("")){return;}
		if (!disalloweditems.contains(",")){disalloweditemslist.add(Integer.parseInt(disalloweditems)); return;}
		String[] sarray = disalloweditems.split(",");
		if (sarray != null) {
		for (int i = 0; i < sarray.length; i++) {
		disalloweditemslist.add(Integer.parseInt(sarray[i]));
		log.info(disalloweditemslist+"");
		}
		return;
		}
		return;
		}
	
	public static ArrayList<String> itemdata(Item item){
		ArrayList<String> r = new ArrayList<String>();
		String name,id, amount, damage, enchantments;
		name = getname(item);
		id = item.getItemId()+"";
		amount = item.getAmount()+"";
		damage = item.getDamage()+"";
			StringBuilder sb = new StringBuilder();
			if (item.getEnchantment() != null){
			for(Enchantment enchantment : item.getEnchantments()){
				sb.append(enchantment.getType().toString()+ " ");
				sb.append(enchantment.getLevel() +" ");
			}
			enchantments = sb.toString();
		}else{
		enchantments = "";
		}
    r.add(0, name);	
	r.add(1, id);	
	r.add(2, amount);	
	r.add(3, damage);	
	r.add(4, enchantments);	
	return r;
	}
	
	public boolean checkscam(){
			List<String> bidder = new ArrayList<String>(bidders.keySet());
			List<String> seller = new ArrayList<String>(auction.keySet());
			Item item = auction.get(seller.get(0));
			String Bplayer = bidder.get(0);
			String Splayer = seller.get(0);
			
			double balance =(Double)etc.getLoader().callCustomHook("dCBalance", new Object[] { "Player-Balance", Bplayer});
			double prices = bidders.get(Bplayer);
			
			Player BPlayer = etc.getServer().matchPlayer(Bplayer);
			Player SPlayer = etc.getServer().matchPlayer(Splayer);
	        if (prices > balance){
				etc.getServer().messageAll("§f[§aiAuction§f] §3"+Bplayer +"§c TRIED TO SCAM! RESTARTING AUCTION!");
				BPlayer.kick("§cDont try to scam ever again...!");
				etc.getServer().messageAll("§f[§aiAuction§f] §3"+Bplayer +"§c IS KICKED FROM THE SERVER!");
				dropdata();
				bidders.put(Splayer, price);
	   		    runauction(time,SPlayer,item);	
	   		    return true;
		}
		return false;
	}
	
	public static String getname(Item item){
		if (item.getItemId() == 5){
			if (item.getDamage() == 0){ return "Oak wood plank";}
			if (item.getDamage() == 1){ return "Spruce Wood Plank";}
			if (item.getDamage() == 2){ return "Birch wood plank";}
			if (item.getDamage() == 3){ return "Jungle wood plank";}
		}
		if (item.getItemId() == 6){
			if (item.getDamage() == 0){ return "Oak Sapling";}
			if (item.getDamage() == 1){ return "Spruce Sapling";}
			if (item.getDamage() == 2){ return "Birch Sapling";}
			if (item.getDamage() == 3){ return "Jungle Tree Sapling";}
		}
		if (item.getItemId() == 17){
		    if (item.getDamage() == 0){ return "Oak Tree";}
		    if (item.getDamage() == 1){ return "Spruce Tree";}
		    if (item.getDamage() == 2){ return "Birch Trees";}
		    if (item.getDamage() == 3){ return "Jungle Tree";}
		}
		if (item.getItemId() == 18){
			if (item.getDamage() == 0){ return "Oak leaves";}
			if (item.getDamage() == 1){ return "Spruce leaves";}
			if (item.getDamage() == 2){ return "Birch leaves";}
			if (item.getDamage() == 3){ return "Jungle leaves";}
		}
		if (item.getItemId() == 24){
			if (item.getDamage() == 0){ return "Normal Sandstone";}
			if (item.getDamage() == 1){ return "Chiseled Sandstone";}
			if (item.getDamage() == 2){ return "Smooth Sandstone";}
		}
		if (item.getItemId() == 31){
			if (item.getDamage() == 0){ return "Dead shrub";}
			if (item.getDamage() == 1){ return "Tall grass";}
			if (item.getDamage() == 2){ return "Fern";}
		}
		if (item.getItemId() == 35){
			String toret = item.getColor().toString().toLowerCase()+" Wool";
			return toret;
		}
		if (item.getItemId() == 43){
			if (item.getDamage() == 0){ return "DoubleStone Slab";}
			if (item.getDamage() == 1){ return "DoubleSandstone Slab";}
			if (item.getDamage() == 2){ return "DoubleWooden Stone Slab";}
			if (item.getDamage() == 3){ return "DoubleCobblestone Slab";}
			if (item.getDamage() == 4){ return "DoubleBrick Slab";}
			if (item.getDamage() == 5){ return "DoubleStone Brick Slab";}
			if (item.getDamage() == 5){ return "DoubleStone Slab";}
		}
		if (item.getItemId() == 44){
			if (item.getDamage() == 0){ return "Stone Slab";}
			if (item.getDamage() == 1){ return "Sandstone Slab";}
			if (item.getDamage() == 2){ return "Wooden Stone Slab";}
			if (item.getDamage() == 3){ return "Cobblestone Slab";}
			if (item.getDamage() == 4){ return "Brick Slab";}
			if (item.getDamage() == 5){ return "Stone Brick Slab";}
		}
		if (item.getItemId() == 97){
			if (item.getDamage() == 0){ return "SilverFish Stone";}
			if (item.getDamage() == 1){ return "SilverFish CobbleStone";}
			if (item.getDamage() == 2){ return "SilverFish Stonebrick";}
	    }
		if (item.getItemId() == 98){
			if (item.getDamage() == 0){ return "Normal Stonebrick";}
			if (item.getDamage() == 1){ return "Mossy Stonebrick";}
			if (item.getDamage() == 2){ return "Cracked Stonebrick";}
			if (item.getDamage() == 3){ return "Chiseled Stonebrick";}
	    }
		if (item.getItemId() == 263){
			if (item.getDamage() == 0){ return "Coal";}
			if (item.getDamage() == 1){ return "Charcoal";}
		}
		if (item.getItemId() == 322){
			if (item.getDamage() == 0){ return "Normal Golden Apple";}
			if (item.getDamage() == 1){ return "Enchanted Golden Apple";}
		}
		if (item.getItemId() == 351){
            if (item.getDamage() == 0){return "Ink Sac";}
            if (item.getDamage() == 1){return "Rose Red";}
            if (item.getDamage() == 2){return "Cactus Green";}
            if (item.getDamage() == 3){return "Cocoa Beans";}
            if (item.getDamage() == 4){return "Lapis Lazuli";}
            if (item.getDamage() == 5){return "Purple Dye";}
            if (item.getDamage() == 6){return "Cyan Dye";}
            if (item.getDamage() == 7){return "Light Gray Dye";}
            if (item.getDamage() == 8){return "Gray Dye";}
            if (item.getDamage() == 9){return "Pink Dye";}
            if (item.getDamage() == 10){return "Lime Dye";}
            if (item.getDamage() == 11){return "Dandelion Yellow";}
            if (item.getDamage() == 12){return "Light Blue Dye";}
            if (item.getDamage() == 13){return "Magenta Dye";}
            if (item.getDamage() == 14){return "Orange Dye";}
            if (item.getDamage() == 15){return "Bone Meal";}
		}
		if (item.getItemId() == 373){
	    if (item.getDamage() == 0){return "Water Bottle";}
        if (item.getDamage() == 1){return "Potion of Regeneration";}
        if (item.getDamage() == 2){return "Potion of Swiftness";}
        if (item.getDamage() == 3){return "Potion of Fire Resistance";}
        if (item.getDamage() == 4){return "Potion of Poison";}
        if (item.getDamage() == 5){return "Potion of Healing";}
        if (item.getDamage() == 6){return "Clear Potion";}
        if (item.getDamage() == 7){return "Clear Potion";}
        if (item.getDamage() == 8){return "Potion of Weakness";}
        if (item.getDamage() == 9){return "Potion of Strength";}
        if (item.getDamage() == 10){return "Potion of Slowness";}
        if (item.getDamage() == 11){return "Diffuse Potion";}
        if (item.getDamage() == 12){return "Potion of Harming";}
        if (item.getDamage() == 13){return "Artless Potion";}
        if (item.getDamage() == 14){return "Thin Potion";}
        if (item.getDamage() == 15){return "Thin Potion";}
        if (item.getDamage() == 16){return "Awkward Potion";}
        if (item.getDamage() == 17){return "Potion of Regeneration";}
        if (item.getDamage() == 18){return "Potion of Swiftness";}
        if (item.getDamage() == 19){return "Potion of Fire Resistance";}
        if (item.getDamage() == 20){return "Potion of Poison";}
        if (item.getDamage() == 21){return "Potion of Healing";}
        if (item.getDamage() == 22){return "Bungling Potion";}
        if (item.getDamage() == 23){return "Bungling Potion";}
        if (item.getDamage() == 24){return "Potion of Weakness";}
        if (item.getDamage() == 25){return "Potion of Strength";}
        if (item.getDamage() == 26){return "Potion of Slowness";}
        if (item.getDamage() == 27){return "Smooth Potion";}
        if (item.getDamage() == 28){return "Potion of Harming";}
        if (item.getDamage() == 29){return "Suave Potion";}
        if (item.getDamage() == 30){return "Debonair Potion";}
        if (item.getDamage() == 31){return "Debonair Potion";}
        if (item.getDamage() == 32){return "Thick Potion";}
        if (item.getDamage() == 33){return "Potion of Regeneration II";}
        if (item.getDamage() == 34){return "Potion of Swiftness II";}
        if (item.getDamage() == 35){return "Potion of Fire Resistance";}
        if (item.getDamage() == 36){return "Potion of Poison II";}
        if (item.getDamage() == 37){return "Potion of Healing II";}
        if (item.getDamage() == 38){return "Charming Potion";}
        if (item.getDamage() == 39){return "Charming Potion";}
        if (item.getDamage() == 40){return "Potion of Weakness";}
        if (item.getDamage() == 41){return "Potion of Strength II";}
        if (item.getDamage() == 42){return "Potion of Slowness";}
        if (item.getDamage() == 43){return "Refined Potion";}
        if (item.getDamage() == 44){return "Potion of Harming II";}
        if (item.getDamage() == 45){return "Cordial Potion";}
        if (item.getDamage() == 46){return "Sparkling Potion";}
        if (item.getDamage() == 47){return "Sparkling Potion";}
        if (item.getDamage() == 48){return "Potent Potion";}
        if (item.getDamage() == 49){return "Potion of Regeneration II";}
        if (item.getDamage() == 50){return "Potion of Swiftness II";}
        if (item.getDamage() == 51){return "Potion of Fire Resistance";}
        if (item.getDamage() == 52){return "Potion of Poison II";}
        if (item.getDamage() == 53){return "Potion of Healing II";}
        if (item.getDamage() == 54){return "Rank Potion";}
        if (item.getDamage() == 55){return "Rank Potion";}
        if (item.getDamage() == 56){return "Potion of Weakness";}
        if (item.getDamage() == 57){return "Potion of Strength II";}
        if (item.getDamage() == 58){return "Potion of Slowness";}
        if (item.getDamage() == 59){return "Acrid Potion";}
        if (item.getDamage() == 60){return "Potion of Harming II";}
        if (item.getDamage() == 61){return "Gross Potion";}
        if (item.getDamage() == 62){return "Stinky Potion";}
        if (item.getDamage() == 63){return "Stinky Potion";}
        }
		if (item.getItemId() == 383){
	        if (item.getDamage() == 50){return "Creeper Egg";}
	        if (item.getDamage() == 51){return "Skeleton Egg";}
	        if (item.getDamage() == 52){return "Spider Egg";}
	        if (item.getDamage() == 54){return "Zombie Egg";}
	        if (item.getDamage() == 55){return "Slime Egg";}
	        if (item.getDamage() == 56){return "Ghast Egg";}
	        if (item.getDamage() == 57){return "Zombie Pigman Egg";}
	        if (item.getDamage() == 58){return "Enderman Egg";}
	        if (item.getDamage() == 59){return "Cave Spider Egg";}
	        if (item.getDamage() == 60){return "Silverfish Egg";}
	        if (item.getDamage() == 61){return "Blaze Egg";}
	        if (item.getDamage() == 62){return "Magma Cube Egg";}
	        if (item.getDamage() == 90){return "Pig Egg";}
	        if (item.getDamage() == 91){return "Sheep Egg";}
	        if (item.getDamage() == 92){return "Cow Egg";}
	        if (item.getDamage() == 93){return "Chicken Egg";}
	        if (item.getDamage() == 94){return "Squid Egg";}
	        if (item.getDamage() == 95){return "Wolf Egg";}
	        if (item.getDamage() == 96){return "Mooshroom Egg";}
	        if (item.getDamage() == 98){return "Ocelot Egg";}
	        if (item.getDamage() == 120){return "Villager Egg";}
	    }
		return item.getType().toString();
	}
}