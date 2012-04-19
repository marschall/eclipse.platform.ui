package org.eclipse.jface.action;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.jface.util.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

/**
 * An abstract contribution item implementation for adding an arbitrary 
 * SWT control to a tool bar. 
 * Note, however, that these items cannot be contributed to menu bars.
 * <p>
 * The <code>createControl</code> framework method must be implemented
 * by concrete subclasses.
 * </p>
 */
public abstract class ControlContribution extends ContributionItem {
/**
 * Creates a control contribution item with the given id.
 *
 * @param id the contribution item id
 */
protected ControlContribution(String id) {
	super(id);
}
/**
 * Computes the width of the given control which is being added
 * to a tool bar.  This is needed to determine the width of the tool bar item
 * containing the given control.
 * <p>
 * The default implementation of this framework method returns 
 * <code>control.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x</code>.
 * Subclasses may override if required.
 * </p>
 *
 * @param control the control being added
 * @return the width of the control
 */
protected int computeWidth(Control control) {
	return control.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x;
}
/**
 * Creates and returns the control for this contribution item
 * under the given parent composite.
 * <p>
 * This framework method must be implemented by concrete
 * subclasses.
 * </p>
 *
 * @param parent the parent composite
 * @return the new control
 */
protected abstract Control createControl(Composite parent);
/**
 * The control item implementation of this <code>IContributionItem</code>
 * method calls the <code>createControl</code> framework method.
 * Subclasses must implement <code>createControl</code> rather than
 * overriding this method.
 */
public final void fill(Composite parent) {
	createControl(parent);
}
/**
 * The control item implementation of this <code>IContributionItem</code>
 * method throws an exception since controls cannot be added to menus.
 */
public final void fill(Menu parent, int index) {
	Assert.isTrue(false, "Can't add a control to a menu");//$NON-NLS-1$
}
/**
 * The control item implementation of this <code>IContributionItem</code>
 * method calls the <code>createControl</code> framework method to
 * create a control under the given parent, and then creates
 * a new tool item to hold it.
 * Subclasses must implement <code>createControl</code> rather than
 * overriding this method.
 */
public final void fill(ToolBar parent, int index) {
	Control control = createControl(parent);
	ToolItem ti = new ToolItem(parent, SWT.SEPARATOR, index);
	ti.setControl(control);	
	ti.setWidth(computeWidth(control));
}
}