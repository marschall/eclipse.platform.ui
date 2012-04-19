package org.eclipse.jface.action;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.graphics.*;
import java.util.*;

/**
 * A tool bar manager is a contribution manager which realizes itself and its items
 * in a tool bar control.
 * <p>
 * This class may be instantiated; it may also be subclassed if a more
 * sophisticated layout is required.
 * </p>
 */
public class ToolBarManager extends ContributionManager implements IToolBarManager {

	/** 
	 * The tool bar items style; <code>SWT.NONE</code> by default.
	 */
	private int itemStyle = SWT.NONE;

	/** 
	 * The tool bat control; <code>null</code> before creation
	 * and after disposal.
	 */
	private ToolBar toolBar = null;	
/**
 * Creates a new tool bar manager with the default SWT button style.
 * Use the <code>createControl</code> method to create the 
 * tool bar control.
 */
public ToolBarManager() {
}
/**
 * Creates a tool bar manager with the given SWT button style.
 * Use the <code>createControl</code> method to create the 
 * tool bar control.
 *
 * @param style the tool bar item style
 * @see org.eclipse.swt.widgets.ToolBar#ToolBar for valid style bits
 */
public ToolBarManager(int style) {
	itemStyle= style;
}
/**
 * Creates a tool bar manager for an existing tool bar control.
 * This manager becomes responsible for the control, and will
 * dispose of it when the manager is disposed.
 *
 * @param toolbar the tool bar control
 */
public ToolBarManager(ToolBar toolbar) {
	this();
	this.toolBar = toolbar;
}
/**
 * Creates and returns this manager's tool bar control. 
 * Does not create a new control if one already exists.
 *
 * @param parent the parent control
 * @return the tool bar control
 */
public ToolBar createControl(Composite parent) {
	if (!toolBarExist() && parent != null) {
		toolBar = new ToolBar(parent, itemStyle);
		update(false);
	}
	return toolBar;
}
/**
 * Disposes of this tool bar manager and frees all allocated SWT resources.
 * Note that this method does not clean up references between this tool bar 
 * manager and its associated contribution items.
 * Use <code>removeAll</code> for that purpose.
 */
public void dispose() {
	if (toolBarExist())
		toolBar.dispose();
	toolBar = null;
}
/**
 * Returns the tool bar control for this manager.
 *
 * @return the tool bar control, or <code>null</code>
 *  if none (before creating or after disposal)
 */
public ToolBar getControl() {
	return toolBar;
}
/**
 * Re-lays out the tool bar.
 * <p>
 * The default implementation of this framework method re-lays out
 * the parent when the number of items crosses the zero threshold. 
 * Subclasses should override this method to implement their own 
 * re-layout strategy
 *
 * @param toolBar the tool bar control
 * @param oldCount the old number of items
 * @param newCount the new number of items
 */
protected void relayout(ToolBar toolBar, int oldCount, int newCount) {
	if ((oldCount == 0) != (newCount == 0))
		toolBar.getParent().layout();
}
/**
 * Returns whether the tool bar control is created
 * and not disposed.
 * 
 * @return <code>true</code> if the control is created
 *	and not disposed, <code>false</code> otherwise
 */
private boolean toolBarExist() {
	return toolBar != null && !toolBar.isDisposed(); 
}
/* (non-Javadoc)
 * Method declared on IContributionManager.
 */
public void update(boolean force) {

	long startTime= 0;
//	if (DEBUG) {
//		dumpStatistics();
//		startTime= (new Date()).getTime();
//	}	
				
	if (isDirty() || force) {
		
		if (toolBarExist()) {
		
			int oldCount= toolBar.getItemCount();

			// clean contains all active items without double separators
			IContributionItem[] items= getItems();
			ArrayList clean= new ArrayList(items.length);
			IContributionItem separator= null;
			long cleanStartTime= 0;
//			if (DEBUG) {
//				cleanStartTime= (new Date()).getTime(); 
//			}
			for (int i = 0; i < items.length; ++i) {
				IContributionItem ci= items[i];
				if (!ci.isVisible())
					continue;
				if (ci.isSeparator()) {
					// delay creation until necessary 
					// (handles both adjacent separators, and separator at end)
					separator= ci;
				} else {
					if (separator != null) {
						if (clean.size() > 0)	// no separator if first item
							clean.add(separator);
						separator= null;
					}
					clean.add(ci);
				}
			}
//			if (DEBUG) {
//				System.out.println("   Time needed to build clean vector: " + ((new Date()).getTime() - cleanStartTime));
//			}

			// determine obsolete items (removed or non active)
			Item[] mi= toolBar.getItems();
			ArrayList toRemove = new ArrayList(mi.length);
			for (int i= 0; i < mi.length; i++) {
				Object data= mi[i].getData();
				if (data == null || !clean.contains(data) ||
						(data instanceof IContributionItem && ((IContributionItem)data).isDynamic())) {
					toRemove.add(mi[i]);
				}
			}

			// Turn redraw off if the number of items to be added 
			// is above a certain threshold, to minimize flicker,
			// otherwise the toolbar can be seen to redraw after each item.
			// Do this before any modifications are made.
			// We assume each contribution item will contribute at least one toolbar item.
			boolean useRedraw = (clean.size() - (mi.length - toRemove.size())) >= 3;
			if (useRedraw) {
				toolBar.setRedraw(false);
			}

			// remove obsolete items
			for (int i = toRemove.size(); --i >= 0;) {
				ToolItem item = (ToolItem) toRemove.get(i);
				Control ctrl = item.getControl();
				if (ctrl != null) {
					item.setControl(null);
					ctrl.dispose();
				}
				item.dispose();
			}

			// add new items
			IContributionItem src, dest;
			mi= toolBar.getItems();
			int srcIx= 0;
			int destIx= 0;
			for (Iterator e = clean.iterator(); e.hasNext();) {
				src= (IContributionItem) e.next();
					
				// get corresponding item in SWT widget
				if (srcIx < mi.length)
					dest= (IContributionItem) mi[srcIx].getData();
				else
					dest= null;
					
				if (dest != null && src.equals(dest)) {
					srcIx++;
					destIx++;
					continue;
				}
				
				if (dest != null && dest.isSeparator() && src.isSeparator()) {
					mi[srcIx].setData(src);
					srcIx++;
					destIx++;
					continue;
				}						
																								
				int start= toolBar.getItemCount();
				src.fill(toolBar, destIx);
				int newItems= toolBar.getItemCount()-start;
				Item[] tis= toolBar.getItems();
				for (int i= 0; i < newItems; i++)
					tis[destIx+i].setData(src);
				destIx+= newItems;
			}

			setDirty(false);
			
			// turn redraw back on if we turned it off above
			if (useRedraw) {
				toolBar.setRedraw(true);
			}
			
			int newCount= toolBar.getItemCount();
			relayout(toolBar, oldCount, newCount);
		}

	}
	
//	if (DEBUG) {
//		System.out.println("   Time needed for update: " + ((new Date()).getTime() - startTime));
//		System.out.println();
//	}		
}
}