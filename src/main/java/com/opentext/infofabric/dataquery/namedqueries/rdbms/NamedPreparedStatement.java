/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.namedqueries.rdbms;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;

public class NamedPreparedStatement {

    private final PreparedStatement preparedStatement;

    public PreparedStatement getPreparedStatement() {
        return preparedStatement;
    }

    private enum FormatType {

        NULL, BOOLEAN, BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BIGDECIMAL, STRING, STRINGLIST, DATE, TIME, TIMESTAMP
    }

    private String originalSQL;
    private final List<String> lstParameters;

    public NamedPreparedStatement(PreparedStatement preparedStatement, String originalSQL, List<String> orderedParameters) {
        this.originalSQL = originalSQL.trim();
        this.lstParameters = orderedParameters;
        this.preparedStatement = preparedStatement;
    }

    public static NamedPreparedStatement prepareStatement(Connection conn, String sql) throws SQLException {
        List<String> orderedParameters = new ArrayList<String>();
        int length = sql.length();
        StringBuffer parsedQuery = new StringBuffer(length);
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inSingleLineComment = false;
        boolean inMultiLineComment = false;

        for (int i = 0; i < length; i++) {
            char c = sql.charAt(i);
            if (inSingleQuote) {
                if (c == '\'') {
                    inSingleQuote = false;
                }
            } else if (inDoubleQuote) {
                if (c == '"') {
                    inDoubleQuote = false;
                }
            } else if (inMultiLineComment) {
                if (c == '*' && sql.charAt(i + 1) == '/') {
                    inMultiLineComment = false;
                }
            } else if (inSingleLineComment) {
                if (c == '\n') {
                    inSingleLineComment = false;
                }
            } else if (c == '\'') {
                inSingleQuote = true;
            } else if (c == '"') {
                inDoubleQuote = true;
            } else if (c == '/' && sql.charAt(i + 1) == '*') {
                inMultiLineComment = true;
            } else if (c == '-' && sql.charAt(i + 1) == '-') {
                inSingleLineComment = true;
            } else if (c == ':' && i + 1 < length && Character.isJavaIdentifierStart(sql.charAt(i + 1))) {
                int j = i + 2;
                while (j < length && Character.isJavaIdentifierPart(sql.charAt(j))) {
                    j++;
                }
                String name = sql.substring(i + 1, j);
                orderedParameters.add(name);
                c = '?';
                i += name.length();
            }
            parsedQuery.append(c);
        }

        return new NamedPreparedStatement(conn.prepareStatement(parsedQuery.toString()), sql, orderedParameters);
    }

//    private NamedPreparedStatement(PreparedStatement preparedStatement, String originalSQL, List<String> orderedParameters) {
//        super(preparedStatement);
//        this.originalSQL = originalSQL.trim();
//        this.lstParameters = orderedParameters;
//    }

    private Collection<Integer> getParameterIndexes(String parameter) {
        Collection<Integer> indexes = new ArrayList<Integer>();
        for (int i = 0; i < lstParameters.size(); i++) {
            if (lstParameters.get(i).equalsIgnoreCase(parameter)) {
                indexes.add(i + 1);
            }
        }
        if (indexes.isEmpty()) {
            throw new IllegalArgumentException(String.format("SQL statement doesn't contain the parameter '%s'",
                    parameter));
        }
        return indexes;
    }

    public void setNull(String parameter, int sqlType) throws SQLException {
        for (Integer i : getParameterIndexes(parameter)) {
            getPreparedStatement().setNull(i, sqlType);
            this.originalSQL = this.originalSQL.replaceFirst("(?i):" + parameter, Matcher.quoteReplacement(format((String) null, NamedPreparedStatement.FormatType.NULL)));
        }
    }

    public void setBoolean(String parameter, boolean x) throws SQLException {
        for (Integer i : getParameterIndexes(parameter)) {
            getPreparedStatement().setBoolean(i, x);
            this.originalSQL = this.originalSQL.replaceFirst("(?i):" + parameter, Matcher.quoteReplacement(format((Boolean) x, NamedPreparedStatement.FormatType.BOOLEAN)));
        }
    }

    public void setByte(String parameter, byte x) throws SQLException {
        for (Integer i : getParameterIndexes(parameter)) {
            getPreparedStatement().setByte(i, x);
            this.originalSQL = this.originalSQL.replaceFirst("(?i):" + parameter, Matcher.quoteReplacement(format((Byte) x, NamedPreparedStatement.FormatType.BYTE)));
        }
    }

    public void setShort(String parameter, short x) throws SQLException {
        for (Integer i : getParameterIndexes(parameter)) {
            getPreparedStatement().setShort(i, x);
            this.originalSQL = this.originalSQL.replaceFirst("(?i):" + parameter, Matcher.quoteReplacement(format((Short) x, NamedPreparedStatement.FormatType.SHORT)));
        }
    }

    public void setInt(String parameter, int x) throws SQLException {
        for (Integer i : getParameterIndexes(parameter)) {
            getPreparedStatement().setInt(i, x);
            this.originalSQL = this.originalSQL.replaceFirst("(?i):" + parameter, Matcher.quoteReplacement(format((Integer) x, NamedPreparedStatement.FormatType.INTEGER)));
        }
    }

    public void setLong(String parameter, long x) throws SQLException {
        for (Integer i : getParameterIndexes(parameter)) {
            getPreparedStatement().setLong(i, x);
            this.originalSQL = this.originalSQL.replaceFirst("(?i):" + parameter, Matcher.quoteReplacement(format((Long) x, NamedPreparedStatement.FormatType.LONG)));
        }
    }

    public void setFloat(String parameter, float x) throws SQLException {
        for (Integer i : getParameterIndexes(parameter)) {
            getPreparedStatement().setFloat(i, x);
            this.originalSQL = this.originalSQL.replaceFirst("(?i):" + parameter, Matcher.quoteReplacement(format((Float) x, NamedPreparedStatement.FormatType.FLOAT)));
        }
    }

    public void setDouble(String parameter, double x) throws SQLException {
        for (Integer i : getParameterIndexes(parameter)) {
            getPreparedStatement().setDouble(i, x);
            this.originalSQL = this.originalSQL.replaceFirst("(?i):" + parameter, Matcher.quoteReplacement(format((Double) x, NamedPreparedStatement.FormatType.DOUBLE)));
        }
    }

    public void setBigDecimal(String parameter, BigDecimal x) throws SQLException {
        for (Integer i : getParameterIndexes(parameter)) {
            getPreparedStatement().setBigDecimal(i, x);
            this.originalSQL = this.originalSQL.replaceFirst("(?i):" + parameter, Matcher.quoteReplacement(format((BigDecimal) x, NamedPreparedStatement.FormatType.BIGDECIMAL)));
        }
    }

    public void setString(String parameter, String x) throws SQLException {
        for (Integer i : getParameterIndexes(parameter)) {
            getPreparedStatement().setString(i, x);
            this.originalSQL = this.originalSQL.replaceFirst("(?i):" + parameter, Matcher.quoteReplacement(format((String) x, NamedPreparedStatement.FormatType.STRING)));
        }
    }

    public void setBytes(String parameter, byte[] x) throws SQLException {
        for (Integer i : getParameterIndexes(parameter)) {
            getPreparedStatement().setBytes(i, x);
            String fval = "";
            for (int j = 0; j < x.length; j++) {
                fval += (char) x[j] + ",";
            }
            if (fval.endsWith(",")) {
                fval = fval.substring(0, fval.length() - 1);
            }
            this.originalSQL = this.originalSQL.replaceFirst("(?i):" + parameter, Matcher.quoteReplacement(format((String) fval, NamedPreparedStatement.FormatType.STRING)));
        }
    }

    public void setDate(String parameter, Date x) throws SQLException {
        for (Integer i : getParameterIndexes(parameter)) {
            getPreparedStatement().setDate(i, x);
            this.originalSQL = this.originalSQL.replaceFirst("(?i):" + parameter, Matcher.quoteReplacement(format((Date) x, NamedPreparedStatement.FormatType.DATE)));
        }
    }

    public void setTime(String parameter, Time x) throws SQLException {
        for (Integer i : getParameterIndexes(parameter)) {
            getPreparedStatement().setTime(i, x);
            this.originalSQL = this.originalSQL.replaceFirst("(?i):" + parameter, Matcher.quoteReplacement(format((Time) x, NamedPreparedStatement.FormatType.TIME)));
        }
    }

    public void setTimestamp(String parameter, Timestamp x) throws SQLException {
        for (Integer i : getParameterIndexes(parameter)) {
            getPreparedStatement().setTimestamp(i, x);
            this.originalSQL = this.originalSQL.replaceFirst("(?i):" + parameter, Matcher.quoteReplacement(format((Timestamp) x, NamedPreparedStatement.FormatType.TIMESTAMP)));
        }
    }

    public String getQuery() {
        return this.originalSQL.trim();
    }

    private String format(Object o, NamedPreparedStatement.FormatType type) {
        String returnParam = "";
        try {
            switch (type) {
                case NULL:
                    returnParam = "NULL";
                    break;
                case BIGDECIMAL:
                    returnParam = ((o == null) ? "NULL" : "'" + ((BigDecimal) o).toString() + "'");
                    break;
                case BOOLEAN:
                    returnParam = ((o == null) ? "NULL" : "'" + (((Boolean) o == Boolean.TRUE) ? "1" : "0") + "'");
                    break;
                case BYTE:
                    returnParam = ((o == null) ? "NULL" : "'" + ((Byte) o).intValue() + "'");
                    break;
                case DATE:
                    returnParam = ((o == null) ? "NULL" : "'" + new SimpleDateFormat("yyyy-MM-dd").format((Date) o) + "'");
                    break;
                case DOUBLE:
                    returnParam = ((o == null) ? "NULL" : "'" + ((Double) o).toString() + "'");
                    break;
                case FLOAT:
                    returnParam = ((o == null) ? "NULL" : "'" + ((Float) o).toString() + "'");
                    break;
                case INTEGER:
                    returnParam = ((o == null) ? "NULL" : "'" + ((Integer) o).toString() + "'");
                    break;
                case LONG:
                    returnParam = ((o == null) ? "NULL" : "'" + ((Long) o).toString() + "'");
                    break;
                case SHORT:
                    returnParam = ((o == null) ? "NULL" : "'" + ((Short) o).toString() + "'");
                    break;
                case STRING:
                    returnParam = ((o == null) ? "NULL" : "'" + o.toString() + "'");
                    break;
                case STRINGLIST:
                    returnParam = ((o == null) ? "NULL" : "'" + o.toString() + "'");
                    break;
                case TIME:
                    returnParam = ((o == null) ? "NULL" : "'" + new SimpleDateFormat("hh:mm:ss a").format(o) + "'");
                    break;
                case TIMESTAMP:
                    returnParam = ((o == null) ? "NULL" : "'" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(o) + "'");
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return returnParam.trim();
    }

    public List<String> getlstParameters() {
        return lstParameters;
    }

}
