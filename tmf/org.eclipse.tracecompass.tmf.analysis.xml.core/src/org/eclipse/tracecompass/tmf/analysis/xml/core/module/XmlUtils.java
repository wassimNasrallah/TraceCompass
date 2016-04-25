/*******************************************************************************
 * Copyright (c) 2014, 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.analysis.xml.core.module;

import static org.eclipse.tracecompass.common.core.NonNullUtils.nullToEmptyString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.Activator;
import org.eclipse.tracecompass.tmf.analysis.xml.core.stateprovider.TmfXmlStrings;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Class containing some utilities for the XML plug-in packages: for example, it
 * manages the XML files and validates them
 *
 * @author Geneviève Bastien
 */
public class XmlUtils {

    /** Sub-directory of the plug-in where XML files are stored */
    private static final String XML_DIRECTORY = "xml_files"; //$NON-NLS-1$

    /** Name of the XSD schema file */
    private static final String XSD = "xmlDefinition.xsd"; //$NON-NLS-1$

    /**
     * Extension for XML files
     * @since 2.0
     */
    public static final String XML_EXTENSION = "xml"; //$NON-NLS-1$

    /** Make this class non-instantiable */
    private XmlUtils() {

    }

    /**
     * Get the path where the XML files are stored. Create it if it does not
     * exist
     *
     * @return path to XML files
     */
    public static IPath getXmlFilesPath() {
        IPath path = Activator.getDefault().getStateLocation();
        path = path.addTrailingSeparator().append(XML_DIRECTORY);

        /* Check if directory exists, otherwise create it */
        File dir = path.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }

        return path;
    }

    /**
     * Validate the XML file input with the XSD schema
     *
     * @param xmlFile
     *            XML file to validate
     * @return True if the XML validates
     */
    public static IStatus xmlValidate(File xmlFile) {
        URL url = XmlUtils.class.getResource(XSD);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source xmlSource = new StreamSource(xmlFile);
        try {
            Schema schema = schemaFactory.newSchema(url);
            Validator validator = schema.newValidator();
            validator.validate(xmlSource);
        } catch (SAXParseException e) {
            String error = NLS.bind(Messages.XmlUtils_XmlParseError, e.getLineNumber(), e.getLocalizedMessage());
            Activator.logError(error);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, error, e);
        } catch (SAXException e) {
            String error = NLS.bind(Messages.XmlUtils_XmlValidationError, e.getLocalizedMessage());
            Activator.logError(error);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, error, e);
        } catch (IOException e) {
            String error = Messages.XmlUtils_XmlValidateError;
            Activator.logError("IO exception occurred", e); //$NON-NLS-1$
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, error, e);
        }
        return Status.OK_STATUS;
    }

    /**
     * Adds an XML file to the plugin's path. The XML file should have been
     * validated using the {@link XmlUtils#xmlValidate(File)} method before
     * calling this method.
     *
     * @param fromFile
     *            The XML file to add
     * @return Whether the file was successfully added
     */
    public static IStatus addXmlFile(File fromFile) {

        /* Copy file to path */
        File toFile = getXmlFilesPath().addTrailingSeparator().append(fromFile.getName()).toFile();

        return copyXmlFile(fromFile, toFile);
    }

    /**
     * List all files under the XML analysis files path. It returns a map where
     * the key is the file name.
     *
     * @return A map with all the XML analysis files
     * @since 2.0
     */
    public static synchronized @NonNull Map<String, File> listFiles() {
        IPath pathToFiles = XmlUtils.getXmlFilesPath();
        File folder = pathToFiles.toFile();

        Map<String, File> fileMap = new HashMap<>();
        if ((folder.isDirectory() && folder.exists())) {
            File[] listOfFiles = folder.listFiles();
            if (listOfFiles != null) {
                for (File file : listOfFiles) {
                    IPath path = new Path(file.getName());
                    if (path.getFileExtension().equals(XML_EXTENSION)) {
                        fileMap.put(file.getName(), file);
                    }
                }
            } else {
                Activator.logError("I/O error occured while accessing files in folder " + folder.getPath()); //$NON-NLS-1$
            }
        }
        return Collections.unmodifiableMap(fileMap);
    }

    /**
     * Delete an XML analysis file
     *
     * @param name
     *            The XML file to delete
     * @since 2.0
     */
    public static void deleteFile(String name) {
        Map<String, File> files = listFiles();
        File file = files.get(name);
        if (file == null) {
            return;
        }
        file.delete();
    }

    /**
     * Export an XML analysis file to an external path
     *
     * @param from
     *            The name of the file to export
     * @param to
     *            The full path of the file to write to
     * @return Whether the file was successfully exported
     * @since 2.0
     */
    public static IStatus exportXmlFile(String from, String to) {

        /* Copy file to path */
        File fromFile = getXmlFilesPath().addTrailingSeparator().append(from).toFile();

        if (!fromFile.exists()) {
            Activator.logError("Failed to find XML analysis file " + fromFile.getName()); //$NON-NLS-1$
            return Status.CANCEL_STATUS;
        }

        File toFile = new File(to);

        return copyXmlFile(fromFile, toFile);
    }

    private static IStatus copyXmlFile(File fromFile, File toFile) {
        try {
            if (!toFile.exists()) {
                toFile.createNewFile();
            }
        } catch (IOException e) {
            String error = Messages.XmlUtils_ErrorCopyingFile;
            Activator.logError(error, e);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, error, e);
        }

        try (FileInputStream fis = new FileInputStream(fromFile);
                FileOutputStream fos = new FileOutputStream(toFile);
                FileChannel source = fis.getChannel();
                FileChannel destination = fos.getChannel();) {
            destination.transferFrom(source, 0, source.size());
        } catch (IOException e) {
            String error = Messages.XmlUtils_ErrorCopyingFile;
            Activator.logError(error, e);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, error, e);
        }
        return Status.OK_STATUS;
    }

    /**
     * Get the IDs of all the analysis described in a single file
     *
     * @param fileName
     *            The file name
     * @return The list of IDs
     * @since 2.0
     */
    public static List<String> getAnalysisIdsFromFile(String fileName) {
        List<String> ids = new ArrayList<>();
        File file = getXmlFilesPath().addTrailingSeparator().append(fileName).toFile();
        if (file.exists()) {
            try {
                /* Load the XML File */
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder;
                dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(file);
                doc.getDocumentElement().normalize();

                /* get State Providers modules */
                NodeList stateproviderNodes = doc.getElementsByTagName(TmfXmlStrings.STATE_PROVIDER);
                for (int i = 0; i < stateproviderNodes.getLength(); i++) {
                    ids.add(nullToEmptyString(((Element) stateproviderNodes.item(i)).getAttribute(TmfXmlStrings.ID)));
                }
            } catch (ParserConfigurationException | SAXException | IOException e) {
                Activator.logError("Failed to get analyses IDs from " + fileName); //$NON-NLS-1$
            }
        }
        return ids;
    }

    /**
     * Get only the XML element children of an XML element.
     *
     * @param parent
     *            The parent element to get children from
     * @return The list of children Element of the parent
     */
    public static List<@Nullable Element> getChildElements(Element parent) {
        NodeList childNodes = parent.getChildNodes();
        List<@Nullable Element> childElements = new ArrayList<>();
        for (int index = 0; index < childNodes.getLength(); index++) {
            if (childNodes.item(index).getNodeType() == Node.ELEMENT_NODE) {
                childElements.add((Element) childNodes.item(index));
            }
        }
        return childElements;
    }

    /**
     * Get the XML children element of an XML element, but only those of a
     * certain type
     *
     * @param parent
     *            The parent element to get the children from
     * @param elementTag
     *            The tag of the elements to return
     * @return The list of children {@link Element} of the parent
     */
    public static List<@NonNull Element> getChildElements(Element parent, String elementTag) {
        /* get the state providers and find the corresponding one */
        NodeList nodes = parent.getElementsByTagName(elementTag);
        List<@NonNull Element> childElements = new ArrayList<>();

        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (node.getParentNode().equals(parent)) {
                childElements.add(node);
            }
        }
        return childElements;
    }

    /**
     * Return the node element corresponding to the requested type in the file.
     *
     * TODO: Nothing prevents from having duplicate type -> id in a same file.
     * That should not be allowed. If you want an element with the same ID as
     * another one, it should be in a different file and we should check it at
     * validation time.
     *
     * @param filePath
     *            The absolute path to the XML file
     * @param elementType
     *            The type of top level element to search for
     * @param elementId
     *            The ID of the desired element
     * @return The XML element or <code>null</code> if not found
     */
    public static Element getElementInFile(String filePath, @NonNull String elementType, @NonNull String elementId) {

        if (filePath == null) {
            return null;
        }

        IPath path = new Path(filePath);
        File file = path.toFile();
        if (file == null || !file.exists() || !file.isFile() || !xmlValidate(file).isOK()) {
            return null;
        }

        try {
            /* Load the XML File */
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;

            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            /* get the state providers and find the corresponding one */
            NodeList nodes = doc.getElementsByTagName(elementType);
            Element foundNode = null;

            for (int i = 0; i < nodes.getLength(); i++) {
                Element node = (Element) nodes.item(i);
                String id = node.getAttribute(TmfXmlStrings.ID);
                if (id.equals(elementId)) {
                    foundNode = node;
                }
            }
            return foundNode;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return null;
        }

    }
}
