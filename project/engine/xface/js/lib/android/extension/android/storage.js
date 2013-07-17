
/*
 This file was modified from or inspired by Apache Cordova.

 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied. See the License for the
 specific language governing permissions and limitations
 under the License.
*/

var argscheck = require('xFace/argscheck'),
    utils = require('xFace/utils'),
    exec = require('xFace/exec'),
    channel = require('xFace/channel');

var queryQueue = {};

/**
 * SQL结果集合对象
 */
var DroidDB_Rows = function() {
    this.resultSet = [];    // 结果u数组
    this.length = 0;        // 行数
};

/**
 * 从SQL结果集合对象中获取item
 *
 * @param row           要返回的行号
 * @return              row对象
 */
DroidDB_Rows.prototype.item = function(row) {
    argscheck.checkArgs('n', 'storage.item', arguments);
    return this.resultSet[row];
};

/**
 * 返回给用户的SQL结果集合对象.
 */
var DroidDB_Result = function() {
    this.rows = new DroidDB_Rows();
};

/**
 * 当查询完成时，来自native代码的回调.
 * @param id   查询 id
 */
function completeQuery(id, data) {
    var query = queryQueue[id];
    if (query) {
        try {
            delete queryQueue[id];

            // 获取事务
            var tx = query.tx;

            if (tx && tx.queryList[id]) {

                // 保存查询结果
                var r = new DroidDB_Result();
                r.rows.resultSet = data;
                r.rows.length = data.length;
                try {
                    if (typeof query.successCallback === 'function') {
                        query.successCallback(query.tx, r);
                    }
                } catch (ex) {
                    console.log("executeSql error calling user success callback: "+ex);
                }

                tx.queryComplete(id);
            }
        } catch (e) {
            console.log("executeSql error: "+e);
        }
    }
}

/**
 * 当查询失败时，来自native代码的回调.
 *
 * @param reason            错误消息
 * @param id                查询 id
 */
function failQuery(reason, id) {
    var query = queryQueue[id];
    if (query) {
        try {
            delete queryQueue[id];

            var tx = query.tx;

            if (tx && tx.queryList[id]) {
                tx.queryList = {};

                try {
                    if (typeof query.errorCallback === 'function') {
                        query.errorCallback(query.tx, reason);
                    }
                } catch (ex) {
                    console.log("executeSql error calling user error callback: "+ex);
                }

                tx.queryFailed(id, reason);
            }

        } catch (e) {
            console.log("executeSql error: "+e);
        }
    }
}

/**
 * SQL查询对象
 *
 * @param tx                查询属于的事务对象
 */
var DroidDB_Query = function(tx) {

    // 设置查询的id
    this.id = utils.createUUID();

    // 添加该查询到队列中
    queryQueue[this.id] = this;

    // 初始化结果集
    this.resultSet = [];

    // 设置查询属于的事务对象
    this.tx = tx;

    // 添加查询到事务列表中
    this.tx.queryList[this.id] = this;

    // Callbacks
    this.successCallback = null;
    this.errorCallback = null;

};

/**
 * 事务对象
 */
var DroidDB_Tx = function() {

    // 设置事务的id
    this.id = utils.createUUID();

    // Callbacks
    this.successCallback = null;
    this.errorCallback = null;

    // 查询列表
    this.queryList = {};
};

/**
 * 标记在事务中的查询成功.
 * 如果所有查询都完成，调用用户的事务成功回调函数
 *
 * @param id                查询 id
 */
DroidDB_Tx.prototype.queryComplete = function(id) {
    delete this.queryList[id];

    if (this.successCallback) {
        var count = 0;
        var i;
        for (i in this.queryList) {
            if (this.queryList.hasOwnProperty(i)) {
                count++;
            }
        }
        if (count === 0) {
            try {
                this.successCallback();
            } catch(e) {
                console.log("Transaction error calling user success callback: " + e);
            }
        }
    }
};

/**
 * 标记在事务中的查询失败.
 *
 * @param id                查询 id
 * @param reason            错误消息
 */
DroidDB_Tx.prototype.queryFailed = function(id, reason) {
    this.queryList = {};

    if (this.errorCallback) {
        try {
            this.errorCallback(reason);
        } catch(e) {
            console.log("Transaction error calling user error callback: " + e);
        }
    }
};

/**
 * 执行SQL语句
 *
 * @param sql                   待执行的SQL语句
 * @param params                SQL语句的参数
 * @param successCallback       成功回调函数
 * @param errorCallback         失败回调函数
 */
DroidDB_Tx.prototype.executeSql = function(sql, params, successCallback, errorCallback) {
    argscheck.checkArgs('sAFF', 'storage.executeSql', arguments);
    if (typeof params === 'undefined') {
        params = [];
    }

    var query = new DroidDB_Query(this);
    queryQueue[query.id] = query;

    query.successCallback = successCallback;
    query.errorCallback = errorCallback;

    exec(null, null, null, "Storage", "executeSql", [sql, params, query.id]);
};

var DatabaseShell = function() {
};

/**
 * 开始一个事务.
 * 在失败的事件中不支持回滚.
 *
 * @param process {Function}            事务函数
 * @param successCallback {Function}
 * @param errorCallback {Function}
 */
DatabaseShell.prototype.transaction = function(process, errorCallback, successCallback) {
    argscheck.checkArgs('fFF', 'storage.transaction', arguments);
    var tx = new DroidDB_Tx();
    tx.successCallback = successCallback;
    tx.errorCallback = errorCallback;
    try {
        process(tx);
    } catch (e) {
        console.log("Transaction error: "+e);
        if (tx.errorCallback) {
            try {
                tx.errorCallback(e);
            } catch (ex) {
                console.log("Transaction error calling user error callback: "+e);
            }
        }
    }
};

/**
 * 打开数据库
 *
 * @param name              数据库名
 * @param version           数据库版本
 * @param display_name      数据库显示的名字
 * @param size              数据库大小（单位:byte）
 * @return                  数据库对象
 */
var DroidDB_openDatabase = function(name, version, display_name, size) {
    argscheck.checkArgs('sssn', 'storage.openDatabase', arguments);
    var db = null;
    var openDatabaseSuccess = function(){
    db = new DatabaseShell();
    };
    var openDatabaseFail = function(){
    db = null;
    };
    exec(openDatabaseSuccess, openDatabaseFail, null, "Storage", "openDatabase", [name, version, display_name, size]);
    return db;
};

module.exports = {
  openDatabase:DroidDB_openDatabase,
  failQuery:failQuery,
  completeQuery:completeQuery,
  DroidDB_Tx:DroidDB_Tx,
  DatabaseShell:DatabaseShell,
  DroidDB_Query:DroidDB_Query,
  queryQueue:queryQueue
};