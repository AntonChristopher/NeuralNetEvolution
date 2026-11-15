import java.io.Serializable;
public class Food implements Serializable {
    private static final long serialVersionUID = 1L;
    public double x, y;
    public double vx, vy;

    public Food(double x, double y, double vx, double vy) {
        this.x = x; this.y = y;
        this.vx = vx; this.vy = vy;
    }

    public void update() {
        x += vx; y += vy;
        if (x <= Config.FOOD_RADIUS || x >= Config.WIDTH - Config.FOOD_RADIUS) vx *= -1;
        if (y <= Config.FOOD_RADIUS || y >= Config.HEIGHT - Config.FOOD_RADIUS) vy *= -1;
    }
}
