package org.eclipse.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.jface.resource.*;

/**
 * A perspective descriptor describes a perspective in an
 * <code>IPerspectiveRegistry</code>.  
 * <p>
 * A perspective is a template for view visibility, layout, and action visibility
 * within a workbench page. There are two types of perspective: a predefined 
 * perspective and a custom perspective.  
 * <ul>
 *   <li>A predefined perspective is defined by an extension to the workbench's 
 *     perspective extension point (<code>"org.eclipse.ui.perspectives"</code>).
 *     The extension defines a id, label, and <code>IPerspectiveFactory</code>.
 *     A perspective factory is used to define the initial layout for a page.
 *     </li>
 *   <li>A custom perspective is defined by the user.  In this case a predefined
 *     perspective is modified to suit a particular task and saved as a new
 *     perspective.  The attributes for the perspective are stored in a separate file 
 *     in the workbench's metadata directory.
 *     </li>
 * </ul>
 * </p>
 * <p>
 * Within a page the user can switch between any of the perspectives known
 * to the workbench's perspective registry, typically by selecting one from the
 * workbench's <code>Switch Perspective</code> menu. When selected, the views,
 * and actions within the active page rearrange to reflect the perspective.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @see IPerspectiveRegistry
 */
public interface IPerspectiveDescriptor {
/**
 * Returns this perspective's id. For perspectives declared via an extension,
 * this is the value of its <code>"id"</code> attribute.
 *
 * @return the perspective id
 */
public String getId();
/**
 * Returns the descriptor of the image for this perspective.
 *
 * @return the descriptor of the image to display next to this perspective
 */
public ImageDescriptor getImageDescriptor();
/**
 * Returns this perspective's label. For perspectives declared via an extension,
 * this is the value of its <code>"label"</code> attribute.
 *
 * @return the label
 */
public String getLabel();
}