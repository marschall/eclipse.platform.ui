package org.eclipse.ui.texteditor;


/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.util.Assert;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.ui.PlatformUI;


/**
 * Abstract base implementation of a marker-based annotation model.
 * <p>
 * Markers are provided by an underlying source (a subclass responsibility).
 * Markers whose textual range gets deleted during text editing are removed
 * from the model on save. The <code>updateMarkers</code> method can be used
 * to force the model to update the source's markers with any changes to their 
 * locations due to edits. Clients can register <code>IMarkerUpdater</code> 
 * objects in order to define the process of marker updating.
 * <p>
 * Subclasses must implement the following methods:
 * <ul>
 *   <li><code>retrieveMarkers</code></li>
 *   <li><code>isAcceptable</code></li>
 *   <li><code>deleteMarkers</code></li>
 *   <li><code>listenToMarkerChanges</code></li>
 * </ul> 
 * </p>
 */
public abstract class AbstractMarkerAnnotationModel extends AnnotationModel {

	/** List of annotations whose text range became invalid because of document changes */
	private List fDeletedAnnotations= new ArrayList(2);
	/** Reference counters to track how often an annotation model is connected to its document */
	private HashMap fRefcountTable= new HashMap();
	/** List of registered and instantiated marker updaters */
	private List fInstantiatedMarkerUpdaters= null;
	/** List of registered but not yet instantiated marker updaters */
	private List fMarkerUpdaterSpecifications= null;
	
	
	/**
	 * Retrieves all markers from this model.<p>
	 * Subclasses must implement this method.
	 *
	 * @return the list of markers
	 * @exception CoreException if there is a problem getting the markers
	 */
	protected abstract IMarker[] retrieveMarkers() throws CoreException;

	/**
	 * Deletes the given markers from this model.<p>
	 * Subclasses must implement this method.
	 *
	 * @param markers the list of markers
	 * @exception CoreException if there are problems deleting the markers
	 */
	protected abstract void deleteMarkers(IMarker[] markers) throws CoreException;

	/**
	 * Tells the model whether it should listen for marker changes. <p>
	 * Subclasses must implement this method.
	 *
	 * @param listen <code>true</code> if this model should listen, and
	 *   <code>false</code> otherwise
	 */
	protected abstract void listenToMarkerChanges(boolean listen);

	/**
	 * Determines whether the marker is acceptable as an addition to this model.
	 * If the marker, say, represents an aspect or range of no interest to this
	 * model, the marker is rejected.<p>
	 * Subclasses must implement this method.
	 *
	 * @param marker the marker
	 * @return <code>true</code> if the marker is acceptable
	 */
	protected abstract boolean isAcceptable(IMarker marker);

	/**
	 * Creates a new annotation model. The annotation model does not manage any
	 * annotations and is not connected to any document.
	 */
	protected AbstractMarkerAnnotationModel() {
	}
	
	/**
	 * Adds the given marker updater to this annotation model.
	 * It is client's responsibility to ensure the consitency of the
	 * set of registered marker updaters.
	 *
	 * @param markerUpdater the marker updater to be added
	 */
	protected void addMarkerUpdater(IMarkerUpdater markerUpdater) {
		if (!fInstantiatedMarkerUpdaters.contains(markerUpdater))
			fInstantiatedMarkerUpdaters.add(markerUpdater);
	}
	
	/**
	 * Removes the given marker updater from this annotation model.
	 *
	 * @param markerUpdater the marker updater to be removed
	 */
	protected void removeMarkerUpdater(IMarkerUpdater markerUpdater) {
		fInstantiatedMarkerUpdaters.remove(markerUpdater);
	}

	/**
	 * Creates a new annotation for the given marker.<p>
	 * Subclasses may override.
	 *
	 * @param marker the marker
	 * @return the new marker annotation
	 */
	protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
		return new MarkerAnnotation(marker);
	}

	/**
	 * Handles an unanticipated <code>CoreException</code> in 
	 * a standard manner.
	 *
	 * @param exception the exception
	 * @param message a message to aid debugging
	 */
	protected void handleCoreException(CoreException exception, String message) {
		
		ILog log= Platform.getPlugin(PlatformUI.PLUGIN_ID).getLog();
		
		if (message != null)
			log.log(new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, 0, message, null));
		
		log.log(exception.getStatus());
	}
	
	/**
	 * Creates and returns the character position of the given marker based
	 * on its attributes. <p>
	 * Subclasses may override.
	 *
	 * @param marker the marker
	 * @return the new position or <code>null</code> if no valid position
	 */
	protected Position createPositionFromMarker(IMarker marker) {
		
		int start= MarkerUtilities.getCharStart(marker);
		int end= MarkerUtilities.getCharEnd(marker);
		
		if (start > end) {
			end= start + end;
			start= end - start;
			end= end - start;
		}
		
		if (start == -1 && end == -1) {
			// marker line number is 1-based
			int line= MarkerUtilities.getLineNumber(marker);
			if (line > 0 && fDocument != null) {
				try {
					start= fDocument.getLineOffset(line - 1);
					end= start;
				} catch (BadLocationException x) {
				}
			}
		}
		
		if (start > -1 && end > -1)
			return new Position(start, end - start);
		
		return null;
	}

	/**
	 * Creates an annotation for the given marker and adds it to this model.
	 * Does nothing if the marker is not acceptable to this model.
	 *
	 * @param marker the marker
	 * @see #isAcceptable
	 */
	protected final void addMarkerAnnotation(IMarker marker) {
		
		if (isAcceptable(marker)) {
			Position p= createPositionFromMarker(marker);
			if (p != null)
				addAnnotation(createMarkerAnnotation(marker), p, false);
		}
	}

	/**
	 * Connects to the source of markers as marker change listener.
	 * @see AnnotationModel#connected
	 */
	protected void connected() {
				
		listenToMarkerChanges(true);
		
		try {
			catchupWithMarkers();
		} catch (CoreException x) {
			handleCoreException(x, EditorMessages.getString("AbstractMarkerAnnotationModel.connected")); //$NON-NLS-1$
		}

		fireModelChanged();
	}
		
	/**
	 * Installs all marker updaters for this marker annotation model. 
	 */
	private void installMarkerUpdaters() {
		
		// initialize lists - indicates that the initialization happened
		fMarkerUpdaterSpecifications= new ArrayList(2);
		fInstantiatedMarkerUpdaters= new ArrayList(2);
		
		// populate list
		IExtensionPoint extensionPoint= Platform.getPluginRegistry().getExtensionPoint(PlatformUI.PLUGIN_ID, "markerUpdaters"); //$NON-NLS-1$
		if (extensionPoint != null) {
			IConfigurationElement[] elements= extensionPoint.getConfigurationElements();
			for (int i= 0; i < elements.length; i++)
				fMarkerUpdaterSpecifications.add(elements[i]);
		}
	}
	
	/**
	 * Uninstalls all marker updaters.
	 */
	private void uninstallMarkerUpdaters() {
		if (fInstantiatedMarkerUpdaters != null) {
			fInstantiatedMarkerUpdaters.clear();
			fInstantiatedMarkerUpdaters= null;
		}
		
		if (fMarkerUpdaterSpecifications != null) {
			fMarkerUpdaterSpecifications.clear();
			fMarkerUpdaterSpecifications= null;
		}
	}
	
	/**
	 * Removes the marker change listener.
	 * @see AnnotationModel#disconnected
	 */
	protected void disconnected() {
		listenToMarkerChanges(false);
		uninstallMarkerUpdaters();
	}
	
	/**
	 * Returns the position known to this annotation model for the given marker.
	 *
	 * @param marker the marker
	 * @return the position, or <code>null</code> if none
	 */
	public Position getMarkerPosition(IMarker marker) {
		MarkerAnnotation a= getMarkerAnnotation(marker);
		if (a != null) {
			return (Position) fAnnotations.get(a);
		}
		return null;
	}
	
	/**
	 * Updates the annotation corresponding to the given marker which has changed
	 * in some way. <p>
	 * Subclasses may override.
	 *
	 * @param marker the marker
	 */
	protected void modifyMarkerAnnotation(IMarker marker) {
		MarkerAnnotation a= getMarkerAnnotation(marker);
		if (a != null) {
			
			// update annotation presentation
			a.update();
			
			// update annotation position
			Position p1= createPositionFromMarker(marker);
			if (p1 != null) {
				Position p0= (Position) fAnnotations.get(a);
				p0.setOffset(p1.getOffset());
				p0.setLength(p1.getLength());
			}
		}
	}

	/*
	 * @see AnnotationModel#removeAnnotations
	 */
	protected void removeAnnotations(List annotations, boolean fireModelChanged, boolean modelInitiated) {
		if (annotations != null && annotations.size() > 0) {

			int n= annotations.size();
			IMarker[] markers= new IMarker[n];
			for (int i= 0; i < n; ++i) {
				MarkerAnnotation a= (MarkerAnnotation) annotations.get(i);
				markers[i]= a.getMarker();

				// remove annotations from annotation model
				removeAnnotation(a, false);
			}

			// if model initiated also remove it from the marker manager
			if (modelInitiated) {
				
				listenToMarkerChanges(false);
				try {
					deleteMarkers(markers);
				} catch (CoreException x) {
					handleCoreException(x, EditorMessages.getString("AbstractMarkerAnnotationModel.removeAnnotations")); //$NON-NLS-1$
				}
				listenToMarkerChanges(true);
			
			} else {
				// remember deleted annotations in order to remove their markers later on
				fDeletedAnnotations.addAll(annotations);
			}

			if (fireModelChanged)
				fireModelChanged();
		}
	}

	/**
	 * Removes the annotation corresponding to the given marker. Does nothing
	 * if there is no annotation for this marker.
	 *
	 * @param marker the marker
	 */
	protected final void removeMarkerAnnotation(IMarker marker) {
		MarkerAnnotation a= getMarkerAnnotation(marker);
		if (a != null) {
			removeAnnotation(a, false);
		}
	}

	/**
	 * Re-populates this model with annotations for all markers retrieved
	 * from the maker source via <code>retrieveMarkers</code>.
	 *
	 * @exception CoreException if there is a problem getting the markers
	 */
	private void catchupWithMarkers() throws CoreException {

		removeAllAnnotations(false);

		IMarker[] markers= retrieveMarkers();
		if (markers != null) {
			for (int i= 0; i < markers.length; i++)
				addMarkerAnnotation(markers[i]);
		}
	}

	/**
	 * Returns this model's annotation for the given marker.
	 *
	 * @param marker the marker
	 * @return the annotation, or <code>null</code> if none
	 */
	public final MarkerAnnotation getMarkerAnnotation(IMarker marker) {
		Iterator e= getAnnotationIterator(false);
		while (e.hasNext()) {
			Object o= e.next();
			if (o instanceof MarkerAnnotation) {
				MarkerAnnotation a= (MarkerAnnotation) o;
				if (marker.equals(a.getMarker())) {
					return a;
				}
			}
		}
		return null;
	}
	
	/**
	 * Creates a marker updater as specified in the given configuration
	 * element.
	 */
	private IMarkerUpdater createMarkerUpdater(IConfigurationElement element) {
		try {
			return (IMarkerUpdater) element.createExecutableExtension("class"); //$NON-NLS-1$
		} catch (CoreException x) {
			handleCoreException(x, EditorMessages.getString("AbstractMarkerAnnotationModel.createMarkerUpdater")); //$NON-NLS-1$
		}
		
		return null;
	}
	
	/**
	 * Checks whether a merker updater is registered for the type of the
	 * given marker but not yet instantiated. If so, the method instantiates
	 * the marker updater and registers it with this model.
	 */
	private void checkMarkerUpdaters(IMarker marker) {
		List toBeDeleted= new ArrayList();
		for (int i= 0; i < fMarkerUpdaterSpecifications.size(); i++) {
			IConfigurationElement spec= (IConfigurationElement) fMarkerUpdaterSpecifications.get(i);
			String markerType= spec.getAttribute("markerType"); //$NON-NLS-1$
			if (markerType == null || MarkerUtilities.isMarkerType(marker, markerType)) {
				toBeDeleted.add(spec);
				IMarkerUpdater updater= createMarkerUpdater(spec);
				if (updater != null)
					addMarkerUpdater(updater);
			}
		}
		
		for (int i= 0; i < toBeDeleted.size(); i++)
			fMarkerUpdaterSpecifications.remove(toBeDeleted.get(i));
	}
	
	/**
	 * Updates the given marker according to the given position in the given
	 * document. If the given position is <code>null</code>, the marker is 
	 * assumed to carry the correct positional information. If it is detected 
	 * that the marker is invalid and should thus be deleted, this method 
	 * returns <code>false</code>.
	 *
	 * @param marker the marker to be updated
	 * @param document the document into which the given position points
	 * @param position the current position of the marker inside the given document
	 * @exception CoreException if there is a problem updating the marker  
	 */
	public boolean updateMarker(IMarker marker, IDocument document, Position position) throws CoreException {
		

		if (!fMarkerUpdaterSpecifications.isEmpty())
			checkMarkerUpdaters(marker);
			
			
		boolean isOK= true;
		
		for (int i= 0; i < fInstantiatedMarkerUpdaters.size();  i++) {
			IMarkerUpdater updater= (IMarkerUpdater) fInstantiatedMarkerUpdaters.get(i);
			String markerType= updater.getMarkerType();
			if (markerType == null || MarkerUtilities.isMarkerType(marker, markerType)) {
				
				if (position == null) {
					/* compatibility code */
					position= createPositionFromMarker(marker);
				}
				
				isOK= (isOK && updater.updateMarker(marker, document, position));
			}
		}
		
		return isOK;
	}
	
	/**
	 * Updates the markers managed by this annotation model by calling 
	 * all registered marker updaters (<code>IMarkerUpdater</code>).
	 *
	 * @param document the document to which this model is currently connected
	 * @exception CoreException if there is a problem updating the markers
	 */
	public void updateMarkers(IDocument document) throws CoreException {

		Assert.isTrue(fDocument == document);
		
		/*
		 * 1GFBE1X: ITPSRCEDIT:ALL - Marker are not deleted on save
		 */
		if (fAnnotations.size() == 0 && fDeletedAnnotations.size() == 0)
			return;
			
		if (fMarkerUpdaterSpecifications == null)
			installMarkerUpdaters();
			
		listenToMarkerChanges(false);
				
		// update all markers with the positions known by the annotation model
		for (Iterator e= getAnnotationIterator(false); e.hasNext();) {
			Object o= e.next();
			if (o instanceof MarkerAnnotation) {
				MarkerAnnotation a= (MarkerAnnotation) o;
				IMarker marker= a.getMarker();
				Position position= (Position) fAnnotations.get(a);
				if ( !updateMarker(marker, document, position)) {
					if ( !fDeletedAnnotations.contains(a))
						fDeletedAnnotations.add(a);
				}
			}
		}
		
		if (!fDeletedAnnotations.isEmpty()) {
			removeAnnotations(fDeletedAnnotations, true, true);
			fDeletedAnnotations.clear();
		}

		listenToMarkerChanges(true);
	}
	
	/**
	 * Resets all the markers to their original state.
	 */
	public void resetMarkers() {
		
		// reinitializes the markers
		for (Iterator e= getAnnotationIterator(false); e.hasNext();) {
			Object o= e.next();
			if (o instanceof MarkerAnnotation) {
				MarkerAnnotation a= (MarkerAnnotation) o;
				Position p1= createPositionFromMarker(a.getMarker());
				if (p1 != null) {
					Position p0= (Position) fAnnotations.get(a);
					p0.setOffset(p1.getOffset());
					p0.setLength(p1.getLength());
				}
			}
		}
		
		// add the deleted markers back to the annotation model
		for (Iterator e= fDeletedAnnotations.iterator(); e.hasNext();) {
			Object o= e.next();
			if (o instanceof MarkerAnnotation) {
				MarkerAnnotation a= (MarkerAnnotation) o;
				Position p= createPositionFromMarker(a.getMarker());
				if (p != null)
					addAnnotation(a, p, false);
			}
		}
		fDeletedAnnotations.clear();
	}
}