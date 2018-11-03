import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.*;
import utils.AgentViewValue;
import utils.Logger;
import utils.Nogood;

public class AirplaneAgent extends Agent {
	
	private int id;
	private TreeMap<Integer, AgentViewValue> agentView; //the state of the world recognized by this agent
	private ArrayList<AirportAgent> airports;
	private AirportAgent currentAirport;
	private Integer value;
	private Set<Integer> originalDomain;
	private Set<Integer> currentDomain;
	private Behaviour okListeningBehaviour;
	private Behaviour nogoodListeningBehaviour;
	
	
	public AirplaneAgent(int id, ArrayList<AirportAgent> airports, AirportAgent origin) {
		this.id = id;
		this.airports = airports;
		this.currentAirport = origin;
		originalDomain = new TreeSet<Integer>();
		currentDomain = new TreeSet<Integer>();
		agentView = new TreeMap<Integer, AgentViewValue>();
		
	}
	
	private void resetState() {
		agentView = new TreeMap<Integer, AgentViewValue>();
		currentDomain.addAll(originalDomain);
		value = null;
	}
	
	public void setup() {
		okListeningBehaviour = new OkListeningBehaviour();
		nogoodListeningBehaviour = new NogoodListeningBehaviour();
		addBehaviour(new ConnectToAirportBehaviour());
		addBehaviour(new SetDomainBehaviour());
		addBehaviour(okListeningBehaviour);
		addBehaviour(nogoodListeningBehaviour);
		addBehaviour(new ResetBehaviour());
		addBehaviour(new StartBehaviour());
		addBehaviour(new StopBehaviour());
	}
	public void takeDown() {
		 Logger.printErrMsg(getAID(),"takedown");
		 removeBehaviour(okListeningBehaviour);
		 removeBehaviour(nogoodListeningBehaviour);
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
	 * @return HashSet with AIDs previously requested agents
	 */
	private HashSet<AID> getAgents(String replyWith) {
		
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
	 * @param N Number of lower priority agents to request. 0 gets all of them. -1 gets parent
	 * @return Reply with
	 */
	private String requestAgents(int N) {
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
		String replyWith = requestAgents(0);
		HashSet<AID> lowerAgents = getAgents(replyWith);
		if (lowerAgents.size() > 0) {
			//Logger.printMsg(getAID(), "Sending ok " + value + " to " + lowerAgents.size() + " agents");
			ACLMessage aclOkMessage = new ACLMessage(M_Ok.perfomative);
			try {
				aclOkMessage.setProtocol(M_Ok.protocol);
				aclOkMessage.setContentObject(new M_Ok(id, value));
			} catch (IOException e) {
				Logger.printErrMsg(getAID(), e.getMessage());
				return;
			}
			
			
		
			if (lowerAgents != null) {
				for (AID aid : lowerAgents) {
					aclOkMessage.addReceiver(aid);
				}
			}
			send(aclOkMessage);
		}
		
	}

	private void sendResetDone(AID higherPriorityAgent) {
		ACLMessage resetDoneMessage = new ACLMessage(M_ResetDone.performative);
		resetDoneMessage.setProtocol(M_ResetDone.protocol);
		resetDoneMessage.addReceiver(higherPriorityAgent);
		send(resetDoneMessage);
	}
	
	public void parseOkMessage(M_Ok okMessage, AID sender) {
		
		AgentViewValue avv = new AgentViewValue(okMessage.getValue(), sender);
		for (Map.Entry<Integer, AgentViewValue> viewEntry : agentView.entrySet()) {
			if (viewEntry.getValue().equals(avv)) {
				if (viewEntry.getKey() < okMessage.getAgentId()) {
					// Optimization. When this is true, the last ok message received is deprecated.
					return;
				}
			}
		}
		// check if new value is compatible with other agent values in the agentview
		AgentViewValue prevValue = agentView.put(okMessage.getAgentId(), avv);
		if (prevValue != null) {
			if (!agentView.containsValue(prevValue)) { //there could be another agent which had the previous value
				currentDomain.add(prevValue.getValue()); //Adds the previous value of the agent to this agent available domain
			}
		}
		// Remove from current domain all values that were defined by agents with bigger or equal priority to the sender of this ok message
		agentView.headMap(okMessage.getAgentId()+1).values().forEach(v -> {
			currentDomain.remove(v.getValue());
		});
		
		if (value == null || !currentDomain.contains(value)) {
			tryToGetNewValue();
		}
	}
	
	private void tryToGetNewValue() {
		if (chooseNewValue()) {
			sendOkMessage(); // send ok to lower priority agents
		} else {
			backtrack();
		}
	}
	
	private void backtrack() {
		// current agent view is a nogood
		if (agentView.size() == 0) { // agentView.size == 0 means that I am the agent with most priority
			// TERMINATE, Broadcast stop message
			Logger.printMsg(getAID(), "No solution found");
			String replyWith = requestAgents(0);
			HashSet<AID> lowerPriorityAgents = getAgents(replyWith);
			ACLMessage aclStopMsg = new ACLMessage(M_Stop.performative);
			aclStopMsg.setProtocol(M_Stop.protocol);
			Logger.printMsg(getAID(), "sending stop to " + lowerPriorityAgents.size() + " agents");
			for(AID agent : lowerPriorityAgents) {
				aclStopMsg.addReceiver(agent);
			}
			send(aclStopMsg);
			takeDown();
			return;
		}
		// Send nogood upwards
		// Check if last Entry is equal to parent
		AID receiver = agentView.lastEntry().getValue().getAID();
		String replyWith = requestAgents(-1);
		HashSet<AID> agents = getAgents(replyWith); // contains only this agent parent
		AID myParent = agents.iterator().next();
		
		
		currentDomain.addAll(originalDomain); //when sending a nogood, reset currentDomain
		if (myParent.compareTo(receiver) != 0) {
			// remove value of lower priority agent from agentview
			agentView.remove(agentView.lastKey());
			return; // no need to send nogood, am I am not the child of the lower priority agent of my agent view
		}
		
		// Prepare nogood Message
		M_Nogood nogoodMsg = new M_Nogood();
		agentView.forEach((agentId,agentValue) -> {
			if (agentId == null) {
				Logger.printErrMsg(getAID(), "Adding null agent to nogood message");
			}
			Nogood nogood = new Nogood(agentId,agentValue);	
			nogoodMsg.addNogood(nogood);
		});
		// remove value of lower priority agent from agentview
		agentView.remove(agentView.lastKey());
		
		// send nogood msg to lower priority agent in agentview
		ACLMessage nogoodACLMsg = new ACLMessage(M_Nogood.performative);
		nogoodACLMsg.setProtocol(M_Nogood.protocol);
		try {
			nogoodACLMsg.setContentObject(nogoodMsg);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		nogoodACLMsg.addReceiver(receiver);
		send(nogoodACLMsg);
		Logger.printMsg(getAID(), "Sent nogood to " + receiver.getLocalName());
	}

	/**
	 * 
	 * @return True if reset was send to child, false if there was no child
	 */
	private boolean sendResetToChild() {
		String replyWith = requestAgents(1);
		HashSet<AID> lowerPriorityAgents = getAgents(replyWith);
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
	
	private void parseNogoodMsg(M_Nogood nogoodMsg) {
		// check if nogood is consistent with agentview
		// this is an optimization to prevent unnecessary messages
		Logger.printMsg(getAID(), "Received nogood");
		boolean isConsistent = true;
		HashMap<Integer, AgentViewValue> nogoods = nogoodMsg.getNogoods();
		for (Map.Entry<Integer, AgentViewValue> nogood : nogoods.entrySet()) {
			if (nogood.getKey() != id) {
				AgentViewValue avv = agentView.get(nogood.getKey());
				if (avv == null || !avv.getValue().equals(nogood.getValue().getValue())) {
					isConsistent = false;
					break;
				}
			}
		}
		if (isConsistent) {
			Logger.printMsg(getAID(), "nogood consistent with agent view");
			// set my value to null
			value = null;
			// update currentDomain
			AgentViewValue myNogood = nogoods.get(id);
			currentDomain.remove(myNogood.getValue());
			// try to get new value
			tryToGetNewValue();
			// if I am not the most priority Agent, add myNogood again to domain
		} else {
			Logger.printErrMsg(getAID(), "nogood not consistent with agent view");
		}
		
	}
	
	class NogoodListeningBehaviour extends CyclicBehaviour {

		MessageTemplate mt = MessageTemplate.and(
				MessageTemplate.MatchPerformative(M_Nogood.performative),
				MessageTemplate.MatchProtocol(M_Nogood.protocol));
		
		@Override
		public void action() {
			ACLMessage msg = receive(mt);
			if(msg != null) {
				try {
					M_Nogood nogoodMsg = (M_Nogood) msg.getContentObject();
					parseNogoodMsg(nogoodMsg);
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
			} else {
				block();
			}
			
		}
		
	}
	
	class OkListeningBehaviour extends CyclicBehaviour {
		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(M_Ok.perfomative), MessageTemplate.MatchProtocol(M_Ok.protocol));

		public void action() {
			ACLMessage msg = receive(mt);
			if(msg != null) {
				try {
					M_Ok okMessage = (M_Ok) msg.getContentObject();
					parseOkMessage(okMessage, msg.getSender());
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
						resetReceived = false;
					}
				} else {
					block();
				}
			}
			
			if (resetReceived) {
				// reset was received and sent, now wait for response
				ACLMessage resetDoneMsg = receive(mtResetDone);
				if (resetDoneMsg != null) {
					resetState();
					sendResetDone(agentWhoAskedReset);
					resetReceived = false;
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
				MessageTemplate.MatchPerformative(M_Stop.performative),
				MessageTemplate.MatchProtocol(M_Stop.protocol));

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
			int domainSize = 5;
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
