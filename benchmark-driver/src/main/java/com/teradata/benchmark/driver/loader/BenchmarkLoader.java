/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.loader;

import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.BenchmarkExecutionException;
import com.teradata.benchmark.driver.BenchmarkProperties;
import com.teradata.benchmark.driver.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static java.nio.file.Files.isRegularFile;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FilenameUtils.removeExtension;

@Component
public class BenchmarkLoader {
    private static final String BENCHMARK_FILE_SUFFIX = "yaml";

    @Autowired
    private BenchmarkProperties properties;

    @Autowired
    private QueryLoader queryLoader;

    public List<Benchmark> loadBenchmarks() {
        try {
            return Files.walk(benchmarksFilesPath())
                    .filter(file -> isRegularFile(file) && file.toString().endsWith(BENCHMARK_FILE_SUFFIX))
                    .sorted((p1, p2) -> p1.toString().compareTo(p2.toString()))
                    .flatMap(file -> loadBenchmarks(file).stream())
                    .collect(toList());
        } catch (IOException e) {
            throw new BenchmarkExecutionException("could not load benchmarks", e);
        }
    }

    public List<Benchmark> loadBenchmarks(Path benchmarkFile) {
        try {
            String benchmarkName = generateDefaultBenchmarkName(benchmarkFile);
            BenchmarkDescriptor descriptor = BenchmarkDescriptor.loadFromFile(
                    benchmarkFile, benchmarkName
            );
            List<Map<String, String>> variableMapList = descriptor.getVariableMapList();
            if (variableMapList.isEmpty()) {
                variableMapList.add(newHashMap());
            }

            return variableMapList
                    .stream()
                    .map(variables -> createBenchmark(descriptor, variables))
                    .collect(toList());
        } catch (IOException e) {
            throw new BenchmarkExecutionException("could not load benchmark: " + benchmarkFile, e);
        }
    }

    private Benchmark createBenchmark(BenchmarkDescriptor descriptor, Map<String, String> variables) {
        List<Query> queries = loadQueries(descriptor.getQueryNames(), variables);
        return new Benchmark(descriptor.getName(), descriptor.getDataSource(), queries, descriptor.getRuns(), descriptor.getConcurrency());
    }

    private List<Query> loadQueries(List<String> queryNames, Map<String, String> variables) {
        return queryNames
                .stream()
                .map(queryName -> queryLoader.loadFromFile(sqlFilesPath().resolve(queryName), variables))
                .collect(toList());
    }

    private Path benchmarksFilesPath() {
        return asPath(properties.getBenchmarksDir());
    }

    private Path sqlFilesPath() {
        return asPath(properties.getSqlDir());
    }

    private Path asPath(String resourcePath) {
        URL resourceUrl = BenchmarkLoader.class.getClassLoader().getResource(resourcePath);
        if (resourceUrl != null) {
            try {
                return Paths.get(resourceUrl.toURI());
            } catch (URISyntaxException e) {
                throw new BenchmarkExecutionException("Cant resolve URL", e);
            }
        }
        return FileSystems.getDefault().getPath(resourcePath);
    }

    private String generateDefaultBenchmarkName(Path benchmarkFile) {
        String relativePath = benchmarksFilesPath().relativize(benchmarkFile).toString();
        String pathWithoutExtension = removeExtension(relativePath);
        return sanitizeBenchmarkName(pathWithoutExtension);
    }

    /**
     * Leaves in benchmark name only alphanumerics, underscores and dashes
     *
     * TODO: We should better do that where we passing benchmark name into REST URL
     */
    private String sanitizeBenchmarkName(String benchmarkName) {
        return benchmarkName.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
