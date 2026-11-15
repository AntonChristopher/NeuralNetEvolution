import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.util.List;
import java.io.Serializable;

public class Entity implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public double x, y;
    public double angle;
    public double energy;
    public Genome genome;
    public double speed = 0;
    
    public int generation = 0;
    public boolean isPredator = false;
    public int r, g, b;
    public int cooldown = 0;
    
    public Entity(double x, double y, boolean isPredator) {
        this.x = x; this.y = y;
        this.angle = Math.random() * Math.PI * 2;
        this.isPredator = isPredator;
        this.genome = new Genome(Config.TOTAL_INPUTS, 2); 
        this.cooldown = isPredator ? Config.PREDATOR_REPRODUCTION_COOLDOWN : Config.PREY_REPRODUCTION_COOLDOWN;
        
        if (isPredator) {
            this.energy = Config.PREDATOR_OFFSPRING_COST;
            this.r = 255; this.g = 50; this.b = 50; 
        } else {
            this.energy = Config.PREY_OFFSPRING_COST;
            this.r = (int)(Math.random() * 200);
            this.g = (int)(Math.random() * 200 + 55);
            this.b = (int)(Math.random() * 200);
        }
    }

    public Entity reproduce() {
        Entity child = new Entity(this.x, this.y, this.isPredator);
        child.generation = this.generation + 1;
        child.genome = this.genome.cloneAndMutate();
        child.cooldown = isPredator ? Config.PREDATOR_REPRODUCTION_COOLDOWN : Config.PREY_REPRODUCTION_COOLDOWN;
        this.cooldown = child.cooldown; // Parent resets cooldown too
        
        if (!isPredator) {
            // Speciation Logic: If neural network diverges heavily, assign vibrant neon color
            if (child.genome.calculateDistance(this.genome) > Config.SPECIATION_DISTANCE) {
                float hue = (float)Math.random();
                Color neon = Color.getHSBColor(hue, 1.0f, 1.0f);
                child.r = neon.getRed();
                child.g = neon.getGreen();
                child.b = neon.getBlue();
            } else {
                child.r = Math.max(0, Math.min(255, this.r + (int)((Math.random() - 0.5) * 40)));
                child.g = Math.max(0, Math.min(255, this.g + (int)((Math.random() - 0.5) * 40)));
                child.b = Math.max(0, Math.min(255, this.b + (int)((Math.random() - 0.5) * 40)));
            }
        }
        
        double cost = isPredator ? Config.PREDATOR_OFFSPRING_COST : Config.PREY_OFFSPRING_COST;
        this.energy -= cost; 
        child.energy = cost; 
        return child;
    }

    public void update(SpatialGrid foodGrid, List<Entity> allEntities) {
        if (cooldown > 0) cooldown--;
        
        double[] inputs = new double[Config.TOTAL_INPUTS];
        List<Food> nearbyFood = foodGrid.getNearby(x, y, Config.RAY_LENGTH);
        
        double angleStep = Config.NUM_RAYS > 1 ? Config.SENSOR_FOV / (Config.NUM_RAYS - 1) : 0;
        double startAngle = angle - (Config.SENSOR_FOV / 2.0);

        for (int i = 0; i < Config.NUM_RAYS; i++) {
            double rayAngle = startAngle + (i * angleStep);
            double rayDirX = Math.cos(rayAngle);
            double rayDirY = Math.sin(rayAngle);
            
            double closestDist = Config.RAY_LENGTH;
            double closestRelVel = 0;
            double closestType = 0; // 0 = Nothing
            
            double tWall = Config.RAY_LENGTH;
            if (rayDirX > 0) { double t = (Config.WIDTH - x) / rayDirX; if (t > 0 && t < tWall) tWall = t; }
            if (rayDirX < 0) { double t = (0 - x) / rayDirX; if (t > 0 && t < tWall) tWall = t; }
            if (rayDirY > 0) { double t = (Config.HEIGHT - y) / rayDirY; if (t > 0 && t < tWall) tWall = t; }
            if (rayDirY < 0) { double t = (0 - y) / rayDirY; if (t > 0 && t < tWall) tWall = t; }
            
            if (tWall < closestDist) {
                closestDist = tWall;
                closestRelVel = -speed; 
                closestType = -1.0; 
            }

            for (Food f : nearbyFood) {
                double dx = f.x - this.x; double dy = f.y - this.y;
                double distSq = dx*dx + dy*dy;
                if (distSq < closestDist * closestDist) {
                    double dist = Math.sqrt(distSq);
                    if ((dx * rayDirX + dy * rayDirY) / dist > 0.95) { 
                        closestDist = dist;
                        closestRelVel = ((f.vx * rayDirX + f.vy * rayDirY) - speed); 
                        closestType = 1.0; 
                    }
                }
            }
            
            for (Entity e : allEntities) {
                if (e == this) continue;
                double dx = e.x - this.x; double dy = e.y - this.y;
                double distSq = dx*dx + dy*dy;
                if (distSq < closestDist * closestDist) {
                    double dist = Math.sqrt(distSq);
                    if ((dx * rayDirX + dy * rayDirY) / dist > 0.95) { 
                        closestDist = dist;
                        closestRelVel = ((Math.cos(e.angle)*e.speed * rayDirX + Math.sin(e.angle)*e.speed * rayDirY) - speed); 
                        closestType = e.isPredator ? -0.5 : 0.5; 
                    }
                }
            }
            
            inputs[i * 3] = 1.0 - (closestDist / Config.RAY_LENGTH); 
            inputs[i * 3 + 1] = closestRelVel / (Config.MAX_SPEED * 2); 
            inputs[i * 3 + 2] = closestType; 
        }

        inputs[Config.NUM_RAYS * 3] = Math.cos(angle);
        inputs[Config.NUM_RAYS * 3 + 1] = Math.sin(angle);

        double[] outputs = genome.feedForward(inputs);
        
        double maxSpeed = isPredator ? Config.PREDATOR_MAX_SPEED : Config.MAX_SPEED;
        double desiredSpeed = outputs[0] * maxSpeed; 
        double rotation = outputs[1] * Config.MAX_ROTATION_SPEED;
        
        this.speed = Math.max(0, desiredSpeed); 
        this.angle += rotation;
        
        this.x += Math.cos(this.angle) * this.speed;
        this.y += Math.sin(this.angle) * this.speed;
        
        if (x < 0) x = 0; if (x > Config.WIDTH) x = Config.WIDTH;
        if (y < 0) y = 0; if (y > Config.HEIGHT) y = Config.HEIGHT;
        
        // Energy Burns
        double energyMultiplier = isPredator ? Config.PREDATOR_ENERGY_COST_MULTIPLIER : 1.0;
        this.energy -= (Config.ENERGY_COST_PER_TICK * energyMultiplier);
        this.energy -= (this.speed * Config.ENERGY_COST_PER_SPEED_UNIT * energyMultiplier);
        
        // Brain Tax (Quadratic penalty for massive networks)
        int excessConnections = Math.max(0, genome.connections.size() - Config.BASE_CONNECTIONS);
        double brainTax = Config.BRAIN_TAX_MULTIPLIER * (excessConnections * excessConnections);
        this.energy -= brainTax;
    }

    public void draw(Graphics2D g) {
        double reproductionThreshold = isPredator
                ? Config.PREDATOR_REPRODUCTION_THRESHOLD
                : Config.PREY_REPRODUCTION_THRESHOLD;
        int alpha = Math.max(50, Math.min(255, (int)((energy / reproductionThreshold) * 255)));
        double rad = isPredator ? Config.PREDATOR_RADIUS : Config.ENTITY_RADIUS;
        
        if (Config.DRAW_RAYS) {
            g.setColor(new Color(255, 255, 255, 15)); 
            g.setStroke(new BasicStroke(1));
            double angleStep = Config.NUM_RAYS > 1 ? Config.SENSOR_FOV / (Config.NUM_RAYS - 1) : 0;
            double startAngle = angle - (Config.SENSOR_FOV / 2.0);
            for (int i = 0; i < Config.NUM_RAYS; i++) {
                double rayAngle = startAngle + (i * angleStep);
                g.drawLine((int)x, (int)y, (int)(x + Math.cos(rayAngle) * Config.RAY_LENGTH), (int)(y + Math.sin(rayAngle) * Config.RAY_LENGTH));
            }
        }
        
        g.setColor(new Color(r, this.g, b, alpha));
        
        if (isPredator) {
            int[] px = {(int)(x + Math.cos(angle)*rad*1.5), (int)(x + Math.cos(angle+2.5)*rad), (int)(x + Math.cos(angle-2.5)*rad)};
            int[] py = {(int)(y + Math.sin(angle)*rad*1.5), (int)(y + Math.sin(angle+2.5)*rad), (int)(y + Math.sin(angle-2.5)*rad)};
            g.fillPolygon(px, py, 3);
            g.setColor(Color.RED);
            g.drawPolygon(px, py, 3);
        } else {
            g.fillOval((int)(x - rad), (int)(y - rad), (int)(rad * 2), (int)(rad * 2));
            g.setColor(Color.WHITE);
            g.drawOval((int)(x - rad), (int)(y - rad), (int)(rad * 2), (int)(rad * 2));
            g.drawLine((int)x, (int)y, (int)(x + Math.cos(angle) * rad * 1.5), (int)(y + Math.sin(angle) * rad * 1.5));
        }
    }
    
    public String toJson() {
        return "{\n  \"generation\": " + generation + ",\n  \"isPredator\": " + isPredator + 
               ",\n  \"energy\": " + energy + ",\n  \"genome\": " + genome.toJson() + "\n}";
    }
}
