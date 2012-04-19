package org.eclipse.jface.text.information;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

 
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;


/**
 * Provides information related to the content of a text viewer.<p>
 * Clients may implement this interface.
 *
 * @see ITextViewer
 */
public interface IInformationProvider {
	
	/**
	 * Returns the region of the text viewer's document close to the given 
	 * offset that contains a subject about which information can be provided.<p>
	 * For example, if information can be provided on a per code block basis, 
	 * the offset should be used to find the enclosing code block and the source
	 * range of the block should be returned.
	 *
	 * @param textViewer the text viewer in which informationhas been requested
	 * @param offset the offset at which information has been requested
	 * @return the region of the text viewer's document containing the information subject
	 */
	IRegion getSubject(ITextViewer textViewer, int offset);
	
	/**
	 * Returns the information about the given subject or <code>null</code> if
	 * no information is available.
	 *  
	 * @param textViewer the viewer in whose document the subject is contained
	 * @param subject the text region constituting the information subject
	 * @return the information about the subject  
	 */
	String getInformation(ITextViewer textViewer, IRegion subject);
}
