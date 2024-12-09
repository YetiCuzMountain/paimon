/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.paimon.format.orc;

import org.apache.orc.OrcConf;
import org.apache.paimon.format.FileFormat;
import org.apache.paimon.format.FileFormatFactory;
import org.apache.paimon.options.MemorySize;
import org.apache.paimon.options.Options;

import org.apache.hadoop.conf.Configuration;

import java.util.Objects;
import java.util.Properties;

// import static org.apache.paimon.CoreOptions.HADOOP_CONF_REUSE_ENABLED;

/** Factory to create {@link OrcFileFormat}. */
public class OrcFileFormatFactory implements FileFormatFactory {

    public static final String IDENTIFIER = "orc";

    public Configuration orcConf;

    @Override
    public String identifier() {
        return IDENTIFIER;
    }

    @Override
    public OrcFileFormat create(FormatContext formatContext) {
        if (Objects.isNull(orcConf)) {
            orcConf = new Configuration();
            getOrcProperties(supplyDefaultOptions(formatContext.options()), formatContext)
                    .forEach((k, v) -> orcConf.set(k.toString(), v.toString()));
        }
        return new OrcFileFormat(formatContext,orcConf);
    }

    private Options supplyDefaultOptions(Options options) {
        if (!options.containsKey("compress")) {
            Properties properties = new Properties();
            options.addAllToProperties(properties);
            properties.setProperty("compress", "lz4");
            Options newOptions = new Options();
            properties.forEach((k, v) -> newOptions.setString(k.toString(), v.toString()));
            return newOptions;
        }
        return options;
    }

    private Properties getOrcProperties(Options options, FormatContext formatContext) {
        Properties orcProperties = new Properties();
        orcProperties.putAll(FileFormat.getIdentifierPrefixOptions(options,"orc").toMap());

        if (!orcProperties.containsKey(OrcConf.COMPRESSION_ZSTD_LEVEL.getAttribute())) {
            orcProperties.setProperty(
                    OrcConf.COMPRESSION_ZSTD_LEVEL.getAttribute(),
                    String.valueOf(formatContext.zstdLevel()));
        }

        MemorySize blockSize = formatContext.blockSize();
        if (blockSize != null) {
            orcProperties.setProperty(
                    OrcConf.STRIPE_SIZE.getAttribute(), String.valueOf(blockSize.getBytes()));
        }

        return orcProperties;
    }
}
