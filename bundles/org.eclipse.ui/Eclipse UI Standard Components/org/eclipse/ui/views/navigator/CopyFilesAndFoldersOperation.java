package org.eclipse.ui.views.navigator;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.ContainerGenerator;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.internal.misc.StatusUtil;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

/**
 * Perform the copy of file and folder resources from the clipboard 
 * when paste action is invoked.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
/*package*/ class CopyFilesAndFoldersOperation {
	
	/**
	 * Status containing the errors detected when running the operation or
	 * <code>null</code> if no errors detected.
	 */
	private MultiStatus errorStatus;

	/**
	 * The parent shell used to show any dialogs.
	 */
	private Shell parentShell;
	
	/**
	 * The destination of the resources to be copied.
	 */
	private IResource destination;

	/**
	 * A list of all resources against which copy errors are reported.
	 */
	private ArrayList errorResources = new ArrayList();
	
	/**
	 * Whether or not the copy has been canceled by the user.
	 */
	private boolean canceled = false;
	
	/**
	 * Overwrite all flag.
	 */
	private boolean alwaysOverwrite = false;
	
	/** 
	 * Creates a new operation initialized with a shell.
	 * 
	 * @param shell parent shell for error dialogs
	 */
	public CopyFilesAndFoldersOperation(Shell shell) {
		parentShell = shell;
	}
		
	/**
	 * Copies the given resources to the destination. 
	 * 
	 * @param resources the resources to copy
	 * @param destination destination to which resources on the clipboard will be pasted 
	 */
	public void copyResources(final IResource[] resources, IContainer destination) {
		final IPath destinationPath = destination.getFullPath();
	
		String errorMsg = validateDestination(destination, resources);
		if (errorMsg != null) {
			displayError(errorMsg); 
			return;
		}
		
		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				// Checks only required if this is an exisiting container path.
				monitor.beginTask(ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.operationTitle"),100); //$NON-NLS-1$
				monitor.worked(10); // show some initial progress
				boolean copyWithAutoRename = false;
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				if (root.exists(destinationPath)) {
					IContainer container = (IContainer) root.findMember(destinationPath);
					// If we're copying to the source container then perform
					// auto-renames on all resources to avoid name collisions.
					if (isDestinationSameAsSource(container, resources)) {
						copyWithAutoRename = true;
					} else {
						// If no auto-renaming will be happening, check for
						// potential name collisions at the target resource
						if (!validateNoNameCollisions(container, resources ,monitor)) {
							if (canceled) 
								return;
							displayError(ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.nameCollision")); //$NON-NLS-1$
							return;
						}
					}
				}
	
				errorStatus = null;
				boolean opSuccess;
				if (copyWithAutoRename)
					opSuccess = performCopyWithAutoRename(resources, destinationPath, monitor);
				else
					opSuccess = performCopy(resources, destinationPath, monitor);
			}
		};
	
		try {
			new ProgressMonitorDialog(parentShell).run(true, true, op);
		} catch (InterruptedException e) {
			return;
		} catch (InvocationTargetException e) {
			// CoreExceptions are collected above, but unexpected runtime exceptions and errors may still occur.
			Platform.getPlugin(PlatformUI.PLUGIN_ID).getLog().log(
				StatusUtil.newStatus(
					IStatus.ERROR,
					null,
					MessageFormat.format("Exception in {0}.performCopy(): {1}", new Object[] {getClass().getName(),e.getTargetException()}), //$NON-NLS-1$
					null));
			displayError(ResourceNavigatorMessages.format("CopyFilesAndFoldersOperation.internalError", new Object[] {e.getTargetException().getMessage()})); //$NON-NLS-1$
		}
	
		// If errors occurred, open an Error dialog
		if (errorStatus != null) {
			ErrorDialog.openError(
				parentShell, 
				ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.copyFailedTitle"), //$NON-NLS-1$
				null, // no special message
				errorStatus);
			errorStatus = null;
		} 
	}
	/**
	 * Paste files and folders on the clipboard to the destination 
	 * (i.e to the selected resource).  
	 *  
	 * @param names destination to which files/folders on the clipboard will be pasted 
	 */
	public void copyFiles(final String[] fileNames, IContainer destination){
		alwaysOverwrite = false;
	
		// if it is a project, see if it is open
		if (destination.getType() == IResource.PROJECT && !((IProject)destination).isOpen()) {
			displayError(ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.destinationAccessError")); //$NON-NLS-1$
			return;
		}
				
		final IPath destinationPath = destination.getFullPath();
	
		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				// Checks only required if this is an exisiting container path.
				boolean copyWithAutoRename = false;
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				if (root.exists(destinationPath)) {
					IContainer container = (IContainer) root.findMember(destinationPath);
					monitor.beginTask("", fileNames.length);
					for (int k = 0; k < fileNames.length; k++)
						performFileImport(new SubProgressMonitor(monitor, 1), container, fileNames[k]);
					monitor.done();	
				}
			}
		};
		try {
			new ProgressMonitorDialog(parentShell).run(true, true, op);
		} catch (InterruptedException e) {
			return;
		} catch (InvocationTargetException e) {
			// CoreExceptions are collected above, but unexpected runtime exceptions and errors may still occur.
			Platform.getPlugin(PlatformUI.PLUGIN_ID).getLog().log(
				StatusUtil.newStatus(
					IStatus.ERROR,
					null,
					MessageFormat.format("Exception in {0}.performCopy(): {1}", new Object[] {getClass().getName(),e.getTargetException()}), //$NON-NLS-1$
					null));
			displayError(ResourceNavigatorMessages.format("CopyFilesAndFoldersOperation.internalError", new Object[] {e.getTargetException().getMessage()})); //$NON-NLS-1$
		}
	
		// If errors occurred, open an Error dialog
		if (errorStatus != null) {
			ErrorDialog.openError(
				parentShell, 
				ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.copyFailedTitle"), //$NON-NLS-1$
				null, // no special message
			errorStatus);
			errorStatus = null;
		} 
	}

	/**
	 * Opens an error dialog to display the given message.
	 * <p>
	 * Note that this method must be called from UI thread.
	 * </p>
	 *
	 * @param message the error message to show
	 */
	private void displayError(final String message) {
		parentShell.getDisplay().syncExec(new Runnable() {
			public void run() {
				MessageDialog.openError(
					parentShell, 
					ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.copyFailedTitle"), //$NON-NLS-1$
					message);
			}
		});
	}

	/**
	 * Performs an import of the given file into the provided
	 * container.  Returns a status indicating if the import was successful.
	 * 
	 * @param monitor a progress monitor for progress and cancelation
	 * @param target container to which the import will be done
	 * @param filePath path to file that is to be imported
	 */
	private void performFileImport(IProgressMonitor monitor,IContainer target, String filePath) {
		File toImport = new File(filePath);
		if (target.getLocation().equals(toImport)) 
			return;
		
		IOverwriteQuery query = new IOverwriteQuery() {
			public String queryOverwrite(String pathString) {
				if (alwaysOverwrite)
					return ALL;
			
				final String returnCode[] = {CANCEL};
				final String msg = ResourceNavigatorMessages.format("CopyFilesAndFoldersOperation.overwriteQuestion", new Object[] {pathString}); //$NON-NLS-1$
				final String[] options = {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.CANCEL_LABEL};
				parentShell.getDisplay().syncExec(new Runnable() {
					public void run() {
						MessageDialog dialog = new MessageDialog(parentShell, ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.question"), null, msg, MessageDialog.QUESTION, options, 0); //$NON-NLS-1$
						dialog.open();
						int returnVal = dialog.getReturnCode();
						String[] returnCodes = {YES, NO, ALL, CANCEL};
						returnCode[0] = returnVal == -1 ? CANCEL : returnCodes[returnVal];
					}
				});
				if(returnCode[0] == ALL)
					alwaysOverwrite = true;
				return returnCode[0];
			}
		};
	
		ImportOperation op = new ImportOperation(
				target.getFullPath(), 
				new File(toImport.getParent()), 
				FileSystemStructureProvider.INSTANCE, 
				query, 
				Arrays.asList(new File[] {toImport})); 
		op.setCreateContainerStructure(false);
		try {
			op.run(monitor);
		} catch (InterruptedException e) {
			return; 
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof CoreException) {
				final IStatus status = ((CoreException) e.getTargetException()).getStatus();
				parentShell.getDisplay().syncExec(new Runnable() {
					public void run() {
						ErrorDialog.openError(
							parentShell, 
							ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.importErrorDialogTitle"),  //$NON-NLS-1$
							null,	// no special message
							status);
					}
				});
			}
			else {
				// CoreExceptions are handled above, but unexpected runtime exceptions and errors may still occur.
				Platform.getPlugin(PlatformUI.PLUGIN_ID).getLog().log(
					StatusUtil.newStatus(
						IStatus.ERROR,
						null,
						MessageFormat.format("Exception in {0}.performFileImport(): {1}", new Object[] {getClass().getName(), e.getTargetException()}), //$NON-NLS-1$
						null));;
				displayError(ResourceNavigatorMessages.format("CopyFilesAndFoldersOperation.internalError", new Object[] {e.getTargetException().getMessage()})); //$NON-NLS-1$
			}
			return;
		}
		// Special case since ImportOperation doesn't throw a CoreException on
		// failure.
		IStatus status= op.getStatus();
		if (!status.isOK()) {
			if (errorStatus == null)
				errorStatus = new MultiStatus(PlatformUI.PLUGIN_ID, IStatus.ERROR, ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.problemMessage"), null);
			errorStatus.merge(status);
		}
	}
	
	/**
	 * Check if the user wishes to overwrite the supplied resource.
	 * 
	 * @param shell the shell to create the dialog in 
	 * @param destination - the resource to be overwritten
	 * @return true if there is no collision or delete was successful
	 */
	private boolean checkOverwrite(
		final Shell shell,
		final IResource destination) {
	
		final boolean[] result = new boolean[1];
	
		//Run it inside of a runnable to make sure we get to parent off of the shell as we are not
		//in the UI thread.
	
		Runnable query = new Runnable() {
			public void run() {
				result[0] =
					MessageDialog.openQuestion(
						shell,
						ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.resourceExists"), //$NON-NLS-1$
						ResourceNavigatorMessages.format("CopyFilesAndFoldersOperation.overwriteQuestion", new Object[] {destination.getFullPath().makeRelative()})); //$NON-NLS-1$
			}
	
		};
	
		shell.getDisplay().syncExec(query);
		return result[0];
	}
	
	/**
	 * Returns a new name for a copy of the resource at the given path in the given
	 * workspace. This name could be determined either automatically or by querying
	 * the user. This name will <b>not</b> be verified by the caller, so it must be
	 * valid and unique.
	 *
	 * @param originalName the full path of the resource
	 * @param workspace the workspace
	 * @return the new full path for the copy, or <code>null</code> if the resource
	 *   should not be copied
	 */
	private IPath getNewNameFor(IPath originalName, IWorkspace workspace) {
		int counter = 1;
		String resourceName = originalName.lastSegment();
		IPath leadupSegment = originalName.removeLastSegments(1);
		
		while (true) {
			String nameSegment;
			
			if (counter > 1)
				nameSegment = ResourceNavigatorMessages.format("CopyFilesAndFoldersOperation.copyNameTwoArgs", new Object[] {new Integer(counter), resourceName}); //$NON-NLS-1$
			else
				nameSegment = ResourceNavigatorMessages.format("CopyFilesAndFoldersOperation.copyNameOneArg", new Object[] {resourceName}); //$NON-NLS-1$
				
			IPath pathToTry = leadupSegment.append(nameSegment);
			
			if (!workspace.getRoot().exists(pathToTry))
				return pathToTry;
				
			counter++;
		}
	}
	
	/**
	 * Checks whether the current destination
	 * is valid for copying the source resources.
	 *
	 * @param destination the destination container
	 * @param sourceResources the source resources
	 * @return an error message, or <code>null</code> 
	 * if the path is valid
	 */
	private String validateDestination(IContainer destination,	IResource[] sourceResources) {
		// if it is a project, see if it is open
		if (destination.getType() == IResource.PROJECT &&
				!((IProject)destination).isOpen())
			return ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.destinationAccessError"); //$NON-NLS-1$

		for (int i = 0; i < sourceResources.length; i++) {
			if (!sourceResources[i].equals(destination) 
				&& isDescendentOf(destination, sourceResources[i])) {
					return ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.destinationDescendentError"); //$NON-NLS-1$;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns whether moving all of the given source resources to the given
	 * destination container could be done without causing name collisions.
	 * 
	 * @param destination the destination container
	 * @param sourceResources the list of resources 
	 * @return <code>true</code> if there would be no name collisions, and
	 *   <code>false</code> if there would
	 */
	private boolean validateNoNameCollisions(
		IContainer destination,
		IResource[] sourceResources,
		IProgressMonitor monitor) {
	
		List deleteItems = new ArrayList();
	
		IWorkspaceRoot workspaceRoot = destination.getWorkspace().getRoot();
	
		for (int i = 0; i < sourceResources.length; i++) {
			final IResource currentResource = sourceResources[i];
			final IPath currentPath = destination.getFullPath().append(currentResource.getName());
	
			IResource newResource = workspaceRoot.findMember(currentPath);
			if (newResource != null) {
				// Check to see if we would be overwriting a parent folder
				if (currentPath.isPrefixOf(currentResource.getFullPath())) {
					//Run it inside of a runnable to make sure we get to parent off of the shell as we are not
					//in the UI thread.
					Runnable notice = new Runnable() {
						public void run() {
							MessageDialog.openError(
								parentShell,
								ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.overwriteProblemTitle"),
								ResourceNavigatorMessages.format("CopyFilesAndFoldersOperation.overwriteProblem", new Object[] {currentPath, currentResource.getFullPath()})); //$NON-NLS-1$
						}
					};
					parentShell.getDisplay().syncExec(notice);
					canceled = true;
					return false;
				} else {
					if (checkOverwrite(parentShell, newResource)) {
						deleteItems.add(newResource);
					} else {
						canceled = true;
						return false;
					}
				}
			}
		} //No overwrite issues
		if (deleteItems.size() == 0)
			return true;
	
		//Now try deletions
	
		IResource[] deleteResources = new IResource[deleteItems.size()];
		deleteItems.toArray(deleteResources);
		try {
		    monitor.subTask(ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.deletingCollision")); //$NON-NLS-1$
			destination.getWorkspace().delete(
				deleteResources,
				IResource.KEEP_HISTORY,
				new SubProgressMonitor(monitor, 25));
		} catch (CoreException exception) {
			recordError(exception);
			return false;
		}
		return true;
	
	}
	
	/**
	 * Returns whether the given resource is accessible, where files and folders
	 * are always considered accessible, and where a project is accessible iff it
	 * is open.
	 *
	 * @param resource the resource
	 * @return <code>true</code> if the resource is accessible, and 
	 *   <code>false</code> if it is not
	 */
	private boolean isAccessible(IResource resource) {
		switch (resource.getType()) {
			case IResource.FILE:
				return true;
			case IResource.FOLDER:
				return true;
			case IResource.PROJECT:
				return ((IProject)resource).isOpen();
			default:
				return false;
		}
	}
	
	/**
	 * Returns whether the given candidate child resource is a descendent
	 * of the the given candidate parent resource.
	 *
	 * @param child the candidate child resource
	 * @param parent the candidate parent resource
	 * @return <code>true</code> if <code>child</code> is a descendent
	 *   of <code>parent</code>
	 */
	private boolean isDescendentOf(IResource child, IResource parent) {
		if (parent.getType() == IResource.FILE)
			return false;
	
		if (child.getType() == IResource.PROJECT)
			return false;
			
		IResource childParent = child.getParent();
		if (childParent.equals(parent)) 
			return true; 
			
		return isDescendentOf(childParent, parent);
	}
	
	/**
	 * Returns whether any of the given source resources are being recopied to their 
	 * current container.
	 *
	 * @param destination the destination container
	 * @param sourceResources the source resources 
	 * @return <code>true</code> if at least one of the given source resource's 
	 *   parent container is the same as the destination 
	 */
	private boolean isDestinationSameAsSource(IContainer destination, IResource[] sourceResources) {
		for (int i = 0; i < sourceResources.length; i++) {
			if (sourceResources[i].getParent().equals(destination)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Copies the given resources to the destination container with the given name.
	 * <p>
	 * Note: the destination container may need to be created prior to copying the
	 * resources.
	 * </p>
	 *
	 * @param resources the resources to copy
	 * @param destination the path of the destination container
	 * @return <code>true</code> if the copy operation completed without errors
	 */
	private boolean performCopy(
		final IResource[] resources,
		final IPath destination,
		IProgressMonitor monitor) {
	
		try {
			monitor.subTask(ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.copying")); //$NON-NLS-1$
			ContainerGenerator generator = new ContainerGenerator(destination);
			generator.generateContainer(new SubProgressMonitor(monitor, 10));
			resources[0].getWorkspace().copy(
				resources,
				destination,
				false,
				new SubProgressMonitor(monitor, 75));
		} catch (CoreException e) {
			recordError(e); // log error
			return false;
		} finally {
			monitor.done();
		}
		return true;
	}

	/**
	 * Individually copies the given resources to the specified destination
	 * container checking for name collisions. If a collision is detected, it is
	 * saved with a new name. 
	 * <p>
	 * Note: the destination container may need to be created prior to copying the
	 * resources.
	 * </p>
	 *
	 * @param resources the resources to copy
	 * @param destination the path of the destination container
	 * @return <code>true</code> if the copy operation completed without errors.
	 */
	private boolean performCopyWithAutoRename(
		IResource[] resources,
		IPath destination,
		IProgressMonitor monitor) {
	
		IWorkspace workspace = resources[0].getWorkspace();
	
		try {
			ContainerGenerator generator = new ContainerGenerator(destination);
	
			generator.generateContainer(new SubProgressMonitor(monitor, 10));
	
			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 75);	
			subMonitor.beginTask(
				ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.copying"), //$NON-NLS-1$
				resources.length);

			for (int i = 0; i < resources.length; i++) {
				
				IResource currentResource = resources[i];
				IPath destinationPath = destination.append(currentResource.getName());
	
				if (workspace.getRoot().exists(destinationPath))
					destinationPath = getNewNameFor(destinationPath, workspace);
	
				if (destinationPath != null) {
					try {
						currentResource.copy(
							destinationPath,
							false,
							new SubProgressMonitor(subMonitor, 0));
					} catch (CoreException e) {
						recordError(e); // log error
						return false;
					}
				}

				subMonitor.worked(1);	
				if (subMonitor.isCanceled()){
					throw new OperationCanceledException();
				}
			}
		} catch (CoreException e) {
			recordError(e); // log error
			return false;
		} finally {
			monitor.done();
		}
	
		return true;
	}

	/**
	 * Removes the given resources from the workspace.  
	 * 
	 * @param resourcesToDelete array of resources to remove from the workspace
	 * @param monitor a progress monitor for progress and cancelation
	 */
	private void delete(IResource[] resourcesToDelete, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("", resourcesToDelete.length); //$NON-NLS-1$
		for (int i = 0; i < resourcesToDelete.length; ++i) {
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			delete(resourcesToDelete[i], new SubProgressMonitor(monitor, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
		}
		monitor.done();
	}

	/**
	 * Removes the given resource from the workspace. 
	 *  
	 * @param resourceToDelete resource to remove from the workspace
	 * @param monitor a progress monitor for progress and cancelation
	 */
	private void delete(IResource resourceToDelete, IProgressMonitor monitor) throws CoreException {
		boolean force = false; // don't force deletion of out-of-sync resources
		if (resourceToDelete.getType() == IResource.PROJECT) {
			// if it's a project, ask whether content should be deleted too
			IProject project = (IProject) resourceToDelete;
			project.delete(true, force, monitor);
		}
		else {
			int flags = IResource.KEEP_HISTORY;
			if (force) flags = flags | IResource.FORCE;
			// if it's not a project, just delete it
			resourceToDelete.delete(flags, monitor);
		}
	}
	
	/**
	 * Records the core exception to be displayed to the user
	 * once the action is finished.
	 *
	 * @param error a <code>CoreException</code>
	 */
	private void recordError(CoreException error) {
		if (errorStatus == null)
			errorStatus = new MultiStatus(PlatformUI.PLUGIN_ID, IStatus.ERROR, ResourceNavigatorMessages.getString("CopyFilesAndFoldersOperation.problemMessage"), error);
	
		errorStatus.merge(error.getStatus());
	}
	
	
}
