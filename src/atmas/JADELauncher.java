package atmas;

import java.util.ArrayList;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;
import sajas.core.Runtime;
import sajas.sim.repasts.RepastSLauncher;
import sajas.wrapper.ContainerController;
import utils.AirportWrapper;

public class JADELauncher extends RepastSLauncher implements ContextBuilder<Object> {
	
	// 1 tick = 5 minutes.
	static final public int TICKS_PER_HOUR = 12;
	
	private int numAirports;
	private int numAirplanes;
	private double proximity;
	private int gridSize = 50;
	private ArrayList<AirportWrapper> airports = new ArrayList<AirportWrapper>();
	private ArrayList<AirplaneAgent> airplanesList = new ArrayList<AirplaneAgent>();
	private ArrayList<AirportAgent> airportsList = new ArrayList<AirportAgent>();
	
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
		Parameters params = RunEnvironment.getInstance().getParameters();
		numAirplanes = (Integer) params.getValue("airplane_count");
		numAirports = (Integer) params.getValue("airport_count");
		
		RunEnvironment.getInstance().setScheduleTickDelay(10);
		RunEnvironment.getInstance().endAt(1000);
		
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
			airportsList.add(ag);
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
			airplanesList.add(airplane);
			context.add(airplane);
			GridPoint pt = airportSelected.getGridPoint();
			space.moveTo(airplane, pt.getX(), pt.getY());
			grid.moveTo(airplane, pt.getX(), pt.getY());
		}
		proximity = calculateAirportsProximity();
		System.out.println(proximity);
	}
	
	public double getProximity() {
		System.out.println("proximity");
		return proximity;
	}
	public int getMessagesExchanged() {
		System.out.println("messages");
		int sum = 0;
		for (int i = 0; i < airplanesList.size(); i++) {
			sum += airplanesList.get(i).getMessageCounter();
		}
		for (int i = 0; i < airportsList.size(); i++) {
			sum += airportsList.get(i).getMessageCounter();
		}
		return sum;
	}
	
	public double calculateAirportsProximity() {
		double totalDistance = 0;
		int numComb = 0;
		for (int i = 0; i < airports.size(); i++) {
			for (int j = i+1; j < airports.size(); j++) {
				GridPoint g1 = airports.get(i).getGridPoint();
				GridPoint g2 = airports.get(j).getGridPoint();
				totalDistance += grid.getDistance(g1, g2);
				numComb++;
			}
		}
		return totalDistance / (double) numComb;
	}
	
}
