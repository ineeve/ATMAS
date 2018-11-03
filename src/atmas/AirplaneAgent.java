package atmas;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import utils.Logger;

public class AirplaneAgent extends Agent {
	
	private int id;
	private HashMap<Integer,Integer> agentView; //the state of the world recognized by this agent
	private ArrayList<AirportAgent> airports;
	private AirportAgent currentAirport;
	private Integer value;
	private Set<Integer> originalDomain;
	private Set<Integer> currentDomain;
	
	
	public AirplaneAgent(int id, ArrayList<AirportAgent> airports, AirportAgent origin) {
		this.id = id;
		this.airports = airports;
		this.currentAirport = origin;
		originalDomain = new TreeSet<Integer>();
		currentDomain = new TreeSet<Integer>();
		agentView = new HashMap<Integer, Integer>();
		
	}
	
	private void resetState() {
		agentView = new HashMap<Integer, Integer>();
		currentDomain.addAll(originalDomain);
		value = null;
	}
	
	public void setup() {
		addBehaviour(new ConnectToAirportBehaviour());
		addBehaviour(new SetDomainBehaviour());
		addBehaviour(new OkListeningBehaviour());
		addBehaviour(new ResetBehaviour());
		addBehaviour(new StartBehaviour());
	}
	public void takeDown() {
		 Logger.printErrMsg(getAID(),"takedown");
	}
	public int getId() {
		return id;
	}
	
	private boolean chooseNewValue() {
		if (currentDomain.size() == 0) return false;
		value = currentDomain.iterator().next();
		Logger.printMsg(getAID(), "updated value to " + value);
		return true;
	}
	
	/**
	 * 
	 * @return HashSet with AIDs of all lower priority agents connected to currentAirport
	 */
	private HashSet<AID> getLowerPriorityAgents(String replyWith) {
		
		MessageTemplate mt = MessageTemplate.MatchProtocol(M_Agents.protocol);
		ACLMessage agentsACLMsg = blockingReceive(mt);
		
		M_Agents agentsMsg;
		try {
			agentsMsg = (M_Agents) agentsACLMsg.getContentObject();
		} catch (UnreadableException e) {
			e.printStackTrace();
			return null;
		}
		return agentsMsg.getAgents();
	}
	
	/**
	 * 
	 * @param N Number of lower priority agents to request. 0 gets all of them.
	 * @return Reply with
	 */
	private String requestLowerPriorityAgents(int N) {
		M_RequestAgents requestAgentsMsg = new M_RequestAgents(id, N);
		ACLMessage msg = new ACLMessage(M_RequestAgents.performative);
		msg.addReceiver(currentAirport.getAID());
		msg.setProtocol(M_RequestAgents.protocol);
		msg.setReplyWith(requestAgentsMsg.getReplyWith());
		try {
			msg.setContentObject(requestAgentsMsg);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		send(msg);
		return requestAgentsMsg.getReplyWith();
	}
	
	
	private void sendOkMessage() {
		ACLMessage aclOkMessage = new ACLMessage(M_Ok.perfomative);
		try {
			aclOkMessage.setProtocol(M_Ok.protocol);
			aclOkMessage.setContentObject(new M_Ok(id, value));
		} catch (IOException e) {
			Logger.printErrMsg(getAID(), e.getMessage());
			return;
		}
		String replyWith = requestLowerPriorityAgents(0);
		HashSet<AID> lowerAgents = getLowerPriorityAgents(replyWith);
		if (lowerAgents != null) {
			for (AID aid : lowerAgents) {
				aclOkMessage.addReceiver(aid);
			}
		}
		send(aclOkMessage);
	}

	private void sendResetDone(AID higherPriorityAgent) {
		ACLMessage resetDoneMessage = new ACLMessage(M_ResetDone.performative);
		resetDoneMessage.setProtocol(M_ResetDone.protocol);
		resetDoneMessage.addReceiver(higherPriorityAgent);
		send(resetDoneMessage);
	}
	
	public void parseOkMessage(M_Ok okMessage) {
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
		String replyWith = requestLowerPriorityAgents(1);
		HashSet<AID> lowerPriorityAgents = getLowerPriorityAgents(replyWith);
		if (lowerPriorityAgents != null) {
			AID child = lowerPriorityAgents.iterator().next(); // there's only 1
			if (child != null) {
				ACLMessage msg = new ACLMessage(M_Reset.performative);
				msg.setProtocol(M_Reset.protocol);
				msg.addReceiver(child);
				send(msg);
				return true;
			}
		}
		return false;
		
	}
	
	class OkListeningBehaviour extends CyclicBehaviour {
		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(M_Ok.perfomative), MessageTemplate.MatchProtocol(M_Ok.protocol));

		public void action() {
			ACLMessage msg = receive(mt);
			if(msg != null) {
				try {
					M_Ok okMessage = (M_Ok) msg.getContentObject();
					parseOkMessage(okMessage);
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
			} else {
				block();
			}
		}

	}
	
	
	
	class ResetBehaviour extends CyclicBehaviour {
		
		MessageTemplate mtReset = MessageTemplate.and(
				MessageTemplate.MatchPerformative(M_Reset.performative),
				MessageTemplate.MatchProtocol(M_Reset.protocol));
		
		MessageTemplate mtResetDone = MessageTemplate.and(
				MessageTemplate.MatchPerformative(M_ResetDone.performative),
				MessageTemplate.MatchProtocol(M_ResetDone.protocol));
		
		boolean resetReceived = false;
		AID agentWhoAskedReset;
		
		@Override
		public void action() {
			if (!resetReceived) {
				ACLMessage resetMsg = receive(mtReset);
				if (resetMsg != null) {
					agentWhoAskedReset = resetMsg.getSender();
					resetReceived = true;
					if (!sendResetToChild()) {
						resetState();
						sendResetDone(agentWhoAskedReset);
					}
				} else {
					block();
				}
			}
			
			if (resetReceived) {
				// reset was received and sent, now wait for response
				ACLMessage resetDoneMsg = receive(mtResetDone);
				if (resetDoneMsg != null) {
					reset();
					sendResetDone(agentWhoAskedReset);
				} else {
					block();
				}
			}
		}
	}
		
	class StartBehaviour extends CyclicBehaviour {

		MessageTemplate mt = MessageTemplate.and(
				MessageTemplate.MatchPerformative(M_Start.performative),
				MessageTemplate.MatchProtocol(M_Start.protocol));
		
		@Override
		public void action() {
			ACLMessage startMsg = receive(mt);
			if (startMsg != null) {
				Logger.printMsg(getAID(), "received start");
				chooseNewValue();
				sendOkMessage();
			} else {
				block();
			}
			
		}
	}
	
	class StopBehaviour extends CyclicBehaviour {
		MessageTemplate mt = MessageTemplate.and(
				MessageTemplate.MatchPerformative(M_Start.performative),
				MessageTemplate.MatchProtocol(M_Start.protocol));

		@Override
		public void action() {
			ACLMessage msg = receive(mt);
			if (msg != null) {
				takeDown();
			} else {
				block();
			}
			
		}
		
		
		
	}
		
	
	class SetDomainBehaviour extends Behaviour {

		boolean done = false;
		@Override
		public void action() {
			int domainSize = 10;
			// initialize domains
			for (int i = 0; i < domainSize; i++) {
				originalDomain.add(i);
				currentDomain.add(i);
			}
			done = true;
			
		}

		@Override
		public boolean done() {
			return done;
		}
		
	}
	
	class ConnectToAirportBehaviour extends Behaviour {

		boolean done = false;
		@Override
		public void action() {
			Logger.printMsg(getAID(), "Starting Connect Protocol");
			M_Connect connectMsg = new M_Connect(id, getAID());
			ACLMessage aclMsg = new ACLMessage(M_Connect.performative);
			aclMsg.setProtocol(M_Connect.protocol);
			try {
				aclMsg.setContentObject(connectMsg);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			aclMsg.addReceiver(currentAirport.getAID());
			send(aclMsg);
			done = true;
		}

		@Override
		public boolean done() {
			return done;
		}
		
	}
	
	

	
}
