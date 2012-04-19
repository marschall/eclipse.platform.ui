package org.eclipse.ui.views.tasklist;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Maintains a model of all known marker types.
 */ 
/* package */ class MarkerTypesModel {
	/**
	 * Maps from marker type id to MarkerType.
	 */
	private HashMap types;
	
/**
 * Creates a new marker types model.
 */
public MarkerTypesModel() {
	types = readTypes();
}
/**
 * Returns the marker type with the given id, or <code>null</code> if there is no such marker type.
 */
public MarkerType getType(String id) {
	return (MarkerType) types.get(id);
}
/**
 * Returns all known marker types.
 */
public MarkerType[] getTypes() {
	MarkerType[] result = new MarkerType[types.size()];
	types.values().toArray(result);
	return result;
}
/**
 * Returns the label for the given marker type.
 * Workaround until we have labels in XML.
 */
String getWellKnownLabel(String type) {
	if (type.equals(IMarker.PROBLEM))
		return "Problem";//$NON-NLS-1$
	if (type.equals(IMarker.TASK))
		return "Task";//$NON-NLS-1$
	if (type.equals("org.eclipse.jdt.core.problem"))//$NON-NLS-1$
		return "Java Problem";//$NON-NLS-1$
	return type;
}
/**
 * Reads the marker types from the registry.
 */
HashMap readTypes() {
	HashMap types = new HashMap();
	IExtensionPoint point = Platform.getPluginRegistry().getExtensionPoint(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_MARKERS);
	if (point != null) {
		// Gather all registered marker types.
		IExtension[] extensions = point.getExtensions();
		for (int i = 0; i < extensions.length; ++i) {
			IExtension ext = extensions[i];
			String id = ext.getUniqueIdentifier();
			String label = ext.getLabel();
			if (label.equals("")) {//$NON-NLS-1$
				label = getWellKnownLabel(id);
			}
			ArrayList supersList = new ArrayList();
			IConfigurationElement[] configElements = ext.getConfigurationElements();
			for (int j = 0; j < configElements.length; ++j) {
				IConfigurationElement elt = configElements[j];
				if (elt.getName().equalsIgnoreCase("super")) {//$NON-NLS-1$
					String sup = elt.getAttribute("type");//$NON-NLS-1$
					if (sup != null) {
						supersList.add(sup);
					}
				}
			}
			String[] superTypes = new String[supersList.size()];
			supersList.toArray(superTypes);
			MarkerType type = new MarkerType(this, id, label, superTypes);
			types.put(id, type);
		}
	}
	return types;
}
}