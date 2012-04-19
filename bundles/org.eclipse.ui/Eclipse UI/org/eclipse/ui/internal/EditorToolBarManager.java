package org.eclipse.ui.internal;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.jface.action.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.RetargetAction;

import java.util.*;
import java.util.List;

/**
 * An <code>EditorToolBarManager</code> is used to sort the contributions
 * made by an editor so that they always appear after the action sets.  
 */
public class EditorToolBarManager extends SubToolBarManager 
{
	private IToolBarManager parentMgr;
	private boolean enabledAllowed = true;
/**
 * Constructs a new manager.
 *
 * @param mgr the parent manager.  All contributions made to the
 *      <code>EditorToolBarManager</code> are forwarded and appear in the
 *      parent manager.
 */
public EditorToolBarManager(IToolBarManager mgr) {
	super(mgr);
	parentMgr = mgr;
}
/* (non-Javadoc)
 * Method declared on IContributionManager.
 */
public IContributionItem[] getItems() {
	return parentMgr.getItems();
}

/**
 * Return the toolbar into which this manager will
 * contribute to.
 */
private ToolBar getToolBar() {
	if (parentMgr == null)
		return null;
		
	IContributionManager mgr = parentMgr;
	while (mgr instanceof SubToolBarManager)
		mgr = ((SubToolBarManager)mgr).getParent();
		
	if (mgr instanceof ToolBarManager)
		return ((ToolBarManager)mgr).getControl();

	return null;
}
/* (non-Javadoc)
 * Method declared on IContributionManager.
 */
/**
 * Inserts the new item after any action set contributions which may
 * exist within the toolbar to ensure a consistent order for actions.
 */
public void insertAfter(String id, IContributionItem item) {
	IContributionItem refItem = PluginActionSetBuilder.findInsertionPoint(id,
		null, parentMgr, false);
	if (refItem != null) {
		super.insertAfter(refItem.getId(), item);
	} else {
		WorkbenchPlugin.log("Reference action not found: " + id); //$NON-NLS-1$
	}
}
/* (non-Javadoc)
 * Method declared on IContributionManager.
 */
/**
 * Inserts the new item after any action set contributions which may
 * exist within the toolbar to ensure a consistent order for actions.
 */
public void prependToGroup(String groupName, IContributionItem item) {
	insertAfter(groupName, item);
}
/**
 * Sets the visibility of the manager. If the visibility is <code>true</code>
 * then each item within the manager appears within the parent manager.
 * Otherwise, the items are not visible if force visibility is
 * <code>true</code>, or grayed out if force visibility is <code>false</code>
 * <p>
 * This is a workaround for the layout flashing when editors contribute
 * large amounts of items.</p>
 *
 * @param visible the new visibility
 * @param forceVisibility whether to change the visibility or just the
 * 		enablement state. This parameter is ignored if visible is 
 * 		<code>true</code>.
 */
public void setVisible(boolean visible, boolean forceVisibility) {
	if (visible) {
		// Make the items visible 
		if (!enabledAllowed) 
			setEnabledAllowed(true);
		if (!isVisible())
			setVisible(true);
	}
	else {
		if (forceVisibility)
			// Remove the editor tool bar items
			setVisible(false);
		else
			// Disabled the tool bar items.
			setEnabledAllowed(false);
	}
}

/**
 * Sets the enablement ability of all the items contributed by the editor.
 */
private void setEnabledAllowed(boolean enabledAllowed) {
	this.enabledAllowed = enabledAllowed;
	IContributionItem[] items = super.getItems();
	for (int i = 0; i < items.length; i++) {
		IContributionItem item = items[i];
		if (!(item instanceof ActionContributionItem &&
			((ActionContributionItem)item).getAction() instanceof RetargetAction)) {
			// skip retarget actions, they stay enabled if they have a handler	
			item.setEnabledAllowed(enabledAllowed);
		}
	}
}
}