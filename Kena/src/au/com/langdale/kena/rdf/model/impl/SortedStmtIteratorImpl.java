package au.com.langdale.kena.rdf.model.impl;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.impl.StmtIteratorImpl;

/**
 * An implementation of StmtIterator.
 * 
 * @author bwm
 * @version Release='$Name: $' Revision='$Revision: 1.1 $' Date='$Date:
 *          2009/06/29 08:55:32 $'
 */

public class SortedStmtIteratorImpl extends StmtIteratorImpl {

	public SortedStmtIteratorImpl(Iterator<Statement> iterator) {
		super(initialize(iterator));
	}

	protected static Iterator<Statement> initialize(Iterator<Statement> iterator) {
		Comparator<Statement> comparator = new Comparator<Statement>() {
			@Override
			public int compare(Statement statement1, Statement statement2) {
				return statement1.toString().compareTo(statement2.toString());
			}
		};

		Set<Statement> sortedSet = new TreeSet<Statement>(comparator);
		while (iterator.hasNext()) {
			sortedSet.add(iterator.next());
		}

		return sortedSet.iterator();
	}
}
