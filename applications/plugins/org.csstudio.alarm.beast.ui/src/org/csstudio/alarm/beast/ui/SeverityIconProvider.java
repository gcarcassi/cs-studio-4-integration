/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.alarm.beast.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.csstudio.alarm.beast.SeverityLevel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/** Helper that provides icon for severity levels
 *  @author Kay Kasemir
 */
public class SeverityIconProvider
{
    /** Pixel widths/height of icon */
    final private static int ICON_SIZE = 12;

    /** Arc for rounded corner. ARC_SIZE==ICON_SIZE results in circle */
    final private static int ARC_SIZE = 10;

    /** Icon to use for disabled items */
    final private Image disabled;

    /** Icons to use for the various severity levels */
    final private Image icons[][];

    /** Initialize
     *  @param parent Parent widget
     */
    public SeverityIconProvider(final Composite parent)
    {
        final Display display = parent.getDisplay();

        RGB gray = display.getSystemColor(SWT.COLOR_GRAY).getRGB();
        final  BufferedImage awtImage = new BufferedImage(ICON_SIZE, ICON_SIZE,
        				BufferedImage.TYPE_INT_RGB);
		Graphics g = awtImage.getGraphics();
        g.setColor(new Color(255,255,255));
        g.fillRect(0, 0, ICON_SIZE, ICON_SIZE);

        // Left rectangle for 'latched', right for 'current' indicator
        g.setColor(new Color(gray.red,gray.green,gray.blue));
        g.fillRoundRect(0, 0, ICON_SIZE, ICON_SIZE, ARC_SIZE, ARC_SIZE);
        
        disabled = makeSWTImage(display, awtImage);
        g.dispose();
        
        icons = createIcons(display);
        parent.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(final DisposeEvent e)
            {
                final int N = icons.length;
                for (int c=0; c<N; ++c)
                    for (int s=0; s<N; ++s)
                        icons[c][s].dispose();
                disabled.dispose();
            }
        });
    }

    /**
     * Convert AWT image to SWT Image (usefull for RAP)
     * 
     * @param display
     * @param awtImage
     * @return
     */
	private static Image makeSWTImage(final Display display, final java.awt.Image awtImage) {
		final int width = awtImage.getWidth(null);
		final int height = awtImage.getHeight(null);
		final BufferedImage bufferedImage = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		final Graphics2D g2d = bufferedImage.createGraphics();
		g2d.drawImage(awtImage, 0, 0, null);
		g2d.dispose();
		
		return new Image(display,
				AWT2SWTImageConverter.convertToSWT(bufferedImage));
	}

    /** @return Array of icons */
    private Image[][] createIcons(final Display display)
    {
        final SeverityLevel[] severities = SeverityLevel.values();
        final Image icons[][] = new Image[severities.length][severities.length];
        // Use AWT to be able to draw icon in RAP version
        for (int c = 0; c < severities.length; c++)
        {
			final Color c_col = new Color(
					severities[c].getRed(),
					severities[c].getGreen(),
					severities[c].getBlue());
			for (int s = 0; s < severities.length; s++) {
				final Color s_col = new Color(
						severities[s].getRed(),
						severities[s].getGreen(),
						severities[s].getBlue());
				final BufferedImage awtImage = new BufferedImage(ICON_SIZE,
						ICON_SIZE, BufferedImage.TYPE_INT_RGB);
        		final Graphics g = awtImage.getGraphics();
                g.setColor(new Color(255,255,255));
                g.fillRect(0, 0, ICON_SIZE, ICON_SIZE);

                // Left rectangle for 'latched', right for 'current' indicator
                g.setColor(s_col);
                g.setClip(0, 0, ICON_SIZE/2, ICON_SIZE);
                g.fillRoundRect(0, 0, ICON_SIZE, ICON_SIZE, ARC_SIZE, ARC_SIZE);
                
                g.setColor(c_col);
                g.setClip(ICON_SIZE/2, 0, ICON_SIZE/2, ICON_SIZE);
                g.fillRoundRect(0, 0, ICON_SIZE, ICON_SIZE, ARC_SIZE, ARC_SIZE);

                icons[c][s] = makeSWTImage(display, awtImage);
                g.dispose();
            }
        }
        return icons;
    }

    /** Obtain icon suitable for displaying a disabled item
     *  @return Icon for those severities
     */
    public Image getDisabledIcon()
    {
        return disabled;
    }

    /** Obtain icon suitable for displaying a severity level
     *  @param current_severity Current severity
     *  @param severity Latched SeverityLevel
     *  @return Icon for those severities
     */
    public Image getIcon(final SeverityLevel current_severity,
            final SeverityLevel severity)
    {
        return icons[current_severity.ordinal()][severity.ordinal()];
    }
}
