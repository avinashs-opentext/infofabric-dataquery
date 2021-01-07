/**
 * Copyright 2018 opentext.infofabric Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.dataloaders.hbase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.opentext.infofabric.dataquery.graphql.results.ResultObject;
import com.opentext.infofabric.dataquery.graphql.dataloaders.DataQueryBatchLoader;
import io.prometheus.client.Histogram;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;
import com.opentext.infofabric.dataquery.graphql.dataloaders.DataQueryBatchLoader;
import com.opentext.infofabric.dataquery.graphql.query.Query;
import com.opentext.infofabric.dataquery.graphql.results.ResultList;

import static com.opentext.infofabric.dataquery.DataqueryConstants.PROMETHEUS_METRICS_ROOT;

public class HbaseBatchLoader implements DataQueryBatchLoader {

	private static final Logger logger = LoggerFactory.getLogger(HbaseBatchLoader.class);
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	static final Histogram executeQueryMethodLatency = Histogram.build()
			.name(PROMETHEUS_METRICS_ROOT + "hbasebatchloader_execute_query_latency_seconds")
			.help("HbaseBatchLoader executeQuery method latency in seconds.")
			.register();

	@Override
	public CompletionStage<List<ResultList>> load(List<Query> queries) {
		return CompletableFuture.supplyAsync(() -> {
			Histogram.Timer timer = executeQueryMethodLatency.startTimer();
			try {
				return executeQuery(queries);
			} catch (Exception e) {
				logger.error("Error parsing hbase query", e);
			} finally {
				timer.observeDuration();
			}
			return null;
		});
	}

	@Override
	public void validate(List<Query> queries) {
		/**
		 * Need to look into what sort of validation we need to do here
		 */
	}

	@Override
	public Object getNativeQuery(List<Query> queries) {
		return null;
	}
	
	private List<ResultList> executeQuery(List<Query> originalQueries) {
		List<ResultList> resultsPerQuery = new ArrayList<>();

		/**
		 * This needs to implement for more then one query
		 */
		if(originalQueries.size() > 1) {
			throw new DataqueryRuntimeException("Only One query Supported right now.");
		}
		
		Query query = originalQueries.get(0);
		if(!CollectionUtils.isEmpty(query.getFilterSet().getFiltersets())) {
			throw new DataqueryRuntimeException("Compound / Nested filterset is not yet supported for HBASE queries.");
		}
		
		
		/**
		 * HBase Query Call
		 */
		HBaseQuery hQuery = null;
		try {
			hQuery = new HBaseQuery(query.getCollection().getTenant(), query.getCollection().getDatamodel());
			ResultList rs = new ResultList();
			for(ResultObject map : hQuery.gqlScan(query)) {
				rs.add(map);
			}
			resultsPerQuery.add(rs);
		} catch (Exception e) {
			throw new DataqueryRuntimeException("Exception during hbase query", e);
		}
		finally {
			if (hQuery != null) {
				hQuery.cleanUp();
			}
		}
		if(logger.isDebugEnabled()) {
			logger.debug("Result:\n{}", gson.toJson(resultsPerQuery));
		}
		
		return resultsPerQuery;
	}
}
