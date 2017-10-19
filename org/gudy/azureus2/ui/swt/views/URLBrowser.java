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

import java.awt.BorderLayout;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import org.eclipse.swt.program.Program;
import com.biglybt.core.util.Constants;
import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.download.DownloadException;

class URLBrowser
{
    URLBrowser(PluginInterface _plugin_interface, String _url)
    {
	    try
	    {	
	    	_plugin_interface.getDownloadManager().addDownload( new URL(_url) );
    	}
    	catch(DownloadException exc)
        {
            return;
        }
        catch(MalformedURLException exc)
        {
            return;
        }
    }
    
    URLBrowser( String url )
    {
    	
	if (!url.matches("^(?i)\\w+:.*")) url = "http://" + url;
	
	if (Constants.isWindows)
		Program.launch(url);
	else
    	openSwingWindow(url);
	    }
    private void openSwingWindow(String url)
    {
	JFrame frame;
	final JEditorPane html;

	html = new JEditorPane();
	html.setEditable(false);
	html.addHyperlinkListener(new HyperlinkListener()
	{
	    @Override
	    public void hyperlinkUpdate(HyperlinkEvent e)
	    {
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
		    if (e instanceof HTMLFrameHyperlinkEvent)
			((HTMLDocument)html.getDocument()).
			processHTMLFrameHyperlinkEvent(
			(HTMLFrameHyperlinkEvent)e);
                    else
			loadSwingPage(html, e.getURL().toString());
	    }
	});

	frame = new JFrame("BiglyBT");
	frame.setLayout(new BorderLayout());
	frame.add(new JScrollPane(html), BorderLayout.CENTER);
	frame.setSize(640, 480);
	frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	frame.setVisible(true);

	html.setText("Loading " + url + ". Please Wait...");
	loadSwingPage(html, url);
    }

    void loadSwingPage(final JEditorPane html, final String url)
    {
	(new Thread()
	{
	    @Override
	    public void run()
	    {
		try
		{
		    html.setPage(url);
		}
		catch (Exception e)
		{
		    html.setText("Error: " + e);
		}
	    }
	}).start();
    }

/*    private void showDialog(Shell shell, int flags, String message)
    {
	MessageBox mb = new MessageBox(shell, flags | SWT.OK);

	mb.setMessage(message.toString());
	mb.open();
    }*/
}
