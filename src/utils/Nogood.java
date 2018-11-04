package utils;

import java.io.Serializable;

//Represent a nogood of ABT protocol
public class Nogood implements Serializable{

	private static final long serialVersionUID = -1135415782452871526L;
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
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (o instanceof Nogood) {
			Nogood nogood = (Nogood) o;
			return agentId.equals(nogood.agentId) && value.equals(nogood.value);
		}
		return false;
		
	}

	
}


