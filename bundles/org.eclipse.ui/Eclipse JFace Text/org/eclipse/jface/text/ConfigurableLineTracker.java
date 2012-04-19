package org.eclipse.jface.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.util.Assert;



/**
 * Standard implementation of a generic <code>ILineTracker</code>.
 * The line tracker can be configured with the set of legal line delimiters.
 * Line delimiters are unconstrainted. The line delimiters are used to
 * compute the tracker's line structure. In the case of overlapping line delimiters,
 * the longest line delimiter is given precedence of the shorter ones.<p>
 * This class is not intended to be subclassed.
 */
public class ConfigurableLineTracker extends AbstractLineTracker {
	
	
	/** The strings which are considered being the line delimiter */
	private String[] fDelimiters;
	/** A predefined delimiter info which is always reused as return value */
	private DelimiterInfo fDelimiterInfo= new DelimiterInfo(); 
	
	
	/**
	 * Creates a standard line tracker for the given line delimiters.
	 *
	 * @param legalLineDelimiters the tracker's legal line delimiters,
	 *		may not be <code>null</code> and must be longer than 0
	 */
	public ConfigurableLineTracker(String[] legalLineDelimiters) {
		Assert.isTrue(legalLineDelimiters != null && legalLineDelimiters.length > 0);
		fDelimiters= legalLineDelimiters;
	}
	
	/*
	 * @see ILineDelimiter@getLegalLineDelimiters
	 */
	public String[] getLegalLineDelimiters() {
		return fDelimiters;
	}

	/*
	 * @see AbstractLineTracker#nextDelimiterInfo(String, int)
	 */
	protected DelimiterInfo nextDelimiterInfo(String text, int offset) {
		int[] info= TextUtilities.indexOf(fDelimiters, text, offset);
		if (info[0] == -1)
			return null;
			
		fDelimiterInfo.delimiterIndex= info[0];
		fDelimiterInfo.delimiter= fDelimiters[info[1]];
		fDelimiterInfo.delimiterLength= fDelimiterInfo.delimiter.length();
		return fDelimiterInfo;
	}
}