import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.*;

public class AirplaneAgent extends Agent {
	
	private int id;
	private HashMap<Integer,Integer> agentView; //the state of the world recognized by this agent
	private ArrayList<AirportAgent> airports;
	private AirportAgent currentAirport;
	private Integer value;
	private Set<Integer> originalDomain;
	private Set<Integer> currentDomain;
	private AID agentWhoAskedForReset;
	
	
	public AirplaneAgent(int id, ArrayList<AirportAgent> airports, AirportAgent origin) {
		this.id = id;
		this.airports = airports;
		this.currentAirport = origin;
		originalDomain = new TreeSet<Integer>();
		currentDomain = new TreeSet<Integer>();
		agentView = new HashMap<Integer, Integer>();
		
	}
	
	private void reset() {
		agentView = new HashMap<Integer, Integer>();
		currentDomain.clear();
		currentDomain.addAll(originalDomain);
		value = null;
	}
	
	public void setup() {
		 addBehaviour(new InitializeBehaviour());
		 addBehaviour(new OkListeningBehaviour());
	}
	public void takeDown() {
		 System.out.println(getLocalName() + ": done working.");
	}
	public int getId() {
		return id;
	}
	
	public void printErr(String msg) {
		System.err.println("airplane " + id + ": " + msg);
	}
	
	private boolean chooseNewValue() {
		if (currentDomain.size() == 0) return false;
		value = currentDomain.iterator().next();
		System.out.println(id + " - updated value to " + value);
		return true;
	}
	
	private void sendOkMessage() {
		ACLMessage aclMessage = new ACLMessage(OkMessage.perfomative);
		try {
			aclMessage.setContentObject(new OkMessage(id, value));
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return;
		}
		ArrayList<AID> lowerPAgents = currentAirport.getLowerPriorityAgents(id); // Replace by a message protocol
		for (AID aid : lowerPAgents) {
			aclMessage.addReceiver(aid);
		}
		send(aclMessage);
	}

	private void sendResetDone(AID higherPriorityAgent) {
		ACLMessage resetDoneMessage = new ACLMessage(ResetDone.performative);
		try {
			resetDoneMessage.setContentObject(new ResetDone());
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return;
		}
		resetDoneMessage.addReceiver(higherPriorityAgent);
		send(resetDoneMessage);
	}
	
	public void parseOkMessage(OkMessage okMessage) {
		Integer senderPrevValue = agentView.put(okMessage.getAgentId(), okMessage.getValue());
		if (senderPrevValue == okMessage.getValue()) {
			// no update was done, do nothing
			return;
		}
		currentDomain.remove(okMessage.getValue());
		if (value == null || !currentDomain.contains(value)) {
			if (chooseNewValue()) {
				// send ok to lower priority agents
				sendOkMessage();
			}
		}
	}
	
	/**
	 * 
	 * @return True if reset was send to child, false if there was no child
	 */
	private boolean sendResetToChild() {
		AID child = currentAirport.getNextAgent(id);
		if (child != null) {
			ResetMessage resetMessage = new ResetMessage();
			ACLMessage msg = new ACLMessage(ResetMessage.performative);
			try {
				msg.setContentObject(resetMessage);
				msg.addReceiver(child);
				send(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}
		return false;
		
	}
	
	class OkListeningBehaviour extends CyclicBehaviour {
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);

		public void action() {
			ACLMessage msg = receive(mt);
			if(msg != null) {
				try {
					OkMessage okMessage = (OkMessage) msg.getContentObject();
					parseOkMessage(okMessage);
				} catch (UnreadableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				block();
			}
		}

	}
	
	public class InformListeningBehaviour extends CyclicBehaviour {
		
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		@Override
		public void action() {
			ACLMessage msg = receive(mt);
			if (msg != null) {
				try {
					Serializable receivedMsg = msg.getContentObject();
					if (receivedMsg instanceof StartMessage) {
						
					} else if (receivedMsg instanceof StopMessage) {
						
					} else if (receivedMsg instanceof ResetDone){
						reset();
						sendResetDone(agentWhoAskedForReset);
					} else {
						printErr("Invalid inform message");
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
	}
	
	
	
	public class PropagateListeningBehaviour extends CyclicBehaviour {
		
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE);
		@Override
		public void action() {
			ACLMessage msg = receive(mt);
			if (msg != null) {
				Serializable receivedMsg;
				try {
					agentWhoAskedForReset = msg.getSender();
					receivedMsg = msg.getContentObject();
					if (receivedMsg instanceof ResetMessage) {
						if (!sendResetToChild()) {
							reset();
							sendResetDone(msg.getSender());
						}
					} else {
						printErr("Invalid propagate message");
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}	
	}
		
	
	public class InitializeBehaviour extends Behaviour {

		@Override
		public void action() {
			int domainSize = 10;
			// initialize domains
			for (int i = 0; i < domainSize; i++) {
				originalDomain.add(i);
				currentDomain.add(i);
			}
			// choose new value
			chooseNewValue();
			sendOkMessage();
			
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
	
	

	
}
