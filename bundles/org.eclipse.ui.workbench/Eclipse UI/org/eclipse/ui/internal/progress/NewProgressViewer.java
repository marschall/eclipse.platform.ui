package org.eclipse.ui.internal.progress;

import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.internal.misc.Assert;


public class NewProgressViewer extends ProgressTreeViewer implements FinishedJobs.KeptJobsListener {
	
	static final boolean DEBUG= false;
	
	static final boolean isCarbon = "carbon".equals(SWT.getPlatform()); //$NON-NLS-1$
    
	static final String PROPERTY_PREFIX= "org.eclipse.ui.workbench.progress"; //$NON-NLS-1$

	/* an property of type URL that specifies the icon to use for this job. */
	static final String PROPERTY_ICON= "icon"; //$NON-NLS-1$
	/* this Boolean property controls whether a finished job is kept in the list. */
	static final String PROPERTY_KEEP= "keep"; //$NON-NLS-1$
	/* this Boolean property controls whether at most one finished job is kept in the list. */
	static final String PROPERTY_KEEPONE= "keepone"; //$NON-NLS-1$
	/* an property of type IAction that is run when link is activated. */
	static final String PROPERTY_GOTO= "goto"; //$NON-NLS-1$

	private static String ELLIPSIS = ProgressMessages.getString("ProgressFloatingWindow.EllipsisValue"); //$NON-NLS-1$

	static final QualifiedName KEEP_PROPERTY= new QualifiedName(PROPERTY_PREFIX, PROPERTY_KEEP);
	static final QualifiedName KEEPONE_PROPERTY= new QualifiedName(PROPERTY_PREFIX, PROPERTY_KEEPONE);
	static final QualifiedName ICON_PROPERTY= new QualifiedName(PROPERTY_PREFIX, PROPERTY_ICON);
	static final QualifiedName GOTO_PROPERTY= new QualifiedName(PROPERTY_PREFIX, PROPERTY_GOTO);
	
	private FinishedJobs finishedJobs;
	private boolean dialogContext;	// viewer runs in dialog: filter accordingly
	private HashMap map= new HashMap();
    private IJobProgressManagerListener progressManagerListener;
    private JobTreeItem highlightItem;
    private Job highlightJob;
    
	
	private Composite list;
	private ScrolledComposite scroller;
	private Color linkColor;
	private Color linkColor2;
	private Color errorColor;
	private Color errorColor2;
	private Color darkColor;
	private Color lightColor;
	private Color textColor;
	private Color selectedColor;
	private Color selectedTextColor;
	private Color highlightColor;
	private Font defaultFont= JFaceResources.getDefaultFont();
    // to be disposed
	private Cursor handCursor;
	private Cursor normalCursor;
	private Image defaultJobIcon;
	private Image cancelJobIcon;
	private Image clearJobIcon;
	private Font boldFont;
	private Font smallerFont;
    //

	
	class ListLayout extends Layout {
	    static final int VERTICAL_SPACING = 1;
		boolean refreshBackgrounds;
		
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {		
			int w= 0, h= -VERTICAL_SPACING;
			Control[] cs= composite.getChildren();
			for (int i= 0; i < cs.length; i++) {
				Control c= cs[i];
				Point e= c.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
				w= Math.max(w, e.x);
				h+= e.y+VERTICAL_SPACING;
			}
			return new Point(w, h);
		}
		
		protected void layout(Composite composite, boolean flushCache) {
			int x= 0, y= 0;
			Point e= composite.getSize();
			Control[] cs= getSortedChildren();
			boolean dark= (cs.length % 2) == 1;
			for (int i= 0; i < cs.length; i++) {
				Control c= cs[i];
				Point s= c.computeSize(e.x, SWT.DEFAULT, flushCache);
				c.setBounds(x, y, s.x, s.y);
				y+= s.y+VERTICAL_SPACING;
				if (refreshBackgrounds && c instanceof JobItem) {
					((JobItem)c).updateBackground(dark);
					dark= !dark;
				}
			}
		}
	}
	
	abstract class JobTreeItem extends Canvas implements Listener {
		JobTreeElement jobTreeElement;
		boolean jobTerminated;
		boolean keepItem;
		
		JobTreeItem(Composite parent, JobTreeElement info, int flags) {
			super(parent, flags);
			jobTreeElement= info;
			map.put(jobTreeElement, this);
			addListener(SWT.Dispose, this);
		}

		void init(JobTreeElement info) {
			map.remove(jobTreeElement);
			jobTreeElement= info;
			map.put(jobTreeElement, this);
			refresh();
		}
		
		void setKept() {
			if (!jobTerminated) {
				keepItem= jobTerminated= true;
				remove();	// bring to keep mode
			}
		}
		
		public void handleEvent(Event e) {
			switch (e.type) {
			case SWT.Dispose:
				map.remove(jobTreeElement);
				break;
			}
		}
		
		Job getJob() {
			if (jobTreeElement instanceof JobInfo)
				return ((JobInfo)jobTreeElement).getJob();
			if (jobTreeElement instanceof SubTaskInfo)
			    return ((SubTaskInfo)jobTreeElement).jobInfo.getJob();
			return null;
		}

		public boolean kill(boolean refresh, boolean broadcast) {
			return true;
		}
		
		boolean checkKeep() {
			if (jobTreeElement instanceof JobInfo && FinishedJobs.keep((JobInfo) jobTreeElement))
				setKeep();
			return keepItem;			
		}
		
		void setKeep() {
			//if (DEBUG) System.err.println("got keep for: " + jobTreeElement);
			keepItem= true;
			Composite parent= getParent();
			if (parent instanceof JobTreeItem) {
			    //if (DEBUG) System.err.println("propagating keep to: " + ((JobTreeItem)parent).jobTreeElement);
			    ((JobTreeItem)parent).keepItem= true;
			}			
		}
		
		abstract boolean refresh();
		
		public boolean remove() {
			if (DEBUG) System.err.println("  JobTreeItem.remove:"); //$NON-NLS-1$
			
			refresh();
			
			if (dialogContext || !keepItem) {
			    if (DEBUG) System.err.println("  JobItem.dispose:"); //$NON-NLS-1$
				dispose();
				return true;
			}
			return false;
		}
	}
	
	/*
	 * Label with hyperlink capability.
	 */
	class Hyperlink extends JobTreeItem implements Listener, IPropertyChangeListener {
		final static int MARGINWIDTH = 1;
		final static int MARGINHEIGHT = 1;
		
		boolean hasFocus;
		boolean mouseOver;
		boolean isError;
		boolean linkEnabled;
		boolean foundImage;
		String text= ""; //$NON-NLS-1$
		IAction gotoAction;
		IStatus result;
		JobItem jobitem;
		
		Hyperlink(JobItem parent, JobTreeElement info) {
			super(parent, info, SWT.NO_BACKGROUND);
			
			jobitem= parent;
			
 			setFont(smallerFont);
			
			addListener(SWT.KeyDown, this);
			addListener(SWT.Paint, this);
			addListener(SWT.MouseEnter, this);
			addListener(SWT.MouseExit, this);
			addListener(SWT.MouseDown, this);
			addListener(SWT.MouseUp, this);
			addListener(SWT.FocusIn, this);
			addListener(SWT.FocusOut, this);
			
 			refresh();
		}
		boolean isLinkEnabled() {
			return !dialogContext && linkEnabled;
		}
		public void handleEvent(Event e) {
			super.handleEvent(e);
			switch (e.type) {
			case SWT.Dispose:
				if (gotoAction != null) {
					gotoAction.removePropertyChangeListener(this);
					gotoAction= null;
				}			
				break;
			case SWT.KeyDown:
				//System.out.println("SWT.KeyDown3 " + this);
				if (e.character == '\r')
					handleActivate();
				else if (e.keyCode == SWT.DEL) {
					cancelSelection();
				} else {
					select(null, e);
				}
				break;
			case SWT.Paint:
				paint(e.gc);
				break;
			case SWT.FocusIn :
				hasFocus = true;
			case SWT.MouseEnter :
				if (isLinkEnabled()) {
					mouseOver = true;
					redraw();
				}
				break;
			case SWT.FocusOut :
				hasFocus = false;
			case SWT.MouseExit :
				if (isLinkEnabled()) {
					mouseOver = false;
					redraw();
				}
				break;
			case SWT.DefaultSelection :
				handleActivate();
				break;
			case SWT.MouseDown :
				if (!isLinkEnabled())
					select((JobItem) getParent(), e);
				break;
			case SWT.MouseUp :
				if (isLinkEnabled()) {
					Point size= getSize();
					if (e.button != 1 || e.x < 0 || e.y < 0 || e.x >= size.x || e.y >= size.y)
						return;
					handleActivate();
				}
				break;
			}
		}
		void setStatus(IStatus r) {
			result= r;
	    	if (result != null) {
	    		String message= result.getMessage().trim();
	    		if (message.length() > 0) {
	    			if (r.getSeverity() == IStatus.ERROR) {
	    				setKeep();
	    				isError= true;
	    				setText("Error: " + message);
		    			setAction(new Action() {
		    				public void run() {
		    					ErrorDialog.openError(getShell(), "Title", "Error", result);
		    				}
		    			});
	    			} else {
		    			setText(message);
	    			}
	    		}
	    	}
		}
		private void setText(String t) {
			if (t == null)
				t= "";	//$NON-NLS-1$
			if (!t.equals(text)) {
				if (gotoAction == null || gotoAction.getToolTipText() == null)
					setToolTipText(t);
				text= t;
				redraw();
			}
		}
		void setAction(IAction action) {
			if (action == gotoAction)
				return;
			if (gotoAction != null)
				gotoAction.removePropertyChangeListener(this);
			gotoAction= action;
			if (gotoAction != null) {
				gotoAction.addPropertyChangeListener(this);
				String tooltip= gotoAction.getToolTipText();
				if (tooltip != null)
					setToolTipText(tooltip);
			} else {
				setToolTipText(text);
			}
			setLinkEnable(action != null && action.isEnabled());
		}
		private void setLinkEnable(boolean enable) {
			if (enable != linkEnabled) {
				linkEnabled= enable;
				if (isLinkEnabled())
					setCursor(handCursor);
				redraw();
			}
		}
		public void propertyChange(PropertyChangeEvent event) {
		    if (DEBUG) System.err.println("action changed: " + gotoAction); //$NON-NLS-1$
		    if (gotoAction != null) {	    	
		    	getDisplay().asyncExec(new Runnable() {
		    		public void run() {
		    			if (!isDisposed())
		    				setLinkEnable(gotoAction.isEnabled());
		    		}
		    	});
		    }
		}
		public Point computeSize(int wHint, int hHint, boolean changed) {
			checkWidget();
			int innerWidth= wHint;
			if (innerWidth != SWT.DEFAULT)
				innerWidth -= MARGINWIDTH * 2;
			GC gc= new GC(this);
			gc.setFont(getFont());
			Point extent= gc.textExtent(text);
			gc.dispose();
			return new Point(extent.x + 2 * MARGINWIDTH, extent.y + 2 * MARGINHEIGHT);
		}
		private Color getFGColor() {
			if (jobitem.selected) 
				return selectedTextColor;

			if (isLinkEnabled()) {
				if (mouseOver) {
					if (isError)
						return errorColor2;
					return linkColor2;
				}
				if (isError)
					return errorColor;
				return linkColor;
			}
			return textColor;
		}
		protected void paint(GC gc) {
			Rectangle clientArea= getClientArea();
			Color fg= getFGColor(), bg= getBackground();
			if (jobitem.selected)
				bg= selectedColor;
			Image buffer= null;
			GC bufferGC= gc;
			if (!isCarbon) {
			    buffer= new Image(getDisplay(), clientArea.width, clientArea.height);
			    buffer.setBackground(bg);
				bufferGC= new GC(buffer, gc.getStyle());
			}
			bufferGC.setForeground(fg);
			bufferGC.setBackground(bg);
			bufferGC.fillRectangle(0, 0, clientArea.width, clientArea.height);
			bufferGC.setFont(getFont());
			String t= shortenText(bufferGC, clientArea.width, text);
			bufferGC.drawText(t, MARGINWIDTH, MARGINHEIGHT, true);
			int sw= bufferGC.stringExtent(t).x;
			if (isLinkEnabled()) {
				FontMetrics fm= bufferGC.getFontMetrics();
				int lineY= clientArea.height - MARGINHEIGHT - fm.getDescent() + 1;
				bufferGC.drawLine(MARGINWIDTH, lineY, MARGINWIDTH + sw, lineY);
				if (hasFocus)
					bufferGC.drawFocus(0, 0, sw, clientArea.height);
			}
			if (buffer != null) {
			    gc.drawImage(buffer, 0, 0);
			    bufferGC.dispose();
			    buffer.dispose();
			}
		}
		protected void handleActivate() {
			if (isLinkEnabled() && gotoAction != null && gotoAction.isEnabled())
				gotoAction.run();
		}
		public boolean refresh() {
			
			if (jobTreeElement == null)
				return false;
			
			Job job= getJob();
			checkKeep();

			String name= jobTreeElement.getDisplayString();
			
			if (jobTreeElement instanceof SubTaskInfo) {
				SubTaskInfo sti= (SubTaskInfo) jobTreeElement;
				if (sti.jobInfo != null) {
					TaskInfo ti= sti.jobInfo.getTaskInfo();
					if (ti != null) {
						String tn= ti.getTaskName();
						if (tn != null)
							name= tn + " / " + name;
					}		
				}
			} else if (jobTreeElement instanceof JobInfo) {
				JobInfo ji= (JobInfo) jobTreeElement;
				Object[] subtasks= ji.getChildren();
				if (subtasks != null && subtasks.length > 0) {
					JobTreeElement sub= (JobTreeElement) subtasks[0];
					name= name + " / " + sub.getDisplayString();
				}
				if (highlightJob == job) {
					highlightItem= jobitem;
				}
			}
			
			setText(name);
			
			if (job != null) {
				// check for icon property and propagate to parent
				if (jobitem.image == null)
					jobitem.updateIcon(job);

				Object property= job.getProperty(GOTO_PROPERTY);
		    	if (property instanceof IAction && property != gotoAction) {
		    	    setAction((IAction) property);
		    	}
		    	
		    	IStatus status= job.getResult();
		    	if (status != null)
		    		setStatus(status);
			}
			
			return false;
		}
	}		

	/*
	 * An SWT widget representing a JobModel
	 */
	class JobItem extends JobTreeItem {
		
		static final int MARGIN= 2;
		static final int HGAP= 7;
		static final int VGAP= 1;
		static final int MAX_PROGRESS_HEIGHT= 12;
		static final int MIN_ICON_SIZE= 16;

		int cachedWidth= -1;
		int cachedHeight= -1;
		private Image image;
		private boolean disposeImage;
		Label nameItem;
		Label iconItem;
		ProgressBar progressBar;
		ToolBar actionBar;
		ToolItem actionButton;
		ToolItem gotoButton;
		boolean selected;
		

		JobItem(Composite parent, JobTreeElement info) {
			super(parent, info, SWT.NONE);
			
			Assert.isNotNull(info);
						
			Display display= getDisplay();
						
			iconItem= new Label(this, SWT.NONE);
			iconItem.addListener(SWT.MouseDown, this);
			updateIcon(getJob());
			if (image == null)
				iconItem.setImage(defaultJobIcon);
			
			nameItem= new Label(this, SWT.NONE);
			nameItem.setFont(boldFont);
			nameItem.addListener(SWT.MouseDown, this);
			
			actionBar= new ToolBar(this, SWT.FLAT);
			actionBar.setCursor(normalCursor);	// set cursor to overwrite any busy cursor we might have
			actionButton= new ToolItem(actionBar, SWT.NONE);
			actionButton.setImage(cancelJobIcon); //$NON-NLS-1$
			actionButton.setToolTipText(ProgressMessages.getString("NewProgressView.CancelJobToolTip")); //$NON-NLS-1$
			actionButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					actionButton.setEnabled(false);
					cancelOrRemove();
				}
			});
			
			addListener(SWT.MouseDown, this);
			addListener(SWT.KeyDown, this);

			addControlListener(new ControlAdapter() {
				public void controlResized(ControlEvent e) {
					handleResize();
				}
			});
			
			refresh();
		}
		
		void updateIcon(Job job) {
			if (job != null) {
				Image im= null;
				boolean dispImage= false;
				Display display= getDisplay();
				Object property= job.getProperty(ICON_PROPERTY);
				if (property instanceof ImageDescriptor) {
					dispImage= true;
					im= ((ImageDescriptor) property).createImage(display);
				} else if (property instanceof URL) {
					dispImage= true;
					im= ImageDescriptor.createFromURL((URL) property).createImage(display);
				} else {
					dispImage= false;
					im= ProgressManager.getInstance().getIconFor(job);
				}
				if (im != null && im != image) {
				    if (disposeImage && image != null) {
				    	if (DEBUG) System.err.println("JobItem.setImage: disposed image");
				        image.dispose();
				    }
					image= im;
					disposeImage= dispImage;
					if (iconItem != null)
						iconItem.setImage(image);
				}
			}
		}
		
		boolean cancelOrRemove() {
			if (jobTerminated)
				return kill(true, true);
			jobTreeElement.cancel();
			return false;
		}

		public void handleEvent(Event event) {
	        switch (event.type) {
		    case SWT.Dispose:
		    	super.handleEvent(event);
        		if (disposeImage && image != null && !image.isDisposed()) {
        			if (DEBUG) System.err.println("JobItem.image disposed");
        		    image.dispose();
        		}
    		    image= null;
		        break;
		    case SWT.KeyDown:
				//System.err.println("KeyDown0 " + this);
				select(null, event);
		    		break;
		    case SWT.MouseDown:
		        //setFocus();
				select(JobItem.this, event);
	        	break;
	        default:
		        super.handleEvent(event);
	        	break;
	        }
	    }

		public boolean remove() {
			jobTerminated= true;
			
			if (dialogContext)
				return super.remove();
			
			checkKeep();
						
			if (keepItem) {
				boolean changed= false;
				if (progressBar != null && !progressBar.isDisposed()) {
				    progressBar.setSelection(100);
					progressBar.dispose();
					changed= true;
				}
				if (!actionButton.isDisposed()) {
					actionButton.setImage(clearJobIcon);
					actionButton.setToolTipText(ProgressMessages.getString("NewProgressView.RemoveJobToolTip")); //$NON-NLS-1$
					actionButton.setEnabled(true);
					changed= true;
				}
				
				changed |= refresh();

				IStatus result= getResult();
				if (result != null) {
					Control[] c= getChildren();
					for (int i= 0; i < c.length; i++) {
						if (c[i] instanceof Hyperlink) {
							Hyperlink hl= (Hyperlink) c[i];
							hl.setStatus(result);
							break;
						}
					}
				} else {
					Control[] c= getChildren();
					for (int i= 0; i < c.length; i++) {
						if (c[i] instanceof Hyperlink) {
							Hyperlink hl= (Hyperlink) c[i];
							hl.refresh();
						}
					}	
				}

				return changed;
			}
			return super.remove();
		}
		
		public boolean kill(boolean refresh, boolean broadcast) {
			if (jobTerminated) {
				
				if (broadcast)
					finishedJobs.remove(jobTreeElement);
				else {
					dispose();
					relayout(refresh, refresh);
					return true;
				}
			}
			return false;
		}
		
		void handleResize() {
			Point e= getSize();
			Point e1= iconItem.computeSize(SWT.DEFAULT, SWT.DEFAULT); e1.x= MIN_ICON_SIZE;
			Point e2= nameItem.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			Point e5= actionBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			
			int iw= e.x-MARGIN-HGAP-e5.x-MARGIN;
			int indent= 16+HGAP;
				
			int y= MARGIN;
			int h= Math.max(e1.y, e2.y);
			
			nameItem.setBounds(MARGIN+e1.x+HGAP, y+(h-e2.y)/2, iw-e1.x-HGAP, e2.y);
			y+= h;
			if (progressBar != null && !progressBar.isDisposed()) {
				Point e3= progressBar.computeSize(SWT.DEFAULT, SWT.DEFAULT); e3.y= MAX_PROGRESS_HEIGHT;
				y+= VGAP+1;
				progressBar.setBounds(MARGIN+indent, y, iw-indent, e3.y);
				y+= e3.y;
			}
			Control[] cs= getChildren();
			for (int i= 0; i < cs.length; i++) {
				if (cs[i] instanceof Hyperlink) {
					Point e4= cs[i].computeSize(SWT.DEFAULT, SWT.DEFAULT);
					y+= VGAP;
					cs[i].setBounds(MARGIN+indent, y, iw-indent, e4.y);
					y+= e4.y;
				}
			}
			
			int hm= (MARGIN+HGAP)/2;
			int vm= (y-e1.y)/2;
			if (hm < (y-e1.y)/2)
				vm= hm;
			iconItem.setBounds(hm, vm, e1.x, e1.y);
			
			actionBar.setBounds(e.x-MARGIN-e5.x, (e.y-e5.y)/2, e5.x, e5.y);
		}
		
		public Point computeSize(int wHint, int hHint, boolean changed) {
			
			if (changed || cachedHeight <= 0 || cachedWidth <= 0) {
				Point e1= iconItem.computeSize(SWT.DEFAULT, SWT.DEFAULT); e1.x= MIN_ICON_SIZE;
				Point e2= nameItem.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				
				cachedWidth= MARGIN + e1.x + HGAP + 100 + MARGIN;
					
				cachedHeight= MARGIN + Math.max(e1.y, e2.y);
				if (progressBar != null && !progressBar.isDisposed()) {
					cachedHeight+= 1;
					Point e3= progressBar.computeSize(SWT.DEFAULT, SWT.DEFAULT); e3.y= MAX_PROGRESS_HEIGHT;
					cachedHeight+= VGAP + e3.y;
				}
				Control[] cs= getChildren();
				for (int i= 0; i < cs.length; i++) {
					if (cs[i] instanceof Hyperlink) {
						Point e4= cs[i].computeSize(SWT.DEFAULT, SWT.DEFAULT);
						cachedHeight+= VGAP + e4.y;
					}
				}
				cachedHeight+= MARGIN;
			}
			
			int w= wHint == SWT.DEFAULT ? cachedWidth : wHint;
			int h= hHint == SWT.DEFAULT ? cachedHeight : hHint;
			
			return new Point(w, h);
		}
		
		/*
		 * Update the background colors.
		 */
		void updateBackground(boolean dark) {
			Color fg, bg;
			
			if (selected) {
				fg= selectedTextColor;
				bg= selectedColor;
			} else {
				if (highlightJob != null && (highlightJob == getJob() || highlightItem == this))
					fg= highlightColor;
				else
					fg= textColor;
				bg= dark ? darkColor : lightColor;
			}
			setForeground(fg);
			setBackground(bg);
			
			Control[] cs= getChildren();
			for (int i= 0; i < cs.length; i++) {
				if (!(cs[i] instanceof ProgressBar))
					cs[i].setForeground(fg);
				cs[i].setBackground(bg);
			}
		}
		
		boolean setPercentDone(int percentDone) {
			if (percentDone >= 0 && percentDone < 100) {
				if (progressBar == null) {
					progressBar= new ProgressBar(this, SWT.HORIZONTAL);
					progressBar.setMaximum(100);
					progressBar.setSelection(percentDone);
					progressBar.addListener(SWT.MouseDown, this);
					return true;
				} else if (!progressBar.isDisposed())
					progressBar.setSelection(percentDone);
			} else {
				if (progressBar == null) {
					progressBar= new ProgressBar(this, SWT.HORIZONTAL | SWT.INDETERMINATE);
					progressBar.addListener(SWT.MouseDown, this);
					return true;
				}
			}
			return false;
		}
		
		boolean isCanceled() {
			if (jobTreeElement instanceof JobInfo)
				return ((JobInfo)jobTreeElement).isCanceled();
			return false;
		}
	
		IStatus getResult() {
			//checkKeep();
			if (jobTerminated) {
				Job job= getJob();
				if (job != null)
			    	return job.getResult();
			}
			return null;
		}
		
		/*
		 * Update the visual item from the model.
		 */
		public boolean refresh() {

		    if (isDisposed())
		        return false;

			boolean changed= false;
		    boolean isGroup= jobTreeElement instanceof GroupInfo;
			Object[] roots= contentProviderGetChildren(jobTreeElement);
			Job job= getJob();
			
			// poll for properties
		    checkKeep();
		    if (image == null && job != null)
		    	updateIcon(job);
		    
			// name
		    String name= null;
		    if (isGroup) {
	    		name= stripPercent(jobTreeElement.getDisplayString());		    	
		    } else if (jobTreeElement instanceof JobInfo)
		    	name= getJobHeader((JobInfo) jobTreeElement, job);		    	
		    if (name == null)
		    	name= stripPercent(jobTreeElement.getDisplayString());
		    
			if (highlightJob != null && (highlightJob == job || highlightItem == this)) {
				name= name + " (Blocks User Operation)";
			}
		    nameItem.setToolTipText(name);
		    nameItem.setText(shortenText(nameItem, name));

			// percentage
			if (jobTreeElement instanceof JobInfo) {				
				TaskInfo ti= ((JobInfo)jobTreeElement).getTaskInfo();
				if (ti != null)
					changed |= setPercentDone(ti.getPercentDone());
			} else if (isGroup) {
		        if (roots.length == 1 && roots[0] instanceof JobTreeElement) {
					TaskInfo ti= ((JobInfo)roots[0]).getTaskInfo();
					if (ti != null)
						changed |= setPercentDone(ti.getPercentDone());
		        } else {
					GroupInfo gi= (GroupInfo) jobTreeElement;
					changed |= setPercentDone(gi.getPercentDone());		            
		        }
			}
			
			// children
		    if (!jobTreeElement.hasChildren())
		        return changed;
			
			Control[] children= getChildren();
			int n= 0;
			for (int i= 0; i < children.length; i++)
				if (children[i] instanceof Hyperlink)
					n++;
			
			if (roots.length == n) {	// reuse all children
				int z= 0;
				for (int i= 0; i < children.length; i++) {
					if (children[i] instanceof Hyperlink) {
						Hyperlink l= (Hyperlink) children[i];					
						l.init((JobTreeElement) roots[z++]);
					}
				}
			} else {
			
				HashSet modelJobs= new HashSet();
				for (int z= 0; z < roots.length; z++)
					modelJobs.add(roots[z]);
				
				// find all removed
				HashSet shownJobs= new HashSet();
				for (int i= 0; i < children.length; i++) {
					if (children[i] instanceof Hyperlink) {
						JobTreeItem ji= (JobTreeItem)children[i];
						shownJobs.add(ji.jobTreeElement);
						if (modelJobs.contains(ji.jobTreeElement)) {
							ji.refresh();
						} else {
							changed |= ji.remove();
						}
					}
				}
				
				// find all added
				for (int i= 0; i < roots.length; i++) {
					Object element= roots[i];
					if (!shownJobs.contains(element)) {
						JobTreeElement jte= (JobTreeElement)element;
						new Hyperlink(this, jte);
						changed= true;
					}
				}
			}
			
			return changed;
		}
		
		private String getJobHeader(JobInfo ji, Job job) {
			String name= job.getName();
			
			if (job.isSystem())
				name= ProgressMessages.format("JobInfo.System", new Object[]{name}); //$NON-NLS-1$
			
			if (ji.isCanceled())
				return ProgressMessages.format("JobInfo.Cancelled", new Object[]{name});  //$NON-NLS-1$

			if (jobTerminated) 
				return ProgressMessages.format("JobInfo.Finished", new Object[]{ name, getTimeString(ji) }); //$NON-NLS-1$
						
			if (ji.isBlocked()) {
				IStatus blockedStatus= ji.getBlockedStatus();
				return ProgressMessages.format("JobInfo.Blocked", //$NON-NLS-1$
						new Object[]{name, blockedStatus.getMessage()});
			}
			
			switch (job.getState()) {
			case Job.RUNNING:
				return name;
			case Job.SLEEPING:
				return ProgressMessages.format("JobInfo.Sleeping", new Object[]{name}); //$NON-NLS-1$
			default:		
				return ProgressMessages.format("JobInfo.Waiting", new Object[]{name}); //$NON-NLS-1$
			}
		}
	}

	private String getTimeString(JobTreeElement jte) {
    	Date date= finishedJobs.getFinishDate(jte);
    	if (date != null)
    		return DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
    	return "???";
	}
	
	private String stripPercent(String s) {
	    int l= s.length();
	    if (l > 0) {
	        if (s.charAt(0) == '(') {
	            int pos= s.indexOf("%) ");	//$NON-NLS-1$
	            if (pos >= 0)
	                s= s.substring(pos+3);
	        } else if (s.charAt(l-1) == ')') {
	            int pos= s.lastIndexOf(": (");	//$NON-NLS-1$
	            if (pos >= 0)
	                s= s.substring(0, pos);
	        }
	    }
	    return s;
	}

	/**
	 * Create a new ProgressViewer
	 */
    public NewProgressViewer(Composite parent, int flags) {
        super(parent, flags);
        Tree c = getTree();
        if (c instanceof Tree)
            c.dispose();
        
        dialogContext= (flags & SWT.BORDER) != 0;	// hack to determine context
        
        finishedJobs= FinishedJobs.getInstance();
        
	    progressManagerListener= new IJobProgressManagerListener() {
            public void addJob(JobInfo info) { }
            public void addGroup(GroupInfo info) { }
            public void refreshJobInfo(JobInfo info) { }
            public void refreshGroup(GroupInfo info) { }
            public void refreshAll() { }
            public void removeJob(JobInfo info) {
                forcedRemove(info);
            }
            public void removeGroup(GroupInfo group) {
                forcedRemove(group);
            }
            public boolean showsDebug() {
                return false;
            }
	    };
	    ProgressManager.getInstance().addListener(progressManagerListener);	
       
	    finishedJobs.addListener(this);
		
		Display display= parent.getDisplay();
		handCursor= new Cursor(display, SWT.CURSOR_HAND);
		normalCursor= new Cursor(display, SWT.CURSOR_ARROW);

		defaultJobIcon= ImageDescriptor.createFromFile(getClass(), "newprogress_circle.gif").createImage(display); //$NON-NLS-1$
		cancelJobIcon= ImageDescriptor.createFromFile(getClass(), "newprogress_cancel.gif").createImage(display); //$NON-NLS-1$
		clearJobIcon= ImageDescriptor.createFromFile(getClass(), "newprogress_clear.gif").createImage(display); //$NON-NLS-1$
		
		boldFont= defaultFont;
		FontData fds[]= defaultFont.getFontData();
		if (fds.length > 0) {
			FontData fd= fds[0];
			int h= fd.getHeight();
			if (isCarbon)
				h-=2;
			boldFont= new Font(display, fd.getName(), h, fd.getStyle() | SWT.BOLD);
			smallerFont= new Font(display, fd.getName(), h, fd.getStyle());
		}
		
		int shift= isCarbon ? -25 : -10; // Mac has different Gamma value
		lightColor= display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		darkColor= new Color(display, lightColor.getRed()+shift, lightColor.getGreen()+shift, lightColor.getBlue()+shift);
		textColor= display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);
		selectedTextColor= display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
		selectedColor= display.getSystemColor(SWT.COLOR_LIST_SELECTION);
		linkColor= display.getSystemColor(SWT.COLOR_DARK_BLUE);
		linkColor2= display.getSystemColor(SWT.COLOR_BLUE);
		errorColor= display.getSystemColor(SWT.COLOR_DARK_RED);
		errorColor2= display.getSystemColor(SWT.COLOR_RED);
		highlightColor= display.getSystemColor(SWT.COLOR_DARK_RED);
				
		scroller= new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | flags);
		int height= defaultFont.getFontData()[0].getHeight();
		scroller.getVerticalBar().setIncrement(height * 2);
		scroller.setExpandHorizontal(true);
		scroller.setExpandVertical(true);
					
		list= new Composite(scroller, SWT.NONE);
		list.setFont(defaultFont);
		list.setBackground(lightColor);
		list.setLayout(new ListLayout());
		
		list.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event event) {
				//System.err.println("Traverse");
				//select(null, event);
			}			
		});
		
		list.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				select(null, event);	// clear selection
			}			
		});

		scroller.addListener(SWT.Dispose, new Listener() {
			public void handleEvent(Event event) {
				finishedJobs.removeListener(NewProgressViewer.this);
			    ProgressManager.getInstance().removeListener(progressManagerListener);
			    
				defaultJobIcon.dispose();
				cancelJobIcon.dispose();
				clearJobIcon.dispose();
				
				handCursor.dispose();
				normalCursor.dispose();
				
				if (boldFont != defaultFont)
					boldFont.dispose();
				if (smallerFont != defaultFont)
					smallerFont.dispose();
				
				darkColor.dispose();
			}			
		});

		scroller.setContent(list);
		
		// refresh UI
		refresh(true);
    }

    public Control getControl() {
        return scroller;
    }

    /**
     * Returns true if given element is filtered (i.e. not shown).
     */
    private boolean filtered(Object element) {
    	
    	if (element == null)
    		return true;
    	
		if (!dialogContext && ProgressViewUpdater.getSingleton().debug) // display all  in debug mode
			return false;
		
    	if (element instanceof JobInfo)
    		return jobFiltered((JobInfo) element);
    	
    	if (element instanceof GroupInfo) {
    		Object[] children = ((GroupInfo) element).getChildren();
    		for (int i= 0; i< children.length; i++)
    			if (jobFiltered((JobInfo) children[i]))
    				return true;
    	}
    	
    	if (element instanceof TaskInfo) {
    		Object parent = ((TaskInfo) element).getParent();
    		if (parent instanceof JobInfo)
    			return jobFiltered((JobInfo) parent);
    	}

    	return false;
    }
    
    private boolean jobFiltered(JobInfo ji) {
		Job job= ji.getJob();
    	if (job != null && job == highlightJob)
    		return false;
		if (job == null || job.isSystem() || job.getState() == Job.SLEEPING)
			return true;	
    	return false;
    }
    
 	public void add(Object parentElement, Object[] elements) {
	    if (DEBUG) System.err.println("add"); //$NON-NLS-1$
 	    if (list.isDisposed())
 	        return;
 	    JobTreeItem lastAdded= null;
		for (int i= 0; i < elements.length; i++)
			if (!filtered(elements[i]))
				lastAdded= findJobItem(elements[i], true);
		relayout(true, true);
		if (lastAdded != null)
			reveal(lastAdded);
	}
 	
	public void remove(Object[] elements) {
 	    if (list.isDisposed())
 	        return;
 	    if (DEBUG) System.err.println("remove"); //$NON-NLS-1$
		boolean changed= false;
		for (int i= 0; i < elements.length; i++) {
			JobTreeItem ji= findJobItem(elements[i], false);
			if (ji != null)
				changed |= ji.remove();
		}
		relayout(changed, changed);
	}
	
	public void refresh(Object element, boolean updateLabels) {
 	    if (list.isDisposed())
 	        return;
 	    if (filtered(element))
 	    	return;
 	    JobTreeItem ji= findJobItem(element, true);
		if (ji != null && ji.refresh())
			relayout(true, true);
	}
	
	public void refresh(boolean updateLabels) {
	    if (list.isDisposed())
	        return;
	    if (DEBUG) System.err.println("refreshAll"); //$NON-NLS-1$
		boolean changed= false;
		boolean countChanged= false;
		JobTreeItem lastAdded= null;
		
		Object[] roots= contentProviderGetRoots(getInput());
		HashSet modelJobs= new HashSet();
		for (int z= 0; z < roots.length; z++)
			modelJobs.add(roots[z]);
				
		// find all removed
		Control[] children= list.getChildren();
		for (int i= 0; i < children.length; i++) {
			JobItem ji= (JobItem)children[i];
			if (modelJobs.contains(ji.jobTreeElement)) {
				if (DEBUG) System.err.println("  refresh"); //$NON-NLS-1$
				changed |= ji.refresh();
			} else {
				if (DEBUG) System.err.println("  remove: " + ji.jobTreeElement); //$NON-NLS-1$
				countChanged= true;
				changed |= ji.remove();
			}
		}
		
		// find all added
		for (int i= 0; i < roots.length; i++) {
			Object element= roots[i];
			if (filtered(element))
				continue;
			if (findJobItem(element, false) == null) {
				if (DEBUG) System.err.println("  added"); //$NON-NLS-1$
			    lastAdded= createItem(element);
				changed= countChanged= true;
			}
		}
	    // now add kept finished jobs
		if (!dialogContext) {
			JobTreeElement[] infos= finishedJobs.getJobInfos();
			for (int i= 0; i < infos.length; i++) {
				Object element= infos[i];
				if (filtered(element))
					continue;
				JobTreeItem jte= findJobItem(element, true);
				if (jte != null) {
					jte.setKept();
					lastAdded= jte;
					
					if (jte instanceof Hyperlink) {
						JobItem p= (JobItem) jte.getParent();
						p.setKept();
						lastAdded= p;
					}
					
					changed= countChanged= true;
				}
			}
		}
		
		relayout(changed, countChanged);
		if (lastAdded != null)
			reveal(lastAdded);
	}
	
	private JobItem createItem(Object element) {
		return new JobItem(list, (JobTreeElement) element);
	}
	
	private JobTreeItem findJobItem(Object element, boolean create) {
		JobTreeItem ji= (JobTreeItem) map.get(element);
		if (ji == null && create) {
			JobTreeElement jte= (JobTreeElement) element;
			Object parent= jte.getParent();
			if (parent != null) {
				JobTreeItem parentji= findJobItem(parent, true);
				if (parentji instanceof JobItem && !(jte instanceof TaskInfo)) {
					if (findJobItem(jte, false) == null)
						ji= new Hyperlink((JobItem)parentji, jte);
				}
			} else {
				ji= createItem(jte);
			}
		}
		return ji;
	}	
		
	public void reveal(JobTreeItem jti) {
		if (jti != null && !jti.isDisposed()) {
			
			Rectangle bounds= jti.getBounds();
			
			int s= bounds.y;
			int e= bounds.y + bounds.height;
			
			int as= scroller.getOrigin().y;
			int ae= as + scroller.getClientArea().height;
			
			if (s < as)
				scroller.setOrigin(0, s);
			else if (e > ae)
				scroller.setOrigin(0, as+(e-ae));
		}
	}

	/*
	 * Needs to be called after items have been added or removed,
	 * or after the size of an item has changed.
	 * Optionally updates the background of all items.
	 * Ensures that the background following the last item is always white.
	 */
	private void relayout(boolean layout, boolean refreshBackgrounds) {
		if (layout) {
			ListLayout l= (ListLayout) list.getLayout();
			l.refreshBackgrounds= refreshBackgrounds;
			Point size= list.computeSize(list.getClientArea().x, SWT.DEFAULT);
			list.setSize(size);
			scroller.setMinSize(size);
		}
	}
	
	void clearAll() {
		Control[] children= list.getChildren();
		boolean changed= false;
		for (int i= 0; i < children.length; i++)
			changed |= ((JobItem)children[i]).kill(false, true);
		relayout(changed, changed);
		
		if (DEBUG) {
			JobTreeElement[] elements = finishedJobs.getJobInfos();
			System.out.println("jobs: " + elements.length);
			for (int i= 0; i < elements.length; i++)
				System.out.println("  " + elements[i]);
		}
	}
	
	private Control[] getSortedChildren() {
		Control[] cs= list.getChildren();
		ViewerSorter vs= getSorter();
		if (vs != null) {
			HashMap map2= new HashMap();	// temp remember items for sorting
			JobTreeElement[] elements= new JobTreeElement[cs.length];
			for (int i= 0; i < cs.length; i++) {
				JobItem ji= (JobItem)cs[i];
				elements[i]= ji.jobTreeElement;
				map2.put(elements[i], ji);
			}
			vs.sort(NewProgressViewer.this, elements);
			for (int i= 0; i < cs.length; i++)
				cs[i]= (JobItem) map2.get(elements[i]);
		}
		return cs;
	}
	
	private void select(JobItem newSelection, Event e) {

		boolean clearAll= false;
		JobItem newSel= null;
		Control[] cs= getSortedChildren();		

		JobTreeElement element= null;
		if (newSelection != null)
			element= newSelection.jobTreeElement;
		
		if (e.type == SWT.KeyDown) { // key
			if (e.keyCode == SWT.ARROW_UP) {
				for (int i= 0; i < cs.length; i++) {
					JobItem ji= (JobItem) cs[i];
					if (ji.selected) {
						if (i-1 >= 0) {
							newSel= (JobItem) cs[i-1];
							if ((e.stateMask & SWT.MOD2) != 0) {
								newSel.selected= true;
							} else {
								clearAll= true;
							}
							break;
						}
						return;
					}
				}
			} else if (e.keyCode == SWT.ARROW_DOWN) {
				for (int i= cs.length-1; i >= 0; i--) {
					JobItem ji= (JobItem) cs[i];
					if (ji.selected) {
						if (i+1 < cs.length) {
							newSel= (JobItem) cs[i+1];
							if ((e.stateMask & SWT.MOD2) != 0) {
								newSel.selected= true;
							} else {
								clearAll= true;
							}
							break;
						}
						return;
					}
				}
			}
		} else if (e.type == SWT.MouseDown) {	// mouse
			
			if (newSelection == null) {
				clearAll= true;
			} else {			
				if ((e.stateMask & SWT.MOD1) != 0) {
					newSelection.selected= !newSelection.selected;
				} else if ((e.stateMask & SWT.MOD2) != 0) {
					
					
					
					//System.out.println("MOD2");
				} else {
					if (newSelection.selected)
						return;
					clearAll= true;
					newSel= newSelection;
				}
			}
		}
		
		if (clearAll) {
			for (int i= 0; i < cs.length; i++) {
				JobItem ji= (JobItem) cs[i];
				ji.selected= ji == newSel;
			}			
		}
		
		boolean dark= (cs.length % 2) == 1;
		for (int i= 0; i < cs.length; i++) {
			JobItem ji= (JobItem) cs[i];
			ji.updateBackground(dark);
			dark= !dark;
		}
		
		if (newSel != null)
			reveal(newSel);
	}
	
	/**
	 * Shorten the given text <code>t</code> so that its length
	 * doesn't exceed the given width. This implementation
	 * replaces characters in the center of the original string with an
	 * ellipsis ("...").
	 */
	static String shortenText(GC gc, int maxWidth, String textValue) {
		if (gc.textExtent(textValue).x < maxWidth) {
			return textValue;
		}
		int length = textValue.length();
		int ellipsisWidth = gc.textExtent(ELLIPSIS).x;
		int pivot = length / 2;
		int start = pivot;
		int end = pivot + 1;
		while (start >= 0 && end < length) {
			String s1 = textValue.substring(0, start);
			String s2 = textValue.substring(end, length);
			int l1 = gc.textExtent(s1).x;
			int l2 = gc.textExtent(s2).x;
			if (l1 + ellipsisWidth + l2 < maxWidth) {
				return s1 + ELLIPSIS + s2;
			}
			start--;
			end++;
		}
		return textValue;
	}
	/**
	 * Shorten the given text <code>t</code> so that its length
	 * doesn't exceed the width of the given control. This implementation
	 * replaces characters in the center of the original string with an
	 * ellipsis ("...").
	 */
	static String shortenText(Control control, String textValue) {
		if (textValue != null) {
			Display display = control.getDisplay();
			GC gc = new GC(display);
			int maxWidth = control.getBounds().width;
			textValue = shortenText(gc, maxWidth, textValue);
			gc.dispose();
		}
		return textValue;
	}
	
	Object[] contentProviderGetChildren(Object parent) {
		IContentProvider provider = getContentProvider();
		if (provider instanceof ITreeContentProvider)
			return ((ITreeContentProvider)provider).getChildren(parent);
		return new Object[0];
	}

	Object[] contentProviderGetRoots(Object parent) {
//		ArrayList tmp= new ArrayList();
//		if (highlightJob != null) {
//			JobInfo ji= ProgressManager.getInstance().getJobInfo(highlightJob);
//			if (ji != null)
//				tmp.add(ji);
//		}
//		IContentProvider provider = getContentProvider();
//		if (provider instanceof ITreeContentProvider) {
//			Object[] a= ((ITreeContentProvider)provider).getElements(parent);
//			for (int i= 0; i < a.length; i++)
//				tmp.add(a[i]);
//		}
//		return tmp.toArray();
		IContentProvider provider = getContentProvider();
		if (provider instanceof ITreeContentProvider)
			return ((ITreeContentProvider)provider).getElements(parent);
		return new Object[0];
	}
	
	private void forcedRemove(final JobTreeElement jte) {
		if (list != null && !list.isDisposed()) {
			list.getDisplay().asyncExec(new Runnable() {
	            public void run() {
	                if (DEBUG) System.err.println("  forced remove"); //$NON-NLS-1$
	        			JobTreeItem ji= findJobItem(jte, false);
	                if (ji != null && !ji.isDisposed() && ji.remove())
	                    relayout(true, true);
	            }
	        });
		}	    
	}

    /* (non-Javadoc)
     * @see org.eclipse.ui.internal.progress.NewKeptJobs.KeptJobsListener#finished(org.eclipse.ui.internal.progress.JobInfo)
     */
    public void finished(JobTreeElement jte) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.internal.progress.NewKeptJobs.KeptJobsListener#removed(org.eclipse.ui.internal.progress.JobInfo)
     */
    public void removed(JobTreeElement info) {
		final JobTreeItem ji= findJobItem(info, false);
		if (ji != null) {
	        ji.getDisplay().asyncExec(new Runnable() {
	            public void run() {
	                ji.kill(true, false);
	            }
	        });
		}
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.internal.progress.FinishedJobs.KeptJobsListener#infoVisited()
	 */
	public void infoVisited() {
    	// we should not have to do anything here
	}
	
	////// SelectionProvider

    public ISelection getSelection() {
    	ArrayList l= new ArrayList();
		Control[] cs= list.getChildren();
		for (int i= 0; i < cs.length; i++) {
			JobItem ji= (JobItem) cs[i];
			l.add(ji.jobTreeElement);
		} 
    	return new StructuredSelection(l);
    }

    public void setSelection(ISelection selection) {
    }

    public void setUseHashlookup(boolean b) {
    }

    public void setInput(IContentProvider provider) {
    	refresh(true);
    }
    
	public void cancelSelection() {
		boolean changed= false;
		Control[] cs= list.getChildren();
		for (int i= 0; i < cs.length; i++) {
			JobItem ji= (JobItem) cs[i];
			if (ji.selected)
				changed |= ji.cancelOrRemove();
		}
		relayout(changed, changed);
	}
    
    ///////////////////////////////////

    protected void addTreeListener(Control c, TreeListener listener) {
    }

    protected void doUpdateItem(final Item item, Object element) {
    }

    protected Item[] getChildren(Widget o) {
        return new Item[0];
    }

    protected boolean getExpanded(Item item) {
        return true;
    }

    protected Item getItem(int x, int y) {
        return null;
    }

    protected int getItemCount(Control widget) {
        return 1;
    }

    protected int getItemCount(Item item) {
        return 0;
    }

    protected Item[] getItems(Item item) {
        return new Item[0];
    }

    protected Item getParentItem(Item item) {
        return null;
    }

    protected Item[] getSelection(Control widget) {
        return new Item[0];
    }

    public Tree getTree() {
        Tree t= super.getTree();
        if (t != null && !t.isDisposed())
            return t;
        return null;
    }

    protected Item newItem(Widget parent, int flags, int ix) {
        return null;
    }

    protected void removeAll(Control widget) {
    }

    protected void setExpanded(Item node, boolean expand) {
    }

    protected void setSelection(List items) {
    }

    protected void showItem(Item item) {
    }
    
	protected void createChildren(Widget widget) {
		refresh(true);
	}
	
	protected void internalRefresh(Object element, boolean updateLabels) {
	}

	public void setHighlightJob(Job job) {
		highlightJob= job;
		relayout(true, true);
	}
}