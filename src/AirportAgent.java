import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import FIPA.FipaMessage;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.M_ResetDone;
import messages.M_Connect;
import messages.M_Disconnect;
import messages.M_Reset;
import messages.M_Start;
import utils.Coordinates;
import utils.Logger;

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
		 addBehaviour(new ConnectListeningBehaviour());
	}
	public void takeDown() {
		 System.out.println(getLocalName() + ": done working.");
	}
	
	public Coordinates getCoordinates() {
		return this.coordinates;
	}
	public void addAirplaneAgent(M_Connect connectMsg) {
		connectedAirplanes.put(connectMsg.getAgentId(), connectMsg.getAgentAID());
	}
	public void removeAirplaneAgent(M_Disconnect msg) {
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
	
	private void parseConnectMessage(M_Connect msg) {
		airplanesConnecting.put(msg.getAgentId(), msg.getAgentAID());
		Logger.printMsg(getAID(), "Airplane " + msg.getAgentId() + " is trying to connect");
		if (!reseting) {
			reseting = true;
			addBehaviour(new InitiateResetProtocol());
		}
	}
	
	private void processResetDone() {
		connectedAirplanes.putAll(airplanesConnecting);
		airplanesConnecting.clear();
		reseting = false;
		sendStartMessage();
	}
	
	public class InitiateResetProtocol extends Behaviour {

		MessageTemplate mt = MessageTemplate.and(
				MessageTemplate.MatchProtocol(M_ResetDone.protocol),
				MessageTemplate.MatchPerformative(M_ResetDone.performative));
		boolean resetSent = false;
		boolean done = false;
		
		@Override
		public void action() {
			if (connectedAirplanes.size() == 0) {
				processResetDone();
				done = true;
				return;
			}
			if (!resetSent) {
				System.out.println("Airport Sending Reset");
				ACLMessage aclMessage = new ACLMessage(M_Reset.performative);
				aclMessage.addReceiver(connectedAirplanes.firstEntry().getValue());
				aclMessage.setProtocol(M_Reset.protocol);
				send(aclMessage);
				resetSent = true;
			}
			
			// wait for ResetDone
			ACLMessage aclReceived = receive(mt);
			if (aclReceived != null) {
				processResetDone();
				done = true;
			} else {
				System.err.println("Null aclReceived");
				block();
				
			}
		}

		@Override
		public boolean done() {
			return done;
		}
		
	}
	
	/**
	 * Sends a start message to the agent with most priority
	 */
	public void sendStartMessage() {
		AID mostPriorityAgent = connectedAirplanes.firstEntry().getValue();
		M_Start startMsg = new M_Start();
		ACLMessage aclMessage = new ACLMessage(M_Start.performative);
		try {
			aclMessage.setProtocol(M_Start.protocol);
			aclMessage.setContentObject(startMsg);
			aclMessage.addReceiver(mostPriorityAgent);
			send(aclMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public class ConnectListeningBehaviour extends CyclicBehaviour{
		MessageTemplate mt = MessageTemplate.and(
				MessageTemplate.MatchPerformative(M_Connect.performative),
				MessageTemplate.MatchProtocol(M_Connect.protocol));
		@Override
		public void action() {
			ACLMessage msg = receive(mt);
			if(msg != null) {
				try {
					M_Connect connectMsg = (M_Connect) msg.getContentObject();
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
			MessageTemplate mt = MessageTemplate.MatchPerformative(M_ResetDone.performative);
			ACLMessage msg = receive(mt);
			if(msg != null) {
				try {
					Serializable serMessage = (M_ResetDone) msg.getContentObject();
					if (serMessage instanceof M_ResetDone) {
						if (--requiredACKs == 0) {
							sendStartMessage();
						}
					} else {
						Logger.printErrMsg(getAID(),"Expecting ACK Message");
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
}
