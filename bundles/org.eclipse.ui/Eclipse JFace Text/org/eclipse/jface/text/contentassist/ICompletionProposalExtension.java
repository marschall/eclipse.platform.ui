package org.eclipse.jface.text.contentassist;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.jface.text.IDocument;


/**
 * For internal use only. Not API. <p>
 * A completion proposal extension is for extending
 * <code>ICompletionProposalExtension</code> instances with new functionality.
*/
public interface ICompletionProposalExtension {
	
	/**
	 * Applies the proposed completion to the given document. The insertion
	 * has been triggered by entering the given character at the given offset.
	 * This method assumes that <code>isValidFor</code> returns
	 * <code>true</code> if called for <code>offset</code>.
	 *
	 * @param document the document into which to insert the proposed completion
	 * @param trigger the trigger to apply the completion
	 * @param offset the offset at which the trigger has been activated
	 */
	void apply(IDocument document, char trigger, int offset);
	
	/**
	 * Returns whether this completion proposal is valid for the given
	 * position in the given document.
	 * 
	 * @param document the document for which the proposal is tested
	 * @param offset the offset for which the proposal is tested
	 */
	boolean isValidFor(IDocument document, int offset);
	
	/**
	 * Returns the characters which trigger the application of this completion proposal.
	 * 
	 * @return the completion characters for this completion proposal or <code>null</code>
	 *		if no completion other than the new line character is possible
	 */
	char[] getTriggerCharacters();
	
	/**
	 * Returns the position to which the computed context information refers to or
	 * <code>-1</code> if no context information can be provided by this completion proposal.
	 * 
	 * @return the position to which the context information refers to or <code>-1</code> for no information
	 */
	int getContextInformationPosition();
}