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
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import utils.Logger;

public class AirportAgent extends Agent {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	// for each agentId, saves it's AID
	private TreeMap<Integer, AID> connectedAirplanes;
	
	public AirportAgent(ContinuousSpace<Object> space, Grid<Object> grid){
		this.space = space;
		this.grid = grid;
		connectedAirplanes = new TreeMap<Integer,AID>();
	}
	
	@Override
	public void setup() {
		 addBehaviour(new RequestAgentsListeningBehaviour());
	}
	
	@Override
	public void takeDown() {
		 Logger.printErrMsg(getAID(), "executing takedown");
	}
	
	public GridPoint getGridLocation() {
		return grid.getLocation(this);
	}
	
	private void processResetDone() {
		sendStartMessage();
	}
	
	public class RequestAgentsListeningBehaviour extends CyclicBehaviour{

		MessageTemplate mt = MessageTemplate.and(
				MessageTemplate.MatchPerformative(M_RequestAgents.performative),
				MessageTemplate.MatchProtocol(M_RequestAgents.protocol));
		@Override
		public void action() {
			ACLMessage aclMessage = receive(mt);
			if (aclMessage != null) {
				//Logger.printMsg(getAID(), "Received request for agents");
				M_RequestAgents requestMsg;
				try {
					requestMsg = (M_RequestAgents) aclMessage.getContentObject();
				} catch (UnreadableException e) {
					e.printStackTrace();
					return;
				}
				ACLMessage reply = aclMessage.createReply();
				reply.setPerformative(M_Agents.performative);
				M_Agents agentsContent = new M_Agents(connectedAirplanes);
				try {
					reply.setContentObject(agentsContent);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				send(reply);
				connectedAirplanes.put(requestMsg.getAgentId(), aclMessage.getSender());
				Logger.printMsg(getAID(), "Sent reply to request for agents");
				
			} else {
				block();
			}
		}
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
}
