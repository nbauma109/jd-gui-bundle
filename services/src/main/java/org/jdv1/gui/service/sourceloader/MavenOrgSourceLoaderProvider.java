/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jdv1.gui.service.sourceloader;

import org.apache.commons.io.IOUtils;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ExceptionUtil;
import org.jd.gui.api.API;
import org.jd.gui.api.model.Container;
import org.jd.gui.model.container.entry.path.DirectoryEntryPath;
import org.jd.gui.spi.SourceLoader;
import org.jdv1.gui.service.preferencespanel.MavenOrgSourceLoaderPreferencesProvider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

public class MavenOrgSourceLoaderProvider implements SourceLoader {
    protected static final String MAVENORG_SEARCH_URL_PREFIX = "https://search.maven.org/solrsearch/select?q=1:%22";
    protected static final String MAVENORG_SEARCH_URL_SUFFIX = "%22&rows=20&wt=xml";

    protected static final String MAVENORG_LOAD_URL_PREFIX = "https://search.maven.org/classic/remotecontent?filepath=";
    protected static final String MAVENORG_LOAD_URL_SUFFIX = "-sources.jar";

    protected Set<Container.Entry> failed = new HashSet<>();
    protected Map<Container.Entry, File> cache = new HashMap<>();

    @Override
    public String getSource(API api, Container.Entry entry) {
        if (isActivated(api)) {
            String filters = api.getPreferences().get(MavenOrgSourceLoaderPreferencesProvider.FILTERS);

            if (filters == null || filters.isEmpty()) {
                filters = MavenOrgSourceLoaderPreferencesProvider.DEFAULT_FILTERS_VALUE;
            }

            if (accepted(filters, entry.getPath())) {
                return searchSource(entry, cache.get(entry.getContainer().getRoot().getParent()));
            }
        }

        return null;
    }

    @Override
    public String loadSource(API api, Container.Entry entry) {
        if (isActivated(api)) {
            String filters = api.getPreferences().get(MavenOrgSourceLoaderPreferencesProvider.FILTERS);

            if (filters == null || filters.isEmpty()) {
                filters = MavenOrgSourceLoaderPreferencesProvider.DEFAULT_FILTERS_VALUE;
            }

            if (accepted(filters, entry.getPath())) {
                return searchSource(entry, downloadSourceJarFile(entry.getContainer().getRoot().getParent()));
            }
        }

        return null;
    }

    @Override
    public File loadSourceFile(API api, Container.Entry entry) {
        return isActivated(api) ? downloadSourceJarFile(entry) : null;
    }

    private static boolean isActivated(API api) {
        return !"false".equals(api.getPreferences().get(MavenOrgSourceLoaderPreferencesProvider.ACTIVATED));
    }

    protected String searchSource(Container.Entry entry, File sourceJarFile) {
        if (sourceJarFile != null) {
            try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(sourceJarFile)))) {
                ZipEntry ze = zis.getNextEntry();
                String name = entry.getPath();

                name = name.substring(0, name.length()-6) + ".java"; // 6 = ".class".length()

                while (ze != null) {
                    if (ze.getName().equals(name)) {
                        return IOUtils.toString(zis, StandardCharsets.UTF_8);
                    }

                    ze = zis.getNextEntry();
                }

                zis.closeEntry();
            } catch (IOException e) {
                assert ExceptionUtil.printStackTrace(e);
            }
        }

        return null;
    }

    protected File downloadSourceJarFile(Container.Entry entry) {
        if (cache.containsKey(entry)) {
            return cache.get(entry);
        }

        if (!entry.isDirectory() && !failed.contains(entry)) {
            try {
                // SHA-1
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
                byte[] buffer = new byte[1024 * 2];

                try (DigestInputStream is = new DigestInputStream(entry.getInputStream(), messageDigest)) {
                    while (is.read(buffer) > -1) {
                        // read fully
                    }
                }

                byte[] array = messageDigest.digest();
                StringBuilder sb = new StringBuilder();

                for (byte b : array) {
                    sb.append(hexa((b & 255) >> 4));
                    sb.append(hexa(b & 15));
                }

                String sha1 = sb.toString();

                // Search artifact on maven.org
                URL searchUrl = new URL(MAVENORG_SEARCH_URL_PREFIX + sha1 + MAVENORG_SEARCH_URL_SUFFIX);
                boolean sourceAvailable = false;
                String id = null;
                String numFound = null;

                try (InputStream is = searchUrl.openStream()) {
                    XMLInputFactory factory = XMLInputFactory.newInstance();
                    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                    XMLStreamReader reader = factory.createXMLStreamReader(is);
                    String name = "";

                    int next;
					while (reader.hasNext()) {
                        next = reader.next();
						if (next == XMLStreamConstants.START_ELEMENT) {
							if ("str".equals(reader.getLocalName())) {
							    if ("id".equals(reader.getAttributeValue(null, "name"))) {
							        name = "id";
							    } else {
							        name = "str";
							    }
							} else if ("result".equals(reader.getLocalName())) {
							    numFound = reader.getAttributeValue(null, "numFound");
							} else {
							    name = "";
							}
						} else if (next == XMLStreamConstants.CHARACTERS) {
							if ("id".equals(name)) {
								id = reader.getText().trim();
							} else if ("str".equals(name)) {
								sourceAvailable |= MAVENORG_LOAD_URL_SUFFIX.equals(reader.getText().trim());
							}
						}
                    }

                    reader.close();
                }

                String groupId=null;
                String artifactId=null;
                String version=null;

                if ("0".equals(numFound)) {
                    // File not indexed by Apache Solr of maven.org -> Try to found groupId, artifactId, version in 'pom.properties'
                    Properties pomProperties = getPomProperties(entry);

                    if (pomProperties != null) {
                        groupId = pomProperties.getProperty("groupId");
                        artifactId = pomProperties.getProperty("artifactId");
                        version = pomProperties.getProperty("version");
                    }
                } else if ("1".equals(numFound) && sourceAvailable && id != null) {
                    int index1 = id.indexOf(':');
                    int index2 = id.lastIndexOf(':');

                    groupId = id.substring(0, index1);
                    artifactId = id.substring(index1+1, index2);
                    version = id.substring(index2+1);
                }

                if (groupId != null && artifactId != null) {
                    // Load source
                    String filePath = groupId.replace('.', '/') + '/' + artifactId + '/' + version + '/' + artifactId + '-' + version;
                    URL loadUrl = new URL(MAVENORG_LOAD_URL_PREFIX + filePath + MAVENORG_LOAD_URL_SUFFIX);
                    File tmpFile = File.createTempFile("jd-gui.tmp.", '.' + groupId + '_' + artifactId + '_' + version + MAVENORG_LOAD_URL_SUFFIX);

                    Files.delete(tmpFile.toPath());
                    tmpFile.deleteOnExit();

                    try (InputStream is = new BufferedInputStream(loadUrl.openStream()); OutputStream os = new BufferedOutputStream(new FileOutputStream(tmpFile))) {
                        IOUtils.copy(is, os);
                    }
                    cache.put(entry, tmpFile);
                    return tmpFile;
                }
            } catch (Exception e) {
                assert ExceptionUtil.printStackTrace(e);
            }
        }

        failed.add(entry);
        return null;
    }

    private static Properties getPomProperties(Container.Entry parent) {
        // Search 'META-INF/maven/*/*/pom.properties'
        Container.Entry child1 = parent.getChildren().get(new DirectoryEntryPath("META-INF"));
        if (child1 != null && child1.isDirectory()) {
            Container.Entry child2 = child1.getChildren().get(new DirectoryEntryPath("META-INF/maven"));
            if (child2 != null && child2.isDirectory()) {
                Collection<Container.Entry> children = child2.getChildren().values();
                if (children.size() == 1) {
                    Container.Entry entry = children.iterator().next();
                    if (entry.isDirectory()) {
                        children = entry.getChildren().values();
                        if (children.size() == 1) {
                            entry = children.iterator().next();
                            for (Container.Entry child3 : entry.getChildren().values()) {
                                if (!child3.isDirectory() && child3.getPath().endsWith("/pom.properties")) {
                                    // Load properties
                                    try (InputStream is = child3.getInputStream()) {
                                        Properties properties = new Properties();
                                        properties.load(is);
                                        return properties;
                                    } catch (Exception e) {
                                        assert ExceptionUtil.printStackTrace(e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static char hexa(int i) { return (char)( i <= 9 ? '0' + i : 'a' - 10 + i ); }

    protected boolean accepted(String filters, String path) {
        // 'filters' example : '+org +com.google +com.ibm +com.jcraft +com.springsource +com.sun -com +java +javax +sun +sunw'
        StringTokenizer tokenizer = new StringTokenizer(filters);

        String filter;
		while (tokenizer.hasMoreTokens()) {
            filter = tokenizer.nextToken();

            if (filter.length() > 1) {
                String prefix = filter.substring(1).replace('.', '/');

                if (prefix.charAt(prefix.length() - 1) != '/') {
                    prefix += '/';
                }

                if (path.startsWith(prefix)) {
                    return filter.charAt(0) == '+';
                }
            }
        }

        return false;
    }
}
