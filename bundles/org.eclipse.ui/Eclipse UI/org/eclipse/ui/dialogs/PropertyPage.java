package org.eclipse.ui.dialogs;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;

/**
 * Abstract base implementation of a workbench property page
 * (<code>IWorkbenchPropertyPage</code>).
 * The implementation is a JFace preference page with an adapatable element.
 * <p>
 * Subclasses must implement the <code>createContents</code> framework
 * method to supply the property page's main control.
 * </p>
 * <p>
 * Subclasses should extend the <code>doComputeSize</code> framework
 * method to compute the size of the page's control.
 * </p>
 * <p>
 * Subclasses may override the <code>performOk</code>, <code>performApply</code>, 
 * <code>performDefaults</code>, <code>performCancel</code>, and <code>performHelp</code>
 * framework methods to react to the standard button events.
 * </p>
 * <p>
 * Subclasses may call the <code>noDefaultAndApplyButton</code> framework
 * method before the page's control has been created to suppress
 * the standard Apply and Defaults buttons.
 * </p>
 *
 * @see IWorkbenchPropertyPage
 */
public abstract class PropertyPage extends PreferencePage implements IWorkbenchPropertyPage {

	/**
	 * The element.
	 */
	private IAdaptable element;
/**
 * Creates a new property page.
 */
public PropertyPage() {}
/* (non-Javadoc)
 * Method declared on IWorkbenchPropertyPage.
 */
public IAdaptable getElement() {
	return element;
}
/**
 * Sets the element that owns properties shown on this page.
 *
 * @param element the element
 */
public void setElement(IAdaptable element) {
	this.element = element;
}
}