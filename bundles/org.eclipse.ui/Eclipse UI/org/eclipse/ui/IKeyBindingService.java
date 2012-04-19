package org.eclipse.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.swt.widgets.Event;
import org.eclipse.jface.action.IAction;

/**
 * The key binding service is responsible for invoking an action
 * when the action's sequence of accelerator keys is pressed. The
 * accelerator key sequence assigned to an action is defined by the
 * extension point <code>org.eclipse.ui.acceleratorSets</code> and the
 * active accelerator configuration choosen by the user.
 * <p>
 * A participating workbench part is responsible to register all
 * its actions with the service. The part is also responsible to
 * set the current scope.
 * </p><p>
 * This interface is not intended to be implemented or extended
 * by clients.
 * </p>
 * @since 2.0
 */
public interface IKeyBindingService {
	/**
	 * Returns the id of the active accelerator configuration.
	 * 
	 * @return the id of the active accelerator configuration
	 */
	public String getActiveAcceleratorConfigurationId();
	
	/**
	 * Returns the id of the active accelerator scope.
	 * 
	 * @return the id of the active accelerator scope
	 */
	public String getActiveAcceleratorScopeId();
	
	/**
	 * To be called by an editor upon receiving a key event from its SWT
	 * text widget. The key binding service invokes the corresponding action
	 * if the key is mapped to an action. The key binding service may also
	 * invoke a mode, if the key is the first key in a (multi-key) accelerator
	 * key sequence of a registered action. If either of these cases occurs,
	 * processKey() returns true. If neither of these cases occurs, nothing
	 * happens and processKey() returns false.
	 * 
	 * @param event The key to be processed
	 * @return true if the key was consumed by the key binding service,
	 * false if the editor is free to consume the key
	 */
	public boolean processKey(Event event);
	
	/**
	 * Registers an action with the key binding service. Thereafter, if the 
	 * accelerator key sequence associated with the action is pressed, the
	 * action is executed.
	 * 
	 * @param action the action to be registered with the key binding service
	 */
	public void registerAction(IAction action);
	
	/**
	 * Sets the active accelerator scope id.
	 * 
	 * @param scopeId the new accelerator scope id
	 */
	public void setActiveAcceleratorScopeId(String scopeId);
}