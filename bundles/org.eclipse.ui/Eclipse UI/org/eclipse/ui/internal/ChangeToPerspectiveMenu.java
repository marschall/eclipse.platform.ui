package org.eclipse.ui.internal;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.actions.PerspectiveMenu;

/**
 * Change the perspective of the active page in the window
 * to the selected one.
 */
public class ChangeToPerspectiveMenu extends PerspectiveMenu {

	/**
	 * Constructor for ChangeToPerspectiveMenu.
	 * 
	 * @param window the workbench window this action applies to.
	 */
	public ChangeToPerspectiveMenu(IWorkbenchWindow window) {
		super(window, "ChangeToPerspectiveMenu"); //$NON-NLS-1$
		showActive(true);
	}

	/* (non-Javadoc)
	 * @see PerspectiveMenu#run(IPerspectiveDescriptor)
	 */
	protected void run(IPerspectiveDescriptor desc) {
		IWorkbenchPage page = getWindow().getActivePage();
		if (page != null)
			page.setPerspective(desc);
		else {
			try {
				IAdaptable input = WorkbenchPlugin.getPluginWorkspace().getRoot();
				getWindow().openPage(desc.getId(), input);
			} catch(WorkbenchException e) {
				MessageDialog.openError(
					getWindow().getShell(),
					WorkbenchMessages.getString("ChangeToPerspectiveMenu.errorTitle"), //$NON-NLS-1$,
					e.getMessage());
			}
		}
	}
}