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
package lanSimulation;

import lanSimulation.internals.*;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.*;

/**
 * A <em>Network</em> represents the basic data stucture for simulating a Local
 * Area Network (LAN). The LAN network architecture is a token ring, implying
 * that packahes will be passed from one node to another, until they reached
 * their destination, or until they travelled the whole token ring.
 */
public class Network {
	/**
	 * Holds a pointer to myself. Used to verify whether I am properly initialized.
	 */
	private Network initPtr;
	/**
	 * Holds a pointer to some "first" node in the token ring. Used to ensure that
	 * various printing operations return expected behaviour.
	 */
	private Node firstNode;
	/**
	 * Maps the names of workstations on the actual workstations. Used to initiate
	 * the requests for the network.
	 */
	private Hashtable workstations;

	/**
	 * Construct a <em>Network</em> suitable for holding #size Workstations.
	 * <p>
	 * <strong>Postcondition:</strong>(result.isInitialized()) & (!
	 * result.consistentNetwork());
	 * </p>
	 */
	public Network(int size) {
		assert size > 0;
		initPtr = this;
		firstNode = null;
		workstations = new Hashtable(size, 1.0f);
	}

	/**
	 * Return a <em>Network</em> that may serve as starting point for various
	 * experiments. Currently, the network looks as follows.
	 * 
	 * <pre>
	 Workstation Filip [Workstation] -> Node -> Workstation Hans [Workstation]
	 -> Printer Andy [Printer] -> ...
	 * </pre>
	 * <p>
	 * <strong>Postcondition:</strong>result.isInitialized() &
	 * result.consistentNetwork();
	 * </p>
	 */
	public static Network defaultExample() {
		final Network network = new Network(2);

		final Node wsFilip = new Workstation("Filip");
		final Node n1 = new Node("n1");
		final Node wsHans = new Workstation("Hans");
		final Node prAndy = new Printer("Andy");

		wsFilip.nextNode_ = n1;
		n1.nextNode_ = wsHans;
		wsHans.nextNode_ = prAndy;
		prAndy.nextNode_ = wsFilip;

		network.workstations.put(wsFilip.name_, wsFilip);
		network.workstations.put(wsHans.name_, wsHans);
		network.firstNode = wsFilip;

		assert network.isInitialized();
		assert network.consistentNetwork();
		return network;
	}

	/**
	 * Answer whether #receiver is properly initialized.
	 */
	public boolean isInitialized() {
		return initPtr.equals(this);
	};

	/**
	 * Answer whether #receiver contains a workstation with the given name.
	 * <p>
	 * <strong>Precondition:</strong>this.isInitialized();
	 * </p>
	 */
	public boolean hasWorkstation(final String workstation) {
		Workstation node;

		assert isInitialized();
		node = (Workstation) workstations.get(workstation);
		if (node == null) {
			return false;
		}
		return true;
	};

	/**
	 * Answer whether #receiver is a consistent token ring network. A consistent
	 * token ring network - contains at least one workstation and one printer - is
	 * circular - all registered workstations are on the token ring - all
	 * workstations on the token ring are registered.
	 * <p>
	 * <strong>Precondition:</strong>this.isInitialized();
	 * </p>
	 */
	public boolean consistentNetwork() {
		assert isInitialized();
		Enumeration iter;
		Node currentNode;
		int printersFound = 0, workstationsFound = 0;
		Hashtable encountered = new Hashtable(workstations.size() * 2, 1.0f);

		if (workstations.isEmpty()) {
			return false;
		}
		
		if (firstNode == null) {
			return false;
		}
		
		// verify whether all registered workstations are indeed workstations
		return verifyWorkstations(printersFound, workstationsFound, encountered);
	}

	private boolean verifyWorkstations(int printersFound, int workstationsFound, Hashtable encountered) {
		Enumeration iter;
		Node currentNode;
		iter = workstations.elements();
		while (iter.hasMoreElements()) {
			currentNode = (Node) iter.nextElement();
			if (currentNode instanceof Workstation == false) {
				return false;
			}
			
		}
		
		// enumerate the token ring, verifying whether all workstations are registered
		// also count the number of printers and see whether the ring is circular
		return enumerateTokenRing(printersFound, workstationsFound, encountered);
	}

	private boolean enumerateTokenRing(int printersFound, int workstationsFound, Hashtable encountered) {
		Node currentNode;
		currentNode = firstNode;
		while (!encountered.containsKey(currentNode.name_)) {
			encountered.put(currentNode.name_, currentNode);
			if (currentNode instanceof Workstation) {
				workstationsFound++;
			}
			
			if (currentNode instanceof Printer) {
				printersFound++;
			}
			
			currentNode = send(currentNode);
		}
		
		if (isFirstNode(currentNode)) {
			return false;
		}
		// not circular
		if (printersFound == 0) {
			return false;
		}
		// does not contain a printer
		if (workstationsFound != workstations.size()) {
			return false;
		}
		 // not all workstations are registered
			// all verifications succeedeed
		return true;
	}

	private Node send(Node currentNode) {
		return currentNode.nextNode_;
	}

	/**
	 * The #receiver is requested to broadcast a message to all nodes. Therefore
	 * #receiver sends a special broadcast packet across the token ring network,
	 * which should be treated by all nodes.
	 * <p>
	 * <strong>Precondition:</strong> consistentNetwork();
	 * </p>
	 * 
	 * @param report Stream that will hold a report about what happened when
	 *               handling the request.
	 * @return Anwer #true when the broadcast operation was succesful and #false
	 *         otherwise
	 */
	public boolean requestBroadcast(Writer report) {
		assert consistentNetwork();

		try {
			report.write("Broadcast Request\n");
		} catch (IOException exc) {
			
		}

		Node currentNode = firstNode;
		Packet packet = new Packet("BROADCAST", firstNode.name_, firstNode.name_);
		do {
			currentNode.logging(report, packet);
			
			currentNode = send(currentNode);
		} while (!packet.destination.equals(currentNode.name_));

		try {
			report.write(">>> Broadcast travelled whole token ring.\n\n");
		} catch (IOException exc) {
			
		}
		
		return true;
	}

	/**
	 * The #receiver is requested by #workstation to print #document on #printer.
	 * Therefore #receiver sends a packet across the token ring network, until
	 * either (1) #printer is reached or (2) the packet travelled complete token
	 * ring.
	 * <p>
	 * <strong>Precondition:</strong> consistentNetwork() &
	 * hasWorkstation(workstation);
	 * </p>
	 * 
	 * @param workstation Name of the workstation requesting the service.
	 * @param document    Contents that should be printed on the printer.
	 * @param printer     Name of the printer that should receive the document.
	 * @param report      Stream that will hold a report about what happened when
	 *                    handling the request.
	 * @return Anwer #true when the print operation was succesful and #false
	 *         otherwise
	 */
	public boolean requestWorkstationPrintsDocument(String workstation, String document, String printer,
			Writer report) {
		assert consistentNetwork() & hasWorkstation(workstation);

		try {
			report.write("'");
			report.write(workstation);
			report.write("' requests printing of '");
			report.write(document);
			report.write("' on '");
			report.write(printer);
			report.write("' ...\n");
		} catch (IOException exc) {
			
		}

		boolean result = false;
		Node startNode, currentNode;
		Packet packet = new Packet(document, workstation, printer);

		startNode = (Node) workstations.get(workstation);
		
		startNode.logging(report, packet);
		
		currentNode = send(startNode);
		while ((!packet.destination.equals(currentNode.name_)) & (!packet.origin.equals(currentNode.name_))) {
			currentNode.logging(report, packet);
			currentNode = send(currentNode);
		}

		result = checkPacketDestination(report, currentNode, packet);

		return result;
	}

	private boolean checkPacketDestination(Writer report, Node currentNode, Packet packet) {
		boolean result;
		if (packet.destination.equals(currentNode.name_)) {
			result = packet.printDocument(currentNode, this, report);
		} else {
			try {
				report.write(">>> Destinition not found, print job cancelled.\n\n");
				report.flush();
			} catch (IOException exc) {
				
			}
			
			result = false;
		}
		return result;
	}

	public void accountingDocument(Writer report, String author, String title) throws IOException {
		report.write("\tAccounting -- author = '");
		report.write(author);
		report.write("' -- title = '");
		report.write(title);
		report.write("'\n");
		if(title != "ASCII DOCUMENT")
			report.write(">>> Postscript job delivered.\n\n");
		else
			report.write(">>> ASCII Print job delivered.\n\n");
		report.flush();
	}

	/**
	 * Return a printable representation of #receiver.
	 * <p>
	 * <strong>Precondition:</strong> isInitialized();
	 * </p>
	 */
	public String toString() {
		assert isInitialized();
		StringBuffer buf = new StringBuffer(30 * workstations.size());
		printOn(buf);
		return buf.toString();
	}

	/**
	 * Write a printable representation of #receiver on the given #buf.
	 * <p>
	 * <strong>Precondition:</strong> isInitialized();
	 * </p>
	 */
	public void printOn(StringBuffer buf) {
		assert isInitialized();
		Node currentNode = firstNode;
		do {
			currentNode.printOn(buf);
			buf.append(" -> ");
			currentNode = send(currentNode);
		} while (isFirstNode(currentNode));
		buf.append(" ... ");
	}

	private boolean isFirstNode(Node currentNode) {
		return currentNode != firstNode;
	}

	/**
	 * Write a HTML representation of #receiver on the given #buf.
	 * <p>
	 * <strong>Precondition:</strong> isInitialized();
	 * </p>
	 */
	public void printHTMLOn(StringBuffer buf) {
		assert isInitialized();

		buf.append("<HTML>\n<HEAD>\n<TITLE>LAN Simulation</TITLE>\n</HEAD>\n<BODY>\n<H1>LAN SIMULATION</H1>");
		Node currentNode = firstNode;
		buf.append("\n\n<UL>");
		do {
			buf.append("\n\t<LI> ");
			currentNode.printOn(buf);
			buf.append(" </LI>");
			currentNode = send(currentNode);
		} while (isFirstNode(currentNode));
		buf.append("\n\t<LI>...</LI>\n</UL>\n\n</BODY>\n</HTML>\n");
	}

	/**
	 * Write an XML representation of #receiver on the given #buf.
	 * <p>
	 * <strong>Precondition:</strong> isInitialized();
	 * </p>
	 */
	public void printXMLOn(StringBuffer buf) {
		assert isInitialized();

		Node currentNode = firstNode;
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<network>");
		do {
			buf.append("\n\t");
			currentNode.printXMLOn(buf);
			currentNode = send(currentNode);
		} while (isFirstNode(currentNode));
		buf.append("\n</network>");
	}

}
