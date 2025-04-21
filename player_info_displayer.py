import cv2
import numpy as np
import requests
import time

# Farbe: Dunkelgrün in BGR
GREEN = (0, 150, 0)
FONT = cv2.FONT_HERSHEY_SIMPLEX

def get_player_position():
    try:
        response = requests.get("http://localhost:8080/position")
        response.raise_for_status()
        return response.json()
    except requests.RequestException as e:
        return {"error": str(e)}

def get_look_at_block():
    try:
        response = requests.get("http://localhost:8080/looking_block")
        response.raise_for_status()
        return response.json()
    except requests.RequestException as e:
        return {"error": str(e)}
    

def draw_text(img, text, pos, color=GREEN, scale=0.7, thickness=1):
    cv2.putText(img, text, pos, FONT, scale, color, thickness, lineType=cv2.LINE_AA)


width, height = 800, 200

while True:
    frame = np.zeros((height, width, 3), dtype=np.uint8)
    data = get_player_position()
    look_at_data = get_look_at_block()

    if "error" in data:
        draw_text(frame, "Fehler: " + data["error"], (10, 50), color=(0, 0, 255))
    else:
        draw_text(frame, f"X: {data['x']:.2f}", (10, 40))
        draw_text(frame, f"Y: {data['y']:.2f}", (10, 70))
        draw_text(frame, f"Z: {data['z']:.2f}", (10, 100))
        draw_text(frame, f"Look: ({data['look_x']:.2f}, {data['look/setblock -38 64 309 minecraft:glass_block
                  t/setblock -37 64 309 minecraft:glass_block
                  _y']:.2f}, {data['look_z']:.2f})", (10, 130))
        draw_text(frame, f"faced Block: ({look_at_data['block']}, {look_at_data['x']:}, {look_at_data['y']:}, {look_at_data['z']:})", (10, 160))

    cv2.imshow("Spielerdaten", frame)

    if cv2.waitKey(1) & 0xFF == ord('q'):  # Warte 1 Sekunde, beende bei 'q'
        break

cv2.destroyAllWindows()


