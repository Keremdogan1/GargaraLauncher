import org.to2mbn.jmccc.option.LaunchOption;
import org.to2mbn.jmccc.option.ServerInfo;
import org.to2mbn.jmccc.auth.OfflineAuthenticator;
import org.to2mbn.jmccc.option.MinecraftDirectory;

public class TestLaunchOption {
    public static void main(String[] args) throws Exception {
        LaunchOption option = new LaunchOption("test", new OfflineAuthenticator("test"), new MinecraftDirectory("test"));
        option.setServerInfo(new ServerInfo("130.61.80.148", 25565));
        System.out.println("ServerInfo works!");
    }
}
