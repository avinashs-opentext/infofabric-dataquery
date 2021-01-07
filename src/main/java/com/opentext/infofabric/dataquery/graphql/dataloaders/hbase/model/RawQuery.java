/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.graphql.dataloaders.hbase.model;

import java.util.ArrayList;
import java.util.List;

public class RawQuery {
	private String table;
	private List<String> rowKeys = new ArrayList<>();
	private int pageSize = 10;
	private int pageOffSet = 1;
	private List<Where> where = new ArrayList<>();
	private List<Select> select = new ArrayList<>();
	
	public String getTable() {
		return table;
	}
	public void setTable(String table) {
		this.table = table;
	}
	public List<String> getRowKeys() {
		return rowKeys;
	}
	public void setRowKeys(List<String> rowKeys) {
		this.rowKeys = rowKeys;
	}
	public List<Where> getWhere() {
		return where;
	}
	public void setWhere(List<Where> where) {
		this.where = where;
	}
	public List<Select> getSelect() {
		return select;
	}
	public void setSelect(List<Select> select) {
		this.select = select;
	}
	public int getPageSize() {
		return pageSize;
	}
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	public int getPageOffSet() {
		return pageOffSet;
	}
	public void setPageOffSet(int pageOffSet) {
		if(pageOffSet <= 0)
			this.pageOffSet = 1;
		this.pageOffSet = pageOffSet;
	}
		
}
