/*******************************************************************************
 * Copyright (C) 2013 University of Waikato, Hamilton, New Zealand.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Sam Sarjant - initial API and implementation
 ******************************************************************************/
package graph.core.cli;

import graph.core.DAGNode;
import core.Command;

public class RemoveNodeCommand extends Command {
	@Override
	public String helpText() {
		return "{0} node : Removes a specific node from "
				+ "the DAG. All edges including the node are "
				+ "also automatically removed.";
	}

	@Override
	public String shortDescription() {
		return "Removes a specific node from the DAG.";
	}

	@Override
	protected void executeImpl() {
		DAGPortHandler dagHandler = (DAGPortHandler) handler;
		if (data.isEmpty()) {
			printErrorNoData();
			return;
		}

		try {
			DAGNode node = (DAGNode) dagHandler.getDAG().findOrCreateNode(data,
					null, false, false, true);
			dagHandler.getDAG().writeCommand("removenode " + data);
			if (node != null && dagHandler.getDAG().removeNode(node.getID()))
				print("1|Node successfully removed.\n");
			else
				print("-1|Could not remove node.\n");
		} catch (Exception e) {
			e.printStackTrace();
			print("-1|Problem removing node.\n");
		}
	}

}
