/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.liaison.dataquery.util;

import com.liaison.dataquery.DataqueryConstants;
import com.opentext.infofabric.registrar.types.MediaType;
import com.opentext.infofabric.registrar.types.Payload;
import com.opentext.infofabric.registrar.types.PayloadCommand;
import com.opentext.infofabric.registrar.types.ProcessorTask;
import com.opentext.infofabric.registrar.types.Status;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

public class ResultSetStreamUtil {

    private static final Logger log = LoggerFactory.getLogger(ResultSetStreamUtil.class);

    public static int stream(ResultSet rs, String jobID, String streamName, String typeName, String tenant) throws Exception {
        int rowCount = 0;
//        ProcessorTask task = Registrar.getTaskForNewProcess(jobID, streamName);
        ProcessorTask task = Registrar.getTaskForNewProcess(tenant, streamName);
        task.beginProcessing();
        JSONObject json = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        List<String> columnList = new ArrayList<String>();
        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();
        for (int i = 1; i < numColumns + 1; i++) {
            columnList.add(rsmd.getColumnName(i));
        }

        while (rs.next()) {
            JSONObject obj = new JSONObject();
            for (int i = 1; i < numColumns + 1; i++) {
                String column_name = columnList.get(i - 1);

                /*if(rsmd.getColumnType(i)==java.sql.Types.ARRAY){
                    obj.put(column_name, rs.getArray(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.BIGINT){
                    obj.put(column_name, rs.getInt(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.BOOLEAN){
                    obj.put(column_name, rs.getBoolean(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.BLOB){
                    obj.put(column_name, rs.getBlob(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.DOUBLE){
                    obj.put(column_name, rs.getDouble(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.FLOAT){
                    obj.put(column_name, rs.getFloat(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.INTEGER){
                    obj.put(column_name, rs.getInt(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.NVARCHAR){
                    obj.put(column_name, rs.getNString(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.VARCHAR){
                    obj.put(column_name, rs.getString(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.TINYINT){
                    obj.put(column_name, rs.getInt(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.SMALLINT){
                    obj.put(column_name, rs.getInt(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.DATE){
                    obj.put(column_name, rs.getDate(column_name));
                }
                else if(rsmd.getColumnType(i)==java.sql.Types.TIMESTAMP){
                    obj.put(column_name, rs.getTimestamp(column_name));
                }
                else{ */

                obj.put(column_name, rs.getObject(column_name));
                //  }
            }
            rowCount++;

            if(null!= typeName) {
                JSONObject type = new JSONObject();
                type.put(typeName, obj);
                jsonArray.put(type);
                json.put(typeName,jsonArray);
            } else {
                jsonArray.put(obj);
            }

            if (rowCount % DataqueryConstants.PAYLOAD_RESULTSET_ROWS == 0) {
                if(null != typeName){
                    log.info("RESPONSE ");
                    log.info(json.toString());
                    sendToStream(json.toString().getBytes(), jobID, task, streamName);
                    json = new JSONObject();
                } else {
                    log.info("RESPONSE ");
                    log.info(jsonArray.toString());
                    sendToStream(jsonArray.toString().getBytes(), jobID, task, streamName);
                    jsonArray = new JSONArray();
                }

            }
        }
        if (rowCount % DataqueryConstants.PAYLOAD_RESULTSET_ROWS != 0) {
            if(null != typeName){
                log.info("RESPONSE ");
                log.info(json.toString());
                sendToStream(json.toString().getBytes(), jobID, task, streamName);
                json = null;
            } else {
                log.info("RESPONSE ");
                log.info(jsonArray.toString());
                sendToStream(jsonArray.toString().getBytes(), jobID, task, streamName);
                jsonArray = null;
            }

        }
        return rowCount;
    }


    private static void sendToStream(final byte[] mappedData, final String key, ProcessorTask processorTask, String streamName) {
        try {
            Payload payload = new Payload(mappedData, key, MediaType.JSON, PayloadCommand.ASSERT);
            DatastreamProducer.sendAsync(processorTask, streamName, payload);
            endProcessing(processorTask, Status.SUCCESS, "Payload is successfully processed!");
        } catch (Exception e) {
            log.error("exceptions from Datastream Client", e);
            endProcessing(processorTask, Status.ERROR, "Failed to write the mapped payload to Datastream");
            throw e;
        }
    }

    private static void endProcessing(final ProcessorTask processorTask, final Status status, final String msg) {
        processorTask.addEvent(status, msg);
        processorTask.endProcessing(status);
    }


    public static JSONArray getSyncResponse(ResultSet rs) throws Exception {
        JSONArray jsonArray = new JSONArray();

        List<String> columnList = new ArrayList<String>();
        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();
        for (int i = 1; i < numColumns + 1; i++) {
            columnList.add(rsmd.getColumnName(i));
        }

        while (rs.next()) {
            JSONObject obj = new JSONObject();
            for (int i = 1; i < numColumns + 1; i++) {
                String column_name = columnList.get(i - 1);
                obj.put(column_name, rs.getObject(column_name));
            }
            jsonArray.put(obj);
        }

        return jsonArray;
    }
}