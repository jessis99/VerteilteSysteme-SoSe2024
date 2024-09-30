package src;

import org.oxoo2a.sim4da.Message;
import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.Node;
import org.oxoo2a.sim4da.UnknownNodeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import java.lang.reflect.Field;

public class Knoten extends Node {

    private int id;
    private State state;
    private int term;
    private int votedFor;
    private List<String> log;
    private List<Knoten> peers;
    private int votesReceived;
    private ReentrantLock lock;
    private Random random;
    private long lastHeartbeatTime;
    private long electionTimeout;
    private boolean isActive = true;
    private boolean hasVoted = false; // Neuer Zustand, um zu prüfen, ob bereits gewählt wurde
    private NetworkConnection nc = null;

    public Knoten(String name) {
        super(name);
        this.state = State.FOLLOWER;
        this.term = 0;
        this.votedFor = -1;
        this.log = new ArrayList<>();
        this.peers = new ArrayList<>();
        this.votesReceived = 0;
        this.lock = new ReentrantLock();
        this.random = new Random();
        this.electionTimeout = random.nextInt(1500) + 2500; // 2500 to 4000ms
        this.lastHeartbeatTime = System.currentTimeMillis();
        this.hasVoted = false;  

        try {
            // Zugriff auf die private Variable "nc" in der Oberklasse
            Field feld = Node.class.getDeclaredField("nc");
            feld.setAccessible(true);
            nc = (NetworkConnection) feld.get(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void engage() {
        new Receiver().start();
        checkElectionTimeout();
    }

    private class Receiver extends Thread {

        @Override
        public void run() {
            while (true) {
                Message m = receive();
                if (m.query("Type").equals("Heartbeat")) {
                    receiveHeartbeat(m);
                } else if (m.query("Type").equals("ID")) {
                    receiveVoteRequest(m);
                } else if (m.query("Type").equals("AppendEntries")) {
                    handleAppendEntries(m);
                }
            }
        }
    }

    public void sendHeartBeat() {
        Random random = new Random();
        int max = random.nextInt(30) + 5;
        int count = 0;
        while (state == State.LEADER && isActive && (count <= max)) {
            Message message = new Message();
            message.add("Type", "Heartbeat");
            message.add("Term", this.term);
            for (Knoten peer : peers) {
                try {
                    send(message, "Node" + peer.id);
                    Thread.sleep(50);
                } catch (UnknownNodeException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(1000); // Herzschlag alle 1000 ms
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count++;
        }
        isActive = false;
        for (Knoten peer : peers){
            peer.peers.remove(this);
        }
    }

    public void appendEntry(String command) {
        if (state == State.LEADER && isActive) {
            log.add(command);
            Message message = new Message();
            message.add("Type", "AppendEntries");
            message.add("Term", this.term);
            message.add("LogIndex", log.size() - 1);
            message.add("Command", command);
            for (Knoten peer : peers) {
                try {
                    send(message, "Node" + peer.id);
                } catch (UnknownNodeException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Leader " + id + ": Appended log entry " + command);
        }
    }

    public void handleAppendEntries(Message m) {
        int leaderTerm = m.queryInteger("Term");
        if (leaderTerm >= this.term) {
            lock.lock();
            try {
                this.state = State.FOLLOWER;
                this.term = leaderTerm;
                int logIndex = m.queryInteger("LogIndex");
                String command = m.query("Command");
                if (logIndex >= log.size()) {
                    log.add(command);
                    System.out.println("Node " + id + ": Appended log entry from leader.");
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void becomeCandidate() {
        if (isActive) {
            this.state = State.CANDIDATE;
            nc.getLogger().debug("Node " + id + " became a Candidate for term " + this.term);
            startElection();
        }
    }

    public void becomeLeader() {
        if (isActive) {
            this.state = State.LEADER;
            nc.getLogger().debug("Node " + id + " became the Leader for term " + this.term);
            sendHeartBeat();
        }
    }

    public void setPeers(List<Knoten> peers) {
        this.peers = peers;
    }

    public void receiveHeartbeat(Message m) {
        if (isActive) {
            lock.lock();
            try {
                int leaderTerm = m.queryInteger("Term");
                if (leaderTerm >= this.term) {
                    this.state = State.FOLLOWER;
                    this.term = leaderTerm;
                    this.lastHeartbeatTime = System.currentTimeMillis();
                    System.out.println("Node " + id + ": Received heartbeat from Leader.");
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public void startElection() {
        lock.lock();
        if(peers.size() != 0){
            try {
                if ((state == State.FOLLOWER || state == State.CANDIDATE) && isActive) {
                    this.state = State.CANDIDATE;
                    this.votesReceived = 1; // Eigene Stimme
                    this.term++;
                    this.votedFor = id;
        
                    System.out.println("Node " + id + ": Starting election for term " + this.term);
        
                    // Anfrage an alle Peers senden, um ihre Stimmen anzufordern
                    for (Knoten peer : peers) {
                        Message m = new Message();
                        m.add("Type", "ID");
                        m.add("ID", this.id);
                        m.add("Term", this.term);
                        try {
                            send(m, "Node" + peer.id);
                        } catch (UnknownNodeException e) {
                            e.printStackTrace();
                        }
                    }
        
                    // Warte auf genügend Stimmen, um die Wahl zu beenden
                    new Thread(() -> waitForMajority()).start();
                }
            } finally {
                lock.unlock();
            }
        }else{
            VerteiltesSystem.endSimulator();
        }
    }
    
    private void waitForMajority() {
        long startTime = System.currentTimeMillis();
        long electionEndTime = startTime + electionTimeout;
    
        while (System.currentTimeMillis() < electionEndTime && votesReceived <= peers.size() / 2) {
            // Warte auf mehr Stimmen oder Timeout
            try {
                Thread.sleep(100); // Kurze Pause zwischen Überprüfungen
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    
        lock.lock();
        try {
            // Wenn die Mehrheit erreicht wurde, werde Leader
            if (votesReceived > peers.size() / 2) {
                becomeLeader();
            } else {
                // Falls keine Mehrheit erreicht wurde, zurück zum Follower-Status
                this.state = State.FOLLOWER;
                System.out.println("Node " + id + ": Election failed, returning to Follower.");
            }
        } finally {
            lock.unlock();
            for(Knoten peer : peers){
                peer.hasVoted = false;
                peer.votedFor = -1;
            }
        }
    }
    
    
    

    public void voteRandomCandidate() {
        lock.lock();
        try {
            if (votedFor == -1 && !hasVoted) { // Prüfen, ob noch nicht gewählt wurde
                List<Knoten> candidates = new ArrayList<>();
                for (Knoten peer : peers) {
                    if (peer.state == State.CANDIDATE) {
                        candidates.add(peer);
                    }
                }
                if (!candidates.isEmpty()) {
                    Knoten randomCandidate = candidates.get(random.nextInt(candidates.size()));
                    votedFor = randomCandidate.getId();
                    randomCandidate.incrementVoteCount();
                    hasVoted = true;  // Verhindert weitere Wahlen
                    System.out.println("Node " + id + ": Voted for Node " + randomCandidate.getId());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void receiveVoteRequest(Message m) {
        lock.lock();
        try {
            Knoten candidate = getKnoten(m.queryInteger("ID"));
            int candidateTerm = m.queryInteger("Term");
            if (!hasVoted && (votedFor == -1 || votedFor == candidate.getId()) && candidateTerm >= this.term && candidate.state == State.CANDIDATE) {
                votedFor = candidate.getId();
                candidate.incrementVoteCount();
                hasVoted = true;  // Sobald eine Stimme abgegeben wurde, keine weiteren Wahlen
                System.out.println("Node " + id + ": Voted for Node " + candidate.getId());
            }
        } finally {
            lock.unlock();
        }
    }

    public void incrementVoteCount() {
        lock.lock();
        try {
            this.votesReceived++;
            if (votesReceived > peers.size() / 2 && state == State.CANDIDATE) {
                becomeLeader();
            }
        } finally {
            lock.unlock();
        }
    }

    public void checkElectionTimeout() {
        while (isActive) {
            lock.lock();
            try {
                long currentTime = System.currentTimeMillis();
                if (state != State.LEADER && currentTime - lastHeartbeatTime > electionTimeout && !hasVoted) {
                    System.out.println("Node " + id + ": Election timeout, starting election.");
                    becomeCandidate();
                }
            } finally {
                lock.unlock();
            }

            try {
                Thread.sleep(500); // Überprüfung alle 500ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public Knoten getKnoten(int id) {
        for (Knoten knoten : VerteiltesSystem.nodes) {
            if (knoten.id == id) {
                return knoten;
            }
        }
        return null;
    }
}
