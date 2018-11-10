package utils;

import atmas.AirportAgent;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;

public class AirportLocator {

	/**
	 * 
	 * @param space
	 * @param selfPoint
	 * @return Null when there are no objects in the space.
	 */
	public static AirportAgent getClosest(ContinuousSpace<Object> space, NdPoint selfPoint) {
		//double gridSize = space.getDimensions().getWidth();
		AirportAgent closest = null;
		double minDistance = Double.MAX_VALUE;
		Iterable<Object> objects = space.getObjects();
		for (final Object obj : objects) {
			if (obj instanceof AirportAgent) {
				NdPoint airportPoint = space.getLocation(obj);
				double currDistance = space.getDistance(selfPoint, airportPoint);
				if (currDistance < minDistance) {
					closest = (AirportAgent) obj;
					minDistance = currDistance;
				}
			}
		}
		return closest;
	}

}
