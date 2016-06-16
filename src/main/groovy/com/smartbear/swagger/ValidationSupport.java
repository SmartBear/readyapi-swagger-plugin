package com.smartbear.swagger;

import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.google.common.collect.Lists;

import java.util.List;

public class ValidationSupport {
    static void validateMessage(JsonSchema jsonSchema, JsonNode contentObject) throws AssertionException {
        ProcessingReport report = null;
        try {
            report = jsonSchema.validate(contentObject);
        } catch (ProcessingException e) {
            throw new AssertionException( new AssertionError( e.getProcessingMessage().getMessage() ));
        }

        if (!report.isSuccess()) {
            List<AssertionError> errors = Lists.newArrayList();
            for (ProcessingMessage message : report) {
                if (message.getLogLevel() == LogLevel.ERROR || message.getLogLevel() == LogLevel.FATAL) {
                    errors.add(new AssertionError(message.getMessage()));
                }
            }

            if (!errors.isEmpty()) {
                throw new AssertionException(errors.toArray(new AssertionError[errors.size()]));
            }
        }
    }
}
