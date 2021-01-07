/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.dataloaders.rdbms;

import com.opentext.infofabric.dataquery.DataqueryConstants;
import com.opentext.infofabric.dataquery.exception.DataqueryRuntimeException;
import com.opentext.infofabric.dataquery.graphql.query.Filter;
import com.opentext.infofabric.dataquery.graphql.query.FilterSet;
import com.opentext.infofabric.dataquery.graphql.results.ResultList;
import com.opentext.infofabric.dataquery.graphql.results.ResultObject;
import com.opentext.infofabric.dataquery.services.impl.sdl.SDLField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RDBMSLoader {

    private static final Logger logger = LoggerFactory.getLogger(RDBMSLoader.class);

    public static ResultList resultSetToList(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();
        ResultList rows = new ResultList();
        while (rs.next()) {
            ResultObject row = new ResultObject(columns);
            for (int i = 1; i <= columns; ++i) {
                if (rs.getObject(i) instanceof java.sql.Time) {
                    row.put(md.getColumnName(i), rs.getObject(i, LocalTime.class));
                } else {
                    row.put(md.getColumnName(i), rs.getObject(i));
                }
            }
            rows.add(row);
        }
        return rows;
    }

    void setFilterValuesOnPreparedStmt(PreparedStatement preparedStatement, List<Filter> filterArrayList) throws SQLException {
        if (!filterArrayList.isEmpty()) {
            int i = 0;
            for (Filter filter : filterArrayList) {
                if (filter.getValue() instanceof ArrayList) {
                    i = setPstmtArray(preparedStatement, filter, i);
                } else {
                    i++;
                    setPstmtByMappingGQLTypeToPgdb(preparedStatement, i, filter);
                }
            }
        }
    }

    void setPstmtByMappingGQLTypeToPgdb(PreparedStatement preparedStatement, int i, Filter filter) throws SQLException {
        switch (filter.getFieldType()) {
            case SDLField.SCALAR_STRING:
                preparedStatement.setString(i, filter.getValue().toString());
                break;
            case SDLField.SCALAR_LONG:
            case SDLField.SCALAR_INT:
            case "ID":
                preparedStatement.setLong(i, Long.parseLong(filter.getValue().toString()));
                break;
            case SDLField.SCALAR_FLOAT:
                preparedStatement.setDouble(i, Double.parseDouble(filter.getValue().toString()));
                break;
            case SDLField.SCALAR_BOOLEAN:
                preparedStatement.setBoolean(i, Boolean.parseBoolean(filter.getValue().toString()));
                break;
            case SDLField.SCALAR_SHORT:
                preparedStatement.setShort(i, Short.parseShort(filter.getValue().toString()));
                break;
            case SDLField.SCALAR_BYTE:
                preparedStatement.setByte(i, Byte.parseByte(filter.getValue().toString()));
                break;
            case DataqueryConstants.SCALAR_TIMESTAMP:
                String timestampStr = (filter.getValue().toString().endsWith("Z") ? filter.getValue().toString() : filter.getValue().toString() + "Z");
                try {
                    Instant instant = Instant.parse(timestampStr);
                    preparedStatement.setTimestamp(i, Timestamp.from(instant));
                } catch (DateTimeParseException ex) {
                    logger.error("Failed to parse timestamp: {} field type: {} for the field: {}", timestampStr, filter.getFieldType(), filter.getFieldName());
                    throw new DataqueryRuntimeException(String.format("Failed to parse timestamp: { %s } field type: { %s } for the field: { %s }", timestampStr, filter.getFieldType(), filter.getFieldName()), ex);
                }
                break;
            case DataqueryConstants.SCALAR_DATE:
                preparedStatement.setDate(i, Date.valueOf(filter.getValue().toString()));
                break;
            case DataqueryConstants.SCALAR_TIME:
                preparedStatement.setObject(i, LocalTime.parse(filter.getValue().toString()));
                break;
            default:
                logger.error("Unsupported field type: {} to set on the prepared statement for the field: {}", filter.getFieldType(), filter.getFieldName());
                throw new DataqueryRuntimeException(String.format("Unsupported field type { %s } to set on the prepared statement for the field { %s }", filter.getFieldType(), filter.getFieldName()));
        }
    }

    int setPstmtArray(PreparedStatement preparedStatement, Filter filter, int j) throws SQLException {
        switch (filter.getFieldType()) {
            case SDLField.SCALAR_STRING:
                ArrayList<String> stringFilterVals = (ArrayList) filter.getValue();
                for (String filterVal : stringFilterVals) {
                    j++;
                    preparedStatement.setString(j, filterVal);
                }
                return j;
            case SDLField.SCALAR_LONG:
            case SDLField.SCALAR_INT:
            case "ID":
                ArrayList<Long> longFilterVals = (ArrayList) filter.getValue();
                for (Long filterVal : longFilterVals) {
                    j++;
                    preparedStatement.setLong(j, filterVal.longValue());
                }
                return j;
            case SDLField.SCALAR_FLOAT:
                ArrayList<Double> doubleFilterVals = (ArrayList) filter.getValue();
                for (Double filterVal : doubleFilterVals) {
                    j++;
                    preparedStatement.setDouble(j, filterVal.doubleValue());
                }
                return j;
            default:
                logger.error("Unsupported field type: {} to set on the prepared statement for the field: {}", filter.getFieldType(), filter.getFieldName());
                throw new DataqueryRuntimeException(String.format("Unsupported field type { %s } to set on the prepared statement for the field { %s }", filter.getFieldType(), filter.getFieldName()));
        }
    }

    void populateFilterArrayList(List<Filter> filterArrayList, FilterSet filterSet) {
        if (filterSet != null) {
            if (filterSet.getFilters() != null)
                filterArrayList.addAll(filterSet.getFilters());
            else if (filterSet.getFiltersets() != null) {
                for (FilterSet innerFilterSet : filterSet.getFiltersets()) {
                    populateFilterArrayList(filterArrayList, innerFilterSet);
                }
            }
        }
    }

}
