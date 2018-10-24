import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import jade.core.AID;
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
	private AirportAgent currentAirport;
	private Integer landingTime;
	private Integer minLandingtime;
	private Integer maxLandingTime;
	public HashMap<AID,Integer> agentView; //the state of the world recognized by this agent
	public HashMap<Integer,TreeSet<AID>> nogoods;
	public Integer desiredTime;
	public Integer minTimeDomain;
	public Integer maxTimeDomain;
	
	public AirplaneAgent(ArrayList<AirportAgent> airports, AirportAgent origin) {
		this.airports = airports;
		this.currentAirport = origin;
		this.coordinates = origin.getCoordinates();
		agentView = new HashMap<AID, Integer>();
		nogoods = new HashMap<Integer, TreeSet<AID>>(); 
	}
	public void setup() {
		 addBehaviour(new TakeoffBehaviour());
		 addBehaviour(new ProposeListeningBehaviour());
		 addBehaviour(new RejectProposalListeningBehaviour());
		 System.out.println(getLocalName() + ": starting takeoff!");
	}
	public void takeDown() {
		 System.out.println(getLocalName() + ": done working.");
	}
	
	public void checkAgentView() {
		if (agentView.containsValue(desiredTime)) {
			for(int t = minTimeDomain; t <= maxTimeDomain; t++) {
				if (!agentView.containsValue(t)) {
					this.desiredTime = t;
					ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
					msg.setContent("ok? " + t);
					ArrayList<AID> otherAgents = currentAirport.getAirplanesAID();
					for (AID a : otherAgents) {
						msg.addReceiver(a);
					}
					send(msg);
					return;
				}
			}
			// in case no consistent time domain was found
			backtrack();
		}
	}
	
	public void backtrack() {
		for(Map.Entry<Integer, TreeSet<AID>> entry : nogoods.entrySet()) {
			Integer v = entry.getKey();
			TreeSet<AID> t = entry.getValue();
			AID lowerPriorityAgent = t.last(); // the bigger the AID the lower it's pripority
			ACLMessage msg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
			msg.setContent("nogood " + v);
			msg.addReceiver(lowerPriorityAgent);
			send(msg);
		}
	}

	
	class TakeoffBehaviour extends Behaviour {
		 private int n = 0;
	
		 public void action() {
			 AirplaneAgent.this.desiredTime = (int)
					 (Math.random() * 
					 AirplaneAgent.this.maxTimeDomain + 
					 AirplaneAgent.this.minTimeDomain);
			 
		 }
		 public boolean done() {
			 return n == 3;
		 }
	}

	class ProposeListeningBehaviour extends CyclicBehaviour {
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);

		public void action() {
			ACLMessage msg = receive(mt);
			if(msg != null) {
				AID sender = msg.getSender();
				String[] message = msg.toString().split(" ");
				// handle ok? here
				if (message[0]=="ok?") {
					AirplaneAgent.this.agentView.put(sender, Integer.parseInt(message[1]));
					checkAgentView();
				}
			} else {
				block();
			}
		}

	}
	class RejectProposalListeningBehaviour extends CyclicBehaviour {
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
		public void action() {
			ACLMessage msg = receive(mt);
			if(msg != null) {
				System.out.println(msg);
				String[] message = msg.toString().split(" ");
				AID sender = msg.getSender();
				// handle nogood here
				if (message[0]=="nogood") {
					HashMap<Integer,TreeSet<AID>> nogoods = AirplaneAgent.this.nogoods;
					for (int i = 1; i < message.length; i++) {
						Integer time = Integer.parseInt(message[i]);
						if (nogoods.containsKey(time)) {
							nogoods.get(time).add(sender);
						} else {
							TreeSet<AID> t = new TreeSet<AID>();
							t.add(sender);
							nogoods.put(time, t);
						}
					}
				}
			} else {
				block();
			}
		}

	}
	

	
}
