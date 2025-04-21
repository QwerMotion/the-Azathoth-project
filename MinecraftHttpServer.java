package qwermotion.azathoth;



import fi.iki.elonen.NanoHTTPD;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.*;

public class MinecraftHttpServer extends NanoHTTPD {

    public MinecraftHttpServer() throws IOException {
        super(8080);
        start(SOCKET_READ_TIMEOUT, false);
        System.out.println("HTTP Server läuft auf Port 8080");
    }

    @Override
    public Response serve(IHTTPSession session) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        World world = mc.world;

        if (player == null || world == null) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Spieler oder Welt nicht bereit");
        }

        String uri = session.getUri();
        Map<String, String> params = session.getParms();

        switch (uri) {
            case "/position":
                return handlePosition(player);
            case "/inventory":
                return handleInventory(player);
            case "/world_items":
                return handleWorldItems(world, player);
            case "/world_snapshot":
                return handleWorldSnapshot(world, player, params);
            case "/next_block":
                return handleNextBlock(world, player, params);
            case "/looking_block":
                return handleLookingBlock(mc, player);
            case "/entities":
                return handleEntities(world, player, params);
            default:
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found");
        }
    }

    private Response handlePosition(ClientPlayerEntity player) {
        Vec3d pos = player.getPos();
        Vec3d rot = new Vec3d(player.getYaw(), player.getPitch(), 0);
        Map<String, Object> out = Map.of(
                "x", pos.x,
                "y", pos.y,
                "z", pos.z,
                "look_x", rot.x,
                "look_y", rot.y,
                "look_z", rot.z
        );
        return jsonResponse(out);
    }

    private Response handleInventory(ClientPlayerEntity player) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (ItemStack stack : player.getInventory().main) {
            if (!stack.isEmpty()) {
                items.add(Map.of(
                        "item", Registries.ITEM.getId(stack.getItem()).toString(),
                        "count", stack.getCount()
                ));
            }
        }
        return jsonResponse(items);
    }

    private Response handleWorldItems(World world, ClientPlayerEntity player) {
        List<Map<String, Object>> drops = new ArrayList<>();
        world.getEntitiesByClass(ItemEntity.class, player.getBoundingBox().expand(32), e -> true)
                .forEach(entity -> {
                    ItemStack stack = entity.getStack();
                    drops.add(Map.of(
                            "x", entity.getX(),
                            "y", entity.getY(),
                            "z", entity.getZ(),
                            "item", Registries.ITEM.getId(stack.getItem()).toString(),
                            "count", stack.getCount()
                    ));
                });
        return jsonResponse(drops);
    }

    private Response handleWorldSnapshot(World world, ClientPlayerEntity player, Map<String, String> params) {
        int radius = 5;
        try {
            radius = Integer.parseInt(params.getOrDefault("r", "5"));
        } catch (NumberFormatException ignored) {}

        BlockPos center = player.getBlockPos();
        Map<String, String> blocks = new HashMap<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos p = center.add(dx, dy, dz);
                    BlockState state = world.getBlockState(p);
                    String id = Registries.BLOCK.getId(state.getBlock()).toString();
                    blocks.put(p.getX() + "," + p.getY() + "," + p.getZ(), id);
                }
            }
        }
        return jsonResponse(blocks);
    }

    private Response handleNextBlock(World world, ClientPlayerEntity player, Map<String, String> params) {
        String blockParam = params.get("block");
        if (blockParam == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Parameter 'block' fehlt");
        }
        Identifier id = Identifier.tryParse(blockParam);
        if (id == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Ungültige Block-ID");
        }
        Block targetBlock = Registries.BLOCK.get(id);
        if (targetBlock == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Block nicht gefunden");
        }
        int maxRadius = 64;
        try {
            maxRadius = Integer.parseInt(params.getOrDefault("r", "64"));
        } catch (NumberFormatException ignored) {}

        BlockPos origin = player.getBlockPos();
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (BlockPos pos : getShellPositions(origin, radius)) {
                if (world.getBlockState(pos).getBlock().equals(targetBlock)) {
                    return jsonResponse(Map.of(
                            "x", pos.getX(),
                            "y", pos.getY(),
                            "z", pos.getZ()
                    ));
                }
            }
        }
        return jsonResponse(Map.of("error", "Kein Block vom Typ " + blockParam + " gefunden im Radius " + maxRadius));
    }

    private Response handleLookingBlock(MinecraftClient mc, ClientPlayerEntity player) {
        HitResult hit = player.raycast(5.0, 1.0f, false);
        if (!(hit instanceof BlockHitResult)) {
            return jsonResponse(Map.of("error", "Kein Block im Blickfeld"));
        }
        BlockHitResult bhr = (BlockHitResult) hit;
        BlockPos pos = bhr.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        Map<String, Object> out = new HashMap<>();
        out.put("x", pos.getX());
        out.put("y", pos.getY());
        out.put("z", pos.getZ());
        out.put("block", Registries.BLOCK.getId(state.getBlock()).toString());
        out.put("break_progress", "n/a");
        return jsonResponse(out);
    }

    private Response handleEntities(World world, ClientPlayerEntity player, Map<String, String> params) {
        int radius = 32;
        try {
            radius = Integer.parseInt(params.getOrDefault("r", "32"));
        } catch (NumberFormatException ignored) {}

        Vec3d pos = player.getPos();
        List<Map<String, Object>> list = new ArrayList<>();
        // Mobs
        world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(radius), e -> true)
                .forEach(e -> addEntityInfo(list, e, pos));
        // Tiere
        world.getEntitiesByClass(AnimalEntity.class, player.getBoundingBox().expand(radius), e -> true)
                .forEach(e -> addEntityInfo(list, e, pos));

        return jsonResponse(list);
    }

    private void addEntityInfo(List<Map<String, Object>> list, Entity e, Vec3d origin) {
        Vec3d ep = e.getPos();
        list.add(Map.of(
                "entity", e.getType().getRegistryEntry().registryKey().getValue().toString(),
                "x", ep.x,
                "y", ep.y,
                "z", ep.z
        ));
    }

    private Iterable<BlockPos> getShellPositions(BlockPos center, int radius) {
        List<BlockPos> positions = new ArrayList<>();
        int r = radius;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.abs(x) == r || Math.abs(y) == r || Math.abs(z) == r) {
                        positions.add(center.add(x, y, z));
                    }
                }
            }
        }
        return positions;
    }

    private Response jsonResponse(Object obj) {
        String json = new com.google.gson.Gson().toJson(obj);
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }
}
