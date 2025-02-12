/*
 * File    : TrackerWebUtil.java
 * Created : 10-Dec-2003
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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.StringTokenizer;
import java.util.Vector;

import com.biglybt.core.util.AENetworkClassifier;
import com.biglybt.core.util.Constants;
import com.biglybt.core.util.DisplayFormatters;
import com.biglybt.pif.Plugin;
import com.biglybt.pif.PluginConfig;
import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.download.Download;
import com.biglybt.pif.download.DownloadManager;
import com.biglybt.pif.download.DownloadManagerStats;
import com.biglybt.pif.torrent.Torrent;
import com.biglybt.pif.torrent.TorrentAttribute;
import com.biglybt.pif.torrent.TorrentException;
import com.biglybt.pif.torrent.TorrentFile;
import com.biglybt.pif.tracker.Tracker;
import com.biglybt.pif.tracker.TrackerPeer;
import com.biglybt.pif.tracker.TrackerTorrent;
import com.biglybt.pif.tracker.web.TrackerWebPageGenerator;
import com.biglybt.pif.utils.Formatters;
import com.biglybt.pif.utils.Utilities;

import HTML.Template;

public abstract class 
TrackerWeb
	implements Plugin, TrackerWebPageGenerator
{
	public static final String	CONFIG_MIGRATED							= "Tracker Config Migrated";
	
	public static final String	CONFIG_TRACKER_PUBLISH_ENABLE			= "Tracker Publish Enable";
	public static final boolean	CONFIG_TRACKER_PUBLISH_ENABLE_DEFAULT	= true;
	
	public static final String	CONFIG_TRACKER_PUBLISH_ENABLE_ABSOLUTE_URLS			= "Tracker Publish Enable Absolute URLs";
	public static final boolean	CONFIG_TRACKER_PUBLISH_ENABLE_ABSOLUTE_URLS_DEFAULT	= false;
	
	public static final String 	CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_MORE_COLUMNS			="Tracker Publish Enable More Columns";
	public static final boolean CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_MORE_COLUMNS_DEFAULT	= false;
	
	public static final String 	CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_LESS_COLUMNS_SWITCH			="Tracker Publish Enable Switch To Less Columns";
	public static final boolean CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_LESS_COLUMNS_SWITCH_DEFAULT	= true;
	
	public static final String 	CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_MORE_COLUMNS_SWITCH			="Tracker Publish Enable Switch To More Columns";
	public static final boolean CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_MORE_COLUMNS_SWITCH_DEFAULT	= false;
	
	public static final String 	CONFIG_TRACKER_PUBLISH_ENABLE_SORTING			="Tracker Publish Enable Sorting";
	public static final boolean CONFIG_TRACKER_PUBLISH_ENABLE_SORTING_DEFAULT	= true;
	
	public static final String 	CONFIG_TRACKER_PUBLISH_ENABLE_SEARCH			="Tracker Publish Enable Search";
	public static final boolean CONFIG_TRACKER_PUBLISH_ENABLE_SEARCH_DEFAULT	= true;
	
	public static final String	CONFIG_TRACKER_PUBLISH_ENABLE_DETAILS			= "Tracker Publish Enable Details";
	public static final boolean	CONFIG_TRACKER_PUBLISH_ENABLE_DETAILS_DEFAULT	= true;
	
	public static final String	CONFIG_TRACKER_PUBLISH_ENABLE_PEER_DETAILS			= "Tracker Publish Enable Peer Details";
	public static final boolean	CONFIG_TRACKER_PUBLISH_ENABLE_PEER_DETAILS_DEFAULT	= true;
	
	public static final String	CONFIG_TRACKER_OVERRIDE_AUTH_ENABLE					= "Tracker Auth Override Enable";
	public static final boolean	CONFIG_TRACKER_OVERRIDE_AUTH_ENABLE_DEFAULT			= false;
	
	public static final String	CONFIG_TRACKER_USERNAME					= "Tracker Username";
	public static final String	CONFIG_TRACKER_PASSWORD					= "Tracker Password";
	
	public static final String	CONFIG_TRACKER_SKIP			= "Tracker Skip";
	public static final int		CONFIG_TRACKER_SKIP_DEFAULT	= 0;
	
	public static final String		CONFIG_CSS				= "Tracker CSS";
	public static final String[]	CONFIG_CSS_BUILTIN		= { "newthemeInv2.css", "newtheme.css", "newthemeInv.css", "oldtheme.css" };
	public static final String		CONFIG_CSS_DEFAULT		= "newtheme.css";
	
	public static final String	CONFIG_TRACKER_PUBLISH_ENABLE_CATEGORIES			= "Tracker Categories Enable";	
	public static final boolean	CONFIG_TRACKER_PUBLISH_ENABLE_CATEGORIES_DEFAULT	= false;
	
	public static final String	CONFIG_CATEGORY_LIST			= "Tracker Category List";	
	public static final String	CONFIG_CATEGORY_LIST_DEFAULT	= "";
	
	public static final String	CONFIG_CATEGORY_EXCLUDE_LIST			= "Tracker Category Exclude List";	
	public static final String	CONFIG_CATEGORY_EXCLUDE_LIST_DEFAULT	= "";
	
	public static final String	CONFIG_RSS_ENABLE				= "Tracker RSS Enable";	
	public static final boolean	CONFIG_RSS_ENABLE_DEFAULT		= true;	
	
	public static final String	CONFIG_RSS_PUBLISH_PUBLISHED = "Tracker RSS Publish Published";
	public static final boolean	CONFIG_RSS_PUBLISH_PUBLISHED_DEFAULT		= true;
	
	public static final String	CONFIG_TRACKER_RSS_AUTH_ENABLE					= "Tracker RSS Auth Enable";
	public static final boolean	CONFIG_TRACKER_RSS_AUTH_ENABLE_DEFAULT			= false;
	
	public static final String	CONFIG_TRACKER_RSS_USERNAME					= "Tracker RSS Username";
	public static final String	CONFIG_TRACKER_RSS_PASSWORD					= "Tracker RSS Password";
	
	public static final String	CONFIG_RSS_CATEGORY_ENABLE				= "Tracker RSS Category Enable";	
	public static final boolean	CONFIG_RSS_CATEGORY_ENABLE_DEFAULT		= true;	
	
	public static final String	CONFIG_RSS_CATEGORY_TRACKER_ENABLE				= "Tracker RSS Category  As Tracker Enable";	
	public static final boolean	CONFIG_RSS_CATEGORY_TRACKER_ENABLE_DEFAULT		= true;	
	
	public static final String	CONFIG_RSS_CATEGORY_LIST			= "Tracker RSS Category List";	
	public static final String	CONFIG_RSS_CATEGORY_LIST_DEFAULT	= "";
	
	public static final String	CONFIG_RSS_CATEGORY_EXCLUDE_LIST			= "Tracker RSS Category Exclude List";	
	public static final String	CONFIG_RSS_CATEGORY_EXCLUDE_LIST_DEFAULT	= "";

	public static final String	CONFIG_TRACKER_UPLOAD_DATA_ENABLE			= "Tracker Upload Data Enable";	
	public static final boolean	CONFIG_TRACKER_UPLOAD_DATA_ENABLE_DEFAULT	= false;	
	
	public static final String	CONFIG_TRACKER_UPLOAD_DIR					= "Tracker Upload Dir";
	
	public static final String	CONFIG_TRACKER_UPLOAD_PASSIVE_DEFAULT			= "Tracker Upload Passive Default";	
	public static final boolean	CONFIG_TRACKER_UPLOAD_PASSIVE_DEFAULT_DEFAULT	= false;	

	public static final String	CONFIG_TRACKER_UPLOAD_PASSIVE_ONLY			= "Tracker Upload Passive Only";	
	public static final boolean	CONFIG_TRACKER_UPLOAD_PASSIVE_ONLY_DEFAULT	= false;	

	
	public static final String	CONFIG_TRACKER_PORT_OVERRIDE				= "Tracker Web Port Override";	
	public static final boolean	CONFIG_TRACKER_PORT_OVERRIDE_DEFAULT		= false;	

	public static final String	CONFIG_TRACKER_HTTP_PORT					= "Tracker Web HTTP Port";
	public static final int		CONFIG_TRACKER_HTTP_PORT_DEFAULT			= 0;
	
	public static final String	CONFIG_TRACKER_HTTPS_PORT					= "Tracker Web HTTPS Port";
	public static final int		CONFIG_TRACKER_HTTPS_PORT_DEFAULT			= 0;
	
	public static final String	CONFIG_TRACKER_TITLE					= "Tracker Title";
	public static final String	CONFIG_TRACKER_TITLE_DEFAULT			= "BiglyBT : Java BitTorrent Client Tracker";
	
	public static final String	CATEGORY_UNKNOWN	= "Uncategorized";
	public static final String	CATEGORY_EXTERNAL	= "External";
	public static final String	CATEGORY_PASSIVE	= "Passive";
	
	protected static final String	NL			= "\r\n";
	
	protected static final String[]		welcome_pages = {"index.html", "index.htm", "index.php", "index.tmpl" };
	protected static File[]				welcome_files;
	
	protected PluginInterface		plugin_interface;
	protected PluginConfig			plugin_config;
	protected DownloadManager		download_manager;
	
	protected Utilities				utilities;
	protected Formatters			formatters;
	protected Tracker				tracker;

	protected Comparator			alphanum_comparator;
	
	protected TorrentAttribute		torrent_categories;
	
	protected String				file_root;
	
	protected long					session_start_total_uploaded;
	protected long					session_start_total_downloaded;
	
	protected String				last_category_list = "";
	protected Map					enabled_category_map	= new HashMap();
	
	protected String				last_category_exclude_list = "";
	protected Map					disabled_category_map	= new HashMap();
	
	protected String				last_rss_category_list = "";
	protected Map					enabled_rss_category_map	= new HashMap();
	protected String				last_rss_category_exclude_list = "";
	protected Map					disabled_rss_category_map	= new HashMap();
	
	@Override
	public void
	initialize(
		PluginInterface _plugin_interface )
	{	
		plugin_interface	= _plugin_interface;
		
		plugin_config		= plugin_interface.getPluginconfig();
	
		download_manager	= plugin_interface.getDownloadManager();
		
		session_start_total_uploaded	= download_manager.getStats().getDataBytesSent();
		session_start_total_downloaded	= download_manager.getStats().getDataBytesReceived();
		
		utilities = plugin_interface.getUtilities();
		
		formatters = utilities.getFormatters();
		
		alphanum_comparator	= formatters.getAlphanumericComparator( true );
		
		tracker = plugin_interface.getTracker();
		
		TorrentAttribute[]	tas = plugin_interface.getTorrentManager().getDefinedAttributes();
		
		for (int i=0;i<tas.length;i++){
			
			if( tas[i].getName() == TorrentAttribute.TA_CATEGORY ){
				
				torrent_categories = tas[i];
				
				break;
			}
		}
		
		file_root = utilities.getUserDir() + File.separator + "web";

		welcome_files = new File[welcome_pages.length];
		
		for (int i=0;i<welcome_pages.length;i++){
			
			welcome_files[i] = new File( file_root + File.separator + welcome_pages[i] );
		}
		
		tracker.addPageGenerator( this );
	}
	
	protected File
	getFileRoot()
	{
		return( new File( file_root ));
	}
	
	protected Hashtable
	decodeParams(
		String	str )
	{
		Hashtable	params = new Hashtable();
		
		int	pos = 0;
		
		while(true){
		
			int	p1 = str.indexOf( '&', pos );
			String	bit;
			
			if ( p1 == -1 ){
				
				bit = str.substring(pos);
				
			}else{
				
				bit = str.substring(pos,p1);
				pos = p1+1;
			}
			
			int	p2 = bit.indexOf('=');
			
			if ( p2 == -1 ){
			
				params.put(bit,"true");
				
			}else{
				params.put(bit.substring(0,p2), bit.substring(p2+1));
				
			}
			
			if ( p1 == -1 ){
				
				break;
			}
		}
		
		return( params );
	}
	
	protected boolean
	handleTemplate(
		String _tracker_url,
		String			_page_url,
		Hashtable		params,
		Hashtable		args,
		OutputStream	os )
	
		throws IOException
	{	
		/*
		__FIRST__
		True for the first run of the loop, false otherwise 
		__LAST__ 
		True for the last run of the loop, false otherwise 
		__ODD__ 
		True for every other iteration of the loop - a loop starts at 1 
		__INNER__
		True if both __FIRST__ and __LAST__ are false 
		__COUNTER__
		Which iteration is currently on. Starts at 1.(new in 0.1.1) 
		*/
		
		TrackerTorrent[]	original_tracker_torrents = tracker.getTorrents();
		List				tracker_torrents;
		List				torrents_ok = new ArrayList();
		
		Map					catMap_orig = new HashMap();
		Map					catMap = new TreeMap();
		Map					catSelected	= new HashMap();
		
		for (int i=0; i<original_tracker_torrents.length; i++) {
			
			torrents_ok.add( new Integer(i) );
			
		}
		
		args.put( "loop_context_vars", "true" );
		args.put( "global_vars", "true" );
		
		Template t = new Template( args );

		int	specific_torrent 	= -1;
		
		int tracker_page 		= -1;
		
		int tracker_last_page 	= 1;
		
		String page_url = _page_url.substring(1);
		
		boolean useAbsoluteURLs = plugin_config.getPluginBooleanParameter(CONFIG_TRACKER_PUBLISH_ENABLE_ABSOLUTE_URLS, CONFIG_TRACKER_PUBLISH_ENABLE_ABSOLUTE_URLS_DEFAULT);
		
		String tracker_url = (useAbsoluteURLs)?_tracker_url:"";

		int tracker_skip = plugin_config.getPluginIntParameter(CONFIG_TRACKER_SKIP, CONFIG_TRACKER_SKIP_DEFAULT); // 0 = disabled, range: >0
		
		boolean allow_search 		= plugin_config.getPluginBooleanParameter(CONFIG_TRACKER_PUBLISH_ENABLE_SEARCH, CONFIG_TRACKER_PUBLISH_ENABLE_SEARCH_DEFAULT );
		
		String search_url_encoded ="";
		
		String search_string = "";
		
		boolean bSearch = false;
		
		boolean	categories = plugin_config.getPluginBooleanParameter( CONFIG_TRACKER_PUBLISH_ENABLE_CATEGORIES, CONFIG_TRACKER_PUBLISH_ENABLE_CATEGORIES_DEFAULT );
		
		boolean bCatChoice = false;
		
		String catchoices = "";
		
		t.setParam("all_selected", true);
		
		if ( torrent_categories != null ){
			
			Map[] resCat = createCatMap();
			
			catMap_orig = resCat[0];
			catSelected = resCat[1];
			
			t.setParam("total_cat", catMap_orig.size()>1?"1":"0");
			
			catMap = new TreeMap(		
					new Comparator()
					{
						@Override
						public int
						compare(
							Object	o1,
							Object	o2 )
						{
							String s1 = (String)o1;
							String s2 = (String)o2;
								
							return( alphanum_comparator.compare( s1.toLowerCase(), s2.toLowerCase()));
								
						}
					});
			
			catMap.putAll(catMap_orig);
			
		}
		
		// parse get parms.
		
		if ( params != null ){
			
			String	specific_torrents_hash 	= (String)params.get( "torrent_info_hash" );
			
			String	page = (String)params.get( "page" );
			String	skip = (String)params.get( "skip" );
			
			String limit_date = (String)params.get("date");
			
			if (allow_search) {
			
				search_url_encoded = (String)params.get("search");
				
					
				if ( search_url_encoded != null ){
					
					if (!search_url_encoded.equals("")) bSearch = true;
					
					search_string = URLDecoder.decode(search_url_encoded, Constants.DEFAULT_ENCODING);
					
					List[] res = getSortedFilteredTrackerTorrents(search_string, original_tracker_torrents);
					
					tracker_torrents = res[0];
					torrents_ok = res[1];
					
				} else if ( limit_date != null ) {
					
						tracker_torrents = getSortedFilteredTrackerTorrentsByDate(original_tracker_torrents);
						
				} else {
					
					tracker_torrents = getSortedFilteredTrackerTorrents(original_tracker_torrents);
				}
				
			} else {
				
				tracker_torrents = getSortedFilteredTrackerTorrents(original_tracker_torrents);
			}
			
			if ( specific_torrents_hash != null ){
				
				byte[] hash = formatters.decodeBytesFromString( specific_torrents_hash );
				
				for (int i=0;i<tracker_torrents.size();i++){
					
					TrackerTorrent	tt = (TrackerTorrent)tracker_torrents.get(i);
					
					if ( Arrays.equals( hash, tt.getTorrent().getHash())){
												
						specific_torrent = i;
						
						break;
					}
				}
				
				if ( specific_torrent == -1 ){
					
					return( torrentNotFound(os) );
				}
			}
			
			if ( page != null ){
				
				//make sure our values are in range
				if (Integer.parseInt( page ) > -1 )
				{
					tracker_page = Integer.parseInt( page );
						// 1 based -> 0 based
					tracker_page--;
				}

			}
			if ( skip != null ){
				
				if (Integer.parseInt( skip ) > 0)
				{
					tracker_skip = Integer.parseInt( skip );
				}
			}
			
			if (categories) {
			
				catchoices = (String)params.get("cat");
				
				if ( catchoices != null ) {
					
					bCatChoice = true;
					
					t.setParam("all_selected", false);
					
					Map[] resFC = getSortedFilteredTrackerTorrentsByCat( catchoices, original_tracker_torrents, torrents_ok, catMap );
					
					tracker_torrents = (List)resFC[0].get(new Integer(0));
					catSelected = (Map)resFC[1].get(new Integer(1));
	
				}
			}
			
		} else {
		
			tracker_torrents = getSortedFilteredTrackerTorrents(original_tracker_torrents);
		
		}
		
			// set up the parameters
		
		
		int	start;
		int	end;
		
			//use "pagenation" page links ?
		
		boolean pagenation = (tracker_skip > 0 && tracker_torrents.size() > tracker_skip);
		
			// if we're using pagenation we need a last page
		
		if ( pagenation ){
		
			int remainder = tracker_torrents.size() % tracker_skip;
			
			tracker_last_page = (tracker_torrents.size()-remainder)/tracker_skip;
			
			if( remainder > 0 ){
			
				tracker_last_page++;
			}
		}
		
		if ( specific_torrent != -1 ){
			
			start 	= specific_torrent;
			
			end		= specific_torrent+1;
			
		}else{
			
			int tracker_start 	= 0;
			
			int tracker_end		= tracker_torrents.size();
			
				//are we using pages ?
			
			if (pagenation){
			
					//make sure we start on the first page.
				
				if ( tracker_page == -1 ){
				
					tracker_page = 0;
				}
				
				if ( tracker_page < tracker_last_page ){
				
						//where to start..
					
					tracker_start = (tracker_page * tracker_skip);
					
				}else{
						//default to page 0
					
					tracker_page = 0;
					
					tracker_start = (tracker_page * tracker_skip);
				}
				
				if ((tracker_page+1) * tracker_skip <= tracker_end){
				
						//where to end..
					
					tracker_end = ((tracker_page+1) * tracker_skip);
				}
			}
			
			start 	= tracker_start;
			end		= tracker_end;
		}
		
		t.setParam("tracker_url", tracker_url);

		t.setParam( "page_url", page_url );
		
		t.setParam( "search_on", bSearch?"1":"0");
		
		t.setParam("search_url_encoded", search_url_encoded);
		
		t.setParam("search", escapeXSS(search_string));
		
		t.setParam("catchoice_on", bCatChoice?"1":"0");
		
		t.setParam("catchoice", catchoices);
		
			//Pagenation
		
		t.setParam("tracker_skip", tracker_skip);
		
		t.setParam("tracker_page", tracker_page+1);
		
		if ( pagenation ){
		
			if ( specific_torrent == -1 ){
			
				t.setParam( "show_pagenation", "1");

				t.setParam( "show_first_link", (tracker_page>1)?"1":"0");
				String url_search = (bSearch) ?  "&amp;search="+search_url_encoded:""; 
				String cat_choice = (bCatChoice) ? "&amp;cat="+catchoices:"";
				t.setParam( "first_link", page_url+"?skip="+tracker_skip+"&amp;page=1"+ url_search + cat_choice);
				t.setParam( "show_previous_link", (tracker_page>0)?"1":"0");
				t.setParam( "previous_link", page_url+"?skip="+tracker_skip+"&amp;page="+(tracker_page)+ url_search + cat_choice);
				t.setParam( "show_last_link", (tracker_page<tracker_last_page-2)?"1":"0");
				t.setParam( "last_link", page_url+"?skip="+tracker_skip+"&amp;page="+tracker_last_page+ url_search + cat_choice);
				t.setParam( "show_next_link", (tracker_page<tracker_last_page-1)?"1":"0");
				t.setParam( "next_link", page_url+"?skip="+tracker_skip+"&amp;page="+(tracker_page+2)+ url_search + cat_choice);

				t.setParam("current_page", tracker_page+1);
				String pagenation_text = "";
				
				for(int i=1; i<=tracker_last_page; i++)
				{
					if(i==tracker_page+1)
					{
						pagenation_text = pagenation_text+"<span class=\"pagenation\">"+i+"</span> ";
					}
					else
					{	
						pagenation_text = pagenation_text+"<a href=\""+tracker_url+page_url+"?skip="+tracker_skip+"&amp;page="+i+ url_search + cat_choice + "\" class=\"pagenation\">"+i+"</a> ";
					}
				}
				
				t.setParam( "pagenation", pagenation_text);
				
				t.setParam("link_switchpage", tracker_url+(page_url.equals("index.tmpl")?"index2.tmpl":"index.tmpl")+"?skip="+tracker_skip+"&amp;page="+ (tracker_page+1) + url_search + cat_choice );
				
			}
		}else{
			
			t.setParam( "show_pagenation", "0");
			
			t.setParam("link_switchpage", tracker_url+(page_url.equals("index.tmpl")?"index2.tmpl":"index.tmpl")+ (bSearch?"?search=" + search_url_encoded + (bCatChoice?"&amp;cat=" + catchoices:""):(bCatChoice?"?cat=" + catchoices : "") )  );

		}

		boolean	upload_data = plugin_config.getPluginBooleanParameter( CONFIG_TRACKER_UPLOAD_DATA_ENABLE, CONFIG_TRACKER_UPLOAD_DATA_ENABLE_DEFAULT );
		
		boolean	upload_passive_default = plugin_config.getPluginBooleanParameter( CONFIG_TRACKER_UPLOAD_PASSIVE_DEFAULT, CONFIG_TRACKER_UPLOAD_PASSIVE_DEFAULT_DEFAULT );
		
		boolean	upload_passive_only = plugin_config.getPluginBooleanParameter( CONFIG_TRACKER_UPLOAD_PASSIVE_ONLY, CONFIG_TRACKER_UPLOAD_PASSIVE_ONLY_DEFAULT );
		
		String	css	= plugin_config.getPluginStringParameter( CONFIG_CSS, CONFIG_CSS_DEFAULT );
		
		boolean allow_more_columns	= plugin_config.getPluginBooleanParameter(CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_MORE_COLUMNS, CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_MORE_COLUMNS_DEFAULT);

		boolean allow_more_columns_switch	= plugin_config.getPluginBooleanParameter(CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_MORE_COLUMNS_SWITCH, CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_MORE_COLUMNS_SWITCH_DEFAULT);
		
		boolean allow_less_columns_switch	= plugin_config.getPluginBooleanParameter(CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_LESS_COLUMNS_SWITCH, CONFIG_TRACKER_PUBLISH_ENABLE_TRACKER_LESS_COLUMNS_SWITCH_DEFAULT);
		
		boolean allow_more_col = allow_more_columns || (!allow_more_columns && allow_more_columns_switch);
		
		boolean allow_less_col = (allow_more_columns && allow_less_columns_switch) || !allow_more_columns;
		
		boolean allow_sorting 		= plugin_config.getPluginBooleanParameter(CONFIG_TRACKER_PUBLISH_ENABLE_SORTING, CONFIG_TRACKER_PUBLISH_ENABLE_SORTING_DEFAULT );
		
		boolean	allow_details 		= plugin_config.getPluginBooleanParameter(CONFIG_TRACKER_PUBLISH_ENABLE_DETAILS, CONFIG_TRACKER_PUBLISH_ENABLE_DETAILS_DEFAULT );
		
		boolean	allow_peer_details 	= plugin_config.getPluginBooleanParameter(CONFIG_TRACKER_PUBLISH_ENABLE_PEER_DETAILS, CONFIG_TRACKER_PUBLISH_ENABLE_PEER_DETAILS_DEFAULT );

		t.setParam( "tracker_title", plugin_config.getPluginStringParameter( CONFIG_TRACKER_TITLE ));
		
		t.setParam( "css_name", css );
		
		// to know when to display the "detailed page" link and when displaying the detailed page is allowed
		t.setParam( "tracker_more_allowed", allow_more_col?"1":"0");
		
		// to know when to display the "simple page" link and when displaying the simple page is allowed
		t.setParam( "tracker_less_allowed", allow_less_col?"1":"0");
		
		t.setParam( "sorting_enabled", allow_sorting?"1":"0");
		
		t.setParam( "search_enabled", allow_search?"1":"0");
		
		t.setParam( "torrent_details_allowed", allow_details?"1":"0");
		
		t.setParam( "torrent_peer_details_allowed", allow_peer_details?"1":"0");
		
		t.setParam( "azureus_version", plugin_interface.getApplicationVersion());
		
		t.setParam( "plugin_version", plugin_interface.getPluginVersion());
		
		t.setParam( "categories_enabled", categories?"1":"0");
		
		t.setParam( "rss_enabled", plugin_config.getPluginBooleanParameter( CONFIG_RSS_ENABLE, CONFIG_RSS_ENABLE_DEFAULT )?"1":"0");
		
		Vector	category_names = new Vector();
		
		Vector	category_names_upload = new Vector();
		
		if ( torrent_categories != null ){
			
			Hashtable ht = new Hashtable();
			
			String[]	x = torrent_categories.getDefinedValues();	
			
			Set entries = catMap.entrySet();
			
			Iterator iterator = entries.iterator();
			
			while(iterator.hasNext()){
				
				Map.Entry entry = (Map.Entry)iterator.next();
			  
				ht = new Hashtable();
				
				ht.put( "cat_name", entry.getKey() );
				
				ht.put("cat_number", entry.getValue() );
				
				ht.put("cat_selected", (catSelected.get(entry.getValue())=="1")?"1":"0");
				
				category_names.add(ht);
				
			}
			
			t.setParam( "categories", category_names );
			
			if(upload_data) {
			
			/*
			 * Full list of categories for upload
			 */
			
			ht = new Hashtable();
			
			ht.put( "cat_name", CATEGORY_UNKNOWN );
			
			category_names_upload.add(ht);
				
			for (int i=0; i< x.length; i++) {
				
				ht = new Hashtable();
				
				ht.put( "cat_name", x[i] );
				
				category_names_upload.add(ht);
				
			}
			
			t.setParam( "categories_upload", category_names_upload );
			
			}
		
		}
		
		t.setParam( "upload_data_enabled", upload_data?"1":"0");
		t.setParam( "upload_passive_default", upload_passive_only || upload_passive_default?"checked":"");
		
		Vector	category_info		= new Vector();
		String	current_category	= CATEGORY_UNKNOWN;
		
		Vector 	torrent_info 		= new Vector();
		
		long	total_torrents	= 0;
		long	total_seeds		= 0;
		long	total_peers		= 0;
		
		long	total_announce_average	= 0;
		long	total_scrape_average	= 0;
		long	total_bytes_in_average	= 0;
		long	total_bytes_out_average	= 0;
		
		long	total_swarm_speed_up	= 0;
		long	total_swarm_speed_down	= 0;
		long	total_swarm_left		= 0;
		
		for (int i=0;i<tracker_torrents.size();i++){
			
			TrackerTorrent	tracker_torrent = (TrackerTorrent)tracker_torrents.get(i);
			Torrent			torrent 		= tracker_torrent.getTorrent();
	
			total_torrents++;
			total_seeds 			+= tracker_torrent.getSeedCount();
			total_peers 			+= tracker_torrent.getLeecherCount();
			total_announce_average 	+= tracker_torrent.getAverageAnnounceCount();
			total_scrape_average	+= tracker_torrent.getAverageScrapeCount();
			total_bytes_in_average	+= tracker_torrent.getAverageBytesIn();
			total_bytes_out_average	+= tracker_torrent.getAverageBytesOut();
			
			total_swarm_speed_up	+= tracker_torrent.getAverageUploaded();
			total_swarm_speed_down	+= tracker_torrent.getAverageDownloaded();
			total_swarm_left		+= tracker_torrent.getTotalLeft();
			
			if ( i < start || i >= end ){
				
				continue;
			}
			
			if ( categories ){
								
				String	c1 = getCategoryName(tracker_torrent);
						
				if ( !c1.equalsIgnoreCase( current_category )){
					
					if ( torrent_info.size() > 0 ){
						
						Hashtable c_row = new Hashtable();

						c_row.put( "category_name", current_category );
						
						c_row.put( "torrent_info", torrent_info );
						
						category_info.add( c_row );
						
						torrent_info = new Vector();
					}
					
					current_category	= c1;
				}
				
			}
			
			Hashtable t_row = new Hashtable();
			
			torrent_info.add( t_row );
			
			
			//String	hash_str = URLEncoder.encode( new String( torrent.getHash(), Constants.BYTE_ENCODING ), Constants.BYTE_ENCODING );
			String	hash_str = formatters.encodeBytesToString( torrent.getHash());
			
			String	torrent_name = torrent.getName();
			
			long torrent_peers = tracker_torrent.getLeecherCount();
			
			long torrent_size = torrent.getSize();
			
			long date_added = tracker_torrent.getDateAdded();
			
			String date_added_text = DisplayFormatters.formatDateNum( date_added );
			
			int	status = tracker_torrent.getStatus();
			
			String	status_str;
			
			if ( status == TrackerTorrent.TS_STARTED ){

				status_str = "Running";
				
			}else if ( status == TrackerTorrent.TS_STOPPED ){
				
				status_str = "Stopped";
				
			}else if ( status == TrackerTorrent.TS_PUBLISHED ){
				
				status_str = "Published";
				
			}else{
				
				status_str = "Failed";
			}
						
			t_row.put( "torrent_name", escapeXML(torrent_name));
			
			if ( torrent.getSize() != 1 ){
				
				t_row.put( "torrent_download_url", tracker_url+"torrents/" + URLEncoder.encode( torrent_name, Constants.DEFAULT_ENCODING).replaceAll("\\+", "%20") + ".torrent?" + hash_str );

				t_row.put( "torrent_details_url", tracker_url+"details.tmpl?torrent_info_hash=" + hash_str );
				
				t_row.put( "torrent_details_params", "torrent_info_hash=" + hash_str );
			}else{
				
				t_row.put( "torrent_download_url", "#" );
				t_row.put( "torrent_details_url", "#" );
				t_row.put( "torrent_details_params", "");
			}
			
			t_row.put( "torrent_status", status_str );
			
			t_row.put( "torrent_size", (torrent_size<=0?"N/A":formatters.formatByteCountToKiBEtc( torrent_size )));
			
			t_row.put( "torrent_seeds", "" + tracker_torrent.getSeedCount());
			
			t_row.put( "torrent_peers", "" + torrent_peers);
			
			t_row.put( "torrent_bad_NAT", "" + tracker_torrent.getBadNATCount());

			t_row.put( "torrent_total_upload", formatters.formatByteCountToKiBEtc( tracker_torrent.getTotalUploaded())); 
			
			t_row.put( "torrent_total_download", formatters.formatByteCountToKiBEtc( tracker_torrent.getTotalDownloaded())); 
			
			t_row.put( "torrent_upload_speed", formatters.formatByteCountToKiBEtcPerSec( tracker_torrent.getAverageUploaded())); 
			
			t_row.put( "torrent_download_speed", formatters.formatByteCountToKiBEtcPerSec( tracker_torrent.getAverageDownloaded())); 
			
			t_row.put( "torrent_total_left", formatters.formatByteCountToKiBEtc( tracker_torrent.getTotalLeft()));
			
			int torrent_progress = (torrent_size > 0 && torrent_peers > 0) ? 
					(int) (100.0f * ( 1.0f  - (float) tracker_torrent.getTotalLeft() / (torrent_peers * torrent_size) ) )
					: -1;
			
			boolean torrent_has_progress = torrent_progress >= 0 && status_str == "Running";
			
			t_row.put( "torrent_average_progress", ( torrent_has_progress ? "" + torrent_progress : "0" ) );
			
			t_row.put( "torrent_has_progress", torrent_has_progress?"1":"0");
			
			t_row.put( "torrent_completed",  "" + tracker_torrent.getCompletedCount());
			
			String	tracker_activity1 = 
				tracker_torrent.getAverageAnnounceCount() + 
				", " + tracker_torrent.getAverageScrapeCount();
			
			t_row.put( "torrent_activity1",  "" + tracker_activity1 );
			
			String	tracker_activity2 = 
				formatters.formatByteCountToKiBEtcPerSec( tracker_torrent.getAverageBytesIn()) + 
				", " + formatters.formatByteCountToKiBEtcPerSec( tracker_torrent.getAverageBytesOut());
			
			t_row.put( "torrent_activity2",  "" + tracker_activity2 );
			
			t_row.put( "torrent_date_added", date_added_text);
			
			boolean bTorrentNoMagnet = torrent.isPrivate() || !torrent.isDecentralisedBackupEnabled();
			
			t_row.put("torrent_has_magnet", bTorrentNoMagnet?"0":"1");

			try{
				t_row.put("torrent_magnet", ""+torrent.getMagnetURI());
			}catch(TorrentException tex) {
				tex.printStackTrace();
			}
			
			if ( allow_details ){
				
				if ( specific_torrent != -1 ){
				
					t_row.put( "torrent_hash", formatters.encodeBytesToString(torrent.getHash()));
								
						// size is 0 for external torrents about which we know no more
					
					if ( torrent.getSize() > 0 ){
				
							// if we put out a normal space for optional bits then the table doesn't draw properly
						
						t_row.put( "torrent_comment", torrent.getComment().length()==0?"&nbsp;": escapeXML(torrent.getComment()));
						
						t_row.put( "torrent_created_by", torrent.getCreatedBy().length()==0?"&nbsp;":torrent.getCreatedBy());
						
						String	date = formatters.formatDate(torrent.getCreationDate() * 1000 );
						
						t_row.put( "torrent_created_on", date.length()==0?"&nbsp;":date );
						
						t_row.put( "torrent_piece_size", formatters.formatByteCountToKiBEtc(torrent.getPieceSize()));
						
						t_row.put( "torrent_piece_count", ""+torrent.getPieceCount());
						
						Vector	file_info = new Vector();
						
						TorrentFile[]	files = torrent.getFiles();
						
						for (int j=0;j<files.length;j++){
							
							Hashtable	f_row = new Hashtable();
							
							file_info.add( f_row );
							
							f_row.put( "file_name", escapeXML( files[j].getName()));
							
							f_row.put( "file_size", formatters.formatByteCountToKiBEtc(files[j].getSize()));
						}
						
						t_row.put( "file_info", file_info );
						t_row.put( "file_info_count", ""+files.length );
					}
					
					Vector	peer_info = new Vector();
					
						// peers details for published torrents are dummy and not useful
					
					if ( status != TrackerTorrent.TS_PUBLISHED && allow_peer_details ){
	
						TrackerPeer[]	peers = tracker_torrent.getPeers();

						// sort in descending completion order
						
						Arrays.sort(
								peers,
								new Comparator()
								{
									@Override
									public int
									compare(
										Object	o1,
										Object	o2 )
									{
										TrackerPeer	p1 = (TrackerPeer)o1;
										TrackerPeer	p2 = (TrackerPeer)o2;
										
										long	l = p1.getAmountLeft() - p2.getAmountLeft();
										
										if ( l < 0 ){
											return(-1);
										}else if ( l > 0 ){
											return(1);
										}else{
											return(0);
										}
									}
								});
						
						for (int j=0;j<peers.length;j++){
							
							Hashtable p_row = new Hashtable();
							
							peer_info.add( p_row );
							
							TrackerPeer	peer = peers[j];
							
							long	uploaded 	= peer.getUploaded();
							long	downloaded	= peer.getDownloaded();
							
							float	share_ratio;
							
							if ( downloaded == 0 ){
								
								share_ratio = uploaded==0?1:-1;
								
							}else{
								share_ratio	= (float)((uploaded*1000)/downloaded)/1000;
							}
							
							int	share_health = (int)share_ratio*5;
							
							if ( share_health > 5 ){
								
								share_health = 5;
							}
														
							p_row.put( "peer_is_seed", peer.isSeed()?"1":"0" );
							p_row.put( "peer_uploaded", formatters.formatByteCountToKiBEtc(uploaded));
							p_row.put( "peer_downloaded", formatters.formatByteCountToKiBEtc(downloaded));
							p_row.put( "peer_left", formatters.formatByteCountToKiBEtc(peer.getAmountLeft()));
							p_row.put( "peer_ip", hideLastIpBlock(peer.getIP()) );
							p_row.put( "peer_ip_full", peer.getIP());
							p_row.put( "peer_share_ratio", (share_ratio==-1?"&#8734;":""+share_ratio));
							p_row.put( "peer_share_health", ""+share_health );
						}
					}
					
					t_row.put( "peer_info", peer_info );
					t_row.put( "peer_info_count", ""+peer_info.size());
				}
			}
		} // end torrents loop
		
		if ( categories && specific_torrent == -1 ){
			
			if ( torrent_info.size() > 0 ){
				
				Hashtable c_row = new Hashtable();

				c_row.put( "category_name", current_category );
				c_row.put( "torrent_info", torrent_info );
				
				category_info.add( c_row );
			}
			
			t.setParam( "category_info", category_info );
			
			t.setParam("category_count", category_info.size());
			
		}else{
			
			t.setParam("torrent_info", torrent_info);
		}
		
		t.setParam( "torrent_info_count", ""+torrent_info.size());
		
		t.setParam( "total_torrents", ""+total_torrents );
		t.setParam( "total_seeds", ""+total_seeds );
		t.setParam( "total_peers", ""+total_peers );
		t.setParam( "total_announce_average", ""+total_announce_average );
		t.setParam( "total_scrape_average", ""+total_scrape_average );
		t.setParam( "total_bytes_in_average", formatters.formatByteCountToKiBEtcPerSec(total_bytes_in_average ));
		t.setParam( "total_bytes_out_average", formatters.formatByteCountToKiBEtcPerSec( total_bytes_out_average ));

		t.setParam( "total_swarm_speed_up", formatters.formatByteCountToKiBEtcPerSec( total_swarm_speed_up ));
		t.setParam( "total_swarm_speed_down", formatters.formatByteCountToKiBEtcPerSec( total_swarm_speed_down ));
		t.setParam( "total_swarm_left", formatters.formatByteCountToKiBEtc( total_swarm_left ));
		
		DownloadManagerStats	dms = download_manager.getStats();
		
		long	session_uploaded 	= dms.getDataBytesSent();
		long	session_downloaded 	= dms.getDataBytesReceived();
		
		t.setParam( "total_upload", 
				formatters.formatByteCountToKiBEtc( dms.getOverallDataBytesSent() ) + ", " +
				formatters.formatByteCountToKiBEtc( session_uploaded ) + ", " +
				formatters.formatByteCountToKiBEtcPerSec( dms.getDataSendRate()));
		
		t.setParam( "total_download", 
				formatters.formatByteCountToKiBEtc( dms.getOverallDataBytesReceived() ) + ", " +
				formatters.formatByteCountToKiBEtc( session_downloaded ) + ", " +
				formatters.formatByteCountToKiBEtcPerSec( dms.getDataReceiveRate()));

		t.setParam( "total_uptime", formatters.formatTimeFromSeconds( dms.getSessionUptimeSeconds()));
		
		t.setParam("more_than_one", (total_torrents>1)?"1":"0");
		
		if(page_url.indexOf("details") != -1) t.setParam("tracker_url", (allow_more_columns?"index2.tmpl":"index.tmpl"));

		String	data = t.output();
		
		PrintWriter	pw = new PrintWriter( new OutputStreamWriter( os, Constants.DEFAULT_ENCODING ), true);
		
		pw.write(data);
		
		pw.flush( );
		
		return( true );
	}

	/**
	 * Category Map of OKed Categories, all selected by default
	 * @return Map[] catMap_orig, catSelected
	 */
	private Map[] createCatMap() {
		
		Map[] result = new Map[2];
		result[0] = new HashMap();
		result[1] = new HashMap();
		
		
		populateEnabledCatList();
		
		populateExcludedCatList();
		
		String[]	x = torrent_categories.getDefinedValues();
		
		int i,j = 0;
		
		if( isCategoryOK(CATEGORY_UNKNOWN) ) {
		
			Integer integer = new Integer(j);
			
			j++;
			
			result[0].put( CATEGORY_UNKNOWN, integer);
		
		}
		
		for ( i = 0;i<x.length;i++){
			
			if( isCategoryOK(x[i]) ) {
				
				if (!result[0].containsKey((String)x[i])) {
					
					Integer integer = new Integer(j);
					
					j++;
					
					result[0].put( x[i], integer);
					
				}
			}
		}
		
		for (int k=0;k<result[0].size();k++){
			
			Integer integer = new Integer(k);
			
			result[1].put(integer, "1");

		}
		
		//result[0].putAll(catMap_orig);
		//result[1].putAll(catSelected);
		
		return result;
		
	}
	
	protected boolean
	torrentNotFound(
		OutputStream	os )
	
		throws IOException
	{
		os.write( "Torrent not found".getBytes());
		
		return( true );
	}
	
	private String hideLastIpBlock(String ip) {
	  if(ip == null)
	    return null;
	  StringTokenizer st = new StringTokenizer(ip,".");
	  if(st.countTokens() == 4){
		  
		  return st.nextToken() + "." + st.nextToken() + "." + st.nextToken() + ".*";
	  }
	  
	  	// try ipv6
	  
	  st = new StringTokenizer(ip,":");
	  if(st.countTokens() >= 3){

		  String	str = "";

		  for (int i=0;i<3;i++){		
			  
			  str += st.nextToken() + ":";
		  }
		  
		  return( str + "*" );
	  }
	  
	  if ( AENetworkClassifier.categoriseAddress( ip ) != AENetworkClassifier.AT_PUBLIC && ip.length() > 15 ) {
		  
		  String str = ip.substring(0,10) + "...";
		  
		  int pos = ip.lastIndexOf( '.' );
		  
		  if ( pos != -1 ) {
			  
			  str += ip.substring( pos+1 );
		  }
		  
		  return( str );
	  }
	  
	  return "*";
	}
	
	// Map the Homepage URL.
	protected String mapHomePage( String url_in )
	{
	  if ( url_in.equals("/")){
		for (int i=0;i<welcome_files.length;i++){
		  if ( welcome_files[i].exists()){
			url_in = "/" + welcome_pages[i];
			return (url_in);
		  }
		}
	  }
	  return (url_in);
	}
	
	protected String
	getCategoryName(
		TrackerTorrent	tr_torrent )
	{
		Download dl = download_manager.getDownload( tr_torrent.getTorrent());
		
		if ( dl == null ){
			
				// deal with passive torrents - category stored in torrent
			
			String	c = tr_torrent.getTorrent().getPluginStringProperty( "category" );
			
			if ( c != null && c.length() > 0 ){
				
				return( c );
			}
			
			return( tr_torrent.isPassive()?CATEGORY_PASSIVE:CATEGORY_EXTERNAL );
		}
		
		String	c = dl.getAttribute( torrent_categories);
		
		if ( c == null || c.equals( "Categories.uncategorized" )){
			
			c = CATEGORY_UNKNOWN;
		}

		return( c );
	}
	
	public boolean
	isCategoryEnabled(
		TrackerTorrent	tt )
	{
		
		return isCategoryOK(getCategoryName(tt));
	}
	
	public boolean
	isRSSCategoryEnabled(
		TrackerTorrent	tt )
	{
		
		return isRSSCategoryOK(getCategoryName(tt));
	}
	
	public boolean
	isCategoryOK(
		String _cat_name )
	{
		String cat_name = _cat_name.toLowerCase();
		
		if ( enabled_category_map.size() == 0 ){
			
				// none explicitly enabled -> only disable explicitly disabled ones 
			
			return( disabled_category_map.get(cat_name) == null );
		}
		
		return( 	enabled_category_map.get( cat_name ) != null &&
				 	disabled_category_map.get( cat_name ) == null );
	}
	
	public boolean
	isRSSCategoryOK(
		String _cat_name )
	{
		String cat_name = _cat_name.toLowerCase();
		
		if ( enabled_rss_category_map.size() == 0 ){
			
				// none explicitly enabled -> only disable explicitly disabled ones 
			
			return( disabled_rss_category_map.get(cat_name) == null );
		}
		
		return( 	enabled_rss_category_map.get( cat_name ) != null &&
				 	disabled_rss_category_map.get( cat_name ) == null );
	}

	protected void populateExcludedCatList() {
		
		String	category_exclude_list = plugin_config.getPluginStringParameter( CONFIG_CATEGORY_EXCLUDE_LIST, CONFIG_CATEGORY_EXCLUDE_LIST_DEFAULT ).trim();
		
		if ( !category_exclude_list.equals( last_category_exclude_list )){
		
			Map	new_map = new HashMap();
			
			if ( category_exclude_list.length() > 0 ){
				
				StringTokenizer tok = new StringTokenizer( category_exclude_list, ";" );
				
				while( tok.hasMoreTokens()){
					
					String	cat = tok.nextToken().trim().toLowerCase();
					
					if ( cat.length() > 0 ){
						
						new_map.put( cat, cat );
					}
				}		
			}
			
			disabled_category_map	= new_map;
			
			last_category_exclude_list	= category_exclude_list;
		}
	}

	protected void populateEnabledCatList() {
		
		String	category_list = plugin_config.getPluginStringParameter( CONFIG_CATEGORY_LIST, CONFIG_CATEGORY_LIST_DEFAULT ).trim();
		
		if ( !category_list.equals( last_category_list )){
		
			Map	new_map = new HashMap();
			
			if ( category_list.length() > 0 ){
				
				StringTokenizer tok = new StringTokenizer( category_list, ";" );
				
				while( tok.hasMoreTokens()){
					
					String	cat = tok.nextToken().trim().toLowerCase();
					
					if ( cat.length() > 0 ){
						
						new_map.put( cat, cat );
					}
				}		
			}
			
			enabled_category_map	= new_map;
			
			last_category_list	= category_list;
		}
	}
	
	protected void populateRSSExcludedCatList() {
		
		String	rss_category_exclude_list = plugin_config.getPluginStringParameter( CONFIG_RSS_CATEGORY_EXCLUDE_LIST, CONFIG_RSS_CATEGORY_EXCLUDE_LIST_DEFAULT ).trim();
		
		if ( !rss_category_exclude_list.equals( last_rss_category_exclude_list )){
		
			Map	new_map = new HashMap();
			
			if ( rss_category_exclude_list.length() > 0 ){
				
				StringTokenizer tok = new StringTokenizer( rss_category_exclude_list, ";" );
				
				while( tok.hasMoreTokens()){
					
					String	cat = tok.nextToken().trim().toLowerCase();
					
					if ( cat.length() > 0 ){
						
						new_map.put( cat, cat );
					}
				}		
			}
			
			disabled_rss_category_map	= new_map;
			
			last_rss_category_exclude_list	= rss_category_exclude_list;
		}
	}

	protected void populateRSSEnabledCatList() {
		
		String	rss_category_list = plugin_config.getPluginStringParameter( CONFIG_RSS_CATEGORY_LIST, CONFIG_RSS_CATEGORY_LIST_DEFAULT ).trim();
		
		if ( !rss_category_list.equals( last_rss_category_list )){
		
			Map	new_map = new HashMap();
			
			if ( rss_category_list.length() > 0 ){
				
				StringTokenizer tok = new StringTokenizer( rss_category_list, ";" );
				
				while( tok.hasMoreTokens()){
					
					String	cat = tok.nextToken().trim().toLowerCase();
					
					if ( cat.length() > 0 ){
						
						new_map.put( cat, cat );
					}
				}		
			}
			
			enabled_rss_category_map	= new_map;
			
			last_rss_category_list	= rss_category_list;
		}
	}
	
	protected List
	getSortedTrackerTorrents(TrackerTorrent[] originalTT)
	{
		TrackerTorrent[] original_tracker_torrents = originalTT;
		
			// sort the torrents
		
		boolean	categories = plugin_config.getPluginBooleanParameter( CONFIG_TRACKER_PUBLISH_ENABLE_CATEGORIES, CONFIG_TRACKER_PUBLISH_ENABLE_CATEGORIES_DEFAULT );
			
		sortTrackerTorrents(original_tracker_torrents, (categories)? true: false);

			// remove deselected torrents
		
		List	tracker_torrents = new ArrayList();

		
		for (int i=0;i<original_tracker_torrents.length;i++){
			
			TrackerTorrent tracker_torrent = original_tracker_torrents[i];
							
				tracker_torrents.add( tracker_torrent );
		}
		
		return( tracker_torrents );
	}
	
	protected List
	getSortedFilteredTrackerTorrents(TrackerTorrent[] originalTT)
	{
		TrackerTorrent[] original_tracker_torrents = originalTT;
		
			// sort the torrents
		
		boolean	categories = plugin_config.getPluginBooleanParameter( CONFIG_TRACKER_PUBLISH_ENABLE_CATEGORIES, CONFIG_TRACKER_PUBLISH_ENABLE_CATEGORIES_DEFAULT );
			
		sortTrackerTorrents(original_tracker_torrents, (categories)? true: false);

			// remove deselected torrents
		
		List	tracker_torrents = new ArrayList();
		
		populateEnabledCatList();
		populateExcludedCatList();
		
		for (int i=0;i<original_tracker_torrents.length;i++){
			
			TrackerTorrent tracker_torrent = original_tracker_torrents[i];
			
			if ( isCategoryEnabled( tracker_torrent )){
							
				tracker_torrents.add( tracker_torrent );
				
			}
		}
		
		return( tracker_torrents );
	}
	
	protected List
	getSortedFilteredRSSTrackerTorrents(TrackerTorrent[] originalTT)
	{
		TrackerTorrent[] original_tracker_torrents = originalTT;
			// sort the torrents
			
		sortTrackerTorrents(original_tracker_torrents, false);

			// remove deselected torrents
		
		List	tracker_torrents = new ArrayList();

		populateRSSEnabledCatList();
		populateRSSExcludedCatList();
		
		for (int i=0;i<original_tracker_torrents.length;i++){
			
			TrackerTorrent tracker_torrent = original_tracker_torrents[i];
			
			if ( isRSSCategoryEnabled( tracker_torrent )){
							
				tracker_torrents.add( tracker_torrent );
				
			}
		}
		
		return( tracker_torrents );
	}


	/**
	 * @param original_tracker_torrents
	 */
	private void sortTrackerTorrents(TrackerTorrent[] original_tracker_torrents, boolean categoriesEnabled) {
		final boolean catOn = categoriesEnabled;
		Arrays.sort(
			original_tracker_torrents,
			new Comparator()
			{
				Map<TrackerTorrent,String>	names = new IdentityHashMap<>();
				
				@Override
				public int
				compare(
					Object	o1,
					Object	o2 )
				{
					TrackerTorrent	t1 = (TrackerTorrent)o1;
					TrackerTorrent	t2 = (TrackerTorrent)o2;
					
					if (catOn) {
						String	c1 = getCategoryName((TrackerTorrent)o1);
						String	c2 = getCategoryName((TrackerTorrent)o2);
	
						int	res = alphanum_comparator.compare( c1, c2 );
						
						if ( res == 0 ){
							
							res = alphanum_comparator.compare( getName(t1), getName(t2));
						}
						
						return( res );
						
					} else {
						
						return( alphanum_comparator.compare( getName(t1), getName(t2)));						
					}
				}
				
				String
				getName(
					TrackerTorrent	t )
				{
					String name = names.get(t);
					
					if ( name == null ){
						
						name = t.getTorrent().getName();
						
						names.put(t, name);
					}
					
					return(  name );
				}
			});
	}

	protected List[]
	getSortedFilteredTrackerTorrents(String s, TrackerTorrent[] originalTT)
	{	
		TrackerTorrent[] original_tracker_torrents = originalTT;
		
		populateEnabledCatList();
		populateExcludedCatList();
		
		List[] result = new ArrayList[2];
		List torrents_ok = new ArrayList();
		
			// sort the torrents
		
		boolean	categories = plugin_config.getPluginBooleanParameter( CONFIG_TRACKER_PUBLISH_ENABLE_CATEGORIES, CONFIG_TRACKER_PUBLISH_ENABLE_CATEGORIES_DEFAULT );
		
		sortTrackerTorrents(original_tracker_torrents, (categories)? true: false);

			// remove deselected torrents
		
		List	tracker_torrents = new ArrayList();
		
		String[] search_items = s.split( " " );
		
		for (int i=0;i<original_tracker_torrents.length;i++){
			
				String t_name = original_tracker_torrents[i].getTorrent().getName().toLowerCase();
				
				int search_count = 0;
				
				for (int j = 0 ; j < search_items.length ; j++) {
					
					if (t_name.indexOf(search_items[j].toLowerCase()) != -1)  search_count += 1; 
					
				}
			
				if (   search_count == search_items.length   ) {
			
					TrackerTorrent tracker_torrent = original_tracker_torrents[i];
					
					if ( isCategoryEnabled( tracker_torrent )){
						
						tracker_torrents.add( tracker_torrent );
						
						torrents_ok.add(new Integer(i));
					}	
				}
				
		}
		result[0] = tracker_torrents;
		result[1] = torrents_ok;
		return( result );
	}
	
	protected List
	getSortedFilteredTrackerTorrentsByDate(TrackerTorrent[] originalTT)
	{	
		TrackerTorrent[] original_tracker_torrents = originalTT;
		
		populateEnabledCatList();
		populateExcludedCatList();
		
			// sort the torrents
		
		boolean	categories = plugin_config.getPluginBooleanParameter( CONFIG_TRACKER_PUBLISH_ENABLE_CATEGORIES, CONFIG_TRACKER_PUBLISH_ENABLE_CATEGORIES_DEFAULT );
		
		sortTrackerTorrents(original_tracker_torrents, (categories)? true: false);

			// remove deselected torrents
		
		List	tracker_torrents = new ArrayList();
		
		Calendar myCalendar = GregorianCalendar.getInstance();
		
		myCalendar.add(GregorianCalendar.DAY_OF_YEAR, -7);
		
		Date oneWeekAgo = new Date( myCalendar.getTimeInMillis() );
		
		for (int i=0;i<original_tracker_torrents.length;i++){
			
				Date t_date = new Date(original_tracker_torrents[i].getDateAdded());
			
				if (   t_date.after(oneWeekAgo)   ) {
			
					TrackerTorrent tracker_torrent = original_tracker_torrents[i];
					
					if ( isCategoryEnabled( tracker_torrent )){
						
						tracker_torrents.add( tracker_torrent );
					}	
				}
				
		}
		
		return( tracker_torrents );
	}
	
	protected Map[]
	getSortedFilteredTrackerTorrentsByCat( String cat, TrackerTorrent[] originalTT, List okT, Map cMap)
	{	
		TrackerTorrent[] original_tracker_torrents = originalTT;
		List torrents_ok = okT;
		Map catMap = cMap;
		Map catSelected = new HashMap();
		Map[] res = new Map[2];
		res[0] = new HashMap();
		res[1] = new HashMap();
		
		TrackerTorrent[] current_tracker_torrents = new TrackerTorrent[torrents_ok.size()];
		
		int j = 0;
		
		for (int i=0;i<original_tracker_torrents.length;i++){
			
			if ( torrents_ok.contains(new Integer(i))){
							
				current_tracker_torrents[j] = original_tracker_torrents[i];
				j++;
			}
		}
		
			// sort the torrents
			
		sortTrackerTorrents(current_tracker_torrents, true);

			// remove deselected torrents
		
		List	tracker_torrents = new ArrayList();
		
		//String[] cat_items = cat.split( "," );
		
		Map resultMap = new HashMap(catMap);
		
		for(Iterator iter=resultMap.keySet().iterator();iter.hasNext();) {
			
			Integer key = (Integer) resultMap.get(iter.next());
			
			int value = key.intValue();
			
			String testValue = "," + value + ",";
			
			catSelected.put(key, "1");
			
			if(cat.indexOf(testValue) == -1) {
				
				iter.remove();
				
				catSelected.put(key, "0");
				
			}
		}
		
		populateEnabledCatList();
		populateExcludedCatList();
		
		for (int i=0;i<current_tracker_torrents.length;i++){
			
			String c_name = getCategoryName(current_tracker_torrents[i]);
					
			if (resultMap.containsKey(c_name))  {
			
				TrackerTorrent tracker_torrent = current_tracker_torrents[i];
				
				if ( isCategoryEnabled( tracker_torrent )){
					
					tracker_torrents.add( tracker_torrent );
				}
			}
		}
		
		res[0].put(new Integer(0), tracker_torrents);
		res[1].put(new Integer(1), catSelected);
		
		return( res );
	}
	
	protected String
	escapeXML(
		String	str )
	{
		if ( str == null ){
			
			return( "" );
			
		}
		str = str.replaceAll( "&", "&amp;" );
		str = str.replaceAll( ">", "&gt;" );
		str = str.replaceAll( "<", "&lt;" );
		str = str.replaceAll( "\"", "&quot;" );
		str = str.replaceAll( "--", "&#45;&#45;" );
		
		return( str );
	}
	protected String
	escapeXSS(
		String	str )
	{
		if ( str == null ){
			
			return( "" );
			
		}
		str = str.replaceAll( "#", "&#35" );
		str = escapeXML(str);
		str = str.replaceAll( "\\(", "&#40;" );
		str = str.replaceAll( "\\)", "&#41;" );
		
		return( str );
	}
}