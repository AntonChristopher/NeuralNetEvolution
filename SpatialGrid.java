import java.util.*;
public class SpatialGrid {
    private int cols, rows;
    private double cellSize;
    private List<Food>[][] grid;

    @SuppressWarnings("unchecked")
    public SpatialGrid(int width, int height, double cellSize) {
        this.cellSize = cellSize;
        this.cols = (int) Math.ceil(width / cellSize);
        this.rows = (int) Math.ceil(height / cellSize);
        grid = new ArrayList[cols][rows];
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                grid[i][j] = new ArrayList<>();
            }
        }
    }

    public void clear() {
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                grid[i][j].clear();
            }
        }
    }

    public void insert(Food f) {
        int cx = (int) (f.x / cellSize);
        int cy = (int) (f.y / cellSize);
        if (cx >= 0 && cx < cols && cy >= 0 && cy < rows) {
            grid[cx][cy].add(f);
        }
    }

    public List<Food> getNearby(double x, double y, double radius) {
        List<Food> nearby = new ArrayList<>();
        int minX = Math.max(0, (int) ((x - radius) / cellSize));
        int maxX = Math.min(cols - 1, (int) ((x + radius) / cellSize));
        int minY = Math.max(0, (int) ((y - radius) / cellSize));
        int maxY = Math.min(rows - 1, (int) ((y + radius) / cellSize));

        for (int i = minX; i <= maxX; i++) {
            for (int j = minY; j <= maxY; j++) {
                nearby.addAll(grid[i][j]);
            }
        }
        return nearby;
    }
}
