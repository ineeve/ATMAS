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
	private AID airport;
	public static int performative = ACLMessage.INFORM;
	public static String protocol = "P_DISCONNECT";
	
	public M_Disconnect(int agentId, AID airport) {
		this.agentId = agentId;
		this.airport = airport;
	}
	
	public int getAgentId() {
		return agentId;
	}
	
	public AID getAirport() {
		return airport;
	}

}
