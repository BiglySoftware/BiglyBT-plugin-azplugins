/* 
Copyright Paul James Mutton, 2001-2004, http://www.jibble.org/

This file is part of PircBot.

This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/

$Author: gouss $
$Id: NickAlreadyInUseException.java,v 1.2 2006-01-26 01:24:37 gouss Exp $

*/


package org.jibble.pircbot;

/**
 * A NickAlreadyInUseException class.  This exception is
 * thrown when the PircBot attempts to join an IRC server
 * with a user name that is already in use.
 *
 * @since   0.9
 * @author  Paul James Mutton,
 *          <a href="http://www.jibble.org/">http://www.jibble.org/</a>
 * @version    1.4.4 (Build time: Tue Mar 29 20:58:46 2005)
 */
public class NickAlreadyInUseException extends IrcException {
	 
	static final long serialVersionUID = 0;
    /**
     * Constructs a new IrcException.
     *
     * @param e The error message to report.
     */
    public NickAlreadyInUseException(String e) {
        super(e);
    }
    
}