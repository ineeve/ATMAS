package messages;

import java.io.Serializable;

import jade.lang.acl.ACLMessage;

public class M_Reset implements Serializable {
	
	private static final long serialVersionUID = 228393053132906589L;
	public static int performative = ACLMessage.PROPAGATE;
	public static String protocol = "P_RESET";
	private int agentWhoLeft;
	
	public M_Reset(int agentWhoLeft) {
		this.agentWhoLeft = agentWhoLeft;
	}
	public int getAgentWhoLeft() {
		return agentWhoLeft;
	}
	
	

}
