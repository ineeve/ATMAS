package atmas;
import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import utils.Coordinates;

public class AirportAgent extends Agent {

	private Coordinates coordinates;
	private ArrayList<AirplaneAgent> connectedAirplanes;
	
	public AirportAgent(long l, long m){
		this.coordinates = new Coordinates(l,m);
	}
	
	public Coordinates getCoordinates() {
		return this.coordinates;
	}
	
	public ArrayList<AID> getAirplanesAID() {
		ArrayList<AID> a = new ArrayList<AID>();
		for (int i = 0; i < connectedAirplanes.size(); i++) {
			a.add(connectedAirplanes.get(i).getAID());
		}
		return a;
	}
}
