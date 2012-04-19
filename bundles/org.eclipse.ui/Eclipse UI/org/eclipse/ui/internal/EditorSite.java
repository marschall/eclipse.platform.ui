package org.eclipse.ui.internal;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.ui.*;
import org.eclipse.ui.internal.registry.EditorDescriptor;

/**
 * An editor container manages the services for an editor.
 */
public class EditorSite extends PartSite
	implements IEditorSite
{
	private EditorDescriptor desc;
	private boolean reuseEditor = true;
	KeyBindingService keyBindingService;
	
/**
 * Constructs an EditorSite for an editor.  The resource editor descriptor
 * may be omitted for an OLE editor.
 */
public EditorSite(IEditorPart editor, WorkbenchPage page, 
	EditorDescriptor desc) 
{
	super(editor, page);
	if (desc != null) {
		this.desc = desc;
		setConfigurationElement(desc.getConfigurationElement());
	}
}
/**
 * Returns the editor action bar contributor for this editor.
 * <p>
 * An action contributor is responsable for the creation of actions.
 * By design, this contributor is used for one or more editors of the same type.
 * Thus, the contributor returned by this method is not owned completely
 * by the editor.  It is shared.
 * </p>
 *
 * @return the editor action bar contributor
 */
public IEditorActionBarContributor getActionBarContributor() {
	EditorActionBars bars = (EditorActionBars)getActionBars();
	if (bars != null)
		return bars.getEditorContributor();
	else
		return null;
}
/**
 * Returns the editor
 */
public IEditorPart getEditorPart() {
	return (IEditorPart)getPart();
}

public EditorDescriptor getEditorDescriptor() {
	return desc;
}

/* (non-Javadoc)
 * Method declared on IEditorSite.
 */
public IKeyBindingService getKeyBindingService() {
	if(keyBindingService == null) {
		keyBindingService = new KeyBindingService(((WorkbenchWindow)getWorkbenchWindow()).getKeyBindingService());
		keyBindingService.setActiveAcceleratorScopeId("org.eclipse.ui.textEditorScope");
	}	
	return keyBindingService;
}

public boolean getReuseEditor() {
	return reuseEditor;
}
	
public void setReuseEditor(boolean reuse) {
	reuseEditor = reuse;
}
}