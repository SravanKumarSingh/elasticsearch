/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.marvel.agent.collector.indices;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.marvel.agent.collector.AbstractCollector;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;

import java.util.Collection;

/**
 * Collector for indices statistics.
 *
 * This collector runs on the master node only and collect a {@link IndexMarvelDoc} document
 * for each existing index in the cluster.
 */
public class IndexCollector extends AbstractCollector<IndexCollector> {

    public static final String NAME = "index-collector";
    protected static final String TYPE = "marvel_index";

    private final ClusterName clusterName;
    private final Client client;

    @Inject
    public IndexCollector(Settings settings, ClusterService clusterService, ClusterName clusterName, Client client) {
        super(settings, NAME, clusterService);
        this.client = client;
        this.clusterName = clusterName;
    }

    @Override
    protected boolean masterOnly() {
        return true;
    }

    @Override
    protected Collection<MarvelDoc> doCollect() throws Exception {
        ImmutableList.Builder<MarvelDoc> results = ImmutableList.builder();

        IndicesStatsResponse indicesStats = client.admin().indices().prepareStats().all()
                .setStore(true)
                .setIndexing(true)
                .setDocs(true)
                .get();

        long timestamp = System.currentTimeMillis();
        for (IndexStats indexStats : indicesStats.getIndices().values()) {
            results.add(buildMarvelDoc(clusterName.value(), TYPE, timestamp, indexStats));
        }
        return results.build();
    }

    protected MarvelDoc buildMarvelDoc(String clusterName, String type, long timestamp, IndexStats indexStats) {
        return IndexMarvelDoc.createMarvelDoc(clusterName, type, timestamp,
                indexStats.getIndex(),
                indexStats.getTotal().getDocs().getCount(),
                indexStats.getTotal().getStore().sizeInBytes(), indexStats.getTotal().getStore().throttleTime().millis(),
                indexStats.getTotal().getIndexing().getTotal().getThrottleTimeInMillis());
    }
}
