package org.eclipse.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * <code>IEditorInput</code> is a light weight descriptor of editor input,
 * like a file name but more abstract.  It is not a model.  It is a 
 * description of the model source for an <code>IEditorPart</code>.
 * <p>
 * Clients should extend this interface to declare new types of editor
 * inputs.
 * </p>
 * <p>
 * An editor input is passed to an editor via the <code>IEditorPart.init</code>
 * method. Due to the wide range of valid editor inputs, it is not possible to
 * define generic methods for getting and setting bytes. However, two subtypes 
 * of <code>IEditorInput</code> have been defined for greater type clarity when
 * IStorage (<code>IStorageEditorInput</code>) and IFiles 
 * (<code>IFileEditorInput</code>) are used. Any editor which is file-oriented
 * should handle these two types. The same pattern may be used to define
 * other editor input types.  
 * </p>
 * <p>
 * The <code>IStorageEditorInput</code> interface is used to wrap an 
 * <code>IStorage</code> object.  This may represent read-only data
 * in a repository, external jar, or file system. The editor should provide 
 * viewing (but not editing) functionality.
 * </p>
 * <p>
 * The <code>IFileEditorInput</code> interface is used to wrap an 
 * file resource (<code>IFile</code>). The editor should provide read and write
 * functionality.
 * </p>
 * <p>
 * Editor input must implement the <code>IAdaptable</code> interface; extensions
 * are managed by the platform's adapter manager.
 * </p>
 *
 * @see IEditorPart
 * @see org.eclipse.core.resources.IFile
 * @see IStreamEditorInput
 * @see IFileEditorInput
 */
public interface IEditorInput extends IAdaptable {
/**
 * Returns whether the editor input exists.  
 * <p>
 * This method is primarily used to determine if an editor input should 
 * appear in the "File Most Recently Used" menu.  An editor input will appear 
 * in the list until the return value of <code>exists</code> becomes 
 * <code>false</code> or it drops off the bottom of the list.
 *
 * @return <code>true</code> if the editor input exists; <code>false</code>
 *		otherwise
 */ 
public boolean exists();
/**
 * Returns the image descriptor for this input.
 *
 * @return the image descriptor for this input
 */
public ImageDescriptor getImageDescriptor();
/**
 * Returns the name of this editor input for display purposes.
 * <p>
 * For instance, if the fully qualified input name is
 * <code>"a\b\MyFile.gif"</code>, the return value would be just
 * <code>"MyFile.gif"</code>.
 *
 * @return the name string
 */ 
public String getName();
/*
 * Returns an object that can be used to save the state of this editor input.
 *
 * @return the persistable element, or <code>null</code> if this editor input
 *   cannot be persisted
 */
public IPersistableElement getPersistable();
/**
 * Returns the tool tip text for this editor input.  This text
 * is used to differentiate between two input with the same name.
 * For instance, MyClass.java in folder X and MyClass.java in folder Y.
 * <p> 
 * The format of the path will vary with each input type.  For instance,
 * if the editor input is of type <code>IFileEditorInput</code> this method
 * should return the fully qualified resource path.  For editor input of
 * other types it may be different. 
 * </p>
 * @return the tool tip text
 */ 
public String getToolTipText();
}