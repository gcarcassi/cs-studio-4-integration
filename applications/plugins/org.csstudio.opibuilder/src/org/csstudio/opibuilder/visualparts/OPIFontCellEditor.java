package org.csstudio.opibuilder.visualparts;

import org.csstudio.opibuilder.util.OPIFont;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * The cell editor for OPIFont
 * @author Xihui Chen
 *
 */
public class OPIFontCellEditor extends AbstractDialogCellEditor {

	private OPIFont opiFont;
	
	
	public OPIFontCellEditor(Composite parent, String title) {
		super(parent, title);
	}

	@Override
	protected void openDialog(Shell parentShell, String dialogTitle) {
		OPIFontDialog dialog = 
			new OPIFontDialog(parentShell, opiFont, dialogTitle);
		if(dialog.open() == Window.OK)
			opiFont = dialog.getOutput();
	}

	@Override
	protected boolean shouldFireChanges() {
		return opiFont != null;
	}

	@Override
	protected Object doGetValue() {
		return opiFont;
	}

	@Override
	protected void doSetValue(Object value) {
		if(value == null || !(value instanceof OPIFont))
			opiFont = new OPIFont("unknown");
		else
			opiFont = (OPIFont)value;
	}

}
