package messages;

import java.io.Serializable;

import jade.lang.acl.ACLMessage;

public class M_Start implements Serializable{

	private static final long serialVersionUID = -6021604817117227581L;
	public static int performative = ACLMessage.INFORM;
	public static String protocol = "P_START";

	public M_Start() {
	}

}
