/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.splitmodel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import au.com.langdale.inference.AsyncModel;
import au.com.langdale.inference.AsyncResult;

import com.hp.hpl.jena.graph.Factory;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.n3.turtle.ParserTurtle;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * A query processor for split models.
 */
public class SplitReader extends SplitBase implements AsyncModel {
	
	private static class Query {
		protected Triple pattern; 
		protected AsyncResult results;
		protected int pending;
	
		public Query(Triple pattern, AsyncResult results, int pending) {
			this.pattern = pattern;
			this.results = results;
			this.pending = pending;
		}

		public Triple getPattern() {
			return pattern;
		}

		public boolean add(Triple result, boolean subjectResident, boolean objectResident) {
			return results.add(result);
		}

		public void close() {
			pending --;
			if( pending == 0 ) 
				results.close();
		}

		public int getPending() {
			return pending;
		}
	}
	
	private static class ObjectQuery extends Query {

		public ObjectQuery(Triple pattern, AsyncResult results, int pending) {
			super(pattern, results, pending);
		}

		@Override
		public boolean add(Triple result,  boolean subjectResident, boolean objectResident) {
			if( objectResident )
				return results.add(result);
			else
				return true;
		}
	}
	
	private static class SubjectQuery extends Query {

		public SubjectQuery(Triple pattern, AsyncResult results, int pending) {
			super(pattern, results, pending);
		}

		@Override
		public boolean add(Triple result,  boolean subjectResident, boolean objectResident) {
			if( subjectResident )
				return results.add(result);
			else
				return true;
		}
	}

	private class Bucket   {
		private Map queries = new LinkedHashMap();
		private Graph graph;
		private int load_count, query_count, index;

		public Bucket(int ix) {
			index = ix;
		}

		public Graph getGraph() {
			return graph;
		}

		public void push(Query query) {
			List group = (List) queries.get(query.getPattern());
			if( group == null) {
				group = new LinkedList();
				queries.put(query.getPattern(), group);
			}
			group.add(query);
			query_count++;
		}

		public boolean remove(Query query) {
			List group = (List) queries.get(query.getPattern());
			if( group != null && group.remove(query)) {
					query_count--;
					return true;
			}
			return false;
		}

		public List pop() {
			Iterator it = queries.values().iterator();
			List group = (List) it.next();
			it.remove();
			query_count -= group.size();
			return group;
		}

		public int size() {
			return query_count;
		}
		
		public void execute() throws IOException {
			load();
			List group = pop();
			Triple pattern = ((Query)group.get(0)).getPattern();
			
			ExtendedIterator it = graph.find(pattern);
			while (it.hasNext()) {
				Triple result = (Triple) it.next();
				executeGroup(group, result);
				if(group.size() == 0) {
					it.close();
					break;
				}
			}
			
			for (Iterator iq = group.iterator(); iq.hasNext();) {
				Query query = (Query) iq.next();
				query.close();
			}
		}

		private void executeGroup(List group, Triple result) {
			Node subject = result.getSubject();
			Node predicate = result.getPredicate();
			Node object = result.getObject();
			
			boolean subjectResident = subject.isURI() && selectBucket(subject) == this;
			boolean objectResident = object.isURI() && ! predicate.equals(RDF_TYPE) && selectBucket(object) == this;
			
			for (Iterator iq = group.iterator(); iq.hasNext();) {
				Query query = (Query) iq.next();
				if(! query.add(result, subjectResident, objectResident)) {
					iq.remove();
					removeFromOthers(query);
				}
			}
		}

		private void removeFromOthers(Query query) {
			for( int ib = 0, ip = query.getPending() -1; ip > 0; ip--) {
				Bucket bucket = buckets[ib++];
				if( bucket == this)
					bucket = buckets[ib++];
				bucket.remove(query);
			}
		}

		private void load() throws IOException {
			if(graph == null) {
				graph = read(getFile(index));
				load_count++;
				// System.out.println("Loaded bucket: " + index + ", loads: " + load_count + ", queries: " + query_count);
				cache.add(this);
			}
		}

		public void unload() {
			graph = null;
			// System.out.println("Unloaded bucket: " + index + ", loads: " + load_count + ", queries: " + query_count);
		}

		public int getModulus() throws IOException {
			load();
			Integer m = getInteger(getGraph(), DOCUMENT, MODULUS);
			if( m == null || m.intValue() < 2 )
				throw new IOException("not a valid split model (modulus undefined): " + destin);
			return m.intValue();
		}
	}
	
	private static class Cache {
		private int quota;
		private Queue resident = new LinkedList();

		public Cache(int quota) {
			this.quota = quota;
		}
		
		public void add(Bucket bucket) {
			evict(quota-1);
			resident.add(bucket);
		}

		public void evict(int goal) {
			while( resident.size() > goal) {
				Bucket bucket = (Bucket) resident.remove();
				bucket.unload();
			}
		}

		public void promote(Bucket bucket) {
			if(resident.remove(bucket))
				resident.add(bucket);
		}
		

		public Bucket findMostQueries() {
			Bucket result = null;
			for (Iterator it = resident.iterator(); it.hasNext();) {
				Bucket cand = (Bucket) it.next();
				if( result == null && cand.size() > 0 || result != null && cand.size() > result.size())
					result = cand;
			}
			return result;
		}
	}

	public static final int DEFAULT_QUOTA = 2;
	
	private static final Node RDF_TYPE = Node.createURI(RDF_TYPE_URI);
	
	private Bucket[] buckets;

	private boolean running;

	private Cache cache;
	
	private Map quotes;
	
	/**
	 * Access the split model at the given location.
	 * @param location: the pathname of a split model directory
	 * @param quote: the maximum number of splits to be resident in memory
	 * @throws IOException
	 */
	public SplitReader(String locations, int quota) throws IOException {
		this(new File(locations), new Cache(quota));
	}
	/**
	 * Access the split model at the given location.
	 * @param location: the pathname of a split model directory
	 * @throws IOException
	 */
	public SplitReader(String location) throws IOException {
		this(new File(location), new Cache(DEFAULT_QUOTA));
	}
	
	private SplitReader(File destin, Cache cache) throws IOException {
		this.destin = destin;
		if( ! this.destin.isDirectory())
			throw new IOException("not a directory: " + destin);
		this.cache = cache;
		quotes = new HashMap();
		Bucket boot = new Bucket(0);
		modulus = boot.getModulus();
		createBuckets(boot);
	}
	
	/* (non-Javadoc)
	 * @see au.com.langdale.splitmodel.AsyncModel#getQuote(com.hp.hpl.jena.graph.Node)
	 */
	public AsyncModel getQuote(Node quote) throws IOException {
		AsyncModel result = null;
		if( quote.isURI()) {
			String name = quote.getLocalName();
			result = (AsyncModel) quotes.get(name);
			if( result == null) {
				File nested = new File(destin, name);
				if( nested.exists()) {
					result = new SplitReader(nested, cache);
					quotes.put(name, result);
				}
			}
		}
		return result;
	}

	/**
	 * Associate an external model with a node in the current model.
	 * This is used to link a base model to an difference model, for example.
	 * @param quote: the node representing (ie quoting) a submodel
	 * @param location: the pathname of a split model
	 * @throws IOException
	 */
	public void assignQuote(Node quote, String location) throws IOException {
		assignQuote(quote, new SplitReader(new File(location), cache));
	}
	
	private void assignQuote(Node quote, AsyncModel model) {
		quotes.put(quote.getLocalName(), model);
	}
	
	/* (non-Javadoc)
	 * @see au.com.langdale.splitmodel.AsyncModel#run()
	 */
	public void run() throws IOException {
		if(running)
			return;
		running = true;
		try {
			for(;;)
				if( !schedule())
					break;
		}
		finally {
			running = false;
			cache.evict(0);
		}
		// printStats();
	}

	private void printStats() {
		System.out.println("-----------------------------");
		System.out.println("Bucket: Loads");
		int total = 0;
		for (int ix = 0; ix < buckets.length; ix++) {
			System.out.println(ix + ": " + buckets[ix].load_count);
			total += buckets[ix].load_count;
		}
		System.out.println("Total: " + total);
	}

	private void createBuckets(Bucket boot) {
		buckets = new Bucket[modulus];
		buckets[0] = boot;
		for (int ix = 1; ix < modulus; ix++) {
			buckets[ix] = new Bucket(ix);
		}
	}
	/* (non-Javadoc)
	 * @see au.com.langdale.splitmodel.AsyncModel#find(com.hp.hpl.jena.reasoner.TriplePattern, au.com.langdale.splitmodel.SplitReader.SplitResult)
	 */
	public void find(TriplePattern clause, AsyncResult results) {
		Triple pattern =  new Triple(
				var2Any(clause.getSubject()), 
				var2Any(clause.getPredicate()), 
				var2Any(clause.getObject()));
		
		Bucket bucket = selectBucket(pattern);
		
//		if( running && (bucket == null || bucket.getGraph() == null))
//			System.out.println("Query to non resident bucket: " + pattern);

		if( bucket != null) {
			bucket.push(new Query(pattern, results, 1));
		} else {
			Query query;
			if(clause.getObject().isVariable() && ! clause.getSubject().isVariable() && ! clause.getPredicate().equals(RDF_TYPE)) 
				query = new ObjectQuery(pattern, results, buckets.length);
			else
				query = new SubjectQuery(pattern, results, buckets.length);
			
			for (int ix = 0; ix < buckets.length; ix++) 
				buckets[ix].push(query);
		}
	}
	
	private Bucket selectBucket(Triple pattern) {
		Node subject = pattern.getSubject();
		Node object = pattern.getObject();
		boolean rdf_type = pattern.getPredicate().equals(RDF_TYPE);
		
		Bucket bucket;
		if( subject.isURI() && object.isURI() && ! rdf_type) {
			Bucket sbucket = selectBucket(subject);
			if( sbucket.getGraph() != null)
				bucket = sbucket;
			else {
				Bucket obucket = selectBucket(object);
				if( obucket.getGraph() != null) {
					bucket = obucket;
				}
				else {
					if( sbucket.size() > 0 || obucket.size() == 0)
						bucket = sbucket;
					else
						bucket = obucket;
				}
			}
		}
		else if(subject.isURI()) {
			bucket = selectBucket(subject);
		}
		else if(object.isURI() && ! rdf_type) {
			bucket = selectBucket(object);
		}
		else {
			// broad query
			bucket = null;
		}
		return bucket;
	}
	

	private Bucket selectBucket(Node subject) {
		return buckets[hashURI(subject.getURI())];
	}

	/**
	 * Select a query and a bucket and execute.
	 */
	private boolean schedule() throws IOException {
		Bucket bucket = cache.findMostQueries();
		if( bucket != null )
			cache.promote(bucket);
		else
			bucket = findBucketWithMostQueries();
		
		if(bucket.size() == 0)
			return false;
		
		bucket.execute();
		return true;
	}

	private Bucket findBucketWithMostQueries() {
		Bucket result = findLocalBucketWithMostQueries();
		for (Iterator it = quotes.values().iterator(); it.hasNext();) {
			SplitReader quote = (SplitReader) it.next();
			Bucket cand = quote.findBucketWithMostQueries();
			if( cand.size() > result.size()) 
				result = cand;
		}
		return result;
	}
	
	private Bucket findLocalBucketWithMostQueries() {
		Bucket result = buckets[0];
		for (int ix = 1; ix < buckets.length; ix++) {
			Bucket cand = buckets[ix];
			if( cand.size() > result.size()) 
				result = cand;
			
		}
		return result;
	}

	public static Node var2Any(Node node) {
		return node.isVariable() || node == AsyncModel.WILDCARD ? Node.ANY: node;
	}

	private static Integer getInteger(Graph data, String subj, String pred) {
	    Iterator it = data.find(Node.createURI(subj), Node.createURI(pred), Node.ANY);
	    while (it.hasNext()) {
			Triple t = (Triple) it.next();
			if( t.getObject().isLiteral() && t.getObject().getLiteralDatatypeURI().equals(XSD_INTEGER_URI)) {
				return (Integer) t.getObject().getLiteralValue();
			}
		}
		return null;
	}

	private static Graph read(File file) throws IOException {
		Graph graph = Factory.createDefaultGraph();
		if( file.exists()) {
			ParserTurtle parser = new ParserTurtle();
			parser.parse(graph, file.toURI().toString(), new BufferedInputStream(new FileInputStream(file)));
		}
		return graph;
	}
}
