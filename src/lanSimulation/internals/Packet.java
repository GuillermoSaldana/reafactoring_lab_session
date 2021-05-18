/*   This file is part of lanSimulation.
 *
 *   lanSimulation is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   lanSimulation is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with lanSimulation; if not, write to the Free Software
 *   Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *   Copyright original Java version: 2004 Bart Du Bois, Serge Demeyer
 *   Copyright C++ version: 2006 Matthias Rieger, Bart Van Rompaey
 */
package lanSimulation.internals;

import java.io.IOException;
import java.io.Writer;

import lanSimulation.Network;

/**
 * A <em>Packet</em> represents a unit of information to be sent over the Local
 * Area Network (LAN).
 */
public class Packet {
	/**
	 * Holds the actual message to be send over the network.
	 */
	public String message;
	/**
	 * Holds the name of the Node which initiated the request.
	 */
	public String origin;
	/**
	 * Holds the name of the Node which should receive the information.
	 */
	public String destination;

	/**
	 * Construct a <em>Packet</em> with given #message and #destination.
	 */
	public Packet(String message, String destination) {
		this.message = message;
		this.origin = "";
		this.destination = destination;
	}

	/**
	 * Construct a <em>Packet</em> with given #message, #origin and #receiver.
	 */
	public Packet(String message, String origin, String destination) {
		this.message = message;
		this.origin = origin;
		this.destination = destination;
	}

	public boolean printDocument(Node printer, Network network, Writer report) {
		String author = "Unknown";
		String title = "Untitled";
		int startPos = 0, endPos = 0;
	
		if (printer instanceof Printer) {
			try {
				if (message.startsWith("!PS")) {
					
					author = generateText(author);
					title = generateText(title);
					
					network.accountingDocument(report, author, title);
				} else {
					title = "ASCII DOCUMENT";
					if (message.length() >= 16) {
						author = message.substring(8, 16);
					}
					
					network.accountingDocument(report, author, title);
				}
				
			} catch (IOException exc) {
				
			}
			
			return true;
		} else {
			try {
				report.write(">>> Destinition is not a printer, print job cancelled.\n\n");
				report.flush();
			} catch (IOException exc) {
				
			}
			
			return false;
		}
	}

	private String generateText(String author) {
		int startPos = 0;
		int endPos = 0;
		String message;
		int num = 0;
		
		if(author.equals("Unknown")) {
			message = "author:";
			num = 7;
		}else {
			message = "title:";
			num = 6;
		}
		
		startPos = message.indexOf(message);
		if (startPos >= 0) {
			endPos = message.indexOf(".", startPos + num);
			if (endPos < 0) {
				endPos = message.length();
			}
			
			author = message.substring(startPos + num, endPos);
		}
		return author;
	}

}