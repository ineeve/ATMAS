package messages;

import java.io.Serializable;

import jade.lang.acl.ACLMessage;

public class M_ABTEnd implements Serializable{
	
	/**
	 * UID used for serialization
	 */
	private static final long serialVersionUID = -6227399561174123410L;
	public static int performative = ACLMessage.INFORM;
	public static String protocol = "P_ABTEND";
	private double tick;
	
	
	public M_ABTEnd(double tick) {
		this.tick = tick;
	}
	
	public double getTick() {
		return tick;
	}
}
