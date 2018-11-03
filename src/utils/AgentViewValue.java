package utils;

import java.io.Serializable;

import jade.core.AID;

public class AgentViewValue implements Serializable {
	
	private static final long serialVersionUID = 6487713522765649619L;
	private Integer value;
	private AID agentAID;
	
	public AgentViewValue(Integer value, AID agentAID) {
		this.value = value;
		this.agentAID = agentAID;
	}
	public Integer getValue() {
		return value;
	}
	public AID getAID() {
		return agentAID;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (o instanceof AgentViewValue) {
			AgentViewValue avv = (AgentViewValue) o;
			return this.value.equals(avv.value);
		}
		return false;
		
	}

	
}
