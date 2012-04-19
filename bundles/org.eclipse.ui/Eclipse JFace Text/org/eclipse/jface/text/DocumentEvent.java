package org.eclipse.jface.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.jface.util.Assert;


/**
 * Specification of changes applied to documents. 
 * All changes are represented as replace commands, i.e.
 * specifying a document range whose text gets replaced with different text.
 * In addition to this information, the event also contains the changed document.
 *
 * @see IDocument
 */
public class DocumentEvent {
	
	/** The changed document */
	IDocument fDocument;
	/** The document offset */
	int fOffset;
	/** Length of the replaced document text */
	int fLength;
	/** Text inserted into the document */
	String fText;
	
	/**
	 * Creates a new document event.
	 *
	 * @param doc the changed document
	 * @param offset the offset of the replaced text
	 * @param length the length of the replaced text
	 * @param text the substitution text
	 */
	public DocumentEvent(IDocument doc, int offset, int length, String text) {
		
		Assert.isNotNull(doc);
		Assert.isTrue(offset >= 0);
		Assert.isTrue(length >= 0);
		
		fDocument= doc;
		fOffset= offset;
		fLength= length;
		fText= text;
	}

	/**
	 * Returns the changed document.
	 *
	 * @return the changed document
	 */
	public IDocument getDocument() {
		return fDocument;
	}
	
	/**
	 * Returns the offset of the change
	 * 
	 * @return the offset of the change
	 */
	public int getOffset() {
		return fOffset;
	}

	/**
	 * Returns the length of the replaced text.
	 *
	 * @return the length of the replaced text
	 */
	public int getLength() {
		return fLength;
	}
			
	/**
	 * Returns the text that has been inserted.
	 *
	 * @return the text that has been inserted
	 */
	public String getText() {
		return fText;
	}
}