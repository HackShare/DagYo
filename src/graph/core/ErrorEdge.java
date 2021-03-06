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

public interface ErrorEdge extends Edge {
	/**
	 * Gets the error message, in either pretty form (for humans) or syntactic
	 * form.
	 *
	 * @param isPretty
	 *            If true, returns a human-readable error. Otherwise, any format
	 *            is acceptable.
	 * @return A description of the error.
	 */
	public String getError(boolean isPretty);
}
