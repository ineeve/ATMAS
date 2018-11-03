package messages;

import java.io.Serializable;
import java.util.HashSet;

import jade.core.AID;
import jade.lang.acl.ACLMessage;

public class M_Agents implements Serializable {

	private HashSet<AID> agents;
	public static int performative = ACLMessage.INFORM;
	public static String protocol = "P_Agents";
	
	public M_Agents(HashSet<AID> agents) {
		this.agents = agents;
	}
	public HashSet<AID> getAgents() {
		return agents;
	}
}
