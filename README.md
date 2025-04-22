Endpoints: 

http://localhost:8080/position

{
  "look_y": 14.4000043869019,
  "look_z": 0,
  "x": -110.469815920124,
  "y": 63,
  "z": -52.7965208654828,
  "look_x": 769.500305175781
}


http://localhost:8080/inventory [{"item":"minecraft:wheat_seeds","count":1},{"item":"minecraft:dirt","count":1},{"item":"minecraft:sand","count":3}]

http://localhost:8080/world_items  [{"item":"minecraft:wheat_seeds","count":1,"x":-86.79052734375,"y":65.0,"z":-33.120361328125},{"item":"minecraft:sand","count":2,"x":-84.93408203125,"y":64.0,"z":-36.658203125}]

http://localhost:8080/world_snapshot?r=5 {"-89,63,-36":"minecraft:sand","-89,63,-35":"minecraft:sand","-88,63,-35":"minecraft:grass_block","-90,64,-37":"minecraft:air","-89,64,-37":"minecraft:air","-88,64,-35":"minecraft:air","-90,64,-36":"minecraft:air","-89,64,-36":"minecraft:air","-88,64,-36":"minecraft:air","-90,64,-35":"minecraft:air","-88,64,-37":"minecraft:air","-88,65,-36":"minecraft:air","-90,65,-37":"minecraft:air","-89,65,-37":"minecraft:air","-88,65,-37":"minecraft:air","-89,65,-36":"minecraft:air","-89,65,-35":"minecraft:air","-88,65,-35":"minecraft:air","-90,65,-36":"minecraft:air","-90,65,-35":"minecraft:air","-90,63,-36":"minecraft:air","-90,63,-35":"minecraft:grass_block","-89,64,-35":"minecraft:air","-90,63,-37":"minecraft:sand","-88,63,-36":"minecraft:sand","-89,63,-37":"minecraft:sand","-88,63,-37":"minecraft:sand"}

http://localhost:8080/next_block?block=minecraft:iron_ore&r=64

http://localhost:8080/forward?pressed=true

http://localhost:8080/entities?r=32 {"z":-38,"x":-83,"y":49}

http://localhost:8080/looking_block {"x":-87,"y":64,"z":-35,"block":"minecraft:grass_block"} 

http://localhost:8080/jump?pressed=true {"jumpPressed":true}

http://localhost:8080/forward?pressed=true

http://localhost:8080/look?yaw=90&pitch=30

http://localhost:8080/block_status?x=100&y=64&z=100

http://localhost:8080/place_block?x=100&y=64&z=100&block=minecraft:dirt

http://localhost:8080/break_block?x=100&y=64&z=100

http://localhost:8080/find_path?sx=-52&sy=63&sz=352&gx=-41&gy=72&gz=390&r=64

{"positions":[[-52,63,352],[-52,63,353],[-52,63,354],[-52,63,355],[-52,63,356],[-52,64,357],[-52,64,358],[-52,64,359],[-52,64,360],[-52,64,361],[-52,64,362],[-52,64,363],[-52,64,364],[-52,64,365],[-52,64,366],[-52,64,367],[-52,64,368],[-52,64,369],[-52,64,370],[-52,64,371],[-52,64,372],[-52,64,373],[-52,64,374],[-52,64,375],[-52,64,376],[-52,64,377],[-52,64,378],[-52,64,379],[-52,65,380],[-52,65,381],[-52,65,382],[-52,65,383],[-52,66,384],[-52,66,385],[-52,66,386],[-52,66,387],[-51,66,387],[-50,66,387],[-49,66,387],[-48,66,387],[-47,66,387],[-46,66,387],[-45,66,387],[-44,66,387],[-44,67,388],[-43,68,388],[-43,69,389],[-43,70,390],[-42,71,390],[-41,72,390]],"buildCost":6,"timeCost":0,"totalCost":55}


