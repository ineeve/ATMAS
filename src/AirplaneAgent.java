import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
	private HashMap<AID,Integer> agentView; //the state of the world recognized by this agent
	public Integer desiredTime;
	public Integer minTimeDomain;
	public Integer maxTimeDomain;
	
	public AirplaneAgent(ArrayList<AirportAgent> airports, AirportAgent origin) {
		this.airports = airports;
		this.currentAirport = origin;
		this.coordinates = origin.getCoordinates();
		agentView = new HashMap<AID, Integer>();
	}
	public void setup() {
		 addBehaviour(new TakeoffBehaviour());
		 addBehaviour(new ProposeListeningBehaviour());
		 addBehaviour(new RejectProposalListeningBehaviour());
	}
	public void takeDown() {
		 System.out.println(getLocalName() + ": done working.");
	}
	
	public boolean updateMyValue() {
		for(int t = minTimeDomain; t <= maxTimeDomain; t++) {
			if (!agentView.containsValue(t)) {
				this.desiredTime = t;
				System.out.println(getLocalName() + ": my_value=" + t);
				return true;
			}
		}
		return false;
	}
	
	public ACLMessage buildOkMessage(ArrayList<AID> receivers) {
		ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
		msg.setContent("ok? " + this.desiredTime);
		for (AID a : receivers) {
			msg.addReceiver(a);
		}
		return msg;
	}
	
	public ACLMessage buildNoGoodMessage(AID receiver, HashMap<AID,Integer> nogoods) {
		ACLMessage msg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
		StringBuilder msgContent = new StringBuilder("nogood");
		for (Map.Entry<AID, Integer> entry : nogoods.entrySet()) {
			msgContent.append(" ");
			msgContent.append(entry.getKey());
			msgContent.append(" ");
			msgContent.append(entry.getValue());
		}
		msg.setContent(msgContent.toString());
		msg.addReceiver(receiver);
		return msg;
	}
	
	public void printAgentView() {
		for (Map.Entry<AID, Integer> e : agentView.entrySet()) {
			System.out.println(" agentView state:" + getLocalName() + " - " + e.getKey().getLocalName() + " : " + e.getValue());
		}
	}
	
	public void checkAgentView() {
		if (agentView.containsValue(desiredTime)) {
			boolean success;
			do {
				success = updateMyValue();
				if (!success) {
					backtrack();
				}
			} while(!success);
			ArrayList<AID> receivers = currentAirport.getLowerPriorityAirplanesAID(getAID());
			send(buildOkMessage(receivers));
				
		}
	}
	
	public HashMap<AID,Integer> buildNogoods(){
		HashMap<AID,Integer> nogoods = new HashMap<AID,Integer>();
		for (int i = minTimeDomain; i <= maxTimeDomain; i++) {
			for (Map.Entry<AID, Integer> e : agentView.entrySet())
				if (e.getValue() == i) {
					nogoods.put(e.getKey(), i);
				}
		}
		return nogoods;
	}
	
	public void backtrack() {
		// nogoods should contain only the agent view intersected with my desires
		HashMap<AID,Integer> nogoods = buildNogoods();
		if (nogoods.size() == 0) {
			// broadcast to other agents that there is no solution
			// terminate the algorithm
			System.err.print("Terminate. NO SOLUTION");
			takeDown();
			return;
		}
		AID lowerPriorityAgent = Collections.max(nogoods.keySet());
		ACLMessage msg = buildNoGoodMessage(lowerPriorityAgent, nogoods);
		send(msg);
		agentView.remove(lowerPriorityAgent);
	}

	
	class TakeoffBehaviour extends Behaviour {
	
		 public void action() {
			minTimeDomain = 0;
			maxTimeDomain = 10;
			updateMyValue();
			ArrayList<AID> lowerAgents = currentAirport.getLowerPriorityAirplanesAID(getAID());
			ACLMessage msg = buildOkMessage(lowerAgents);
			send(msg);
			 
		 }
		 public boolean done() {
			 return true;
		 }
	}

	class ProposeListeningBehaviour extends CyclicBehaviour {
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);

		public void action() {
			try {
				ACLMessage msg = receive(mt);
				if(msg != null) {
					AID sender = msg.getSender();
					System.out.println(getLocalName() + ": Received Propose From " + sender.getLocalName() + ": " + msg.getContent());
					
					String[] message = msg.getContent().split(" ");
					// handle ok? here
					if (message[0].equals("ok?")) {
						agentView.put(sender, Integer.parseInt(message[1]));
						checkAgentView();
					}
				} else {
					block();
				}
			} catch(Exception e) {
				System.err.println(e);
			}
			
		}

	}
	class RejectProposalListeningBehaviour extends CyclicBehaviour {
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
		public void action() {
			try {
				ACLMessage msg = receive(mt);
				if(msg != null) {
					System.out.println(getLocalName() + ": Received REJECT_PROPOSAL");
					String[] message = msg.getContent().split(" ");
					AID sender = msg.getSender();
					// handle nogood here
					if (message[0].equals("nogood")) {
						HashMap<AID, Integer> nogoodReceived = new HashMap<AID,Integer>();
						for (int i = 1; i < message.length; i+=2) {
							AID agentId = new AID();
							agentId.setName(message[i]);
							Integer value = Integer.parseInt(message[i+1]);
						}
						boolean compatible = true;
						AID myId = AirplaneAgent.this.getAID();
						if (nogoodReceived.containsKey(myId)) {
							if (nogoodReceived.get(myId) != AirplaneAgent.this.desiredTime) {
								compatible = false;
							}
						}
						if (compatible) {
							for(Map.Entry<AID, Integer> entry : nogoodReceived.entrySet()) {
								AID key = entry.getKey();
								Integer v = entry.getValue();
								if (agentView.containsKey(key)) {
									if (agentView.get(key) != v) {
										compatible = false;
										break;
									}
								}
							}
						}
						
						if (compatible) {
							checkAgentView();
						} else {
							// send ok?
							ArrayList<AID> receivers = new ArrayList<AID>() ;
							receivers.add(sender);
							send(buildOkMessage(receivers));
						}
					}
				} else {
					block();
				}
			} catch (Exception e) {
				System.err.println(e);
			}
			
		}

	}
	

	
}
