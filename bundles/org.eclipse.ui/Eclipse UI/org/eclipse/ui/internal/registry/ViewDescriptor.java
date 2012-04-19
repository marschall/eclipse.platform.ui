package org.eclipse.ui.internal.registry;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.internal.*;
import org.eclipse.ui.*;
import org.eclipse.jface.resource.ImageDescriptor;
import java.util.*;

/**
 * Capture the attributes of a view extension.
 */
public class ViewDescriptor implements IViewDescriptor {
	private String id;
	private ImageDescriptor imageDescriptor;
	private static final String ATT_ID="id";//$NON-NLS-1$
	private static final String ATT_NAME="name";//$NON-NLS-1$
	private static final String ATT_ACCELERATOR="accelerator";//$NON-NLS-1$
	private static final String ATT_ICON="icon";//$NON-NLS-1$
	private static final String ATT_CATEGORY="category";//$NON-NLS-1$
	private static final String ATT_CLASS="class";//$NON-NLS-1$
	private String label;
	private String accelerator;
	private String className;
	private IConfigurationElement configElement;
	private String [] categoryPath;
/**
 * Create a new ViewDescriptor for an extension.
 */
public ViewDescriptor(IConfigurationElement e) throws CoreException {
	configElement = e;
	loadFromExtension();
}
/**
 * Return an instance of the declared view.
 */
public IViewPart createView() throws CoreException
{
	Object obj = WorkbenchPlugin.createExtension(configElement, ATT_CLASS);
	return (IViewPart) obj;
}
/**
 * Returns tokens for the category path or null if not defined.
 */
public String[] getCategoryPath() {
	return categoryPath;
}
	public IConfigurationElement getConfigurationElement() {
		return configElement;
	}
	public String getID() {
		return id;
	}
public ImageDescriptor getImageDescriptor() {
	if (imageDescriptor != null)
		return imageDescriptor;
	String iconName = configElement.getAttribute(ATT_ICON);
	if (iconName == null)
		return null;
	imageDescriptor = 
		WorkbenchImages.getImageDescriptorFromExtension(
			configElement.getDeclaringExtension(), 
			iconName); 
	return imageDescriptor;
}

public String getLabel() {
	return label;
}

public String getAccelerator() {
	return accelerator;
}

/**
 * load a view descriptor from the registry.
 */
private void loadFromExtension() throws CoreException {
	id = configElement.getAttribute(ATT_ID);
	label = configElement.getAttribute(ATT_NAME);
	accelerator = configElement.getAttribute(ATT_ACCELERATOR);
	className = configElement.getAttribute(ATT_CLASS);
	String category = configElement.getAttribute(ATT_CATEGORY);

	// Sanity check.
	if ((label == null) || (className == null)) {
		throw new CoreException(
			new Status(
				IStatus.ERROR, 
				configElement.getDeclaringExtension().getDeclaringPluginDescriptor().getUniqueIdentifier(), 
				0, 
				"Invalid extension (missing label or class name): " + id, //$NON-NLS-1$
				null)); 
	}
	if (category != null) {
		StringTokenizer stok = new StringTokenizer(category, "/");//$NON-NLS-1$
		categoryPath = new String[stok.countTokens()];
		// Parse the path tokens and store them
		for (int i = 0; stok.hasMoreTokens(); i++) {
			categoryPath[i] = stok.nextToken();
		}
	}
}
/**
 * Returns a string representation of this descriptor.  For
 * debugging purposes only.
 */
public String toString() {
	return "View(" + getID() + ")";//$NON-NLS-2$//$NON-NLS-1$
}
}