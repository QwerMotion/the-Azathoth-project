package name.azathoth;


import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.item.BlockItem;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;



public class SimpleHttpServer {
    private final HttpServer server;
    private final Gson gson = new Gson();

    public SimpleHttpServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/position", this::handlePosition);
        server.createContext("/inventory", this::handleInventory);
        server.createContext("/world_items", this::handleWorldItems);
        server.createContext("/world_snapshot", this::handleWorldSnapshot);
        server.createContext("/next_block", this::handleNextBlock);
        server.createContext("/entities", this::handleEntities);
        server.createContext("/looking_block", this::handleLookingBlock);
        server.createContext("/forward", this::handleForward);
        server.createContext("/look", this::handleLook);
        server.createContext("/jump",    this::handleJump);
        server.createContext("/place_block", this::handlePlaceBlock);
        server.createContext("/break_block", this::handleBreakBlock);
        server.createContext("/block_status", this::handleBlockStatus);
        server.createContext("/find_path",      this::handleFindPath);
        server.createContext("/visualize_path", this::handleVisualizePath);
        server.createContext("/set_velocity", this::handleSetVelocity);

        server.setExecutor(null);
        server.start();
        System.out.println("Simple HTTP Server läuft auf Port 8080");
    }

    private void handleSetVelocity(HttpExchange ex) throws IOException {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        Map<String,String> params = parseQuery(ex.getRequestURI());

        if (player == null) {
            sendError(ex, 500, "Spieler nicht verfügbar");
            return;
        }

        try {
            double vx = Double.parseDouble(params.getOrDefault("x", "0"));
            double vy = Double.parseDouble(params.getOrDefault("y", "0"));
            double vz = Double.parseDouble(params.getOrDefault("z", "0"));

            // Auf dem Client-Thread ausführen
            mc.execute(() -> {
                player.setVelocity(vx, vy, vz);
                // Stelle sicher, dass die Velocity übernommen wird
                player.velocityModified = true;
            });

            // Einfach eine Rückmeldung als JSON
            Map<String, Object> result = Map.of(
                    "velocity_x", vx,
                    "velocity_y", vy,
                    "velocity_z", vz
            );
            sendJson(ex, result);

        } catch (NumberFormatException nfe) {
            sendError(ex, 400, "Ungültige Zahl: " + nfe.getMessage());
        } catch (Exception e) {
            sendError(ex, 500, "Fehler beim Setzen der Velocity: " + e.getMessage());
        }
    }



    private void handleVisualizePath(HttpExchange ex) throws IOException {
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;
        Map<String,String> p = parseQuery(ex.getRequestURI());

        if (world == null) {
            sendError(ex, 500, "Welt nicht verfügbar");
            return;
        }

        try {
            int sx = Integer.parseInt(p.get("sx"));
            int sy = Integer.parseInt(p.get("sy"));
            int sz = Integer.parseInt(p.get("sz"));
            int gx = Integer.parseInt(p.get("gx"));
            int gy = Integer.parseInt(p.get("gy"));
            int gz = Integer.parseInt(p.get("gz"));
            int maxRadius = Integer.parseInt(p.getOrDefault("r", "64"));

            BlockPos start = new BlockPos(sx, sy, sz);
            BlockPos goal  = new BlockPos(gx, gy, gz);

            qwermotion.azathoth.Pathfinder.Path path = qwermotion.azathoth.Pathfinder.findPath(world, start, goal, maxRadius);
            if (path == null) {
                sendError(ex, 404, "Kein Pfad gefunden");
                return;
            }

            // Auf dem Client‑Thread Glasspanes setzen
            mc.execute(() -> {
                for (List<Integer> coord : path.positions()) {
                    BlockPos pos = new BlockPos(coord.get(0), coord.get(1), coord.get(2));
                    world.setBlockState(pos, Blocks.RED_WOOL.getDefaultState());
                }
            });

            // Rückgabe des Pfades als JSON (optional)
            sendJson(ex, path);

        } catch (Exception e) {
            sendError(ex, 400, "Ungültige Parameter: " + e.getMessage());
        }
    }








    private void handleFindPath(HttpExchange ex) throws IOException {
        var mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        World world = mc.world;
        if (player == null || world == null) {
            sendError(ex, 500, "Spieler oder Welt nicht verfügbar");
            return;
        }

        Map<String,String> p = parseQuery(ex.getRequestURI());
        try {
            int sx = Integer.parseInt(p.get("sx"));
            int sy = Integer.parseInt(p.get("sy"));
            int sz = Integer.parseInt(p.get("sz"));
            int gx = Integer.parseInt(p.get("gx"));
            int gy = Integer.parseInt(p.get("gy"));
            int gz = Integer.parseInt(p.get("gz"));
            int maxRadius = Integer.parseInt(p.getOrDefault("r", "64"));

            BlockPos start = new BlockPos(sx, sy, sz);
            BlockPos goal  = new BlockPos(gx, gy, gz);

            qwermotion.azathoth.Pathfinder.Path result = qwermotion.azathoth.Pathfinder.findPath(world, start, goal, maxRadius);
            if (result == null) {
                sendError(ex, 404, "Kein Pfad gefunden");
            } else {
                sendJson(ex, result);
            }
        } catch (Exception e) {
            sendError(ex, 400, "Ungültige Parameter: " + e.getMessage());
        }
    }
    private void handleBlockStatus(HttpExchange ex) throws IOException {
        var mc = MinecraftClient.getInstance();
        var world = mc.world;
        Map<String,String> params = parseQuery(ex.getRequestURI());

        // Parameter prüfen
        if (world == null) {
            sendError(ex, 500, "Welt nicht verfügbar");
            return;
        }
        try {
            int x = Integer.parseInt(params.get("x"));
            int y = Integer.parseInt(params.get("y"));
            int z = Integer.parseInt(params.get("z"));

            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(pos);
            String blockId = Registries.BLOCK.getId(state.getBlock()).toString();

            // Optional kannst du auch "air" abdecken:
            if (state.isAir()) {
                blockId = "minecraft:air";
            }

            // Antwort senden
            sendJson(ex, Map.of(
                    "x", x, "y", y, "z", z,
                    "block", blockId
            ));
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Ungültige Koordinaten");
        }
    }

    private void handlePlaceBlock(HttpExchange ex) throws IOException {
        var mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        var world = mc.world;
        if (player == null || world == null) {
            sendError(ex, 500, "Spieler oder Welt nicht verfügbar");
            return;
        }

        Map<String,String> params = parseQuery(ex.getRequestURI());
        try {
            int x = Integer.parseInt(params.get("x"));
            int y = Integer.parseInt(params.get("y"));
            int z = Integer.parseInt(params.get("z"));
            String blockId = params.get("block");
            if (blockId == null) throw new IllegalArgumentException("block fehlt");

            Identifier id = Identifier.tryParse(blockId);
            if (id == null) throw new IllegalArgumentException("Ungültige Block-ID");

            // Finde passenden BlockItem im Hotbar-Inventar
            int slot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack s = player.getInventory().getStack(i);
                if (!s.isEmpty() && s.getItem() instanceof BlockItem bi &&
                        Registries.BLOCK.getId(bi.getBlock()).toString().equals(blockId)) {
                    slot = i; break;
                }
            }
            if (slot < 0) {
                sendError(ex, 400, "BlockItem nicht im Hotbar gefunden");
                return;
            }

            BlockPos target = new BlockPos(x,y,z);
            // Schedule auf Main Thread
            int finalSlot = slot;
            mc.execute(() -> {
                player.getInventory().selectedSlot = finalSlot;
                // Erzeuge einen BlockHitResult an der Oberkante
                Vec3d hitVec = Vec3d.ofCenter(target);
                BlockHitResult bhr = new BlockHitResult(hitVec, Direction.UP, target, false);
                mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, bhr);
            });

            sendJson(ex, Map.of("placed", blockId, "at", List.of(x,y,z)));
        } catch (Exception e) {
            sendError(ex, 400, "Fehler: " + e.getMessage());
        }
    }

    // ====== Neuer Endpunkt: Block abbauen ======
    private void handleBreakBlock(HttpExchange ex) throws IOException {
        var mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        var world = mc.world;
        if (player == null || world == null) {
            sendError(ex, 500, "Spieler oder Welt nicht verfügbar");
            return;
        }

        Map<String, String> params = parseQuery(ex.getRequestURI());
        try {
            int x = Integer.parseInt(params.get("x"));
            int y = Integer.parseInt(params.get("y"));
            int z = Integer.parseInt(params.get("z"));
            BlockPos target = new BlockPos(x, y, z);

            // Werkzeug automatisch wählen
            mc.execute(() -> selectBestToolFor(target));

            // Starten und in Hintergrund fortlaufend progress senden
            new Thread(() -> {
                ClientPlayerInteractionManager im = mc.interactionManager;
                Direction dir = Direction.UP;
                // Block-Abbau starten
                mc.execute(() -> im.attackBlock(target, dir));
                // Solange nicht weg, weiter progress senden
                while (world.getBlockState(target).getBlock() != Blocks.AIR) {
                    mc.execute(() -> im.updateBlockBreakingProgress(target, dir));
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                }
                // Abbau beendet
            }, "BreakThread").start();

            sendJson(ex, Map.of("breaking", List.of(x, y, z)));
        } catch (Exception e) {
            sendError(ex, 400, "Fehler: " + e.getMessage());
        }
    }

    /**
     * Wählt automatisch das beste Werkzeug aus, um einen Block schneller abzubauen.
     */
    private void selectBestToolFor(BlockPos pos) {
        var mc = MinecraftClient.getInstance();
        var player = mc.player;
        var world = mc.world;
        if (player == null || world == null) return;

        var blockState = world.getBlockState(pos);

        float bestSpeed = 0f;
        int bestSlot = -1;

        for (int i = 0; i < 9; i++) { // Nur Hotbar-Slots durchsuchen (0-8)
            var stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            float speed = stack.getMiningSpeedMultiplier(blockState);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        if (bestSlot != -1 && player.getInventory().selectedSlot != bestSlot) {
            player.getInventory().selectedSlot = bestSlot;
        }
    }


    // ====== Neuer Endpunkt: Block abbauen ======
    private void handleBreakBlock_olf(HttpExchange ex) throws IOException {
        var mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        var world = mc.world;
        if (player == null || world == null) {
            sendError(ex, 500, "Spieler oder Welt nicht verfügbar");
            return;
        }

        Map<String,String> params = parseQuery(ex.getRequestURI());
        try {
            int x = Integer.parseInt(params.get("x"));
            int y = Integer.parseInt(params.get("y"));
            int z = Integer.parseInt(params.get("z"));
            BlockPos target = new BlockPos(x,y,z);

            // Starten und in Hintergrund fortlaufend progress senden
            new Thread(() -> {
                ClientPlayerInteractionManager im = mc.interactionManager;
                Direction dir = Direction.UP;
                // Block-Abbau starten
                mc.execute(() -> im.attackBlock(target, dir));
                // Solange nicht weg, weiter progress senden
                while (world.getBlockState(target).getBlock() != Blocks.AIR) {
                    mc.execute(() -> im.updateBlockBreakingProgress(target, dir));
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                }
                // Abbau beendet
            }, "BreakThread").start();

            sendJson(ex, Map.of("breaking", List.of(x,y,z)));
        } catch (Exception e) {
            sendError(ex, 400, "Fehler: " + e.getMessage());
        }
    }

    private void handleJump(HttpExchange ex) throws IOException {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            sendError(ex, 500, "Spieler nicht verfügbar");
            return;
        }

        Map<String, String> params = parseQuery(ex.getRequestURI());
        String pressedParam = params.get("pressed");
        if (pressedParam == null) {
            sendError(ex, 400, "Parameter 'pressed' fehlt (true|false)");
            return;
        }

        boolean pressed = Boolean.parseBoolean(pressedParam);
        MinecraftClient.getInstance().options.jumpKey.setPressed(pressed);

        sendJson(ex, Map.of("jumpPressed", pressed));
    }

    private void handleForward(HttpExchange ex) throws IOException {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            sendError(ex, 500, "Spieler nicht verfügbar");
            return;
        }

        Map<String, String> params = parseQuery(ex.getRequestURI());
        String pressedParam = params.get("pressed");
        if (pressedParam == null) {
            sendError(ex, 400, "Parameter 'pressed' fehlt (true|false)");
            return;
        }

        boolean pressed = Boolean.parseBoolean(pressedParam);
        MinecraftClient.getInstance().options.forwardKey.setPressed(pressed);

        sendJson(ex, Map.of("forwardPressed", pressed));
    }

    private void handleLook(HttpExchange ex) throws IOException {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            sendError(ex, 500, "Spieler nicht verfügbar");
            return;
        }

        Map<String, String> params = parseQuery(ex.getRequestURI());
        String yawParam   = params.get("yaw");
        String pitchParam = params.get("pitch");

        if (yawParam == null || pitchParam == null) {
            sendError(ex, 400, "Parameter 'yaw' und 'pitch' erforderlich");
            return;
        }

        try {
            float yaw   = Float.parseFloat(yawParam);
            float pitch = Float.parseFloat(pitchParam);
            player.setYaw(yaw);
            player.setPitch(pitch);
            sendJson(ex, Map.of("yaw", yaw, "pitch", pitch));
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Ungültiges Zahlenformat für yaw oder pitch");
        }
    }



    private void handleLookingBlock(HttpExchange ex) throws IOException {
        var mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) {
            sendError(ex, 500, "Spieler oder Welt nicht verfügbar");
            return;
        }

        // Raycast in Blickrichtung (5 Blöcke Reichweite)
        HitResult hit = player.raycast(5.0, 1.0f, false);
        if (!(hit instanceof BlockHitResult)) {
            sendJson(ex, Map.of("error", "Kein Block im Sichtfeld"));
            return;
        }

        BlockHitResult bhr = (BlockHitResult) hit;
        BlockPos pos = bhr.getBlockPos();
        String blockId = Registries.BLOCK.getId(mc.world.getBlockState(pos).getBlock()).toString();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("x", pos.getX());
        out.put("y", pos.getY());
        out.put("z", pos.getZ());
        out.put("block", blockId);

        sendJson(ex, out);
    }
    private void handlePosition(HttpExchange ex) throws IOException {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            sendError(ex, 500, "Spieler nicht verfügbar");
            return;
        }

        Vec3d pos = player.getPos();
        Vec3d rot = new Vec3d(player.getYaw(), player.getPitch(), 0);
        Map<String, Object> out = Map.of(
                "x", pos.x, "y", pos.y, "z", pos.z,
                "look_x", rot.x, "look_y", rot.y, "look_z", rot.z
        );
        sendJson(ex, out);
    }

    private void handleInventoryOld(HttpExchange ex) throws IOException {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            sendError(ex, 500, "Spieler nicht verfügbar");
            return;
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (ItemStack stack : player.getInventory().main) {
            if (!stack.isEmpty()) {
                items.add(Map.of(
                        "item", Registries.ITEM.getId(stack.getItem()).toString(),
                        "count", stack.getCount()
                ));
            }
        }
        sendJson(ex, items);
    }

    private void handleInventory(HttpExchange ex) throws IOException {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            sendError(ex, 500, "Spieler nicht verfügbar");
            return;
        }

        var mainInv = player.getInventory().main; // DefaultedList<ItemStack>
        List<Map<String, Object>> slots = new ArrayList<>();

        for (int slot = 0; slot < mainInv.size(); slot++) {
            ItemStack stack = mainInv.get(slot);
            Map<String, Object> entry = new HashMap<>();
            entry.put("slot", slot);
            if (stack.isEmpty()) {
                entry.put("item", null);
                entry.put("count", 0);
            } else {
                entry.put("item", Registries.ITEM.getId(stack.getItem()).toString());
                entry.put("count", stack.getCount());
            }
            slots.add(entry);
        }

        sendJson(ex, slots);
    }


    private void handleWorldItems(HttpExchange ex) throws IOException {
        var mc = MinecraftClient.getInstance();
        var player = mc.player;
        var world = mc.world;
        if (player == null || world == null) {
            sendError(ex, 500, "Spieler oder Welt nicht verfügbar");
            return;
        }

        List<Map<String, Object>> drops = new ArrayList<>();
        Box box = player.getBoundingBox().expand(32);
        world.getEntitiesByClass(ItemEntity.class, box, e -> true)
                .forEach(entity -> {
                    var stack = entity.getStack();
                    drops.add(Map.of(
                            "x", entity.getX(),
                            "y", entity.getY(),
                            "z", entity.getZ(),
                            "item", Registries.ITEM.getId(stack.getItem()).toString(),
                            "count", stack.getCount()
                    ));
                });
        sendJson(ex, drops);
    }

    private void handleWorldSnapshot(HttpExchange ex) throws IOException {
        var mc = MinecraftClient.getInstance();
        var player = mc.player;
        var world = mc.world;
        if (player == null || world == null) {
            sendError(ex, 500, "Spieler oder Welt nicht verfügbar");
            return;
        }

        Map<String, String> params = parseQuery(ex.getRequestURI());
        int radius = parseInt(params.get("r"), 5);

        BlockPos center = player.getBlockPos();
        Map<String, String> blocks = new HashMap<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos p = center.add(dx, dy, dz);
                    BlockState state = world.getBlockState(p);
                    String id = Registries.BLOCK.getId(state.getBlock()).toString();
                    blocks.put(p.getX()+","+p.getY()+","+p.getZ(), id);
                }
            }
        }
        sendJson(ex, blocks);
    }

    private void handleNextBlock(HttpExchange ex) throws IOException {
        var mc = MinecraftClient.getInstance();
        var player = mc.player;
        var world = mc.world;
        if (player == null || world == null) {
            sendError(ex, 500, "Spieler oder Welt nicht verfügbar");
            return;
        }

        Map<String, String> params = parseQuery(ex.getRequestURI());
        String blockParam = params.get("block");
        if (blockParam == null) {
            sendError(ex, 400, "Parameter 'block' fehlt");
            return;
        }
        Identifier id = Identifier.tryParse(blockParam);
        if (id == null) {
            sendError(ex, 400, "Ungültige Block-ID");
            return;
        }
        Block target = Registries.BLOCK.get(id);
        if (target == null) {
            sendError(ex, 400, "Block nicht gefunden");
            return;
        }
        int maxR = parseInt(params.get("r"), 64);
        BlockPos origin = player.getBlockPos();

        for (int r = 1; r <= maxR; r++) {
            for (BlockPos pos : getShellPositions(origin, r)) {
                if (world.getBlockState(pos).getBlock().equals(target)) {
                    sendJson(ex, Map.of(
                            "x", pos.getX(), "y", pos.getY(), "z", pos.getZ()
                    ));
                    return;
                }
            }
        }
        sendJson(ex, Map.of("error", "Kein "+ blockParam +" im Radius "+ maxR));
    }

    private void handleEntities(HttpExchange ex) throws IOException {
        var mc = MinecraftClient.getInstance();
        var player = mc.player;
        var world = mc.world;
        if (player == null || world == null) {
            sendError(ex, 500, "Spieler oder Welt nicht verfügbar");
            return;
        }

        Map<String, String> params = parseQuery(ex.getRequestURI());
        int radius = parseInt(params.get("r"), 32);
        Box box = player.getBoundingBox().expand(radius);

        List<Map<String, Object>> list = new ArrayList<>();
        world.getEntitiesByClass(MobEntity.class, box, e -> true)
                .forEach(e -> addEntity(list, e));
        world.getEntitiesByClass(AnimalEntity.class, box, e -> true)
                .forEach(e -> addEntity(list, e));

        sendJson(ex, list);
    }

    // Hilfsmethode zum Parsen der Query-Parameter
    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> map = new HashMap<>();
        String query = uri.getRawQuery();
        if (query == null) return map;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length==2) map.put(kv[0], kv[1]);
        }
        return map;
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { return def; }
    }

    private void sendError(HttpExchange ex, int code, String msg) throws IOException {
        byte[] data = msg.getBytes();
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }

    private void sendJson(HttpExchange ex, Object obj) throws IOException {
        String json = gson.toJson(obj);
        byte[] data = json.getBytes();
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }

    private void addEntity(List<Map<String,Object>> list, net.minecraft.entity.Entity e) {
        var pos = e.getPos();
        list.add(Map.of(
                "entity", e.getType().getRegistryEntry().registryKey().getValue().toString(),
                "x", pos.x, "y", pos.y, "z", pos.z
        ));
    }

    private List<BlockPos> getShellPositions(BlockPos center, int radius) {
        List<BlockPos> out = new ArrayList<>();
        int r = radius;
        for (int x=-r; x<=r; x++) for (int y=-r; y<=r; y++) for (int z=-r; z<=r; z++) {
            if (Math.abs(x)==r || Math.abs(y)==r || Math.abs(z)==r) {
                out.add(center.add(x,y,z));
            }
        }
        return out;
    }
}
