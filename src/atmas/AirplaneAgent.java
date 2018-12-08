package atmas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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
	
	private int originalId;
	private int id;
	private TreeMap<Integer, AgentViewValue> agentView; //the state of the world recognized by this agent
	private ArrayList<AirportWrapper> airports;
	private AirportWrapper currentAirport;
	private boolean isConnectedToAirport;
	private TreeMap<Integer,AID> agentsInAirport;
	private Integer chosenTick;
	private TreeSet<Integer> originalDomain;
	private TreeSet<Integer> currentDomain;
	private boolean isReseting;
	private boolean isABTRunning = false;
	private double startTick;
	private int messageCounter;
	
	// Grid units / tick.
	private final double minSpeed = 1.0 / JADELauncher.TICKS_PER_HOUR;
	private final double maxSpeed = 3.0 / JADELauncher.TICKS_PER_HOUR;
	private double realSpeed = maxSpeed;
	
	public static final int maxFuel = 14 * JADELauncher.TICKS_PER_HOUR;
	
	private AirplaneStatus status;
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
		this.originalId = id;
		this.id = id;
		this.airports = airports;
		setAirport(origin);
		status = AirplaneStatus.PARKED;
		originalDomain = new TreeSet<Integer>();
		currentDomain = new TreeSet<Integer>();
		agentView = new TreeMap<Integer, AgentViewValue>();
		agentsInAirport = new TreeMap<Integer,AID>();
		this.isReseting = false;
		this.emergencyChance = (emergencyChance != null ? emergencyChance : Math.pow(10, -6));
		this.messageCounter = 0;
	}
	
	public boolean getIsABTRunning() {
		return isABTRunning;
	}
	
	public double getMaxSpeed() {
		return maxSpeed;
	}
	
	public double getMinSpeed() {
		return minSpeed;
	}
	
	public String getAirport() {
		return currentAirport.getAID().getLocalName();
	}
	
	private void setAirport(AirportWrapper airport) {
		currentAirport = airport;
		isConnectedToAirport = true;
	}
	
	public double getStartTick() {
		return startTick;
	}
	
	public String getOriginalDomain() {
		return originalDomain.first() + " | " + originalDomain.last();
	}
	
	public String getCurrentDomain() {
		return currentDomain.first() + " | " + currentDomain.last();
	}
	public int getMessageCounter() {
		return messageCounter;
	}
	
	public String getStatus() {
		switch (status) {
		case PARKED:
			return "PARKED";
		case BLIND_FLIGHT:
			return "BLIND_FLIGHT";
		case FLIGHT:
			return "FLIGHT";
		default:
			return "Invalid";
		}
	}
	
	public Integer getChosenTick() {
		return chosenTick;
	}
	
	private void resetState() {
		agentsInAirport.clear();
		agentView.clear();
		originalDomain.clear();
		currentDomain.clear();
		chosenTick = null;
	}
	
	@Override
	public void setup() {
		addBehaviour(new OkListeningBehaviour());
		addBehaviour(new NogoodListeningBehaviour());
		addBehaviour(new ListenForAirplanesInAirport());
		addBehaviour(new ConnectListeningBehaviour());
		addBehaviour(new StopBehaviour());
		addBehaviour(new ListenABTEnd());
		addBehaviour(new NotInAirportListeningBehaviour());
		addBehaviour(new ListenDisconnectBehaviour());
	}
	
	
	public class ListenDisconnectBehaviour extends CyclicBehaviour {
		
		MessageTemplate mtDisconnect = MessageTemplate.and(
				MessageTemplate.MatchPerformative(M_Disconnect.performative),
				MessageTemplate.MatchProtocol(M_Disconnect.protocol));
		
		@Override
		public void action() {
			
			ACLMessage disconnectACLMsg = receive(mtDisconnect);
			if (disconnectACLMsg != null) {
				messageCounter++;
				M_Disconnect disconnectMsg;
				 try {
					disconnectMsg = (M_Disconnect) disconnectACLMsg.getContentObject();
				} catch (UnreadableException e) {
					e.printStackTrace();
					return;
				}
				processDisconnect(disconnectMsg.getAgentId(), disconnectMsg.getAirport());
			} else {
				block();
			}
		}
	}

	private void processDisconnect(int disconnectedAgentId, AID airport) {
		
		if (airport.equals(currentAirport.getAID())) {
			Logger.printMsg(getAID(), currentAirport.getAID(), "Received Disconnect by airplane" + disconnectedAgentId);
			if (agentsInAirport.containsKey(disconnectedAgentId)) {
				agentsInAirport.remove(disconnectedAgentId);
				AgentViewValue prevValue = agentView.remove(disconnectedAgentId);
				if (prevValue != null) {
					boolean isAdded = addAgentPrevValueToDomain(prevValue);
					if (isAdded) {
						tryToChooseNewTick();
						return;
					}
				}
				checkABTSuccess();
			}
		}
	}
	
	
	@Override
	public void takeDown() {
		 Logger.printErrMsg(getAID(), currentAirport.getAID(),"takedown");
		 System.exit(1);
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
		Logger.printErrMsg(getAID(), currentAirport.getAID(), "EMERGENCY - FINDING CLOSEST AIRPORT");
		// find closest airport
		setAirport(AirportLocator.getClosest(space, space.getLocation(this), airports));
		// set emergency priority (random avoids emergency collisions)
		id = RandomHelper.nextIntFromTo(Integer.MIN_VALUE, -1);
		// connect to it
		// TODO: Do not reconnect when it already is the closest.
		//addBehaviour(new SetDomainBehaviour());
		isABTRunning = true;
		Logger.printErrMsg(getAID(), currentAirport.getAID(), "Emergency");
		addBehaviour(new ConnectToNewAirportBehaviour(true));
		
		// regular scheduled move function will then land it
	}
	
	@ScheduledMethod(start = 10, interval = 1)
	public void moveTowardsAirport() {
		double currentTick = RepastEssentials.GetTickCount();
		switch (status) {
		case PARKED: // must schedule and do takeoff
			if (parkedIdle) { // pre-algorithm
				parkedIdle = false;
				addBehaviour(new ConnectToNewAirportBehaviour(false)); // connect to current airport
			} else if (isABTRunning) { // during algorithm
				return;
			} else if (chosenTick == null) {
				isABTRunning = true;
			} else if (currentTick >= chosenTick) { // post-algorithm, takeoff at picked time
				status = AirplaneStatus.BLIND_FLIGHT;
				Logger.printMsg(getAID(), currentAirport.getAID(), "Taking off at " + currentTick);
				// start algorithm in destiny airport
				isABTRunning = true;
				addBehaviour(new ConnectToNewAirportBehaviour(true));
				startTick = RepastEssentials.GetTickCount();
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
			if (isABTRunning || chosenTick == null) {
				return;
			}
			if (isAtAirport()) {
				// land at picked time
				Logger.printMsg(getAID(), currentAirport.getAID(), "Landing at " + chosenTick);
				status = AirplaneStatus.PARKED;
				parkedIdle = true;
				id = originalId;
			} else {
				NdPoint myPoint = space.getLocation(this);
				GridPoint pt = currentAirport.getGridPoint();
				NdPoint airportPoint = new NdPoint(pt.getX(), pt.getY());
				double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, airportPoint);
				double distance = realSpeed * (RepastEssentials.GetTickCount() - startTick);
				double realDistance = Math.min(distance, space.getDistance(myPoint, airportPoint)); // do not overshoot
				space.moveByVector(this, realDistance, angle, 0);
				myPoint = space.getLocation(this);
				grid.moveTo(this, (int) myPoint.getX(), (int) myPoint.getY());
				updateLandingDomain();
				startTick = RepastEssentials.GetTickCount();
			}
			break;
		}
	}

	private boolean isAtAirport() {
		GridPoint pt = currentAirport.getGridPoint();
		//Logger.printErrMsg(getAID(), currentAirport.getAID(), "Distance to airport = " + grid.getDistance(pt, grid.getLocation(this)));
		return grid.getDistance(pt, grid.getLocation(this)) <= 1;
	}

	private void updateLandingDomain() {
		GridPoint myPoint = grid.getLocation(this);
		GridPoint airportPoint = currentAirport.getGridPoint();
		double distance = grid.getDistance(myPoint, airportPoint);
		/*
		int fastestEtaTicks = (int) (distance / maxSpeed);
		
		int minTick = (int)RepastEssentials.GetTickCount() +  fastestEtaTicks;
		
		for (Iterator<Integer> it = originalDomain.iterator(); it.hasNext(); ) {
			Integer elem = it.next();
			if (elem < minTick) {
				it.remove();
			}
		}
		for (Iterator<Integer> it = currentDomain.iterator(); it.hasNext(); ) {
			Integer elem = it.next();
			if (elem < minTick) {
				it.remove();
			}
		}
		if (chosenTick == null || !currentDomain.contains(chosenTick)) {
			tryToChooseNewTick();
		}*/
		
	}

	private AirportWrapper chooseNewDestiny() {
		int airportIndex;
		do {
			airportIndex = RandomHelper.nextIntFromTo(0, airports.size() - 1);
		} while (currentAirport.equals(airports.get(airportIndex))); // do not travel to same airport
		return airports.get(airportIndex);
	}
	
	private void disconnectFromAirport() {
		Logger.printMsg(getAID(), currentAirport.getAID(), "Disconnecting from airport");
		ACLMessage aclDisconnectMessage = new ACLMessage(M_Disconnect.performative);
		try {
			aclDisconnectMessage.setProtocol(M_Disconnect.protocol);
			aclDisconnectMessage.setContentObject(new M_Disconnect(id, currentAirport.getAID()));
		} catch (IOException e) {
			Logger.printErrMsg(getAID(), currentAirport.getAID(), e.getMessage());
			return;
		}
		aclDisconnectMessage.addReceiver(currentAirport.getAID());
		agentsInAirport.forEach( (airplaneID, airplaneAID) -> {
			aclDisconnectMessage.addReceiver(airplaneAID);
		});
		send(aclDisconnectMessage);
		isConnectedToAirport = false;
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
		//int randomIndex = (int) (Math.random() * currentDomain.size());
		//do {
			chosenTick = currentDomain.iterator().next();
		//	randomIndex--;
		//} while (randomIndex >= 0);
		Logger.printMsg(getAID(), currentAirport.getAID(), "updated value to " + chosenTick);
		return true;
	}
	
	/**
	 * Send ok message to all lower priority agents
	 */
	private void sendOkMessage() {
		if (chosenTick == null) {
			chooseNewValue();
		}
		TreeMap<Integer, AID> lowerPriorityAgents = getLowerPriorityAgents();
		if (lowerPriorityAgents != null && lowerPriorityAgents.size() > 0) {
			//Logger.printMsg(getAID(), currentAirport.getAID(), "Sending ok " + value + " to " + lowerAgents.size() + " agents");
			ACLMessage aclOkMessage = new ACLMessage(M_Ok.performative);
			try {
				aclOkMessage.setProtocol(M_Ok.protocol);
				aclOkMessage.setContentObject(new M_Ok(id, chosenTick));
			} catch (IOException e) {
				Logger.printErrMsg(getAID(), currentAirport.getAID(), e.getMessage());
				return;
			}
			
			for (AID aid : lowerPriorityAgents.values()) {
				aclOkMessage.addReceiver(aid);
			}
			Logger.printMsg(getAID(), currentAirport.getAID(), "Sending ok with tick " + chosenTick);
			send(aclOkMessage);
		}
	}
	/**
	 * Send ok message to a specific agent
	 * @param agentAID AID of agent to which the message should be sent
	 */
	private void sendOkMessage(AID agentAID) {
		ACLMessage aclOkMessage = new ACLMessage(M_Ok.performative);
		try {
			aclOkMessage.setProtocol(M_Ok.protocol);
			aclOkMessage.setContentObject(new M_Ok(id, chosenTick));
		} catch (IOException e) {
			Logger.printErrMsg(getAID(), currentAirport.getAID(), e.getMessage());
			return;
		}
		aclOkMessage.addReceiver(agentAID);
		send(aclOkMessage);
	}
	
	private boolean addAgentPrevValueToDomain(AgentViewValue prevValue) {
		if (!agentView.containsValue(prevValue) && originalDomain.contains(prevValue.getValue())) { //there could be another agent which had the previous value
			if (!currentDomain.contains(prevValue.getValue())) {
				currentDomain.add(prevValue.getValue()); //Adds the previous value of the agent to this agent available domain
				return true;
			}
		}
		return false;
	}
	
	public void parseOkMessage(M_Ok okMessage, AID sender) {
		if (agentsInAirport.containsValue(sender)) {
			isABTRunning = true;
			AgentViewValue avv = new AgentViewValue(okMessage.getValue(), sender);
			Logger.printMsg(getAID(), currentAirport.getAID(), "Received ok " + okMessage.getValue() + " from " + sender.getLocalName());
			// check if new value is compatible with other agent values in the agentview
			AgentViewValue prevValue = agentView.put(okMessage.getAgentId(), avv);
			if (prevValue != null) {
				addAgentPrevValueToDomain(prevValue);
			}
			tryToChooseNewTick();
			if (agentsInAirport.tailMap(id+1).size() == 0) {
				// if I am the element with less priority, check for ABT end (success).
				checkABTSuccess();
			}
		} else {
			Logger.printErrMsg(getAID(), "Received Ok from agent that is not in airport");
		}
	}
	
	private void checkABTSuccess() {
		Logger.printMsg(getAID(), currentAirport.getAID(), "Checking ABT success");
		HashSet<Integer> h = new HashSet<Integer>();
		if (chosenTick == null) {
			tryToChooseNewTick();
			return;
		}
		if (agentView.size() != agentsInAirport.size()) {
			if (agentView.size() > agentsInAirport.size()) {
				Logger.printErrMsg(getAID(), currentAirport.getAID(), "Checking ABT success: agentViewSize != agentsInAirportSize: " + agentView.size() + " != " + agentsInAirport.size());
			}
			agentView.forEach((k,avv)->{
				Logger.printMsg(getAID(), currentAirport.getAID(), k + "->" + avv.getValue());
			});
			return;
		}
		h.add(chosenTick);
		for (AgentViewValue avv : agentView.values()) {
			Integer agentValue = avv.getValue();
			if (agentValue == null || h.add(agentValue) == false) {
				Logger.printErrMsg(getAID(), currentAirport.getAID(), "Checking ABT success: duplicated value in agentView");
				return; // ABT has not ended yet
			}
		}
		isABTRunning = false;
		addBehaviour(new InformABTEnd());
		Logger.printMsg(getAID(), currentAirport.getAID(), "ABT ended");
	}
	
	
	class ListenABTEnd extends CyclicBehaviour {
		
		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(M_ABTEnd.performative), MessageTemplate.MatchProtocol(M_ABTEnd.protocol));
		
		@Override
		public void action() {
			ACLMessage aclMsg = receive(mt);
			if (aclMsg != null) {
				messageCounter++;
				if (chosenTick != null) {
					// ABT hasn't ended if value is null, others will resume once this sends OK with value
					Logger.printMsg(getAID(), currentAirport.getAID(), "Received ABT End");
					isABTRunning = false;
				}
			} else {
				block();
			}
			
		}
		
	}
	
	class InformABTEnd extends OneShotBehaviour {

		@Override
		public void action() {
			if (agentsInAirport.size() > 0) {
				ACLMessage aclMsg = new ACLMessage(M_ABTEnd.performative);
				aclMsg.setProtocol(M_ABTEnd.protocol);
				for (AID airplaneAID : agentsInAirport.values()) {
					aclMsg.addReceiver(airplaneAID);
				}
				send(aclMsg);
			}
		}
	}
	
	private void tryToChooseNewTick() {
		// Remove from current domain all values that were defined by agents with bigger or equal priority to the sender of this ok message
		agentView.values().forEach(v -> {
			currentDomain.remove(v.getValue());
		});
		//Logger.printErrMsg(getAID(), currentAirport.getAID(), "currentDomain.size() old | new = " + oldSize + " | " + currentDomain.size());
		if (chosenTick == null || !currentDomain.contains(chosenTick)) {
			if (chooseNewValue()) {
				sendOkMessage(); // send ok to lower priority agents
			} else {
				backtrack();
			}
		}
		if (agentsInAirport.size() == 0) {
			isABTRunning = false;
		}
	}
	
	private void backtrack() {
		// current agent view is a nogood
		Logger.printErrMsg(getAID(), currentAirport.getAID(), "Generating Nogood");
		TreeMap<Integer, AID> lowerPriorityAgents = getLowerPriorityAgents();
		if (agentView.size() == 0) { // agentView.size == 0 means that I am the agent with most priority
			// TERMINATE, Broadcast stop message
			Logger.printMsg(getAID(), currentAirport.getAID(), "No solution found");
			ACLMessage aclStopMsg = new ACLMessage(M_Stop.performative);
			aclStopMsg.setProtocol(M_Stop.protocol);
			Logger.printMsg(getAID(), currentAirport.getAID(), "sending stop to " + lowerPriorityAgents.size() + " agents");
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
		//Logger.printMsg(getAID(), currentAirport.getAID(), "Sent nogood to " + receiver.getLocalName());
	}
	
	private void parseNogoodMsg(M_Nogood nogoodMsg) {
		isABTRunning = true;
		// check if nogood is consistent with agentview
		// this is an optimization to prevent unnecessary messages
		Logger.printMsg(getAID(), currentAirport.getAID(), "Received nogood");
		HashSet<Nogood> nogoods = nogoodMsg.getNogoods();
		AgentViewValue myNogood = null;
		for (Nogood nogood : nogoods) {
			if (nogood.getAgentId().equals(id)) {
				myNogood = nogood.getValue();
			}
		}
		if (!myNogood.getValue().equals(chosenTick)) {
			Logger.printMsg(getAID(), currentAirport.getAID(), "Ignoring nogood, reason: " + myNogood.getValue() + " " + chosenTick);
			return;
		}
		
		//Logger.printMsg(getAID(), currentAirport.getAID(), "nogood consistent with agent view");
		// set my value to null
		chosenTick = null;
		// update currentDomain
		currentDomain.remove(myNogood.getValue());
		// try to get new value
		tryToChooseNewTick();
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
				messageCounter++;
				if (!isReseting) {
					Logger.printMsg(getAID(), currentAirport.getAID(), "Received Nogood");
					try {
						M_Nogood nogoodMsg = (M_Nogood) msg.getContentObject();
						parseNogoodMsg(nogoodMsg);
					} catch (UnreadableException e) {
						e.printStackTrace();
					}
				}
			} else {
				block();
			}
			
		}
		
	}
	
	class OkListeningBehaviour extends CyclicBehaviour {
		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(M_Ok.performative), MessageTemplate.MatchProtocol(M_Ok.protocol));

		public void action() {
			ACLMessage msg = receive(mt);
			if(msg != null) {
				messageCounter++;
				if (!isReseting) {
					try {
						messageCounter++;
						M_Ok okMessage = (M_Ok) msg.getContentObject();
						parseOkMessage(okMessage, msg.getSender());
					} catch (UnreadableException e) {
						e.printStackTrace();
					}
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
				messageCounter++;
				takeDown();
			} else {
				block();
			}
			
		}		
	}
	
	private void setNewDomains() {
		GridPoint myPoint = grid.getLocation(this);
		GridPoint otherPoint = currentAirport.getGridPoint();
		double distance = grid.getDistance(myPoint, otherPoint);
		int minTick = (int) RepastEssentials.GetTickCount() + (int) Math.ceil(distance / maxSpeed);
		int maxTick = (int) RepastEssentials.GetTickCount() + maxFuel;
		// initialize domains
		originalDomain = new TreeSet<Integer>();
		currentDomain = new TreeSet<Integer>();
		Logger.printMsg(getAID(), currentAirport.getAID(), "New domain goes from " + minTick + " to " + maxTick);
		for (int i = minTick; i <= maxTick; i++) {
			originalDomain.add(i);
			currentDomain.add(i);
		}
		chosenTick = null;
		isABTRunning = true;
		tryToChooseNewTick();
	}
	
	public void connectToOtherAirplanes() {
		if (agentsInAirport.size() == 0) {
			isABTRunning = false;
		}
		M_Connect connectObject = null;
		try {
			connectObject = new M_Connect(id, chosenTick, currentAirport.getAID());
		} catch (NullPointerException e) {
			Logger.printErrMsg(getAID(), currentAirport.getAID(), id + " " + chosenTick + " " + currentAirport.getAID().getLocalName());
		}
		
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
	}
	
	public class ListenForAirplanesInAirport extends CyclicBehaviour {

		MessageTemplate mtAgents = MessageTemplate.and(MessageTemplate.MatchPerformative(M_Agents.performative),
				MessageTemplate.MatchProtocol(M_Agents.protocol));
		
		@Override
		public void action() {
			ACLMessage agentsACLMsg = receive(mtAgents);
			if (agentsACLMsg != null) {
				messageCounter++;
				M_Agents agentsMsg;
				try {
					agentsMsg = (M_Agents) agentsACLMsg.getContentObject();
				} catch (UnreadableException e) {
					e.printStackTrace();
					return;
				}
				agentsInAirport.clear();
				agentsInAirport.putAll(agentsMsg.getAgents());
				Logger.printMsg(getAID(),currentAirport.getAID(), "Received agents in airport, count: " + agentsMsg.getAgents().size());
				connectToOtherAirplanes();
			} else {
				block();
			}
			
		}
		
	}
	
	
	class ConnectToNewAirportBehaviour extends OneShotBehaviour {

		/**
		 * 
		 */
		private static final long serialVersionUID = 7390566766458888349L;
		
		boolean done = false;
		int state = 0;
		
		

		private boolean disconnectCurrent;

		public ConnectToNewAirportBehaviour() {
			disconnectCurrent = true;
		}
		public ConnectToNewAirportBehaviour(boolean disconnectCurrent) {
			this.disconnectCurrent = disconnectCurrent;
		}
		
		@Override
		public void action() {
			isABTRunning = true;
			Logger.printMsg(getAID(), currentAirport.getAID(), "Created Connect to new airport behaviour. Disconnecting from current: " + disconnectCurrent);
			if (disconnectCurrent) {
				disconnectFromAirport();
				pickNewAirport();
				requestAirplanesInAirport();
			} else {
				setNewDomains();
				//connectToOtherAirplanes();
			}
		}
		
		public void pickNewAirport() {
			setAirport(chooseNewDestiny());
			Logger.printMsg(getAID(), currentAirport.getAID(), "Picked new airport: " + currentAirport.getAID().getLocalName());
			resetState();
			setNewDomains();
		}
		
		public void requestAirplanesInAirport() {
			Logger.printMsg(getAID(), currentAirport.getAID(), "Starting Connect Protocol to " + currentAirport.getAID().getLocalName());
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
		}
	}
	
	public class NotInAirportListeningBehaviour extends CyclicBehaviour {

		MessageTemplate mt = MessageTemplate.and(
				MessageTemplate.MatchPerformative(M_NotInAirport.performative),
				MessageTemplate.MatchProtocol(M_NotInAirport.protocol));
		
		@Override
		public void action() {
			ACLMessage aclMsg = receive(mt);
			if (aclMsg != null) {
				messageCounter++;
				Logger.printMsg(getAID(), currentAirport.getAID(), "Received not in airport");
				M_NotInAirport notInAirportMsg;
				try {
					notInAirportMsg = (M_NotInAirport) aclMsg.getContentObject();
				} catch (UnreadableException e) {
					e.printStackTrace();
					return;
				}
				int agentNotInAirport = notInAirportMsg.getAgentId();
				agentsInAirport.remove(agentNotInAirport);
				checkABTSuccess();
			} else {
				block();
			}
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
				messageCounter++;
				try {
					M_Connect connectMsg = (M_Connect) msg.getContentObject();
					AID airportInMsg = connectMsg.getAirport();
					Logger.printMsg(getAID(), currentAirport.getAID(), "Received connect from " + connectMsg.getAgentId() + " for airport " + airportInMsg.getLocalName());
					if (!isConnectedToAirport || !currentAirport.getAID().equals(airportInMsg)) {
						sendNotInAirportMsg(msg.getSender());
						return;
					}
					isABTRunning = true;
					agentsInAirport.put(connectMsg.getAgentId(), msg.getSender());
					if (id > connectMsg.getAgentId()) {
						agentView.put(connectMsg.getAgentId(), new AgentViewValue(connectMsg.getValue(), msg.getSender()));
					}
					
					if (connectMsg.getAgentId() > id) {
						if (chosenTick != null) {
							sendOkMessage();
						} else {
							tryToChooseNewTick();
						}
						
					} else {
						if (chosenTick == null || chosenTick.intValue() == connectMsg.getValue()) {
							tryToChooseNewTick();
						}
						checkABTSuccess();
					}
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
			} else {
				block();
			}
		}
		private void sendNotInAirportMsg(AID receiver) {
			ACLMessage notInAirportACL = new ACLMessage(M_NotInAirport.performative);
			notInAirportACL.setProtocol(M_NotInAirport.protocol);
			M_NotInAirport notInAirportMsg = new M_NotInAirport(id);
			try {
				notInAirportACL.setContentObject(notInAirportMsg);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			notInAirportACL.addReceiver(receiver);
			send(notInAirportACL);
			Logger.printMsg(getAID(), currentAirport.getAID(), "Sent not in airport. IsConnectedToAirport=" + isConnectedToAirport);
		}
	}

	
}
