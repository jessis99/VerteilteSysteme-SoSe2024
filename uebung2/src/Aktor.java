package src;

import org.oxoo2a.sim4da.Message;
import org.oxoo2a.sim4da.Node;
import org.oxoo2a.sim4da.UnknownNodeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Aktor extends Node{

    static Message message = new Message();
    private boolean zustandAktiv = true;
    private double probability = 1.0;
    private static final double reductionFaktor = 0.5;
    private Random random = new Random();
    private int nameNumber;

    @Override
    protected void engage() {
        new Receiver().start();
    }

    private class Receiver extends Thread {

        public Receiver() {
            super();
        }

        @Override
        public void run() {
            while (true) {
                Message m = receive();
                onMessage(m);
            }
        }
    }

    public Aktor(String name){
        super(name);
    }
    
    public static void randomWait(int millisek) {
        Random random = new Random();
        int wartezeit = random.nextInt(millisek); 
        try {
            Thread.sleep(wartezeit);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToRandomNodes() {
        int anzahlMessages = random.nextInt(VerteiltesSystem.numberOfNodes);
        int[] array = new int[VerteiltesSystem.numberOfNodes];
        for(int i = 0; i < VerteiltesSystem.numberOfNodes; i++){
            array[i] = i;
        }

		List<Integer> intList = new ArrayList<>();
        for (int i : array) {
            intList.add(i); //Array in Liste umwandeln, da das Mischen auf Listen arbeitet
        }
        Collections.shuffle(intList); // Mischen
        for (int i = 0; i < intList.size(); i++) {
            array[i] = intList.get(i); //zurück in ein Array umwandeln
        }

        for (int i = 0; i < anzahlMessages; i++) {
            try {
                Message message = new Message();
                message.add("Type", "Firework");
                if(array[i] != nameNumber){     //damit keine Nachrichten an sich selber geschickt werden
                    send(message, "Node" + array[i]);
                    Observer.anzahlAusgehenderNachrichten += 1;
                    System.out.println("Ausgehend: " + Observer.anzahlAusgehenderNachrichten);
                }
            } catch (UnknownNodeException e) {
                e.printStackTrace();
            }
        }
        zustandAktiv = false;
        Observer.activeAktors -= 1;
        System.out.println("Aktiv: " + Observer.activeAktors);
    }

    protected void onMessage(Message message) {

        Observer.anzahlEingehenderNachrichten += 1;
        System.out.println("Eingehend: " + Observer.anzahlEingehenderNachrichten);
        if (zustandAktiv) {
            return;
        }

        String type = message.query("Type");
        if ("Firework".equals(type)) {
            if (random.nextDouble() < probability) {
                zustandAktiv = true;
                Observer.activeAktors += 1;
        System.out.println("Aktiv: " + Observer.activeAktors);
                sendMessageToRandomNodes();
                probability *= reductionFaktor;
            }
        }
    }

    protected void initialize(){
        randomWait(5000); //warten bis Nachrichten versendet werden
        sendMessageToRandomNodes();
    }

    public void setNameNumber(int nameNumber) {
        this.nameNumber = nameNumber;
    }
}
