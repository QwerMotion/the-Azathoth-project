import heapq
from dataclasses import dataclass, field
from typing import Tuple, Dict, List, Optional

@dataclass(order=True)
class Node:
    f_cost: float
    pos: Tuple[int, int, int] = field(compare=False)
    parent: Optional['Node'] = field(compare=False, default=None)
    g_cost: float = field(compare=False, default=0.0)
    build_cost: int = field(compare=False, default=0)
    time_cost: int = field(compare=False, default=0)
    h_cost: float = field(compare=False, default=0.0)

@dataclass
class Path:
    positions: List[Tuple[int, int, int]]
    build_cost: int
    time_cost: int
    total_cost: float

class Pathfinder:
    def __init__(self, snapshot: Dict[str, str]):
        # Snapshot: "x,y,z" -> block_id
        self.world = {}
        for key, block in snapshot.items():
            x, y, z = map(int, key.split(','))
            self.world[(x, y, z)] = block

    def is_air(self, pos):
        return self.world.get(pos, "minecraft:air") == "minecraft:air"

    def is_solid(self, pos):
        return not self.is_air(pos)

    def get_neighbors(self, pos: Tuple[int,int,int]):
        x, y, z = pos
        neighbors = []
        # 6 Richtungen: N,S,E,W, Up, Down
        moves = [
            (1,0,0),(-1,0,0),(0,0,1),(0,0,-1),
            (0,1,0),(0,-1,0)
        ]
        for dx, dy, dz in moves:
            nx, ny, nz = x+dx, y+dy, z+dz
            above = (nx, ny+1, nz)
            below = (nx, ny-1, nz)

            # 1) Clearance: Über dir muss Luft sein
            if not self.is_air(above):
                continue

            # 2) Zwei Fälle:
            #   a) Die ZIELPOSITION ist Luft => normale Bewegung
            if self.is_air((nx,ny,nz)):
                build_c  = 0 if self.is_solid(below) else 1
                time_c   = 0
            #   b) Die ZIELPOSITION ist solide => ab- und dann bewegen
            else:
                # wir erlauben Abbau
                build_c  = 0                # kein Bauen
                time_c   = 1                # Abbau kostet 1
                # danach als Luft behandeln

            neighbors.append(((nx,ny,nz), build_c, time_c))
        return neighbors

    def heuristic(self, a, b):
        # Manhattan
        return abs(a[0]-b[0]) + abs(a[1]-b[1]) + abs(a[2]-b[2])

    def find_path(self, start, goal) -> Optional[Path]:
        open_set = []
        start_h = self.heuristic(start, goal)
        heapq.heappush(open_set, Node(f_cost=start_h, pos=start, g_cost=0.0, h_cost=start_h))
        cost_so_far = {start: 0.0}

        while open_set:
            current = heapq.heappop(open_set)
            if current.pos == goal:
                # Path rekonstruieren
                path = []
                node = current
                while node:
                    path.append(node.pos)
                    node = node.parent
                path.reverse()
                return Path(path, current.build_cost, current.time_cost, current.g_cost)

            for nbr_pos, build_c, time_c in self.get_neighbors(current.pos):
                move_cost = 1
                new_build = current.build_cost + build_c
                new_time  = current.time_cost + time_c
                new_g     = current.g_cost + move_cost + build_c + time_c

                if nbr_pos not in cost_so_far or new_g < cost_so_far[nbr_pos]:
                    cost_so_far[nbr_pos] = new_g
                    h = self.heuristic(nbr_pos, goal)
                    f = new_g + h
                    heapq.heappush(open_set, Node(
                        f_cost=f,
                        pos=nbr_pos,
                        parent=current,
                        g_cost=new_g,
                        build_cost=new_build,
                        time_cost=new_time,
                        h_cost=h
                    ))

        return None  # kein Pfad gefunden
