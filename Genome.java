import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.*;
import java.io.Serializable;

public class Genome implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public static class Connection implements Serializable {
        private static final long serialVersionUID = 1L;
        int inNode, outNode;
        double weight;
        boolean enabled = true;
        boolean defaultConnection;
        public Connection(int in, int out, double w) {
            this(in, out, w, false);
        }
        public Connection(int in, int out, double w, boolean defaultConnection) {
            inNode = in;
            outNode = out;
            weight = w;
            this.defaultConnection = defaultConnection;
        }
    }
    
    List<Connection> connections = new ArrayList<>();
    Map<Integer, double[]> nodePos = new HashMap<>(); 
    int inputCount, outputCount;
    int maxNodeId;
    double[] biases;
    
    public Genome(int inputs, int outputs) {
        inputCount = inputs;
        outputCount = outputs;
        maxNodeId = inputs + outputs - 1;
        biases = new double[maxNodeId + 1];
        
        for (int i = 0; i < inputs; i++) {
            nodePos.put(i, new double[]{0.0, (i + 1.0) / (inputs + 1.0)});
        }
        for (int i = 0; i < outputs; i++) {
            nodePos.put(inputs + i, new double[]{1.0, (i + 1.0) / (outputs + 1.0)});
        }

        for (int i = 0; i < inputs; i++) {
            for (int j = 0; j < outputs; j++) {
                connections.add(new Connection(i, inputs + j, (Math.random() * 2) - 1, true));
            }
        }
    }
    
    public Genome cloneAndMutate() {
        Genome child = new Genome(inputCount, outputCount);
        child.connections.clear();
        child.maxNodeId = this.maxNodeId;
        this.ensureBiasCapacity();
        child.biases = Arrays.copyOf(this.biases, child.maxNodeId + 1);
        
        for(Integer k : this.nodePos.keySet()) {
            child.nodePos.put(k, this.nodePos.get(k).clone());
        }
        
        for (Connection c : this.connections) {
            Connection cc = new Connection(c.inNode, c.outNode, c.weight, c.defaultConnection);
            cc.enabled = c.enabled;
            child.connections.add(cc);
        }
        
        if (Math.random() < Config.MUTATION_RATE_WEIGHT) {
            for(Connection c : child.connections) {
                if (Math.random() < 0.2) c.weight += (Math.random() * 0.5) - 0.25;
            }
        }
        
        if (Math.random() < Config.MUTATION_RATE_ADD_NODE && !child.connections.isEmpty()) {
            Connection c = child.connections.get((int)(Math.random() * child.connections.size()));
            c.enabled = false;
            child.maxNodeId++;
            child.nodePos.put(child.maxNodeId, new double[]{0.2 + Math.random()*0.6, 0.1 + Math.random()*0.8});
            child.biases = Arrays.copyOf(child.biases, child.maxNodeId + 1);
            
            child.connections.add(new Connection(c.inNode, child.maxNodeId, 1.0));
            child.connections.add(new Connection(child.maxNodeId, c.outNode, c.weight));
        }

        if (Math.random() < Config.MUTATION_RATE_BIAS) {
            for (int nodeId = inputCount; nodeId <= child.maxNodeId; nodeId++) {
                if (Math.random() < 0.2) {
                    child.biases[nodeId] += (Math.random() * 2.0 - 1.0) * Config.BIAS_MUTATION_STEP;
                }
            }
        }
        
        if (Math.random() < Config.MUTATION_RATE_ADD_CONNECTION) {
            int n1 = (int)(Math.random() * (child.maxNodeId + 1));
            int n2 = inputCount + (int)(Math.random() * (child.maxNodeId + 1 - inputCount)); 
            child.connections.add(new Connection(n1, n2, (Math.random() * 2) - 1));
        }

        if (Math.random() < Config.MUTATION_RATE_REMOVE_NODE) {
            List<Integer> removableNodes = new ArrayList<>();
            for (Integer nodeId : child.nodePos.keySet()) {
                if (nodeId >= inputCount + outputCount) removableNodes.add(nodeId);
            }
            if (!removableNodes.isEmpty()) {
                int nodeId = removableNodes.get((int)(Math.random() * removableNodes.size()));
                child.nodePos.remove(nodeId);
                child.connections.removeIf(c -> !c.defaultConnection
                        && (c.inNode == nodeId || c.outNode == nodeId));
                child.recalculateMaxNodeId();
                child.biases = Arrays.copyOf(child.biases, child.maxNodeId + 1);
            }
        }

        if (Math.random() < Config.MUTATION_RATE_REMOVE_CONNECTION) {
            List<Connection> removableConnections = new ArrayList<>();
            for (Connection c : child.connections) {
                if (!c.defaultConnection) removableConnections.add(c);
            }
            if (!removableConnections.isEmpty()) {
                child.connections.remove(removableConnections.get(
                        (int)(Math.random() * removableConnections.size())));
            }
        }
        
        return child;
    }

    public void repairStructure() {
        connections.removeIf(c -> !nodePos.containsKey(c.inNode) || !nodePos.containsKey(c.outNode));
        recalculateMaxNodeId();
        ensureBiasCapacity();
    }

    private void recalculateMaxNodeId() {
        maxNodeId = inputCount + outputCount - 1;
        for (Integer nodeId : nodePos.keySet()) {
            maxNodeId = Math.max(maxNodeId, nodeId);
        }
    }

    private void ensureBiasCapacity() {
        if (biases == null) {
            biases = new double[maxNodeId + 1];
        } else if (biases.length <= maxNodeId) {
            biases = Arrays.copyOf(biases, maxNodeId + 1);
        }
    }
    
    public double calculateDistance(Genome other) {
        int disjoint = Math.abs(this.connections.size() - other.connections.size());
        double weightDiff = 0;
        int common = Math.min(this.connections.size(), other.connections.size());
        for (int i = 0; i < common; i++) {
            weightDiff += Math.abs(this.connections.get(i).weight - other.connections.get(i).weight);
        }
        if (common > 0) weightDiff /= common;
        return disjoint + weightDiff;
    }

    public double[] feedForward(double[] inputs) {
        ensureBiasCapacity();
        Map<Integer, Double> nodeValues = new HashMap<>();
        for (int i = 0; i < inputCount; i++) nodeValues.put(i, inputs[i]);
        
        for (int k = 0; k < 3; k++) {
            double[] nextValues = new double[maxNodeId + 1];
            for (Connection c : connections) {
                if (c.enabled && nodeValues.containsKey(c.inNode)) {
                    nextValues[c.outNode] += nodeValues.get(c.inNode) * c.weight;
                }
            }
            for (int i = inputCount; i <= maxNodeId; i++) {
                nodeValues.put(i, Math.tanh(nextValues[i] + biases[i]));
            }
        }
        
        double[] outputs = new double[outputCount];
        for (int i = 0; i < outputCount; i++) {
            outputs[i] = nodeValues.getOrDefault(inputCount + i, 0.0);
        }
        return outputs;
    }

    public void drawNetwork(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(30, 30, 30, 220));
        g.fillRect(x, y, w, h);
        g.setColor(Color.WHITE);
        g.drawRect(x, y, w, h);

        for(Connection c : connections) {
            if(!c.enabled) continue;
            double[] p1 = nodePos.get(c.inNode);
            double[] p2 = nodePos.get(c.outNode);
            if(p1 == null || p2 == null) continue;

            int x1 = x + (int)(p1[0] * (w - 20)) + 10;
            int y1 = y + (int)(p1[1] * (h - 20)) + 10;
            int x2 = x + (int)(p2[0] * (w - 20)) + 10;
            int y2 = y + (int)(p2[1] * (h - 20)) + 10;

            if(c.weight > 0) g.setColor(new Color(0, 255, 0, 150));
            else g.setColor(new Color(255, 0, 0, 150));

            g.setStroke(new BasicStroke((float)Math.min(5, Math.abs(c.weight))));
            g.drawLine(x1, y1, x2, y2);
        }

        g.setStroke(new BasicStroke(1));
        for(Integer id : nodePos.keySet()) {
            double[] p = nodePos.get(id);
            int nx = x + (int)(p[0] * (w - 20)) + 10;
            int ny = y + (int)(p[1] * (h - 20)) + 10;
            g.setColor(Color.LIGHT_GRAY);
            g.fillOval(nx - 4, ny - 4, 8, 8);
        }
    }
    
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"maxNodeId\": ").append(maxNodeId).append(",\n  \"connections\": [\n");
        for (int i = 0; i < connections.size(); i++) {
            Connection c = connections.get(i);
            sb.append(String.format("    {\"in\": %d, \"out\": %d, \"weight\": %.4f, \"enabled\": %b}", 
                      c.inNode, c.outNode, c.weight, c.enabled));
            if (i < connections.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}");
        return sb.toString();
    }
}
