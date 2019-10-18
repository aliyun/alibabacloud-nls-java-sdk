/*
 * Copyright 2015 Alibaba Group Holding Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.nls.client.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * native lib load util
 *
 * @author zhishen.ml
 * @Date 2018/12/7.
 */
public class NativeLibUtil {
    private static Logger logger = LoggerFactory.getLogger(NativeLibUtil.class);

    public static enum Architecture {
        UNKNOWN,
        LINUX_32,
        LINUX_64,
        LINUX_ARM,
        LINUX_ARM64,
        WINDOWS_32,
        WINDOWS_64,
        OSX_32,
        OSX_64,
        OSX_PPC
    }

    private static enum Processor {
        UNKNOWN,
        INTEL_32,
        INTEL_64,
        PPC,
        ARM,
        AARCH_64
    }

    private static Architecture architecture = Architecture.UNKNOWN;

    public static Architecture getArchitecture() {
        if (Architecture.UNKNOWN == architecture) {
            final Processor processor = getProcessor();
            if (Processor.UNKNOWN != processor) {
                final String name = System.getProperty("os.name").toLowerCase();
                if (name.contains("nix") || name.contains("nux")) {
                    if (Processor.INTEL_32 == processor) {
                        architecture = Architecture.LINUX_32;
                    } else if (Processor.INTEL_64 == processor) {
                        architecture = Architecture.LINUX_64;
                    } else if (Processor.ARM == processor) {
                        architecture = Architecture.LINUX_ARM;
                    } else if (Processor.AARCH_64 == processor) {
                        architecture = Architecture.LINUX_ARM64;
                    }
                } else if (name.contains("win")) {
                    if (Processor.INTEL_32 == processor) {
                        architecture = Architecture.WINDOWS_32;
                    } else if (Processor.INTEL_64 == processor) {
                        architecture = Architecture.WINDOWS_64;
                    }
                } else if (name.contains("mac")) {
                    if (Processor.INTEL_32 == processor) {
                        architecture = Architecture.OSX_32;
                    } else if (Processor.INTEL_64 == processor) {
                        architecture = Architecture.OSX_64;
                    } else if (Processor.PPC == processor) {
                        architecture = Architecture.OSX_PPC;
                    }
                }
            }
        }
        logger.debug("architecture is " + architecture + " os.name is " +
            System.getProperty("os.name").toLowerCase());
        return architecture;
    }

    private static Processor getProcessor() {
        Processor processor = Processor.UNKNOWN;
        int bits;

        // Note that this is actually the architecture of the installed JVM.
        final String arch = System.getProperty("os.arch").toLowerCase();

        if (arch.contains("arm")) {
            processor = Processor.ARM;
        } else if (arch.contains("aarch64")) {
            processor = Processor.AARCH_64;
        } else if (arch.contains("ppc")) {
            processor = Processor.PPC;
        } else if (arch.contains("86") || arch.contains("amd")) {
            bits = 32;
            if (arch.contains("64")) {
                bits = 64;
            }
            processor = (32 == bits) ? Processor.INTEL_32 : Processor.INTEL_64;
        }
        logger.debug("processor is " + processor + " os.arch is " +
            System.getProperty("os.arch").toLowerCase());
        return processor;
    }

    public static String getPlatformLibraryName(final String libName) {
        String name = null;
        switch (getArchitecture()) {
            case LINUX_32:
            case LINUX_64:
            case LINUX_ARM:
            case LINUX_ARM64:
                name = "lib" + libName + ".so";
                break;
            case WINDOWS_32:
            case WINDOWS_64:
                name = libName + ".dll";
                break;
            case OSX_32:
            case OSX_64:
                name = "lib" + libName + ".dylib";
                break;
            default:
                break;
        }
        logger.debug("native library name " + name);
        return name;
    }

    public static void loadFromJar(String libName) throws Exception {
        String path = String.format("natives/%s/%s",
            getArchitecture().name().toLowerCase(),
            getPlatformLibraryName(libName));
        logger.info("begin to load from {}", path);
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        File file = File.createTempFile("lib", ".lib");
        OutputStream os = new FileOutputStream(file);
        byte[] buffer = new byte[8192];
        int length;
        while ((length = is.read(buffer)) != -1) {
            os.write(buffer, 0, length);
        }
        is.close();
        os.close();
        System.load(file.getAbsolutePath());
        file.deleteOnExit();
        logger.info("finish to load from jar {}", path);
    }

}
