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

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import util.UtilityMethods;

import commands.MapCommand;

import core.Command;
import core.CommandParser;

public class SortableMapCommand extends MapCommand {

	@Override
	public String helpText() {
		return "{0} function funcArgs delimiter [showInput] [mapSort] \\n "
				+ "collection collectionArgs \\n captureRegExp : "
				+ "Applies function with args (using $1, $2,... "
				+ "syntax for regexp) to every item in the output of "
				+ "collection using collectionArgs. Every output is "
				+ "separated by delimiter. Optional showing of the "
				+ "input to the map function. Optional sorting by the "
				+ "output of map command rather than collection command "
				+ "(cannot show inputs if this is selected). "
				+ "Multiline commands are not explicitly supported "
				+ "by should work.";
	}

	@Override
	protected void executeImpl() {
		DAGPortHandler dagHandler = (DAGPortHandler) handler;
		// Format each element into individual commands
		int spaceIndex = data.indexOf(' ');
		if (spaceIndex == -1) {
			print("No function or args specified.\n");
			return;
		}
		String[] split = data.split("\\s+");
		if (split.length <= 2) {
			print("No args or delimiter specified.\n");
			return;
		}

		BufferedReader in = getPortHandler().getReader();
		int funcExtent = split.length - 1;
		String delimiter = split[funcExtent];
		boolean showInputs = false;
		if (delimiter.equalsIgnoreCase("T") || delimiter.equalsIgnoreCase("F")) {
			if (delimiter.equalsIgnoreCase("T"))
				showInputs = true;
			funcExtent--;
			delimiter = split[funcExtent];
		}

		boolean sortByMap = false;
		if (delimiter.equalsIgnoreCase("T") || delimiter.equalsIgnoreCase("F")) {
			if (delimiter.equalsIgnoreCase("T"))
				sortByMap = true;
			if (sortByMap)
				showInputs = false;
			funcExtent--;
			delimiter = split[funcExtent];
		}

		// Parse mapped function
		if (funcExtent == 0) {
			print("No args or delimiter specified.\n");
			return;
		}
		String functionName = split[0];
		String[] funcArgs = Arrays.copyOfRange(split, 1, funcExtent);
		String funcArgsStr = StringUtils.join(funcArgs, ' ');

		try {
			// Read collection
			String collectionCommand = in.readLine().trim();
			spaceIndex = collectionCommand.indexOf(' ');
			String collectionName = collectionCommand;
			String collectionArgs = "";
			if (spaceIndex != -1) {
				collectionName = collectionCommand.substring(0, spaceIndex);
				collectionArgs = collectionCommand.substring(spaceIndex + 1)
						.trim();
			}

			// Run the command
			// Disable sorting (temporarily)
			String sortOrder = null;
			if (sortByMap) {
				sortOrder = dagHandler.get(DAGPortHandler.SORT_ORDER);
				dagHandler.set(DAGPortHandler.SORT_ORDER, "");
			}
			Command command = CommandParser.parse(collectionName + " "
					+ collectionArgs);
			command.setPortHandler(handler);
			command.execute();
			String output = command.getResult();

			// Read regex
			String regexStr = in.readLine().trim();
			Pattern regex = Pattern.compile(regexStr);

			// Map function to it
			Matcher m = regex.matcher(output);
			ArrayList<String> inputs = new ArrayList<>();
			ArrayList<String> outputs = new ArrayList<>();
			while (m.find()) {
				inputs.add(m.group());
				String[] groups = new String[m.groupCount() + 1];
				for (int i = 0; i < groups.length; i++)
					groups[i] = m.group(i);

				Command funcCommand = CommandParser.parse(functionName + " "
						+ UtilityMethods.replaceToken(funcArgsStr, groups));
				funcCommand.setPortHandler(handler);
				funcCommand.execute();
				outputs.add(funcCommand.getResult());
			}

			// Re-enable sort-order and sort mapped results.
			if (sortByMap) {
				dagHandler.set(DAGPortHandler.SORT_ORDER, sortOrder);
				outputs = (ArrayList<String>) dagHandler.postProcess(outputs,
						0, outputs.size(), true);
			}
			for (int i = 0; i < outputs.size(); i++) {
				if (showInputs)
					print(inputs.get(i) + delimiter + "\n");
				print(outputs.get(i) + delimiter + "\n");
			}
		} catch (Exception e) {
			print("" + e);
		}
	}
}
