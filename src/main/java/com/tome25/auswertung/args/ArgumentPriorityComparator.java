package com.tome25.auswertung.args;

import java.util.Comparator;

/**
 * A {@link Comparator} comparing {@link Argument Arguments} by their priority.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class ArgumentPriorityComparator implements Comparator<Argument> {

	/**
	 * The only instance of this comparator to be used.
	 */
	public static final ArgumentPriorityComparator INSTANCE = new ArgumentPriorityComparator();

	/**
	 * The only constructor to create a new instance.<br/>
	 * Private to prevent other instances being created..
	 */
	private ArgumentPriorityComparator() {
	}

	@Override
	public int compare(Argument a1, Argument a2) {
		return Short.compare(a1.prio, a2.prio);
	}

}
