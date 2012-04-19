package org.eclipse.jface.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


/**
 * A document partitioner divides a document into a set 
 * of disjoint text partitions. Each partition has a content type, an 
 * offset, and a length. The document partitioner is connected to one document
 * and informed about all changes of this document before any of the 
 * document's document listeners. A document partitioner can thus 
 * incrementally update on the receipt of a document change event.
 * Clients may implement this interface or use the standard 
 * implementation <code>RuleBasedDocumentPartitioner</code>.
 *
 * @see IDocument
 */
public interface IDocumentPartitioner {
	
	/**
	 * Connects the partitioner to a document.
	 * Connect indicates the begin of the usage of the receiver 
	 * as partitioner of the given document. Thus, resources the partitioner
	 * needs to be operational for this document should be allocated.<p>
	 * The caller of this method must ensure that this partitioner is
	 * also set as the document's document partitioner.
	 *
	 * @param document the document to be connected to
	 */
	void connect(IDocument document);
	
	/**
	 * Disconnects the partitioner from the document it is connected to.
	 * Disconnect indicates the end of the usage of the receiver as 
	 * partitioner of the connected document. Thus, resources the partitioner
	 * needed to be operation for its connected document should be deallocated.<p>
	 * The caller of this method should also must ensure that this partitioner is
	 * no longer the document's partitioner.
	 */
	void disconnect();
	
	/**
	 * Informs about a forthcoming document change. Will be called by the
	 * connected document and is not intended to be used by clients
	 * other than the connected document.
	 *
	 * @param event the event describing the forthcoming change
	 */
	void documentAboutToBeChanged(DocumentEvent event); 
	
	/**
	 * The document has been changed. The partitioner updates 
	 * the document's partitioning and returns whether the structure of the
	 * document partitioning has been changed, i.e. whether partitions
	 * have been added or removed. Will be called by the connected document and
	 * is not intended to be used by clients other than the connected document.
	 *
	 * @param event the event describing the document change
	 * @return <code>true</code> if partitioning changed
	 */
	boolean documentChanged(DocumentEvent event);
	
	/**
	 * Returns the set of all legal content types of this partitioner.
	 * I.e. any result delivered by this partitioner may not contain a content type
	 * which would not be included in this method's result.
	 *
	 * @return the set of legal content types
	 */
	String[] getLegalContentTypes();
		
	/**
	 * Returns the content type of the partition containing the
	 * given offset in the connected document. There must be a
	 * document connected to this partitioner.
	 *
	 * @param offset the offset in the connected document
	 * @return the content type of the offset's partition
	 */
	String getContentType(int offset);
	
	/**
	 * Returns the partitioning of the given range of the connected
	 * document. There must be a document connected to this partitioner.
	 *
	 * @param offset the offset of the range of interest
	 * @param length the length of the range of interest
	 * @return the partitioning of the range
	 */
	ITypedRegion[] computePartitioning(int offset, int length);
	
	/**
	 * Returns the partition containing the given offset of
	 * the connected document. There must be a document connected to this
	 * partitioner.
	 *
	 * @param offset the offset for which to determine the partition
	 * @return the partition containing the offset
	 */
	ITypedRegion getPartition(int offset);
}