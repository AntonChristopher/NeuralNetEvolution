public class Config {
    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;
    public static final double SCALE = 1.0;
    public static volatile boolean FAST_TRAINING_MODE = false;
    public static volatile boolean DRAW_RAYS = false;

    // Food Mechanics
    public static final int INITIAL_FOOD_COUNT = 200;
    public static final int INITIAL_PREY_COUNT = 100;
    public static final int INITIAL_PREDATOR_COUNT = 0;
    public static final double FOOD_RADIUS = 3.0;
    public static final double FOOD_MAX_SPEED = 3.0;
    public static final double FOOD_SPAWN_RATE = 0.2; 
    public static final int BURST_FOOD_COUNT = 25;

    // Prey Traits
    public static final double ENTITY_RADIUS = 7.0;
    public static final double MAX_SPEED = 8.0;
    public static final double MAX_ROTATION_SPEED = 0.15; 
    
    // Predator Traits
    public static final double PREDATOR_RADIUS = 10.0;
    public static final double PREDATOR_MAX_SPEED = 9.5;
    public static final double PREDATOR_ENERGY_COST_MULTIPLIER = 2.5; 

    // Brain Tax & Speciation
    public static final double BRAIN_TAX_MULTIPLIER = 0.0002; // Exponential penalty multiplier for large brains
    public static final int BASE_CONNECTIONS = 34; // Expected base connections (17 inputs * 2 outputs)
    public static final double SPECIATION_DISTANCE = 2.5; // Network distance threshold to trigger neon color

    // Energy, Saturation & Metabolism
    public static final double STARTING_ENERGY = 2000.0;
    public static final double ENERGY_COST_PER_TICK = 0.25; 
    public static final double ENERGY_COST_PER_SPEED_UNIT = 0.1; 
    
    public static final double ENERGY_FROM_FOOD = 400.0;
    public static final double PREY_MAX_ENERGY = 5000.0;
    public static final double PREDATOR_MAX_ENERGY = 6000.0;

    // Reproduction Thresholds & Costs
    public static final double PREY_REPRODUCTION_THRESHOLD = 500.0;
    public static final double PREY_OFFSPRING_COST = 200.0; // Energy deducted from parent and given to child
    public static final int PREY_REPRODUCTION_COOLDOWN = 60; // 1 second at 60fps

    public static final double PREDATOR_REPRODUCTION_THRESHOLD = 1000.0;
    public static final double PREDATOR_OFFSPRING_COST = 500.0;
    public static final int PREDATOR_REPRODUCTION_COOLDOWN = 180; // 3 seconds at 60fps

    // Neural Traits (3 inputs per ray + 2 compass inputs)
    public static final int NUM_RAYS = 5; 
    public static final double SENSOR_FOV = Math.PI / 1.5; 
    public static final double RAY_LENGTH = 350.0;
    public static final int TOTAL_INPUTS = (NUM_RAYS * 3) + 2; 
    
    // Mutation Rates
    public static final double MUTATION_RATE_WEIGHT = 0.10;
    public static final double MUTATION_RATE_BIAS = 0.10;
    public static final double BIAS_MUTATION_STEP = 0.25;
    public static final double MUTATION_RATE_ADD_NODE = 0.02;
    public static final double MUTATION_RATE_REMOVE_NODE = 0.005;
    public static final double MUTATION_RATE_ADD_CONNECTION = 0.06;
    public static final double MUTATION_RATE_REMOVE_CONNECTION = 0.02;
}




