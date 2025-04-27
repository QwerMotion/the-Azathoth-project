package name.azathoth;

import net.fabricmc.api.ClientModInitializer;

import java.io.IOException;

public class AzathothClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        try {
            new SimpleHttpServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
