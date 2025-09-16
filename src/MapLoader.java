import org.w3c.dom.*;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.util.*;

public class MapLoader {
    public int mapWidthTiles, mapHeightTiles, tileWidth, tileHeight;
    public int mapPixelW, mapPixelH;
    public Tileset tileset;
    public List<Layer> layers = new ArrayList<>();
    public List<Rectangle2D.Double> collisions = new ArrayList<>();

    public static class Layer {
        String name;
        int width, height;
        int[] gids;
    }

    public static class Tileset {
        BufferedImage sheet;
        int firstGid = 1, columns, tileWidth, tileHeight, tileCount;
        Map<Integer, BufferedImage> cache = new HashMap<>();

        BufferedImage getTile(int gid) {
            if (gid <= 0) return null;
            int local = gid - firstGid;
            if (local < 0 || local >= tileCount) return null;
            return cache.computeIfAbsent(gid, g -> {
                int col = local % columns, row = local / columns;
                int sx = col * tileWidth, sy = row * tileHeight;
                return sheet.getSubimage(sx, sy, tileWidth, tileHeight);
            });
        }
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

        Element ts = (Element) map.getElementsByTagName("tileset").item(0);
        Tileset t = new Tileset();
        t.firstGid = Integer.parseInt(ts.getAttribute("firstgid"));
        t.tileWidth = Integer.parseInt(ts.getAttribute("tilewidth"));
        t.tileHeight = Integer.parseInt(ts.getAttribute("tileheight"));
        t.tileCount = Integer.parseInt(ts.getAttribute("tilecount"));
        t.columns = Integer.parseInt(ts.getAttribute("columns"));
        String baseName = new File(((Element) ts.getElementsByTagName("image").item(0))
                .getAttribute("source")).getName();
        Path sheetPath = tmxPath.getParent().resolve(baseName);
        if (!Files.exists(sheetPath)) sheetPath = Paths.get("assets").resolve(baseName);
        t.sheet = ImageIO.read(sheetPath.toFile());
        tileset = t;

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
                try { gids[k] = Integer.parseInt(parts[k].trim()); }
                catch (Exception e) { gids[k] = 0; }
            }
            L.gids = gids;
            layers.add(L);
        }

        NodeList objGroups = map.getElementsByTagName("objectgroup");
        for (int i = 0; i < objGroups.getLength(); i++) {
            Element og = (Element) objGroups.item(i);
            if (!"Collision".equalsIgnoreCase(og.getAttribute("name"))) continue;
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
