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
		addBehaviour(new OkListeningBehaviour());
		addBehaviour(new InitializeBehaviour());
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
	
	private void sendOkMessage() {
		ACLMessage aclMessage = new ACLMessage(M_Ok.perfomative);
		try {
			aclMessage.setProtocol(M_Stop.protocol);
			aclMessage.setContentObject(new M_Ok(id, value));
		} catch (IOException e) {
			Logger.printErrMsg(getAID(), e.getMessage());
			return;
		}
		ArrayList<AID> lowerPAgents = currentAirport.getLowerPriorityAgents(id); // Replace by a message protocol
		for (AID aid : lowerPAgents) {
			aclMessage.addReceiver(aid);
		}
		send(aclMessage);
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
		AID child = currentAirport.getNextAgent(id);
		if (child != null) {
			ACLMessage msg = new ACLMessage(M_Reset.performative);
			msg.setProtocol(M_Reset.protocol);
			msg.addReceiver(child);
			send(msg);
			return true;
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
				addBehaviour(new InitializeBehaviour());
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
		
	
	class InitializeBehaviour extends Behaviour {

		boolean done = false;
		@Override
		public void action() {
			if (originalDomain.size() == 0) {
				int domainSize = 10;
				// initialize domains
				for (int i = 0; i < domainSize; i++) {
					originalDomain.add(i);
					currentDomain.add(i);
				}
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
