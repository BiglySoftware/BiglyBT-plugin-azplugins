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

import java.util.Calendar;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseEvent;

/**
* ConsoleText is an SWT Widget of type StyledText extended to include
* functionality for hyperlinks and other highlights.
*/
class ConsoleText extends StyledText implements MouseMoveListener, MouseListener
{
	/** The number of lines near the end that autoscroll will snap to. */
	final static int SCROLLSNAP = 3;
	/** The color white. */
	final Color DEFAULTCOLOR;
	/** The maximum number of styles that can be applied. */
	final static int MAXSTYLES = 1024;
	/** The maximum number of extended styles ranges can be used. */
	//	final static int MAXSTYLERANGES = 1024;
	/** The cursor to use for hyperlinks. */
	final Cursor HYPERLINKCURSOR;
	/** Bold flag. */
	public final static int BOLD = SWT.BOLD;
	/** Italics flag. */
	public final static int ITALIC = SWT.ITALIC;
	/** Hyperlink flag. */
	public final static int HYPERLINK = 1 << 10;	// Leave enough gap
	/** Maximum number of lines in history. */
	final static int MAXHISTORY = 4096;
	/** When history grows beyond MAXHISTORY by this much, remove that many
	lines. */
	final static int HISTORYBUFFER = 256;
	/** Standard hyperlink visual style. */
	final static int HYPERLINKSTYLE = BOLD | HYPERLINK;
	
	/** Saved Display context. */
	Display display = null;
	/** List of colours for selection. */
	Color colors[] = null;
	/** Configurable styles. */
	Object styles[][] = new Object[MAXSTYLES][];
	/** The current list of non-SWT styles. */
	Vector extendedStyles = new Vector();
	/** The pattern match to detect highlight strings in the text. */
	Pattern styleMatch = null;
	/** Whether a link is currently highlighted. */
	boolean linkHighlighted = false;
	
	/**
	* Main constructor.
	* @param comp The parent.
	*/
	ConsoleText(Composite comp, int params)
	{
		super(comp, params);
		display = comp.getDisplay();
		DEFAULTCOLOR = new Color(display, 238, 238, 238);	// Light Grey
		colors = new Color[] {
			new Color(display, 169, 212, 254),
			new Color(display, 198, 226, 255),
			new Color(display, 226, 240, 255),
			new Color(display, 255, 192, 192),
			new Color(display, 255, 170, 170),
			new Color(display, 238, 238, 238),
			new Color(display, 255, 0, 0),
			new Color(display, 0, 255, 0),};
		
		addMouseMoveListener(this);
		addMouseListener(this);
		HYPERLINKCURSOR = new Cursor(display, SWT.CURSOR_HAND);
	}
	
	/**
	* Adds the standard hyperlink style.
	*/
	public void addHyperlinkStyle()
	{
		addHyperlinkStyle(null, null, HYPERLINKSTYLE);
	}
	
	/**
	* Adds the hyperlink pattern matches. Remember that Java needs
	* double-backspace to escape it from String translation.
	* @param fg The foreground color to use for hyperlinks.
	* @param bg The background color to use for hyperlinks.
	* @param style The visual font style to use for hyperlinks.
	*/
	public void addHyperlinkStyle(Color fg, Color bg, int style)
	{
		//if (!(System.getProperty("java.vendor").equals("Free Software Foundation, Inc.") && System.getProperty("java.version").equals("1.4.2"))) {
			
			//New regex ahoy!
			
			//HTTP urls (Well, not for 10.3.9 Macs... They can't handle them)
			if (!(System.getProperty("java.version").indexOf("1.4.2") > -1)) {
				addStyle("http[s]?://[^\\s]{2,}", fg, bg, style);
				addStyle("www\\.[^\\s]{2,}", fg, bg, style);
			}
			
			// File links
			addStyle("\\\"file:///[^\\\"]+\\\"", fg, bg, style);
			
			// Include IRC /join command
			addStyle("\\s#[^\\s]+", fg, bg, style);
			addStyle("\\s##[^\\s]+", fg, bg, style);
			
			//Include magnet links
			addStyle("magnet:\\?xt=urn:btih:[0-9A-Z]{32}", fg, bg, style);
			addStyle("magnet:\\\\\\\\\\?xt=urn:btih:[0-9A-Z]{32}", fg, bg, style);
			
			/* Unused (Old) expressions
			* Text starting with http:// until the first space.
			* addStyle("(http://|www\\.)[\\w\\.\\-/]*", fg, bg, style);
			* addStyle("http://[\\w.#=?:%&+/-]{2,}[\\w/]", fg, bg, style);
			* addStyle("https://[\\w.#=?:%&+/-]{2,}[\\w/]", fg, bg, style);
			* addStyle("(http://|www\\.)(\\w{2,}\\.){1,2}\\w{2,}(/[\\w-]+)*(/|\\.\\w+)?", fg, bg, style);
			* addStyle("http://(\\w{2,}\\.){1,2}\\w{2,}(/[\\w-]+)*(/|\\.\\w+)?", fg, bg, style);
			* Anything of the form www.A.B (such as www.foo.com/foo...)
			* addStyle("www\\.[\\w.#=?:%&+/-]{2,}[\\w/]", fg, bg, style);
			* addStyle("\\bwww\\.\\w{2,}\\.\\w{2,}[\\w\\-/]*(\\.)?(/[\\s\\.]|\\w+)", fg, bg, style);
			*/
		}
		
		/**
		* Sets a style which applies to regular expression matches. Pattern
		* matching flags can be included in-line with the (?idmsux-idmsux)
		* construct. See Java regex documentation.
		* @param re The regular expression on which to match.
		* @param fg The foreground colour to use when matched.
		* @param bg The background colour to use when matched.
		* @param style The numeric style to apply when matched. May be an OR of
		*     BOLD | ITALIC | HYPERLINK.
		*/
		public void addStyle(String re, Color fg, Color bg, int style)
		{
			try { //Use try because the 1.4.2 GCJ can't handle various regex strings...
				StringBuffer buffer = new StringBuffer();
				int i;
				
				for (i = 0; i < styles.length; i++)
				if (styles[i] == null || styles[i][0].equals(re))
				{
					styles[i] = new Object[] {re, fg, bg, new Integer(style)};
					break;
				}
				
				for (i = 0; i < styles.length; i++)
				if (styles[i] != null)
				buffer.append("|(").append(styles[i][0]).append(')');
				if (buffer.length() > 0)
				buffer.deleteCharAt(0);
				//System.out.println("Pattern match buffer is: " + buffer);
				styleMatch = Pattern.compile(buffer.toString());
			}
			catch (Exception ex)
			{
				
			}
		}
		
		/**
		* Sets a list of background colors which can then be selected by index.
		* Useful for applying different themes.
		* @param colors The list of color values.
		*/
		public void setColors(Color _colors[])
		{
			this.colors = _colors;
		}
		
		/**
		* Appends text with a specific background color index.
		* @param color The background color index.
		* @param text The text to append.
		*/
		public void append(int color, String text)
		{
			text = getTimestamp() + "  " + text + "\n";
			
			if (colors != null && color >= 0 && color < colors.length)
			changeText(colors[color], text, true);
			else
			changeText(DEFAULTCOLOR, text, true);
		}
		
		/**
		* Appends text using the defauly background color.
		* @param text The text to append.
		*/
		@Override
		public void append(String text)
		{
			changeText(DEFAULTCOLOR, text, true);
		}
		
		public void setText(int color, String text)
		{
			if (colors != null && color >= 0 && color < colors.length)
			changeText(colors[color], text, false);
			else
			changeText(DEFAULTCOLOR, text, false);
		}
		
		/**
		* Appends text with a specific background color.
		* @param color The background color.
		* @param text The text to append.
		*/
		private void changeText(final Color color, final String text, final boolean append)
		{
			if (display != null && !display.isDisposed() && !isDisposed())
			{
				display.asyncExec(new Runnable()
				{
					@Override
					public void run()
					{
						if ( display == null || display.isDisposed() || isDisposed()){
							return;
						}
						
						ScrollBar sb = getVerticalBar();
						int lines = getLineCount();
						int start = 0;
						Matcher m;
						StyleRange sr;
						int i, val;
						boolean autoScroll = sb == null || sb.getSelection() >= (sb.getMaximum()
						- sb.getThumb() - getLineHeight() * SCROLLSNAP);
						
						if (lines > MAXHISTORY + HISTORYBUFFER)
						trimHistory(HISTORYBUFFER);
						if(!append)
						{
							replaceTextRange(0, getCharCount(), "");
							extendedStyles.clear();
						}
						
						start = getCharCount();
						//if(!append)
						//	ConsoleText.super.setText("");
						ConsoleText.super.append(text);
						
						if (color != null)
						setLineBackground(lines - 1, 1, color);
						if (styleMatch != null)
						{
							m = styleMatch.matcher(text);
							while (m.find())
							for (i = 0; i < m.groupCount(); i++)
							if (m.group(i + 1) != null)
							{
								val = ((Integer)styles[i][3]).intValue();
								sr = new StyleRange(start + m.start(),
								m.end() - m.start(),
								(Color)styles[i][1],
								(Color)styles[i][2],
								val & (BOLD | ITALIC));
								setStyleRange(sr);
								if (val != (val & (BOLD | ITALIC)))
								extendedStyles.add(new int[] {start +
									m.start(), m.end() - m.start(),
								val});
							}
						}
						
						if (autoScroll)
						setSelection(getCharCount());
					}
				});
			}
		}
		
		/**
		* Trims the list of extended styles when the history gets clipped.
		* @param pos The new start of the history.
		*/
		void trimHistory(int historyBuffer)
		{
			int pos = getOffsetAtLine(historyBuffer);
			int i;
			
			replaceTextRange(0, pos, "");
			
			// Remove all extended styles before pos
			while (extendedStyles.size() > 0 &&
			((int[])extendedStyles.elementAt(0))[0] < pos)
			extendedStyles.removeElementAt(0);
			
			// Shift remaining extended styles by pos
			for (i = 0; i < extendedStyles.size(); i++)
			((int[])extendedStyles.elementAt(i))[0] -= pos;
		}
		
		/**
		* Retrieves the extended style at a given position.
		* @param pos The position.
		* @return The tripple of (start, length, style).
		*/
		private int []getExtendedStyle(int pos)
		{
			int vals[];
			int i;
			
			for (i = 0; i < extendedStyles.size(); i++)
			{
				vals = (int[])extendedStyles.elementAt(i);
				if (vals[0] <= pos && vals[0] + vals[1] > pos)
				return vals;
			}
			
			return null;
		}
		
		/**
		* Formats the current time for display.
		* @return The current time in the form [17:23:07]
		*/
		private String getTimestamp()
		{
			Calendar now = Calendar.getInstance();
			//Full time stamps.
			return "[" + format(now.get(Calendar.HOUR_OF_DAY)) + ":" +
			format(now.get(Calendar.MINUTE)) + ":" +
			format(now.get(Calendar.SECOND)) + "]";
		}
		
		/**
		* Zero-pads a number less than 10.
		* @param The number.
		* @return A String containing a two-digit number.
		*/
		private String format(int n)
		{
			return (n < 10) ? "0" + n : "" + n;
		}
		
		/**
		* Called by SWT to notify of mouse movements. This has the job of
		* highlighting links.
		*/
		@Override
		public void mouseMove(MouseEvent e)
		{
			if (getLinkAtMouse(e) == null)
			{
				setCursor(null);
				if (e.stateMask == 0 && linkHighlighted)
				setSelectionRange(getSelectionRange().x, 0);
				linkHighlighted = false;
				this.setToolTipText(null);
			}
			else {
				//Some funky features to show the action being performed when a click is made.
				setCursor(HYPERLINKCURSOR);
				String link = getLinkAtMouse(e);
				if (link.startsWith(" #")) {
					link = "/join" + link;
				} else if ((link.toLowerCase().startsWith("http")) || (link.toLowerCase().startsWith("www.")) || (link.toLowerCase().startsWith("\"file:///")))
				{
					link = "Open " + link;
				} else if (link.toLowerCase().startsWith("magnet:"))
				{
					link = "Load " + link;
				}
				this.setToolTipText(link);
			}
		}
		
		/**
		* Detects the link at the mouse position denoted by MouseEvent e.
		* @param e The source mouse event from the system.
		* @return The link at the current mouse position, or null if there is no
		*     valid link there.
		*/
		private String getLinkAtMouse(MouseEvent e)
		{
			int pos;
			int extendedStyle[];
			
			try
			{
				pos = getOffsetAtLocation(new Point(e.x, e.y));
				extendedStyle = getExtendedStyle(pos);
				if (extendedStyle != null && (extendedStyle[2] & HYPERLINK) > 0)
				{
					setSelection(extendedStyle[0], extendedStyle[0] +
					extendedStyle[1]);
					linkHighlighted = true;
					return getText(extendedStyle[0], extendedStyle[0] +
					extendedStyle[1] - 1);
				}
			}
			catch (Exception ex)
			{
			}
			
			return null;
		}
		
		/**
		* Detect a mouse click. Called by MouseEvent handler.
		*/
		@Override
		public void mouseUp(MouseEvent e) { }
		
		/**
		* Called when a hyperlink is clicked. Override to receive notification.
		* @param link The hyperlink that was clicked, exactly as per the source
		*     text.
		*/
		public void hyperlinkSelected(String link)
		{
			//		System.out.println("Link clicked: " + link);
		}
		
		/** Unnused. */
		@Override
		public void mouseDown(MouseEvent e) {
		}
		/** Unnused. */
		@Override
		public void mouseDoubleClick(MouseEvent e) {
			//Changed links to double click, too many users having problems controlling their fingers...
			String link = getLinkAtMouse(e);
			
			if (link != null && e.button == 1)
			hyperlinkSelected(link);
		}
	}
	