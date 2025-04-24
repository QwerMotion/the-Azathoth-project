import requests
import time
import math

class Azathoth:
    def __init__(self, base_url="http://localhost:8080"):
        self.base = base_url.rstrip("/")
        self.path = []
        self.position = None

    def get_playerpos(self):
        r = requests.get(f"{self.base}/position"); r.raise_for_status()
        data = r.json()
        self.position = (data["x"], data["y"], data["z"])
        return self.position

    def get_path_to(self, goal_position, radius=64):
        x, y, z = self.get_playerpos()
        gx, gy, gz = goal_position
        params = {"sx":int(x),"sy":int(y),"sz":int(z),
                  "gx":gx,"gy":gy,"gz":gz,"r":radius}
        r = requests.get(f"{self.base}/find_path", params=params); r.raise_for_status()
        self.path = [tuple(pos) for pos in r.json()["positions"]]
        return self.path

    def get_block_status(self, pos):
        x, y, z = pos
        r = requests.get(f"{self.base}/block_status", params={"x":x,"y":y,"z":z}); r.raise_for_status()
        return r.json()["block"]

    def destroy_block(self, pos, interval=0.2):
        x, y, z = pos
        requests.get(f"{self.base}/break_block", params={"x":x,"y":y,"z":z}).raise_for_status()
        # Polling
        while self.get_block_status(pos) != "minecraft:air":
            time.sleep(interval)

    def place_block(self, pos, name, interval=0.2):
        x, y, z = pos
        
        #requests.get(f"{self.base}/place_block", params={"x":x,"y":y,"z":z,"block":name}).raise_for_status()
        requests.get(f"http://localhost:8080/place_block?x={x}&y={y}&z={z}&block=minecraft:dirt")
        
        while self.get_block_status(pos) != name:
            time.sleep(interval)

    def set_forward(self, on):
        requests.get(f"{self.base}/forward", params={"pressed":str(on).lower()})

    def set_jumping(self, on):
        requests.get(f"{self.base}/jump", params={"pressed":str(on).lower()})

    def set_looking(self, yaw, pitch):
        requests.get(f"{self.base}/look", params={"yaw":yaw,"pitch":pitch})

    def goto_next_position(self, target):
        # target ist (tx_block, ty_block, tz_block) als Integer
        tx_block, ty, tz_block = target
        # Block-Mitte
        tx = tx_block + 0.5
        tz = tz_block + 0.5

        tolerance_h = 0.2
        tolerance_v = 0.5
        start_time = time.time()
        self.set_forward(False)

        while True:
            # aktuelle Position
            cx, cy, cz = self.get_playerpos()

            # 1) Hindernisse abbauen (Zielblock & Kopf darüber)
            for pos in [(tx_block, ty, tz_block), (tx_block, ty + 1, tz_block)]:
                if self.get_block_status(pos) != "minecraft:air":
                    self.destroy_block(pos)

            # 2) Boden sichern
            below = (tx_block, ty - 1, tz_block)
            if self.get_block_status(below) == "minecraft:air":
                self.place_block(below, "minecraft:dirt")

            # 3) Distanz prüfen (horizontal zur Mitte, vertikal zur Blockhöhe)
            dx = tx - cx
            dz = tz - cz
            dy = ty - cy
            dist_h = math.hypot(dx, dz)
            dist_v = abs(dy)
            if dist_h < tolerance_h and dist_v < tolerance_v:
                break

            # 4) Springen falls nötig
            if dy > 0.5:
                self.set_jumping(True)
            else:
                self.set_jumping(False)

            # 5) Blickrichtung laufend korrigieren
            yaw   = math.degrees(math.atan2(-dx, dz))
            pitch = math.degrees(-math.atan2(dy, math.hypot(dx, dz)))
            self.set_looking(yaw, pitch)

            # 6) Vorwärts halten
            self.set_forward(True)

            # 7) kurze Pause
            #time.sleep(0.05)

            # 8) Safety-Timeout
            if time.time() - start_time > 30:
                print("goto_next_position: Timeout, gehe weiter")
                break

        # Am Ende stoppen und letzte Blickkorrektur
        self.set_forward(False)
        self.set_jumping(False)
        # finale Ausrichtung auf Mitte
        dx = tx - cx; dz = tz - cz; dy = ty - cy
        final_yaw   = math.degrees(math.atan2(-dx, dz))
        final_pitch = math.degrees(-math.atan2(dy, math.hypot(dx, dz)))
        self.set_looking(final_yaw, final_pitch)

    def goto(self, goal_position):
        path = self.get_path_to(goal_position)
        for step in path:
            self.goto_next_position(step)
        print("Ziel erreicht:", goal_position)



az = Azathoth()
print("trying...")
#az.place_block((191, 66, -143), "dings")
time.sleep(3)
az.goto((120,80,-179))
