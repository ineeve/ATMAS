package messages;

import java.io.Serializable;
import java.util.TreeMap;

import jade.core.AID;
import jade.lang.acl.ACLMessage;

public class M_Agents implements Serializable {

	private TreeMap<Integer,AID> agents;
	public static int performative = ACLMessage.INFORM;
	public static String protocol = "P_Agents";
	
	public M_Agents(TreeMap<Integer,AID> agents) {
		this.agents = agents;
		
	}
	public TreeMap<Integer,AID> getAgents() {
		return agents;
	}
}
