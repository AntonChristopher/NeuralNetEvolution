import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Simulation extends JPanel implements Runnable {
    private List<Food> foods = new ArrayList<>();
    private List<Entity> entities = new ArrayList<>();
    private SpatialGrid foodGrid;
    private volatile boolean running = true;
    private volatile boolean paused = false;
    private long totalTicks = 0;
    private final Object stateLock = new Object();
    
    private int mouseX, mouseY;
    private Entity selectedEntity = null;
    
    public Simulation() {
        setPreferredSize(new Dimension(Config.WIDTH, Config.HEIGHT));
        setBackground(new Color(20, 20, 20));
        setFocusable(true);
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F) Config.FAST_TRAINING_MODE = !Config.FAST_TRAINING_MODE;
                else if (e.getKeyCode() == KeyEvent.VK_P) paused = !paused;
                else if (e.getKeyCode() == KeyEvent.VK_R) Config.DRAW_RAYS = !Config.DRAW_RAYS;
                else if (e.getKeyCode() == KeyEvent.VK_M) spawnPredator();
                else if (e.getKeyCode() == KeyEvent.VK_SPACE) burstFoodAtMouse();
                else if (e.getKeyCode() == KeyEvent.VK_S) savePopulationState();
                else if (e.getKeyCode() == KeyEvent.VK_L) loadPopulationState();
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
            @Override
            public void mouseDragged(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
        });
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { selectEntityAt(e.getX(), e.getY()); }
        });

        foodGrid = new SpatialGrid(Config.WIDTH, Config.HEIGHT, Config.RAY_LENGTH);
        
        for(int i = 0; i < Config.INITIAL_FOOD_COUNT; i++) spawnFoodEdge();
        for(int i = 0; i < Config.INITIAL_PREY_COUNT; i++) {
            entities.add(new Entity(Math.random() * Config.WIDTH, Math.random() * Config.HEIGHT, false));
        }
        for(int i = 0; i < Config.INITIAL_PREDATOR_COUNT; i++) {
            entities.add(new Entity(Math.random() * Config.WIDTH, Math.random() * Config.HEIGHT, true));
        }
        
        new Thread(this).start();
    }
    
    private void spawnFoodEdge() {
        double vx = (Math.random() - 0.5) * Config.FOOD_MAX_SPEED * 2;
        double vy = (Math.random() - 0.5) * Config.FOOD_MAX_SPEED * 2;
        
        // Spawn uniformly from all 4 walls
        int edge = (int)(Math.random() * 4);
        if (edge == 0) { // Top
            foods.add(new Food(Math.random() * Config.WIDTH, 10, vx, Math.abs(vy))); 
        } else if (edge == 1) { // Right
            foods.add(new Food(Config.WIDTH - 10, Math.random() * Config.HEIGHT, -Math.abs(vx), vy));
        } else if (edge == 2) { // Bottom
            foods.add(new Food(Math.random() * Config.WIDTH, Config.HEIGHT - 10, vx, -Math.abs(vy)));
        } else { // Left
            foods.add(new Food(10, Math.random() * Config.HEIGHT, Math.abs(vx), vy));
        }
    }
    
    private void burstFoodAtMouse() {
        synchronized (stateLock) {
            for(int i = 0; i < Config.BURST_FOOD_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double speed = Math.random() * Config.FOOD_MAX_SPEED;
                foods.add(new Food(mouseX, mouseY, Math.cos(angle) * speed, Math.sin(angle) * speed));
            }
        }
    }
    
    private void spawnPredator() {
        synchronized (stateLock) {
            entities.add(new Entity(mouseX, mouseY, true));
        }
    }
    
    private void selectEntityAt(int x, int y) {
        synchronized (stateLock) {
            selectedEntity = null;
            double closestDist = Double.MAX_VALUE;
            for (Entity e : entities) {
                double dx = e.x - x; double dy = e.y - y;
                double distSq = dx*dx + dy*dy;
                if (distSq < 900 && distSq < closestDist) { 
                    closestDist = distSq;
                    selectedEntity = e;
                }
            }
        }
    }

    private void savePopulationState() {
        List<Entity> entitiesToSave;
        List<Food> foodsToSave;
        long ticksToSave;
        synchronized (stateLock) {
            entitiesToSave = new ArrayList<>(entities);
            foodsToSave = new ArrayList<>(foods);
            ticksToSave = totalTicks;
        }

        try (FileWriter writer = new FileWriter("population.json")) {
            writer.write("[\n");
            for (int i = 0; i < entitiesToSave.size(); i++) {
                writer.write(entitiesToSave.get(i).toJson());
                if (i < entitiesToSave.size() - 1) writer.write(",\n");
            }
            writer.write("\n]");
        } catch (Exception ex) { ex.printStackTrace(); }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("population.dat"))) {
            oos.writeObject(entitiesToSave);
            oos.writeObject(foodsToSave);
            oos.writeLong(ticksToSave);
            System.out.println("Saved JSON and Binary population successfully!");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @SuppressWarnings("unchecked")
    private void loadPopulationState() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("population.dat"))) {
            List<Entity> loadedEntities = (List<Entity>) ois.readObject();
            List<Food> loadedFoods = (List<Food>) ois.readObject();
            long loadedTicks = ois.readLong();
            for (Entity entity : loadedEntities) {
                if (entity.genome != null) entity.genome.repairStructure();
            }
            synchronized (stateLock) {
                entities = loadedEntities;
                foods = loadedFoods;
                totalTicks = loadedTicks;
                selectedEntity = null;
            }
            System.out.println("Loaded population successfully!");
        } catch (Exception ex) { 
            System.err.println("Could not load population.dat. Ensure you have saved first.");
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60.0;
        double delta = 0;
        boolean wasFastTraining = Config.FAST_TRAINING_MODE;

        while (running) {
            boolean fastTraining = Config.FAST_TRAINING_MODE;
            if (fastTraining != wasFastTraining) {
                lastTime = System.nanoTime();
                delta = 0;
                wasFastTraining = fastTraining;
            }

            if (fastTraining && !paused) {
                tick();
            } else {
                long now = System.nanoTime();
                delta += (now - lastTime) / nsPerTick;
                lastTime = now;
                while (delta >= 1) { 
                    if (!paused) tick(); 
                    delta--; 
                }
                repaint();
            }
        }
    }

    private void tick() {
        synchronized (stateLock) {
            totalTicks++;
            if (Math.random() < Config.FOOD_SPAWN_RATE) spawnFoodEdge();
        
            foodGrid.clear();
            for (Food f : foods) {
                f.update();
                foodGrid.insert(f);
            }
        
            List<Entity> newChildren = new ArrayList<>();
            Iterator<Entity> it = entities.iterator();
        
        while (it.hasNext()) {
            Entity e = it.next();
            e.update(foodGrid, entities);
            
            // Eating Logic (With Saturation Diminishing Returns)
            if (!e.isPredator) {
                Iterator<Food> fit = foods.iterator();
                while (fit.hasNext()) {
                    Food f = fit.next();
                    double dx = f.x - e.x; double dy = f.y - e.y;
                    if (dx*dx + dy*dy < Math.pow(Config.ENTITY_RADIUS + Config.FOOD_RADIUS, 2)) {
                        double efficiency = Math.max(0, 1.0 - (e.energy / Config.PREY_MAX_ENERGY));
                        e.energy += Config.ENERGY_FROM_FOOD * efficiency;
                        fit.remove();
                    }
                }
            } else {
                for (Entity other : entities) {
                    if (other == e || other.isPredator || other.energy <= 0) continue;
                    double dx = other.x - e.x; double dy = other.y - e.y;
                    if (dx*dx + dy*dy < Math.pow(Config.PREDATOR_RADIUS + Config.ENTITY_RADIUS, 2)) {
                        e.energy += other.energy;
                        other.energy = -1000; // Kill prey
                    }
                }
            }
            
            if (e.energy <= 0) {
                if (e == selectedEntity) selectedEntity = null;
                it.remove();
                continue;
            }
            
            // Reproduction Check (Checks threshold and cooldown)
            double threshold = e.isPredator ? Config.PREDATOR_REPRODUCTION_THRESHOLD : Config.PREY_REPRODUCTION_THRESHOLD;
            if (e.energy >= threshold && e.cooldown <= 0) {
                newChildren.add(e.reproduce());
            }
        }
        
            entities.addAll(newChildren);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        synchronized (stateLock) {
            g2d.setColor(Color.GREEN);
            for (Food f : foods) {
                g2d.fillOval((int)(f.x - Config.FOOD_RADIUS), (int)(f.y - Config.FOOD_RADIUS), 
                             (int)(Config.FOOD_RADIUS * 2), (int)(Config.FOOD_RADIUS * 2));
            }
        
            for (Entity e : entities) {
                e.draw(g2d);
                if (e == selectedEntity) {
                    g2d.setColor(Color.YELLOW);
                    int rad = (int)(e.isPredator ? Config.PREDATOR_RADIUS : Config.ENTITY_RADIUS);
                    g2d.drawOval((int)(e.x - rad - 5), (int)(e.y - rad - 5), rad * 2 + 10, rad * 2 + 10);
                }
            }
        
            g2d.setColor(Color.WHITE);
            g2d.drawString("Ticks: " + (totalTicks / 100) / 10.0 + " k | Entities: " + entities.size() + " | Food: " + foods.size(), 20, 20);
            if (paused) {
                g2d.setColor(Color.RED);
                g2d.drawString("PAUSED (Press P to resume)", 20, 40);
                g2d.setColor(Color.WHITE);
            } else {
                g2d.drawString("Press 'P' to PAUSE", 20, 40);
            }
            g2d.drawString("Press 'F' to toggle Fast Training: " + (Config.FAST_TRAINING_MODE ? "ON" : "OFF"), 20, 60);
            g2d.drawString("Press 'M' to inject 1 Predator at Mouse", 20, 80);
            g2d.drawString("Press 'S' to Save Population | 'L' to Load", 20, 100);
        
            if (selectedEntity != null) {
            int netWidth = 350;
            int netHeight = 300;
            int netX = Config.WIDTH - netWidth - 40;
            int netY = Config.HEIGHT - netHeight - 40;
            
            selectedEntity.genome.drawNetwork(g2d, netX, netY, netWidth, netHeight);
            
            g2d.setColor(Color.WHITE);
            String type = selectedEntity.isPredator ? "PREDATOR" : "PREY";
                g2d.drawString("Selected " + type + " (Gen: " + selectedEntity.generation + " | Energy: " + (int)selectedEntity.energy + ")", netX, netY - 10);
            }
        }
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Neuro-Evolution Ecosystem v4");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new Simulation());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
