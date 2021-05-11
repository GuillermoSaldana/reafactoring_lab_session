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

import lanSimulation.tests.*;
import java.lang.AssertionError;
import java.io.*;

public class LANSimulation {

	public static void doRegressionTests() {
		junit.textui.TestRunner.run(LANTests.suite());
	}

	public static void simulate() {
		Network network = Network.defaultExample();
		StringWriter report = new StringWriter(100);
		StringBuffer buf = new StringBuffer(100);

		System.out.print("siumlate on Network: ");
		System.out.println(network);
		System.out.println();

		network.printHTMLOn(buf);
		System.out.println("---------------------------------HTML------------------------------------------");
		System.out.println(buf.toString());
		System.out.println();

		buf.setLength(0);
		network.printXMLOn(buf);
		System.out.println("---------------------------------XML------------------------------------------");
		System.out.println(buf.toString());
		System.out.println();

		System.out.println("---------------------------------SCENARIOS------------------------------------------");
		String workstation = "Filip";
		String validPrinter = "Andy";
		
		String document = "author: FILIP   Hello World";
		savePrintRequestInReport(network, workstation, document, validPrinter, report, true);
		savePrintRequestInReport(network, workstation, document, "UnknownPrinter", report, false);
		savePrintRequestInReport(network, workstation, document, "Hans", report, false);
		savePrintRequestInReport(network, workstation, document, "n1", report, false);

		document = "Hello World";
		savePrintRequestInReport(network, workstation, document, validPrinter, report, true);

		document = "!PS Hello World in postscript.author:Filip.title:Hello.";
		savePrintRequestInReport(network, workstation, document, validPrinter, report, true);
		savePrintRequestInReport(network, workstation, document, "Hans", report, false);

		document = "!PS Hello World in postscript.Author:Filip.Title:Hello.";
		savePrintRequestInReport(network, workstation, document, validPrinter, report, true);

		document = "!PS Hello World in postscript.author:Filip;title:Hello;";
		savePrintRequestInReport(network, workstation, document, validPrinter, report, true);

		document = "!PS Hello World in postscript.author:.title:.";
		savePrintRequestInReport(network, workstation, document, validPrinter, report, true);

		try {
			System.out.print("'UnknownWorkstation' prints 'does not matter' on 'does not matter': ");
			System.out.print(network.requestWorkstationPrintsDocument("UnknownWorkstation", "does not matter",
					"does not matter", report));
			System.out.println(" (??? no exception);");
		} catch (AssertionError e1) {
			System.out.println("exception (as expected);");
		}
		;

		System.out.print("BROADCAST REQUEST: ");
		System.out.print(network.requestBroadcast(report));
		System.out.println(" (expects true);");

		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println("---------------------------------REPORT------------------------------------------");
		System.out.println(report.toString());
	}
	
	private static void savePrintRequestInReport(Network network, String workstation, String document, String printer, Writer report, boolean expected) {
		System.out.print("'" + workstation + "' prints '" + document + "' on '" + printer + "': ");
		System.out.print(network.requestWorkstationPrintsDocument(workstation, document, printer, report));
		if(expected) {
			System.out.println(" (expects true);");
		}else {
			System.out.println(" (expects false);");
		}
		
	}

	public static void main(String args[]) {

		if (args.length <= 0) {
			System.out.println("Usage: t(est) | s(imulate) nrOfIterations '");
		} else if (args[0].equals("t")) {
			doRegressionTests();
		} else if (args[0].equals("s")) {
			executeSimulateCommand(args);
		} else {
			printUnknownCommand(args);
		}
	}

	private static void executeSimulateCommand(String[] args) {
		Integer nrOfIters = new Integer(1);
		if (args.length > 1) {
			nrOfIters = new Integer(args[1]);
		}
		;

		for (int i = 0; i < nrOfIters.intValue(); i++) {
			simulate();
		}
	}

	private static void printUnknownCommand(String[] args) {
		System.out.print("Unknown command to LANSimulation: '");
		System.out.print(args[0]);
		System.out.println("'");
	}
}
