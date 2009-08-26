/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdklib.internal.project;

import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.SdkManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class to load and save project properties for both ADT and Ant-based build.
 *
 */
public final class ProjectProperties {
    /** The property name for the project target */
    public final static String PROPERTY_TARGET = "target";

    public final static String PROPERTY_SDK = "sdk.dir";
    // LEGACY - compatibility with 1.6 and before
    public final static String PROPERTY_SDK_LEGACY = "sdk-location";

    public final static String PROPERTY_APP_PACKAGE = "application.package";
    // LEGACY - compatibility with 1.6 and before
    public final static String PROPERTY_APP_PACKAGE_LEGACY = "application-package";

    public final static String PROPERTY_SPLIT_BY_DENSITY = "split.density";

    public static enum PropertyType {
        BUILD("build.properties", BUILD_HEADER),
        DEFAULT("default.properties", DEFAULT_HEADER),
        LOCAL("local.properties", LOCAL_HEADER);

        private final String mFilename;
        private final String mHeader;

        PropertyType(String filename, String header) {
            mFilename = filename;
            mHeader = header;
        }

        public String getFilename() {
            return mFilename;
        }
    }

    private final static String LOCAL_HEADER =
//           1-------10--------20--------30--------40--------50--------60--------70--------80
            "# This file is automatically generated by Android Tools.\n" +
            "# Do not modify this file -- YOUR CHANGES WILL BE ERASED!\n" +
            "# \n" +
            "# This file must *NOT* be checked in Version Control Systems,\n" +
            "# as it contains information specific to your local configuration.\n" +
            "\n";

    private final static String DEFAULT_HEADER =
//          1-------10--------20--------30--------40--------50--------60--------70--------80
           "# This file is automatically generated by Android Tools.\n" +
           "# Do not modify this file -- YOUR CHANGES WILL BE ERASED!\n" +
           "# \n" +
           "# This file must be checked in Version Control Systems.\n" +
           "# \n" +
           "# To customize properties used by the Ant build system use,\n" +
           "# \"build.properties\", and override values to adapt the script to your\n" +
           "# project structure.\n" +
           "\n";

    private final static String BUILD_HEADER =
//          1-------10--------20--------30--------40--------50--------60--------70--------80
           "# This file is used to override default values used by the Ant build system.\n" +
           "# \n" +
           "# This file must be checked in Version Control Systems, as it is\n" +
           "# integral to the build system of your project.\n" +
           "\n" +
           "# This file is only used by the Ant script.\n" +
           "\n" +
           "# You can use this to override default values such as\n" +
           "#  'source.dir' for the location of your java source folder and\n" +
           "#  'out.dir' for the location of your output folder.\n" +
           "\n" +
           "# You can also use it define how the release builds are signed by declaring\n" +
           "# the following properties:\n" +
           "#  'key.store' for the location of your keystore and\n" +
           "#  'key.alias' for the name of the key to use.\n" +
           "# The password will be asked during the build when you use the 'release' target.\n" +
           "\n";

    private final static Map<String, String> COMMENT_MAP = new HashMap<String, String>();
    static {
//               1-------10--------20--------30--------40--------50--------60--------70--------80
        COMMENT_MAP.put(PROPERTY_TARGET,
                "# Project target.\n");
        COMMENT_MAP.put(PROPERTY_SPLIT_BY_DENSITY,
                "# Indicates whether an apk should be generated for each density.\n");
        COMMENT_MAP.put(PROPERTY_SDK,
                "# location of the SDK. This is only used by Ant\n" +
                "# For customization when using a Version Control System, please read the\n" +
                "# header note.\n");
        COMMENT_MAP.put(PROPERTY_APP_PACKAGE,
                "# The name of your application package as defined in the manifest.\n" +
                "# Used by the 'uninstall' rule.\n");
    }

    private final String mProjectFolderOsPath;
    private final Map<String, String> mProperties;
    private final PropertyType mType;

    /**
     * Loads a project properties file and return a {@link ProjectProperties} object
     * containing the properties
     *
     * @param projectFolderOsPath the project folder.
     * @param type One the possible {@link PropertyType}s.
     */
    public static ProjectProperties load(String projectFolderOsPath, PropertyType type) {
        File projectFolder = new File(projectFolderOsPath);
        if (projectFolder.isDirectory()) {
            File defaultFile = new File(projectFolder, type.mFilename);
            if (defaultFile.isFile()) {
                Map<String, String> map = SdkManager.parsePropertyFile(defaultFile, null /* log */);
                if (map != null) {
                    return new ProjectProperties(projectFolderOsPath, map, type);
                }
            }
        }
        return null;
    }

    /**
     * Merges all properties from the given file into the current properties.
     * <p/>
     * This emulates the Ant behavior: existing properties are <em>not</em> overriden.
     * Only new undefined properties become defined.
     * <p/>
     * Typical usage:
     * <ul>
     * <li>Create a ProjectProperties with {@link PropertyType#BUILD}
     * <li>Merge in values using {@link PropertyType#DEFAULT}
     * <li>The result is that this contains all the properties from default plus those
     *     overridden by the build.properties file.
     * </ul>
     *
     * @param type One the possible {@link PropertyType}s.
     * @return this object, for chaining.
     */
    public ProjectProperties merge(PropertyType type) {
        File projectFolder = new File(mProjectFolderOsPath);
        if (projectFolder.isDirectory()) {
            File defaultFile = new File(projectFolder, type.mFilename);
            if (defaultFile.isFile()) {
                Map<String, String> map = SdkManager.parsePropertyFile(defaultFile, null /* log */);
                if (map != null) {
                    for(Entry<String, String> entry : map.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        if (!mProperties.containsKey(key) && value != null) {
                            mProperties.put(key, value);
                        }
                    }
                }
            }
        }
        return this;
    }

    /**
     * Creates a new project properties object, with no properties.
     * <p/>The file is not created until {@link #save()} is called.
     * @param projectFolderOsPath the project folder.
     * @param type
     */
    public static ProjectProperties create(String projectFolderOsPath, PropertyType type) {
        // create and return a ProjectProperties with an empty map.
        return new ProjectProperties(projectFolderOsPath, new HashMap<String, String>(), type);
    }

    /**
     * Sets a new properties. If a property with the same name already exists, it is replaced.
     * @param name the name of the property.
     * @param value the value of the property.
     */
    public void setProperty(String name, String value) {
        mProperties.put(name, value);
    }

    /**
     * Sets the target property to the given {@link IAndroidTarget} object.
     * @param target the Android target.
     */
    public void setAndroidTarget(IAndroidTarget target) {
        assert mType == PropertyType.DEFAULT;
        mProperties.put(PROPERTY_TARGET, target.hashString());
    }

    /**
     * Returns the value of a property.
     * @param name the name of the property.
     * @return the property value or null if the property is not set.
     */
    public String getProperty(String name) {
        return mProperties.get(name);
    }

    /**
     * Removes a property and returns its previous value (or null if the property did not exist).
     * @param name the name of the property to remove.
     */
    public String removeProperty(String name) {
        return mProperties.remove(name);
    }

    /**
     * Saves the property file, using UTF-8 encoding.
     * @throws IOException
     */
    public void save() throws IOException {
        File toSave = new File(mProjectFolderOsPath, mType.mFilename);

        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(toSave),
                SdkConstants.INI_CHARSET);

        // write the header
        writer.write(mType.mHeader);

        // write the properties.
        for (Entry<String, String> entry : mProperties.entrySet()) {
            String comment = COMMENT_MAP.get(entry.getKey());
            if (comment != null) {
                writer.write(comment);
            }
            String value = entry.getValue();
            value = value.replaceAll("\\\\", "\\\\\\\\");
            writer.write(String.format("%s=%s\n", entry.getKey(), value));
        }

        // close the file to flush
        writer.close();
    }

    /**
     * Private constructor.
     * <p/>
     * Use {@link #load(String, PropertyType)} or {@link #create(String, PropertyType)}
     * to instantiate.
     */
    private ProjectProperties(String projectFolderOsPath, Map<String, String> map,
            PropertyType type) {
        mProjectFolderOsPath = projectFolderOsPath;
        mProperties = map;
        mType = type;
    }
}
