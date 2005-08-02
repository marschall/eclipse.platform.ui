/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.ide.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;

/**
 * A quick menu actions provides support to assign short cuts
 * to sub menus.
 * 
 * @since 3.0
 */
public abstract class QuickMenuAction extends Action {

    private static final int CHAR_INDENT = 3;

    private Menu quickMenu;

    /**
     * Creates a new quick menu action with the given command id.
     * 
     * @param commandId the command id of the short cut used to open
     *  the sub menu 
     */
    public QuickMenuAction(String commandId) {
        setId(commandId);
        setActionDefinitionId(commandId);
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        Display display = Display.getCurrent();
        if (display == null)
            return;
        Control focus = display.getFocusControl();
        if (focus == null || focus.isDisposed())
            return;

        MenuManager menu = new MenuManager();
        fillMenu(menu);
        if (quickMenu != null) {
            quickMenu.dispose();
            quickMenu = null;
        }
        quickMenu = menu.createContextMenu(focus.getShell());
        Point location = computeMenuLocation(focus, quickMenu);
        if (location == null)
            return;
        quickMenu.setLocation(location);
        quickMenu.setVisible(true);
    }

    public void dispose() {
        if (quickMenu != null) {
            quickMenu.dispose();
            quickMenu = null;
        }
    }

    /**
     * Hook to fill a menu manager with the items of the sub menu.
     * 
     * @param menu the sub menu to fill
     */
    protected abstract void fillMenu(IMenuManager menu);

    /**
     * Returns the short cut assigned to the sub menu or <code>null</code> if
     * no short cut is assigned.
     * 
     * @return the short cut as a human readable string or <code>null</code>
     */
    public String getShortCutString() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		final IBindingService bindingService = (IBindingService) workbench
				.getAdapter(IBindingService.class);
		final TriggerSequence[] activeBindings = bindingService
				.getActiveBindingsFor(getActionDefinitionId());
		if (activeBindings.length > 0) {
			return activeBindings[0].format();
		}

		return null;
    }

    private Point computeMenuLocation(Control focus, Menu menu) {
        Point cursorLocation = focus.getDisplay().getCursorLocation();
        Rectangle clientArea = null;
        Point result = null;
        if (focus instanceof StyledText) {
            StyledText styledText = (StyledText) focus;
            clientArea = styledText.getClientArea();
            result = computeMenuLocation(styledText);
        } else if (focus instanceof Tree) {
            Tree tree = (Tree) focus;
            clientArea = tree.getClientArea();
            result = computeMenuLocation(tree);
        } else if (focus instanceof Table) {
            Table table = (Table) focus;
            clientArea = table.getClientArea();
            result = computeMenuLocation(table);
        }
        if (result == null) {
            result = focus.toControl(cursorLocation);
        }
        if (clientArea != null && !clientArea.contains(result)) {
            result = new Point(clientArea.x + clientArea.width / 2,
                    clientArea.y + clientArea.height / 2);
        }
        Rectangle shellArea = focus.getShell().getClientArea();
        if (!shellArea.contains(focus.getShell().toControl(
                focus.toDisplay(result)))) {
            result = new Point(shellArea.x + shellArea.width / 2, shellArea.y
                    + shellArea.height / 2);
        }
        return focus.toDisplay(result);
    }

    /**
     * Hook to compute the menu location if the focus widget is
     * a styled text widget.
     * 
     * @param text the styled text widget that has the focus
     * 
     * @return a widget relative position of the menu to pop up or
     *  <code>null</code> if now position inside the widget can
     *  be computed
     */
    protected Point computeMenuLocation(StyledText text) {
        Point result = text.getLocationAtOffset(text.getCaretOffset());
        result.y += text.getLineHeight();
        if (!text.getClientArea().contains(result))
            return null;
        return result;
    }

    /**
     * Hook to compute the menu location if the focus widget is
     * a tree widget.
     * 
     * @param tree the tree widget that has the focus
     * 
     * @return a widget relative position of the menu to pop up or
     *  <code>null</code> if now position inside the widget can
     *  be computed
     */
    protected Point computeMenuLocation(Tree tree) {
        TreeItem[] items = tree.getSelection();
        Rectangle clientArea = tree.getClientArea();
        switch (items.length) {
        case 0:
            return null;
        case 1:
            Rectangle bounds = items[0].getBounds();
            Rectangle intersect = clientArea.intersection(bounds);
            if (intersect != null && intersect.height == bounds.height) {
                return new Point(Math.max(0, bounds.x
                        + getAvarageCharWith(tree) * CHAR_INDENT), bounds.y
                        + bounds.height);
            } else {
                return null;
            }
        default:
            Rectangle[] rectangles = new Rectangle[items.length];
            for (int i = 0; i < rectangles.length; i++) {
                rectangles[i] = items[i].getBounds();
            }
            Point cursorLocation = tree.getDisplay().getCursorLocation();
            Point result = findBestLocation(getIncludedPositions(rectangles,
                    clientArea), tree.toControl(cursorLocation));
            if (result != null)
                result.x = result.x + getAvarageCharWith(tree) * CHAR_INDENT;
            return result;
        }
    }

    /**
     * Hook to compute the menu location if the focus widget is
     * a table widget.
     * 
     * @param table the table widget that has the focus
     * 
     * @return a widget relative position of the menu to pop up or
     *  <code>null</code> if now position inside the widget can
     *  be computed
     */
    protected Point computeMenuLocation(Table table) {
        TableItem[] items = table.getSelection();
        Rectangle clientArea = table.getClientArea();
        switch (items.length) {
        case 0: {
            return null;
        }
        case 1: {
            Rectangle bounds = items[0].getBounds(0);
            Rectangle iBounds = items[0].getImageBounds(0);
            Rectangle intersect = clientArea.intersection(bounds);
            if (intersect != null && intersect.height == bounds.height) {
                return new Point(Math.max(0, bounds.x + iBounds.width
                        + getAvarageCharWith(table) * CHAR_INDENT), bounds.y
                        + bounds.height);
            } else {
                return null;
            }
        }
        default: {
            Rectangle[] rectangles = new Rectangle[items.length];
            for (int i = 0; i < rectangles.length; i++) {
                rectangles[i] = items[i].getBounds(0);
            }
            Rectangle iBounds = items[0].getImageBounds(0);
            Point cursorLocation = table.getDisplay().getCursorLocation();
            Point result = findBestLocation(getIncludedPositions(rectangles,
                    clientArea), table.toControl(cursorLocation));
            if (result != null)
                result.x = result.x + iBounds.width + getAvarageCharWith(table)
                        * CHAR_INDENT;
            return result;
        }
        }
    }

    private Point[] getIncludedPositions(Rectangle[] rectangles,
            Rectangle widgetBounds) {
        List result = new ArrayList();
        for (int i = 0; i < rectangles.length; i++) {
            Rectangle rectangle = rectangles[i];
            Rectangle intersect = widgetBounds.intersection(rectangle);
            if (intersect != null && intersect.height == rectangle.height) {
                result.add(new Point(intersect.x, intersect.y
                        + intersect.height));
            }
        }
        return (Point[]) result.toArray(new Point[result.size()]);
    }

    private Point findBestLocation(Point[] points, Point relativeCursor) {
        Point result = null;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < points.length; i++) {
            Point point = points[i];
            int a = 0;
            int b = 0;
            if (point.x > relativeCursor.x) {
                a = point.x - relativeCursor.x;
            } else {
                a = relativeCursor.x - point.x;
            }
            if (point.y > relativeCursor.y) {
                b = point.y - relativeCursor.y;
            } else {
                b = relativeCursor.y - point.y;
            }
            double dist = Math.sqrt(a * a + b * b);
            if (dist < bestDist) {
                result = point;
                bestDist = dist;
            }
        }
        return result;
    }

    private int getAvarageCharWith(Control control) {
        GC gc = null;
        try {
            gc = new GC(control);
            return gc.getFontMetrics().getAverageCharWidth();
        } finally {
            if (gc != null)
                gc.dispose();
        }
    }
}
