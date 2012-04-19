package org.eclipse.ui.internal.registry;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.ArrayList;

/**
 * Category provides for hierarchical grouping of elements
 * registered in the registry. One extension normally defines
 * a category, and other reference it via its ID.
 * <p>
 * A category may specify its parent category in order to
 * achieve hierarchy.
 * </p><p>
 * [Issue: This interface is not exposed in API, but time may
 * demonstrate that it should be.  For the short term leave it be.
 * In the long term its use should be re-evaluated.]
 * </p>
 */
public interface ICategory {
	/**
	 * Name of the miscellaneous category
	 */
	public final static String MISC_NAME = "Other";//$NON-NLS-1$
	
	/**
	 * Identifier of the miscellaneous category
	 */
	public final static String MISC_ID = "org.eclipse.ui.internal.otherCategory";//$NON-NLS-1$
	
	/**
	 * Adds an element as being part of this category
	 */
	public void addElement(Object element);

	/**
	 * Returns the element that are known to be part
	 * of this category or <code>null</code>.
	 */
	public ArrayList getElements();
	
	/**
	 * Returns a unique category ID.
	 */
	public String getId();
	
	/**
	 * Returns a presentation label for this category.
	 */
	public String getLabel();
	
	/**
	 * Returns an array of category IDs that represent a path
	 * to the parent category of this category. If this is a
	 * top-level category, <code>null</code> is returned.
	 */
	public String[] getParentPath();
	
	/**
	 * Returns the category ID that represents the root
	 * of the parent category of this category. If this is a
	 * top-level category, <code>null</code> is returned.
	 */
	public String getRootPath();
}