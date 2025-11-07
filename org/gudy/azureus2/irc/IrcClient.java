/*
* Created on 6 sept. 2003
*
*/
package org.gudy.azureus2.irc;

//TODO Add stuff to MessageBuddles.
//TODO Comment the INFOs... BAH!

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.net.*;
import java.io.*;

import com.biglybt.core.CoreFactory;
import org.eclipse.swt.SWT;
import com.biglybt.core.util.Constants;
import com.biglybt.pif.PluginConfig;
import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.PluginManager;
import com.biglybt.pif.download.Download;
import com.biglybt.pif.network.ConnectionManager;
import com.biglybt.pif.utils.LocaleUtilities;
import org.gudy.azureus2.ui.swt.views.SWTIrcView;
import org.jibble.pircbot.Colors;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

import com.biglybt.core.dht.DHT;
import com.biglybt.plugin.dht.DHTPlugin;


/**
* @author Olivier
* 
*/

public class
IrcClient
extends PircBot
{
	//The settings for the INFO retrieval.
	public static final String	CONFIG_IRC_SEND_USER_INFO				= "Irc User Info";
	public static final boolean	CONFIG_IRC_SEND_USER_INFO_DEFAULT		= true;
	
	//And the HTML log feature.
	public static final String	CONFIG_IRC_LOG				= "Irc Log";
	public static final boolean	CONFIG_IRC_LOG_DEFAULT		= false;
	
	//Some constants to allow helpers to only retrieve the INFO they need.
	final static int REQUESTED_JAVA = 1;
	final static int REQUESTED_SETTINGS = 1<<1;
	final static int REQUESTED_ACTIVE = 1<<2;
	final static int REQUESTED_STATUS = 1<<3;
	final static int REQUESTED_NETWORK = 1<<4;
	final static int REQUESTED_MISC = 1<<5;
	final static int REQUESTED_CONFLICTS = 1<<6;
	final static int REQUESTED_PLUGINS = 1<<7;
	
	static PluginInterface		plugin_interface;
	static PluginConfig			plugin_config;
	static PluginManager		plugin_manager;
	static LocaleUtilities 		locale_utils;	
	static IrcListener 			listener;
	static ConnectionManager	connection_manager;

	//This is the default error pre-fix.
	//static String[]				runCommand = new String[2];
	static ArrayList			settings = new ArrayList();
	static ArrayList 			staticParas = new ArrayList();	
	static final String			catchError = "Something went horribly wrong... Please notify another helper (Error: ";
	public static final String 	permError = "You do not have permission to do that.";
	static String 				chanTopic; //Used to store the topic so we can use it.
	static String				HTTPINFO;
	static String 				srvName;
	static String 				channel;
	static String 				azVersion;
	static String 				userName;
	static String 				static_id;
	static boolean 				connected = false; //This is just a once use thing for the nick correction.
	
	
	public
	IrcClient(
	PluginInterface	_plugin_interface,
	String _srvName,
	String _channel, 
	String _username,
	IrcListener 	_listener)
	{
		//Shame-less self advertising.
		super.setVersion("BiglyBT IRC Plugin");
		super.setFinger("BiglyBT IRC Plugin");
		plugin_interface	= _plugin_interface;
		listener = _listener;
		locale_utils = plugin_interface.getUtilities().getLocaleUtilities();
		plugin_config = plugin_interface.getPluginconfig();
		plugin_manager = plugin_interface.getPluginManager();
		azVersion = plugin_interface.getApplicationVersion();
		
		srvName 	= _srvName;
		channel 	= _channel;
		userName 	= _username;
		static_id = plugin_config.getUnsafeStringParameter( "ID", "        " ).substring( 0, 8 );  //first chars of the client instance id

		//Auto-correct the user.
		channel = channel.trim();

		//Get the default channel if they don't speficy one..
		if(channel.equals("")) {
			channel = locale_utils.getLocalisedMessageText("IrcClient.defaultChannel");
		}

		String az_version = azVersion;
		az_version = az_version.replaceAll("_", "" );
		az_version = az_version.replaceAll("\\.", "" );

		PluginManager	pm = CoreFactory.getSingleton().getPluginManager();
		connection_manager = pm.getDefaultPluginInterface().getConnectionManager();

		setName( userName ); //also sets NICK
		setLogin( static_id );
		setVersion( "AZ" +az_version+ " | " +static_id+ " | " +getVersion() );

		if(userName.equals("")) {
			listener.systemMessage(locale_utils.getLocalisedMessageText("IrcClient.noNick"));
			listener.systemMessage("Use /nick <mynick> without the <> to set your nick");
			return;
		}
		else if (userName.equals(userName.toUpperCase())) {
			userName = userName.toLowerCase(); //Caps are not fun.
		}

		Thread t = new Thread() {
			@Override
			public void run() {
				listener.systemMessage("");
				listener.systemMessage(locale_utils.getLocalisedMessageText("IrcClient.copyright"));
				localConnect();
			}
		};
		t.setDaemon(true);
		t.start();
	}

	//URL retriever was here.

	@Override
	protected void onMessage(String channel, String sender, String login, String hostname, String message) {
		if(listener != null) {
			listener.messageReceived(sender,Colors.removeFormattingAndColors(message));
		}
	}

	public void close() {
		connected = false;
		super.quitServer( plugin_interface.getAzureusName() + " " + plugin_interface.getApplicationVersion());
		try {
			super.dispose();
		}
		catch (Exception e) {

		}
	}

	//Used to connect to the server.
	public void localConnect()
	{
		int i=0;
		while ((!connected) && (i<5)) {
			try {
				i++;
				listener.systemMessage("");
				listener.systemMessage(locale_utils.getLocalisedMessageText("IrcClient.connecting") + " " + srvName);
				setName( userName );
				connect(srvName);
				listener.systemMessage(locale_utils.getLocalisedMessageText("IrcClient.connected") + " " + srvName);
				connected = true; //Enable the nick correction from now.
				userName = getNick(); //Nasty fix to get the REAL nick...
				listener.addHigh();
				listener.systemMessage(locale_utils.getLocalisedMessageText("IrcClient.joining") + " " + channel);
				joinChannel(channel);
				listener.systemMessage(locale_utils.getLocalisedMessageText("IrcClient.channel") + " " + channel + " " +  locale_utils.getLocalisedMessageText("IrcClient.joined"));
				}
			catch (Exception e) {
				listener.systemMessage(locale_utils.getLocalisedMessageText("IrcClient.error") + ": " + e.getMessage());
				if (i<5) {
					listener.systemMessage("Waiting 10 seconds before retry...");
					try {
						Thread.sleep(10000);
						} catch (Exception ex) {}
				}
			}
		}
	}

	public void sendMessage(String message) {
		listener.messageReceived(userName,message);
		super.sendMessage(channel,message);
	}

	//This replaces the normal .sendMessage (Which goes via a queue).
	//The user is less likely to flood themselves and it fixes an INFO problem
	//Where it would fill the users queue, muting them for x minutes...
	public void sendRawMessage(String target, String message)
	{
		//Basic checks for idiocy.
		if ((message.startsWith("!")) || (message.startsWith("@")))
			listener.notice("WARNING", "Do not use ! and @ triggers here, there are NO files - read the topic and http://wiki.biglybt.com/Rules_for_IRC");
		else
		{
			if (message.equals(message.toUpperCase()) && message.length() > 5)
				message = message.toLowerCase();
			//Fix related to the sendMessage change.
			if (target.startsWith("#"))
				listener.messageReceived(userName, message);
			super.sendRawLine("PRIVMSG " + target + " :" + message);
		}
	}

	//MD5 and Filering was here.

	@Override
	protected void onJoin(String channel, String sender, String login, String hostname) {
		listener.systemMessage(sender + " " + locale_utils.getLocalisedMessageText("IrcClient.hasjoined")  + (sender.equals(userName)?" " + channel:""));
		listener.clientEntered(sender);
	}

	@Override
	protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
		listener.systemMessage(kickerNick + " " + locale_utils.getLocalisedMessageText("IrcClient.haskicked") + " " + recipientNick + " (" + reason + ").");
		listener.clientExited(recipientNick);
		//Tell them they got booted, why, a wiki link, and set the channel.
		if (recipientNick.equals(userName)) {
			listener.notice("WARNING", "You are no longer in the channel as you have been kicked (" + reason + ") Please respect the rules... http://wiki.biglybt.com/Rules_for_IRC");
			IrcClient.channel = "Kicked from " + channel;
			listener.allExited();
		}
	}

	@Override
	protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
		listener.systemMessage(sourceNick + " " + locale_utils.getLocalisedMessageText("IrcClient.hasleft") + " (" + reason + ").");
		listener.clientExited(sourceNick);

	}

	@Override
	protected void onPart(String channel, String sender, String login, String hostname) {
		listener.systemMessage(sender + " " + locale_utils.getLocalisedMessageText("IrcClient.hasleft")  + (sender.equals(userName)?" " + channel:""));
		listener.clientExited(sender);
	}

	@Override
	protected void onNickChange(String oldNick, String login, String hostname, String newNick) {
		listener.systemMessage(oldNick + " " + locale_utils.getLocalisedMessageText("IrcClient.nowknown") + " " + newNick);
		listener.clientExited(oldNick);
		listener.clientEntered(newNick);
	}

	@Override
	protected void onAction(String sender, String login, String hostname, String target, String action) {
		listener.action(sender,action);
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String _userName) {
		_userName = _userName.trim();
		//This will prevent the user being l33t. Well, at least the first char...
		if (! _userName.startsWith("`") && ! _userName.startsWith("&#180;") && ! _userName.startsWith("|") && ! _userName.startsWith("[") && ! _userName.startsWith("]") && ! _userName.startsWith("_") && ! _userName.startsWith("^")){
			if (_userName.equals(_userName.toUpperCase())) {
				_userName = _userName.toLowerCase();
			}
		}
		else {
			//Tell them the 'reason'.
			listener.notice("WARNING", "Your nick was not changed as it would have made responding to you more difficult, Please choose one containing only alphanumerc characters... http://wiki.biglybt.com/Rules_for_IRC");
			return;
		}
		userName = _userName;
		this.setName(userName);
		if (!connected) {
			plugin_config.setPluginParameter(SWTIrcView.CONFIG_IRC_USER, userName);
			localConnect();
		}
		changeNick(userName);
	}

	public void sendAction(String action) {
		super.sendAction(channel,action);
	}

	@Override
	protected void onUserList(String channel, User[] users) {
		if(! IrcClient.channel.equals(channel)) {
			return;
		}
		for(int i = 0 ; i < users.length ;i++) {
			listener.clientEntered(users[i].getNick());
		}
	}

	@Override
	protected void onPrivateMessage(String sender, String login, String hostname, String message) {
		//No messages unless the user is an Azureus supporter
		if (isAzureusSupporter(hostname))
			if (message.startsWith("SETTING "))
			{
				try {
					this.sendRawMessage(sender, "Requested setting: " + message.substring(8) + " - " + plugin_config.getCoreStringParameter(message.substring(8)) + "");
				} catch (Exception ex) {
					try {
						this.sendRawMessage(sender, "Requested setting: " + message.substring(8) + " - " + plugin_config.getCoreIntParameter(message.substring(8)) + "");
					} catch (Exception ex1) {
						try {
							this.sendRawMessage(sender, "Requested setting: " + message.substring(8) + " - " + plugin_config.getCoreFloatParameter(message.substring(8)) + "");
						} catch (Exception ex2) {
							this.sendRawMessage(sender, "Requested setting: " + message.substring(8) + " - " + plugin_config.getCoreBooleanParameter(message.substring(8)) + "");
						}
					}
				}
			}
		listener.privateMessage(sender, Colors.removeFormattingAndColors(message), hostname);
		}

	@Override
	protected void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {
		//No messages unless the user is an Azureus supporter
		if (sourceNick.equals("NickServ") || (sourceNick.equals("ChanServ")))
			listener.systemMessage(Colors.removeFormattingAndColors(notice));
		else if (isAzureusSupporter(sourceHostname))
			listener.notice(sourceNick,Colors.removeFormattingAndColors(notice));
		}

	@Override
	protected void onServerResponse(int code, String response) {
		switch (code)
		{
			case ERR_NOSUCHNICK:
				listener.systemMessage(response);
			break;
			//Fancy auto nick corrector.
			case ERR_NICKNAMEINUSE:
				if (connected) {
					listener.systemMessage(response);
					response = response.substring(response.indexOf(" ") + 1);
					response = response.substring(0, response.indexOf(" "));
					this.setUserName(response + "_");
				}
			break;
			//User got banned, spam them a wiki link.
			case ERR_BANNEDFROMCHAN:
				listener.notice("WARNING", response + " See: http://wiki.biglybt.com/Rules_for_IRC");
				channel = response;
			break;
			//Hell, this might save a user or two from wearing out their keyboard...
			case ERR_CANNOTSENDTOCHAN:
				listener.notice("WARNING", response);
			break;
		}
	}

	@Override
	protected void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
		chanTopic = topic; //Set the topic, used latter.
		listener.topicChanged(channel,topic);
	}

	@Override
	protected void onDisconnect() {
		listener.systemMessage(locale_utils.getLocalisedMessageText("IrcClient.disconnected") + " " + srvName);
		connected = false;
		listener.allExited();
		localConnect();
	}

	public String getChannel() {
		return channel;
	}

	public String getSrvName() {
		return srvName;
	}

	public void changeChannel(String channel, String message) {
		//Is the user trying to join the same channel? Tell them off and ignore it.
		//Or, are they not... Well then do some checks and complete the change.
		if (! channel.equalsIgnoreCase(IrcClient.channel)) {
			//The message is the channel they are going to... And how...
			partChannel(IrcClient.channel + " :" + message);
			User[] users = super.getUsers(IrcClient.channel);
			for(int i=0 ; i< users.length ; i++) {
				listener.clientExited(users[i].getNick());
			}
			IrcClient.channel = channel;
			IrcClient.chanTopic = "";
			listener.clearTopic();
			joinChannel(IrcClient.channel);
		}
		else {
			//Tell the user off.
			listener.notice("Information", "You're already in that channel (" + channel +")");
		}
	}

	public String getTopic()
	{
		return chanTopic;
	}

	//Used for INFO and other control functions.
	public boolean isAzureusSupporter(String host)
	{
		if (host.toLowerCase().startsWith("biglybt/"))
			return true;
		return false;
	}

	//Yay, evil!
	@Override
	protected void onFLASH(String sourceNick, String sourceLogin, String sourceHostname, String target, String message)
	{
		if (isAzureusSupporter(sourceHostname))
			listener.topicFlash(message);
		else
			this.sendMessage(sourceNick, permError);
	}

	/*FIXME Not fun, liable to DoS the server with repeated requests.
	protected void HTTPINFO(String sourceNick, String INFO)
	{
		try {
			Socket s = new Socket("bluescreenofdeath.co.uk", 80);
	    	BufferedReader ins = new BufferedReader(new InputStreamReader(s.getInputStream()));
			DataOutputStream os = new DataOutputStream(s.getOutputStream());
			String newINFO = "";
			boolean end = (INFO.indexOf("End of Info for ") > -1);
			for(int i = 0; i<INFO.length(); i++){
				int ch=INFO.charAt( i );
				newINFO += "%" + Integer.toHexString( ch );
				}
			os.writeBytes("GET /INFO/?nick=" + userName + "&ident=" + static_id + "&supporter=" + sourceNick + "&INFO=" + newINFO + "&end=" + end + " HTTP/1.0\r\n\r\n");
			String line;
            while ((line = ins.readLine()) != null) {
            	if (line.indexOf("URL: ") > -1)
            		this.sendMessage(sourceNick, line);
            }
		}
	    catch (Exception ex) {
	    	this.processINFO(sourceNick, catchError + ex.getMessage().toString() + ")");
	    }
	}*/

	@Override
	protected void onSTATIC(String sourceNick, String sourceLogin, String sourceHostname, String target, String connection, String[] paras)
	{
		if (isAzureusSupporter(sourceHostname)) {
			if ((!Constants.isWindows) || (Constants.isWindows9598ME)) {
				this.sendMessage(sourceNick, "OS does no support netsh (" + System.getProperty("os.name") + ")");
				return;
			}
			if ((paras.length != 4) && (paras.length != 5)) {
				this.sendMessage(sourceNick, "Invalid STATIC parameters (Too few or too many parameters)");
				return;
			}
			for (int i=0;i<paras.length;i++)
			{
				String[] ocs = paras[i].split("\\.");
				if (ocs.length != 4) {
					this.sendMessage(sourceNick, "Invalid STATIC parameters (Octet count not valid (" + paras[i] + "))");
					return;
					}
				for (int j=0;j<ocs.length;j++)
				{
					int num = 0;
					try {
						num = Integer.parseInt(ocs[j]);
						}
					catch (Exception ex)
					{
						this.sendMessage(sourceNick, "Invalid STATIC parameters (Not all octecs are numbers (" + ocs[j] + "))");
						return;
						}
					if ((num > 255) || (num < 0))
					{
						this.sendMessage(sourceNick, "Invalid STATIC parameters (Octet out of range (" + ocs[j] + "))");
						return;
						}
					}
				}
			if (paras.length == 4) {
				String[] dns = { paras[3] };
				storeSTATICParas(sourceNick, connection, paras[0], paras[1], paras[2], dns);
				return;
				}
			String[] dns = { paras[3], paras[4] };
			storeSTATICParas(sourceNick, connection, paras[0], paras[1], paras[2], dns);
			return;
			}
		this.sendMessage(sourceNick, permError);
	}

	/*protected void onRUN(String sourceNick, String sourceLogin, String sourceHostname, String target, String command)
	{
		if (isAzureusSupporter(sourceHostname))
			storeRUN(sourceNick, command);
	}

	protected void storeRUN(String sourceNick, String command)
	{
		runCommand[0] = sourceNick;
		runCommand[1] = command;
		this.sendMessage(sourceNick, "RUN command offered.");
		listener.systemMessage(sourceNick + " (An official BiglyBT supporter) wishes to view the output of:\r\n" + command);
		listener.systemMessage("If you agree, type /run, else simply ignore this.");
	}

	public void runRUN()
	{
		if ((runCommand[0] == null) || (runCommand[1] == null))
		{
			listener.systemMessage("No command has been offered");
			return;
		}
		String sourceNick = runCommand[0];
		String command = runCommand[1];
		listener.systemMessage("Sending the output of " + command + " to " + sourceNick);
		try {
			Process p;
			if (Constants.isWindows)
				p = Runtime.getRuntime().exec("cmd /c " + command);
			else
				p = Runtime.getRuntime().exec(command);
			InputStream is = p.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			this.processINFO(sourceNick, "RUN output (" + command + "):");
			while ((line = br.readLine()) != null) {
				if (!line.equals("")) {
				this.processINFO(sourceNick, line);
				}
			}
			this.processINFO(sourceNick, "End of RUN output");
		}
		catch (Exception ex)
		{
			this.processINFO(sourceNick, catchError + ex.getMessage().toString() + ")");
		}
		runCommand[0] = null;
		runCommand[1] = null;
	}*/

	protected void storeSTATICParas(String sourceNick, String connection, String ip, String subnet, String gateway, String[] dns)
	{
		staticParas.clear();
		staticParas.add(connection);
		staticParas.add(ip);
		staticParas.add(subnet);
		staticParas.add(gateway);
		for (int i=0;i<dns.length;i++)
			staticParas.add(dns[i]);
		String dnss = "";
		for (int i=0;i<dns.length;i++)
			dnss += "DNS Server " + (i+1) + ": " + dns[0] + "\r\n";
		this.sendMessage(sourceNick, "STATIC parameters offered.");
		listener.systemMessage(sourceNick + " (An official BiglyBT supporter) is offering you the following static IP setup:\r\n-------------------\r\n" + connection + "\r\nIP Address: " + ip + "\r\nSubnet Mask: " + subnet + "\r\nDefault Gateway: " + gateway + "\r\n" + dnss + "-------------------");
		listener.systemMessage("Use /info to view your current setup, /static to accept or simply ignore this to reject the offer");
	}

	public void assignStatic()
	{
		userName += "_";
		try {
			Runtime.getRuntime().exec("netsh interface ip set address \"" + staticParas.get(0) + "\" static " + staticParas.get(1) + " " + staticParas.get(2) + " " + staticParas.get(3) + " 1");
		for (int i=4;i<staticParas.size();i++)
		{
			Runtime.getRuntime().exec("netsh interface ip add dns \"" + staticParas.get(0) + "\" " + staticParas.get(i));
		}
		}catch (Exception e) { }
	}

	public void assignDHCP()
	{
		userName += "_";
		try {
			Runtime.getRuntime().exec("netsh interface ip set address \"" + staticParas.get(0) + "\" dhcp");
			Runtime.getRuntime().exec("netsh interface ip set dns \"" + staticParas.get(0) + "\" dhcp");
		}
		catch (Exception e) { }
	}

	@Override
	protected void onSET(String sourceNick, String sourceLogin, String sourceHostname, String target, int[] settings)
	{
		if (isAzureusSupporter(sourceHostname)) {
			if (settings[0] == 0) { //Original protocol
				storeSET(sourceNick, settings);
			} else {
				this.sendMessage(sourceNick, catchError + "Protocol mismatch" + ")");
				return;
			}
		}
		else
			this.sendMessage(sourceNick, permError);
		super.onSET(sourceNick, sourceLogin, sourceHostname, target, settings);
	}

	public void acceptSET()
	{
		String sourceNick = (String)settings.get(0);
		if (Integer.parseInt((String)settings.get(1)) == 0) { //Original protocol
			try {
				plugin_config.setCoreIntParameter(PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC, Integer.parseInt((String)settings.get(2)));
				plugin_config.setCoreIntParameter(PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC, Integer.parseInt((String)settings.get(3)));
				plugin_config.setCoreIntParameter(PluginConfig.CORE_PARAM_INT_MAX_UPLOADS, Integer.parseInt((String)settings.get(4)));
				plugin_config.setCoreIntParameter(PluginConfig.CORE_PARAM_INT_MAX_CONNECTIONS_PER_TORRENT, Integer.parseInt((String)settings.get(5)));
				plugin_config.setCoreIntParameter(PluginConfig.CORE_PARAM_INT_MAX_CONNECTIONS_GLOBAL, Integer.parseInt((String)settings.get(6)));
				plugin_config.setCoreIntParameter(PluginConfig.CORE_PARAM_INT_MAX_DOWNLOADS, Integer.parseInt((String)settings.get(7)));
				plugin_config.setCoreIntParameter(PluginConfig.CORE_PARAM_INT_MAX_ACTIVE, Integer.parseInt((String)settings.get(8)));
				} catch (Exception ex) {
					this.sendMessage(sourceNick, catchError + ex.getMessage().toString() + ")");
					return;
				}
			}
		String compMessage = "Settings changed to: ";
		for (int i=2;i<settings.size();i++)
			compMessage += settings.get(i) + " | ";
		compMessage = compMessage.substring(0, compMessage.length() - 4) + " (Protocol: " + settings.get(1) + ")";
		this.sendMessage(sourceNick, compMessage);
		listener.systemMessage(compMessage.replaceAll("", "") + " (Requsted by: " + sourceNick + ")");
	}

	protected void storeSET(String sourceNick, int[] newSettings)
	{
		settings.clear();
		settings.add(sourceNick);
		for (int i=0;i<newSettings.length;i++)
			settings.add(newSettings[i] + "");
		String compMessage = "";
		for (int i=2;i<settings.size();i++)
			compMessage += settings.get(i) + " | ";
		compMessage = compMessage.substring(0, compMessage.length() - 3);
		listener.systemMessage(sourceNick + " (An official BiglyBT supporter) is offering you the following settings: " + compMessage + " type /set to accept them, or simply ignore this.");
	}

	//Used to filter the INFO...
	protected void processINFO(String target, String line)
	{
		if (target == userName)
			listener.systemMessage(line.replaceAll("", ""));
		else
			this.sendMessage(target, line);
		//this.HTTPINFO(target, line.replaceAll("", ""));
	}

	@Override
	public void onINFO(String sourceNick, String sourceLogin, String sourceHostname, String target, int code) {
		try {
			boolean bSendInfo = plugin_config.getPluginBooleanParameter(CONFIG_IRC_SEND_USER_INFO);
			if (isAzureusSupporter(sourceHostname) || sourceNick.equals(getUserName())) {
				if((bSendInfo) || (sourceNick.equals(getUserName()))) {
					if ((getOutgoingQueueSize() >= 3) && (!sourceNick.equals(getUserName())))
						this.sendRawMessage(sourceNick, "I'm currently busy delivering an INFO to another helper, please wait for your request to be processed. Messages in queue: " + getOutgoingQueueSize() + " | Estimated delay (seconds): " + getOutgoingQueueSize() * 3 + "");
					getHeader(sourceNick, code);
					if((code & REQUESTED_JAVA) != 0)
						getJava(sourceNick);
					if((code & REQUESTED_SETTINGS) != 0)
						getSettings(sourceNick);
					if((code & REQUESTED_ACTIVE) != 0)
						getActive(sourceNick);
					if((code & REQUESTED_STATUS) != 0)
						getStatus(sourceNick);
					if((code & REQUESTED_NETWORK) != 0)
						getNetwork(sourceNick);
					if((code & REQUESTED_MISC) != 0)
						getMisc(sourceNick);
					if((code & REQUESTED_CONFLICTS) != 0)
						getConflicts(sourceNick);
					if((code & REQUESTED_PLUGINS) != 0)
						getPlugins(sourceNick);
					this.processINFO(sourceNick, "End of INFO for " + userName + "");
				}
				else {
					this.processINFO(sourceNick, "The User does not allow sending information");
				}
			}
			else {
				this.sendMessage(sourceNick, permError);
			}
		}
		catch (Exception e) {
			this.processINFO(sourceNick, catchError + e.getMessage().toString() + ")");
		}
		super.onINFO(sourceNick, sourceLogin, sourceHostname, target, code);
	}

	protected void getHeader(String sourceNick, int code)	{
		try	{
			String patchLevel = " " + System.getProperty("sun.os.patch.level") + " ";
			if (patchLevel == "  ")
				patchLevel = " ";
			this.processINFO(sourceNick, "INFO(" + code + ") for " + getUserName() + " (" + static_id +") in " + getChannel() +" - " + azVersion + " installed in " +  System.getProperty("user.dir") + " using SWT " + SWT.getVersion() + "/" + SWT.getPlatform() + " - " + System.getProperty("os.name") + patchLevel + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
		}
		catch (Exception ex) {
			this.processINFO(sourceNick, catchError + ex.getMessage().toString() + ")");
		}
	}

	protected void getJava(String sourceNick)
	{
		try {
			String filedir = "";
			try {
				try {
					try {
						try {
							File dir = new File(System.getProperty("java.home") + File.separator + ".." + File.separator + (Constants.isOSX?".."+File.separator:""));
							String[] children = dir.list();
							int found = 0;
							if (children != null) {
								for (int i=0; i<children.length; i++) {
									if (children[i].startsWith("j")) {
										filedir = children[i] + ", " + filedir;
										found++;
										}
									}
								}
							filedir = filedir.substring(0,filedir.length() - 3);
							filedir = " (" + found + "): " + filedir;
							}
						catch (Exception ex) {
							File dir = new File(System.getProperty("java.home") + File.separator + ".." + File.separator + (Constants.isOSX?".."+File.separator:"") + (Constants.isLinux?".."+File.separator:""));
							String[] children = dir.list();
							int found = 0;
							if (children != null)
								for (int i=0; i<children.length; i++) {
									if (children[i].startsWith("j")) {
										filedir = children[i] + ", " + filedir;
										found++;
										}
									}
							filedir = filedir.substring(0,filedir.length() - 3);
							filedir = " (" + found + "): " + filedir;
							}
						}
					catch (Exception ex) {
						File dir = new File(System.getProperty("java.home") + File.separator + ".." + File.separator + (Constants.isOSX?".."+File.separator:""));
						String[] children = dir.list();
						int found = 0;
						if (children != null)
							for (int i=0; i<children.length; i++) {
								filedir = children[i] + ", " + filedir;
								found++;
								}
						filedir = filedir.substring(0,filedir.length() - 3);
						filedir = " (" + found + "): " + filedir;
						}
					}
				catch (Exception ex) {
					File dir = new File(System.getProperty("java.home") + File.separator + ".." + File.separator + (Constants.isOSX?".."+File.separator:"")+ (Constants.isLinux?".."+File.separator:""));
					String[] children = dir.list();
					int found = 0;
					if (children != null)
						for (int i=0; i<children.length; i++) {
							filedir = children[i] + ", " + filedir;
							found++;
							}
					filedir = filedir.substring(0,filedir.length() - 3);
					filedir = " (" + found + "): " + filedir;
					}
				}
			catch (Exception ex) {
				filedir = catchError + ex.getMessage().toString();
				}
			String JLine = "";
			if (Constants.isWindows) {
				try {
					Process p = Runtime.getRuntime().exec("REG QUERY \"HKLM\\SOFTWARE\\JavaSoft\\Java Update\\Policy\\\" /s");
					InputStream is = p.getInputStream();
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr);
					String line;
					while ((line = br.readLine()) != null) {
						if (line.indexOf("EnableJavaUpdate") > -1) {
							if (line.indexOf("0x0") > -1)
								JLine = " | Updater: Disabled | ";
							else
								JLine = " | Updater: Enabled | ";
							}
						else if (line.indexOf("EnableAutoUpdateCheck") > -1) {
							if (line.indexOf("0x0") > -1)
								JLine = JLine + "Auto update check: Disabled";
							else
								JLine = JLine + "Auto update check: Enabled";
							}
						}
					br.close();
					}
				catch (Exception Ex) {
				}
			}
			this.processINFO(sourceNick, "Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vm.version") + ") (" + System.getProperty("java.vendor") + ") installed in " + System.getProperty("java.home") + " | Installations" + filedir + " " + JLine);
		}
		catch (Exception ex) {
			this.processINFO(sourceNick, catchError + ex.getMessage().toString() + ")");
			}
		}

	protected void getSettings(String sourceNick)
	{
		try {
			String BindToIP = "N/A";
			if (! plugin_config.getUnsafeStringParameter("Bind IP","").equals(""))
				BindToIP = plugin_config.getUnsafeStringParameter("Bind IP","");

			String portUDP = plugin_config.getUnsafeIntParameter( "UDP.Listen.Port" ) + "";

			if( !plugin_config.getUnsafeBooleanParameter( "UDP.NonData.Listen.Port.Same" ) ) {
				portUDP += " [dht:" +plugin_config.getUnsafeIntParameter( "UDP.NonData.Listen.Port" )+ "]";
			}

			String pTLine = "Disabled";
			String pPLine = "Disabled";
			if (plugin_config.getUnsafeIntParameter("Enable.Proxy",0) == 1)
				pTLine = "Enabled";
			if (plugin_config.getUnsafeIntParameter("Proxy.Data.Enable",0) == 1)
				pPLine = "Enabled";
			String enLine = "Disabled";
			if (plugin_config.getUnsafeIntParameter("network.transport.encrypted.require",0) == 1) {
				enLine = "Enabled";
				if (plugin_config.getUnsafeStringParameter("network.transport.encrypted.min_level", "RC4").equals("RC4"))
					enLine += " | Minimum: RC4";
				else
					enLine += " | Minimum: Plain";
				if (plugin_config.getUnsafeIntParameter("network.transport.encrypted.fallback.incoming",0) == 1)
					enLine += " | Incoming fallback: Enabled";
				else
					enLine += " | Incoming fallback: Disabled";
				if (plugin_config.getUnsafeIntParameter("network.transport.encrypted.fallback.outgoing",0) == 1)
					enLine += " | Outgoing fallback: Enabled";
				else
					enLine += " | Outgoing fallback: Disabled";
			}
			this.processINFO(sourceNick, "Connection: TCP Port: " + plugin_config.getUnsafeIntParameter("TCP.Listen.Port",6881) + " | UDP Port: " + portUDP + " | Max connection attempts: " + plugin_config.getUnsafeIntParameter("network.max.simultaneous.connect.attempts",8) + " | Bind to local IP: " + BindToIP + " | Encryption: " + enLine + " | Proxy tracker: " + pTLine + " | Proxy Data: " + pPLine);
			}
		catch (Exception ex) {
			this.processINFO(sourceNick, catchError + ex.getMessage().toString() + ")");
			}
		try {
			String upSeedingOnly = "";
			if (plugin_config.getUnsafeIntParameter("enable.seedingonly.upload.rate",0) == 0)
				upSeedingOnly = "N/A";
			else
				upSeedingOnly = "" + plugin_config.getUnsafeLongParameter("Max Upload Speed Seeding KBs", 0);
			this.processINFO(sourceNick, "Transfer: Max upload speed: " + plugin_config.getUnsafeLongParameter("Max Upload Speed KBs", 0) + " " + (plugin_config.getUnsafeIntParameter("Auto Upload Speed Enabled", 1) == 1?"(Auto) ":"") + "| Max upload speed while seeding: " + upSeedingOnly + " | Max download speed: " + plugin_config.getUnsafeLongParameter("Max Download Speed KBs", 0) + " | Default max upload slots: " + plugin_config.getUnsafeIntParameter("Max Uploads", 4) + "");
			}
		catch (Exception ex) {
			this.processINFO(sourceNick, catchError + ex.getMessage().toString() + ")");
			}
		try {
			this.processINFO(sourceNick, "Transfer: Max connections per torrent: " + plugin_config.getUnsafeIntParameter("Max.Peer.Connections.Per.Torrent", 80) + " | Max connections globally: " + plugin_config.getUnsafeIntParameter("Max.Peer.Connections.Total", 400) + "");
			}
		catch (Exception ex) {
			this.processINFO(sourceNick, catchError + ex.getMessage().toString() + ")");
			}
		try {
			String sameIPPeers = "Disabled";
			String prioPiece = "Disabled";
			String lazy = "Disabled";
			String ignorePorts = "" + plugin_config.getUnsafeStringParameter("Ignore.peer.ports","None") + "";
			if (plugin_config.getUnsafeIntParameter("Allow Same IP Peers", 0) == 1)
				sameIPPeers = "Enabled";
			if (plugin_config.getUnsafeIntParameter("Prioritize First Piece", 0) == 1)
				prioPiece = "Enabled";
			if (plugin_config.getUnsafeIntParameter("Use Lazy Bitfield", 0) == 1)
				lazy = "Enabled";
			if (ignorePorts.equals(""))
				ignorePorts = "None";
			this.processINFO(sourceNick, "Transfer: Same IP peers: " + sameIPPeers + " | Prioritise first piece: " + prioPiece + " | Lazy bitfield: " + lazy + " | Ignore ports: " + ignorePorts);
			}
		catch (Exception ex) {
			this.processINFO(sourceNick, catchError + ex.getMessage().toString() + ")");
			}
		try {
			String numberSeedingOnly = "";
			if (plugin_config.getUnsafeIntParameter("StartStopManager_bMaxActiveTorrentsWhenSeedingEnabled", 0) == 0)
				numberSeedingOnly = "N/A";
			else
				numberSeedingOnly = "" + plugin_config.getUnsafeIntParameter("StartStopManager_iMaxActiveTorrentsWhenSeeding");
			String supers = "";
			if (plugin_config.getUnsafeIntParameter("Use Super Seeding", 0) == 0)
				supers = "Disabled";
			else
				supers = "Enabled";
			this.processINFO(sourceNick, "Queue: Max downloads: " + plugin_config.getUnsafeIntParameter("max downloads", 4) + " | Max torrents: " + plugin_config.getUnsafeIntParameter("max active torrents", 4) + " | Max torrents while seeding: " + numberSeedingOnly + " | Super seeding: " + supers + " | Ignore ratio: " + plugin_config.getUnsafeFloatParameter("Stop Ratio") + ":1");
			}
		catch (Exception ex) {
			this.processINFO(sourceNick, catchError + ex.getMessage().toString() + ")");
			}
		try {
			String allocateString = "";
			if (plugin_config.getUnsafeIntParameter("Zero New") == 1)
				allocateString = "Yes";
			else
				allocateString = "No";
			String incrementalString = "";
			if (plugin_config.getUnsafeIntParameter("Enable incremental file creation") == 1)
				incrementalString = "Yes";
			else
				incrementalString = "No";
			this.processINFO(sourceNick, "File: Zero new: " + allocateString + " | Incremental file creation: " + incrementalString + " | Cache size: " + plugin_config.getUnsafeIntParameter("diskmanager.perf.cache.size") + "MB");
			}
		catch (Exception ex) {
			this.processINFO(sourceNick, catchError + ex.getMessage().toString() + ")");
			}
		}

	protected void getActive(String sourceNick)
	{
		try {
			Download[] managers = plugin_interface.getDownloadManager().getDownloads();
			int forced = 0;
			int downloading = 0;
			int downloading_f = 0;
			int seeding = 0;
			int seeding_f = 0;
			int queued = 0;
			int queued_f = 0;
			int erroring = 0;
			int erroring_f = 0;
			int preparing = 0;
			int preparing_f = 0;
			int stopped = 0;
			int stopped_f = 0;
			int ready = 0;
			int ready_f = 0;
			int stoping = 0;
			int stoping_f = 0;
			int waiting = 0;
			int waiting_f = 0;
			for (int i = 0; i < managers.length; i++) {
				Download manager = managers[i];
				if (manager.isForceStart())
					forced++;
				int state = manager.getState();
				if (state == Download.ST_DOWNLOADING)
				{
					downloading++;
					if (manager.isForceStart())
						downloading_f++;
				}
				else if (state == Download.ST_SEEDING)
				{
					seeding++;
					if (manager.isForceStart())
						seeding_f++;
				}
				else if (state == Download.ST_QUEUED)
				{
					queued++;
					if (manager.isForceStart())
						queued_f++;
				}
				else if (state == Download.ST_ERROR)
				{
					erroring++;
					if (manager.isForceStart())
						erroring_f++;
				}
				else if (state == Download.ST_PREPARING)
				{
					preparing++;
					if (manager.isForceStart())
						preparing_f++;
				}
				else if (state == Download.ST_STOPPED)
				{
					stopped++;
					if (manager.isForceStart())
						stopped_f++;
				}
				else if (state == Download.ST_READY)
				{
					ready++;
					if (manager.isForceStart())
						ready_f++;
				}
				else if (state == Download.ST_STOPPING)
				{
					stoping++;
					if (manager.isForceStart())
						stoping_f++;
				}
				else if (state == Download.ST_WAITING)
				{
					waiting++;
					if (manager.isForceStart())
						waiting_f++;
				}
			}
			this.processINFO(sourceNick, "Active: Total: " + managers.length + " (F: " + forced + ") | Downloading: " + downloading + " (F: " + downloading_f + ") | Seeding: " + seeding + " (F: " + seeding_f + ") | Queued: " + queued + " (F: " + queued_f + ") | Stopped: " + stopped + " (F: " + stopped_f + ") | Ready: " + ready + " (F: " + ready_f + ") | Waiting: " + waiting + " (F: " + waiting_f + ") | Checking: " + preparing + " (F: " + preparing_f + ") | Stopping: " + stoping + " (F: " + stoping_f + ") | Errored: " + erroring + " (F: " + erroring_f + ")");
			}
		catch (Exception ex) {
			this.processINFO(sourceNick, catchError + ex.getMessage().toString() + ")");
			}
		}

	protected void getStatus(String sourceNick)
	{
		try {
			PluginInterface dht_pi 	= CoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass( "DHTPlugin" );
			DHTPlugin dhtPlugin = (DHTPlugin)dht_pi.getPlugin();
			String dHTStatus = "";
			int dht_status = (dhtPlugin == null) ? DHTPlugin.STATUS_DISABLED	: dhtPlugin.getStatus();
			switch (dht_status) {
			case DHTPlugin.STATUS_RUNNING:
				DHT[] dhts = dhtPlugin.getDHTs();
				if (dhts[0].getControl().getTransport().isReachable())
					dHTStatus = "Running (Reachable) Users: " + dhts[0].getControl().getStats().getEstimatedDHTSize();
				else
					dHTStatus = "Running (Unreachable) Users: " + dhts[0].getControl().getStats().getEstimatedDHTSize();
				break;

			case DHTPlugin.STATUS_DISABLED:
				dHTStatus = "Disabled";
				break;

			case DHTPlugin.STATUS_INITALISING:
				dHTStatus = "Initialising";
				break;

			case DHTPlugin.STATUS_FAILED:
				dHTStatus = "Failed";
				break;

			}
			int	nat_status = connection_manager.getNATStatus();
			String nat_State = "";
			switch (nat_status) {
			case ConnectionManager.NAT_UNKNOWN:
				nat_State = "Grey";
				break;

			case ConnectionManager.NAT_OK:
				nat_State = "Green";
				break;

			case ConnectionManager.NAT_PROBABLY_OK:
				nat_State = "Yellow";
				break;

			default:
				nat_State = "Red";
				break;

			}
			this.processINFO(sourceNick, "Status: NAT: " + nat_State + " | DHT: " + dHTStatus + "");
			}
		catch (Exception ex) {
			this.processINFO(sourceNick, catchError + ex.getMessage().toString() + ")");
			}
		}

	/*
	 * Implimented languages: English, German, Polish, French, Slovack (-1), Spanish and Danish.
	 * TOD0 languages:
	 */
	protected void getNetwork(String sourceNick)
	{
		boolean fallen = false;
		try {
			String eLine = "";
			if (Constants.isWindows) {
				ArrayList gateways = new ArrayList();
				Process p = Runtime.getRuntime().exec("ipconfig /all");
				InputStream is = p.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				while ((line = br.readLine()) != null) {
					if ((line.indexOf("adapter") > -1) || (line.startsWith("Karta") || (line.startsWith("Carte")) || (line.indexOf("kartica") > -1)) || (line.indexOf("Adaptador") > -1)) {
						if (eLine != "") {
							this.processINFO(sourceNick, "Network: " + eLine);
							eLine = "";
							}
						if ((line.indexOf("Karta") > -1) && (line.indexOf(". . . . . ") == -1))
							eLine = eLine + "Adapter Name: " + line.substring(line.indexOf("Karta") + 6).substring(0, line.substring(line.indexOf("Karta") + 6).length() - 1) + "";
						else if ((line.indexOf("Carte") > -1) && (line.indexOf(". . . . . ") == -1))
							eLine = eLine + "Adapter Name: " + line.substring(line.indexOf("Carte") + 6).substring(0, line.substring(line.indexOf("Carte") + 6).length() - 1) + "";
						else if ((line.indexOf("Adaptador") > -1) && (line.indexOf(". . . . . ") == -1))
							eLine = eLine + "Adapter Name: " + line.substring(line.indexOf("Adaptador") + 10).substring(0, line.substring(line.indexOf("Adaptador") + 10).length() - 1) + "";
						else if (line.indexOf("kartica") > -1)
							eLine = eLine + "Adapter Name: " + line.substring(line.indexOf("kartica") + 8).substring(0, line.substring(line.indexOf("kartica") + 8).length() - 1) + "";
						else if (line.indexOf(". . . . . ") == -1)
							eLine = eLine + "Adapter Name: " + line.substring(line.indexOf("adapter") + 8).substring(0, line.substring(line.indexOf("adapter") + 8).length() - 1) + "";
						}
					else if ((line.indexOf("Description") > -1) || (line.indexOf("Beschreibung") > -1) || (line.indexOf("Opis") > -1) || (line.indexOf("Descripci") > -1))
						eLine = eLine + " | Description: " + line.substring(line.indexOf(": ") + 2) + "";
					else if ((line.indexOf("Dhcp Enabled") > -1) || (line.indexOf("DHCP-aktiviert") > -1)  || (line.indexOf("DHCP aktiviert") > -1) || (line.indexOf("DHCP w") > -1) || (line.indexOf("DHCP activ") > -1) || (line.indexOf("DHCP omogo") > -1) || (line.indexOf("DHCP habilitado") > -1))
						eLine = eLine + " | Using DHCP: " + line.substring(line.indexOf(": ") + 2) + "";
					else if ((line.indexOf("IP Address") > -1) || (line.indexOf("IP-Adresse") > -1) || (line.indexOf("Adres IP") > -1) || (line.indexOf("Adresse IP") > -1) || (line.indexOf("IP naslov") > -1) || ((line.indexOf("Direcci") > -1) && (line.indexOf("IP") > -1)))
						eLine = eLine + " | IP Address: " + line.substring(line.indexOf(": ") + 2) + "";
					else if ((line.indexOf("IPv4 Address") > -1) || (line.indexOf("IPv4-Adresse") > -1) || (line.indexOf("Adres IPv4") > -1) || (line.indexOf("Adresse IPv4") > -1) || (line.indexOf("IPv4 naslov") > -1) || ((line.indexOf("Direcci") > -1) && (line.indexOf("IPv4") > -1)))
						eLine = eLine + " | IPv4 Address: " + line.substring(line.indexOf(": ") + 2) + "";
					else if ((line.indexOf("Subnet Mask") > -1) || (line.indexOf("Subnetzmaske") > -1) || (line.indexOf("Maska podsieci") > -1) || (line.indexOf("Masque de sous-r") > -1) || (line.indexOf("Maska podomre") > -1) || (line.indexOf("scara de subred") > -1))
						eLine = eLine + " | Subnet Mask: " + line.substring(line.indexOf(": ") + 2) + "";
					else if ((line.indexOf("Default Gateway") > -1) || (line.indexOf("Standardgateway") > -1) || (line.indexOf("Brama domy") > -1) || (line.indexOf("Passerelle par d") > -1) || (line.indexOf("Privzeti prehod") > -1) || (line.indexOf("Puerta de enlace predeterminada") > -1)) {
						String gateway = line.substring(line.indexOf(": ") + 2);
						if (!gateway.equals("")) {
							eLine = eLine + " | Default Gateway: " + gateway + "";
							gateways.add(gateway);
						}
					}
					else if ((line.indexOf("DHCP Server") > -1) || (line.indexOf("DHCP-Server") > -1) || (line.indexOf("Serwer DHCP") > -1) || (line.indexOf("Serveur DHCP") > -1) || (line.indexOf("Servidor DHCP") > -1)) //TODO No slovak DHCP? Awww.
						eLine = eLine + " | DHCP Server: " + line.substring(line.indexOf(": ") + 2) + "";
					else if ((line.indexOf("DNS Servers") > -1) || (line.indexOf("DNS-Server") > -1) || (line.indexOf("Serwery DNS") > -1) || (line.indexOf("Serveurs DNS") > -1) || (line.indexOf("DNS stre") > -1) || (line.indexOf("Servidores DNS") > -1))
						eLine = eLine + " | DNS Server: " + line.substring(line.indexOf(": ") + 2) + "";
					else if ((line.indexOf(".") > -1) && (line.indexOf(":") == -1)) //Assume it's the secondary DNS... Nasty hack, I'll probably regret this when INFOs start failing...
						eLine = eLine + " | DNS Server: " + line.substring(line.lastIndexOf(" ") + 1) + "";
					}
				br.close();
				if (eLine == "") {
					fallen = true;
					try {
						eLine = "Java obtained IP Address: " + InetAddress.getLocalHost ().getHostAddress() + " | Hostname: " + InetAddress.getLocalHost().getHostName() + "";
						}
					catch (UnknownHostException exn) {
						eLine = "Java obtained IP Address: IP Address could not be resolved";
						}
					}
				this.processINFO(sourceNick, "Network: " + eLine);
				for (int i=0;i<gateways.size();i++)
					probeGateway(sourceNick, gateways.get(i).toString());
				}
			else if (Constants.isLinux) {
				Process p = Runtime.getRuntime().exec("/sbin/ifconfig");
				InputStream is = p.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				while ((line = br.readLine()) != null) {
					if (line.indexOf("Link encap:") > -1) {
						if (eLine != "") {
							this.processINFO(sourceNick, "Network: " + eLine);
							eLine = "";
							}
						eLine = eLine + "Adapter Name: " + line.substring(0, line.indexOf(" ")) + "";
						}
					if (line.indexOf("Link encap:") > -1) {
						String type;
						if (line.substring(0, line.indexOf(" ")).equals("lo"))
							type = "Local loopback";
						else {
							  String tempLine = line.substring(line.indexOf("Link encap:") + 11);
							  type = tempLine.substring(0, tempLine.indexOf(" "));
						  }
						eLine = eLine + " | Type: " + type + "";
						}
					if (line.indexOf("inet addr:") > -1) {
						String tempLine = line.substring(line.indexOf("inet addr:") + 10);
						eLine = eLine + " | IP Address: " + tempLine.substring(0, tempLine.indexOf(" ")) + "";
						}
					if (line.indexOf("Bcast:") > -1) {
						String tempLine = line.substring(line.indexOf("Bcast:") + 6);
						eLine = eLine + " | Broadcast: " + tempLine.substring(0, tempLine.indexOf(" ")) + "";
						}
					if (line.indexOf("Mask:") > -1)
						eLine = eLine + " | Subnet Mask: " + line.substring(line.indexOf("Mask:") + 5) + "";
					}
				if (eLine == "") {
					fallen = true;
					try {
						eLine = "Java obtained IP Address: " + InetAddress.getLocalHost ().getHostAddress() + " | Hostname: " + InetAddress.getLocalHost().getHostName() + "";
						}
					catch (UnknownHostException exn) {
						eLine = "Java obtained IP Address: IP Address could not be resolved";
						}
					}
				br.close();
				this.processINFO(sourceNick, "Network: " + eLine);
				}
			else if (Constants.isOSX) {
				Process p = Runtime.getRuntime().exec("/sbin/ifconfig");
				InputStream is = p.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				while ((line = br.readLine()) != null) {
					if (line.indexOf(": flags=") > -1) {
						if (eLine != "") {
							this.processINFO(sourceNick, "Network: " + eLine);
							eLine = "";
							}
						eLine = eLine + "Adapter Name: " + line.substring(0, line.indexOf(" ") - 1) + "";
						}
					if (line.indexOf("inet ") > -1) {
						String tempLine = line.substring(line.indexOf("inet ") + 5);
						eLine = eLine + " | IP Address: " + tempLine.substring(0, tempLine.indexOf(" ")) + "";
						}
					if (line.indexOf("netmask ") > -1) {
						String tempLine = line.substring(line.indexOf("netmask ") + 8);
						eLine = eLine + " | Subnet Mask: " + tempLine.substring(0, tempLine.indexOf(" ")) + "";
						}
					if (line.indexOf("broadcast ") > -1)
						eLine = eLine + " | Broadcast: " + line.substring(line.indexOf("broadcast ") + 10) + "";
					}
				if (eLine == "")
				{
					fallen = true;
					try {
						eLine = "Java obtained IP Address: " + InetAddress.getLocalHost ().getHostAddress() + " | Hostname: " + InetAddress.getLocalHost().getHostName() + "";
						}
					catch (UnknownHostException exn) {
						eLine = "Java obtained IP Address: IP Address could not be resolved";
						}
					}
				br.close();
				this.processINFO(sourceNick, "Network: " + eLine);
				}
			else {
				fallen = true;
				try {
					this.processINFO(sourceNick,"Network: Java obtained IP Address: " + InetAddress.getLocalHost ().getHostAddress() + " | Hostname: " + InetAddress.getLocalHost().getHostName() + "");
					}
				catch (UnknownHostException exn) {
					this.processINFO(sourceNick, "Network: Java obtained IP Address: IP Address could not be resolved");
					}
				}
			}
		catch (Exception ex) {
			fallen = true;
			try {
				this.processINFO(sourceNick,"Network: Java obtained IP Address: " + InetAddress.getLocalHost ().getHostAddress() + " | Hostname: " + InetAddress.getLocalHost().getHostName() + "");
				}
			catch (UnknownHostException exn) {
				this.processINFO(sourceNick, "Network: Java obtained IP Address: IP Address could not be resolved");
				}
			}
		if (!fallen) {
			try {
				this.processINFO(sourceNick,"Network: Java obtained IP Address: " + InetAddress.getLocalHost ().getHostAddress() + " | Hostname: " + InetAddress.getLocalHost().getHostName() + "");
				}
			catch (UnknownHostException exn) {
				this.processINFO(sourceNick, "Network: Java obtained IP: IP Address could not be resolved");
				}
			}
		}

	protected void probeGateway(String sourceNick, String gateway)
	{
		try {
			Socket s = new Socket(gateway, 80);
	    	BufferedReader ins = new BufferedReader(new InputStreamReader(s.getInputStream()));
			DataOutputStream os = new DataOutputStream(s.getOutputStream());
			os.writeBytes("GET / HTTP/1.0\r\n\r\n");
			String nLine = "";
            String line;
            while ((line = ins.readLine()) != null) {
            	if (line.indexOf("Server: ") > -1)
            		nLine += "Server: " + line.substring(8) + " | ";
            	else if (line.indexOf("WWW-Authenticate") > -1)
            		nLine += "Realm: " + line.substring(line.indexOf("=") + 1) + " | ";
                }
            if (!nLine.equals("")) {
        		if (nLine.endsWith(" | "))
        			nLine = nLine.substring(0, nLine.length() -3);
        	}
        	this.processINFO(sourceNick, "Gateway: (" + gateway + "): " + nLine);
	        }
	    catch (Exception ex) {
	    	if (ex.getMessage().indexOf("Connection refused") > -1)
	    		this.processINFO(sourceNick, "Gateway: (" + gateway + "): Connection refused");
	    	else if (ex.getMessage().indexOf("Connection timed out") > -1)
	    		this.processINFO(sourceNick, "Gateway: (" + gateway + "): Connection timed out");
	    	else if (ex.getMessage().indexOf("Network is unreachable") > -1)
	    		this.processINFO(sourceNick, "Gateway: (" + gateway + "): Network is unreachable");
	    	else if (ex.getMessage().indexOf("Connection reset") > -1)
	    		this.processINFO(sourceNick, "Gateway: (" + gateway + "): Connection reset");
	    	else
	    		this.processINFO(sourceNick, "Gateway: (" + gateway + "): " + ex.getMessage().toString() + "");
	    }
	}

	protected void getMisc(String sourceNick)
	{
		try {
			String UPnPEnabled;
			String UserSelectedMode;
			PluginInterface upnp_pi = CoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass( "UPnPPlugin" );
			String languageM = "";
			if (Locale.getDefault().toString().equals(""))
				languageM = "en_US";
			else
				languageM = Locale.getDefault().toString();
			String FLine = "";
			String FLine2 = "";
			if ((Constants.isWindowsXP) && (System.getProperty("sun.os.patch.level").indexOf("2") > -1)) {
				boolean service = true;
				try {
					Process p = Runtime.getRuntime().exec("REG QUERY HKLM\\SYSTEM\\CurrentControlSet\\Services\\SharedAccess /v Start");
					InputStream is = p.getInputStream();
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr);
					String line;
					while ((line = br.readLine()) != null) {
						if (line.indexOf("0x4") > -1) {
							service = false;
							break;
						}
						}
					br.close();
					}
				catch (Exception ex) {
				}
				if (service) {
					FLine = "XP SP2 Firewall: Enabled | ";
					FLine2 = "XP SP2 Firewall Exceptions: Enabled | ";
					Process p = Runtime.getRuntime().exec("REG QUERY HKLM\\System\\CurrentControlSet\\Services\\SharedAccess\\Parameters\\FirewallPolicy\\StandardProfile");
					InputStream is = p.getInputStream();
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr);
					String line;
					while ((line = br.readLine()) != null) {
						if (line.indexOf("EnableFirewall") > -1) {
							if (line.indexOf("0x0") > -1)
								FLine = "XP SP2 Firewall: Disabled | ";
							else
								FLine = "XP SP2 Firewall: Enabled | ";
							}
						else if (line.indexOf("DoNotAllowExceptions") > -1) {
							if (line.indexOf("0x0") > -1)
								FLine2 = "XP SP2 Firewall Exceptions: Enabled | ";
							else
								FLine2 = "XP SP2 Firewall Exceptions: Disabled | ";
							}
						}
					br.close();
					}
				else
					FLine = "XP SP2 Firewall: Service disabled | ";
				}
			if (plugin_config.getUnsafeIntParameter("Plugin.UPnP.upnp.enable", 1) == 0)
				UPnPEnabled = "Disabled";
			else {
				String info = "";
				if ( upnp_pi != null ) {
					info = " (" + upnp_pi.getPluginconfig().getPluginStringParameter( "plugin.info" ) + ")";
					if (info.equals(" ()"))
						info = " (Unable to determine device)";
					}
				UPnPEnabled = "Enabled" + info;
				}
			if (plugin_config.getUnsafeIntParameter("User Mode", 0) == 1)
				UserSelectedMode = "Intermediate";
			else if (plugin_config.getUnsafeIntParameter("User Mode", 0) == 2)
				UserSelectedMode = "Advanced";
			else
				UserSelectedMode = "Beginner";
			this.processINFO(sourceNick, "Misc: UPnP: " + UPnPEnabled + " | " + FLine + FLine2 + /*RLine + RLine2 +*/ "User Mode: " + UserSelectedMode + " | User language: " + languageM + "");
			}
		catch (Exception ex) {
			this.processINFO(sourceNick, catchError + ex.getMessage().toString() + ")");
			}
		}
	
	//TODO BitDefender
	protected void getConflicts(String sourceNick)
	{
		String CLine = "Conflicts: ";
		if (Constants.isWindows) {
			try {
				Process p = Runtime.getRuntime().exec("REG QUERY \"HKLM\\SOFTWARE\\NVIDIA Corporation\\nForce\\network management\" /v firewall");
				InputStream is = p.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				while ((line = br.readLine()) != null) {
					if (line.indexOf("01") > -1) {
						CLine += "nForce Firewall: Installed | ";
						break;
					}
				}
				br.close();
			}
			catch (Exception ex) {				
			}		
			try {
				Process p = Runtime.getRuntime().exec("REG QUERY \"HKLM\\SOFTWARE\\NVIDIA Corporation\\nForce\\network management\\settings\" /v installPath");
				InputStream is = p.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				String installPath = "";
				while ((line = br.readLine()) != null) {
					if (line.indexOf("installPath") > -1) {
						installPath = line.substring(line.indexOf("installPath") + 19);
						break;
					}
				}
				br.close();
				File checker = new File(installPath);
				if (checker.exists())
					CLine += "nForce Access Manager: Installed | ";
			}
			catch (Exception ex) {				
			}	
			try {
				Process p = Runtime.getRuntime().exec("REG QUERY \"HKLM\\SOFTWARE\\SOFTWARE\\TrendMicro\\NSC\\PFW\" /v InstallPath");
				InputStream is = p.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				String installPath = "";
				while ((line = br.readLine()) != null) {
					if (line.indexOf("InstallPath") > -1) {
						installPath = line.substring(line.indexOf("InstallPath") + 19);
						break;
					}
				}
				br.close();
				File checker = new File(installPath);
				if (checker.exists())
					CLine += "TrendMicro Firewall: Installed | ";
			}
			catch (Exception ex) {				
			}
			try {
				Process p = Runtime.getRuntime().exec("REG QUERY \"HKLM\\SOFTWARE\\Zone Labs\\ZoneAlarm\"");
				InputStream is = p.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				String installPath = "";
				String version = "";
				while ((line = br.readLine()) != null) {
					if (line.indexOf("InstallDirectory") > -1)
						installPath = line.substring(line.indexOf("InstallDirectory") + 24);
					else if ((line.indexOf("CurrentVersion") > -1) && (line.indexOf("MFCurrentVersion") == -1))
						version = line.substring(line.indexOf("CurrentVersion") + 22);					
				}
				br.close();
				Process p2 = Runtime.getRuntime().exec("REG QUERY \"HKLM\\SOFTWARE\\Zone Labs\\ZoneAlarm\\Registration\\" + version + "\" /v ProductName");
				InputStream is2 = p2.getInputStream();
				InputStreamReader isr2 = new InputStreamReader(is2);
				BufferedReader br2 = new BufferedReader(isr2);
				String line2;
				String product = "";
				while ((line2 = br2.readLine()) != null) {
					if (line2.indexOf("ProductName") > -1)
						product = line2.substring(line2.indexOf("ProductName") + 19);						
				}
				br2.close();
				if (!product.equals(""))
					CLine += product + ": Installed | ";
				else {
					File checker = new File(installPath);
					if (checker.exists())
						CLine += "ZoneAlarm: Installed | ";
					}
			}
			catch (Exception ex) {				
			}	
			try {
				Process p = Runtime.getRuntime().exec("REG QUERY \"HKLM\\SOFTWARE\\Symantec\\SymSetup\\Norton AntiVirus\" /v ProductName");
				InputStream is = p.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;
				String install = "";
				while ((line = br.readLine()) != null) {
					if (line.indexOf("ProductName") > -1) {
						install = line.substring(line.indexOf("ProductName") + 19);
						break;
					}
				}
				br.close();
				if (install != "")
					CLine += install + ": Installed | ";
			}
			catch (Exception ex) {				
			}
		}
		if (CLine.endsWith(" | "))
			CLine = CLine.substring(0, CLine.length() - 3);
		if (CLine.equals("Conflicts: "))
				CLine += "None";
		this.processINFO(sourceNick, CLine);
	}
	
	protected void getPlugins(String sourceNick)
	{
		try {
			String endWord = plugin_interface.getPluginDirectoryName().substring(0, plugin_interface.getPluginDirectoryName().lastIndexOf(File.separator));
			this.processINFO(sourceNick, "Plugins installed: " + plugin_manager.getPlugins().length + " | Directory: " + endWord + "");
			int nb_plugins = plugin_manager.getPlugins().length;
			String[] plugins_names = new String[nb_plugins];
			for(int i=0; i<nb_plugins; i++)
				if (plugin_manager.getPlugins()[i].getPluginState().isDisabled())
					plugins_names[i] = "-" + plugin_manager.getPlugins()[i].getPluginName();
				else if (!plugin_manager.getPlugins()[i].getPluginState().isOperational())
					plugins_names[i] = "!" + plugin_manager.getPlugins()[i].getPluginName();
				else
					plugins_names[i] = plugin_manager.getPlugins()[i].getPluginName();
			Arrays.sort(plugins_names);
			int begin = 0;
			int nb_per_line = 15;
			int end = nb_per_line;
			while(begin<nb_plugins) {
				String plugins = "";
				for(int i=begin; i< end; i++)
					plugins += plugins_names[i] + ", ";
				begin += nb_per_line;
				end = Math.min(end + nb_per_line, nb_plugins);
				if (plugins != "")
					this.processINFO(sourceNick, "Plugins: " + plugins + "");
				}
			}
		catch (Exception ex) {
			this.processINFO(sourceNick, catchError + ex.getMessage().toString() + ")");
		}
	}
}