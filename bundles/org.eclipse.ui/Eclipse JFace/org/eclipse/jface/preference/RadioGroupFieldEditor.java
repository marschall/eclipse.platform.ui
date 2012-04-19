package org.eclipse.jface.preference;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.text.MessageFormat;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.util.Assert;

/**
 * A field editor for an enumeration type preference.
 * The choices are presented as a list of radio buttons.
 */
public class RadioGroupFieldEditor extends FieldEditor {

	/**
	 * List of radio button entries of the form [label,value].
	 */
	private String[][] labelsAndValues;

	/**
	 * Number of columns into which to arrange the radio buttons.
	 */
	private int numColumns;

	/**
	 * Indent used for the first column of the radion button matrix.
	 */
	private int indent = HORIZONTAL_GAP;

	/**
	 * The current value, or <code>null</code> if none.
	 */
	private String value;

	/**
	 * The box of radio buttons, or <code>null</code> if none
	 * (before creation and after disposal).
	 */
	private Composite radioBox;

	/**
	 * The radio buttons, or <code>null</code> if none
	 * (before creation and after disposal).
	 */
	private Button[] radioButtons;
/**
 * Creates a new radio group field editor 
 */
protected RadioGroupFieldEditor() {
}
/**
 * Creates a radio group field editor.
 * <p>
 * Example usage:
 * <pre>
 *		RadioGroupFieldEditor editor= new RadioGroupFieldEditor(
 *			"GeneralPage.DoubleClick", resName, 1,
 *			new String[][] {
 *				{"Open Browser", "open"},
 *				{"Expand Tree", "expand"}
 *			},
 *          parent);	
 * </pre>
 * </p>
 * 
 * @param name the name of the preference this field editor works on
 * @param labelText the label text of the field editor
 * @param numColumns the number of columns for the radio button presentation
 * @param labelAndValues list of radio button [label, value] entries;
 *  the value is returned when the radio button is selected
 * @param parent the parent of the field editor's control
 */
public RadioGroupFieldEditor(String name, String labelText, int numColumns, String[][] labelAndValues, Composite parent) {
	init(name, labelText);
	Assert.isTrue(checkArray(labelAndValues));
	this.labelsAndValues = labelAndValues;
	this.numColumns = numColumns;
	createControl(parent);
}
/* (non-Javadoc)
 * Method declared on FieldEditor.
 */
protected void adjustForNumColumns(int numColumns) {
	Control control = getLabelControl();
	((GridData)control.getLayoutData()).horizontalSpan = numColumns;
	((GridData)radioBox.getLayoutData()).horizontalSpan = numColumns;
}
/**
 * Checks whether given <code>String[][]</code> is of "type" 
 * <code>String[][2]</code>.
 *
 * @return <code>true</code> if it is ok, and <code>false</code> otherwise
 */
private boolean checkArray(String[][] table) {
	if (table == null)
		return false;
	for (int i = 0; i < table.length; i++) {
		String[] array = table[i];
		if (array == null || array.length != 2)
			return false;
	}
	return true;
}
/* (non-Javadoc)
 * Method declared on FieldEditor.
 */
protected void doFillIntoGrid(Composite parent, int numColumns) {
	Control control = getLabelControl(parent);
	GridData gd = new GridData();
	gd.horizontalSpan = numColumns;
	control.setLayoutData(gd);

	radioBox = getRadioBoxControl(parent);
	gd = new GridData();
	gd.horizontalSpan = numColumns;
	gd.horizontalIndent = indent;
	radioBox.setLayoutData(gd);
}
/* (non-Javadoc)
 * Method declared on FieldEditor.
 */
protected void doLoad() {
	updateValue(getPreferenceStore().getString(getPreferenceName()));
}
/* (non-Javadoc)
 * Method declared on FieldEditor.
 */
protected void doLoadDefault() {
	updateValue(getPreferenceStore().getDefaultString(getPreferenceName()));
}
/* (non-Javadoc)
 * Method declared on FieldEditor.
 */
protected void doStore() {
	if (value == null) {
		getPreferenceStore().setToDefault(getPreferenceName());
		return;
	}

	getPreferenceStore().setValue(getPreferenceName(), value);
}
/* (non-Javadoc)
 * Method declared on FieldEditor.
 */
public int getNumberOfControls() {
	return 1;
}
/**
 * Returns this field editor's radio group control.
 *
 * @return the radio group control
 */
public Composite getRadioBoxControl(Composite parent) {
	if (radioBox == null) {
		radioBox = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = HORIZONTAL_GAP;
		layout.numColumns = numColumns;
		radioBox.setLayout(layout);

		radioButtons = new Button[labelsAndValues.length];
		for (int i = 0; i < labelsAndValues.length; i++) {
			Button radio = new Button(radioBox, SWT.RADIO | SWT.LEFT);
			radioButtons[i] = radio;
			String[] labelAndValue = labelsAndValues[i];
			radio.setText(labelAndValue[0]);
			radio.setData(labelAndValue[1]);
			radio.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					String oldValue = value;
					value = (String) event.widget.getData();
					setPresentsDefaultValue(false);
					fireValueChanged(VALUE, oldValue, value);
				}
			});
		}
		radioBox.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent event) {
				radioBox = null;
				radioButtons = null;
			}
		});
	} else {
		checkParent(radioBox, parent);
	}
	return radioBox;
}
/**
 * Sets the indent used for the first column of the radion button matrix.
 *
 * @param indent the indent (in pixels)
 */
public void setIndent(int indent) {
	if (indent < 0)
		this.indent = 0;
	else
		this.indent = indent;
}
/**
 * Select the radio button that conforms to the given value.
 *
 * @param selectedValue the selected value
 */
private void updateValue(String selectedValue) {
	this.value = selectedValue;
	if (radioButtons == null)
		return;

	if (this.value != null) {
		boolean found = false;
		for (int i = 0; i < radioButtons.length; i++) {
			Button radio = radioButtons[i];
			boolean selection = false;
			if (((String) radio.getData()).equals(this.value)) {
				selection = true;
				found = true;
			}
			radio.setSelection(selection);
		}
		if (found)
			return;
	}

	// We weren't able to find the value. So we select the first
	// radio button as a default.
	if (radioButtons.length > 0) {
		radioButtons[0].setSelection(true);
		this.value = (String) radioButtons[0].getData();
	}
	return;
}
}