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

import graph.module.SubDAGExtractorModule;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

import org.nustaq.serialization.annotations.OneOf;

import util.UniqueID;

/**
 * A generic DAG object. This object has an ID and properties.
 * 
 * ==== IMPORTANT ==== Ensure that any subclass defines a default constructor. I
 * don't know how to enforce this, but it is required for module serialisation.
 * 
 * @author Sam Sarjant
 */
// @EqualnessIsIdentity
public abstract class DAGObject implements UniqueID, Serializable,
		Identifiable, Comparable<DAGObject> {
	private static final long serialVersionUID = -5088948795943227278L;
	public static final String CREATION_DATE = "creationDate";
	public static final String CREATOR = "creator";

	/**
	 * Unfortunately, I cannot delegate this annotation to a subclass, so
	 * serialisation shortcuts must be defined here.
	 */
	@OneOf({ CREATION_DATE, CREATOR, "ancsID", "predID",
			DirectedAcyclicGraph.EPHEMERAL_MARK, "CommonConcepts", "CYCImport",
			"MT", SubDAGExtractorModule.TAG_PREFIX, "provenance" })
	private String[] properties_;

	public void clearProps() {
		properties_ = new String[0];
	}

	protected int id_;

	protected DAGObject(Node creator) {
		int index = 0;
		if (creator != null) {
			properties_ = new String[4];
			properties_[index++] = CREATOR;
			properties_[index++] = creator.getIdentifier(true);
		} else
			properties_ = new String[2];
		properties_[index++] = CREATION_DATE;
		properties_[index++] = "" + System.currentTimeMillis();
		id_ = requestID();
	}

	public DAGObject() {
		this(null);
	}

	protected abstract void readFullObject(ObjectInput in) throws IOException,
			ClassNotFoundException;

	protected int requestID() {
		return -1;
	}

	protected abstract void writeFullObject(ObjectOutput out)
			throws IOException;

	synchronized void put(String key, String value) {
		for (int i = 0; i < properties_.length; i += 2) {
			if (properties_[i].equals(key)) {
				properties_[i + 1] = value;
				return;
			}
		}

		String[] propCopy = new String[properties_.length + 2];
		System.arraycopy(properties_, 0, propCopy, 0, properties_.length);
		propCopy[properties_.length] = key;
		propCopy[properties_.length + 1] = value;
		properties_ = propCopy;
	}

	synchronized void remove(String key) {
		for (int i = 0; i < properties_.length; i += 2) {
			if (properties_[i].equals(key)) {
				String[] propCopy = new String[properties_.length - 2];
				if (i > 0) {
					System.arraycopy(properties_, 0, propCopy, 0, i);
				}
				if (i < properties_.length - 2) {
					System.arraycopy(properties_, i + 2, propCopy, i,
							properties_.length - i - 2);
				}
				properties_ = propCopy;
			}
		}
	}

	@Override
	public int compareTo(DAGObject o) {
		if (o == null)
			return -1;
		return Integer.compare(getID(), o.getID());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DAGObject other = (DAGObject) obj;
		if (getID() != other.getID())
			return false;
		return true;
	}

	public Date getCreationDate() {
		return new Date(Long.parseLong(getProperty(CREATION_DATE)));
	}

	public String getCreator() {
		return getProperty(CREATOR);
	}

	@Override
	public int getID() {
		return id_;
	}

	@Override
	public String getIdentifier() {
		return "" + id_;
	}

	public String[] getProperties() {
		return Arrays.copyOf(properties_, properties_.length);
	}

	public String getProperty(String key) {
		for (int i = 0; i < properties_.length; i += 2)
			if (properties_[i].equals(key))
				return properties_[i + 1];
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		int id = getID();
		result = prime * result + id;
		return result;
	}

	/**
	 * Only use this method if you're absolutely sure of the object's ID, and
	 * the object represents a skeleton reference.
	 * 
	 * @param id
	 *            The ID to set.
	 */
	public void setID(int id) {
		id_ = id;
	}

	@Override
	public String toString() {
		return id_ + "";
	}
}
