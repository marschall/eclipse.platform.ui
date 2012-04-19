package org.eclipse.jface.action;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

/**
 * An abstract base implementation for contribution items.
 */
public abstract class ContributionItem implements IContributionItem {

	/**
	 * The identifier for this contribution item, of <code>null</code> if none.
	 */
	private String id = null;
	
	/**
	 * Indicates this item is allowed to be enabled in its manager;
	 * <code>true</code> by default.
	 */
	private boolean enabledAllowed = true;

	/**
	 * Indicates this item is visible in its manager; <code>true</code> 
	 * by default.
	 */
	private boolean visible = true;
/**
 * Creates a contribution item with a <code>null</code> id.
 * Calls <code>this(String)</code> with <code>null</code>.
 */
protected ContributionItem() {
	this(null);
}
/**
 * Creates a contribution item with the given (optional) id.
 * The given id is used to find items in a contribution manager,
 * and for positioning items relative to other items.
 *
 * @param id the contribution item identifier, or <code>null</code>
 */
protected ContributionItem(String id) {
	this.id = id;
}
/**
 * The default implementation of this <code>IContributionItem</code>
 * method does nothing. Subclasses may override.
 */
public void fill(Composite parent) {
}
/**
 * The default implementation of this <code>IContributionItem</code>
 * method does nothing. Subclasses may override.
 */
public void fill(Menu menu, int index) {
}
/**
 * The default implementation of this <code>IContributionItem</code>
 * method does nothing. Subclasses may override.
 */
public void fill(ToolBar parent, int index) {
}
/* (non-Javadoc)
 * Method declared on IContributionItem.
 */
public String getId() {
	return id;
}
/**
 * The default implementation of this <code>IContributionItem</code>
 * method returns <code>false</code>. Subclasses may override.
 */
public boolean isDynamic() {
	return false;
}
/**
 * The default implementation of this <code>IContributionItem</code>
 * method returns <code>false</code>. Subclasses may override.
 */
public boolean isGroupMarker() {
	return false;
}
/**
 * The default implementation of this <code>IContributionItem</code>
 * method returns <code>false</code>. Subclasses may override.
 */
public boolean isSeparator() {
	return false;
}
/**
 * The default implementation of this <code>IContributionItem</code>
 * method returns the value recorded in an internal state variable,
 * which is <code>true</code> by default. <code>setEnableAllowed</code>
 * should be used to change this setting.
 */
public boolean isEnabledAllowed() {
	return enabledAllowed;
}
/**
 * The default implementation of this <code>IContributionItem</code>
 * method stores the value in an internal state variable,
 * which is <code>true</code> by default.
 */
public void setEnabledAllowed(boolean enabledAllowed) {
	this.enabledAllowed = enabledAllowed;
}
/**
 * The default implementation of this <code>IContributionItem</code>
 * method returns the value recorded in an internal state variable,
 * which is <code>true</code> by default. <code>setVisible</code>
 * should be used to change this setting.
 */
public boolean isVisible() {
	return visible;
}
/**
 * The default implementation of this <code>IContributionItem</code>
 * method stores the value in an internal state variable,
 * which is <code>true</code> by default.
 */
public void setVisible(boolean visible) {
	this.visible = visible;
}
/**
 * Returns a string representation of this contribution item 
 * suitable only for debugging.
 */
public String toString() {
	return getClass().getName() + "(id=" + getId() + ")";//$NON-NLS-2$//$NON-NLS-1$
}
/**
 * The default implementation of this <code>IContributionItem</code>
 * method does nothing. Subclasses may override.
 */
public void update() {
}
}