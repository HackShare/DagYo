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

import java.io.Serializable;

import util.UniqueID;

public interface Edge extends Serializable, Identifiable, UniqueID {
	public boolean containsNode(Node node);

	public Node[] getNodes();
	
	public String toString(boolean useIDs);
}
