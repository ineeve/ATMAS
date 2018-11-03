import java.util.ArrayList;

import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class JADELauncher {

	public static void main(String[] args) {
		Runtime rt = Runtime.instance();
		Profile p1 = new ProfileImpl();
		ContainerController mainContainer = rt.createMainContainer(p1);
		setup(mainContainer);
	}
	public static void setup(ContainerController mainContainer) {
		
		int numAirports = 1;
		int numAirplanes = 1;
		int grid_size = 50;
		ArrayList<AirportAgent> airports = new ArrayList<AirportAgent>();
		for(int i = 0; i < numAirports; i++) {
			try {
				AirportAgent ag = new AirportAgent(Math.round(Math.random()*grid_size), Math.round(Math.random()*grid_size));
				AgentController ac1 = mainContainer.acceptNewAgent("airport"+i,ag);
				ac1.start();
				airports.add(ag);
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}
		}
		for (int i = 0; i < numAirplanes; i++) {
			try {
				int airportIndex = (int)(Math.random()*numAirports);
				AirportAgent airportSelected = airports.get(airportIndex);
				AirplaneAgent airplane = new AirplaneAgent(i, airports, airportSelected);
				AgentController ap = mainContainer.acceptNewAgent("airplane"+i, airplane);
				ap.start();
			} catch(StaleProxyException e) {
				e.printStackTrace();
			}
		}
		

		AgentController ac3;
		try {
			ac3 = mainContainer.acceptNewAgent("myRMA", new jade.tools.rma.rma());
			ac3.start();
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}

}
