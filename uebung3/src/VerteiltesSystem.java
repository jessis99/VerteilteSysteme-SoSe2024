package src;

import org.oxoo2a.sim4da.Simulator;

import java.util.ArrayList;
import java.util.List;

public class VerteiltesSystem {

    public static int numberOfNodes;
    public static Simulator simulator;
    public static List<Knoten> nodes = new ArrayList<>();
    
    public static void main(String[] args) {
        // Check if the number of nodes is provided
        if (args.length != 1) {
            System.err.println("Usage: java src.DistributedApp <number_of_nodes>");
            System.exit(1);
        }

        try {
            numberOfNodes = Integer.parseInt(args[0]);
            //numberOfNodes = 5;
        } catch (NumberFormatException e) {
            System.err.println("The number of nodes must be an integer.");
            System.exit(1);
            return;
        }

        simulator = Simulator.getInstance();

        // Create the nodes
        for (int i = 0; i < numberOfNodes; i++) {
            Knoten knoten = new Knoten("Node" + i);
            knoten.setId(i);
            nodes.add(knoten);
        }

        for (int i = 0; i < numberOfNodes; i++) {
            Knoten node = nodes.get(numberOfNodes - 1 - i);
            List<Knoten> peers = new ArrayList<>();
            for(int j = 0; j < numberOfNodes; j++){
                peers.add(nodes.get(j));
            }
            peers.remove(node);  // Remove itself from the peer list
            node.setPeers(peers);
        }

        simulator.simulate();
    }

    public static void endSimulator(){
        simulator.shutdown();
    }

}
