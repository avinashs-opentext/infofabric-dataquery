/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.services;


import com.opentext.infofabric.dataquery.dto.NamedQueryRequest;
import com.opentext.infofabric.dataquery.dto.NamedQueryResponse;
import com.opentext.infofabric.dataquery.dto.SqlResource;

import javax.ws.rs.container.AsyncResponse;
import java.util.Set;

public interface NamedQueryExecutorService {

    Set<String> getNames();

    void executeQuery(String tenant, String datamodel, NamedQueryRequest request, AsyncResponse response);

    String previewQuery(String tenant, String datamodel, NamedQueryRequest request);

    String getQuery(String queryName);

    NamedQueryResponse getJobIdStatus(String jobId);

    void setJobIdStatus(NamedQueryResponse response);

    void updateMap(SqlResource resource);

}
