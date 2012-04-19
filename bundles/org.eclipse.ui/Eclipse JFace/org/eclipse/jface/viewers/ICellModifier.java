package org.eclipse.jface.viewers;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
  
/**
 * A cell modifier is used to access the data model from a cell
 * editor in an abstract way. It offers methods to:
 * <ul>
 *	<li>to check if a a model element's property can be edited or not</li>
 *	<li>retrieve a value a model element's property</li>
 *	<li>to store a cell editor's value back into the model 
 *    element's property</li>
 * </ul>
 * <p>
 * This interface should be implemented by classes that wish to
 * act as cell modifiers.
 * </p>
 */
public interface ICellModifier {
/**
 * Checks whether the given property of the given element can be 
 * modified.
 *
 * @param element the element
 * @param property the property
 * @return <code>true</code> if the property can be modified,
 *   and <code>false</code> if it is not modifiable
 */
public boolean canModify(Object element, String property);
/**
 * Returns the value for the given property of the given element.
 * Returns <code>null</code> if the element does not have the given property.
 *
 * @param element the element
 * @param property the property
 * @return the property value
 */
public Object getValue(Object element, String property);
/**
 * Modifies the value for the given property of the given element.
 * Has no effect if the element does not have the given property,
 * or if the property cannot be modified.
 *
 * @param element the element
 * @param property the property
 * @param value the new property value
 */
public void modify(Object element, String property, Object value);
}