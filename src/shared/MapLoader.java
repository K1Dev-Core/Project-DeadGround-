package shared;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

public class MapLoader {
    public int mapWidthTiles, mapHeightTiles, tileWidth, tileHeight;
    public int mapPixelW, mapPixelH;
    public List<Tileset> tilesets = new ArrayList<>();
    public List<Layer> layers = new ArrayList<>();
    public List<Rectangle2D.Double> collisions = new ArrayList<>();

    public static class Layer {
        public String name;
        public int width, height;
        public int[] gids;
    }

    public static class Tileset {
        public BufferedImage sheet;
        public int firstGid = 1, columns, tileWidth, tileHeight, tileCount;
        public Map<Integer, BufferedImage> cache = new HashMap<>();

        public BufferedImage getTile(int gid) {
            if (gid <= 0)
                return null;
            
            int flags = gid & 0xF0000000;
            int cleanGid = gid & 0x0FFFFFFF;
            
            int local = cleanGid - firstGid;
            if (local < 0 || local >= tileCount) {
                System.out.println("DEBUG: GID " + gid + " (clean: " + cleanGid + ") out of range. firstGid=" + firstGid + ", tileCount=" + tileCount);
                return null;
            }
            
            return cache.computeIfAbsent(gid, g -> {
                int col = local % columns, row = local / columns;
                int sx = col * tileWidth, sy = row * tileHeight;
                
                if (sx + tileWidth > sheet.getWidth() || sy + tileHeight > sheet.getHeight()) {
                    System.out.println("DEBUG: Tile bounds exceeded for GID " + gid + " at (" + col + "," + row + ")");
                    return null;
                }
                
                BufferedImage tile = sheet.getSubimage(sx, sy, tileWidth, tileHeight);
                
                if ((flags & 0x80000000) != 0) {
                    tile = flipHorizontal(tile);
                }
                if ((flags & 0x40000000) != 0) {
                    tile = flipVertical(tile);
                }
                if ((flags & 0x20000000) != 0) {
                    tile = flipDiagonal(tile);
                }
                
                return tile;
            });
        }
        
        private BufferedImage flipHorizontal(BufferedImage img) {
            int w = img.getWidth();
            int h = img.getHeight();
            BufferedImage flipped = new BufferedImage(w, h, img.getType());
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    flipped.setRGB(w - 1 - x, y, img.getRGB(x, y));
                }
            }
            return flipped;
        }
        
        private BufferedImage flipVertical(BufferedImage img) {
            int w = img.getWidth();
            int h = img.getHeight();
            BufferedImage flipped = new BufferedImage(w, h, img.getType());
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    flipped.setRGB(x, h - 1 - y, img.getRGB(x, y));
                }
            }
            return flipped;
        }
        
        private BufferedImage flipDiagonal(BufferedImage img) {
            int w = img.getWidth();
            int h = img.getHeight();
            BufferedImage flipped = new BufferedImage(h, w, img.getType());
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    flipped.setRGB(y, x, img.getRGB(x, y));
                }
            }
            return flipped;
        }
    }

    public BufferedImage getTile(int gid) {
        if (gid <= 0)
            return null;
        
     
        for (Tileset tileset : tilesets) {
            int cleanGid = gid & 0x0FFFFFFF;
            if (cleanGid >= tileset.firstGid && cleanGid < tileset.firstGid + tileset.tileCount) {
                return tileset.getTile(gid);
            }
        }
        
        System.out.println("DEBUG: No tileset found for GID " + gid);
        return null;
    }

    public void load(String path) throws Exception {
        Path tmxPath = Paths.get(path);
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(Files.newInputStream(tmxPath));
        Element map = doc.getDocumentElement();

        mapWidthTiles = Integer.parseInt(map.getAttribute("width"));
        mapHeightTiles = Integer.parseInt(map.getAttribute("height"));
        tileWidth = Integer.parseInt(map.getAttribute("tilewidth"));
        tileHeight = Integer.parseInt(map.getAttribute("tileheight"));
        mapPixelW = mapWidthTiles * tileWidth;
        mapPixelH = mapHeightTiles * tileHeight;

        
        NodeList tilesetNodes = map.getElementsByTagName("tileset");
        for (int i = 0; i < tilesetNodes.getLength(); i++) {
            Element ts = (Element) tilesetNodes.item(i);
            
         
            if (ts.hasAttribute("source")) {
                System.out.println("Creating tileset for external reference: " + ts.getAttribute("source"));
                // สร้าง tileset ใหม่สำหรับ external tileset โดยใช้ข้อมูลจาก tileset ที่มีอยู่แล้ว
                Tileset t = new Tileset();
                t.firstGid = Integer.parseInt(ts.getAttribute("firstgid"));
                t.tileWidth = 64; // ใช้ขนาดมาตรฐาน
                t.tileHeight = 64;
                
                // ใช้ tileset ที่มีอยู่แล้ว (towerDefense_tilesheet.png)
                String baseName = "towerDefense_tilesheet.png";
                Path sheetPath = tmxPath.getParent().resolve(baseName);
                if (!Files.exists(sheetPath))
                    sheetPath = Paths.get("assets").resolve(baseName);
                
                if (Files.exists(sheetPath)) {
                    t.sheet = ImageIO.read(sheetPath.toFile());
                    
                    // คำนวณ columns และ tileCount จากขนาดภาพจริง
                    t.columns = t.sheet.getWidth() / t.tileWidth;
                    int rows = t.sheet.getHeight() / t.tileHeight;
                    t.tileCount = t.columns * rows;
                    
                    System.out.println("External tileset loaded: " + t.sheet.getWidth() + "x" + t.sheet.getHeight() + 
                                     " (firstGid=" + t.firstGid + ", tileCount=" + t.tileCount + ", columns=" + t.columns + ")");
                    tilesets.add(t);
                } else {
                    System.err.println("External tileset image not found: " + sheetPath);
                }
                continue;
            }
            
            Tileset t = new Tileset();
            t.firstGid = Integer.parseInt(ts.getAttribute("firstgid"));
            t.tileWidth = Integer.parseInt(ts.getAttribute("tilewidth"));
            t.tileHeight = Integer.parseInt(ts.getAttribute("tileheight"));
            t.tileCount = Integer.parseInt(ts.getAttribute("tilecount"));
            t.columns = Integer.parseInt(ts.getAttribute("columns"));
            
            String baseName = new File(((Element) ts.getElementsByTagName("image").item(0))
                    .getAttribute("source")).getName();
            Path sheetPath = tmxPath.getParent().resolve(baseName);
            if (!Files.exists(sheetPath))
                sheetPath = Paths.get("assets").resolve(baseName);
            
            System.out.println("Looking for tileset image: " + sheetPath);
            if (Files.exists(sheetPath)) {
                t.sheet = ImageIO.read(sheetPath.toFile());
                System.out.println("Tileset loaded: " + t.sheet.getWidth() + "x" + t.sheet.getHeight() + 
                                 " (firstGid=" + t.firstGid + ", tileCount=" + t.tileCount + ")");
                tilesets.add(t);
            } else {
                System.err.println("Tileset image not found: " + sheetPath);
              
            }
        }

        NodeList layerNodes = map.getElementsByTagName("layer");
        for (int i = 0; i < layerNodes.getLength(); i++) {
            Element l = (Element) layerNodes.item(i);
            Layer L = new Layer();
            L.name = l.getAttribute("name");
            L.width = Integer.parseInt(l.getAttribute("width"));
            L.height = Integer.parseInt(l.getAttribute("height"));
            String[] parts = ((Element) l.getElementsByTagName("data").item(0))
                    .getTextContent().trim().split("\\s*,\\s*");
            int[] gids = new int[L.width * L.height];
            for (int k = 0; k < gids.length && k < parts.length; k++) {
                try {
                    gids[k] = Integer.parseInt(parts[k].trim());
                } catch (Exception e) {
                    gids[k] = 0;
                }
            }
            L.gids = gids;
            layers.add(L);
        }

        NodeList objGroups = map.getElementsByTagName("objectgroup");
        for (int i = 0; i < objGroups.getLength(); i++) {
            Element og = (Element) objGroups.item(i);
            if (!"Collision".equalsIgnoreCase(og.getAttribute("name")))
                continue;
            NodeList objs = og.getElementsByTagName("object");
            for (int j = 0; j < objs.getLength(); j++) {
                Element o = (Element) objs.item(j);
                double x = Double.parseDouble(o.getAttribute("x"));
                double y = Double.parseDouble(o.getAttribute("y"));
                double w = Double.parseDouble(o.getAttribute("width"));
                double h = Double.parseDouble(o.getAttribute("height"));
                collisions.add(new Rectangle2D.Double(x, y, w, h));
            }
        }
    }
}
