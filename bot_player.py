import requests
from Pathfinder import Pathfinder
import pyautogui
import time

class Bot:

    world = None
 
    path = []

    def __init__(self):
        pass

    def get_player_position(self):
        try:
            response = requests.get("http://localhost:8080/position")
            response.raise_for_status()
            return response.json()
        except requests.RequestException as e:
            return {"error": str(e)}

    def get_world_snapshot(self):
        print("getting world data...")
        try:
            response = requests.get("http://localhost:8080/world_snapshot?r=64")
            response.raise_for_status()
            print("got world data")
            return response.json()
        except requests.RequestException as e:
            print("error getting world data")
            return {"error": str(e)}

    def find_path(self, start, goal):
        print("trying to find path...")
        world = self.get_world_snapshot()
        pathfinder = Pathfinder(world)
        self.path = pathfinder.find_path(start, goal)
        if self.path:
            print("Pfad gefunden:", self.path.positions)
            print("Baukosten:", self.path.build_cost)
            print("Zeitkosten:", self.path.time_cost)
            print("Gesamtkosten:", self.path.total_cost)
        else:
            print("Kein Pfad gefunden.")

    def place_red_wool_path(self, path: list[tuple[int, int, int]], delay=0.2):
        """
        Setzt mit /setblock-Befehlen rote Wolle an jede Position des gegebenen Pfads.
        Geht davon aus, dass Minecraft aktiv ist und der Chat geschlossen ist.
        """
        for x, y, z in path:
            # 1. Chat öffnen
            pyautogui.press('t')        # öffnet den Chat mit '/' und schreibt direkt los
            time.sleep(0.1)

            # 2. Befehl schreiben
            cmd = f"/setblock {x} {y} {z} minecraft:glass"
            pyautogui.typewrite(cmd)
            time.sleep(0.05)

            # 3. Enter drücken
            pyautogui.press('enter')
            time.sleep(delay)           # kleine Pause, damit der Server hinterherkommt


        
            

b = Bot()
b.find_path((-52, 63, 352), (-41, 77, 365))
b.place_red_wool_path(b.path.positions)

