package messages;

import java.io.Serializable;

import jade.lang.acl.ACLMessage;

public class ResetDone implements Serializable {

	private static final long serialVersionUID = 6645934759935195070L;
	public static int performative = ACLMessage.INFORM;
	public static String protocol = "P_RESET_DONE";

	public ResetDone() {}

}
