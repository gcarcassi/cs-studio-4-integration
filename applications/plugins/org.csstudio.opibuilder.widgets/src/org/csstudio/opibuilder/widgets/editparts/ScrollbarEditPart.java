package org.csstudio.opibuilder.widgets.editparts;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.csstudio.opibuilder.editparts.AbstractPVWidgetEditPart;
import org.csstudio.opibuilder.editparts.ExecutionMode;
import org.csstudio.opibuilder.model.AbstractPVWidgetModel;
import org.csstudio.opibuilder.model.AbstractWidgetModel;
import org.csstudio.opibuilder.properties.IWidgetPropertyChangeHandler;
import org.csstudio.opibuilder.widgets.figures.ScrollbarFigure;
import org.csstudio.opibuilder.widgets.model.ScrollBarModel;
import org.csstudio.platform.data.INumericMetaData;
import org.csstudio.platform.data.IValue;
import org.csstudio.platform.data.ValueUtil;
import org.csstudio.utility.pv.PV;
import org.csstudio.utility.pv.PVListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.RangeModel;
import org.eclipse.draw2d.ScrollBar;

/**
 * The controller of scrollbar widget.
 * 
 * @author Xihui Chen
 * 
 */
public class ScrollbarEditPart extends AbstractPVWidgetEditPart {
	
	private PVListener pvLoadLimitsListener;
	private INumericMetaData meta = null;
	
	/**
	 * All double value properties can converted to integer value by multiplying
	 * this number. 
	 */
	private int multiplyFactor = 1;
	
	private boolean innerUpdate;

	@Override
	public ScrollBarModel getWidgetModel() {
		return (ScrollBarModel) super.getWidgetModel();
	}
	
	@Override
	protected void doActivate() {
		super.doActivate();
		if(getExecutionMode() == ExecutionMode.RUN_MODE){
			final ScrollBarModel model = getWidgetModel();
			if(model.isLimitsFromPV()){
				PV pv = getPV(AbstractPVWidgetModel.PROP_PVNAME);
				if(pv != null){	
					pvLoadLimitsListener = new PVListener() {				
						public void pvValueUpdate(PV pv) {
							IValue value = pv.getValue();
							if (value != null && value.getMetaData() instanceof INumericMetaData){
								INumericMetaData new_meta = (INumericMetaData)value.getMetaData();
								if(meta == null || !meta.equals(new_meta)){
									meta = new_meta;
									model.setPropertyValue(ScrollBarModel.PROP_MAX,	meta.getDisplayHigh());
									model.setPropertyValue(ScrollBarModel.PROP_MIN,	meta.getDisplayLow());								
								}
							}
						}					
						public void pvDisconnected(PV pv) {}
					};
					pv.addListener(pvLoadLimitsListener);				
				}
			}
		}
	}
	
	private void updateMutiplyFactor(IFigure figure, double value){
		int currentPrecision = (int) Math.log10(multiplyFactor);
		String valueString = Double.toString(value);
		int dotPosition = valueString.indexOf(".");
		if(dotPosition < 0)
			return;
		int newPrecision = valueString.length() - dotPosition-1; 
		if(currentPrecision > newPrecision)
			return;
		int oldMultiplyFactor = multiplyFactor;
		multiplyFactor = (int) Math.pow(10, newPrecision);		
		resetScrollbarWithNewMultiplyFactor(figure, oldMultiplyFactor, multiplyFactor);
	}
	
	private void resetScrollbarWithNewMultiplyFactor(IFigure figure, int oldFactor, int newFactor){
		if(figure == null || !(figure instanceof ScrollBar))
			return;
		ScrollBar scrollBar = (ScrollBar) getFigure();
		scrollBar.setMaximum(scrollBar.getMaximum()*newFactor/oldFactor);
		scrollBar.setMinimum(scrollBar.getMinimum()*newFactor/oldFactor);
		scrollBar.setStepIncrement(scrollBar.getStepIncrement()*newFactor/oldFactor);
		scrollBar.setPageIncrement(scrollBar.getPageIncrement()*newFactor/oldFactor);
		scrollBar.setExtent(scrollBar.getExtent()*newFactor/oldFactor);
	}
	
	
	@Override
	protected IFigure doCreateFigure() {
		ScrollbarFigure scrollBar = new ScrollbarFigure();
		ScrollBarModel model = getWidgetModel();
		updateMutiplyFactor(null, model.getMaximum());
		updateMutiplyFactor(null, model.getMinimum());
		updateMutiplyFactor(null, model.getPageIncrement());
		updateMutiplyFactor(null, model.getStepIncrement());
		updateMutiplyFactor(null, model.getBarLength());
		
		scrollBar.setMaximum((int) ((model.getMaximum() + model.getBarLength()) * multiplyFactor));
		scrollBar.setMinimum((int) (model.getMinimum() * multiplyFactor));
		scrollBar.setStepIncrement((int) (model.getStepIncrement() * multiplyFactor));
		scrollBar.setPageIncrement((int) (model.getPageIncrement() * multiplyFactor));		
		scrollBar.setExtent((int) (model.getBarLength() * multiplyFactor));
		
		scrollBar.setHorizontal(model.isHorizontal());

		if (getExecutionMode() == ExecutionMode.RUN_MODE){
			scrollBar.addPropertyChangeListener(RangeModel.PROPERTY_VALUE, 
					new PropertyChangeListener() {			
						public void propertyChange(PropertyChangeEvent evt) {
							if(innerUpdate){
								innerUpdate = false;
								return;
							}
							setPVValue(ScrollBarModel.PROP_PVNAME, 
								(((Integer)evt.getNewValue()).doubleValue())/multiplyFactor);
				}
			});
		}
		
		markAsControlPV(AbstractPVWidgetModel.PROP_PVNAME, AbstractPVWidgetModel.PROP_PVVALUE);
		return scrollBar;
	}

	@Override
	protected void registerPropertyChangeHandlers() {
		((ScrollBar)getFigure()).setEnabled(getWidgetModel().isEnabled() && 
				(getExecutionMode() == ExecutionMode.RUN_MODE));		
		
		removeAllPropertyChangeHandlers(AbstractWidgetModel.PROP_ENABLED);
		
		//enable
		IWidgetPropertyChangeHandler enableHandler = new IWidgetPropertyChangeHandler(){
			public boolean handleChange(Object oldValue, Object newValue,
					IFigure figure) {
				if(getExecutionMode() == ExecutionMode.RUN_MODE)
					figure.setEnabled((Boolean)newValue);
				return false;
			}
		};		
		setPropertyChangeHandler(AbstractWidgetModel.PROP_ENABLED, enableHandler);
		
		// value
		IWidgetPropertyChangeHandler valueHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				if(newValue == null)
					return false;
				ScrollBar figure = (ScrollBar) refreshableFigure;
				innerUpdate = true;
				figure.setValue((int) (ValueUtil.getDouble((IValue)newValue) * multiplyFactor));
				innerUpdate = false;
				return false;
			}
		};
		setPropertyChangeHandler(AbstractPVWidgetModel.PROP_PVVALUE, valueHandler);
		
		//minimum
		IWidgetPropertyChangeHandler minimumHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				ScrollBar figure = (ScrollBar) refreshableFigure;
				updateMutiplyFactor(refreshableFigure, (Double) newValue);	
				figure.setMinimum((int) (((Double)newValue)*multiplyFactor));
				figure.setEnabled(getWidgetModel().isEnabled() && 
						getExecutionMode() == ExecutionMode.RUN_MODE && figure.isEnabled());
				return false;
			}
		};
		setPropertyChangeHandler(ScrollBarModel.PROP_MIN, minimumHandler);
		
		//maximum
		IWidgetPropertyChangeHandler maximumHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				ScrollBar figure = (ScrollBar) refreshableFigure;
				updateMutiplyFactor(refreshableFigure, (Double) newValue);				
				figure.setMaximum((int) (
						((Double)newValue + getWidgetModel().getBarLength())*multiplyFactor));
				figure.setEnabled(getWidgetModel().isEnabled() && 
						getExecutionMode() == ExecutionMode.RUN_MODE && figure.isEnabled());
				return false;
			}
		};
		setPropertyChangeHandler(ScrollBarModel.PROP_MAX, maximumHandler);
	

		//page increment
		IWidgetPropertyChangeHandler pageIncrementHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				ScrollBar figure = (ScrollBar) refreshableFigure;
				updateMutiplyFactor(refreshableFigure, (Double) newValue);				
				figure.setPageIncrement((int) (((Double)newValue)*multiplyFactor));
				return false;
			}
		};
		setPropertyChangeHandler(ScrollBarModel.PROP_PAGE_INCREMENT, pageIncrementHandler);
		
		//step increment
		IWidgetPropertyChangeHandler stepIncrementHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				ScrollBar figure = (ScrollBar) refreshableFigure;
				updateMutiplyFactor(refreshableFigure, (Double) newValue);				
				figure.setStepIncrement((int) (((Double)newValue)*multiplyFactor));
				return false;
			}
		};
		setPropertyChangeHandler(ScrollBarModel.PROP_STEP_INCREMENT, stepIncrementHandler);
		
		//bar length
		IWidgetPropertyChangeHandler barLengthHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				ScrollBar figure = (ScrollBar) refreshableFigure;
				updateMutiplyFactor(refreshableFigure, (Double) newValue);				
				figure.setExtent((int) (((Double)newValue)*multiplyFactor));
				figure.setMaximum((int)
						((getWidgetModel().getMaximum() + (Double)newValue)*multiplyFactor));
				figure.setEnabled(getWidgetModel().isEnabled() && 
						getExecutionMode() == ExecutionMode.RUN_MODE && figure.isEnabled());
				return false;
			}
		};
		setPropertyChangeHandler(ScrollBarModel.PROP_BAR_LENGTH, barLengthHandler);
		
		
		//horizontal
		IWidgetPropertyChangeHandler horizontalHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				ScrollBar figure = (ScrollBar) refreshableFigure;
				
				ScrollBarModel model = getWidgetModel();
				if((Boolean) newValue) //from vertical to horizontal
					model.setLocation(model.getLocation().x - model.getSize().height/2 + model.getSize().width/2,
						model.getLocation().y + model.getSize().height/2 - model.getSize().width/2);
				else  //from horizontal to vertical
					model.setLocation(model.getLocation().x + model.getSize().width/2 - model.getSize().height/2,
						model.getLocation().y - model.getSize().width/2 + model.getSize().height/2);					
				
				model.setSize(model.getSize().height, model.getSize().width);
				figure.setHorizontal((Boolean)newValue);
				return false;
			}
		};
		setPropertyChangeHandler(ScrollBarModel.PROP_HORIZONTAL, horizontalHandler);
		
	}

	@Override
	protected void doDeActivate() {
		super.doDeActivate();
		if(getWidgetModel().isLimitsFromPV()){
			PV pv = getPV(AbstractPVWidgetModel.PROP_PVNAME);
			if(pv != null){	
				pv.removeListener(pvLoadLimitsListener);
			}
		}
		
	}

	@Override
	public void setValue(Object value) {
		if(value instanceof Double)
			((ScrollBar)getFigure()).setValue((Integer)value);
	}
	
	@Override
	public Integer getValue() {
		return ((ScrollBar)getFigure()).getValue();
	}

	
	
}
