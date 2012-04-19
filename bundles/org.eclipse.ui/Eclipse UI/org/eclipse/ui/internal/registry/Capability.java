package org.eclipse.ui.internal.registry;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.internal.WorkbenchImages;
import org.eclipse.ui.internal.model.WorkbenchAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A capability is the user interface aspect of a project's nature. There is
 * a 1-1 relationship between a capability and a nature. It is possible for
 * a nature to not have a capability, but the opposite is not true - that is,
 * a capability must represent a nature.
 * <p>
 * A capability can take control of the user interface of other capabilities. It
 * is then responsible for collecting the necessary information and adding the
 * natures represented by these capabilites.
 * </p>
 */
public class Capability extends WorkbenchAdapter implements IAdaptable {
	private static final String ATT_ID = "id";
	private static final String ATT_NAME = "name";
	private static final String ATT_ICON = "icon";
	private static final String ATT_NATURE_ID = "natureId";
	private static final String ATT_CATEGORY = "category";
	private static final String ATT_INSTALL_WIZARD = "installWizard";
	private static final String ATT_DESCRIPTION = "description";
	
	private String id;
	private String natureId;
	private String name;
	private ImageDescriptor icon;
	private IConfigurationElement element;
	private ArrayList handleUIs;
	
	/**
	 * Creates an instance of <code>Capability</code> using the
	 * information provided by the configuration element.
	 * 
	 * @param configElement the <code>IConfigurationElement<code> containing
	 * 		the attributes
	 * @param reader the <code>CapabilityRegistryReader<code> used to log missing attributes
	 * @throws a <code>WorkbenchException</code> if the ID or label is <code>null</code
	 */
	public Capability(IConfigurationElement configElement, CapabilityRegistryReader reader)
		throws WorkbenchException
	{
		boolean missingAttribute = false;
		String attr_id = configElement.getAttribute(ATT_ID);
		String attr_name = configElement.getAttribute(ATT_NAME);
		String attr_nature = configElement.getAttribute(ATT_NATURE_ID);
			
		if (attr_id == null) {
			reader.logMissingAttribute(configElement, ATT_ID);
			missingAttribute = true;
		}
		if (attr_name == null) {
			reader.logMissingAttribute(configElement, ATT_NAME);
			missingAttribute = true;
		}
		if (attr_nature == null) {
			reader.logMissingAttribute(configElement, ATT_NATURE_ID);
			missingAttribute = true;
		}
		if (configElement.getAttribute(ATT_INSTALL_WIZARD) == null) {
			reader.logMissingAttribute(configElement, ATT_INSTALL_WIZARD);
			missingAttribute = true;
		}
		
		if (missingAttribute)
			throw new WorkbenchException("Missing required category attribute"); //$NON-NLS-1$

		id = attr_id;
		name = attr_name;
		natureId = attr_nature;
		element = configElement;
	}
	
	/**
	 * Adds the id of a capability for which this capability handles
	 * the user interface.
	 */
	public void addHandleUI(String capabilityId) {
		if (handleUIs == null)
			handleUIs = new ArrayList(4);
		handleUIs.add(capabilityId);	
	}
	
	public String getId() {
		return id;
	}
	
	/* (non-Javadoc)
	 * Method declared on IWorkbenchAdapter.
	 */
	public String getLabel(Object o) {
		return name;
	}
	
	public String getName() {
		return name;
	}
	
	public ImageDescriptor getIconDescriptor() {
		if (icon == null) {
			IExtension extension = element.getDeclaringExtension();
			String location = element.getAttribute(ATT_ICON);
			icon = WorkbenchImages.getImageDescriptorFromExtension(extension, location);
		}
		return icon;
	}
	
	public String getNatureId() {
		return natureId;
	}
	
	/* (non-Javadoc)
	 * Method declared on IAdaptable.
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) 
			return this;
		else
			return null;
	}
	
	public String getCategoryPath() {
		return element.getAttribute(ATT_CATEGORY);
	}
	
	public IWizard getInstallWizard() {
		try {
			return (IWizard)element.createExecutableExtension(ATT_INSTALL_WIZARD);
		} catch (CoreException e) {
			return null;
		}
	}
	
	public String getDescription() {
		return element.getAttribute(ATT_DESCRIPTION);
	}
	
	/**
	 * Returns a list of ids of other capabilities for which this 
	 * capability handles the user interface, or <code>null</code>
	 * if not applicable.
	 */
	public ArrayList getHandleUIs() {
		return handleUIs;	
	}
}