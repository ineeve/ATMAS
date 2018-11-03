package messages;

import java.io.Serializable;
import java.util.HashMap;

import jade.lang.acl.ACLMessage;
import utils.Nogood;


public class M_Nogood implements Serializable {
	
	private static final long serialVersionUID = -7178093960057801334L;
	// maps agentIds to values
	private HashMap<Integer,Integer> nogoods;
	public static int performative = ACLMessage.REJECT_PROPOSAL;
	public static String protocol = "P_NOGOOD";
	
	public M_Nogood() {
	}
	
	public void addNoGood(Nogood nogood) {
		nogoods.put(nogood.getAgentId(),nogood.getValue());
	}
	public HashMap<Integer,Integer> getNogoods(){
		return nogoods;
	}

}
