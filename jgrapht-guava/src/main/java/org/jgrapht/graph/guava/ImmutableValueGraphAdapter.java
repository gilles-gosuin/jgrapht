/*
 * (C) Copyright 2018-2018, by Dimitrios Michail and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
package org.jgrapht.graph.guava;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.ToDoubleFunction;

import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.util.TypeUtil;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

/**
 * A graph adapter class using Guava's {@link ImmutableValueGraph}.
 * 
 * <p>The adapter uses class {@link EndpointPair} to represent edges. Since the underlying value graph 
 * is immutable, the resulting graph is unmodifiable.
 * 
 * <p>
 * The class uses a converter from Guava's values to JGraphT's double weights. Thus, the resulting
 * graph is weighted.
 * 
 * <p>Assume for example that the following class is the value type: <blockquote>
 * 
 * <pre>
 * class MyValue
 *     implements Serializable
 * {
 *     private double value;
 *
 *     public MyValue(double value)
 *     {
 *         this.value = value;
 *     }
 *
 *     public double getValue()
 *     {
 *         return value;
 *     }
 * }
 * </pre>
 * 
 * </blockquote>
 * 
 * Then one could create an adapter using the following code: <blockquote>
 * 
 * <pre>
 * MutableValueGraph&lt;String, MyValue&gt; valueGraph =
 *     ValueGraphBuilder.directed().allowsSelfLoops(true).build();
 * valueGraph.addNode("v1");
 * valueGraph.addNode("v2");
 * valueGraph.putEdgeValue("v1", "v2", new MyValue(5.0));
 * 
 * ImmutableValueGraph&lt;String, MyValue&gt; immutableValueGraph = ImmutableValueGraph.copyOf(valueGraph);
 * 
 * Graph&lt;String, EndpointPair&lt;String&gt;&gt; graph = new ImmutableValueGraphAdapter&lt;&gt;(
 *     immutableValueGraph, (ToDoubleFunction&lt;MyValue&gt; &amp; Serializable) MyValue::getValue);
 * 
 * double weight = graph.getEdgeWeight(EndpointPair.ordered("v1", "v2")); // should return 5.0
 * </pre>
 * 
 * </blockquote>
 * 
 * @author Dimitrios Michail
 *
 * @param <V> the graph vertex type
 * @param <W> the value type
 */
public class ImmutableValueGraphAdapter<V, W>
    extends BaseValueGraphAdapter<V, W, ImmutableValueGraph<V, W>>
    implements Graph<V, EndpointPair<V>>, Cloneable, Serializable
{
    private static final long serialVersionUID = 2629294259825656044L;

    protected static final String GRAPH_IS_IMMUTABLE = "Graph is immutable";

    /**
     * Create a new adapter.
     * 
     * @param valueGraph the value graph
     * @param valueConverter a function that converts a value to a double
     */
    public ImmutableValueGraphAdapter(
        ImmutableValueGraph<V, W> valueGraph, ToDoubleFunction<W> valueConverter)
    {
        super(valueGraph, valueConverter);
    }

    @Override
    public EndpointPair<V> addEdge(V sourceVertex, V targetVertex)
    {
        throw new UnsupportedOperationException(GRAPH_IS_IMMUTABLE);
    }

    @Override
    public boolean addEdge(V sourceVertex, V targetVertex, EndpointPair<V> e)
    {
        throw new UnsupportedOperationException(GRAPH_IS_IMMUTABLE);
    }

    @Override
    public boolean addVertex(V v)
    {
        throw new UnsupportedOperationException(GRAPH_IS_IMMUTABLE);
    }

    @Override
    public EndpointPair<V> removeEdge(V sourceVertex, V targetVertex)
    {
        throw new UnsupportedOperationException(GRAPH_IS_IMMUTABLE);
    }

    @Override
    public boolean removeEdge(EndpointPair<V> e)
    {
        throw new UnsupportedOperationException(GRAPH_IS_IMMUTABLE);
    }

    @Override
    public boolean removeVertex(V v)
    {
        throw new UnsupportedOperationException(GRAPH_IS_IMMUTABLE);
    }

    @Override
    public void setEdgeWeight(EndpointPair<V> e, double weight)
    {
        throw new UnsupportedOperationException(GRAPH_IS_IMMUTABLE);
    }

    @Override
    public GraphType getType()
    {
        return super.getType().asUnmodifiable();
    }

    /**
     * Returns a shallow copy of this graph instance. Neither edges nor vertices are cloned.
     *
     * @return a shallow copy of this graph.
     *
     * @throws RuntimeException in case the clone is not supported
     *
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone()
    {
        try {
            ImmutableValueGraphAdapter<V, W> newGraph = TypeUtil.uncheckedCast(super.clone());

            newGraph.unmodifiableVertexSet = null;
            newGraph.unmodifiableEdgeSet = null;
            newGraph.valueConverter = this.valueConverter;
            newGraph.valueGraph = ImmutableValueGraph.copyOf(Graphs.copyOf(this.valueGraph));

            return newGraph;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    private void writeObject(ObjectOutputStream oos)
        throws IOException
    {
        oos.defaultWriteObject();

        // write type
        oos.writeObject(getType());

        // write vertices
        int n = vertexSet().size();
        oos.writeInt(n);
        for (V v : vertexSet()) {
            oos.writeObject(v);
        }

        // write edges
        int m = edgeSet().size();
        oos.writeInt(m);
        for (EndpointPair<V> e : edgeSet()) {
            V u = e.nodeU();
            V v = e.nodeV();
            oos.writeObject(u);
            oos.writeObject(v);
            oos.writeObject(valueGraph.edgeValue(u, v).get());
        }
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream ois)
        throws ClassNotFoundException, IOException
    {
        ois.defaultReadObject();

        GraphType type = (GraphType) ois.readObject();
        if (type.isMixed() || type.isAllowingMultipleEdges()) {
            throw new IOException("Graph type not supported");
        }

        MutableValueGraph<V, W> mutableValueGraph =
            (type.isDirected() ? ValueGraphBuilder.directed() : ValueGraphBuilder.undirected())
                .allowsSelfLoops(type.isAllowingSelfLoops()).build();

        // read vertices
        int n = ois.readInt();
        for (int i = 0; i < n; i++) {
            V v = (V) ois.readObject();
            mutableValueGraph.addNode(v);
        }

        // read edges
        int m = ois.readInt();
        for (int i = 0; i < m; i++) {
            V s = (V) ois.readObject();
            V t = (V) ois.readObject();
            W w = (W) ois.readObject();
            mutableValueGraph.putEdgeValue(s, t, w);
        }

        // setup the immutable copy
        this.valueGraph = ImmutableValueGraph.copyOf(mutableValueGraph);
    }

}
