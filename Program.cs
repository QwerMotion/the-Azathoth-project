using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace AzathothClient
{
    public class Azathoth
    {
        private readonly HttpClient _http = new HttpClient();
        private readonly string _base;

        public Azathoth(string baseUrl = "http://localhost:8080")
        {
            _base = baseUrl.TrimEnd('/');
        }

        // --- Hilfstypen ---
        public record Vec3(double X, double Y, double Z, double LookX, double LookY);
        public record Vec3Int(int X, int Y, int Z);

        // --- Endpoints ---

        private Vec3 GetPlayerState()
        {
            var resp = _http.GetStringAsync($"{_base}/position").Result;
            using var doc = JsonDocument.Parse(resp);
            var e = doc.RootElement;
            return new Vec3(
                e.GetProperty("x").GetDouble(),
                e.GetProperty("y").GetDouble(),
                e.GetProperty("z").GetDouble(),
                e.GetProperty("look_x").GetDouble(),
                e.GetProperty("look_y").GetDouble()
            );
        }

        private string GetBlockStatus(Vec3Int p)
        {
            var r = _http.GetStringAsync($"{_base}/block_status?x={p.X}&y={p.Y}&z={p.Z}").Result;
            using var doc = JsonDocument.Parse(r);
            return doc.RootElement.GetProperty("block").GetString();
        }

        public Vec3Int GetNextBlock(string blockname, int r)
        {
            // ACHTUNG: .Result blockiert den Thread. Besser wäre es, die Funktion selbst async zu machen
            // und await zu verwenden, z.B. "public async Task<Vec3Int> GetNextBlockAsync(...)"
            // Für dieses Beispiel behalten wir .Result bei, um nah am Original zu bleiben.
            var nextBlock = _http.GetStringAsync($"{_base}/next_block?block={blockname}&r={r}").Result;

            using var doc = JsonDocument.Parse(nextBlock);
            var e = doc.RootElement;

            // Korrektur: GetInt32() statt GetInt64 und die fehlenden Klammern ()
            int x = e.GetProperty("x").GetInt32();
            int y = e.GetProperty("y").GetInt32();
            int z = e.GetProperty("z").GetInt32();

            // Angenommen, Vec3Int hat einen Konstruktor, der 3 int Werte nimmt.
            return new Vec3Int(x, y, z);

            // Alternative, falls Vec3Int nur long nimmt oder du explizit long brauchst:
            // long x = e.GetProperty("x").GetInt64();
            // long y = e.GetProperty("y").GetInt64();
            // long z = e.GetProperty("z").GetInt64();
            // return new Vec3Int(x, y, z);
        }

        public void DestroyBlock_old(Vec3Int p)
        {
            //Console.WriteLine("destroying Block! " + p.ToString());
            _http.GetAsync($"{_base}/break_block?x={p.X}&y={p.Y}&z={p.Z}").Wait();
            while (GetBlockStatus(p) != "minecraft:air") Thread.Sleep(50);
        }

        public void DestroyBlock(Vec3Int p)
        {
            // 1) Blick auf die Block-Mitte setzen
            var state = GetPlayerState();
            double bx = p.X + 0.5;
            double by = p.Y + 0.5;
            double bz = p.Z + 0.5;

            double dx = bx - state.X;
            double dy = by - (state.Y + 1.62);
            double dz = bz - state.Z;

            // yaw  = atan2(-dx, dz)
            double yaw = Math.Atan2(-dx, dz) * 180.0 / Math.PI;
            // pitch = -atan2(dy, hypot(dx,dz))
            double pitch = -Math.Atan2(dy, Math.Sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI;

            //Console.WriteLine($"  ▶ Looking at block center yaw={yaw:F1}, pitch={pitch:F1}");
            // falls du eine Helper-Methode hast:
           
            // oder direkt:
            _http.GetAsync($"{_base}/look?yaw={yaw}&pitch={pitch}".Replace(',', '.')).Wait();

            // kurz warten, damit die Drehung ankommt
            //Thread.Sleep(50);

            // 2) Block abbauen
            //Console.WriteLine($"  ▶ Breaking block at {p}");
            _http.GetAsync($"{_base}/break_block?x={p.X}&y={p.Y}&z={p.Z}").Wait();

            // 3) Warten bis Luft
            while (GetBlockStatus(p) != "minecraft:air")
            {
                Thread.Sleep(50);
            }

            //Console.WriteLine("  ✔ Block destroyed");
        }


        public void PlaceBlock(Vec3Int p, string block)
        {
            
            //Console.WriteLine("placing Block! " + p.ToString());
            _http.GetAsync($"{_base}/place_block?x={p.X}&y={p.Y}&z={p.Z}&block=minecraft:dirt".Replace(',', '.')).Wait();
            while (GetBlockStatus(p) != block) {
                Thread.Sleep(50);
            } 
        }

        public bool timedPlaceBlock(Vec3Int p, string block)
        {
            var task = Task.Run(() => PlaceBlock(p, block));
            if (task.Wait(TimeSpan.FromSeconds(5)))
                return true;
            else
                Console.WriteLine("placing timed out!");
                return false;
        }

        public bool timedBreakBlock(Vec3Int p)
        {
            var task = Task.Run(() => DestroyBlock(p));
            if (task.Wait(TimeSpan.FromSeconds(10)))
                return true;
            else
                Console.WriteLine("breaking timed out!");
            return false;
        }

        /// <summary>
        /// Setzt die Velocity des Spielers über deinen neuen HTTP‐Endpoint.
        /// </summary>
        public void SetVelocity(double vx, double vy, double vz)
        {
            
            
           
            
                //Console.WriteLine("setting velocity to zero");
                var uri = $"{_base}/set_velocity?x={vx}&y={vy}&z={vz}";
                uri = uri.Replace(',', '.');
                _http.GetAsync(uri).Wait();

            

            







            //Console.WriteLine($"  ▶ SetVelocity x={vx:F2}, y={vy:F2}, z={vz:F2}");
        }

        public List<Vec3Int> GetPathTo(Vec3Int goal, int radius = 128)
        {
            Console.WriteLine("getting Path...");
            var st = GetPlayerState();
            var p = new Dictionary<string, string>
            {
                ["sx"] = ((int)st.X).ToString(),
                ["sy"] = ((int)st.Y).ToString(),
                ["sz"] = ((int)st.Z).ToString(),
                ["gx"] = goal.X.ToString(),
                ["gy"] = goal.Y.ToString(),
                ["gz"] = goal.Z.ToString(),
                ["r"] = radius.ToString()
            };
            var uri = $"{_base}/find_path?{ToQuery(p)}";
            var resp = _http.GetStringAsync(uri).Result;
            using var doc = JsonDocument.Parse(resp);
            var arr = doc.RootElement.GetProperty("positions");
            var path = new List<Vec3Int>();
            foreach (var el in arr.EnumerateArray())
                path.Add(new Vec3Int(el[0].GetInt32(), el[1].GetInt32(), el[2].GetInt32()));
            Console.WriteLine("got Path!");
            return path;
        }

        private static string ToQuery(Dictionary<string, string> p)
        {
            var list = new List<string>();
            foreach (var kv in p) list.Add($"{kv.Key}={Uri.EscapeDataString(kv.Value)}");
            return string.Join("&", list);
        }

        public void Goto_old(List<Vec3Int> path)
        {
            foreach (var target in path)
            {
                Console.WriteLine($"\n>> Moving to block [{target.X}, {target.Y}, {target.Z}]");
                MoveToBlockCenter(target);
            }
            Console.WriteLine("==> All targets reached!");
        }

        public bool Goto(List<Vec3Int> path)
        {
            bool finished = false;
            Console.WriteLine($"Goto: received path with {path.Count} steps");
            
            var now = DateTime.UtcNow;

            while (path.Count > 0)
            {
                // Nächstes Ziel
                var target = path[0];

                // Aktuelle Block-Koordinate
                var state = GetPlayerState();
                var curBlock = new Vec3Int(
                    (int)Math.Floor(state.X),
                    (int)Math.Floor(state.Y),
                    (int)Math.Floor(state.Z)
                );

                // 1) Prüfen, ob target ein erlaubter Nachbar ist
                if (!IsNeighbor(curBlock, target))
                {
                    Console.WriteLine($"  ▶ Target {target} is NOT neighbor of {curBlock}, recalculating path...");
                    path = GetPathTo(target);
                    Console.WriteLine($"  ▶ New path has {path.Count} steps");
                    continue;  // Schleife mit neuem path neu starten
                }

                // 2) Ist korrekt benachbart → Bewegung ausführen
                //Console.WriteLine($"\n>> Moving to block [{target.X}, {target.Y}, {target.Z}]");
                MoveToBlockCenter(target);

                // 3) Schritt geschafft, aus Liste entfernen
                path.RemoveAt(0);

                if ((DateTime.UtcNow - now).TotalSeconds > Math.Max(30, path.Count))
                {
                    Console.WriteLine("goto timed out...");
                    return finished;
                }

            }
            finished = true;


            Console.WriteLine("==> All targets reached!");
            return finished;
        }

        private void MoveToBlockCenter_old(Vec3Int target)
        {
            // Mitte des Zielblocks
            double tx = target.X + 0.5, ty = target.Y, tz = target.Z + 0.5;
            const double tolH = 0.2, tolV = 0.2;
            var overallStart = DateTime.UtcNow;

            // 1) Hindernisse räumen & Boden legen
            var head = new Vec3Int(target.X, target.Y + 1, target.Z);
            if (GetBlockStatus(head) != "minecraft:air") DestroyBlock(head);
            var head2 = new Vec3Int(target.X, target.Y + 2, target.Z);
            if (GetBlockStatus(head2) != "minecraft:air") DestroyBlock(head2);
            
            var blk = new Vec3Int(target.X, target.Y, target.Z);
            if (GetBlockStatus(blk) != "minecraft:air") DestroyBlock(blk);
            var below = new Vec3Int(target.X, target.Y - 1, target.Z);
            if (GetBlockStatus(below) == "minecraft:air") PlaceBlock(below, "minecraft:dirt");

            // Aktuelle Y-Differenz prüfen
            var s0 = GetPlayerState();
            double cy0 = s0.Y;
            double dy0 = ty - cy0;

            // Entscheide Reihenfolge
            if (dy0 < -tolV)
            {
                //Console.WriteLine("  ↓ Downhill → Horizontal first, then Vertical");
                // a) Horizontal-Phase
                HorizontalPhase(tx, tz, tolH, overallStart);

                // b) Vertikal-Phase (Runter)
                VerticalPhase(ty, tolV, 5);
            }
            else
            {
                //Console.WriteLine("  ↑ Uphill/Level → Vertical first, then Horizontal");
                // a) Vertical-Phase (Hoch oder Level)
                VerticalPhase(ty, tolV, 5);

                // b) Horizontal-Phase
                HorizontalPhase(tx, tz, tolH, overallStart);
            }

            // 4) Endgültiger Stopp & Ausrichtung
            //Console.WriteLine("  ▶ Final stop");
            SetVelocity(0, 0, 0);

            // finale Blickkorrektur
            var end = GetPlayerState();
            double ex = end.X, ey = end.Y, ez = end.Z;
            double edx = tx - ex, edy = ty - ey, edz = tz - ez;
            //double yaw = Math.Atan2(-edx, edz) * 180.0 / Math.PI;
            //double pitch = -Math.Atan2(edy, Math.Sqrt(edx * edx + edz * edz)) * 180.0 / Math.PI;
            //_http.GetAsync($"{_base}/look?yaw={yaw}&pitch={pitch}").Wait();
        }

        private void MoveToBlockCenter(Vec3Int target)
        {
            // Mitte des Zielblocks
            double tx = target.X + 0.5, ty = target.Y, tz = target.Z + 0.5;
            const double tolH = 0.1, tolV = 0.2;
            var overallStart = DateTime.UtcNow;

            // Aktuelle Block-Pos
            var s0 = GetPlayerState();
            var curBlock = new Vec3Int(
                (int)Math.Floor(s0.X),
                (int)Math.Floor(s0.Y),
                (int)Math.Floor(s0.Z)
            );

            // Prüfen, ob Ziel exakt eine Blockhöhe drüber ist
            bool isDirectlyAbove =
                target.X == curBlock.X &&
                target.Z == curBlock.Z &&
                target.Y == curBlock.Y + 1;

            // 1) Hindernisse räumen & Boden legen
            var head = new Vec3Int(target.X, target.Y + 1, target.Z);
            if (GetBlockStatus(head) != "minecraft:air") timedBreakBlock(head);
            var head2 = new Vec3Int(target.X, target.Y + 2, target.Z);
            if (GetBlockStatus(head2) != "minecraft:air") timedBreakBlock(head2);

            var blk = new Vec3Int(target.X, target.Y, target.Z);
            if (GetBlockStatus(blk) != "minecraft:air") timedBreakBlock(blk);

            var below = new Vec3Int(target.X, target.Y - 1, target.Z);
            if (GetBlockStatus(below) == "minecraft:air")
            {
                // Wenn wir nur genau hochbauen, fliegen wir über VerticalPhase vorab auf ty:
                if (isDirectlyAbove)
                {
                    Console.WriteLine("  ↑ Pure up → using VerticalPhase for jump");
                    VerticalPhase(ty, tolV, 5);
                }

                Console.WriteLine("  Placing floor block");
                timedPlaceBlock(below, "minecraft:dirt");
            }

            // 2) Bewegungs-Reihenfolge für alle anderen Fälle
            double dy0 = ty - s0.Y;
            if (dy0 < -tolV)
            {
                HorizontalPhase(tx, tz, tolH, overallStart);
                VerticalPhase(ty, tolV, 5);
            }
            else if (!isDirectlyAbove) // beim direkten Hochbau wurde VerticalPhase schon gemacht
            {
                VerticalPhase(ty, tolV, 5);
                HorizontalPhase(tx, tz, tolH, overallStart);
            }

            // 3) Endgültiger Stopp & Ausrichtung
            SetVelocity(0, 0, 0);
            var end = GetPlayerState();
            double ex = end.X, ey = end.Y, ez = end.Z;
            double edx = tx - ex, edy = ty - ey, edz = tz - ez;
            //double yaw = Math.Atan2(-edx, edz) * 180.0 / Math.PI;
            //double pitch = -Math.Atan2(edy, Math.Sqrt(edx * edx + edz * edz)) * 180.0 / Math.PI;
            //_http.GetAsync($"{_base}/look?yaw={yaw}&pitch={pitch}").Wait();
        }

        private bool IsNeighbor(Vec3Int a, Vec3Int b)
        {
            int dx = Math.Abs(a.X - b.X);
            int dy = Math.Abs(a.Y - b.Y);
            int dz = Math.Abs(a.Z - b.Z);

            // reine Auf-/Abwärtsbewegung?
            //if (dx == 0 && dz == 0 && dy == 1) return true;

            // kardinal: exactly one of dx/dz == 1, the other zero, and dy ∈ {-1,0,1}
            //if (((dx == 1 && dz == 0) || (dx == 0 && dz == 1)) && dy <= 1) return true;
            if (Math.Max(Math.Max(dx, dy), dz) <= 1)
            {
                return true;
            }

            return false;
        }


        // Extrahiere die Vertikal-Phase
        private void VerticalPhase(double ty, double tolV, int timeoutSeconds)
        {
            //Console.WriteLine($"    → VerticalPhase to Y={ty:F2}");
            var start = DateTime.UtcNow;
            while (true)
            {
                if ((DateTime.UtcNow - start).TotalSeconds > timeoutSeconds)
                {
                    Console.WriteLine("      Vertical: Timeout");
                    break;
                }
                var s = GetPlayerState();
                double cy = s.Y, dy = ty - cy;
                //Console.WriteLine($"      vert pos={cy:F2}, dy={dy:F2}");
                if (Math.Abs(dy) <= tolV)
                {
                    //Console.WriteLine("      ✔ Vertical aligned");
                    break;
                }
                double vy = Math.Sign(dy) * Math.Min(Math.Abs(dy), 1.0);
                SetVelocity(0, vy, 0);
                Thread.Sleep(50);
            }
            // Stoppe vertikal-Komponente
            SetVelocity(0, 0, 0);
            
        }

        // Extrahiere die Horizontal-Phase
        private void HorizontalPhase(double tx, double tz, double tolH, DateTime overallStart)
        {
            //Console.WriteLine($"    → HorizontalPhase to ({tx:F2},{tz:F2})");
            while (true)
            {
                if ((DateTime.UtcNow - overallStart).TotalSeconds > 10)
                {
                    Console.WriteLine("      Horizontal: Overall timeout");
                    break;
                }
                var s = GetPlayerState();
                double cx = s.X, cz = s.Z;
                double dx = tx - cx, dz = tz - cz;
                double distH = Math.Sqrt(dx * dx + dz * dz);
                //Console.WriteLine($"      horiz pos=({cx:F2},{cz:F2}), distH={distH:F2}");
                if (distH <= tolH)
                {
                    //Console.WriteLine("      ✔ Horizontal aligned");
                    break;
                }
                double speed = Math.Min(distH, 0.2);
                double vx = dx / distH * speed;
                double vz = dz / distH * speed;
                SetVelocity(vx, 0, vz);
                
            }
        }

        public void timedFarm(string blockname)
        {
            bool finished = false;
            var now = DateTime.UtcNow;

            while (!finished)
            {

            }
        }


    }

    class Program
    {
        static void Main()
        {
            //Thread.Sleep(2000);
            var az = new Azathoth();
            bool finished = false;
            int finishCounter = 0;
            int stuckCounter = 0;
            int tryCounter = 0;
            

            while (true) {
                Console.WriteLine($"------ RUN: {tryCounter} FINISHED: {finishCounter} STUCK: {stuckCounter} ------");
                var result = az.GetNextBlock("minecraft:oak_log", 128);
                var path = az.GetPathTo(new Azathoth.Vec3Int(result.X, result.Y, result.Z));
                finished = az.Goto(path);
                if (finished)
                {
                    finishCounter++;
                }
                else
                {
                    stuckCounter++;
                }
                tryCounter++;
            }

        }
    }
}
