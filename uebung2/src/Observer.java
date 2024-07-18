package src;

import org.oxoo2a.sim4da.Node;

public class Observer extends Node{

    static int anzahlAusgehenderNachrichten = 0;
    static int anzahlEingehenderNachrichten = 0;
    static int activeAktors;
    static boolean fertig = false;


    @Override
    protected void engage() {
        new Counter().start();
    }

    private class Counter extends Thread {

        private Counter() {
            super();
        }

        @Override
        public void run() {
            while (!fertig) {
                pruefeFertig();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    public Observer(String name){
        super(name);
    }

    public static void setActiveAktors(int anzahlActiveAktors) {
        activeAktors = anzahlActiveAktors;
    }


    private void pruefeFertig(){
        if((anzahlAusgehenderNachrichten == anzahlEingehenderNachrichten) && (activeAktors == 0)){
            fertig = true;
            VerteiltesSystem.endSimulator();
        }
    }

}
