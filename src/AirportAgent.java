import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import jade.core.AID;
import jade.core.Agent;
import utils.Coordinates;

public class AirportAgent extends Agent {

	private Coordinates coordinates;
	private HashSet<AirplaneAgent> connectedAirplanes;
	
	public AirportAgent(long l, long m){
		this.coordinates = new Coordinates(l,m);
		connectedAirplanes = new HashSet<AirplaneAgent>();
	}
	
	public Coordinates getCoordinates() {
		return this.coordinates;
	}
	public void addAirplaneAgent(AirplaneAgent a) {
		connectedAirplanes.add(a);
	}
	public void removeAirplaneAgent(AirplaneAgent a) {
		connectedAirplanes.remove(a);
	}
	
	public ArrayList<AID> getLowerPriorityAirplanesAID(AID myAID) {
		ArrayList<AID> a = new ArrayList<AID>();
		for (AirplaneAgent airplane : connectedAirplanes) {
			if (myAID.compareTo(airplane.getAID()) < 0){
				a.add(airplane.getAID());
			}
		}
		return a;
	}
}
