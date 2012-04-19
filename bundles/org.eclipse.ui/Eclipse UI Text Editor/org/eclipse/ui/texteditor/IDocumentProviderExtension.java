package org.eclipse.ui.texteditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;


/**
 * Extension to <code>IDocumentProvider</code>. Intention to be integrated with
 * <code>IDocumentProvider</code>. Should not yet be considered API.
 */
public interface IDocumentProviderExtension {
	
	/**
	 * Returns whether the document provider thinks that the given element is read-only.
	 * If this method returns <code>true</code>, <code>saveDocument</code> could fail.
	 * This method does not say anything about the document constructed from the given
	 * element. If the given element is not connected to this document provider, the return
	 * value is undefined. Document providers are allowed to use a cache to answer this
	 * question, i.e. there can be a difference between the "real" state of the element and
	 * the return value.
	 * 
	 * @param element the element
	 * @return <code>true</code> if the given element is read-only, <code>false</code> otherwise
	 */
	boolean isReadOnly(Object element);
	
	/**
	 * Returns whether the document provider thinks that the given element can persistently be modified.
	 * This is orthogonal to <code>isReadOnly</code> as read-only elements may be modifiable and
	 * writable elements may not be modifiable. If the given element is not connected to this document
	 * provider, the result is undefined. Document providers are allowed to use a cache to answer this
	 * question, i.e. there can be a difference between the "real" state of the element and the return
	 * value.
	 * 
	 * @param element the element
	 * @return <code>true</code> if the given element is modifiable, <code>false</code> otherwise
	 */
	boolean isModifiable(Object element);
	
	/**
	 * Validates the state of the given element. This method  may change the "real" state of the
	 * element. If using, it also updates the internal caches, so that this method may also change
	 * the results returned by <code>isReadOnly</code> and <code>isModifiable</code>. If the
	 * given element is not connected to this document provider, the effect is undefined.
	 * 
	 * @param element the element
	 * @param computationContext the context in which the computation is performed, e.g., a SWT shell
	 * @exception CoreException if validating fails
	 */
	void validateState(Object element, Object computationContext) throws CoreException;
	
	/**
	 * Updates the state cache for the given element. This method may change the result returned
	 * by <code>isReadOnly</code> and <code>isModifiable</code>. If the given element is not
	 * connected to this document provider, the effect is undefined.
	 * 
	 * @param element the element
	 * @exception CoreException if validating fails
	 */
	void updateStateCache(Object element) throws CoreException;
}
