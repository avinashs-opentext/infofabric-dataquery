/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.dataloaders.hbase;

import com.google.inject.Inject;
import com.opentext.infofabric.dataquery.DataqueryConfiguration;
import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;
import com.opentext.infofabric.dataquery.graphql.dataloaders.hbase.model.RawQuery;
import com.opentext.infofabric.dataquery.graphql.dataloaders.hbase.model.Select;
import com.opentext.infofabric.dataquery.graphql.dataloaders.hbase.model.Where;
import com.opentext.infofabric.dataquery.graphql.query.FilterSet;
import com.opentext.infofabric.dataquery.graphql.query.Projection;
import com.opentext.infofabric.dataquery.graphql.query.Query;
import com.opentext.infofabric.dataquery.graphql.results.ResultObject;
import com.opentext.infofabric.dataquery.util.DataqueryUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.FilterList.Operator;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import static java.lang.Math.toIntExact;

public class HBaseQuery {

    private static final Logger logger = LoggerFactory.getLogger(HBaseQuery.class);
    private static final String VOLUME_BASE_PATH_PROPERTY_NAME = "volumeBasePath";

    private String tablePath;
    private String dataCenterEnv;
    private String tenant;
    private String model;

    @Inject
    private static DataqueryConfiguration config;

    @Inject
    private static HBaseConnection dbc;

    public HBaseQuery(String tenant,
                      String model) {
        this.tenant = tenant;
        this.model = model;
        tablePath = "/%s/%s/%s/views/%s/";

        dataCenterEnv = config.getEnvironment().getEnvironment().getCanonicalName().toLowerCase();

        if (config.getLoadBalancerConfig() != null) {
            HbaseConfig.getConfiguration().set("hbase.zookeeper.quorum", config.getLoadBalancerConfig().get("quorum").toString());
            HbaseConfig.getConfiguration().set("hbase.zookeeper.property.clientPort",
                    config.getLoadBalancerConfig().get("clientPort").toString());
        }
        try {
            dbc.configure(HbaseConfig.getConfiguration());
        } catch (IOException e) {
            logger.error("Exception in HBaseQuery()", e);
            throw new DataqueryRuntimeException("Exception in HBaseQuery()", e);
        }
        logger.info("Task started");
    }

    /**
     * Query the HBASE by list of row keys
     *
     * @return
     * @throws IOException
     */
    public Map<String, Object> getByRowKey(RawQuery rq) throws IOException {

        List<Get> gets = new ArrayList<>();
        if (!rq.getRowKeys().isEmpty()) {
            for (String rowKey : rq.getRowKeys()) {
                gets.add(new Get(Bytes.toBytes(rowKey)));
            }
        }

        Table table = null;
        try {
            table = dbc.getTable(TableName.valueOf(String.format(tablePath, config.getAdmin().get(VOLUME_BASE_PATH_PROPERTY_NAME), dataCenterEnv, tenant, model) + "/" + rq.getTable()));
            Result[] results = table.get(gets);

            if (results != null && results.length > 0) {
                Map<String, String> selectedColumns = getSelectColumn(rq.getSelect());
                for (Result result : results) {
                    Map<String, Object> row = new HashMap<>();
                    if (!result.isEmpty()) {
                        return getResult(result, selectedColumns);
                    } else {
                        return row;
                    }
                }
            }
        } finally {
            if (table != null) {
                table.close();
            }
        }
        return new HashMap<>();
    }

    /**
     * Query the HBASE by list of row keys
     *
     * @return
     * @throws IOException
     */
    public ResultObject gqlByRowKey(String rowKey, Map<String, String> selects, String tableName) throws IOException {

        List<Get> gets = new ArrayList<>();
        if (!StringUtils.isBlank(rowKey)) {
            gets.add(new Get(Bytes.toBytes(rowKey)));
        }

        Table table = null;

        try {
            table = dbc.getTable(TableName.valueOf(String.format(tablePath, config.getAdmin().get(VOLUME_BASE_PATH_PROPERTY_NAME), dataCenterEnv, tenant, model) + "/" + tableName));
            Result[] results = table.get(gets);

            if (results != null && results.length > 0) {
                for (Result result : results) {
                    ResultObject row = new ResultObject();
                    if (!result.isEmpty()) {
                        return getResult(result, selects);
                    } else {
                        return row;
                    }
                }
            }
        } finally {
            if (table != null) {
                table.close();
            }
        }
        return new ResultObject();
    }

    /**
     * Populate the individual Result
     *
     * @param result
     * @return
     */
    private ResultObject getResult(Result result, Map<String, String> selects) {
        NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> nMap = result.getMap();
        ResultObject colMap = new ResultObject();
        if (nMap != null) {
            for (Map.Entry<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> family : nMap.entrySet()) {
                populateColumnMap(colMap, family, selects);
            }
        }
        return colMap;
    }

    private void populateColumnMap(ResultObject colMap,
                                   Map.Entry<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> family, Map<String, String> selects) {
        NavigableMap<byte[], NavigableMap<Long, byte[]>> cMap = family.getValue();
        for (Map.Entry<byte[], NavigableMap<Long, byte[]>> column : cMap.entrySet()) {
            if (selects == null || selects.isEmpty() || selects.get(Bytes.toString(column.getKey()).toLowerCase()) != null) {
                NavigableMap<Long, byte[]> vMap = column.getValue();
                for (Map.Entry<Long, byte[]> l : vMap.entrySet()) {
                    colMap.put(Bytes.toString(column.getKey()), getValue(l.getValue(), (selects != null) ?
                            selects.get(Bytes.toString(column.getKey()).toLowerCase()) : null));
                }
            }
        }
    }

    private Object getValue(byte[] value, String type) {
        String str = Bytes.toStringBinary(value);
        if (StringUtils.isBlank(type)) {
            type = "";
        }
        switch (type.toLowerCase()) {
            case "int":
                if (StringUtils.isBlank(str)) {
                    return null;
                }
                return Integer.parseInt(Bytes.toHex(value), 16);
            case "timestamp":
                if (StringUtils.isBlank(str)) {
                    return null;
                }
                try {
                    return new Timestamp(DataqueryUtils.iso8601StringToJavaDate(str).getTime());
                } catch (ParseException e1) {
                    logger.error("unable to parse timestamp: {}", str);
                    throw new DataqueryRuntimeException(String.format("Unable to parse timestamp: %s", str));
                }
            default:
                if (str.indexOf("\\x") >= 0) {
                    try {
                        return Integer.parseInt(Bytes.toHex(value), 16);
                    } catch (Exception e) {
                        logger.info("Error Parsing hex String");
                    }
                }
                str = Bytes.toString(value);
                str = str.replaceAll("\\x0D", "\r");
                str = str.replaceAll("\\x0A", "\n");
                if (str.indexOf("\\u") >= 0) {
                    str = Bytes.toStringBinary(value);
                }

                return str;
        }
    }

    /**
     * HBase Scan api
     *
     * @param rq
     * @return
     * @throws IOException
     */
    public List<Map<String, Object>> scan(RawQuery rq) throws IOException {

        Table table = dbc.getTable(TableName.valueOf(String.format(tablePath, config.getAdmin().get(VOLUME_BASE_PATH_PROPERTY_NAME), dataCenterEnv, tenant, model) + "/" + rq.getTable()));

        /**
         * Instantiating the Scan class
         */
        Scan scan = new Scan();
        List<Filter> filterList = new ArrayList<>();

        /**
         * Set the filters
         */
        for (Where where : rq.getWhere()) {
            SingleColumnValueFilter singleColumnValueFilterA = null;
            int intValue = -1;
            try {
                intValue = Integer.parseInt(where.getValue());

                singleColumnValueFilterA = new SingleColumnValueFilter(
                        Bytes.toBytes(where.getFamily()), Bytes.toBytes(where.getColumn()), CompareOp.EQUAL,
                        Bytes.toBytes(intValue));
            } catch (Exception e) {
                logger.debug("Not a long value.");
                singleColumnValueFilterA = new SingleColumnValueFilter(
                        Bytes.toBytes(where.getFamily()), Bytes.toBytes(where.getColumn()), CompareOp.EQUAL,
                        Bytes.toBytes(where.getValue()));
            }

            singleColumnValueFilterA.setFilterIfMissing(true);
            filterList.add(singleColumnValueFilterA);
        }

        if (!filterList.isEmpty()) {
            FilterList filter = new FilterList(Operator.MUST_PASS_ALL, filterList);
            scan.setFilter(filter);
        }

        for (Select select : rq.getSelect()) {
            scan.addColumn(Bytes.toBytes(select.getFamily()), Bytes.toBytes(select.getColumn()));
        }

        /**
         * Getting the scan result
         */
        ResultScanner scanner = table.getScanner(scan);

        /**
         * Reading values from scan result
         */
        List<Map<String, Object>> rows = new ArrayList<>();
        Result result;

        for (int i = 0; i < (rq.getPageSize() * (rq.getPageOffSet() - 1)); i++) {
            result = scanner.next();
            if (result == null)
                break;
        }
        int count = 0;
        for (result = scanner.next(); result != null; result = scanner.next()) {
            rows.add(getResult(result, null));
            count++;
            if (count >= rq.getPageSize()) {
                break;
            }
        }

        /**
         * closing the table and scanner
         */
        table.close();
        scanner.close();
        return rows;
    }


    private void parseProjection(Query rq, Scan scan, Map<String, String> selects) {
        /**
         * Add the Projection
         */
        for (Projection pr : rq.getProjectionSet().getProjections()) {
            String family = "default";
            String column = pr.getField();
            if (pr.getField().indexOf(':') >= 0) {
                family = pr.getField().split(":")[0];
                column = pr.getField().split(":")[1];
            }
            selects.put(column.toLowerCase(), pr.getDataType());
            scan.addColumn(Bytes.toBytes(family), Bytes.toBytes(column));
        }
    }

    private SingleColumnValueFilter getColumnValueFilter(com.opentext.infofabric.dataquery.graphql.query.Filter f) {
        SingleColumnValueFilter singleColumnValueFilterA = null;
        String fieldName = f.getFieldName();
        String familyName = "default";
        String columnName = fieldName;
        String columnValue = String.valueOf(f.getValue());
        if (fieldName.indexOf(':') >= 0) {
            familyName = fieldName.split(":")[0];
            columnName = fieldName.split(":")[1];
        }

        /**
         * This will be updated, no need to guess after adding the filed types in filter
         */
        byte valueBytes[];
        try {
            valueBytes = Bytes.toBytes(Integer.parseInt(columnValue));
        } catch (NumberFormatException e) {
            logger.debug("Not a Int value.");
            valueBytes = Bytes.toBytes(columnValue);
        }
        singleColumnValueFilterA = new SingleColumnValueFilter(
                Bytes.toBytes(familyName), Bytes.toBytes(columnName), getFilterComparisonOperator(f.getComparisonOperator()),
                valueBytes);

        singleColumnValueFilterA.setFilterIfMissing(true);
        return singleColumnValueFilterA;
    }

    public List<ResultObject> gqlScan(Query rq) throws IOException {

        Table table = dbc.getTable(TableName.valueOf(String.format(tablePath, config.getAdmin().get(VOLUME_BASE_PATH_PROPERTY_NAME), dataCenterEnv, tenant, model) + "/" + rq.getCollection().getName()));

        /**
         * Instantiating the Scan class
         */
        Scan scan = new Scan();
        List<Filter> filterList = new ArrayList<>();
        List<ResultObject> returnResults = new ArrayList<>();
        Map<String, String> selects = new HashMap<>();

        parseProjection(rq, scan, selects);

        /**
         * Set the filters
         */
        for (com.opentext.infofabric.dataquery.graphql.query.Filter f : rq.getFilterSet().getFilters()) {
            if (f.isID()) {
                returnResults.add(gqlByRowKey(f.getValue().toString(), selects, rq.getCollection().getName()));
                return returnResults;
            } else {
                filterList.add(getColumnValueFilter(f));
            }
        }

        if (!filterList.isEmpty()) {
            FilterList filter = new FilterList(getFilterlogicalOperator(rq.getFilterSet().getLogicalOperator()), filterList);
            scan.setFilter(filter);
        }

        /**
         * Getting the scan result
         */
        ResultScanner scanner = table.getScanner(scan);

        /**
         * Reading values from scan result
         */
        Result result;

        for (int i = 0; i < (toIntExact(rq.getLimit()) * (toIntExact(rq.getSkip()) - 1)); i++) {
            result = scanner.next();
            if (result == null)
                break;
        }
        int count = 0;
        for (result = scanner.next(); result != null; result = scanner.next()) {
            returnResults.add(getResult(result, selects));
            count++;
            if (count >= toIntExact(rq.getLimit())) {
                break;
            }
        }

        /**
         * closing the table and scanner
         */
        table.close();
        scanner.close();
        return returnResults;
    }


    private CompareOp getFilterComparisonOperator(FilterSet.ComparisonOperator op) {
        switch (op.name()) {
            case "EQ":
                return CompareOp.EQUAL;
            case "NE":
                return CompareOp.NOT_EQUAL;
            case "LT":
                return CompareOp.LESS;
            case "LE":
                return CompareOp.LESS_OR_EQUAL;
            case "GT":
                return CompareOp.GREATER;
            case "GE":
                return CompareOp.GREATER_OR_EQUAL;
            default:
                return CompareOp.EQUAL;
        }
    }

    private Operator getFilterlogicalOperator(FilterSet.LogicalOperator op) {
        if (op == null) {
            return Operator.MUST_PASS_ALL;
        }
        switch (op.name()) {
            case "AND":
                return Operator.MUST_PASS_ALL;
            case "OR":
                return Operator.MUST_PASS_ONE;
            default:
                return Operator.MUST_PASS_ALL;
        }
    }

    private Map<String, String> getSelectColumn(List<Select> selects) {
        Map<String, String> list = new HashMap<>();
        for (Select s : selects) {
            list.put(s.getColumn().toLowerCase(), null);
        }
        return list;
    }

    public void cleanUp() {
        if (dbc != null) {
            try {
                dbc.close();
            } catch (IOException e) {
                logger.error("Exception while closing HBASE Connection", e);
            }
        }
    }
}
