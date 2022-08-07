# LDtk

## Intgrid layers

An intgrid layer is just a layer of values (colors which usually have a name i.e. "walls").

There are 2 ways to render an intgrid to actual tiles:

### Auto-layer tileset

You can assign a certain tileset to an intgrid layer, enabling it to render tiles based on a set of rules.

|  Tilemap | Level
| --- | --- |
| ![tilemap](https://user-images.githubusercontent.com/1263058/172050248-091500b0-b71f-405a-b79e-ec48cbd728db.png) | ![image](https://user-images.githubusercontent.com/1263058/172050942-3009ac14-00e7-4cd6-9cca-48d9a4e5b5cb.png) |
| Rules | Options |
| ![rules](https://user-images.githubusercontent.com/1263058/172050213-69e9476c-e45e-4281-b5aa-1c6cd17203de.png) | ![rules options](https://user-images.githubusercontent.com/1263058/172050719-3789d4a1-2c7c-4e12-ae46-76ddb3564c46.png) |

Each rule specifies which tiles are rendered depending on the tiles around (and the tile itself, which is usually the center tile).

In the example (top left corner):

1. The tile itself must be white walls.
2. The tiles on the left and top must **not** be walls.
3. The empty tiles are not specified.

The rule is then mirrored on the X axis, including the rendering (functioning as a top right corner)

### Separate auto-layer

Instead of assigning a tilemap to the intgrid layer, you can leave it as is and create a separate auto-layer. 

![auto-layer](https://user-images.githubusercontent.com/1263058/172050789-eea98a21-5cf3-47cc-a7d1-09a9bea03769.png)

This works the same way as above, but it means you can reuse the same info for multiple tilesets (for example to add decorations).
