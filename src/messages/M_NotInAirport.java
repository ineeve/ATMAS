package messages;

import java.io.Serializable;

import jade.lang.acl.ACLMessage;

public class M_NotInAirport implements Serializable {

	private static final long serialVersionUID = 4670146803806792375L;
	private int agentId;
	public static int performative = ACLMessage.INFORM;
	public static String protocol = "P_CONNECT_RESPONSE";
	
	public M_NotInAirport(int agentId) {
		this.agentId = agentId;
	}
	
	public int getAgentId() {
		return agentId; 
	}
}
