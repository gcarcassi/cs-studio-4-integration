/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.server;

import java.util.Date;

import org.csstudio.scan.PathUtil;
import org.csstudio.scan.data.ScanSampleFormatter;

/** Scan server info
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanServerInfo extends MemoryInfo
{
    final private String version;
    final private Date start_time;
    final private String scan_config;
    final private String simulation_config;
    final private String[] script_paths;
    final private String macros;

    /** Initialize
     *  @param version
     *  @param start_time
     *  @param scan_config
     *  @param simulation_config
     */
    public ScanServerInfo(final String version, final Date start_time,
    		final String scan_config,
    		final String simulation_config,
    		final String[] script_paths,
    		final String macros)
    {
	    this.version = version;
	    this.start_time = start_time;
	    this.scan_config = scan_config;
	    this.simulation_config = simulation_config;
	    this.script_paths = script_paths;
	    this.macros = macros;
    }

    /** Initialize
     *  @param version
     *  @param start_time
     *  @param scan_config
     *  @param simulation_config
     *  @param script_paths
     *  @param macros
     *  @param used_mem Used memory (kB)
     *  @param max_mem Maximum available memory (kB)
     *  @param non_heap
     */
    public ScanServerInfo(final String version, final Date start_time,
            final String scan_config,
            final String simulation_config,
            final String[] script_paths,
            final String macros,
            final long used_mem, final long max_mem, final long non_heap)
    {
        super(used_mem, max_mem, non_heap);
        this.version = version;
        this.start_time = start_time;
        this.scan_config = scan_config;
        this.simulation_config = simulation_config;
        this.script_paths = script_paths;
        this.macros = macros;
    }
    
    /** @return Version number */
	public String getVersion()
    {
    	return version;
    }

	/** @return Start time */
	public Date getStartTime()
    {
    	return start_time;
    }

	/** @return Scan configuration path */
	public String getScanConfig()
    {
    	return scan_config;
    }

	/** @return Simulation configuration path (since originally that was a separate file)
	 *  @see #getScanConfig()
	 */
	public String getSimulationConfig()
	{
		return simulation_config;
	}

	/** @return Script paths */
    public String[] getScriptPaths()
    {
        return script_paths;
    }

    /** @return Macros */
    public String getMacros()
    {
        return macros;
    }
	
    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("Version: ").append(version).append("\n");
        buf.append("Started: ").append(ScanSampleFormatter.format(start_time)).append("\n");
        buf.append("Scan Configuration: ").append(scan_config).append("\n");
        buf.append("Simulation Configuration: ").append(simulation_config).append("\n");
        buf.append("Script paths: ").append(PathUtil.joinPaths(script_paths)).append("\n");
        buf.append("Macros: ").append(macros).append("\n");
        buf.append("Memory: ").append(getMemoryInfo()).append("\n");
        return buf.toString();
    }
}
