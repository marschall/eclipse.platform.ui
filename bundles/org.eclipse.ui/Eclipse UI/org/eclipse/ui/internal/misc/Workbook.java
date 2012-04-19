package org.eclipse.ui.internal.misc;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;

public class Workbook {
	private TabFolder tabFolder;
	private TabItem selectedTab;
/**
 * Workbook constructor comment.
 */
public Workbook(Composite parent, int style) {
	tabFolder = new TabFolder(parent, style);

	tabFolder.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent event) {
			TabItem newSelectedTab = (TabItem) event.item;
			if (selectedTab == newSelectedTab) // Do nothing if the selection did not change.
				return;

			if (selectedTab != null && (!selectedTab.isDisposed())) {
				WorkbookPage selectedPage = getWorkbookPage(selectedTab);
				if (!selectedPage.deactivate()) {
					tabFolder.setSelection(new TabItem[] {selectedTab});
					return;
				}
			}

			selectedTab = newSelectedTab;
			WorkbookPage newSelectedPage = getWorkbookPage(newSelectedTab);
			newSelectedPage.activate();

		}
	});

}
public WorkbookPage getSelectedPage() {

	int index = tabFolder.getSelectionIndex();
	if (index == -1) // When can this be -1
		return null;

	TabItem selectedItem = tabFolder.getItem(index);

	return (WorkbookPage)selectedItem.getData();
}
public TabFolder getTabFolder() {

	return tabFolder;

}
protected WorkbookPage getWorkbookPage(TabItem item) {

	try {
		return (WorkbookPage) item.getData();
	} catch (ClassCastException e) {
		return null;
	}
}
public WorkbookPage[] getWorkbookPages() {

	TabItem[] tabItems = tabFolder.getItems();
	int nItems = tabItems.length;
	WorkbookPage[] workbookPages = new WorkbookPage[nItems];
	for (int i = 0; i < nItems; i++)
		workbookPages[i] = getWorkbookPage(tabItems[i]);
	return workbookPages;
}
public void setSelectedPage (WorkbookPage workbookPage)
{
	TabItem newSelectedTab = workbookPage.getTabItem();

	if (selectedTab == newSelectedTab)
		return;

	selectedTab = newSelectedTab;
	workbookPage.activate();
	tabFolder.setSelection(new TabItem[] {newSelectedTab});

}
}