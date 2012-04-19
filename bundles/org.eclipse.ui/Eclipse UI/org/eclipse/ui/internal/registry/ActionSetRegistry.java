package org.eclipse.ui.internal.registry;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.core.runtime.*;
import org.eclipse.ui.*;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.model.WorkbenchAdapter;
import java.util.*;

/**
 * The registry of action set extensions.
 */
public class ActionSetRegistry extends Object
{
	public static final String OTHER_CATEGORY = "org.eclipse.ui.actionSetCategory";//$NON-NLS-1$
	private ArrayList children = new ArrayList();
	private ArrayList categories = new ArrayList(1);

/**
 * Creates the action set registry.
 */
public ActionSetRegistry() {
	super();
	readFromRegistry();
}
/**
 * Adds an action set.
 */
public void addActionSet(ActionSetDescriptor desc) {
	children.add(desc);
}
/**
 * Finds and returns the registered action set with the given id.
 *
 * @param id the action set id 
 * @return the action set, or <code>null</code> if none
 * @see IActionSetDescriptor#getId
 */
public IActionSetDescriptor findActionSet(String id) {
	Iterator enum = children.iterator();
	while (enum.hasNext()) {
		IActionSetDescriptor desc = (IActionSetDescriptor)enum.next();
		if (desc.getId().equals(id))
			return desc;
	}
	return null;
}
/**
 * Find a category with a given id.
 */
public ActionSetCategory findCategory(String id) {
	Iterator enum = categories.iterator();
	while (enum.hasNext()) {
		ActionSetCategory cat = (ActionSetCategory) enum.next();
		if (id.equals(cat.getId()))
			return cat;
	}
	return null;
}
/**
 * Returns a list of the action sets known to the workbench.
 *
 * @return a list of action sets
 */
public IActionSetDescriptor[] getActionSets() {
	int count = children.size();
	IActionSetDescriptor [] array = new IActionSetDescriptor[count];
	for (int nX = 0; nX < count; nX ++) {
		array[nX] = (IActionSetDescriptor)children.get(nX);
	}
	return array;
}
/**
 * Returns a list of action set categories.
 *
 * @return a list of action sets categories
 */
public ActionSetCategory[] getCategories() {
	int count = categories.size();
	ActionSetCategory[] array = new ActionSetCategory[count];
	for (int i = 0; i < count; i++) {
		array[i] = (ActionSetCategory)categories.get(i);
	}
	return array;
}
/**
 * Adds each action set in the registry to a particular category.
 * For now, everything goes into the OTHER_CATEGORY.
 */
public void mapActionSetsToCategories() {
	// Create "other" category.
	ActionSetCategory cat = new ActionSetCategory(OTHER_CATEGORY,
		WorkbenchMessages.getString("ActionSetRegistry.otherCategory")); //$NON-NLS-1$
	categories.add(cat);

	// Add everything to it.
	Iterator enum = children.iterator();
	while (enum.hasNext()) {
		IActionSetDescriptor desc = (IActionSetDescriptor) enum.next();
		cat.addActionSet(desc);
	}
}
/**
 * Reads the registry.
 */
public void readFromRegistry() {
	ActionSetRegistryReader reader = new ActionSetRegistryReader();
	reader.readRegistry(Platform.getPluginRegistry(), this);
}
}