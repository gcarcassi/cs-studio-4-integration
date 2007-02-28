package org.csstudio.trends.databrowser.model;

import org.csstudio.archive.ArchiveValues;
import org.csstudio.archive.util.TimestampUtil;
import org.csstudio.platform.util.ITimestamp;
import org.csstudio.platform.util.TimestampFactory;
import org.csstudio.swt.chart.ChartSample;
import org.csstudio.swt.chart.ChartSampleSequence;
import org.csstudio.value.DoubleValue;
import org.csstudio.value.Value;
import org.eclipse.swt.widgets.Display;

/** Samples of a model item, combination of archived and live samples,
 *  appears as one long ChartSampleSequence.
 *  <p>
 *  Note the synchronize comments of the ChartSampleSequence class.
 *  Users of ModelItemSamples must synchronize on it.
 *  @author Kay Kasemir
 *  @see ChartSampleSequence
 */
public class ModelSamples implements ChartSampleSequence
{
    /** The 'archived' samples for this item.
     *  Read from the GUI thread, but updated from an archive reader thread.
     */
    private volatile ModelSampleArray archive_samples;

    /** All the 'live' samples we collected for this item.
     *  Usually accessed from the UI thread for plotting,
     *  but also archive reader when getting the 'border'.
     */
    private volatile ModelSampleRing live_samples;

    /** Construct with given initial 'live' buffer size */
    ModelSamples(int ring_size)
    {
        archive_samples = null;
        live_samples = new ModelSampleRing(ring_size);
    }
    
    /** Should be called for cleanup. */
    synchronized void dispose()
    {
        live_samples.dispose();
    }

    /** Change the number of 'live' samples. */
    synchronized void setLiveCapacity(int size)
    {
        live_samples.setCapacity(size);
    }
    
    /** Add samples from an archive. */
    @SuppressWarnings("nls") //$NON-NLS-1$
    synchronized void add(ArchiveValues samples)
    {
        // To prevent archived samples from overlapping 'live' data,
        // use only archived samples only until reaching the 'border':
        ITimestamp border;
        if (live_samples.size() > 0)
            border = 
                TimestampUtil.fromDouble(live_samples.get(0).getX());
        else
            border = TimestampFactory.now();
        // One could consider unlocking 'this' here,
        // because the copy takes some time, so we don't have
        // to synchonize...
        ModelSampleArray new_samples =
            ModelSampleArray.fromArchivedSamples(samples, border);
        // ... and then sync again when we update the archive_samples:
        archive_samples =
            ModelSampleMerger.merge(archive_samples, new_samples);
    }
    
    /** Marks the end of the 'live' buffer as currently disconnected. */
    synchronized void markCurrentlyDisconnected(ITimestamp now)
    {
        /** Add one(!) last 'end' sample. */
        int size = live_samples.size();
        if (size > 0)
        {
            String last = live_samples.get(size - 1).getSample().getStatus();
            // Does last sample already have 'disconnected' status?
            if (last != null && last.equals(Messages.LivePVDisconnected))
                return;
        }
        
        DoubleValue disconnected = new DoubleValue(now,
                        SeverityFactory.getInvalid(),
                        Messages.LivePVDisconnected,
                        MetaDataFactory.getNumeric(),
                        new double[] { Double.NEGATIVE_INFINITY });
        
        live_samples.add(disconnected);
    }
    
    /** Add most recent timestamp/value */
    synchronized void addLiveSample(Value value)
    {
        // We expect all access to this method from the UI thread.
        if (Display.getCurrent() == null)
            throw new Error("Accessed from non-UI thread"); //$NON-NLS-1$
        live_samples.add(value);
    }
    
    /** @see ChartSampleSequence */
    synchronized public ModelSample get(int i)
    {
        if (archive_samples != null)
        {
            int arch_size = archive_samples.size();
            if (i < arch_size)
            {
                ModelSample sample = archive_samples.get(i);
                // Patch the last 'archive' sample to indicate
                // that that's the end of historic samples.
                // Note that the original sample is kept unchanged,
                // since after the next archive request, it might no
                // longer be the 'last' one.
                // We only morph it right here and now, temporarily.
                if (i == arch_size - 1)
                    return new ModelSampleMorpher(sample,
                                    ChartSample.Type.Point,
                                    Messages.LastArchivedSample);
                return sample;
            }
            i -= arch_size;
        }
        return live_samples.get(i);
    }

    /** @see ChartSampleSequence */
    synchronized public int size()
    {
        int size = live_samples.size();
        if (archive_samples != null)
            size += archive_samples.size();
        return size;
    }
    
    synchronized public void clearArchive() {
    	this.archive_samples = null;
    }
}