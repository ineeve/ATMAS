package messages;

import java.io.Serializable;

import jade.lang.acl.ACLMessage;

//use it when there is no solution
public class StopMessage implements Serializable{
	
	private static final long serialVersionUID = -1620201702087927390L;
	public static int performative = ACLMessage.INFORM;
	public static String protocol = "P_STOP";

	public StopMessage() {
	}
	
}
