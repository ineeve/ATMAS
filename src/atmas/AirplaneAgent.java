package atmas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import jade.core.AID;
import sajas.core.Agent;
import sajas.core.behaviours.Behaviour;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.*;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import utils.Logger;
import utils.AgentViewValue;
import utils.AirplaneStatus;
import utils.AirportLocator;
import utils.AirportWrapper;
import utils.Nogood;

public class AirplaneAgent extends Agent {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	private int id;
	private TreeMap<Integer, AgentViewValue> agentView; //the state of the world recognized by this agent
	private ArrayList<AirportWrapper> airports;
	private AirportWrapper currentAirport;
	private TreeMap<Integer,AID> agentsInAirport;
	private Integer value;
	private TreeSet<Integer> originalDomain;
	private TreeSet<Integer> currentDomain;
	
	private boolean isABTRunning = false;
	
	// Grid units / tick.
	private int minSpeed = 20 / JADELauncher.TICKS_PER_HOUR;
	private int maxSpeed = 50 / JADELauncher.TICKS_PER_HOUR;
	private int realSpeed = minSpeed;
	
	public static final int maxFuelHours = 14;
	private int fuelRemaining = maxFuelHours * JADELauncher.TICKS_PER_HOUR; // ticks of fuel remaining
	
	private AirplaneStatus status = AirplaneStatus.PARKED;
	private boolean parkedIdle = true;
	
	private double emergencyChance;
	
	/**
	 * 
	 * @param space
	 * @param grid
	 * @param id
	 * @param airports
	 * @param origin
	 * @param emergencyChance Null for default emergency value.
	 */
	public AirplaneAgent(ContinuousSpace<Object> space, Grid<Object> grid, int id, ArrayList<AirportWrapper> airports, AirportWrapper origin, Double emergencyChance) {
		this.space = space;
		this.grid = grid;
		this.id = id;
		this.airports = airports;
		this.currentAirport = origin;
		originalDomain = new TreeSet<Integer>();
		currentDomain = new TreeSet<Integer>();
		agentView = new TreeMap<Integer, AgentViewValue>();
		agentsInAirport = new TreeMap<Integer,AID>();

		this.emergencyChance = (emergencyChance != null ? emergencyChance : Math.pow(10, -6));
	}
	
	private void resetState() {
		agentView = new TreeMap<Integer, AgentViewValue>();
		currentDomain.addAll(originalDomain);
		value = null;
	}
	
	@Override
	public void setup() {
		addBehaviour(new OkListeningBehaviour());
		addBehaviour(new NogoodListeningBehaviour());
		addBehaviour(new SetDomainBehaviour());
		addBehaviour(new ConnectToAirportBehaviour());
		addBehaviour(new ConnectListeningBehaviour());
		addBehaviour(new StopBehaviour());
	}
	
	@Override
	public void takeDown() {
		 Logger.printErrMsg(getAID(),"takedown");
	}
	
	/**
	 * Each simulated hour, there's a chance this in-progress flight gets in an emergency state.
	 */
	@ScheduledMethod(start = JADELauncher.TICKS_PER_HOUR, interval = JADELauncher.TICKS_PER_HOUR)
	public void emergency() {
		if (status == AirplaneStatus.FLIGHT && RandomHelper.nextDoubleFromTo(0, 1) <= emergencyChance) {
			activateEmergency();
		}
	}
	
	/**
	 * Activate emergency state.
	 * Airplane will try to land on the closest airport with maximum priority.
	 */
	private void activateEmergency() {
		// find closest airport
		currentAirport = AirportLocator.getClosest(space, space.getLocation(this), airports);
		// connect to it
		// TODO: Do not reconnect when it already is the closest.
		addBehaviour(new ConnectToAirportBehaviour());
		
		// regular scheduled move function will then land it
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void moveTowardsAirport() {
		double currentTick = RepastEssentials.GetTickCount();
		switch (status) {
		case PARKED: // must schedule and do takeoff
			if (parkedIdle) {
				addBehaviour(new ConnectToAirportBehaviour());
			} else if (isABTRunning) {
				return;
			} else if (currentTick >= value) { // takeoff at picked time
				status = AirplaneStatus.BLIND_FLIGHT;
				// start algorithm in destiny airport
				currentAirport = chooseNewDestiny();
				addBehaviour(new ConnectToAirportBehaviour());
			}
			break;
		case BLIND_FLIGHT: // must await landing scheduling and start travel
			if (isABTRunning) {
				return;
			} else {
				status = AirplaneStatus.FLIGHT;
			}
			break;
		case FLIGHT: // must travel to and land in airport
			GridPoint pt = currentAirport.getGridPoint();
			if (pt.equals(grid.getLocation(this))) {
				if (currentTick >= value) { // land at picked time
					status = AirplaneStatus.PARKED;
					parkedIdle = true;
				}
			} else {
				NdPoint myPoint = space.getLocation(this);
				NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
				double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
				double speed = Math.min(realSpeed, space.getDistance(myPoint, otherPoint)); // do not overshoot
				space.moveByVector(this, speed, angle, 0);
				myPoint = space.getLocation(this);
				grid.moveTo(this, (int) myPoint.getX(), (int) myPoint.getY());
				updateDomain();
			}
			break;
		}
	}

	private void updateDomain() {
		GridPoint myPoint = grid.getLocation(this);
		GridPoint otherPoint = currentAirport.getGridPoint();
		double distance = grid.getDistance(myPoint, otherPoint);
		int minTick = (int) Math.ceil(distance / maxSpeed);
		int maxTick = fuelRemaining;
		
		originalDomain = (TreeSet<Integer>) originalDomain.subSet(minTick, maxTick);
		currentDomain = (TreeSet<Integer>) currentDomain.subSet(minTick, maxTick);
	}

	private AirportWrapper chooseNewDestiny() {
		int airportIndex;
		do {
			airportIndex = RandomHelper.nextIntFromTo(0, airports.size() - 1);
		} while (currentAirport.equals(airports.get(airportIndex))); // do not travel to same airport
		return airports.get(airportIndex);
	}
	
	public int getId() {
		return id;
	}
	
	private TreeMap<Integer,AID> getLowerPriorityAgents() {
		TreeMap<Integer,AID> t = new TreeMap<Integer,AID>();
		t.putAll(agentsInAirport.tailMap(id+1));
		return t;
	}
	
	private boolean chooseNewValue() {
		if (currentDomain.size() == 0) return false;
		int randomIndex = (int) (Math.random() * currentDomain.size());
		do {
			value = currentDomain.iterator().next();
			randomIndex--;
		} while (randomIndex >= 0);
		Logger.printMsg(getAID(), "updated value to " + value);
		return true;
	}
	
	/**
	 * Send ok message to all lower priority agents
	 */
	private void sendOkMessage() {
		if (value == null) {
			chooseNewValue();
		}
		TreeMap<Integer, AID> lowerPriorityAgents = getLowerPriorityAgents();
		if (lowerPriorityAgents != null && lowerPriorityAgents.size() > 0) {
			//Logger.printMsg(getAID(), "Sending ok " + value + " to " + lowerAgents.size() + " agents");
			ACLMessage aclOkMessage = new ACLMessage(M_Ok.perfomative);
			try {
				aclOkMessage.setProtocol(M_Ok.protocol);
				aclOkMessage.setContentObject(new M_Ok(id, value));
			} catch (IOException e) {
				Logger.printErrMsg(getAID(), e.getMessage());
				return;
			}
			
			for (AID aid : lowerPriorityAgents.values()) {
				aclOkMessage.addReceiver(aid);
			}			
			send(aclOkMessage);
		}
	}
	/**
	 * Send ok message to a specific agent
	 * @param agentAID AID of agent to which the message should be sent
	 */
	private void sendOkMessage(AID agentAID) {
		ACLMessage aclOkMessage = new ACLMessage(M_Ok.perfomative);
		try {
			aclOkMessage.setProtocol(M_Ok.protocol);
			aclOkMessage.setContentObject(new M_Ok(id, value));
		} catch (IOException e) {
			Logger.printErrMsg(getAID(), e.getMessage());
			return;
		}
		aclOkMessage.addReceiver(agentAID);
		send(aclOkMessage);
	}
	
	public void parseOkMessage(M_Ok okMessage, AID sender) {
		isABTRunning = true;
		AgentViewValue avv = new AgentViewValue(okMessage.getValue(), sender);
		Logger.printMsg(getAID(), "Received ok " + okMessage.getValue() + " from " + sender.getLocalName());
		// check if new value is compatible with other agent values in the agentview
		AgentViewValue prevValue = agentView.put(okMessage.getAgentId(), avv);
		if (prevValue != null) {
			if (!agentView.containsValue(prevValue)) { //there could be another agent which had the previous value
				currentDomain.add(prevValue.getValue()); //Adds the previous value of the agent to this agent available domain
			}
		}
		tryToGetNewValue();
		if (agentsInAirport.tailMap(id+1).size() == 0) {
			// if I am the element with less priority, check for ABT end (success).
			checkABTSuccess();
		}
	}
	
	private void checkABTSuccess() {
		HashSet<Integer> h = new HashSet<Integer>();
		if (value == null) return;
		if (agentView.size() != agentsInAirport.size()) return;
		h.add(value);
		for (AgentViewValue avv : agentView.values()) {
			Integer agentValue = avv.getValue();
			if (agentValue == null || h.add(agentValue) == false) {
				return; // ABT has not ended yet
			}
		}
		isABTRunning = false;
		addBehaviour(new InformABTEnd());
		Logger.printMsg(getAID(), "ABT ended");
	}
	
	
	class ListenABTEnd extends CyclicBehaviour {
		
		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(M_ABTEnd.performative), MessageTemplate.MatchProtocol(M_ABTEnd.protocol));
		
		@Override
		public void action() {
			ACLMessage aclMsg = receive(mt);
			if (aclMsg != null) {
				isABTRunning = false;
			} else {
				block();
			}
			
		}
		
	}
	
	class InformABTEnd extends OneShotBehaviour {

		@Override
		public void action() {
			ACLMessage aclMsg = new ACLMessage(M_ABTEnd.performative);
			aclMsg.setProtocol(M_ABTEnd.protocol);
			for (AID airplaneAID : agentsInAirport.values()) {
				aclMsg.addReceiver(airplaneAID);
			}
			send(aclMsg);	
		}
		
	}
	
	private void tryToGetNewValue() {
		// Remove from current domain all values that were defined by agents with bigger or equal priority to the sender of this ok message
		
		agentView.values().forEach(v -> {
			currentDomain.remove(v.getValue());
		});
		if (value == null || !currentDomain.contains(value)) {
			if (chooseNewValue()) {
				sendOkMessage(); // send ok to lower priority agents
			} else {
				backtrack();
			}
		}
	}
	
	private void backtrack() {
		// current agent view is a nogood
		TreeMap<Integer, AID> lowerPriorityAgents = getLowerPriorityAgents();
		if (agentView.size() == 0) { // agentView.size == 0 means that I am the agent with most priority
			// TERMINATE, Broadcast stop message
			Logger.printMsg(getAID(), "No solution found");
			ACLMessage aclStopMsg = new ACLMessage(M_Stop.performative);
			aclStopMsg.setProtocol(M_Stop.protocol);
			Logger.printMsg(getAID(), "sending stop to " + lowerPriorityAgents.size() + " agents");
			for(AID agent : lowerPriorityAgents.values()) {
				aclStopMsg.addReceiver(agent);
			}
			send(aclStopMsg);
			takeDown();
			return;
		}
		// Check if last Entry is equal to parent. If it is not, then there's no need to send the nogood
		AID receiver = agentView.lastEntry().getValue().getAID();
		
		Entry<Integer,AID> parentEntry = agentsInAirport.floorEntry(id - 1);
		if (parentEntry != null) {
			if (!receiver.equals(parentEntry.getValue())) {
				return;
			}
		}
				
		currentDomain.addAll(originalDomain); //when sending a nogood, reset currentDomain
		
		// Prepare nogood Message
		M_Nogood nogoodMsg = new M_Nogood();
		agentView.forEach((agentId,agentValue) -> {
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
		//Logger.printMsg(getAID(), "Sent nogood to " + receiver.getLocalName());
	}
	
	private void parseNogoodMsg(M_Nogood nogoodMsg) {
		isABTRunning = true;
		// check if nogood is consistent with agentview
		// this is an optimization to prevent unnecessary messages
		Logger.printMsg(getAID(), "Received nogood");
		HashSet<Nogood> nogoods = nogoodMsg.getNogoods();
		AgentViewValue myNogood = null;
		for (Nogood nogood : nogoods) {
			if (nogood.getAgentId().equals(id)) {
				myNogood = nogood.getValue();
			}
		}
		if (!myNogood.getValue().equals(value)) {
			Logger.printMsg(getAID(), "Ignoring nogood, reason: " + myNogood.getValue() + " " + value);
			return;
		}
		
		//Logger.printMsg(getAID(), "nogood consistent with agent view");
		// set my value to null
		value = null;
		// update currentDomain
		currentDomain.remove(myNogood.getValue());
		// try to get new value
		tryToGetNewValue();
		// if I am not the most priority Agent, add myNogood again to domain		
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
			GridPoint myPoint = grid.getLocation(myAgent);
			GridPoint otherPoint = currentAirport.getGridPoint();
			double distance = grid.getDistance(myPoint, otherPoint);
			int minTick = (int) Math.ceil(distance / maxSpeed);
			int maxTick = fuelRemaining;
			// initialize domains
			for (int i = minTick; i <= maxTick; i++) {
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
		int state = 0;
		MessageTemplate mtAgents = MessageTemplate.MatchPerformative(M_Agents.performative);

		@Override
		public void action() {
			parkedIdle = false;
			switch (state) {
			case 0:
				// Request airplanes to airport
				Logger.printMsg(getAID(), "Starting Connect Protocol");
				M_RequestAgents requestAgentsMsg = new M_RequestAgents(id);
				String replyWith = requestAgentsMsg.getReplyWith();
				ACLMessage requestAgentsACL = new ACLMessage(M_RequestAgents.performative);
				requestAgentsACL.setProtocol(M_RequestAgents.protocol);
				try {
					requestAgentsACL.setContentObject(requestAgentsMsg);
				} catch (IOException e1) {
					e1.printStackTrace();
					return;
				}
				requestAgentsACL.setReplyWith(replyWith);
				requestAgentsACL.addReceiver(currentAirport.getAID());
				send(requestAgentsACL);
				
				state = 1;
				break;
			case 1:
				// Get airplanes in airport
				
				ACLMessage agentsACLMsg = receive(mtAgents);
				if (agentsACLMsg != null) {
					M_Agents agentsMsg;
					try {
						agentsMsg = (M_Agents) agentsACLMsg.getContentObject();
						agentsInAirport.putAll(agentsMsg.getAgents());
					} catch (UnreadableException e) {
						e.printStackTrace();
						return;
					}
					state = 2;
				} else {
					block();
				}
					break;
				case 2:
					// Connect to other airplanes
					M_Connect connectObject = new M_Connect(id);
					ACLMessage connectACL = new ACLMessage(M_Connect.performative);
					connectACL.setProtocol(M_Connect.protocol);
					try {
						connectACL.setContentObject(connectObject);
					} catch (IOException e) {
						e.printStackTrace();
						return;
					}
					agentsInAirport.forEach( (k,aid) -> {					
						connectACL.addReceiver(aid);
					});
					send(connectACL);
					state=3;
					break;
				case 3:
					tryToGetNewValue();
					done = true;
					state++;
				default:
					return;
			}		
			
		}

		@Override
		public boolean done() {
			return done;
		}
		
	}
	
	public class ConnectListeningBehaviour extends CyclicBehaviour {
		MessageTemplate mt = MessageTemplate.and(
				MessageTemplate.MatchPerformative(M_Connect.performative),
				MessageTemplate.MatchProtocol(M_Connect.protocol));
		@Override
		public void action() {
			ACLMessage msg = receive(mt);
			if(msg != null) {
				try {
					M_Connect connectMsg = (M_Connect) msg.getContentObject();
					agentsInAirport.put(connectMsg.getAgentId(), msg.getSender());
					if (connectMsg.getAgentId() > id) {
						if (value != null) {
							sendOkMessage();
						}
						
					}
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
			} else {
				block();
			}
		}
	}

	
}
