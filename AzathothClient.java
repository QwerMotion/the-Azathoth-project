package qwermotion.azathoth;

import net.fabricmc.api.ClientModInitializer;

import java.io.IOException;

public class AzathothClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("Client wurde initialisiert!");
        try {
            new SimpleHttpServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
