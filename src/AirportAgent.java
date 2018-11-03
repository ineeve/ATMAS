import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.ResetDone;
import messages.ConnectMessage;
import messages.DisconnectMessage;
import messages.ResetMessage;
import messages.StartMessage;
import utils.Coordinates;

public class AirportAgent extends Agent {

	private Coordinates coordinates;
	// for each agentId, saves it's AID
	private TreeMap<Integer, AID> connectedAirplanes;
	private HashMap<Integer,AID> airplanesConnecting;
	private boolean reseting = false; // true if airport is in the middle of a reset protocol
	
	public AirportAgent(long l, long m){
		this.coordinates = new Coordinates(l,m);
		connectedAirplanes = new TreeMap<Integer,AID>();
		airplanesConnecting = new HashMap<Integer,AID>();
	}
	public void setup() {
		 addBehaviour(new RequestListeningBehaviour());
	}
	public void takeDown() {
		 System.out.println(getLocalName() + ": done working.");
	}
	
	public Coordinates getCoordinates() {
		return this.coordinates;
	}
	public void addAirplaneAgent(ConnectMessage connectMsg) {
		connectedAirplanes.put(connectMsg.getAgentId(), connectMsg.getAgentAID());
	}
	public void removeAirplaneAgent(DisconnectMessage msg) {
		connectedAirplanes.remove(msg.getAgentId());
	}
	
	/**
	 * Obtains agent which has lower priority than id
	 * @param id
	 * @return AID of agent with lower priority than id
	 */
	public AID getNextAgent(int id) {
		Integer nextId = connectedAirplanes.higherKey(id);
		if (nextId == null) return null;
		return connectedAirplanes.get(nextId);
	}
	
	public ArrayList<AID> getLowerPriorityAgents(int id) {
		ArrayList<AID> a = new ArrayList<AID>();
		connectedAirplanes.tailMap(id+1).forEach((k,aid) -> {
			a.add(aid);
		});
		return a;
	}
	public AID getLowerPriorityAirplane() {
		return connectedAirplanes.lastEntry().getValue();
	}
	
	private void parseConnectMessage(ConnectMessage msg) {
		airplanesConnecting.put(msg.getAgentId(), msg.getAgentAID());
		if (!reseting) {
			reseting = true;
			addBehaviour(new InitiateResetProtocol());
		}
	}
	
	private void waitForACK() {
		addBehaviour(new WaitACKBehaviour());
	}

	
	public class InitiateResetProtocol extends Behaviour {

		@Override
		public void action() {
			ResetMessage resetMessage = new ResetMessage();
			ACLMessage aclMessage = new ACLMessage(ResetMessage.performative);
			aclMessage.addReceiver(connectedAirplanes.firstEntry().getValue());
			aclMessage.setProtocol("P_RESET");
			send(aclMessage);
			
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchProtocol("P_RESET"), MessageTemplate.MatchPerformative(ResetDone.performative));
			
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
	
	/**
	 * Sends a start message to the agent with most priority
	 */
	public void sendStartMessage() {
		AID mostPriorityAgent = connectedAirplanes.firstEntry().getValue();
		StartMessage startMsg = new StartMessage();
		ACLMessage aclMessage = new ACLMessage(StartMessage.performative);
		try {
			aclMessage.setContentObject(startMsg);
			aclMessage.addReceiver(mostPriorityAgent);
			send(aclMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public class RequestListeningBehaviour extends CyclicBehaviour{
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
		@Override
		public void action() {
			ACLMessage msg = receive(mt);
			if(msg != null) {
				try {
					ConnectMessage connectMsg = (ConnectMessage) msg.getContentObject();
					parseConnectMessage(connectMsg);
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
			}
			
		}	
	}
	
	public class WaitACKBehaviour extends Behaviour{

		int requiredACKs = connectedAirplanes.size() - 1; // -1 because of the new airplane that just connected
		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ResetDone.performative);
			ACLMessage msg = receive(mt);
			if(msg != null) {
				try {
					Serializable serMessage = (ResetDone) msg.getContentObject();
					if (serMessage instanceof ResetDone) {
						if (--requiredACKs == 0) {
							sendStartMessage();
						}
					} else {
						printErrMessage("Expecting ACK Message");
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				block();
			}
		}

		@Override
		public boolean done() {
			return requiredACKs == 0;
		}
	}
	
	private void printErrMessage(String msg) {
		System.err.println(getAID().getLocalName() + ": " + msg);
	}
}
