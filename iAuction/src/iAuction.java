import java.io.File;
import java.util.logging.Logger;


public class iAuction extends Plugin{
	
	public String name = "iAuction";
	public String author = "Spenk";
	public String version = "1.0";
	public Logger log = Logger.getLogger("Minecraft");
	public iAuctionListener listener = new iAuctionListener();
	
	public void enable(){
		log.info(name+" version "+version+" by "+author+ " is Enabled!");
	}
	
	public void disable(){
		log.info(name +" version "+version+" by "+author+ " is Disabled!");
	}
	
	public void initialize(){
		log.info(name+" version "+version+" by "+author+ " is Initialized!");
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
		File f = new File("plugins/config");
		f.mkdir();
		listener.loadprops();
		try {
			listener.tolist();
		} catch (Exception e) {
		}
	}

}
