package qwermotion.azathoth;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.util.*;

/**
 * A* Pathfinder mit realistischen Kosten, 14 Bewegungsrichtungen,
 * Radius-Begrenzung und speziellen Regeln für das Ziel:
 * - Alle nicht-kollidierenden Blöcke (z.B. hohe Gräser) werden wie Luft behandelt.
 * - Wasser/Lava werden nicht betreten.
 * - Luftbewegung hat hohe Baukosten, um „Fliegen“ zu verhindern.
 * - Kopf-Freiheit entfällt komplett: Blöcke über dem Wegpunkt werden als Abbaukosten eingerechnet.
 * - Suche stoppt, wenn kein Pfad im Radius gefunden oder Max-Iteration überschritten.
 */
public class Pathfinder {
    public record Path(List<List<Integer>> positions, int buildCost, int timeCost, double totalCost) {}

    private static class Node implements Comparable<Node> {
        final BlockPos pos;
        final Node parent;
        final double g, f;
        final int build, time;
        Node(BlockPos pos, Node parent, double g, double h, int build, int time) {
            this.pos = pos;
            this.parent = parent;
            this.g = g; this.f = g + h;
            this.build = build; this.time = time;
        }
        @Override
        public int compareTo(Node o) { return Double.compare(this.f, o.f); }
    }

    public static Path findPath(World world, BlockPos start, BlockPos goal, int maxRadius) {
        PriorityQueue<Node> open = new PriorityQueue<>();
        Map<BlockPos, Double> costSoFar = new HashMap<>();
        open.add(new Node(start, null, 0, heuristic(start, goal), 0, 0));
        costSoFar.put(start, 0.0);

        int iterations = 0, MAX_ITER = 10_000_000;
        while (!open.isEmpty() && iterations++ < MAX_ITER) {
            Node cur = open.poll();
            if (cur.pos.equals(goal)) {
                List<List<Integer>> path = new ArrayList<>();
                for (Node n = cur; n != null; n = n.parent)
                    path.add(Arrays.asList(n.pos.getX(), n.pos.getY(), n.pos.getZ()));
                Collections.reverse(path);
                return new Path(path, cur.build, cur.time, cur.g);
            }
            for (Step step : neighbors(world, start, goal, cur.pos, maxRadius)) {
                BlockPos np = step.pos();
                double ng = cur.g + step.cost();
                if (!costSoFar.containsKey(np) || ng < costSoFar.get(np)) {
                    costSoFar.put(np, ng);
                    open.add(new Node(np, cur, ng, heuristic(np, goal),
                            cur.build + step.build(), cur.time + step.time()));
                }
            }
        }
        return null; // kein Pfad
    }

    private record Step(BlockPos pos, int build, int time) {
        double cost() { return 1 + build + time; }
    }

    private static List<Step> neighbors(World w, BlockPos start, BlockPos goal, BlockPos p, int maxRadius) {
        List<Step> list = new ArrayList<>();
        int[][] dirs = {{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}};
        for (int[] d : dirs) {
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos np = p.add(d[0], dy, d[2]);
                if (withinRadius(start, np, maxRadius)) addStepIfValid(w, np, goal, list);
            }
        }
        for (BlockPos np : List.of(p.up(), p.down())) {
            if (withinRadius(start, np, maxRadius)) addStepIfValid(w, np, goal, list);
        }
        return list;
    }

    private static boolean withinRadius(BlockPos start, BlockPos np, int maxRadius) {
        int dx = Math.abs(np.getX() - start.getX());
        int dy = Math.abs(np.getY() - start.getY());
        int dz = Math.abs(np.getZ() - start.getZ());
        return dx + dy + dz <= maxRadius;
    }

    private static void addStepIfValid(World w, BlockPos np, BlockPos goal, List<Step> list) {
        BlockState bs        = w.getBlockState(np);
        BlockPos above       = np.up();
        BlockState aboveState= w.getBlockState(above);

        // 1) Fluss- und Lavablöcke nie betreten
        if (!bs.getFluidState().isEmpty()) return;

        // 2) Wenn ein Block über np steht, muss er zuerst abgebaut werden:
        int timeAbove = 0;
        if (!np.equals(goal) && !aboveState.isAir()) {
            float hard = aboveState.getHardness(w, above);
            timeAbove = Math.max(1, Math.round(hard));
        }

        // 3) Kollisions‐Form abfragen
        VoxelShape shape = bs.getCollisionShape(w, np);
        boolean passable = bs.isAir() || shape.isEmpty();

        if (passable) {
            // Luft/Raum: Baukosten, wenn kein Boden
            BlockState below = w.getBlockState(np.down());
            boolean solidBelow = below.isOpaque() && below.getFluidState().isEmpty();
            int buildCost = solidBelow ? 0 : 40;
            list.add(new Step(np, buildCost, timeAbove));
        } else {
            // Solider Block: Abbaukosten plus timeAbove
            float hard = bs.getHardness(w, np);
            int t = Math.max(4, Math.round(hard));
            list.add(new Step(np, 0, t + timeAbove));
        }
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX())
                + Math.abs(a.getY() - b.getY())
                + Math.abs(a.getZ() - b.getZ());
    }
}
