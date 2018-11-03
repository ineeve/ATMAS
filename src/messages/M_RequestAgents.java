package messages;

import java.io.Serializable;

import jade.lang.acl.ACLMessage;

/**
 * Use to get descendant Agents.
 * Descendant agents are agents which have lower priority
 * @author ineeve
 *
 */
public class M_RequestAgents implements Serializable {

	private static final long serialVersionUID = -3100148937006942953L;
	private Integer agentId;
	private Integer nAgentsToGet;
	public static int performative = ACLMessage.REQUEST;
	public static String protocol = "P_Agents";
	private String replyWith;
	
	/**
	 * 
	 * @param agentId id of Agent making the request
	 * @param nAgentsToGet 0 if getting all descendants, 1 if getting first child, -1 if getting parent
	 */
	public M_RequestAgents(Integer agentId, Integer nAgentsToGet){
		this.agentId = agentId;
		this.nAgentsToGet = nAgentsToGet;
		replyWith = "P_Agents_"+System.currentTimeMillis();
	}
	
	public Integer getAgentId() {
		return agentId;
	}
	public Integer getNAgentsToGet() {
		return nAgentsToGet;
	}
	public String getReplyWith() {
		return replyWith;
	}
}
