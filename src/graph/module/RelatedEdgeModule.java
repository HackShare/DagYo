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
package graph.module;

import graph.core.DAGEdge;
import graph.core.DAGNode;
import graph.core.DirectedAcyclicGraph;
import graph.core.Edge;
import graph.core.Node;
import graph.core.StringNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import util.Pair;
import util.collection.MultiMap;
import util.collection.trove.TIndexedCollection;

/**
 * The related edge module indexes sets of edges related to a node. The execute
 * method can take both Nodes and ints, representing Nodes present in the edge
 * and the positions of those nodes (if provided).
 * 
 * @author Sam Sarjant
 */
// TODO Refactor class to use DAGEdges instead of Edges
public class RelatedEdgeModule extends DAGModule<Collection<Edge>> {
	private static final long serialVersionUID = 1588174113071358990L;
	protected ConcurrentMap<Node, MultiMap<Object, Edge>> relatedEdges_ = new ConcurrentHashMap<>();
	protected StringHashedEdgeModule stringHashedModule_;
	protected StringStorageModule stringStorageModule_;

	protected void addIfNonDAG(Node node, Object key,
			Collection<Pair<Node, Object>> nonDAGNodes) {
		if (!(node instanceof DAGNode))
			nonDAGNodes.add(new Pair<Node, Object>(node, key));
	}

	protected Object[] asIndexed(Node... nodes) {
		Object[] indexedNodes = new Object[nodes.length * 2];
		for (int i = 0; i < nodes.length; i++) {
			indexedNodes[i * 2] = nodes[i];
			indexedNodes[i * 2 + 1] = i + 1;
		}
		return indexedNodes;
	}

	/**
	 * If the string hash index should be used for string searching.
	 * 
	 * @param predicate
	 *            The predicate of the edge to search. If not-null, checks with
	 *            string storage to see if the predicate is registered.
	 * @param stringNode
	 *            The node to search for (only returns true if a string node).
	 * @return True if the string hash module should be used.
	 */
	protected boolean canSearchHashedModule(Node predicate, Node stringNode) {
		if (stringHashedModule_ == null || !(stringNode instanceof StringNode))
			return false;
		if (predicate != null && stringStorageModule_ != null
				&& stringStorageModule_.isCompressedEdge(predicate))
			return false;
		return true;
	}

	protected Object defaultKey() {
		return -1;
	}

	/**
	 * Filters out the non-DAG results for the query that do not match the
	 * non-DAG inputs.
	 * 
	 * @param edges
	 *            The edges to filter.
	 * @param args
	 *            The args to filter by.
	 * @return The collection of filtered edges.
	 */
	protected final Collection<Edge> filterNonDAGs(Collection<Edge> edges,
			Object[] args) {
		Collection<Pair<Node, Object>> nonDAGNodes = findNonDAGs(args);

		if (nonDAGNodes.isEmpty() || edges == null) {
			return edges;
		}

		// Check every edge (EXPENSIVE)
		Collection<Edge> filtered = new HashSet<>();
		for (Edge e : edges) {
			Node[] edgeNodes = e.getNodes();
			boolean matches = true;
			for (Pair<Node, Object> nonDAG : nonDAGNodes) {
				if (nonDAG.objB_ == defaultKey()) {
					if (!ArrayUtils.contains(edgeNodes, nonDAG.objA_)) {
						matches = false;
						break;
					}
				} else if (!matchingNonDAG(nonDAG, edgeNodes)) {
					matches = false;
					break;
				}
			}

			if (matches)
				filtered.add(e);
		}
		return filtered;
	}

	protected final Collection<Pair<Node, Object>> findNonDAGs(Object[] args) {
		Collection<Pair<Node, Object>> nonDAGNodes = new ArrayList<>();
		for (int i = 0; i < args.length; i++) {
			Node node = (Node) args[i];
			Object key = defaultKey();
			if (i < args.length - 1 && !(args[i + 1] instanceof Node)) {
				i++;
				key = parseKeyArg(args[i]);
			}

			addIfNonDAG(node, key, nonDAGNodes);
		}

		return nonDAGNodes;
	}

	protected Collection<Edge> getEdges(Node node, Object edgeKey,
			boolean createNew) {
		MultiMap<Object, Edge> indexedEdges = relatedEdges_.get(node);
		if (indexedEdges == null) {
			if (createNew) {
				indexedEdges = MultiMap.createConcurrentHashSetMultiMap();
				relatedEdges_.put(node, indexedEdges);
			} else
				return new ConcurrentLinkedQueue<>();
		}

		Collection<Edge> edges = (edgeKey != null) ? indexedEdges.get(edgeKey)
				: indexedEdges.values();
		if (edges == null) {
			if (createNew)
				edges = indexedEdges
						.putCollection(edgeKey, new TreeSet<Edge>());
			else
				return new ConcurrentLinkedQueue<>();
		}
		return edges;
	}

	protected List<EdgeCol> locateEdgeCollections(boolean createNew,
			Object... args) {
		List<EdgeCol> edgeCols = new ArrayList<>();
		Node pred = null;
		for (int i = 0; i < args.length; i++) {
			Node n = (Node) args[i];
			boolean additive = true;

			// Get index of node
			Integer index = null;
			if (i < args.length - 1 && args[i + 1] instanceof Integer) {
				i++;
				index = (int) args[i];
				if (index < 0) {
					additive = false;
					index *= -1;
				} else if (index == 1)
					pred = n;
			}

			Collection<Edge> edgeCol = null;
			if (n instanceof DAGNode)
				edgeCol = getEdges(n, index, createNew);
			else if (canSearchHashedModule(pred, n))
				edgeCol = stringHashedModule_.execute(createNew, n.getName());

			if (edgeCol != null)
				edgeCols.add(new EdgeCol(additive, edgeCol));
		}
		return edgeCols;
	}

	protected boolean matchingNonDAG(Pair<Node, Object> nonDAG, Node[] edgeNodes) {
		return edgeNodes[(Integer) nonDAG.objB_].toString().equals(
				nonDAG.objA_.toString());
	}

	protected Object parseKeyArg(Object arg) {
		return (int) arg - 1;
	}

	@Override
	public boolean addEdge(DAGEdge edge) {
		Collection<EdgeCol> edgeCollections = locateEdgeCollections(true,
				asIndexed(edge.getNodes()));
		for (EdgeCol edgeCol : edgeCollections)
			edgeCol.add(edge);
		return true;
	}

	@Override
	public void clear() {
		relatedEdges_.clear();
	}

	@Override
	public Collection<Edge> execute(Object... args)
			throws IllegalArgumentException {
		List<EdgeCol> edgeCollections = locateEdgeCollections(false, args);
		Collections.sort(edgeCollections, new SmallestFirstComparator());
		Collection<Edge> edges = null;
		for (EdgeCol edgeCol : edgeCollections) {
			if (edgeCol.edgeCol_ == null)
				continue;
			if (edges == null)
				edges = edgeCol.edgeCol_;
			else if (edgeCol.additive_)
				edges = CollectionUtils.retainAll(edges, edgeCol.edgeCol_);
			else if (!edgeCol.additive_)
				edges = CollectionUtils.removeAll(edges, edgeCol.edgeCol_);

			if (edges.isEmpty())
				return edges;
		}

		edges = filterNonDAGs(edges, args);
		if (edges == null)
			return new ArrayList<>(0);
		return edges;
	}

	public Collection<Edge> findEdgeByNodes(Node... nodes) {
		Object[] indexedNodes = asIndexed(nodes);
		return execute(indexedNodes);
	}

	@Override
	public boolean initialisationComplete(TIndexedCollection<DAGNode> nodes,
			TIndexedCollection<DAGEdge> edges, boolean forceRebuild) {
		if (!relatedEdges_.isEmpty() && !forceRebuild)
			return false;

		// Iterate through all nodes and edges, adding aliases
		System.out.print("Rebuilding related edge map... ");
		relatedEdges_.clear();
		defaultRebuild(nodes, false, edges, true);
		System.out.println("Done!");
		return true;
	}

	@Override
	public boolean removeEdge(DAGEdge edge) {
		boolean result = false;
		Collection<EdgeCol> indexedEdges = locateEdgeCollections(false,
				asIndexed(edge.getNodes()));

		for (EdgeCol col : indexedEdges)
			result |= col.remove(edge);
		return result;
	}

	@Override
	public void setDAG(DirectedAcyclicGraph directedAcyclicGraph) {
		super.setDAG(directedAcyclicGraph);
		stringHashedModule_ = (StringHashedEdgeModule) directedAcyclicGraph
				.getModule(StringHashedEdgeModule.class);
		stringStorageModule_ = (StringStorageModule) directedAcyclicGraph
				.getModule(StringStorageModule.class);
	}

	@Override
	public boolean supportsEdge(DAGEdge edge) {
		return true;
	}

	@Override
	public boolean supportsNode(DAGNode node) {
		return false;
	}

	@Override
	public String toString() {
		return "Related Edges: " + relatedEdges_.size();
	}

	protected class EdgeCol {
		public boolean additive_;

		public Collection<Edge> edgeCol_;

		public EdgeCol(boolean additive, Collection<Edge> edgeCol) {
			additive_ = additive;
			edgeCol_ = edgeCol;
		}

		public boolean add(Edge edge) {
			return edgeCol_.add(edge);
		}

		public boolean remove(Edge edge) {
			return edgeCol_.remove(edge);
		}

		public int size() {
			return edgeCol_.size();
		}

		@Override
		public String toString() {
			return edgeCol_.toString() + " (" + additive_ + ")";
		}
	}

	protected class SmallestFirstComparator implements Comparator<EdgeCol> {
		@Override
		public int compare(EdgeCol o1, EdgeCol o2) {
			if (o1.additive_ && !o2.additive_)
				return -1;
			else if (!o1.additive_ && o2.additive_)
				return 1;
			return Integer.compare(o1.size(), o2.size());
		}
	}
}
