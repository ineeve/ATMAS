import java.util.ArrayList;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.Coordinates;
import utils.State;

@SuppressWarnings("serial")
public class AirplaneAgent extends Agent {
	
	private int id;
	private int maxFuel;
	private int maxSpeed;
	private int minSpeed;
	private int fuelPerTime;
	private Coordinates coordinates;
	private State state;
	private boolean onEmergency;
	private ArrayList<AirportAgent> airports;
	private AirportAgent destination;
	private AirportAgent origin;
	private Integer landingTime;
	private Integer minLandingtime;
	private Integer maxLandingTime;
	
	public AirplaneAgent(int id, ArrayList<AirportAgent> airports, AirportAgent origin) {
		this.airports = airports;
		this.origin = origin;
		this.coordinates = origin.getCoordinates();
	}
	public void setup() {
		 addBehaviour(new TakeoffBehaviour());
		 addBehaviour(new QueryListeningBehaviour());
		 System.out.println(getLocalName() + ": starting takeoff!");
	}
	public void takeDown() {
		 System.out.println(getLocalName() + ": done working.");
	}

	
	class TakeoffBehaviour extends Behaviour {
		 private int n = 0;
	
		 public void action() {
			 int takeoffTime = (int)(Math.random() * 21);
			 
		 }
		 public boolean done() {
			 return n == 3;
		 }
	}
	class QueryListeningBehaviour extends CyclicBehaviour {
		 MessageTemplate mt =
		 MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
		 public void action() {
		 ACLMessage msg = receive(mt);
		 if(msg != null) {
		 System.out.println(msg);
		 ACLMessage reply = msg.createReply();
		 reply.setPerformative(ACLMessage.INFORM);
		 reply.setContent("Don't have a clue...");
		 send(reply);
		 } else {
		 block();
		 }
		 }
	}
	

	
}
