import java.util.ArrayList;

import jade.core.Agent;
import utils.Coordinates;
import utils.State;

@SuppressWarnings("serial")
public class AirplaneAgent extends Agent {

	private int maxFuel;
	private int maxSpeed;
	private int minSpeed;
	private int fuelPerTime;
	private Coordinates coordinates;
	private State state;
	private boolean onEmergency;
	private ArrayList<AirportAgent> airports;
	private AirportAgent destination;
	private AirportAgent origin;
	private Integer landingTime;
	private Integer minLandingtime;
	private Integer maxLandingTime;
	
}
