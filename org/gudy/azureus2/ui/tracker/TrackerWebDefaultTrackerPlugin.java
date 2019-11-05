/*
 * File    : TrackerWebDefaultPlugin.java
 * Created : 08-Dec-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.ui.tracker;

/**
 * @author parg
 *
 */

import java.net.*;
import java.io.*;
import java.util.*;

import com.biglybt.core.util.SystemTime;
import com.biglybt.core.util.Constants;
import com.biglybt.core.util.TimeFormatter;
import com.biglybt.pif.PluginEvent;
import com.biglybt.pif.PluginEventListener;
import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.PluginListener;
import com.biglybt.pif.download.*;
import com.biglybt.pif.torrent.*;
import com.biglybt.pif.tracker.*;
import com.biglybt.pif.tracker.web.*;
import com.biglybt.pif.ui.menus.*;
import com.biglybt.pif.ui.config.*;
import com.biglybt.pif.ui.model.BasicPluginConfigModel;
import com.biglybt.pif.ui.*;
import com.biglybt.pif.ui.tables.*;

import com.biglybt.plugin.magnet.MagnetPlugin;
import com.biglybt.plugin.upnp.UPnPPlugin;


public class 
TrackerWebDefaultTrackerPlugin
	extends TrackerWeb
{
	private DirectoryParameter	upload_dir;
	private BooleanParameter	upload_passive_only;
	
	private MagnetPlugin		magnet_plugin;
	
	private long				rss_feed_pub_time	= SystemTime.getCurrentTime();
	private boolean				initial_sharing_complete;
	
	@Override
	public void
	initialize(
		PluginInterface _plugin_interface )
	{	
		super.initialize( _plugin_interface );
	
		plugin_interface.getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle( "org.gudy.azureus2.ui.tracker.internat.Messages");
				
		plugin_interface.addEventListener(
			new PluginEventListener()
			{
				@Override
				public void
				handleEvent(
					PluginEvent ev )
				{
					if ( ev.getType() == PluginEvent.PEV_INITIAL_SHARING_COMPLETE ){
						
						initial_sharing_complete	= true;
					}
				}
			});
		
		UIManager	ui_manager = plugin_interface.getUIManager();
		
		BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "plugins.TrackerWeb");
				
			// migrate from core to plugin
		
		final byte[]	BLANK_PASSWORD = plugin_interface.getUtilities().getSecurityManager().calculateSHA1(new byte[0]);

		final BooleanParameter enable 		= config_model.addBooleanParameter2( CONFIG_TRACKER_PUBLISH_ENABLE, "ConfigView.section.tracker.publishenable", CONFIG_TRACKER_PUBLISH_ENABLE_DEFAULT );

		final StringParameter title_param = config_model.addStringParameter2( CONFIG_TRACKER_TITLE, "azplugins.tracker.title", CONFIG_TRACKER_TITLE_DEFAULT );
		
		final BooleanParameter enable_absolute_urls = config_model.addBooleanParameter2( CONFIG_TRACKER_PUBLISH_ENABLE_ABSOLUTE_URLS, "azplugins.tracker.absoluteurls", CONFIG_TRACKER_PUBLISH_ENABLE_ABSOLUTE_URLS_DEFAULT);
				
		BooleanParameter enable_details	= config_model.addBooleanParameter2( CONFIG_TRACKER_PUBLISH_ENABLE_DETAILS, "ConfigView.section.tracker.publishenabledetails", CONFIG_TRACKER_PUBLISH_ENABLE_DETAILS_DEFAULT );
		
		BooleanParameter enable_peer_details	= config_model.addBooleanParameter2( CONFIG_TRACKER_PUBLISH_ENABLE_PEER_DETAILS, "ConfigView.section.tracker.publishenablepeerdetails", CONFIG_TRACKER_PUBLISH_ENABLE_PEER_DETAILS_DEFAULT );
		
		IntParameter skip = config_model.addIntParameter2( CONFIG_TRACKER_SKIP, "ConfigView.section.tracker.torrentsperpage", CONFIG_TRACKER_SKIP_DEFAULT );
		
		String[]	css_list = getAvailableCSS();
		
		StringListParameter css_param = config_model.addStringListParameter2( CONFIG_CSS, "azplugins.tracker.css", css_list, CONFIG_CSS_DEFAULT );
		
		final BooleanParameter enable_tracker_more_columns = config_model.addBooleanParameter2( CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_MORE_COLUMNS, "azplugins.tracker.morecolumns", CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_MORE_COLUMNS_DEFAULT);
		
		final BooleanParameter enable_tracker_less_columns_switch = config_model.addBooleanParameter2( CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_LESS_COLUMNS_SWITCH, "azplugins.tracker.lesscolumnsswitch", CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_LESS_COLUMNS_SWITCH_DEFAULT);
			
		final BooleanParameter enable_tracker_more_columns_switch = config_model.addBooleanParameter2( CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_MORE_COLUMNS_SWITCH, "azplugins.tracker.morecolumnsswitch", CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_MORE_COLUMNS_SWITCH_DEFAULT);
		
		final BooleanParameter enable_sorting = config_model.addBooleanParameter2( CONFIG_TRACKER_PUBLISH_ENABLE_SORTING, "azplugins.tracker.enablesorting", CONFIG_TRACKER_PUBLISH_ENABLE_SORTING_DEFAULT);
		
		final BooleanParameter enable_search = config_model.addBooleanParameter2( CONFIG_TRACKER_PUBLISH_ENABLE_SEARCH, "azplugins.tracker.enablesearch", CONFIG_TRACKER_PUBLISH_ENABLE_SEARCH_DEFAULT);

		config_model.createGroup( "azplugins.tracker.details_group",
				new Parameter[]{ 
					enable_tracker_more_columns, 
					enable_tracker_less_columns_switch,
					enable_tracker_more_columns_switch,
					enable_sorting,
					enable_search,
					enable_details,
					enable_peer_details,
					skip, 
					css_param});

		final BooleanParameter override_auth	= config_model.addBooleanParameter2( CONFIG_TRACKER_OVERRIDE_AUTH_ENABLE, "azplugins.tracker.overrideauth", CONFIG_TRACKER_OVERRIDE_AUTH_ENABLE_DEFAULT );

		final StringParameter	username = config_model.addStringParameter2( CONFIG_TRACKER_USERNAME, "azplugins.tracker.username", "" );
		
		final PasswordParameter	password = config_model.addPasswordParameter2( CONFIG_TRACKER_PASSWORD, "azplugins.tracker.password", PasswordParameter.ET_SHA1, new byte[0] );
		
		config_model.createGroup( "azplugins.tracker.auth_group",
				new Parameter[]{ 
					override_auth, 
					username,
					password });

		/// Categories
		
		final BooleanParameter enable_categories	= config_model.addBooleanParameter2( CONFIG_TRACKER_PUBLISH_ENABLE_CATEGORIES, "ConfigView.section.tracker.enablecategories", CONFIG_TRACKER_PUBLISH_ENABLE_CATEGORIES_DEFAULT );
		
		final LabelParameter category_label = config_model.addLabelParameter2( "azplugins.tracker.categoryexcludelist.info" );
		
		final StringParameter	category_list		= config_model.addStringParameter2( CONFIG_CATEGORY_LIST, "azplugins.tracker.categorylist", CONFIG_CATEGORY_LIST_DEFAULT );
		
		final StringParameter	category_exclude_list		= config_model.addStringParameter2( CONFIG_CATEGORY_EXCLUDE_LIST, "azplugins.tracker.categoryexcludelist", CONFIG_CATEGORY_EXCLUDE_LIST_DEFAULT );
		
		config_model.createGroup( "azplugins.tracker.category_group",
				new Parameter[]{ 
						enable_categories, 
						category_label,
						category_list,
						category_exclude_list});	
		
		ParameterListener	pl4 = 
			new ParameterListener()
			{
				@Override
				public void
				parameterChanged(
					Parameter	p )
				{
					boolean	e = enable_categories.getValue();
					
					category_label.setEnabled( e );
					category_list.setEnabled( e );
					category_exclude_list.setEnabled( e );
				}
			};
			
			enable_categories.addListener( pl4 );
		
		pl4.parameterChanged(null);
		
		/// RSS Feed
		
		final BooleanParameter	rss_enable = config_model.addBooleanParameter2( CONFIG_RSS_ENABLE, "azplugins.tracker.rssenable", CONFIG_RSS_ENABLE_DEFAULT );
		
		final BooleanParameter	rss_pubok = config_model.addBooleanParameter2( CONFIG_RSS_PUBLISH_PUBLISHED, "azplugins.tracker.rsspublishpublished", CONFIG_RSS_PUBLISH_PUBLISHED_DEFAULT );
		
		final BooleanParameter 	rss_auth	= config_model.addBooleanParameter2( CONFIG_TRACKER_RSS_AUTH_ENABLE, "azplugins.tracker.rssauth", CONFIG_TRACKER_RSS_AUTH_ENABLE_DEFAULT );

		final StringParameter	rss_username = config_model.addStringParameter2( CONFIG_TRACKER_RSS_USERNAME, "azplugins.tracker.rssusername", "" );
		
		final PasswordParameter	rss_password = config_model.addPasswordParameter2( CONFIG_TRACKER_RSS_PASSWORD, "azplugins.tracker.rsspassword", PasswordParameter.ET_SHA1, new byte[0] );
		
		final BooleanParameter	rss_category_enable = config_model.addBooleanParameter2( CONFIG_RSS_CATEGORY_ENABLE, "azplugins.tracker.rsscategoryenable", CONFIG_RSS_CATEGORY_ENABLE_DEFAULT );
		
		final BooleanParameter	rss_category_tracker_enable = config_model.addBooleanParameter2( CONFIG_RSS_CATEGORY_TRACKER_ENABLE, "azplugins.tracker.rsscategorytrackerenable", CONFIG_RSS_CATEGORY_TRACKER_ENABLE_DEFAULT );
		
		final StringParameter	rss_category_list		= config_model.addStringParameter2( CONFIG_RSS_CATEGORY_LIST, "azplugins.tracker.categorylist", CONFIG_RSS_CATEGORY_LIST_DEFAULT );
		
		final StringParameter	rss_category_exclude_list		= config_model.addStringParameter2( CONFIG_RSS_CATEGORY_EXCLUDE_LIST, "azplugins.tracker.categoryexcludelist", CONFIG_RSS_CATEGORY_EXCLUDE_LIST_DEFAULT );


		config_model.createGroup( "azplugins.tracker.rss_group",
				new Parameter[]{ 
					rss_enable,
					rss_pubok,
					rss_auth,
					rss_username,
					rss_password,
					rss_category_enable,
					rss_category_tracker_enable,
					rss_category_list,
					rss_category_exclude_list});

		ParameterListener	pl = 
			new ParameterListener()
			{
				@Override
				public void
				parameterChanged(
					Parameter	p )
				{
					boolean web			= enable.getValue();
					boolean	override 	= override_auth.getValue();
					boolean	rss			= rss_enable.getValue();
					boolean	r_auth		= rss_auth.getValue();
					boolean more_info	= enable_tracker_more_columns.getValue();
					boolean rss_cat_enabled = rss_category_enable.getValue(); 
					boolean rss_cat_tracker_enabled = rss_category_tracker_enable.getValue();
					
					override_auth.setEnabled( web );
					
					rss_pubok.setEnabled( rss );
					
					username.setEnabled( web && override );
					password.setEnabled( web && override );
					
					rss_auth.setEnabled( web && override && rss );
					
					rss_username.setEnabled( web && override && rss && r_auth );
					rss_password.setEnabled( web && override && rss && r_auth );
					
					rss_category_enable.setEnabled( rss);
					
					rss_category_tracker_enable.setEnabled( web && rss && rss_cat_enabled);
					
					rss_category_list.setEnabled( rss && rss_cat_enabled && (web &&!rss_cat_tracker_enabled || !web));
					rss_category_exclude_list.setEnabled( rss && rss_cat_enabled && (web &&!rss_cat_tracker_enabled || !web));
					
					enable_tracker_more_columns_switch.setEnabled( web && !more_info );
					enable_tracker_less_columns_switch.setEnabled( web && more_info );
					
				}
			};
			
		enable.addListener( pl );
		override_auth.addListener( pl );
		rss_enable.addListener( pl );
		rss_auth.addListener( pl );
		rss_category_enable.addListener(pl);
		rss_category_tracker_enable.addListener(pl);
		enable_tracker_more_columns.addListener( pl );
		
			// set initial values
		
		pl.parameterChanged(null);
		
		/// Upload
		
		BooleanParameter enable_upload				= config_model.addBooleanParameter2( CONFIG_TRACKER_UPLOAD_DATA_ENABLE, "azplugins.tracker.showuploaddata", CONFIG_TRACKER_UPLOAD_DATA_ENABLE_DEFAULT );
		BooleanParameter upload_passive_default		= config_model.addBooleanParameter2( CONFIG_TRACKER_UPLOAD_PASSIVE_DEFAULT, "azplugins.tracker.uploadpassivedefault", CONFIG_TRACKER_UPLOAD_PASSIVE_DEFAULT_DEFAULT );
		
		upload_passive_only		= config_model.addBooleanParameter2( CONFIG_TRACKER_UPLOAD_PASSIVE_ONLY, "azplugins.tracker.uploadpassiveonly", CONFIG_TRACKER_UPLOAD_PASSIVE_ONLY_DEFAULT );
				
		upload_dir		= config_model.addDirectoryParameter2( CONFIG_TRACKER_UPLOAD_DIR, "azplugins.tracker.uploaddir", "" );
		
		config_model.createGroup( "azplugins.tracker.upload_group",
				new Parameter[]{ 
					enable_upload, 
					upload_passive_default,
					upload_passive_only,
					upload_dir});	
		
		final BooleanParameter	port_override = config_model.addBooleanParameter2( CONFIG_TRACKER_PORT_OVERRIDE, "azplugins.tracker.portoverride", CONFIG_TRACKER_PORT_OVERRIDE_DEFAULT );

		final IntParameter	http_port 	= config_model.addIntParameter2( CONFIG_TRACKER_HTTP_PORT, "azplugins.tracker.httpport", CONFIG_TRACKER_HTTP_PORT_DEFAULT );
		final IntParameter	https_port 	= config_model.addIntParameter2( CONFIG_TRACKER_HTTPS_PORT, "azplugins.tracker.httpsport", CONFIG_TRACKER_HTTPS_PORT_DEFAULT );

		ParameterListener	pl3 = 
			new ParameterListener()
			{
				@Override
				public void
				parameterChanged(
					Parameter	p )
				{
					boolean	e = enable.getValue() && port_override.getValue();
					
					http_port.setEnabled( e );
					https_port.setEnabled( e );
				}
			};
			
		enable.addListener( pl3 );
		port_override.addListener( pl3 );
		
		pl3.parameterChanged(null);
		
		config_model.createGroup( "azplugins.tracker.port_override_group",
				new Parameter[]{ 
					port_override, 
					http_port,
					https_port });
		
		
		ParameterListener	pl2 = 
			new ParameterListener()
			{
				@Override
				public void
				parameterChanged(
					Parameter	p )
				{
					boolean	e = enable.getValue();
					
					title_param.setEnabled( e );
					enable_categories.setEnabled( e );
					category_label.setEnabled( e );
					category_list.setEnabled( e );
					category_exclude_list.setEnabled( e );
				}
			};
			
		enable.addListener( pl2 );
			
		pl2.parameterChanged(null);
		
		enable.addEnabledOnSelection( enable_absolute_urls );
		enable.addEnabledOnSelection( enable_tracker_more_columns);
		enable.addEnabledOnSelection( enable_sorting );
		enable.addEnabledOnSelection( enable_search );
		enable.addEnabledOnSelection( enable_details );
		enable.addEnabledOnSelection( enable_peer_details );
		enable.addEnabledOnSelection( css_param );

		enable.addEnabledOnSelection( enable_upload );
		enable.addEnabledOnSelection( skip );
		enable.addEnabledOnSelection( upload_dir );
		enable.addEnabledOnSelection( port_override );
		enable.addEnabledOnSelection( http_port );
		enable.addEnabledOnSelection( https_port );
						

		
		int	http 	= http_port.getValue();
		int https 	= https_port.getValue();
		
		final List	my_contexts = new ArrayList();
		
		if ( port_override.getValue()){
			
			tracker.removePageGenerator( this );

			if ( http != 0 ){
			
				try{
					TrackerWebContext ctx = plugin_interface.getTracker().createWebContext( "Tracker Web", http, Tracker.PR_HTTP );
					
					my_contexts.add( ctx );
					
					ctx.addPageGenerator( this );
					
				}catch( TrackerException e ){
					
					e.printStackTrace();
				}
			}
			
			if ( https != 0 ){
				
				try{
					TrackerWebContext ctx = plugin_interface.getTracker().createWebContext( "Tracker Web", https, Tracker.PR_HTTPS );
				
					my_contexts.add( ctx );
					
					ctx.addPageGenerator( this );
					
				}catch( TrackerException e ){
					
					e.printStackTrace();
				}
			}
			
			plugin_interface.addListener(
				new PluginListener()
				{
					@Override
					public void
					initializationComplete()
					{
						PluginInterface pi_upnp = plugin_interface.getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );
						
						if ( pi_upnp != null ){
							
							for (int i=0;i<my_contexts.size();i++){
								
								TrackerWebContext	ctx = (TrackerWebContext)my_contexts.get(i);
							
								((UPnPPlugin)pi_upnp.getPlugin()).addMapping( 
												plugin_interface.getPluginName() + ": port override", 
												true, 
												ctx.getURLs()[0].getPort(), 
												true );
							}
						}
					}
					
					
					@Override
					public void
					closedownInitiated()
					{
					}
					
					@Override
					public void
					closedownComplete()
					{
					}
				});
				
		}else{
			
			my_contexts.add( plugin_interface.getTracker());
		}
		
		if ( my_contexts.size() > 0 ){
		
			TableContextMenuItem menu = plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTRACKER, "MyTrackerView.webui.contextmenu.copyurl" );
			
			menu.addListener(
				new MenuItemListener()
				{
					@Override
					public void
					selected(
						MenuItem		_menu,
						Object			_target )
					{
						TrackerTorrent trackerTorrent = (TrackerTorrent)((TableRow)_target).getDataSource();
					  
						if ( trackerTorrent == null ){
							
							return;
						}
						
						Torrent	torrent = trackerTorrent.getTorrent();
						
						String	hash_str = formatters.encodeBytesToString( torrent.getHash());
						
						String	torrent_name = torrent.getName();
	
						URL	url = torrent.getAnnounceURL();
						
						boolean https_announce = url.getProtocol().toLowerCase().equals("https");
						
							// could be udp -> use http
						
						String url_str = https_announce?"https":"http";
						
							// work out the correct host to use. We can just use the tracker settings 
						
						String	host;
						
						URL[]	tracker_urls = tracker.getURLs();
						
						if ( tracker_urls.length > 0 ){
						
							host	= tracker_urls[0].getHost();
							
						}else{
							
							host	= url.getHost();
						}
						
						url_str += "://" + host;
						
							// and now work out the port, just grab the first we find
						
						TrackerWebContext	context = (TrackerWebContext)my_contexts.get(0);
							
						URL[]	urls = context.getURLs();
	
						int	port = urls[0].getPort();
						
						if ( port != -1 ){
							
							url_str += ":" + port;
						}			
						
						try{
							url_str +=  "/torrents/" + URLEncoder.encode( torrent_name, Constants.DEFAULT_ENCODING).replaceAll("\\+", "%20") + ".torrent?" + hash_str;
							
							plugin_interface.getUIManager().copyToClipBoard( url_str );
							
						}catch( Throwable  e ){
							
							e.printStackTrace();
						}
					}
				});
		}
		
		if ( override_auth.getValue()){
			
			for (int i=0;i<my_contexts.size();i++){
				
				((TrackerWebContext)my_contexts.get(i)).addAuthenticationListener(
					new TrackerAuthenticationListener()
					{
						@Override
						public boolean
						authenticate(
							URL			resource,
							String		r_user,
							String		r_password )
						{
							StringParameter		p_user;
							PasswordParameter	p_password;
							
							if ( 	rss_auth.getValue() &&
									(	resource.toString().endsWith("rss_feed.xml") ||
										resource.toString().indexOf( "rss_feed.xml?") != -1 )){
								
								p_user		= rss_username;
								p_password	= rss_password;
								
							}else{
								
								p_user		= username;
								p_password	= password;
							
							}
							

							byte[]	p_sha1 = p_password.getValue();
							
							if ( p_sha1.length == 0 || Arrays.equals( p_sha1, BLANK_PASSWORD )){
								
								return( true );
							}
							
							if ( !r_user.equals( p_user.getValue())){
								
								return( false );
							}
							
							return( Arrays.equals( p_sha1, plugin_interface.getUtilities().getSecurityManager().calculateSHA1(r_password.getBytes())));
						}
					
						@Override
						public byte[]
						authenticate(
							URL			resource,
							String		user )
						{
							return( null );
						}
					});
			}
		}
		
			// trap changes that alter the feed publication date
		
		plugin_interface.getTracker().addListener(
			new TrackerListener()
			{
				@Override
				public void
				torrentAdded(
					TrackerTorrent	torrent )
				{
					rss_feed_pub_time	= SystemTime.getCurrentTime();					
				}
				
				@Override
				public void
				torrentChanged(
					TrackerTorrent	torrent )
				{
					rss_feed_pub_time	= SystemTime.getCurrentTime();					
				}
				
				@Override
				public void
				torrentRemoved(
					TrackerTorrent	torrent )
				{
					rss_feed_pub_time	= SystemTime.getCurrentTime();					
				}
			});
	}
	
	@Override
	public boolean
	generate(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		boolean	tracker_web_enabled = plugin_config.getPluginBooleanParameter( CONFIG_TRACKER_PUBLISH_ENABLE, CONFIG_TRACKER_PUBLISH_ENABLE_DEFAULT );
		boolean tracker_more_columns_enabled = plugin_config.getPluginBooleanParameter( CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_MORE_COLUMNS, CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_MORE_COLUMNS_DEFAULT);
		boolean	rss_feed_enabled	= plugin_config.getPluginBooleanParameter( CONFIG_RSS_ENABLE, CONFIG_RSS_ENABLE_DEFAULT );
		boolean absolute_urls_enabled = plugin_config.getPluginBooleanParameter(CONFIG_TRACKER_PUBLISH_ENABLE_ABSOLUTE_URLS);
		
		String trackerURL = "";
		if(absolute_urls_enabled){
			String requestURL = request.getAbsoluteURL().toString();
			int trackerURL_end = requestURL.lastIndexOf("/")+1;
			trackerURL = requestURL.substring(0,trackerURL_end);
		}
		String	url = request.getURL();
				
		OutputStream	os = response.getOutputStream();
		
		// System.out.println( "TrackerWebDefaultTrackerPlugin: " + url);
		
		try{
			if ( 	tracker_web_enabled &&
					url.startsWith( "/torrents/")){
				
				String	str = url.substring(10);
				
				int	pos = str.indexOf ( "?" );
				
				String	hash_str = str.substring(pos+1);
				
				byte[]	hash = formatters.decodeBytesFromString( hash_str );
						
				TrackerTorrent[]	torrents = tracker.getTorrents();
				
				populateEnabledCatList();
				populateExcludedCatList();
				
				for (int i=0;i<torrents.length;i++){
						
					TrackerTorrent	tracker_torrent = torrents[i];
						
					if ( isCategoryEnabled( tracker_torrent )){
						
						Torrent	torrent = tracker_torrent.getTorrent();
							
						if ( Arrays.equals( hash, torrent.getHash())){
								
							response.writeTorrent( tracker_torrent );
								
							return( true );
						}
					}
				}
				
				System.out.println( "Torrent not found at '" + url + "'" );
										
				response.setReplyStatus( 404 );
				
				return( true );
			
			}else if ( 	tracker_web_enabled &&
						url.equalsIgnoreCase("/favicon.ico" )){
								
				response.setContentType( "image/x-icon" );
				
				response.setHeader( "Last Modified",
									"Fri,05 Sep 2003 01:01:01 GMT" );
				
				response.setHeader( "Expires",
									"Sun, 17 Jan 2038 01:01:01 GMT" );
				
				InputStream is = TrackerWebDefaultTrackerPlugin.class.getClassLoader().getResourceAsStream("org/gudy/azureus2/ui/tracker/templates/favicon.ico");
				
				if ( is == null ){
										
					response.setReplyStatus( 404 );
					
				}else{
					
					byte[] data = new byte[4096];
										
					while(true){
						
						int len = is.read(data, 0, 4096 );
						
						if ( len <= 0 ){
							
							break;
						}
						
						os.write( data, 0, len );
					}	
				}	
				
				return( true );
				
			}else if ( 	tracker_web_enabled &&
						url.equals( "/upload.cgi") &&
						plugin_config.getPluginBooleanParameter( CONFIG_TRACKER_UPLOAD_DATA_ENABLE, CONFIG_TRACKER_UPLOAD_DATA_ENABLE_DEFAULT )){

					// Content-Type: multipart/form-data; boundary=---------------------------7d437a28e70644

				PrintWriter	pw = new PrintWriter( new OutputStreamWriter( os, Constants.DEFAULT_ENCODING ));
				
				pw.println( "<html>" );
				pw.println( "<head><link href=\""+trackerURL+"styles/newtheme.css\" rel=\"stylesheet\" type=\"text/css\"></head>" );
				pw.println( "<body><pre>");
				
				pw.println( "Upload processing starts" );
				
				try{
					if ( !new File(upload_dir.getValue()).exists()){
						
						throw( new Exception( "Upload directory not configured" ));
					}
					
					String	header = request.getHeader();
					
					int	pos = header.toLowerCase().indexOf( "content-type:");
					
					if( pos == -1 ){
						
						throw( new Exception( "content-type missing" ));
					}
					
					int	p2 = header.indexOf( "\r", pos );
					
					String	content_type = header.substring(pos,p2);
					
					
					int bp = content_type.toLowerCase().indexOf( "boundary=" );
					
					if ( bp == -1 ){
						
						throw( new Exception( "boundary missing" ));
					}
					
					String	boundary = content_type.substring(bp+9).trim();
					
					int	ep = boundary.indexOf( ';' );
					
					if ( ep != -1 ){
						
						boundary = boundary.substring(0,ep).trim();
					}
					
					TrackerFormDecoder.formField[]	fields = 
						new TrackerFormDecoder().decode( boundary, request.getInputStream());
					
					String	torrent_category 		= "";	
					String	torrent_name			= null;
					String	torrent_comment			= null;
					String	torrent_hash			= null;
					boolean torrent_include_hashes	= false;
					boolean	force_start				= false;
					String  torrent_protocol		= "http";
					boolean	passive					= false;
					
					InputStream	torrent_data	= null;
					
					try{
						for (int i=0;i<fields.length;i++){
							
							TrackerFormDecoder.formField	field = fields[i];
							
							String	name = field.getName();
							
							if ( name.equalsIgnoreCase( "torrent_category" )){
								
								torrent_category	= field.getString();
															
							}else if ( name.equalsIgnoreCase( "torrent_comment" )){
								
								torrent_comment	= field.getString();
								
							}else if ( name.equalsIgnoreCase( "torrent_include_hashes" )){
								
								torrent_include_hashes	= true;
								
							}else if ( name.equalsIgnoreCase( "torrent_announce_protocol" )){
								
								torrent_protocol	= field.getString().trim().toLowerCase();
								
							}else if ( name.equalsIgnoreCase( "torrent_force_start" )){
								
								force_start	= true;
								
							}else if ( name.equalsIgnoreCase( "torrent_passive" )){
								
								passive	= true;
								
							}else if ( name.equalsIgnoreCase( "torrent_hash" )){
								
								torrent_hash	= field.getString();
								
							}else if ( name.equals( "upfile" )){
								
								torrent_data	= field.getInputStream();
								
								torrent_name 	= field.getAttribute( "filename");
								
								if ( torrent_name == null ){
									
									throw( new Exception( "'filename' attribute missing from upload" ));
								}
								
								int	ep1	= torrent_name.lastIndexOf("\\");
								int ep2	= torrent_name.lastIndexOf("/");
																				
								torrent_name = torrent_name.substring( Math.max( ep1, ep2 )+1 );
							}
						}
					
						if ( upload_passive_only.getValue() && !passive ){
							
							throw( new Exception( "Only passive uploads supported" ));
						}
						
						if ( torrent_data == null ){
							
							throw( new Exception( "Failed to read upload data" ));
						}
						
						upload( pw, 
								torrent_data, 
								torrent_name, 
								torrent_category, 
								torrent_comment, 
								torrent_hash, 
								torrent_include_hashes,
								force_start,
								torrent_protocol,
								passive );
						
						pw.println( "Upload successful" );
						
					}finally{
												
						for (int i=0;i<fields.length;i++){
							
							TrackerFormDecoder.formField	field = fields[i];
							
							field.destroy();
						}
					}
				}catch( Throwable e ){
					
					pw.println( "Upload failed" );
					
					e.printStackTrace( pw );
					
				}finally{
					
					pw.println( "</pre></body></html>");

					pw.flush();
				}
				
				return( true );
				
			}else if ( 	url.startsWith( "/rss_feed.xml") &&
						rss_feed_enabled ){
				
				if ( !initial_sharing_complete ){
				
					throw( new NoStackException( "RSS Feed initialising, please wait..." ));
				}
				
				Map	args = getArgs( url );
				
				String	val = (String)args.get( "magnet");
				
				boolean	use_magnets = val != null && val.equalsIgnoreCase("true");
				
				if ( use_magnets ){
					
					if ( magnet_plugin == null ){
						
						magnet_plugin = (MagnetPlugin)plugin_interface.getPluginManager().getPluginInterfaceByClass( MagnetPlugin.class ).getPlugin();

						if ( magnet_plugin == null ){
							
							use_magnets = false;
						}
					}
				}
				
				val = (String)args.get( "stats" );
				
				boolean	include_stats = val == null || val.equalsIgnoreCase("true");

				long	feed_date;
				
					// if we include the stats then the details can (and prolly do) vary
					// for each XML get - just use the current time as the feed date
				
				if ( include_stats ){
				
					feed_date = SystemTime.getCurrentTime();
					
				}else{
					
					feed_date = rss_feed_pub_time;
				}
				
				String	cats = (String)args.get( "categories" );
				
				if ( cats != null ){
					
					cats += ";";
				}
				
				List tracker_torrents;
				TrackerTorrent[] original_tracker_torrents = tracker.getTorrents();
				
				if(plugin_config.getPluginBooleanParameter(CONFIG_RSS_CATEGORY_ENABLE)){

					if(plugin_config.getPluginBooleanParameter(CONFIG_RSS_CATEGORY_TRACKER_ENABLE) && plugin_config.getPluginBooleanParameter(CONFIG_TRACKER_PUBLISH_ENABLE)){
						tracker_torrents = getSortedFilteredTrackerTorrents( original_tracker_torrents);
					} else {
						tracker_torrents = getSortedFilteredRSSTrackerTorrents(original_tracker_torrents);
					}
					
				} else {
				
					tracker_torrents = getSortedTrackerTorrents(original_tracker_torrents);
				
				}
				
				PrintWriter	pw = new PrintWriter( new OutputStreamWriter( os, Constants.DEFAULT_ENCODING ));
	
				response.setContentType("text/xml;charset=utf-8");
	
				URL[]	defined_urls = request.getContext().getURLs();
				
				if ( defined_urls.length == 0 ){
					
					throw( new NoStackException( "Tracker IP address not defined or tracker is not enabled" ));
				}
				
				URL	tracker_url	= defined_urls[0];

				String	http_host_port = (String)request.getHeaders().get( "host" );
				
				if ( http_host_port == null ){
					
					http_host_port = tracker_url.getHost();
					
				}else{
					
					http_host_port = http_host_port.toLowerCase( Locale.US );
				}
								
				String	tracker_url_string;
				
				int	pos = http_host_port.lastIndexOf(':');
				
				String	http_host;
				String	http_port;
				
				if ( pos == -1 ){
					http_host 	= http_host_port;
					http_port	= "";
				}else{
					http_host 	= http_host_port.substring(0,pos);
					http_port	= http_host_port.substring(pos);
				}
				if ( http_host.endsWith( ".i2p") || http_host.endsWith( ".onion" )){
				
					tracker_url_string = tracker_url.getProtocol() + "://" + http_host + http_port + "/";
					
				}else{
					
						// unfortunately, historically the default tracker url includes an /announce, which it
						// obviously shouldn't. However, we can't change this now as existing plugins might
						// (well, do) rely on it. Never mind. remove path if found

					tracker_url_string = tracker_url.toString();
				
					String	path = tracker_url.getPath();
				
					if ( path.length() > 1 ){
					
						tracker_url_string = tracker_url_string.substring(0,tracker_url_string.length()-(path.length()-1));
					}
				}
				
				pw.println( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" ); 
				
				String	tracker_title = plugin_config.getPluginStringParameter( CONFIG_TRACKER_TITLE );
				
				pw.println( "<rss version=\"2.0\" xmlns:azureus=\"http://azureus.sourceforge.net/files/rss/\">");
				pw.println(     "<channel>");
				pw.println(         "<title>" + tracker_title + " on " + http_host_port + "</title>"); 
				pw.println(	 		"<description>RSS Feed for " + tracker_title+ "</description>");
													
				pw.println(			"<link>" + tracker_url_string + "</link>" );
	
				
				pw.println(			"<pubDate>" + TimeFormatter.getHTTPDate( feed_date ) + "</pubDate>" );
				pw.println(			"<ttl>30</ttl>" );
				
				pw.println(			"<language>en-us</language>" );
				pw.println(			"<generator>" + plugin_interface.getAzureusName() + " " +
													plugin_interface.getApplicationVersion() + ", Tracker plugin version " +
													plugin_interface.getPluginVersion() + "</generator>" );
				
				boolean isPublishedRSSable = plugin_config.getPluginBooleanParameter(CONFIG_RSS_PUBLISH_PUBLISHED);
								
				for (int i=0;i<tracker_torrents.size();i++){
				
					TrackerTorrent	tracker_torrent = (TrackerTorrent)tracker_torrents.get(i);
				
					if (! (tracker_torrent.getStatus() == TrackerTorrent.TS_STARTED || (tracker_torrent.getStatus() == TrackerTorrent.TS_PUBLISHED && isPublishedRSSable)) ){
						
						continue;
					}
	
					Torrent	torrent = tracker_torrent.getTorrent();
					
					String	category = getCategoryName( tracker_torrent );
					
					if ( cats != null && cats.indexOf( category+";") == -1 ){
						
						continue;
					}
					
					pw.println( "<item>" );
					
					pw.println( 	"<title>" + escapeXML( torrent.getName()) + "</title>" );
					pw.println( 	"<description>" + escapeXML( torrent.getComment()) + "</description>" );
					
					String	hash_str = formatters.encodeBytesToString( torrent.getHash());
											
					String	link;
					
					if ( use_magnets ){
											
						link = magnet_plugin.getMagnetURL( torrent.getHash()).toString();
						
					}else{
						
						link = escapeXML( 	tracker_url_string + "torrents/" + 
									URLEncoder.encode( torrent.getName(), Constants.DEFAULT_ENCODING ).replaceAll("\\+", "%20") + ".torrent?" + hash_str );
					}
					
					pw.println( 	"<link>" +link + "</link>" );
					pw.println( 	"<guid>" +link + "</guid>" );
					
					if ( !category.equals( CATEGORY_UNKNOWN )){
					
						pw.println( 	"<category>" +category + "</category>" );
					}
					
						// torrent may be external 
					
					Download	download = plugin_interface.getDownloadManager().getDownload(torrent);
					
					if ( download != null ){
						
						File f = new File(plugin_interface.getDownloadManager().getDownload(torrent).getTorrentFileName());

						pw.println( 	"<pubDate>" + TimeFormatter.getHTTPDate(f.lastModified()) + "</pubDate>" );
					}
					
					pw.println(     "<enclosure url=\"" + link +"\" length=\"" + torrent.getSize() + "\" type=\"application/x-bittorrent\"/>" ); 

					pw.println(     "<azureus:torrent_sha1>" + hash_str + "</azureus:torrent_sha1>" );
					pw.println(     "<azureus:torrent_size>" + torrent.getSize() + "</azureus:torrent_size>" );
					
					if ( include_stats ){
						
						pw.println(     "<azureus:torrent_seeders>" + tracker_torrent.getSeedCount() + "</azureus:torrent_seeders>" );
						pw.println(     "<azureus:torrent_leechers>" + tracker_torrent.getLeecherCount() + "</azureus:torrent_leechers>" );
						pw.println(     "<azureus:torrent_completed>" + tracker_torrent.getCompletedCount() + "</azureus:torrent_completed>" );
					}
					
					pw.println(     "<azureus:torrent_hosted>" + ( tracker_torrent.getStatus() == TrackerTorrent.TS_PUBLISHED?"false":"true" ) + "</azureus:torrent_hosted>" );
					
					
					pw.println( "</item>" );
				}
				
				pw.println(     "</channel>");
				pw.println( "</rss>" );
				pw.flush();
				
				return( true );
				
			}else if ( tracker_web_enabled ){
				
				if ( url.equals("/")){
					
					url = tracker_more_columns_enabled?"/index2.tmpl":"/index.tmpl";

				}
				
				Hashtable	params = null;
				
				int	p_pos = url.indexOf( '?' );
				
				if ( p_pos != -1 ){
					
					params = decodeParams( url.substring( p_pos+1 ));
					
					url = url.substring(0,p_pos);
				}	
				
				InputStream is = TrackerWebDefaultTrackerPlugin.class.getClassLoader().getResourceAsStream("org/gudy/azureus2/ui/tracker/templates" + url );
				
				if ( is == null ){
					
					return( false );
				}
				
				try{
					int	pos = url.lastIndexOf( "." );
					
					if ( pos == -1 ){
						
						return( false );
					}
					String	file_type = url.substring(pos+1);
					
					if ( file_type.equals("php") || file_type.equals("tmpl")){
						
						Hashtable	args = new Hashtable();
						
						args.put( "filehandle", new InputStreamReader( is, Constants.DEFAULT_ENCODING ));

						return( handleTemplate( trackerURL, url, params, args, os ));
							
					}else{ 
													
						response.useStream( file_type, is );
						
						return( true );
					}	
				}finally{
													
					is.close();
				}			
			}else{
				
				return( false );
			}
	
		}catch( Throwable e ){
			
			if ( e instanceof NoStackException ){
			
				os.write( e.getMessage().getBytes());
				
			}else{
				
				e.printStackTrace();

				os.write( e.toString().getBytes());
			}
			
			return( true );
		}
	}
	
	protected void
	upload(
		final PrintWriter	pw,
		InputStream			is,
		String				torrent_name,
		String				torrent_category,
		String				torrent_comment,
		String				torrent_hash,
		boolean				torrent_include_hashes,
		boolean				force_start,
		String				torrent_protocol,
		boolean				passive ) 		
	
		throws Exception
	{
		// System.out.println( "Update:" + torrent_name + "/" + torrent_category + "/" + torrent_comment );

		File		torrent_file;
		File		data_directory	= new File(upload_dir.getValue());
	
		Torrent		torrent;
				
		URL[]	defined_urls = plugin_interface.getTracker().getURLs();
		
		if ( defined_urls.length == 0 ){
			
			throw( new Exception( "Tracker IP address not defined or tracker is not enabled" ));
		}
		
		URL	announce_url = null;
		
		for (int i=0;i<defined_urls.length;i++){
			
			URL	url	= defined_urls[i];
				
			if ( url.getProtocol().equalsIgnoreCase( torrent_protocol )){
				
				announce_url = url;
				
				break;
			}
		}
		
		if ( announce_url == null ){
			
			throw( new Exception("Tracker protocol '" + torrent_protocol + "' not enabled" ));
		}
			
		pw.println( "Using announce url '" + announce_url.toString() +"'" );
		
		if ( torrent_name.toLowerCase().endsWith( ".torrent" )){
		
			torrent_file 	= new File(upload_dir.getValue(), torrent_name );
		
			pw.println( "Saving torrent as '" + torrent_file.toString() + "'" );

			byte[]	buffer = new byte[65536];
			
			FileOutputStream	fos = new FileOutputStream( torrent_file );
			
			while(true){
				
				int	len = is.read( buffer );
				
				if ( len <= 0 ){
					
					break;
				}
				
				fos.write( buffer, 0, len );
			}
			
			fos.close();		
			
			torrent = plugin_interface.getTorrentManager().createFromBEncodedFile( torrent_file );

			torrent.setAnnounceURL( announce_url );
			
		}else{
					
			File	target_data		= new File( data_directory, torrent_name );

			torrent_file 	= new File( data_directory, torrent_name + ".torrent" );
			
			byte[]	buffer = new byte[65536];
			
			FileOutputStream	fos = new FileOutputStream( target_data );
			
			while(true){
				
				int	len = is.read( buffer );
				
				if ( len <= 0 ){
					
					break;
				}
				
				fos.write( buffer, 0, len );
			}
			
			fos.close();
			
			TorrentManager	tm = plugin_interface.getTorrentManager();
			
			TorrentManagerListener	tml = 
				new TorrentManagerListener()
				{
					@Override
					public void
					event(
						TorrentManagerEvent	ev )
					{
						pw.println( ev.getData());
						
						pw.flush();
					}
				};
							
			try{
				
				pw.println( "Creating torrent from '" + target_data.toString() + "'" );
				pw.println( "Include other hashes = " + torrent_include_hashes );
				
				tm.addListener( tml );
				
				torrent = tm.createFromDataFile( 
							target_data,
							announce_url,
							torrent_include_hashes );

				byte[]	hash = torrent.getHash();
				
				String	hash_str = plugin_interface.getUtilities().getFormatters().formatByteArray( hash, true );
				
				pw.println( "Generated torrent hash = '" + hash_str + "'" );
				
				if( torrent_hash.length() > 0 ){
					
					pw.println( "Supplied hash = '" + torrent_hash + "'" );
					
					if ( !hash_str.equals( torrent_hash.trim())){
						
						// t.writeToFile(new File("D:\\temp\\failed.torrent"));
						
						throw(new Exception("Uploaded data hash mismatch"));
					}
				}
			}finally{
				
				tm.removeListener( tml );
			}
			
			torrent.setComplete( data_directory );
		}

		torrent.setComment( torrent_comment );
			
		if ( passive && torrent_category.length() > 0 ){
			
			torrent.setPluginStringProperty( "category", torrent_category );
		}
		
		torrent.writeToFile( torrent_file );
					
		if ( passive ){
			
			plugin_interface.getTracker().host( torrent, true, true );

			pw.println( "Hosted it (passive)" );

		}else{
			
			Download dl = download_manager.addDownload( torrent, torrent_file, data_directory );
		
			pw.println( "Added download" );
	
			if ( torrent_category.length() > 0 ){
					
				dl.setAttribute(torrent_categories, torrent_category );
	
				pw.println( "Set category" );
			}
				
			if ( force_start ){
	
				dl.setForceStart( true );
				
				pw.println( "Force started it" );
			}
	
			plugin_interface.getTracker().host( dl.getTorrent(), true );
			
			pw.println( "Hosted it (active)" );
		}
	}
	
	protected String[]
	getAvailableCSS()
	{
		List	css = new ArrayList();
		
		File styles_dir = new File( getFileRoot(), "styles" );
		
		if ( styles_dir.exists()){
			
			File[]	files = styles_dir.listFiles(
								new FilenameFilter()
								{
									@Override
									public boolean
									accept(File dir, String name)
									{
										return( name.toLowerCase().endsWith( ".css" ));
									}
								});
			
			if ( files != null ){
				
				for ( int i=0;i<files.length;i++){
					
					css.add( files[i].getName());
				}
			}
		}
		
		for (int i=0;i<CONFIG_CSS_BUILTIN.length;i++){
			
			css.add( CONFIG_CSS_BUILTIN[i] );
		}
		
		String[]	res = new String[css.size()];
		
		css.toArray(res);
		
		Arrays.sort( res );

		return( res );
	}
	
	protected Map
	getArgs(
		String	url )
	{
		Map	res = new HashMap();
		
		int	pos = url.indexOf ( "?" );

		if ( pos != -1 ){
			
			pos++;
			
			while( true ){
				
				int	p1 = url.indexOf( "&", pos );
				
				String	bit;
				
				if ( p1 == -1 ){
					
					bit = url.substring(pos);
					
				}else{
					
					bit = url.substring(pos,p1);
					
					pos	= p1+1;
				}
				
				int	p2 = bit.indexOf( "=" );
				
				if ( p2 != -1 ){
					
					res.put( bit.substring(0,p2).toLowerCase(), bit.substring(p2+1));

				}
				
				if ( p1 == -1 ){
					
					break;
				}
			}
		}
		
		return( res );
	}
	
	protected static class
	NoStackException
		extends Exception
	{
		protected
		NoStackException(
			String	str )
		{
			super( str );
		}
	}
}
