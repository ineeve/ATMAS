package messages;

import java.io.Serializable;

import jade.core.AID;
import jade.lang.acl.ACLMessage;

/**
 * Disconnect agent from airport
 * @author ineeve
 *
 */
public class M_Disconnect implements Serializable {

	private static final long serialVersionUID = -3414042195353745941L;
	private int agentId;
	public static int performative = ACLMessage.INFORM;
	public static String protocol = "P_DISCONNECT";
	
	public M_Disconnect(int agentId) {
		this.agentId = agentId;
	}
	
	public int getAgentId() {
		return agentId;
	}

}
