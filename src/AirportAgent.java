import java.util.ArrayList;

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
}
