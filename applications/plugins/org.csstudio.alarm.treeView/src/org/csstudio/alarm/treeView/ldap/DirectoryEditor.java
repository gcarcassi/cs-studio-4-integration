/*
 * Copyright (c) 2008 Stiftung Deutsches Elektronen-Synchrotron,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
package org.csstudio.alarm.treeView.ldap;

import static org.csstudio.utility.ldap.LdapFieldsAndAttributes.ATTR_FIELD_ALARM_HIGH_UNACK;
import static org.csstudio.utility.ldap.LdapFieldsAndAttributes.ATTR_FIELD_ALARM_SEVERITY;
import static org.csstudio.utility.ldap.LdapFieldsAndAttributes.ATTR_FIELD_ALARM_TIMESTAMP;
import static org.csstudio.utility.ldap.LdapFieldsAndAttributes.ATTR_FIELD_OBJECT_CLASS;
import static org.csstudio.utility.ldap.LdapFieldsAndAttributes.EPICS_CTRL_FIELD_VALUE;
import static org.csstudio.utility.ldap.LdapFieldsAndAttributes.EREN_FIELD_NAME;
import static org.csstudio.utility.ldap.LdapFieldsAndAttributes.OU_FIELD_NAME;
import static org.csstudio.utility.ldap.LdapUtils.createLdapQuery;

import java.net.URL;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Rdn;

import org.apache.log4j.Logger;
import org.csstudio.alarm.service.declaration.LdapEpicsAlarmCfgObjectClass;
import org.csstudio.alarm.treeView.AlarmTreePlugin;
import org.csstudio.alarm.treeView.model.AbstractAlarmTreeNode;
import org.csstudio.alarm.treeView.model.AlarmTreeNodePropertyId;
import org.csstudio.alarm.treeView.model.IAlarmSubtreeNode;
import org.csstudio.alarm.treeView.model.IAlarmTreeNode;
import org.csstudio.alarm.treeView.model.ProcessVariableNode;
import org.csstudio.alarm.treeView.model.SubtreeNode;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.utility.ldap.reader.LdapSearchResult;
import org.csstudio.utility.ldap.service.ILdapService;

/**
 * Editor for the alarm tree in the LDAP directory. The methods of this class
 * update the LDAP directory and also the tree model.
 *
 * @author Joerg Rathlev
 */
public final class DirectoryEditor {

    private static final Logger LOG = CentralLogger.getInstance().getLogger(DirectoryEditor.class);

    private static final ILdapService LDAP_SERVICE = AlarmTreePlugin.getDefault().getLdapService();


    /**
     * Private constructor.
     */
    private DirectoryEditor() {
        // Don't instantiate.
    }

    /**
     * Deletes the given node from the directory.
     *
     * @param node
     *            the node to delete.
     * @throws DirectoryEditException
     *             if the node could not be deleted.
     */
    public static void delete(@Nonnull final IAlarmTreeNode node) throws DirectoryEditException {

//        final LdapName name = node.getLdapName();
//        if (name == null) {
//            throw new DirectoryEditException("LdapName for node " + node.getName() + " could not be created.", null);
//        }
//
//        if (!LDAP_SERVICE.removeComponent(name)) {
//            final String message = "Error unbinding directory entry " + name;
//            final DirectoryEditException editException =
//                new DirectoryEditException(message, new NamingException());
//            LOG.error(message, editException);
//            throw editException;
//        }
        final IAlarmSubtreeNode parent = node.getParent();
        if (parent != null) {
            parent.removeChild(node);
        }
    }


    /**
     * Modifies the help guidance attribute of the given node in the directory.
     *
     * @param node
     *            the node.
     * @param value
     *            the new value for the attribute. Use <code>null</code> to
     *            remove the attribute.
     * @throws DirectoryEditException
     *             if the attribute could not be modified.
     */
    public static void modifyHelpGuidance(@Nonnull final AbstractAlarmTreeNode node,
                                          @Nullable final String value) throws DirectoryEditException {
        modifyAttribute(node, AlarmTreeNodePropertyId.HELP_GUIDANCE.getLdapAttribute(), value);
        node.setHelpGuidance(value);
    }


    /**
     * Modifies the help page attribute of the given node in the directory.
     *
     * @param node
     *            the node.
     * @param value
     *            the new value for the attribute. Use <code>null</code> to
     *            remove the attribute.
     * @throws DirectoryEditException
     *             if the attribute could not be modified.
     */
    public static void modifyHelpPage(@Nonnull final AbstractAlarmTreeNode node,
                                      @Nullable final URL value) throws DirectoryEditException {
        modifyAttribute(node,
                        AlarmTreeNodePropertyId.HELP_PAGE.getLdapAttribute(),
                        value != null ? value.toString() : null);
        node.setHelpPage(value);
    }


    /**
     * Modifies the CSS alarm display attribute of the given node in the
     * directory.
     *
     * @param node
     *            the node.
     * @param value
     *            the new value for the attribute. Use <code>null</code> to
     *            remove the attribute.
     * @throws DirectoryEditException
     *             if the attribute could not be modified.
     */
    public static void modifyCssAlarmDisplay(@Nonnull final AbstractAlarmTreeNode node,
                                             @Nullable final String value) throws DirectoryEditException {
        modifyAttribute(node, AlarmTreeNodePropertyId.CSS_ALARM_DISPLAY.getLdapAttribute(), value);
        node.setCssAlarmDisplay(value);
    }


    /**
     * Modifies the CSS display attribute of the given node in the directory.
     *
     * @param node
     *            the node.
     * @param value
     *            the new value for the attribute. Use <code>null</code> to
     *            remove the attribute.
     * @throws DirectoryEditException
     *             if the attribute could not be modified.
     */
    public static void modifyCssDisplay(@Nonnull final AbstractAlarmTreeNode node,
                                        @Nullable final String value) throws DirectoryEditException {
        modifyAttribute(node, AlarmTreeNodePropertyId.CSS_DISPLAY.getLdapAttribute(), value);
        node.setCssDisplay(value);
    }


    /**
     * Modifies the CSS strip chart attribute of the given node in the
     * directory.
     *
     * @param node
     *            the node.
     * @param value
     *            the new value for the attribute. Use <code>null</code> to
     *            remove the attribute.
     * @throws DirectoryEditException
     *             if the attribute could not be modified.
     */
    public static void modifyCssStripChart(@Nonnull final AbstractAlarmTreeNode node,
                                           @Nullable final String value) throws DirectoryEditException {
        modifyAttribute(node, AlarmTreeNodePropertyId.CSS_STRIP_CHART.getLdapAttribute(), value);
        node.setCssStripChart(value);
    }


    /**
     * Modifies an attribute of a directory entry.
     *
     * @param node
     *            the node.
     * @param attribute
     *            the id of the attribute to modify.
     * @param value
     *            the new attribute value. Use <code>null</code> to remove the
     *            attribute.
     * @throws DirectoryEditException
     *             if the attribute could not be modified.
     */
    private static void modifyAttribute(@Nonnull final IAlarmTreeNode node,
                                        @Nonnull final String attribute,
                                        @Nullable final String value)
    throws DirectoryEditException {

//        final LdapName name;
//        try {
//            name = node.getLdapName();
//            if (name == null) {
//                throw new InvalidNameException("Node " + node.getName() + " could not construct a valid LdapName." );
//            }
//            final Attribute attr = new BasicAttribute(attribute, value);
//            final int op = value == null ? DirContext.REMOVE_ATTRIBUTE : DirContext.REPLACE_ATTRIBUTE;
//            final ModificationItem[] mods = new ModificationItem[] {new ModificationItem(op, attr)};
//
//            LOG.debug(DirectoryEditor.class, name + ": " + mods[0]);
//            LDAP_SERVICE.modifyAttributes(name, mods);
//        } catch (final NamingException e) {
//            LOG.error(DirectoryEditor.class, "Failed to modify attributes for " + node.getName() + " and operation " + value, e);
//            throw new DirectoryEditException(e.getMessage(), e);
//        }

        final AlarmTreeNodePropertyId propId = AlarmTreeNodePropertyId.getIdByLdapAttribute(attribute);
        if (propId != null) {
            node.setProperty(propId, value);
        } else {
            LOG.warn("No alarm tree node property exists for " + attribute);
        }
    }

    /**
     * Renames a node.
     *
     * @param node
     *            the node.
     * @param newName
     *            the new name. This should be just the simple name, not an LDAP
     *            name.
     * @throws DirectoryEditException
     *             if an error occurs.
     */
    public static void rename(@Nonnull final IAlarmTreeNode node, @Nonnull final String newName)
        throws DirectoryEditException {

//        try {
//            final LdapName oldLdapName = node.getLdapName();
//            if (oldLdapName == null) {
//                throw new DirectoryEditException("LdapName for node " + node.getName() + "could not be created.", null);
//            }
//            final String type = oldLdapName.getRdn(oldLdapName.size() - 1).getType();
//
//            final Rdn newRdn = new Rdn(type, newName);
//            final LdapName newLdapName = (LdapName) oldLdapName.getPrefix(oldLdapName.size() - 1);
//            newLdapName.add(newRdn);
//            LDAP_SERVICE.rename(oldLdapName, newLdapName);

            node.setName(newName);
//        } catch (final NamingException e) {
//            LOG.error(DirectoryEditor.class, "Error renaming node", e);
//            throw new DirectoryEditException(e.getMessage(), e);
//        }
    }

    /**
     * Moves a node into a new subtree node. If the node is a subtree node, the
     * whole subtree will be moved, including its children.
     *
     * @param node
     *            the node.
     * @param target
     *            the target node which will become the new parent of the node.
     * @throws DirectoryEditException
     *             if an error occurs.
     */
    public static void moveNode(@Nonnull final IAlarmTreeNode node, @Nonnull final SubtreeNode target)
        throws DirectoryEditException {
        /*
         * Note: I tried to use _directory.rename(...) here, but that failed
         * with an "LDAP: error code 50 - Insufficient Access Rights". So for
         * now, this code uses copy-and-delete to move the node.
         */
        copyNode(node, target);
        deleteRecursively(node);
    }

    /**
     * Recursively deletes a node and all of its children.
     *
     * @param node
     *            the node.
     * @throws DirectoryEditException
     *             if an error occurs.
     */
    private static void deleteRecursively(@Nonnull final IAlarmTreeNode node)
    throws DirectoryEditException {
        if (node instanceof SubtreeNode) {
            deleteChildren((SubtreeNode) node);
        }
        delete(node);
    }

    /**
     * Deletes the children of a subtree node (recursively).
     *
     * @param node
     *            the node.
     * @throws DirectoryEditException
     *             if an error occurs.
     */
    private static void deleteChildren(@Nonnull final SubtreeNode node)
    throws DirectoryEditException {
        for (final IAlarmTreeNode child : node.getChildren()) {
            deleteRecursively(child);
        }
    }


    /**
     * Creates a copy of a node under a new subtree node. If the node to be
     * copied is a subtree node, all of its children will be copied into the new
     * node, too.
     *
     * @param node
     *            the original node.
     * @param target
     *            the target into which the node will be copied.
     * @throws DirectoryEditException
     *             if an error occurs.
     */
    @Nonnull
    public static IAlarmTreeNode copyNode(@Nonnull final IAlarmTreeNode node,
                                          @Nonnull final SubtreeNode target)
    throws DirectoryEditException {

        if (node instanceof ProcessVariableNode) {
            return copyProcessVariableNode((ProcessVariableNode) node, target);
        }
        final SubtreeNode copy =
            new SubtreeNode.Builder(node.getName(), node.getObjectClass()).setParent(target).build();
        copyProperties((SubtreeNode) node, copy);

        for (final IAlarmTreeNode child : ((SubtreeNode) node).getChildren()) {
            copyNode(child, copy);
        }
        return copy;
    }


    /**
     * Creates a new process variable node which is a copy of the given process
     * variable node.
     *
     * @param node
     *            the original.
     * @param target
     *            the target.
     * @throws DirectoryEditException
     *             if an error occurs.
     */
    @Nonnull
    private static IAlarmTreeNode copyProcessVariableNode(@Nonnull final ProcessVariableNode node,
                                                          @Nonnull final SubtreeNode target)
    throws DirectoryEditException {

        final ProcessVariableNode copy =
            new ProcessVariableNode.Builder(node.getName()).setParent(target).build();
        copy.updateAlarm(node.getAlarm());
        copy.setHighestUnacknowledgedAlarm(node.getHighestUnacknowledgedAlarm());
        copyProperties(node, copy);
        return copy;
    }

//    /**
//     * Copies an entry in the LDAP directory. All of the entries attributes will
//     * be copied into the new entry.
//     *
//     * @param source
//     *            the original node which will be copied.
//     * @param target
//     *            the target node which will be the parent of the copy.
//     * @throws DirectoryEditException
//     *             if an error occurs.
//     */
//    private static void copyDirectoryEntry(@Nonnull final IAlarmTreeNode source,
//                                           @Nonnull final IAlarmTreeNode target) throws DirectoryEditException {
//        try {
//            final LdapName ldapName = source.getLdapName();
//            Attributes attributes = LDAP_SERVICE.getAttributes(ldapName);
//            if (attributes != null) {
//                attributes = (Attributes) attributes.clone();
//            }
//            final LdapName newName =
//                (LdapName) target.getLdapName().add(new Rdn(source.getObjectClass().getNodeTypeName(), source.getName()));
//            if (!LDAP_SERVICE.createComponent(newName, attributes)) {
//                throw new NamingException("Error binding component.");
//            }
//        } catch (final NamingException e) {
//            LOG.error(DirectoryEditor.class,
//                      "Error creating directory entry", e);
//            throw new DirectoryEditException(e.getMessage(), e);
//        }
//    }

    /**
     * Copies the properties from one node to another node.
     *
     * @param source
     *            the source.
     * @param destination
     *            the destination.
     */
    private static void copyProperties(@Nonnull final AbstractAlarmTreeNode source,
                                       @Nonnull final AbstractAlarmTreeNode destination) {
        for (final AlarmTreeNodePropertyId id : AlarmTreeNodePropertyId.values()) {
            final String value = source.getOwnProperty(id);
            if (value != null) {
                destination.setProperty(id, value);
            }
        }
    }


    /**
     * Creates an entry for a process variable record (eren) in the directory
     * below the given parent.
     *
     * @param parent
     *            the parent node.
     * @param recordName
     *            the name of the process variable record.
     * @throws DirectoryEditException if the entry could not be created.
     */
    public static void createProcessVariableRecord(@Nonnull final SubtreeNode parent,
                                                   @Nonnull final String recordName)
    throws DirectoryEditException {
        try {
            final Rdn rdn = new Rdn(LdapEpicsAlarmCfgObjectClass.RECORD.getNodeTypeName(), recordName);
//            final LdapName fullName = (LdapName) ((LdapName) parent.getLdapName().clone()).add(rdn);
            Attributes attrs = createBaseAttributesForEntry(LdapEpicsAlarmCfgObjectClass.RECORD, rdn);
            // TODO (jpenning) : retrieve initial alarm states not from LDAP (Epics-Control) but from DAL
            attrs = addAlarmAttributes(attrs, recordName);
//            createEntry(fullName, attrs);
            final ProcessVariableNode node = new ProcessVariableNode.Builder(recordName).setParent(parent).build();
            AlarmTreeNodeModifier.setAlarmState(node, attrs);
        } catch (final NamingException e) {
            LOG.error("Error creating directory entry", e);
            throw new DirectoryEditException(e.getMessage(), e);
        }
    }


    /**
     * Creates an entry for a component (ecom) in the directory below the given
     * parent.
     *
     * @param parent
     *            the parent node.
     * @param componentName
     *            the name of the component.
     * @throws DirectoryEditException
     *             if the entry could not be created.
     */
    public static void createComponent(@Nonnull final SubtreeNode parent,
                                       @Nonnull final String componentName) throws DirectoryEditException {

//        createEntry(parent.getLdapName(), componentName, LdapEpicsAlarmCfgObjectClass.COMPONENT);
        new SubtreeNode.Builder(componentName, LdapEpicsAlarmCfgObjectClass.COMPONENT).setParent(parent).build();
    }


//    /**
//     * Creates a new directory entry.
//     *
//     * @param parentName
//     *            the full name of the parent entry.
//     * @param name
//     *            the name of the entry to be created.
//     * @param objectClass
//     *            the object class of the entry to be created.
//     * @throws DirectoryEditException
//     *             if the entry could not be created.
//     */
//    private static void createEntry(@Nonnull final LdapName parentName,
//                                    @Nonnull final String name,
//                                    @Nonnull final LdapEpicsAlarmCfgObjectClass objectClass) throws DirectoryEditException {
//        try {
//            final Rdn rdn = new Rdn(objectClass.getNodeTypeName(), name);
//            final LdapName fullName = (LdapName) ((LdapName) parentName.clone()).add(rdn);
//            final Attributes attrs = createBaseAttributesForEntry(objectClass, rdn);
//            createEntry(fullName, attrs);
//        } catch (final NamingException e) {
//            LOG.error(DirectoryEditor.class,
//                      "Error creating directory entry", e);
//            throw new DirectoryEditException(e.getMessage(), e);
//        }
//    }

//    /**
//     * Creates a new directory entry.
//     *
//     * @param name
//     *            the full name of the new entry.
//     * @param attributes
//     *            the attributes of the entry.
//     * @throws DirectoryEditException
//     *             if the entry could not be created.
//     */
//    private static void createEntry(@Nonnull final LdapName name,
//                                    @Nonnull final Attributes attributes) throws DirectoryEditException {
//        try {
//            LOG.debug(DirectoryEditor.class,
//                      "Creating entry " + name + " with attributes " + attributes);
//            if (!LDAP_SERVICE.createComponent(name, attributes)) {
//                throw new NamingException("Binding error of " + name);
//            }
//        } catch (final NamingException e) {
//            LOG.error(DirectoryEditor.class,
//                      "Error creating directory entry", e);
//            throw new DirectoryEditException(e.getMessage(), e);
//        }
//    }

    /**
     * Returns the base attributes (name, objectclass and EPICS type) for a new
     * entry with the given object class and name.
     *
     * @param objectClass
     *            the object class of the new entry.
     * @param rdn
     *            the relative name of the entry.
     * @return the attributes for the new entry.
     */
    @Nonnull
    private static Attributes createBaseAttributesForEntry(@Nonnull final LdapEpicsAlarmCfgObjectClass objectClass,
                                                           @Nonnull final Rdn rdn) {
        final Attributes result = rdn.toAttributes();
        result.put(ATTR_FIELD_OBJECT_CLASS, objectClass.getDescription());
        //result.put(ATTR_FIELD_CSS_TYPE, objectClass.getCssType());
        return result;
    }

    /**
     * Reads the alarm attributes from the EpicsControls path of the LDAP
     * directory and adds the result to a new copy of the the target attributes.
     *
     * @param target
     *            an (probably) extended copy the attributes
     * @param recordName
     *            the name of the record
     * @throws NamingException
     */
    @CheckReturnValue
    private static Attributes addAlarmAttributes(@Nonnull final Attributes target,
                                                 @Nonnull final String recordName) throws NamingException {

        final Attributes attributes = (Attributes) target.clone();

        final LdapSearchResult searchResult =
            LDAP_SERVICE.retrieveSearchResultSynchronously(createLdapQuery(OU_FIELD_NAME, EPICS_CTRL_FIELD_VALUE),
                                                           createLdapQuery(EREN_FIELD_NAME, recordName).toString(),
                                                           SearchControls.SUBTREE_SCOPE);
        if (searchResult != null) {
            final Set<SearchResult> answer = searchResult.getAnswerSet();
            if (!answer.isEmpty()) {
                final SearchResult result = answer.iterator().next();
                final Attributes foundAttributes = result.getAttributes();
                final String[] attrs = {ATTR_FIELD_ALARM_SEVERITY,
                                        ATTR_FIELD_ALARM_TIMESTAMP,
                                        ATTR_FIELD_ALARM_HIGH_UNACK};
                for (final String attr : attrs) {
                    final Attribute foundAttribute = foundAttributes.get(attr);
                    if (foundAttribute != null) {
                        attributes.put((Attribute) foundAttribute.clone());
                    }
                }
            }
        }
        return attributes;
    }
}
