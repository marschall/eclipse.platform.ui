package org.eclipse.ui.internal.misc;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.registry.Capability;
import org.eclipse.ui.internal.registry.CapabilityRegistry;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * A group of controls used to view and modify the
 * set of capabilities on a project.
 */
public class ProjectCapabilitySelectionGroup {
	// Initial number of visible capabilities in the list.
	private static final int PROJECT_LIST_MULTIPLIER = 30;

	private CapabilityRegistry registry;
	private Capability[] initialCapabilities;
	private boolean modified = false;
	private CheckboxTableViewer listViewer;
	private ICheckStateListener checkStateListener;

	// For a given capability as key, the value will be a list of
	// other capabilities that require the capability. Also,
	// it may include the capability key if it was selected by the
	// user before being required by other capabilities.
	private HashMap dependents = new HashMap();
	
	// Contains the features that the user has selected to
	// be added and deleted. The features are in prerequisite
	// order already. The structure is a collection of
	// prerequisite levels. Each level is a collection of features.
	private ArrayList featuresToAdd;
	private ArrayList featuresToRemove;
	
	
	/**
	 * Creates a new instance of the <code>ProjectCapabilitySelectionGroup</code>
	 * 
	 * @param capabilities the intial collection of capabilities to select
	 * @param registry all available capabilities registered by plug-ins
	 */
	public ProjectCapabilitySelectionGroup(Capability[] capabilities, CapabilityRegistry registry) {
		super();
		this.initialCapabilities = capabilities;
		this.registry = registry;
	}
	
	/**
	 * Create the contents of this group. The basic layout is a checkbox
	 * list with a text field at the bottom to display the capability
	 * description.
	 */
	public Control createContents(Composite parent) {
		// Create the main composite for the other controls
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Add a label to identify the checkbox tree viewer of capabilities
		Label capabilityLabel = new Label(composite, SWT.LEFT);
		capabilityLabel.setText(WorkbenchMessages.getString("ProjectCapabilitySelectionGroup.available")); //$NON-NLS-1$
		GridData data = new GridData();
		data.verticalAlignment = SWT.TOP;
		capabilityLabel.setLayoutData(data);
		
		// Checkbox tree viewer of all available capabilities
		listViewer = CheckboxTableViewer.newCheckList(composite, SWT.TOP | SWT.BORDER);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.heightHint =
			getDefaultFontHeight(listViewer.getTable(), PROJECT_LIST_MULTIPLIER);
		listViewer.getTable().setLayoutData(data);
		listViewer.setLabelProvider(new WorkbenchLabelProvider());
		listViewer.setContentProvider(new WorkbenchContentProvider());
		listViewer.setInput(registry);
		listViewer.setCheckedElements(initialCapabilities);

		// Add a label to identify the text field of capability's description
		Label descLabel = new Label(composite, SWT.LEFT);
		descLabel.setText(WorkbenchMessages.getString("ProjectCapabilitySelectionGroup.description")); //$NON-NLS-1$
		data = new GridData();
		data.verticalAlignment = SWT.TOP;
		descLabel.setLayoutData(data);
		
		// Text field to display the capability's description
		final Text descText = new Text(composite, SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		descText.setText("\n\n\n");
		descText.setEditable(false);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		descText.setLayoutData(data);
		
		// Listen for selection changes to update the description field
		listViewer.addSelectionChangedListener(new ISelectionChangedListener () {
			public void selectionChanged(SelectionChangedEvent event) {
				if (event.getSelection() instanceof IStructuredSelection) {
					IStructuredSelection sel = (IStructuredSelection)event.getSelection();
					Capability cap = (Capability)sel.getFirstElement();
					if (cap != null)
						descText.setText(cap.getDescription());
				}
			}
		});
		
		// Properly handle user checking and unchecking project features
		listViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getChecked())
					handleCapabilityChecked((Capability)event.getElement());
				else
					handleCapabilityUnchecked((Capability)event.getElement());
			}
		});

		populateDependents();
		
		return composite;
	}
	
	/**
	 * Return <code>true</code> if there is a current selection
	 * of capabilities.
	 */
	public boolean hasCapabilitiesSelected() {
		if (listViewer == null)
			return false;
			
		TableItem[] children = listViewer.getTable().getItems();
		for (int i = 0; i < children.length; i++) {
			if (children[i].getChecked())
				return true;
		}
		
		return false;
	}
	
	/**
	 * Get the default widget height for the supplied control.
	 * 
	 * @param control  the control being queried about fonts.
	 * @param lines  the number of lines to be shown on the table.
	 * @return int  the font height required to show the requested lines.
	 */
	private int getDefaultFontHeight(Control control, int lines) {
		FontData[] viewerFontData = control.getFont().getFontData();
		int fontHeight = 10;
	
		//If we have no font data use our guess
		if (viewerFontData.length > 0)
			fontHeight = viewerFontData[0].getHeight();
		return lines * fontHeight;
	}
	
	/**
	 * Return <code>true</code> if the user may have made changes
	 * to the capabilities of the project. Otherwise <code>false</code>
	 * if no changes were made.
	 * 
	 * @return <code>true</true> when possible changes may have been made,
	 *    <code>false</code> otherwise
	 */
	public boolean getCapabilitiesModified() {
		return modified;
	}

	/**
	 * The user has changed the project capability selection.
	 * Set the modified flag and clear the caches.
	 */
	private void capabilitiesModified() {
		modified = true;
		featuresToAdd = null;
		featuresToRemove = null;
	}
	
	/**
	 * Add a dependency between the target and dependent
	 * capabilities
	 */
	private void addDependency(Capability target, Capability dependent) {
		ArrayList descriptors = (ArrayList) dependents.get(target);
		if (descriptors == null) {
			descriptors = new ArrayList();
			descriptors.add(dependent);
			dependents.put(target, descriptors);
		}
		else if (!descriptors.contains(dependent)) {
			descriptors.add(dependent);
		}
	}
		
	/**
	 * Populate the dependents map based on the project's
	 * current set of capabilities.
	 */
	private void populateDependents() {
		LinkedList capabilities = new LinkedList();
		Object[] checked = listViewer.getCheckedElements();
		for (int i = 0; i < checked.length; i++)
			capabilities.addLast(checked[i]);
			
		while (!capabilities.isEmpty()) {
			// Retrieve the target capability
			Capability target;
			target = (Capability) capabilities.removeFirst();
			// Add the capability as a dependent of itself.
			// It will indicate to the uncheck handler to not uncheck this
			// capability automatically even if a another capability which
			// depended on it is unchecked.
			addDependency(target, target);
			
			if (registry.hasPrerequisites(target)) {
				// Retrieve the prerequisite capabilities...
				String[] prereqIds = registry.getPrerequisiteIds(target);
				Capability[] prereqCapabilities;
				prereqCapabilities = registry.findCapabilities(prereqIds);
				// For each prerequisite capability...
				for (int i = 0; i < prereqCapabilities.length; i++) {
					// Update the dependent map for the prerequisite capability
					addDependency(prereqCapabilities[i], target);
					listViewer.setGrayed(prereqCapabilities[i], true);
					// Recursive if prerequisite capability also has prerequisites
					if (registry.hasPrerequisites(prereqCapabilities[i]))
						capabilities.addLast(prereqCapabilities[i]);
				}
			}
		}
	}
	
	/**
	 * Handle the case of a capability being checked
	 * by ensuring the action is allowed and the prerequisite
	 * capabilities are also checked.
	 */
	private void handleCapabilityChecked(Capability capability) {
		if (registry.hasPrerequisites(capability)) {
			// Retrieve all the prerequisite capabilities, including
			// any prerequisite of the prerequisites!
			ArrayList allPrereqs = new ArrayList();
			LinkedList capabilities = new LinkedList();
			capabilities.addLast(capability);
			while (!capabilities.isEmpty()) {
				Capability target;
				target = (Capability) capabilities.removeFirst();
				// Retrieve the capability's immediate prerequisites
				String[] prereqIds = registry.getPrerequisiteIds(target);
				Capability[] prereqCapabilities;
				prereqCapabilities = registry.findCapabilities(prereqIds);
				for (int i = 0; i < prereqCapabilities.length; i++) {
					// If the prerequisite is missing, warn the user and
					// do not allow the check to proceed.
					if (prereqCapabilities[i] == null) {
						StringBuffer msg = new StringBuffer();
						msg.append("The project feature \"");
						msg.append(capability.getName());
						msg.append("\" requires another project feature which is missing (id = ");
						msg.append(prereqIds[i]);
						msg.append(").\n\nThis feature cannot be checked.");
						MessageDialog.openWarning(listViewer.getControl().getShell(), "Required Feature Missing", msg.toString());
						listViewer.setChecked(capability, false);
						return;
					}
					// If the prerequisite capability has prerequisites
					// also, then add it to be processed.
					if (registry.hasPrerequisites(prereqCapabilities[i]))
						capabilities.addLast(prereqCapabilities[i]);
					// Merge the prerequisite into a master list
					allPrereqs.add(prereqCapabilities[i]);
				}
			}
			// Warn the user that the prerequisites need to be checked also
			StringBuffer msg = new StringBuffer();
			msg.append("The project feature \"");
			msg.append(capability.getName());
			msg.append("\" requires the following:");
			for (int i = 0; i < allPrereqs.size(); i++) {
				Capability cap = (Capability) allPrereqs.get(i);
				msg.append("\n   ");
				msg.append(cap.getName());
			}
			msg.append("\n\nDo you want these required project features checked also?");
			boolean yes = MessageDialog.openQuestion(listViewer.getControl().getShell(), "Required Features", msg.toString());
			if (yes) {
				// User wants all prerequisite capabilities to be checked.
				capabilitiesModified();
				capabilities = new LinkedList();
				capabilities.addLast(capability);
				// For each capability that has prerequisites...
				while (!capabilities.isEmpty()) {
					Capability target;
					target = (Capability) capabilities.removeFirst();
					// Retrieve the prerequisite capabilities...
					String[] prereqIds = registry.getPrerequisiteIds(target);
					Capability[] prereqCapabilities;
					prereqCapabilities = registry.findCapabilities(prereqIds);
					// For each prerequisite capability...
					for (int i = 0; i < prereqCapabilities.length; i++) {
						// Check the prerequisite capability
						listViewer.setChecked(prereqCapabilities[i], true);
						// Gray the prerequisite to show the user its required
						// by another capability.
						listViewer.setGrayed(prereqCapabilities[i], true);
						// Update the dependent map for the prerequisite capability
						addDependency(prereqCapabilities[i], target);
						// Recursive if prerequisite capability also has prerequisites
						if (registry.hasPrerequisites(prereqCapabilities[i]))
							capabilities.addLast(prereqCapabilities[i]);
					}
				}
				// Add the capability as a dependent of itself.
				// It will indicate to the uncheck handler to not uncheck this
				// capability automatically even if a another capability which
				// depended on it is unchecked.
				addDependency(capability, capability);
			
				// Notify those interested
				notifyCheckStateListner();
			}
			else {
				// User does not want prerequisite checked so do not
				// allow the original capability to be checked
				listViewer.setChecked(capability, false);
			}
		}
		else {
			// There are no prerequisite capabilities so allow the
			// check to proceed. The item should already be ungray...
			// if it is gray, then there's a problem elsewhere.
			capabilitiesModified();
			
			// Add the capability as a dependent of itself.
			// It will indicate to the uncheck handler to not uncheck this
			// capability automatically even if a another capability which
			// depended on it is unchecked.
			addDependency(capability, capability);
			
			// Notify those interested
			notifyCheckStateListner();
		}
	}
	
	/**
	 * Handle the case of a capability being unchecked
	 * by ensuring the action is allowed. 
	 */
	private void handleCapabilityUnchecked(Capability capability) {
		ArrayList descriptors = (ArrayList) dependents.get(capability);
		
		// Note, there is no need to handle the case where descriptors size
		// is zero because it cannot happen. For this method to be called, the
		// item must have been checked previously. If it was checked by the user,
		// then the item itself would be a dependent. If the item was checked
		// because it was required by another capability, then that other capability
		// would be a dependent.
		
		if (descriptors.size() == 1 && descriptors.get(0) == capability) {
			// If the only dependent is itself, then its ok to uncheck
			// The item should not be gray...if it is, there's a problem elsewhere.
			capabilitiesModified();
			// Remove the dependency entry.
			dependents.remove(capability);
			
			// Remove this capability as a dependent on its prerequisite
			// capabilities. Recursive if a prerequisite capability
			// no longer has any dependents.
			if (registry.hasPrerequisites(capability)) {
				LinkedList capabilities = new LinkedList();
				capabilities.addLast(capability);
				// For each capability that has prerequisite capabilities
				while (!capabilities.isEmpty()) {
					Capability target;
					target = (Capability) capabilities.removeFirst();
					// Retrieve the prerequisite capabilities...
					String[] prereqIds = registry.getPrerequisiteIds(target);
					Capability[] prereqCapabilities;
					prereqCapabilities = registry.findCapabilities(prereqIds);
					// For each prerequisite capability...
					for (int i = 0; i < prereqCapabilities.length; i++) {
						// Retrieve the list of dependents on the prerequisite capability...
						Capability prereqCap = prereqCapabilities[i];
						ArrayList prereqDependents = (ArrayList) dependents.get(prereqCap);
						// Remove the dependent target capability...
						prereqDependents.remove(target);
						if (prereqDependents.isEmpty()) {
							// If no more dependents, then uncheck
							// and ungray prerequisite capability
							listViewer.setChecked(prereqCap, false);
							listViewer.setGrayed(prereqCap, false);
							// Recursive if prerequisite capability also has
							// prerequisite capabilities
							if (registry.hasPrerequisites(prereqCap))
								capabilities.addLast(prereqCap);
							// Clear the dependency entry
							dependents.remove(prereqCap);
						}
						else if (prereqDependents.size() == 1 && prereqDependents.get(0) == prereqCap) {
							// Only dependent is itself so ungray the item to let the
							// user know no other capability is dependent on it
							listViewer.setGrayed(prereqCap, false);
						}
					}
				}
			}
			
			// Notify those interested
			notifyCheckStateListner();
		}
		else {
			// At least one other capability depends on it being checked
			// so force it checked and warn the user.
			listViewer.setChecked(capability, true);
			StringBuffer msg = new StringBuffer();
			msg.append("The project feature \"");
			msg.append(capability.getName());
			msg.append("\" is required by the following:");
			for (int i = 0; i < descriptors.size(); i++) {
				Capability cap = (Capability) descriptors.get(i);
				if (cap != capability) {
					msg.append("\n   ");
					msg.append(cap.getName());
				}
			}
			msg.append("\n\nThis feature must remain checked.");
			MessageDialog.openWarning(listViewer.getControl().getShell(), "Feature Required", msg.toString());
		}
	}
	
	/**
	 * Return the collection of capabilities
	 * that the user wants to add. The capability are in
	 * proper order (i.e. prerequisite is before
	 * any of its dependents).
	 * 
	 * @return array of ordered capabilities to add
	 */
	public Capability[] getCapabilitiesToAdd() {
//		buildFeatureChangesCache();
//		
//		ArrayList results = new ArrayList();
//		for (int i = 0; i < featuresToAdd.size(); i++) {
//			ArrayList level = (ArrayList) featuresToAdd.get(i);
//			results.addAll(level);
//		}
//		
//		ProjectFeatureDescriptor[] features;
//		features = new ProjectFeatureDescriptor[results.size()];
//		return (ProjectFeatureDescriptor[]) results.toArray(features);
		return new Capability[0];
	}

	/**
	 * Return the collection of capabilities
	 * that the user wants to remove. The capabilities are in
	 * proper order (i.e. dependent is before
	 * any of its prerequisites).
	 * 
	 * @return array of ordered capabilities to remove
	 */
	public Capability[] getCapabilitiesToRemove() {
//		buildFeatureChangesCache();
//		
//		ArrayList results = new ArrayList();
//		for (int i = 0; i < featuresToRemove.size(); i++) {
//			ArrayList level = (ArrayList) featuresToRemove.get(i);
//			results.addAll(level);
//		}
//		
//		ProjectFeatureDescriptor[] features;
//		features = new ProjectFeatureDescriptor[results.size()];
//		// Add from last to first for proper order (dependents before prerequisites)
//		for (int i = results.size() - 1, j = 0; i >= 0; i--, j++)
//			features[j] = (ProjectFeatureDescriptor)results.get(i);
//			
//		return features;
		return new Capability[0];
	}
	
	/**
	 * Return the current listener interested when the check
	 * state of a capability actually changes.
	 * 
	 * @return Returns a ICheckStateListener
	 */
	public ICheckStateListener getCheckStateListener() {
		return checkStateListener;
	}

	/**
	 * Set the current listener interested when the check
	 * state of a capability actually changes.
	 * 
	 * @param checkStateListener The checkStateListener to set
	 */
	public void setCheckStateListener(ICheckStateListener checkStateListener) {
		this.checkStateListener = checkStateListener;
	}

	/**
	 * Notify the check state listener that a capability
	 * check state has changed. The event past will
	 * always be <code>null</code> as it could be
	 * triggered by code instead of user input.
	 */
	private void notifyCheckStateListner() {
		if (checkStateListener != null)
			checkStateListener.checkStateChanged(null);
	}
}

