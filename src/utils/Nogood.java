package utils;

import jade.core.AID;

//Represent a nogood of ABT protocol
public class Nogood {
	private Integer agentId;
	private AgentViewValue value;
	
	public Nogood(int agentId, AgentViewValue value) {
		this.agentId = agentId;
		this.value = value;
	}
	
	public Integer getAgentId() {
		return agentId;
	}

	public AgentViewValue getValue() {
		return value;
	}

	
}


