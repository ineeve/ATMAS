package utils;

import jade.core.AID;

public class Logger {

	public static void printMsg(AID agentAID, String msg) {
		System.out.println(agentAID.getLocalName() +  ": " + msg);
	}
	
	public static void printErrMsg(AID agentAID, String msg) {
		System.err.println(agentAID.getLocalName() +  ": " + msg);
	}
}
