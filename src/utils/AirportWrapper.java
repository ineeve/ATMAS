package utils;

import jade.core.AID;
import repast.simphony.space.grid.GridPoint;

public class AirportWrapper {

	private AID aid;
	private GridPoint gridPoint;
	
	public AirportWrapper(AID aid, GridPoint gp) {
		this.aid = aid;
		this.gridPoint = gp;
	}
	public AID getAID() {
		return aid;
	}
	public GridPoint getGridPoint() {
		return gridPoint;
	}
	
}
