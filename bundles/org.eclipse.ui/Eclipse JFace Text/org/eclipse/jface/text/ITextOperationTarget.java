package org.eclipse.jface.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


/**
 * Defines the target for a text operation.
 */
public interface ITextOperationTarget {
	
	
	/** 
	 * Text operation code for undoing the last edit command. 
	 */
	static final int UNDO= 1;
	
	/** 
	 * Text operation code for redoing the last undone edit command.
	 */
	static final int REDO= 2;
	
	/** 
	 * Text operation code for moving the selected text to the clipboard.
	 */
	static final int CUT= 3;
	
	/** 
	 * Text operation code for copying the selected text to the clipboard.
	 */
	static final int COPY= 4;
	
	/** 
	 * Text operation code for inserting the clipboard content at the 
	 * current position.
	 */
	static final int PASTE= 5;
	
	/** 
	 * Text operation code for deleting the selected text or if selection
	 * is empty the character  at the right of the current position.
	 */
	static final int DELETE= 6;
	
	/** 
	 * Text operation code for selecting the complete text. 
	 */
	static final int SELECT_ALL= 7;
	
	/** 
	 * Text operation code for shifting the selected text block to the right.
	 */
	static final int SHIFT_RIGHT= 8;
	
	/** 
	 * Text operation code for unshifting the selected text block to the left. 
	 */
	static final int SHIFT_LEFT= 9;
	
	/** 
	 * Text operation code for printing the complete text.
	 */
	static final int PRINT=	10;
	
	/** 
	 * Text operation code for prefixing the selected text block.
	 */
	static final int PREFIX= 11;
	
	/** 
	 * Text operation code for removing the prefix from the selected text block.
	 */
	static final int STRIP_PREFIX= 12;
	
		
	/**
	 * Returns whether the operation specified by the given operation code
	 * can be performed.
	 *
	 * @param operation the operation code
	 * @return <code>true</code> if the specified operation can be performed
	 */
	boolean canDoOperation(int operation);
	
	/**
	 * Performs the operation specified by the operation code on the target.
	 * <code>doOperation</code> must only be called if <code>canDoOperation</code>
	 * returns <code>true</code>.
	 *
	 * @param operation the operation code
	 */
	void doOperation(int operation);
}