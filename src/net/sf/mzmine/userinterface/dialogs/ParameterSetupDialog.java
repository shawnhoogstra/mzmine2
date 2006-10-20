/*
 * Copyright 2006 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.userinterface.dialogs;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.Frame;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterValue;
import net.sf.mzmine.data.impl.SimpleParameterValue;
import net.sf.mzmine.data.impl.SimpleParameterValueInvalidValueException;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.project.MZmineProject;



/**
 * This class represents the parameter setup dialog shown to the user before processing
 */
public class ParameterSetupDialog extends JDialog implements ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	// Array for Text fields
	private JFormattedTextField[] textFields;

	// Number formatting used in text fields
	private NumberFormat decimalNumberFormat;

	// Labels
	private JLabel[] labels;

	// Parameters and their representation in the dialog
	private Hashtable<Parameter, Component> parametersAndComponents;
	
	// Buttons
	private JButton btnOK;
	private JButton btnCancel;

	// Panels for all above
	private JPanel pnlAll;
	private JPanel pnlLabels;
	private JPanel pnlFields;
	private JPanel pnlButtons;

	private MethodParameters parameters;
	
	// Exit code for controlling ok/cancel response
	private int exitCode = -1;


	/**
	 * Constructor
	 */
	public ParameterSetupDialog(Frame owner, String title, MethodParameters methodParameters) {

		// Make dialog modal
		super(owner, true);
		
		this.parameters = methodParameters;
		
		exitCode = -1;

		// Check if there are any parameters
		Parameter[] allParameters = methodParameters.getParameters();
		if ( (allParameters==null) || (allParameters.length==0) ) {
			exitCode = 1;
			dispose();		
		}
		
		// Allocate arrays
		textFields = new JFormattedTextField[methodParameters.getParameters().length];
		labels = new JLabel[methodParameters.getParameters().length];
		parametersAndComponents = new Hashtable<Parameter, Component>();

		// Panel where everything is collected
		pnlAll = new JPanel(new BorderLayout());
		pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		getContentPane().add(pnlAll);

		// Two more panels: one for labels and another for text fields
		pnlLabels = new JPanel(new GridLayout(0,1));
		pnlFields = new JPanel(new GridLayout(0,1));
		
		pnlFields.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
		pnlLabels.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

		// Setup number format for text fields
		decimalNumberFormat = NumberFormat.getNumberInstance();
		decimalNumberFormat.setMinimumFractionDigits(1);

		// Create labels and components for each parameter
		MZmineProject project = MZmineProject.getCurrentProject();
		for (int i=0; i<methodParameters.getParameters().length; i++) {
			Parameter p = methodParameters.getParameters()[i];

			// Create label
			String labelString = p.getName();
			if ((p.getUnits()!=null) && (p.getUnits().length()>0)) labelString = labelString.concat(" (" + p.getUnits() + ")");
			JLabel lbl = new JLabel(labelString);
			
			pnlLabels.add(lbl);
			
			switch (p.getType()) {
			case INTEGER:
			case DOUBLE:
			case STRING:
			default:
				Parameter numberFormatParameter = p.getNumberFormatParameter();
				NumberFormat numberFormat = null;
				if (numberFormatParameter!=null) numberFormat = (NumberFormat)project.getParameterValue(numberFormatParameter).getValue();
				JFormattedTextField txtField = new JFormattedTextField(numberFormat);
				ParameterValue parameterValue = parameters.getParameterValue(p);
				Object value=null;
				if (parameterValue!=null) value = parameterValue.getValue();
				else value = p.getDefaultValue();
				txtField.setValue(value);
				txtField.setColumns(8);
				txtField.setToolTipText(p.getDescription());
				parametersAndComponents.put(p, txtField);
				
				lbl.setLabelFor(txtField);
				
				pnlFields.add(txtField);
				
				break;
				
			case BOOLEAN:
				JCheckBox checkBox = new JCheckBox();
				checkBox.setSelected(parameters.getParameterValue(p).getBooleanValue());
				checkBox.setToolTipText(p.getDescription());
				parametersAndComponents.put(p, checkBox);
				
				lbl.setLabelFor(checkBox);
				
				pnlFields.add(checkBox);
				
				break;
				
			case OBJECT:
				JComboBox cmbPossibleValues = new JComboBox(p.getPossibleValues());
				parameterValue = parameters.getParameterValue(p);
				value=null;
				if (parameterValue!=null) value = parameterValue.getValue();
				else value = p.getDefaultValue();
				cmbPossibleValues.setSelectedItem(value);
				cmbPossibleValues.setToolTipText(p.getDescription());
				parametersAndComponents.put(p, cmbPossibleValues);
				
				lbl.setLabelFor(cmbPossibleValues);

 				pnlFields.add(cmbPossibleValues);
 				
 				break;
				
			}
			
		}

		// Buttons
		pnlButtons = new JPanel();
		btnOK = new JButton("OK");
		btnOK.addActionListener(this);
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(this);
		pnlButtons.add(btnOK);
		pnlButtons.add(btnCancel);

		pnlAll.add(pnlLabels,BorderLayout.CENTER);
		pnlAll.add(pnlFields,BorderLayout.LINE_END);
		pnlAll.add(pnlButtons,BorderLayout.SOUTH);

		getContentPane().add(pnlAll);

		setTitle(title);

		pack();

		setLocationRelativeTo(owner);
	}

	/**
	 * Implementation for ActionListener interface
	 */
	public void actionPerformed(java.awt.event.ActionEvent ae) {
		Object src = ae.getSource();
		if (src==btnOK) {
			
			// Copy values from form, validate them, and set them to project
			Enumeration<Parameter> paramEnum = parametersAndComponents.keys();
			MZmineProject project = MZmineProject.getCurrentProject();
			while (paramEnum.hasMoreElements()) {
				Parameter p = paramEnum.nextElement();
				Component c = parametersAndComponents.get(p);
				Object value=null;
				
				switch (p.getType()) {
				case INTEGER:
					JFormattedTextField txtField = (JFormattedTextField)parametersAndComponents.get(p); 
					try { value = (Integer)txtField.getValue(); }
					catch (Exception e) { e.printStackTrace(); displayMessage("Invalid parameter value for " + p.getName()); return;	}
					break;
				case DOUBLE:
					txtField = (JFormattedTextField)parametersAndComponents.get(p);
					try { value = (Double)txtField.getValue(); } 
					catch (Exception e) { e.printStackTrace(); displayMessage("Invalid parameter value for " + p.getName()); return;	}
					break;
				case STRING:
					txtField = (JFormattedTextField)parametersAndComponents.get(p); 
					try { value = (String)txtField.getValue(); }
					catch (Exception e) { e.printStackTrace(); displayMessage("Invalid parameter value for " + p.getName()); return;	}
					break;
				case BOOLEAN:
					JCheckBox checkBox = (JCheckBox)parametersAndComponents.get(p);
					if (checkBox.isSelected()) value = new Boolean(true); else value = new Boolean(false);
					break;
				case OBJECT:
					JComboBox comboBox = (JComboBox)parametersAndComponents.get(p);
					value = comboBox.getSelectedItem();
					break;
				}

				SimpleParameterValue parameterValue = null;
				try {
					parameterValue = new SimpleParameterValue(p, value);
				} catch (SimpleParameterValueInvalidValueException invalidValueException) {				
					displayMessage(invalidValueException.getMessage());
					return;
				} 
				if (parameterValue!=null) { 
					parameters.setParameterValue(p, parameterValue);
					project.setParameterValue(p, parameterValue);
				}
				
			}
			
			
			exitCode = 1;
			dispose();
			//setVisible(false);
		}
		if (src==btnCancel) {
			exitCode = -1;
			dispose();
			//setVisible(false);
		}
	}

	
	private void displayMessage(String msg) {
		try {
			logger.info(msg);
			JOptionPane.showMessageDialog(
										this,
										msg,
										"Error",
										JOptionPane.ERROR_MESSAGE
									 );
		} catch (Exception exce ) {}
	}
	
	/**
	 * Method for reading exit code
	 * @return	1=OK clicked, -1=cancel clicked
	 */
	public int getExitCode() {
		return exitCode;
	}

}
