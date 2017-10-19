/*
* Created on 6 sept. 2003
*
*/
package org.gudy.azureus2.irc;

/**
* @author Olivier
*
*/
public interface IrcListener {
	
	public void messageReceived(String sender,String message);
	public void systemMessage(String message);
	public void clientEntered(String client);
	public void clientExited(String client);
	public void topicChanged(String channel, String topic);
	public void topicFlash(String message);
	public void clearTopic();
	public void addHigh();
	public void allExited(); // Used when the user gets kicked to clear the list.
	public void action(String sender,String action);
	public void privateMessage(String sender,String message, String sourceHostname);
	public void notice(String sender,String message);
	
}