package atmas;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import FIPA.FipaMessage;
import jade.core.AID;
import sajas.core.Agent;
import sajas.core.behaviours.Behaviour;
import sajas.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.M_ResetDone;
import messages.M_Agents;
import messages.M_Connect;
import messages.M_Disconnect;
import messages.M_RequestAgents;
import messages.M_Reset;
import messages.M_Start;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import utils.Coordinates;
import utils.Logger;

public class AirportAgent extends Agent {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private Coordinates coordinates;
	// for each agentId, saves it's AID
	private TreeMap<Integer, AID> connectedAirplanes;
	private HashMap<Integer,AID> airplanesConnecting;
	private boolean reseting = false; // true if airport is in the middle of a reset protocol
	
	public AirportAgent(ContinuousSpace<Object> space, Grid<Object> grid, long l, long m){
		this.space = space;
		this.grid = grid;
		this.coordinates = new Coordinates(l,m);
		connectedAirplanes = new TreeMap<Integer,AID>();
		airplanesConnecting = new HashMap<Integer,AID>();
	}
	public void setup() {
		 addBehaviour(new ConnectListeningBehaviour());
		 addBehaviour(new RequestAgentsListeningBehaviour());
	}
	public void takeDown() {
		 Logger.printErrMsg(getAID(), "executing takedown");
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
	private AID getNextAgent(int id) {
		Integer nextId = connectedAirplanes.higherKey(id);
		if (nextId == null) return null;
		return connectedAirplanes.get(nextId);
	}
	
	private HashSet<AID> getLowerPriorityAgents(int id) {
		HashSet<AID> a = new HashSet<AID>();
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
	
	private HashSet<AID> processRequestAgentsMsg(M_RequestAgents requestAgentsMsg){
		Integer requesterId = requestAgentsMsg.getAgentId();
		Integer nAgentsRequested = requestAgentsMsg.getNAgentsToGet();
		HashSet<AID> agentsToSend;
		if (nAgentsRequested == 0) {
			agentsToSend = getLowerPriorityAgents(requesterId);
		} else {
			agentsToSend = new HashSet<AID>();
			agentsToSend.add(getNextAgent(requesterId));
		}
		return agentsToSend;
	}
	
	public class RequestAgentsListeningBehaviour extends CyclicBehaviour{

		MessageTemplate mt = MessageTemplate.and(
				MessageTemplate.MatchPerformative(M_RequestAgents.performative),
				MessageTemplate.MatchProtocol(M_RequestAgents.protocol));
		@Override
		public void action() {
			ACLMessage aclMessage = receive(mt);
			if (aclMessage != null) {
				Logger.printMsg(getAID(), "Received request for agents");
				M_RequestAgents requestAgentsMsg;
				try {
					requestAgentsMsg = (M_RequestAgents) aclMessage.getContentObject();
				} catch (UnreadableException e) {
					e.printStackTrace();
					return;
				}
				HashSet<AID> agentsToSend = processRequestAgentsMsg(requestAgentsMsg);
				ACLMessage reply = aclMessage.createReply();
				reply.setPerformative(M_Agents.performative);
				M_Agents agentsContent = new M_Agents(agentsToSend);
				try {
					reply.setContentObject(agentsContent);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				send(reply);
				Logger.printMsg(getAID(), "Sent reply to request for agents");
				
			} else {
				block();
			}
		}
	}
}
