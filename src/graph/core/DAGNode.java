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
package graph.core;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.regex.Pattern;

import util.UtilityMethods;

/**
 * A node is a unique object representing a single 'concept'. Each node has a
 * unique name and id.
 * 
 * @author Sam Sarjant
 */
public class DAGNode extends DAGObject implements Node {
	private static final long serialVersionUID = 2072866863770254720L;

	public static final String ANON_TO_STRING = "__ANON__";

	/** The counter for assigning ids to edges. */
	public static int idCounter_ = 1;

	public static final Pattern QUOTED_NAME = Pattern
			.compile("\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"");

	public static final Pattern UNSPACED_NAME = Pattern
			.compile("[a-zA-Z0-9][^\\s?()']+");

	public static final Pattern NAME_OR_ID = Pattern.compile("(?:'?\\d+)|"
			+ "(?:" + QUOTED_NAME.pattern() + ")|(?:" + UNSPACED_NAME.pattern()
			+ ")");

	public static final Pattern VALID_NAME = Pattern.compile("(?:"
			+ QUOTED_NAME.pattern() + ")|" + "(?:" + UNSPACED_NAME.pattern()
			+ ")");

	/** The unique name of the node. */
	protected String nodeName_;

	protected DAGNode(Node creator) {
		super(creator);
	}

	protected DAGNode(String name, Node creator) {
		super(creator);

		if (!name.matches(VALID_NAME.pattern())) {
			System.err.println("Node name:" + name
					+ " is invalid. Must not start with [ ()'?].");
			name = "INVALID_NAME" + name;
		}
		if (name.startsWith("\"") && name.endsWith("\""))
			name = UtilityMethods.shrinkString(name, 1);
		nodeName_ = name;
	}

	public DAGNode() {
		super(null);
	}

	@Override
	protected void readFullObject(ObjectInput in) throws IOException,
			ClassNotFoundException {
		nodeName_ = (String) in.readObject();
	}

	@Override
	protected int requestID() {
		return idCounter_++;
	}

	@Override
	protected void writeFullObject(ObjectOutput out) throws IOException {
		out.writeObject(nodeName_);
	}

	@Override
	public String getIdentifier(boolean useName) {
		if (useName)
			return getName();
		return getIdentifier();
	}

	@Override
	public String getName() {
		if (isAnonymous())
			return ANON_TO_STRING + id_;
		return nodeName_;
	}

	public boolean isAnonymous() {
		return nodeName_ == null;
	}

	@Override
	public String toString() {
		return getName();
	}

	public static boolean isValidName(String nodeStr) {
		return nodeStr.matches(VALID_NAME.pattern())
				&& PrimitiveNode.parseNode(nodeStr) == null;
	}
}
