package messages;

import java.io.Serializable;

import jade.core.AID;
import jade.lang.acl.ACLMessage;

/**
 * Used for airplanes to connect to airports
 * @author ineeve
 *
 */
public class ConnectMessage implements Serializable {

	private static final long serialVersionUID = 4044390113376790821L;
	private int agentId;
	private AID agentAID;
	public static int performative = ACLMessage.REQUEST;
	
	public ConnectMessage(int agentId, AID agentAID) {
		this.agentId = agentId;
		this.agentAID = agentAID;
	}
	
	public int getAgentId() {
		return agentId;
	}
	
	public AID getAgentAID() {
		return agentAID;
	}

}
