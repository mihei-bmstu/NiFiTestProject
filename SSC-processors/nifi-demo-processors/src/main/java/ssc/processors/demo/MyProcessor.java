/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ssc.processors.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.flowfile.attributes.CoreAttributes;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

@Tags({"example"})
@CapabilityDescription("Provide a description")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class MyProcessor extends AbstractProcessor {

    private ObjectMapper objectMapper;
    private Pattern pattern;

    public static final PropertyDescriptor MY_PROPERTY = new PropertyDescriptor
            .Builder().name("MY_PROPERTY")
            .displayName("My property")
            .description("Example Property")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor REGEX = new PropertyDescriptor
            .Builder().name("REGEX")
            .displayName("REGEX")
            .description("REGEX")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor PASS = new PropertyDescriptor
            .Builder().name("PASS")
            .displayName("Password")
            .description("Example Property")
            .required(true)
            .sensitive(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("REL_SUCCESS")
            .description("A Flowfile is routed to this relationship when everything goes well here")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("REL_FAILURE")
            .description("A Flowfile is routed to this relationship it can not be parsed or a problem happens")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(REGEX);
        descriptors.add(PASS);

        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        objectMapper = new ObjectMapper();
        String regex = context.getProperty(REGEX).getValue();
        pattern = Pattern.compile(regex);
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        try {
            flowFile = session.write(flowFile, (rawIn, rawOut) -> {
                try (final InputStream in = new BufferedInputStream(rawIn); final OutputStream out =
                        new BufferedOutputStream(rawOut)) {

                    // Convert input stream flow file into a byte array
                    byte[] input = toByteArray(in);

                    // Extract fields out of input
                    Map<String, String> extracted = extractFields(input);

                    // Write json bytes into output
                    out.write(objectMapper.writeValueAsBytes(extracted));
                }
            });
        } catch (ProcessException pe) {
            getLogger().error("Failed to deserialize {}", new Object[]{flowFile, pe});
            session.transfer(flowFile, REL_FAILURE);
            return;
        }

        flowFile = session.putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), "application/json");
        session.transfer(flowFile, REL_SUCCESS);
    }

    private Map<String, String> extractFields(final byte[] input) {
        // extract fields out of the byte array inout
        return new HashMap<>();
    }


    private byte[] toByteArray(final InputStream in) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;

        while ((len = in.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }

        return os.toByteArray();
    }
}
