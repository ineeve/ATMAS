package messages;

import java.io.Serializable;

import jade.lang.acl.ACLMessage;

public class OkMessage implements Serializable {
	
	private static final long serialVersionUID = 3538676575506526433L;
	private int agentId;
	private int value;
	public static int perfomative = ACLMessage.PROPOSE;
	public static String protocol = "P_OK";
	
	public OkMessage(int agentId, int value) {
		this.agentId = agentId;
		this.value = value;
	}
	
	public int getAgentId() {
		return this.agentId;
	}
	public int getValue() {
		return this.value;
	}

}
