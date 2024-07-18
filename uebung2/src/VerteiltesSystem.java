package src;

import org.oxoo2a.sim4da.Simulator;

import java.util.ArrayList;
import java.util.List;

public class VerteiltesSystem {

    public static int numberOfNodes;
    public static Simulator simulator;

    public static void main(String[] args) {
        // Check if the number of nodes is provided
 /*       if (args.length != 1) {
            System.err.println("Usage: java src.DistributedApp <number_of_nodes>");
            System.exit(1);
        }
*/
        try {
            //numberOfNodes = Integer.parseInt(args[0]);
            numberOfNodes = 5;
        } catch (NumberFormatException e) {
            System.err.println("The number of nodes must be an integer.");
            System.exit(1);
            return;
        }

        simulator = Simulator.getInstance();

        List<Aktor> nodes = new ArrayList<>();

        // Create the nodes
        for (int i = 0; i < numberOfNodes; i++) {
            Aktor aktor = new Aktor("Node" + i);
            aktor.setNameNumber(i);
            nodes.add(aktor);
        }

        Observer observer = new Observer("Observer");
        Observer.setActiveAktors(nodes.size());

        // Initialize the nodes
        for (Aktor aktor : nodes) {
            aktor.initialize();
        }

        simulator.simulate();
    }

    public static void endSimulator(){
        simulator.shutdown();
    }

}
