package messages;

import java.io.Serializable;

import jade.lang.acl.ACLMessage;

public class M_DisconnectDone implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4405663944744681943L;
	public static int performative = ACLMessage.INFORM;
	public static String protocol = "P_DISCONNECT_DONE";

	public M_DisconnectDone() {}

}
