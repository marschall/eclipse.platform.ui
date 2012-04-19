package org.eclipse.jface.text.contentassist;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.IDocument;


/**
 * The interface of completion proposals generated by content assist processors.
 * A completion proposal contains information used to present the proposed completion
 * to the user, to insert the completion should the user select it, and to present
 * context information for the choosen completion once it has been inserted.<p>
 * The interface can be implemented by clients. By default, clients use
 * <code>CompletionProposal</code> as the standard implementer of this interface. 
 *
 * @see IContentAssistProcessor
 */
public interface ICompletionProposal {

	/**
	 * Inserts the proposed completion into the given document.
	 *
	 * @param document the document into which to insert the proposed completion
	 */
	void apply(IDocument document);
	
	/**
	 * Returns the new selection after the proposal has been applied to 
	 * the given document in absolute document coordinates.
	 *
	 * @param document the document into which the proposed completion has been inserted
	 * @return the new selection in absolute document coordinates
	 */
	Point getSelection(IDocument document);

	/**
	 * Returns optional additional information about the proposal.
	 * The additional information will be presented to assist the user
	 * in deciding if the selected proposal is the desired choice.
	 *
	 * @return the additional information or <code>null</code>
	 */
	String getAdditionalProposalInfo();

	/**
	 * Returns the string to be displayed in the list of completion proposals.
	 *
	 * @return the string to be displayed
	 */
	String getDisplayString();

	/**
	 * Returns the image to be displayed in the list of completion proposals.
	 * The image would typically be shown to the left of the display string.
	 *
	 * @return the image to be shown or <code>null</code> if no image is desired
	 */
	Image getImage();

	/**
	 * Returns optional context information associated with this proposal.
	 * The context information will automatically be shown if the proposal
	 * has been applied.
	 *
	 * @return the context information for this proposal or <code>null</code>
	 */
	IContextInformation getContextInformation();
}