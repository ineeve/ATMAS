package utils;

//Represent a nogood of ABT protocol
public class Nogood {
	private int agentId;
	private int value;
	
	public Nogood(int agentId, int value) {
		this.agentId = agentId;
		this.value = value;
	}

	public int getValue() {
		return value;
	}


	public int getAgentId() {
		return agentId;
	}
	
	
}


