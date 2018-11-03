package atmas;

import java.util.ArrayList;

import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;
import sajas.sim.repasts.RepastSLauncher;

public class JADELauncher extends RepastSLauncher implements ContextBuilder<Object> {

	private int numAirports = 1;
	private int numAirplanes = 5;
	private int grid_size = 50;
	ArrayList<AirportAgent> airports = new ArrayList<AirportAgent>();
	
	private Context<Object> context;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;

	@Override
	public String getName() {
		return "ATMAS Launcher";
	}
	
	@Override
	public Context<?> build(Context<Object> context) {
		this.context = context;
		context.setId("ATMAS");

		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>(
				"air traffic network", context, true);
		netBuilder.buildNetwork();

		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder
				.createContinuousSpaceFactory(null);
		space = spaceFactory.createContinuousSpace(
				"space", context, new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(), 50,
				50);

		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		grid = gridFactory.createGrid("grid", context,
				new GridBuilderParameters<Object>(new WrapAroundBorders(),
						new SimpleGridAdder<Object>(), true, 50, 50));

		return super.build(context);
	}
	
	@Override
	protected void launchJADE() {
		Runtime rt = Runtime.instance();
		Profile p1 = new ProfileImpl();
		ContainerController mainContainer = rt.createMainContainer(p1);
		setup(mainContainer);
	}
	
	private void setup(ContainerController mainContainer) {
		// startup airport agents
		for(int i = 0; i < numAirports; i++) {
			try {
				AirportAgent ag = new AirportAgent(Math.round(Math.random()*grid_size), Math.round(Math.random()*grid_size));
				AgentController ac1 = mainContainer.acceptNewAgent("airport"+i,ag);
				context.add(ag);
				ac1.start();
				airports.add(ag);
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}
		}
		// startup plane agents
		for (int i = 0; i < numAirplanes; i++) {
			try {
				int airportIndex = (int)(Math.random()*numAirports);
				AirportAgent airportSelected = airports.get(airportIndex);
				AirplaneAgent airplane = new AirplaneAgent(i, airports, airportSelected);
				AgentController ap = mainContainer.acceptNewAgent("airplane"+i, airplane);
				context.add(airplane);
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