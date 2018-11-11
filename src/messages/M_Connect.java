package messages;

import java.io.Serializable;

import jade.core.AID;
import jade.lang.acl.ACLMessage;

/**
 * Used for airplanes to connect to airports
 * @author ineeve
 *
 */
public class M_Connect implements Serializable {

	private static final long serialVersionUID = 4044390113376790821L;
	private int agentId;
	public static int performative = ACLMessage.REQUEST;
	public static String protocol = "P_CONNECT";
	private int value;
	private AID airportAID;
	
	public M_Connect(int agentId, int value, AID airportAID) {
		this.agentId = agentId;
		this.value = value;
		this.airportAID = airportAID;
	}
	
	public int getAgentId() {
		return agentId;
	}
	
	public int getValue() {
		return value;
	}
	
	public AID getAirport() {
		return airportAID;
	}

}
