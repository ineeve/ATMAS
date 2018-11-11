package atmas;

import java.util.ArrayList;

import jade.core.AID;
import sajas.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import sajas.core.Runtime;
import sajas.wrapper.AgentController;
import sajas.wrapper.ContainerController;
import utils.AirportWrapper;
import jade.wrapper.StaleProxyException;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;
import sajas.sim.repasts.RepastSLauncher;

public class JADELauncher extends RepastSLauncher implements ContextBuilder<Object> {
	
	// 1 tick = 5 minutes.
	static final public int TICKS_PER_HOUR = 12;
	
	private int numAirports = 5;
	private int numAirplanes = 10;
	private int gridSize = 50;
	private ArrayList<AirportWrapper> airports = new ArrayList<AirportWrapper>();
	
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
		context.setId("atmas");

		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder
				.createContinuousSpaceFactory(null);
		space = spaceFactory.createContinuousSpace(
				"space", context, new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(), gridSize,
				gridSize);

		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		grid = gridFactory.createGrid("grid", context,
				new GridBuilderParameters<Object>(new WrapAroundBorders(),
						new SimpleGridAdder<Object>(), true, gridSize, gridSize));

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
		airports.clear();
		// startup airport agents
		for(int i = 0; i < numAirports; i++) {
			AirportAgent ag = new AirportAgent(space, grid);
			try {
				mainContainer.acceptNewAgent("airport"+i,ag).start();
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}
			context.add(ag);
			NdPoint pt = space.getLocation(ag);
			grid.moveTo(ag, (int) pt.getX(), (int) pt.getY());
			airports.add(new AirportWrapper(ag.getAID(), grid.getLocation(ag)));
		}
		// startup plane agents
		for (int i = 0; i < numAirplanes; i++) {
			int airportIndex = (int)(Math.random()*numAirports);
			AirportWrapper airportSelected = airports.get(airportIndex);
			AirplaneAgent airplane = new AirplaneAgent(space, grid, i, airports, airportSelected, null);
			try {
				mainContainer.acceptNewAgent("airplane"+i, airplane).start();
			} catch(StaleProxyException e) {
				e.printStackTrace();
			}
			context.add(airplane);
			GridPoint pt = airportSelected.getGridPoint();
			space.moveTo(airplane, pt.getX(), pt.getY());
			grid.moveTo(airplane, pt.getX(), pt.getY());
			// TODO: Move to origin airports once travel is implemented.
//			space.moveTo(airplane, (int) Math.round(Math.random()*grid_size), (int) Math.round(Math.random()*grid_size));
//			grid.moveTo(airplane, (int) Math.round(Math.random()*grid_size), (int) Math.round(Math.random()*grid_size));
		}
	}

}
