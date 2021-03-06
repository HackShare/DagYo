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

import graph.module.DAGModule;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import core.Command;

public class DAGStatsCommand extends Command {

	@Override
	public String shortDescription() {
		return "Outputs a brief overview of various DAG statistics.";
	}

	@Override
	protected void executeImpl() {
		DAGPortHandler dagHandler = (DAGPortHandler) handler;
		long elapsed = System.currentTimeMillis()
				- dagHandler.getDAG().startTime_;
		String elapsedStr = String.format(
				"%02d:%02d:%02d",
				TimeUnit.MILLISECONDS.toHours(elapsed),
				TimeUnit.MILLISECONDS.toMinutes(elapsed)
						- TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS
								.toHours(elapsed)),
				TimeUnit.MILLISECONDS.toSeconds(elapsed)
						- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
								.toMinutes(elapsed)));
		print("Uptime: " + elapsedStr + "\n");
		print("Num nodes: " + dagHandler.getDAG().getNumNodes() + "\n");
		print("Num edges: " + dagHandler.getDAG().getNumEdges() + "\n");
		print("Active modules:\n");
		Collection<DAGModule<?>> modules = dagHandler.getDAG().getModules();
		for (DAGModule<?> modName : modules)
			print("\t" + modName + "\n");
		Runtime rt = Runtime.getRuntime();
		rt.gc();
		long bytes = rt.totalMemory() - rt.freeMemory();
		double mb = bytes / (1024d * 1024);
		DecimalFormat format = new DecimalFormat("###,###,###,###,##0.00");
		print("Total memory usage: " + format.format(mb) + "MB\n");
	}
}
