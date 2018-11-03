package messages;

import java.io.Serializable;
import java.util.HashMap;

import jade.lang.acl.ACLMessage;
import utils.AgentViewValue;
import utils.Nogood;


public class M_Nogood implements Serializable {
	
	private static final long serialVersionUID = -7178093960057801334L;
	// maps agentIds to values
	private HashMap<Integer, AgentViewValue> nogoods;
	public static int performative = ACLMessage.REJECT_PROPOSAL;
	public static String protocol = "P_NOGOOD";
	
	public M_Nogood() {
		nogoods = new HashMap<Integer, AgentViewValue>();
	}
	
	public void addNogood(Nogood nogood) {
		nogoods.put(nogood.getAgentId(), nogood.getValue());
	}
	public HashMap<Integer,AgentViewValue> getNogoods(){
		return nogoods;
	}
	public void removeNogood(Integer agentId) {
		nogoods.remove(agentId);
	}

}
