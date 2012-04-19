package org.eclipse.jface.text.reconciler;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.jface.text.ITextViewer;



/**
 * An <code>IReconciler</code> defines and maintains a model of the content
 * of the text  viewer's document in the presence of changes applied to this 
 * document. An <code>IReconciler</code> is a <code>ITextViewer</code> add-on.<p>
 * Reconcilers are assumed to be asynchronous, i.e. they allow a certain 
 * temporal window of inconsistency between the document and the model of
 * the content of this document. <p>
 * Reconcilers have a list of  <code>IReconcilingStrategy</code> objects 
 * each of which is registered for a  particular document content type. 
 * The reconciler uses the strategy objects to react on the changes applied
 * to the text viewer's document.<p>
 * The interface can be implemented by clients. By default, clients use
 * <code>Reconciler</code> as the standard implementer of this interface. 
 *
 * @see ITextViewer
 * @see IReconcilingStrategy 
 */
public interface IReconciler {
		
	/**
	 * Installs the reconciler on the given text viewer. After this method has been
	 * finished, the reconciler is operational. I.e., it works without requesting 
	 * further client actions until <code>uninstall</code> is called.
	 * 
	 * @param textViewer the viewer on which the reconciler is installed
	 */
	void install(ITextViewer textViewer);
	
	/**
	 * Removes the reconciler from the text viewer it has previously been
	 * installed on. 
	 */
	void uninstall();
	
	/**
	 * Returns the reconciling strategy registered with the reconciler
	 * for the specified content type.
	 *
	 * @param contentType the content type for which to determine the reconciling strategy
	 * @return the reconciling strategy registered for the given content type, or
	 *		<code>null</code> if there is no such strategy
	 */
	IReconcilingStrategy getReconcilingStrategy(String contentType);
}