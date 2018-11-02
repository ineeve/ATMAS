package messages;

import java.io.Serializable;

import jade.lang.acl.ACLMessage;

public class ResetMessage implements Serializable {
	
	private static final long serialVersionUID = 228393053132906589L;
	public static int performative = ACLMessage.PROPAGATE;

	public ResetMessage() {
	}
	
	

}
