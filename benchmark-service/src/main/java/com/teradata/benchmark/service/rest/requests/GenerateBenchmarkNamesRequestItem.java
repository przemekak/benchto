/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service.rest.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.util.Map;

public class GenerateBenchmarkNamesRequestItem
{

    @NotNull
    @Size(min = 1, max = 255)
    private final String benchmarkName;
    private final Map<String, String> variables;

    @JsonCreator
    public GenerateBenchmarkNamesRequestItem(@JsonProperty("benchmarkName") String benchmarkName, @JsonProperty("variables") Map<String, String> variables)
    {
        this.benchmarkName = benchmarkName;
        this.variables = variables;
    }

    public String getBenchmarkName()
    {
        return benchmarkName;
    }

    public Map<String, String> getVariables()
    {
        return variables;
    }
}