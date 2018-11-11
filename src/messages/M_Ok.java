package messages;

import java.io.Serializable;

import jade.lang.acl.ACLMessage;

public class M_Ok implements Serializable {
	
	private static final long serialVersionUID = 3538676575506526433L;
	private Integer agentId;
	private Integer value;
	public static int performative = ACLMessage.PROPOSE;
	public static String protocol = "P_OK";
	
	public M_Ok(Integer agentId, Integer value) {
		this.agentId = agentId;
		this.value = value;
	}
	
	public Integer getAgentId() {
		return this.agentId;
	}
	
	public Integer getValue() {
		return this.value;
	}

}
