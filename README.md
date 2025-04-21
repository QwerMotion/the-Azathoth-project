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
http://localhost:8080/entities?r=32 {"z":-38,"x":-83,"y":49}
http://localhost:8080/looking_block {"x":-87,"y":64,"z":-35,"block":"minecraft:grass_block"}
