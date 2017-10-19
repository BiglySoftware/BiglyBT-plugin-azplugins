/*
* Created on 6-Sept-2003
* Created by Olivier
* Copyright (C) 2004 Aelitis, All Rights Reserved.
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*
* AELITIS, SARL au capital de 30,000 euros
* 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
*
*/

package org.gudy.azureus2.ui.swt.views;

import java.text.Collator;
import java.util.Locale;
import java.util.StringTokenizer;

import com.biglybt.pif.PluginConfig;
import com.biglybt.pif.PluginInterface;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

import java.io.*;
import java.util.Calendar;

import org.gudy.azureus2.irc.IrcClient;
import org.gudy.azureus2.irc.IrcListener;

import com.biglybt.pif.utils.*;
import com.biglybt.ui.swt.pif.UISWTView;

/**
* @author Olivier
*
*/
public class
SWTIrcView
implements IrcListener
{
	public static final String	CONFIG_IRC_SERVER 			= "Irc Server";
	public static final String	CONFIG_IRC_SERVER_DEFAULT	= "irc.freenode.net";
	
	public static final String	CONFIG_IRC_CHANNEL			= "Irc Channel";
	public static final String	CONFIG_IRC_CHANNEL_DEFAULT	= "";
	
	public static final String	CONFIG_IRC_USER				= "Irc Login";
	public static final String	CONFIG_IRC_USER_DEFAULT		= "";

	PluginInterface	plugin_interface;
	PluginConfig plugin_config;
	LocaleUtilities locale_utils;
	
	Display display;
	Composite cIrc;
	ConsoleText consoleText;
	ConsoleText topicBar;
	List users;
	Label userSumUp;
	Text inputField;
	Color[] colors;
	FileOutputStream logOut;
	
	IrcClient client;
	boolean newMessage;
	private boolean	blink;
	
	private String lastPrivate;
	
	public
	SWTIrcView(
	PluginInterface _plugin_interface)
	{
		plugin_interface	= _plugin_interface;		
		plugin_config	= _plugin_interface.getPluginconfig();		
		locale_utils = plugin_interface.getUtilities().getLocaleUtilities();
	}
	
	public void initialize(Composite composite) {
		initialize(composite, null, null, null);
	}

	public void initialize(Composite composite, String server, String channel,
			String alias) {
		display = composite.getDisplay();
		
		
		GridData gridData;
		
		cIrc = new Composite(composite,SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		cIrc.setLayout(layout);
		gridData = new GridData(GridData.FILL_BOTH);
		cIrc.setLayoutData(gridData);
		
		topicBar  = new ConsoleText(cIrc,SWT.READ_ONLY | SWT.BORDER | SWT.WRAP)
		{
			
			@Override
			public void hyperlinkSelected(String link)
			{
				//What type of link is it ("What do I with with it")
				if (link.startsWith(" #")) {
					link = link.substring(link.indexOf("#"));
					client.changeChannel(link, link + " (C)");
					} else if (link.startsWith("magnet") || (link.endsWith(".torrent"))) {
					new URLBrowser( plugin_interface, link);
					} else if (link.startsWith("\"")) {
					Program.launch( link.substring(9, link.length() - 1) );
					} else {
					new URLBrowser( link );
				}
			}
			
		};
		topicBar.addHyperlinkStyle();
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		gridData.heightHint = 51;
		topicBar.setLayoutData(gridData);
		
		
		
		
		consoleText = new ConsoleText(cIrc,	SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.WRAP)
		{
			@Override
			public void hyperlinkSelected(String link)
			{
				//What type of link is it ("What do I with with it")
				if (link.startsWith(" #")) {
					link = link.substring(link.indexOf("#"));
					client.changeChannel(link, link + " (C)");
					} else if (link.startsWith("magnet") || (link.endsWith(".torrent"))) {
					new URLBrowser( plugin_interface, link);
					} else if (link.startsWith("\"")) {
					Program.launch( link.substring(9, link.length() - 1) );
					} else {
					new URLBrowser( link );
				}
			}
			
		};
		
		consoleText.addHyperlinkStyle();
		gridData = new GridData(GridData.FILL_BOTH | GridData.CENTER);
		gridData.grabExcessHorizontalSpace = true;
		consoleText.setLayoutData(gridData);
		users = new List(cIrc, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL );
		gridData = new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_END | GridData.END);
		gridData.widthHint = 120;
		users.setLayoutData(gridData);
		inputField = new Text(cIrc, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		//gridData.horizontalSpan = 2;
		inputField.setLayoutData(gridData);
		inputField.setTextLimit(435);
		inputField.addKeyListener(new KeyAdapter() {
			/* (non-Javadoc)
			* @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
			*/
			@Override
			public void keyReleased(KeyEvent event) {
				if (event.keyCode == 13) {
					String text = inputField.getText();
					inputField.setText("");
					sendMessage(text);
				}
			}
		});
		
		userSumUp = new Label(cIrc, SWT.NONE);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gridData.widthHint = 120;
		userSumUp.setLayoutData(gridData);
		colors = new Color[8];
		colors[0] = new Color(display, new RGB(169,212,254 ));
		colors[1] = new Color(display, new RGB(198,226,255 ));
		colors[2] = new Color(display, new RGB(226,240,255 ));
		colors[3] = new Color(display, new RGB(255,192,192));
		colors[4] = new Color(display, new RGB(255,170,170));
		colors[5] = new Color(display, new RGB(238, 238, 238)); // Nice grey.
		colors[6] = new Color(display, new RGB(255,0,0)); // Flash 1
		colors[7] = new Color(display, new RGB(0,255,0)); // Flash 2
		
		if (server == null || server.length() == 0) {
			server = plugin_config.getPluginStringParameter(CONFIG_IRC_SERVER,
					CONFIG_IRC_SERVER_DEFAULT);
		}
		if (channel == null || channel.length() == 0) {
			channel = plugin_config.getPluginStringParameter(CONFIG_IRC_CHANNEL,
					CONFIG_IRC_CHANNEL_DEFAULT);
		}
		if (alias == null || alias.length() == 0) {
			alias = plugin_config.getPluginStringParameter(CONFIG_IRC_USER,
					CONFIG_IRC_USER_DEFAULT);
		}
		client = new IrcClient(plugin_interface, server, channel, alias, this);
		//		client.setVerbose(true);
	}
	
	protected void
	refresh(
	UISWTView		view )
	{
		String title = "";		
		if ( newMessage ){
			//You've got mail!			
			blink = !blink;			
			title += blink?"!":" " ;
		}		
		title += locale_utils.getLocalisedMessageText("IrcView.title.short");		
		if ( client != null){			
			title += " " + client.getChannel() + " on " + client.getSrvName();
		}		
		view.setTitle( title );
	}
	
	public void
	focusGained()
	{
		newMessage = false;
		inputField.setFocus();
	}
	
	
	public void delete() {
		Thread t = new Thread() {
			@Override
			public void run() {
				client.close();
			}
		};
		t.setDaemon(true);
		t.start();
		
		if ( colors != null ){
			for (int i=0;i<colors.length;i++){
				if ( !colors[i].isDisposed()){
					colors[i].dispose();
				}
			}
		}
	}
	
	@Override
	public void messageReceived(String sender, String message) {
		doLog(1, "<" + sender + "> " + message);
		newMessage = true;
	}
	
	@Override
	public void topicChanged(String channel, String topic)
	{
		doLog(5,locale_utils.getLocalisedMessageText("IrcClient.topicforchannel") + " " + channel + " : " + topic);
		topicBar.setText(5, locale_utils.getLocalisedMessageText("IrcClient.topicforchannel") + ": " + topic);
	}
	
	@Override
	public void clearTopic()
	{
		topicBar.setText(5, " ");
	}
	
	@Override
	public void systemMessage(String message) {
		//From services?
		if ((message.indexOf("This nickname is owned by someone else") == 0) || (message.indexOf("If this is your nickname, type /msg NickServ IDENTIFY <password>") == 0))
		{
			doLog(5, message);
			} else {
			doLog(2, message);
		}
	}
	
	private void doLog(int color, String text)
	{
		consoleText.append(color, text);
		//HTML logs! Woo.
		if( plugin_config.getPluginBooleanParameter(IrcClient.CONFIG_IRC_LOG) ) {
			try
			{
				File log = new File("IRC_Log.htm");
				if (log.exists() == false) {
					logOut = new FileOutputStream ("IRC_log.htm", true);
					new PrintStream(logOut).print("<span style=\"font-family:Verdana,sans-serif;font-size:small;\"><h4>BiglyBT IRC log</h4><br>");
					logOut.close();
				}
				logOut = new FileOutputStream ("IRC_log.htm", true);
				Calendar now = Calendar.getInstance();
				String endText = "  " + text;
				endText = endText.replaceAll("<", "&lt;");
				endText = endText.replaceAll(">", "&gt;");
				endText = endText + "<br>";
				if (endText.indexOf(client.getUserName()) > -1) {
					endText = "<strong>" + endText + "</strong>";
					//endText = endText.replace(client.getUserName(), "<strong>" + client.getUserName() + "</strong>");
				}
				if ((endText.indexOf(locale_utils.getLocalisedMessageText("IrcClient.hasleft")) > -1) || endText.indexOf(locale_utils.getLocalisedMessageText("IrcClient.hasjoined")) > -1)
				{
					endText = "<span style=\"color:#999999;\"><em>" + endText + "</em></span>";
				}
				if (endText.indexOf("  " + locale_utils.getLocalisedMessageText("IrcView.noticefrom") + " -") > 0 || endText.indexOf("  " + locale_utils.getLocalisedMessageText("IrcView.privatefrom") + " *") > 0 || endText.indexOf("  " + locale_utils.getLocalisedMessageText("IrcView.privateto") + " *") > -1) {
					endText = "<span style=\"color:#FF0000;\">" + endText + "</span>";
				}
				if (endText.indexOf("  " + locale_utils.getLocalisedMessageText("IrcClient.topicforchannel") + " #") > -1) {
					endText = "<span style=\"color:#008000;\">" + endText + "</span>";
				}
				endText = "[" + format(now.get(Calendar.HOUR_OF_DAY)) + ":" + format(now.get(Calendar.MINUTE)) + ":" + format(now.get(Calendar.SECOND)) + "]" + endText;
				new PrintStream(logOut).print(endText);
				logOut.close();
			}
			catch (IOException e)
			{
				System.err.println ("Unable to write to file");
			}
		}
	}
	private String format(int n)
	{
		return (n < 10) ? "0" + n : "" + n;
	}
	
	/* (non-Javadoc)
	* @see org.gudy.azureus2.irc.IrcListener#action(java.lang.String, java.lang.String)
	*/
	@Override
	public void action(String sender, String action) {
		doLog(0, sender + " " + action);
	}
	
	/* (non-Javadoc)
	* @see org.gudy.azureus2.irc.IrcListener#clientEntered(java.lang.String)
	*/
	@Override
	public void clientEntered(final String client) {
		if (display == null || display.isDisposed())
		return;
		display.asyncExec(new Runnable() {
			/* (non-Javadoc)
			* @see java.lang.Runnable#run()
			*/
			@Override
			public void run() {
				if (users != null && !users.isDisposed()) {
					int index = users.indexOf(client);
					if (index == -1) {
						Collator collator = Collator.getInstance(Locale.getDefault());
						String items[] = users.getItems();
						int i = 0;
						for (; i < items.length; i++) {
							if (collator.compare(client, items[i]) < 0) {
								users.add(client, i);
								break;
							}
						}
						if (i == items.length) {
							users.add(client);
						}
						int nbUsers = users.getItemCount();
						if (userSumUp != null && !userSumUp.isDisposed()) {
							userSumUp.setText(nbUsers + " " + locale_utils.getLocalisedMessageText("IrcView.clientsconnected"));
						}
					}
				}
			}
		});
	}
	
	/* (non-Javadoc)
	* @see org.gudy.azureus2.irc.IrcListener#clientExited(java.lang.String)
	*/
	@Override
	public void clientExited(final String client) {
		if (display == null || display.isDisposed())
		return;
		display.asyncExec(new Runnable() {
			/* (non-Javadoc)
			* @see java.lang.Runnable#run()
			*/
			@Override
			public void run() {
				if (users != null && !users.isDisposed()) {
					int index = users.indexOf(client);
					if (index != -1) {
						users.remove(index);
					}
					int nbUsers = users.getItemCount();
					if (userSumUp != null && !userSumUp.isDisposed()) {
						userSumUp.setText(nbUsers + " " + locale_utils.getLocalisedMessageText("IrcView.clientsconnected"));
					}
				}
			}
		});
	}
	
	//This is evil in it's purest form.
	class flash extends Thread {
		String message = locale_utils.getLocalisedMessageText("IrcClient.topicforchannel") + ": " + client.getTopic();
		flash(String message) {
			if (message != "")
				this.message = message;
        }
		@Override
		public void run() {
			for (int i=0;i<20;i++) {
				try {
					topicBar.setText(6, message);
					Thread.sleep(100);
					topicBar.setText(7, message);
					Thread.sleep(100);
				}
				catch (Exception ex)
				{
					
				}
			}
			topicBar.setText(5, locale_utils.getLocalisedMessageText("IrcClient.topicforchannel") + ": " + client.getTopic());
		}
	}
	
	@Override
	public void topicFlash(String message) {
		Thread bleh = new flash(message);
		bleh.start();
	}
	
	//When the user gets kicked, remove all the users from the view.
	@Override
	public void allExited() {
		if (display == null || display.isDisposed())
		return;
		display.asyncExec(new Runnable() {
			/* (non-Javadoc)
			* @see java.lang.Runnable#run()
			*/
			@Override
			public void run() {
				if (users != null && !users.isDisposed()) {
					users.removeAll();
					int nbUsers = 0;
					if (userSumUp != null && !userSumUp.isDisposed()) {
						userSumUp.setText(nbUsers + " " + locale_utils.getLocalisedMessageText("IrcView.clientsconnected"));
					}
				}
			}
		});
	}
	/**
	* Regular expression for matching the nick for highlighting. This can be a
	* partial nick.
	*/
	String nickHighlight(String nick)
	{
		//This string is what kills GCJ 1.4.2 compatibility \o/
		return "(?i)(?<!<)\\b" + nick + "\\w*";
	}

	//Run the local INFO in a new thread so it doesn't lag the one doing the GUI updating.
	class myINFO extends Thread {
		@Override
		public void run() {
			systemMessage("Now gathering INFO...");
			systemMessage("---------------------");
			client.onINFO(client.getUserName(), "", "", "", 255);
			systemMessage("---------------------");
		}
	}
	
	class staticIP extends Thread {
		@Override
		public void run() {
			systemMessage("Now assigning a static IP... (Use /dhcp to revert to DHCP)");
			client.assignStatic();
		}
	}
	
	/*class runRUN extends Thread {
		public void run() {
			client.runRUN();
		}
	}*/
	
	class dhcpIP extends Thread {
		@Override
		public void run() {
			systemMessage("Now reverting back to DHCP...");
			client.assignDHCP();
		}
	}
	
	void sendMessage(String text) {
		if (text.equals(""))
		return;
		if (text.startsWith("/")) {
			if(text.toLowerCase().equals("/help")) {
				doLog(0,locale_utils.getLocalisedMessageText("IrcView.help"));
				} else if (text.toLowerCase().startsWith("/nick ") || text.toLowerCase().startsWith("/name ")) {
				String newNick = text.substring(6).trim();
				consoleText.addStyle(nickHighlight(client.getUserName()), null, null, 0);
				consoleText.addStyle(nickHighlight(newNick), null, colors[3], 0);
				client.setUserName(newNick);
				} else if (text.toLowerCase().startsWith("/me ")) {
				String action = text.substring(4).trim();
				client.sendAction(action);
				action(client.getUserName(), action);
				} else if (text.equalsIgnoreCase("/dhcp"))
				{
					Thread bleh = new dhcpIP();
					bleh.start();
				}
				else if (text.equalsIgnoreCase("/static"))
				{
					Thread bleh = new staticIP();
					bleh.start();
				}
				else if (text.equalsIgnoreCase("/set"))
					client.acceptSET();
				/*else if (text.equalsIgnoreCase("/run"))
				{
					Thread bleh = new runRUN();
					bleh.start();
				}*/
				else if(text.toLowerCase().startsWith("/msg ")) {
				StringTokenizer st = new StringTokenizer(text," ");
				st.nextToken();
				try {
					String target = st.nextToken();
					String message = "";
					while(st.hasMoreElements()) {
						message += st.nextElement() + " ";
					}
					client.sendRawMessage(target,message);
					doLog(4,locale_utils.getLocalisedMessageText("IrcView.privateto") + " *" + target + "* " + message);
					} catch(Exception e) {
					doLog(0,locale_utils.getLocalisedMessageText("IrcView.errormsg"));
				}
				} else if(text.toLowerCase().startsWith("/r ")) {
				if(lastPrivate != null) {
					String message = text.substring(3).trim();
					client.sendMessage(lastPrivate,message);
					doLog(4,locale_utils.getLocalisedMessageText("IrcView.privateto") + " *" + lastPrivate + "* " + message);
				}
			}
			else if(text.toLowerCase().startsWith("/join "))
			{
				String[] parts = text.substring(6).split(" ");
				client.changeChannel(parts[0], parts[0] + " (T)");
			}
			else if(text.toLowerCase().equals("/info"))
			{
				//Local INFO, spams a lot of information to the user.
				Thread bleh = new myINFO();
				bleh.start();
			}
			else if(text.toLowerCase().startsWith("/azbot "))
			{
				//Apparently "/msg AzBot help" was too much �_�, Introducing "/azbot help"...
				doLog(4,locale_utils.getLocalisedMessageText("IrcView.privateto") + " *AzBot* " + text.substring(7));
				client.sendRawMessage("AzBot", text.substring(7));
			}
			else {
				systemMessage(locale_utils.getLocalisedMessageText("IrcView.actionnotsupported"));
			}
		}
		else {
			client.sendRawMessage(client.getChannel(), text);
		}
	}
	
	/* (non-Javadoc)
	* @see org.gudy.azureus2.irc.IrcListener#notice(java.lang.String, java.lang.String)
	*/
	@Override
	public void notice(String sender, String message) {
		doLog(3,locale_utils.getLocalisedMessageText("IrcView.noticefrom") + " -" + sender + "- " + message);
		newMessage = true;
		lastPrivate = sender;
	}
	
	/* (non-Javadoc)
	* @see org.gudy.azureus2.irc.IrcListener#privateMessage(java.lang.String, java.lang.String)
	*/
	@Override
	public void privateMessage(String sender, String message, String sourceHostname) {
		//The /shove function, for moving users...
		if (message.startsWith("/shove")) {
			{
				//Only Azureus supporters
				if (client.isAzureusSupporter(sourceHostname)) {
					String st[] = message.split(" ");
					String Reason = " (\"";
					if (! st[1].equalsIgnoreCase(client.getChannel())) {
						for (int i = 2; i < st.length; i++) {
							Reason = Reason + st[i] + " ";
						}
						if (Reason.endsWith(" ")) {
							Reason = Reason.substring(0, Reason.length() - 1);
						}
						Reason = Reason + "\")";
						if (Reason.equals(" (\"\")")) {
							Reason = "";
						}
						//Tell the user wtf is happening.
						notice("WARNING", "You have been moved to " + st[1] + Reason);
						client.changeChannel(st[1], st[1] + " (R)" + Reason);
						message = message.substring(st[1].length() + 7);
					}
					else {
						//Tell the /shove'er to pay attention.
						client.sendMessage(sender, "I'm already in that channel (" + st[1] + ")");
					}
					} else {
					//Abuse control.
					client.sendMessage(sender, IrcClient.permError);
				}
			}
		}
		else if (message.equalsIgnoreCase("/topic"))
		{
			if (client.isAzureusSupporter(sender)) {
				//A new way to tell users to read the topic!
				this.systemMessage(locale_utils.getLocalisedMessageText("IrcClient.topicforchannel") + " " + client.getChannel() + ": " + client.getTopic());
				}
			else {
				client.sendMessage(sender, IrcClient.permError);
			}
		}
		else
		{
			lastPrivate = sender;
			newMessage = true;
			doLog(4,locale_utils.getLocalisedMessageText("IrcView.privatefrom") + " *" + sender + "* " + message);
		}
	}

	@Override
	public void addHigh() {
		consoleText.addStyle(nickHighlight(client.getUserName()), null, colors[3], 0);		
	}
}