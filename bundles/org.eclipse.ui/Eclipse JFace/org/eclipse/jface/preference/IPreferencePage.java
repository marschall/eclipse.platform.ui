package org.eclipse.jface.preference;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.*;

/**
 * An interface for a preference page. This interface
 * is used primarily by the page's container 
 */
public interface IPreferencePage extends IDialogPage {
	
	
/**
 * Computes a size for this page's UI component. 
 *
 * @return the size of the preference page encoded as
 *   <code>new Point(width,height)</code>, or 
 *   <code>(0,0)</code> if the page doesn't currently have any UI component
 */
public Point computeSize();
/**
 * Returns whether this dialog page is in a valid state.
 * 
 * @return <code>true</code> if the page is in a valid state,
 *   and <code>false</code> if invalid
 */
public boolean isValid();
/**
 * Checks whether it is alright to leave this page.
 * 
 * @return <code>false</code> to abort page flipping and the
 *  have the current page remain visible, and <code>true</code>
 *  to allow the page flip
 */
public boolean okToLeave();
/**
 * Notifies that the container of this preference page has been canceled.
 * 
 * @return <code>false</code> to abort the container's cancel 
 *  procedure and <code>true</code> to allow the cancel to happen
 */
public boolean performCancel();
/**
 * Notifies that the OK button of this page's container has been pressed.
 * 
 * @return <code>false</code> to abort the container's OK
 *  processing and <code>true</code> to allow the OK to happen
 */
public boolean performOk();
/**
 * Sets or clears the container of this page.
 *
 * @param preferencePageContainer the preference page container, or <code>null</code> 
 */
public void setContainer(IPreferencePageContainer preferencePageContainer);
/**
 * Sets the size of this page's UI component.
 *
 * @param size the size of the preference page encoded as
 *   <code>new Point(width,height)</code>
 */
public void setSize(Point size);
}