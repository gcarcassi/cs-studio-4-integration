/*
 * Copyright (c) 2008 Stiftung Deutsches Elektronen-Synchrotron, Member of the Helmholtz
 * Association, (DESY), HAMBURG, GERMANY. THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN
 * "../AS IS" BASIS. WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE IN
 * ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR CORRECTION. THIS
 * DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE. NO USE OF ANY SOFTWARE IS
 * AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER. DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS. THE FULL LICENSE SPECIFYING FOR THE SOFTWARE
 * THE REDISTRIBUTION, MODIFICATION, USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE
 * DISTRIBUTION OF THIS PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY
 * FIND A COPY AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
package org.csstudio.alarm.treeView.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.csstudio.alarm.service.declaration.AlarmConnectionException;
import org.csstudio.alarm.service.declaration.AlarmTreeLdapConstants;
import org.csstudio.alarm.service.declaration.IAlarmConfigurationService;
import org.csstudio.alarm.service.declaration.IAlarmConnection;
import org.csstudio.alarm.service.declaration.LdapEpicsAlarmCfgObjectClass;
import org.csstudio.alarm.treeView.AlarmTreePlugin;
import org.csstudio.alarm.treeView.jobs.ImportInitialConfigJob;
import org.csstudio.alarm.treeView.jobs.ImportXmlFileJob;
import org.csstudio.alarm.treeView.model.IAlarmTreeNode;
import org.csstudio.alarm.treeView.model.ProcessVariableNode;
import org.csstudio.alarm.treeView.model.Severity;
import org.csstudio.alarm.treeView.model.SubtreeNode;
import org.csstudio.alarm.treeView.service.AlarmMessageListener;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.utility.ldap.service.ILdapService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.DelegatingDragAdapter;
import org.eclipse.jface.util.DelegatingDropAdapter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.PendingUpdateAdapter;

/**
 * Tree view of process variables and their alarm state. This view uses LDAP to get a hierarchy of
 * process variables and displays them in a tree view. Process variables which are in an alarm state
 * are visually marked in the view.
 *
 * @author Joerg Rathlev
 */
public class AlarmTreeView extends ViewPart {

    /**
     * The ID of this view.
     */
    private static final String ID = "org.csstudio.alarm.treeView.views.AlarmTreeView";

    private static final Logger LOG = CentralLogger.getInstance().getLogger(AlarmTreeView.class);

    /**
     * Converts a selection into a list of selected alarm tree nodes.
     */
    @Nonnull
    static List<IAlarmTreeNode> selectionToNodeList(@Nullable final ISelection selection) {

        if (selection instanceof IStructuredSelection) {
            final List<IAlarmTreeNode> result = new ArrayList<IAlarmTreeNode>();
            final IStructuredSelection s = (IStructuredSelection) selection;
            for (final Iterator<?> i = s.iterator(); i.hasNext();) {
                result.add((IAlarmTreeNode) i.next());
            }
            return result;
        }
        return Collections.emptyList();
    }


    /**
     * Returns whether a list of nodes contains only ProcessVariableNodes.
     */
    static boolean containsOnlyPVNodes(@Nonnull final List<IAlarmTreeNode> nodes) {
        for (final IAlarmTreeNode node : nodes) {
            if (!(node instanceof ProcessVariableNode)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the id of this view.
     *
     * @return the id of this view.
     */
    @Nonnull
    public static String getID() {
        return ID;
    }

    /**
     * The tree viewer that displays the alarm objects.
     */
    private TreeViewer _viewer;

    /**
     * The message area above the tree viewer
     */
    private MessageArea _myMessageArea;

    /**
     * The subscriber to the alarm topic.
     */
    private IAlarmConnection _connection;

    /**
     * The callback for the alarm messages
     */
    private AlarmMessageListener _alarmListener;

    /**
     * The reload action.
     */
    private Action _reloadAction;

    /**
     * The import xml file action.
     */
    private Action _importXmlFileAction;

    /**
     * The acknowledge action.
     */
    private Action _acknowledgeAction;

    /**
     * The Run CSS Alarm Display action.
     */
    private Action _runCssAlarmDisplayAction;

    /**
     * The Run CSS Display action.
     */
    private Action _runCssDisplayAction;

    /**
     * The Open CSS Strip Chart action.
     */
    private Action _openCssStripChartAction;

    /**
     * The Show Help Page action.
     */
    private Action _showHelpPageAction;

    /**
     * The Show Help Guidance action.
     */
    private Action _showHelpGuidanceAction;

    /**
     * The Create Record action.
     */
    private Action _createRecordAction;

    /**
     * The Create Component action.
     */
    private Action _createComponentAction;

    /**
     * The Rename action.
     */
    private Action _renameAction;

    /**
     * The Delete action.
     */
    private Action _deleteNodeAction;

    /**
     * The Show Property View action.
     */
    private Action _showPropertyViewAction;

    /**
     * A filter which hides all nodes which are not currently in an alarm state.
     */
    private ViewerFilter _currentAlarmFilter;

    /**
     * The action which toggles the filter on and off.
     */
    private Action _toggleFilterAction;

    /**
     * Saves the currently configured alarm tree as xml file.
     */
    private Action _saveAsXmlFileAction;

    /**
     * Whether the filter is active.
     */
    private Boolean _isFilterActive = Boolean.FALSE;



    private final SubtreeNode _rootNode;

    private final IAlarmConfigurationService _configService =
        AlarmTreePlugin.getDefault().getAlarmConfigurationService();

    private final ILdapService _ldapService =
        AlarmTreePlugin.getDefault().getLdapService();


    /**
     * Creates an LDAP tree viewer.
     */
    public AlarmTreeView() {
        _rootNode = new SubtreeNode.Builder(AlarmTreeLdapConstants.EPICS_ALARM_CFG_FIELD_VALUE,
                                            LdapEpicsAlarmCfgObjectClass.ROOT).build();
    }

    /**
     * Getter.
     * @param the alarm listener
     */
    @CheckForNull
    public AlarmMessageListener getAlarmListener() {
        return _alarmListener;
    }

    /**
     * Getter.
     * @return the tree viewer
     */
    @CheckForNull
    public TreeViewer getViewer() {
        return _viewer;
    }

    /**
     * Getter.
     * @return the message area
     */
    @CheckForNull
    public MessageArea getMessageArea() {
        return _myMessageArea;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void createPartControl(@Nonnull final Composite parent) {

        final GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        parent.setLayout(layout);

        _myMessageArea = new MessageArea(parent);

        _viewer = createTreeViewer(parent);

        _currentAlarmFilter = new CurrentAlarmFilter();

        _alarmListener = new AlarmMessageListener();

        initializeContextMenu();

        makeActions(_viewer, _alarmListener, getSite(), _currentAlarmFilter);

        contributeToActionBars();

        getSite().setSelectionProvider(_viewer);

        startConnection();

        addDragAndDropSupport();
    }


    /**
     * @param parent
     */
    @Nonnull
    private TreeViewer createTreeViewer(@Nonnull final Composite parent) {
        final TreeViewer viewer = new TreeViewer(parent, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

        viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        viewer.setContentProvider(new AlarmTreeContentProvider());
        viewer.setLabelProvider(new AlarmTreeLabelProvider());
        viewer.setComparator(new ViewerComparator());

        final ISelectionChangedListener selectionChangedListener =
            new ISelectionChangedListener() {
                public void selectionChanged(@Nonnull final SelectionChangedEvent event) {
                    AlarmTreeView.this.selectionChanged(event);
                }
            };
        viewer.addSelectionChangedListener(selectionChangedListener);

        return viewer;
    }

    /**
     * Starts the connection.
     */
    private void startConnection() {
        LOG.debug("Starting connection.");

        if (_connection != null) {
            // There is still an old connection. This shouldn't happen.
            _connection.disconnect();
            LOG.warn("There was an active connection when starting a new connection");
        }

        final IWorkbenchSiteProgressService progressService = (IWorkbenchSiteProgressService) getSite()
                .getAdapter(IWorkbenchSiteProgressService.class);

        final Job connectionJob = createConnectionJob();

        progressService.schedule(connectionJob, 0, true);
    }

    @Nonnull
    private Job createConnectionJob() {
        final Job connectionJob = new Job("Connecting via alarm service") {

            @Override
            protected IStatus run(@Nonnull final IProgressMonitor monitor) {
                monitor.beginTask("Connecting via alarm service", IProgressMonitor.UNKNOWN);
                _connection = AlarmTreePlugin.getDefault().getAlarmService().newAlarmConnection();
                try {
                    final AlarmMessageListener listener = AlarmTreeView.this.getAlarmListener();
                    if (listener == null) {
                        throw new IllegalStateException("Listener of " +
                                                        AlarmTreeView.class.getName() +
                                                        " mustn't be null.");
                    }
                    _connection.connectWithListener(new AlarmTreeConnectionMonitor(AlarmTreeView.this,
                                                                                   _rootNode),
                                                    listener,
                                                    "c:\\alarmConfig.xml");

                } catch (final AlarmConnectionException e) {
                    throw new RuntimeException("Could not connect via alarm service", e);
                }
                return Status.OK_STATUS;
            }
        };
        return connectionJob;
    }

    /**
     * Adds drag and drop support to the tree viewer.
     */
    private void addDragAndDropSupport() {
        final DelegatingDropAdapter dropAdapter = new DelegatingDropAdapter();
        dropAdapter.addDropTargetListener(new AlarmTreeLocalSelectionDropListener(this));
        dropAdapter.addDropTargetListener(new AlarmTreeProcessVariableDropListener(this));
        _viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE,
                               dropAdapter.getTransfers(),
                               dropAdapter);

        final DelegatingDragAdapter dragAdapter = new DelegatingDragAdapter();
        dragAdapter.addDragSourceListener(new AlarmTreeLocalSelectionDragListener(this));
        dragAdapter.addDragSourceListener(new AlarmTreeProcessVariableDragListener(this));
        dragAdapter.addDragSourceListener(new AlarmTreeTextDragListener(this));
        _viewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE,
                               dragAdapter.getTransfers(),
                               dragAdapter);
    }

    /**
     * Starts a job which reads the contents of the directory in the background.
     * @param rootNode
     */
    public void startImportInitialConfiguration(@Nonnull final SubtreeNode rootNode) {
        LOG.debug("Starting directory reader.");
        final IWorkbenchSiteProgressService progressService = (IWorkbenchSiteProgressService) getSite()
                .getAdapter(IWorkbenchSiteProgressService.class);

        final Job importInitialConfigJob = createImportInitialConfigJob(rootNode);

        // Set the tree to which updates are applied to null. This means updates
        // will be queued for later application.
        _alarmListener.setUpdater(null);

        // The directory is read in the background. Until then, set the viewer's
        // input to a placeholder object.
        _viewer.setInput(new Object[] {new PendingUpdateAdapter()});

        // Start the directory reader job.
        progressService.schedule(importInitialConfigJob, 0, true);
    }

    @Nonnull
    private ImportXmlFileJob createImportXmlFileJob(@Nonnull final SubtreeNode rootNode) {
        final ImportXmlFileJob importXmlFileJob = new ImportXmlFileJob(_configService,
                                                                       _ldapService,
                                                                       rootNode);
        importXmlFileJob.addJobChangeListener(new RefreshAlarmTreeViewAdapter(this, rootNode));

        return importXmlFileJob;
    }

    @Nonnull
    private Job createImportInitialConfigJob(@Nonnull final SubtreeNode rootNode) {

        final Job importInitConfigJob = new ImportInitialConfigJob(this, rootNode, _configService);

        importInitConfigJob.addJobChangeListener(new RefreshAlarmTreeViewAdapter(this, rootNode));

        return importInitConfigJob;
    }

    /**
     * Sets the input for the tree. The actual work will be done asynchronously in the UI thread.
     *
     * @param inputElement the new input element.
     */
    void asyncSetViewerInput(@Nonnull final SubtreeNode inputElement) {
        getSite().getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
                _viewer.setInput(inputElement);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void dispose() {
        _connection.disconnect();
        super.dispose();
    }

    /**
     * Called when the selection of the tree changes.
     *
     * @param event the selection event.
     */
    private void selectionChanged(@Nonnull final SelectionChangedEvent event) {
        final IStructuredSelection sel = (IStructuredSelection) event.getSelection();
        _acknowledgeAction.setEnabled(containsNodeWithUnackAlarm(sel));
        _runCssAlarmDisplayAction.setEnabled(hasCssAlarmDisplay(sel.getFirstElement()));
        _runCssDisplayAction.setEnabled(hasCssDisplay(sel.getFirstElement()));
        _openCssStripChartAction.setEnabled(hasCssStripChart(sel.getFirstElement()));
        _showHelpGuidanceAction.setEnabled(hasHelpGuidance(sel.getFirstElement()));
        _showHelpPageAction.setEnabled(hasHelpPage(sel.getFirstElement()));
    }

    /**
     * Returns whether the given node has a CSS strip chart.
     *
     * @param node the node.
     * @return <code>true</code> if the node has a strip chart, <code>false</code> otherwise.
     */
    private boolean hasCssStripChart(@Nonnull final Object node) {
        if (node instanceof IAlarmTreeNode) {
            return ((IAlarmTreeNode) node).getCssStripChart() != null;
        }
        return false;
    }

    /**
     * Returns whether the given node has a CSS display.
     *
     * @param node the node.
     * @return <code>true</code> if the node has a display, <code>false</code> otherwise.
     */
    private boolean hasCssDisplay(@Nonnull final Object node) {
        if (node instanceof IAlarmTreeNode) {
            return ((IAlarmTreeNode) node).getCssDisplay() != null;
        }
        return false;
    }

    /**
     * Return whether help guidance is available for the given node.
     *
     * @param node the node.
     * @return <code>true</code> if the node has a help guidance string, <code>false</code>
     *         otherwise.
     */
    private boolean hasHelpGuidance(@Nonnull final Object node) {
        if (node instanceof IAlarmTreeNode) {
            return ((IAlarmTreeNode) node).getHelpGuidance() != null;
        }
        return false;
    }

    /**
     * Return whether the given node has an associated help page.
     *
     * @param node the node.
     * @return <code>true</code> if the node has an associated help page, <code>false</code>
     *         otherwise.
     */
    private boolean hasHelpPage(@Nonnull final Object node) {
        if (node instanceof IAlarmTreeNode) {
            return ((IAlarmTreeNode) node).getHelpPage() != null;
        }
        return false;
    }

    /**
     * Returns whether the given process variable node in the tree has an associated CSS alarm
     * display configured.
     *
     * @param node the node.
     * @return <code>true</code> if a CSS alarm display is configured for the node,
     *         <code>false</code> otherwise.
     */
    private boolean hasCssAlarmDisplay(@Nonnull final Object node) {
        if (node instanceof IAlarmTreeNode) {
            final String display = ((IAlarmTreeNode) node).getCssAlarmDisplay();
            return (display != null) && display.matches(".+\\.css-sds");
        }
        return false;
    }

    /**
     * Returns whether the given selection contains at least one node with an unacknowledged alarm.
     *
     * @param sel the selection.
     * @return <code>true</code> if the selection contains a node with an unacknowledged alarm,
     *         <code>false</code> otherwise.
     */
    private boolean containsNodeWithUnackAlarm(@Nonnull final IStructuredSelection sel) {
        final Object selectedElement = sel.getFirstElement();
        // Note: selectedElement is not instance of IAlarmTreeNode if nothing
        // is selected (selectedElement == null), and during initialization,
        // when it is an instance of PendingUpdateAdapter.
        if (selectedElement instanceof IAlarmTreeNode) {
            return ((IAlarmTreeNode) selectedElement).getUnacknowledgedAlarmSeverity() != Severity.NO_ALARM;
        }
        return false;
    }

    /**
     * Adds a context menu to the tree view.
     */
    private void initializeContextMenu() {
        final MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);

        // add menu items to the context menu when it is about to show
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(@Nullable final IMenuManager manager) {
                AlarmTreeView.this.fillContextMenu(manager);
            }
        });

        // add the context menu to the tree viewer
        final Menu contextMenu = menuMgr.createContextMenu(_viewer.getTree());
        _viewer.getTree().setMenu(contextMenu);

        // register the context menu for extension by other plug-ins
        getSite().registerContextMenu(menuMgr, _viewer);
    }

    /**
     * Adds tool buttons and menu items to the action bar of this view.
     */
    private void contributeToActionBars() {
        final IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    /**
     * Adds the actions for the action bar's pull down menu.
     *
     * @param manager the menu manager.
     */
    private void fillLocalPullDown(@Nonnull final IMenuManager manager) {
        // EMPTY
    }

    /**
     * Adds the context menu actions.
     *
     * @param menuManager the menu manager.
     */
    private void fillContextMenu(@Nullable final IMenuManager menuManager) {
        if (menuManager == null) {
            MessageDialog
                    .openError(getSite().getShell(),
                               "Context menu",
                               "Inernal error occurred when trying to open the context menu (IMenuManager is null).");
            return;
        }

        final IStructuredSelection selection = (IStructuredSelection) _viewer.getSelection();
        if (selection.size() > 0) {
            menuManager.add(_acknowledgeAction);
        }
        if (selection.size() == 1) {
            menuManager.add(_runCssAlarmDisplayAction);
            menuManager.add(_runCssDisplayAction);
            menuManager.add(_openCssStripChartAction);
            menuManager.add(_showHelpGuidanceAction);
            menuManager.add(_showHelpPageAction);
            menuManager.add(new Separator("edit"));
            menuManager.add(_renameAction);
            menuManager.add(_deleteNodeAction);

            final Object firstElement = selection.getFirstElement();
            if (firstElement instanceof SubtreeNode) {
                menuManager.add(_createRecordAction);
                menuManager.add(_createComponentAction);

                final LdapEpicsAlarmCfgObjectClass oc = ((SubtreeNode) firstElement)
                        .getObjectClass();
                if (LdapEpicsAlarmCfgObjectClass.FACILITY.equals(oc)) {
                    menuManager.add(_saveAsXmlFileAction);
                }
            }
        }

        // adds a separator after which contributed actions from other plug-ins
        // will be displayed
        menuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    /**
     * Adds the tool bar actions.
     *
     * @param manager the menu manager.
     */
    private void fillLocalToolBar(@Nonnull final IToolBarManager manager) {
        manager.add(_toggleFilterAction);
        manager.add(new Separator());
        manager.add(_showPropertyViewAction);
        manager.add(_reloadAction);
        manager.add(_importXmlFileAction);
    }

    /**
     * Creates the actions offered by this view.
     * @param iWorkbenchPartSite
     * @param alarmListener
     * @param viewer
     * @param currentAlarmFilter
     */
    private void makeActions(@Nonnull final TreeViewer viewer,
                             @Nonnull final AlarmMessageListener alarmListener,
                             @Nonnull final IWorkbenchPartSite site,
                             @Nonnull final ViewerFilter currentAlarmFilter) {

        _reloadAction =
            AlarmTreeViewActionFactory.createReloadAction(createImportInitialConfigJob(_rootNode),
                                                          site,
                                                          alarmListener,
                                                          viewer);

        _importXmlFileAction =
            AlarmTreeViewActionFactory.createImportXmlFileAction(createImportXmlFileJob(_rootNode),
                                                                 site,
                                                                 alarmListener,
                                                                 viewer);

        _acknowledgeAction = AlarmTreeViewActionFactory.createAcknowledgeAction(viewer);

        _runCssAlarmDisplayAction = AlarmTreeViewActionFactory.createCssAlarmDisplayAction(viewer);

        _runCssDisplayAction = AlarmTreeViewActionFactory.createRunCssDisplayAction(viewer);

        _openCssStripChartAction = AlarmTreeViewActionFactory.createCssStripChartAction(site,
                                                                                        viewer);

        _showHelpGuidanceAction = AlarmTreeViewActionFactory.createShowHelpGuidanceAction(site,
                                                                                          viewer);

        _showHelpPageAction = AlarmTreeViewActionFactory.createShowHelpPageAction(viewer);

        _createRecordAction = AlarmTreeViewActionFactory.createCreateRecordAction(site, viewer);

        _createComponentAction = AlarmTreeViewActionFactory.createCreateComponentAction(site,
                                                                                        viewer);

        _renameAction = AlarmTreeViewActionFactory.createRenameAction(site, viewer);

        _deleteNodeAction = AlarmTreeViewActionFactory.createDeleteNodeAction(site, viewer);

        _showPropertyViewAction = AlarmTreeViewActionFactory.createShowPropertyViewAction(site);

        _toggleFilterAction = AlarmTreeViewActionFactory
                .createToggleFilterAction(this, viewer, currentAlarmFilter);

        _saveAsXmlFileAction = AlarmTreeViewActionFactory.createSaveAsXmlFileAction(site, viewer);

    }

    @Nonnull
    public Boolean getIsFilterActive() {
        return _isFilterActive;
    }

    public void setIsFilterActive(@Nonnull final Boolean isFilterActive) {
        _isFilterActive = isFilterActive;
    }

    /**
     * Passes the focus request to the viewer's control.
     */
    @Override
    public final void setFocus() {
        _viewer.getControl().setFocus();
    }

    /**
     * Refreshes this view.
     */
    public final void refresh() {
        _viewer.refresh();
    }
}
