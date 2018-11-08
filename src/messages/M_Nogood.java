package messages;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

import jade.lang.acl.ACLMessage;
import utils.AgentViewValue;
import utils.Nogood;


public class M_Nogood implements Serializable {
	
	private static final long serialVersionUID = -7178093960057801334L;
	// maps agentIds to values
	private HashSet<Nogood> nogoods;
	public static int performative = ACLMessage.REJECT_PROPOSAL;
	public static String protocol = "P_NOGOOD";
	
	public M_Nogood() {
		nogoods = new HashSet<Nogood>();
	}
	
	public void addNogood(Nogood nogood) {
		nogoods.add(nogood);
	}
	public HashSet<Nogood> getNogoods(){
		return nogoods;
	}
	public void removeNogood(Integer agentId) {
		nogoods.removeIf(n -> {
			return n.getAgentId().equals(agentId);
		});
	}

}
