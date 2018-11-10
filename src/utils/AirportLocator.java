package utils;

import java.util.ArrayList;

import atmas.AirportAgent;
import jade.core.AID;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;

public class AirportLocator {

	/**
	 * 
	 * @param space
	 * @param selfPoint
	 * @return Null when there are no objects in the space.
	 */
	public static AirportWrapper getClosest(ContinuousSpace<Object> space, NdPoint selfPoint, ArrayList<AirportWrapper> airports) {
		//double gridSize = space.getDimensions().getWidth();
		AirportWrapper closest = null;
		double minDistance = Double.MAX_VALUE;
		for (AirportWrapper airport : airports) {
			NdPoint airportPoint = new NdPoint(airport.getGridPoint().getX(),airport.getGridPoint().getY());
			double currDistance = space.getDistance(selfPoint, airportPoint);
			if (currDistance < minDistance) {
				closest = airport;
				minDistance = currDistance;
			}
		}
		return closest;
	}

}
