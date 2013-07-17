
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

package com.polyvi.xface.extension.contact;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.polyvi.xface.extension.XActivityResultListener;
import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtension;
import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.extension.XExtensionResult.Status;
import com.polyvi.xface.util.XConstant;
import com.polyvi.xface.util.XLog;
import com.polyvi.xface.util.XNotification;
import com.polyvi.xface.util.XStringUtils;

public class XContactsExt extends XExtension implements XActivityResultListener{

    private static final String CLASS_NAME = XContactsExt.class.getSimpleName();

    /** XContactsExt 提供给js用户的接口名字 */
    private static final String COMMAND_SEARCH = "search";
    private static final String COMMAND_SAVE = "save";
    private static final String COMMAND_REMOVE = "remove";
    private static final String COMMAND_CHOOSE = "chooseContact";
    private static final int    PICK_CONTACT = 1;
    private static final String TAG_FIELDS = "fields";
    private static final String CALL_CHOOSE_ERROR ="error occurred while call chooseContact";

    // FIXME: 未使用的常量以后可能会用到
    // 错误码
    private static final int UNKNOWN_ERROR = 0;
    private static final int INVALID_ARGUMENT_ERROR = 1;
    private static final int TIMEOUT_ERROR = 2;
    private static final int PENDING_OPERATION_ERROR = 3;
    private static final int IO_ERROR = 4;
    private static final int NOT_SUPPORTED_ERROR = 5;
    private static final int PERMISSION_DENIED_ERROR = 20;

    // 确保照片大小在 1 MB 以下
    private static final long MAX_PHOTO_SIZE = 1048576;

    private XContactAccessor mContactAccessor;
    private XCallbackContext mCallbackCtx;
    private JSONArray mFields;

    @Override
    public void sendAsyncResult(String result) {
    }

    @Override
    public boolean isAsync(String action) {
        return true;
    }

    @Override
    protected Object getNativeWorker() {
        return new XContactAccessorAPILevel5Impl(getContext());
    }

    /**
     * 执行请求并返回XExtensionResult.
     *
     * @param action
     *            需要执行的action.
     * @param args
     *            此 Extension 的参数的 JSONArry.
     * @param callbackCtx
     *            native端js回调上下文环境
     * @return 带有 status 和 message 的 XExtensionResult 对象
     */
    @Override
    public XExtensionResult exec(String action,
            JSONArray args, XCallbackContext callbackCtx) throws JSONException {
        XExtensionResult.Status status = XExtensionResult.Status.OK;

        // 检查设备的 Android 版本号，目前仅考虑支持 2.x(API > 4)的 Android 设备
        if (android.os.Build.VERSION.RELEASE.startsWith("1.")) {
            return new XExtensionResult(XExtensionResult.Status.ERROR,
                    XContactsExt.NOT_SUPPORTED_ERROR);
        }

        // 仅在检查完 Android version 后才创建 mContactAccessor，否则会在老版本上的手机上崩溃
        if (this.mContactAccessor == null) {
            this.mContactAccessor = (XContactAccessor) getNativeWorker();
        }

        if (action.equals(COMMAND_SEARCH)) {
            if (args.length() < 2) {
                return new XExtensionResult(Status.ERROR,
                        XContactsExt.INVALID_ARGUMENT_ERROR);
            }
            JSONArray ret = search(args.getJSONArray(0), args.optJSONObject(1));
            return new XExtensionResult(status, ret);
        } else if (action.equals(COMMAND_SAVE)) {
            String id = saveContact(args.getJSONObject(0));
            if (id != null) {
                JSONObject ret = getContactById(id);
                if (ret != null) {
                    return new XExtensionResult(status, ret);
                }
            }
        } else if (action.equals(COMMAND_REMOVE)) {
            if (mContactAccessor.removeContact(args.getString(0))) {
                return new XExtensionResult(status, "");
            }
        } else if(action.equals(COMMAND_CHOOSE)) {
            mCallbackCtx = callbackCtx;
            mFields = getFields(args);
            startContactActivity();
            XExtensionResult r = new XExtensionResult(XExtensionResult.Status.NO_RESULT);
            return r;
        }
        return new XExtensionResult(Status.ERROR, XContactsExt.UNKNOWN_ERROR);
    }

    /**
     * 根据JS传入的需要查询的域和选项进行查询.
     *
     * @param fields
     *            需要查询的域，即用于匹配的属性域，返回的联系人也仅包含fields中指定的属性域
     * @param options
     *            指定查询的匹配条件及一些选项
     * @return
     */
    private JSONArray search(JSONArray fields, JSONObject options) {
        // 获得查询选项
        String searchTerm = "";
        int contactsMaxNum = Integer.MAX_VALUE; // 返回联系人数量的最大值
        boolean isMultipleContacts = true; // 当依据查询条件存在多个匹配值时，是否返回多个值

        if (options != null) {
            searchTerm = options
                    .optString(XContactAccessor.FIND_OPTION_NAME_FILTER);
            if (XStringUtils.isEmptyString(searchTerm)) {
                searchTerm = "%";
            } else if (!"%".equals(searchTerm)) {
                searchTerm = "%" + searchTerm + "%";
            }

            try {
                isMultipleContacts = options
                        .getBoolean(XContactAccessor.FIND_OPTION_NAME_MULTIPLE);
                if (!isMultipleContacts) {
                    contactsMaxNum = 1;
                }
            } catch (JSONException e) {
                // 存在多个匹配值很正常，所以默认为 true，不做处理
            }
        } else {
            searchTerm = "%";
        }

        // 检查 fields 是否为 "*"
        boolean willMatchAllField = false;
        if (fields.length() == 1) {
            try {
                if ("*".equals(fields.getString(0))) {
                    willMatchAllField = true;
                }
            } catch (JSONException e) {
                // isWildCardSearch 默认为false，不做改变
            }
        }

        String accountType = options
                .optString(XContactAccessor.FIND_OPTION_NAME_ACCOUNT_TYPE);

        HashSet<String> logicContactFields = getContactFieldSet(fields);

        Cursor c = mContactAccessor.getMatchedContactsCursor(
                logicContactFields, searchTerm, willMatchAllField, accountType);
        // 形成最终查询结果集
        JSONArray contacts = populateContactArray(logicContactFields, c,
                contactsMaxNum);
        c.close();
        return contacts;
    }

    /**
     * 将JSONArray表示的联系人属性集转换为HashSet表示的集合
     *
     * @param fields
     *            js传下来的联系人属性集合
     */
    protected HashSet<String> getContactFieldSet(JSONArray fields) {
        HashSet<String> requiredFieldsSet = new HashSet<String>();

        try {
            // fields为 ["*"]时，查找所有的属性域
            if (fields.length() == 1 && fields.getString(0).equals("*")) {
                // 此处需要放入具体的field name，而不能在此进行映射后存值
                requiredFieldsSet.addAll(XContactAccessor.LOGIC_CONTACT_FIELDS);
            } else {
                int len = fields.length();
                for (int i = 0; i < len; i++) {
                    // 此处对 JS 传入的域名不作容错处理，交由 Impl 去判断
                    requiredFieldsSet.add(fields.getString(i));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            XLog.e(CLASS_NAME, e.getMessage(), e);
        }
        return requiredFieldsSet;
    }

    /**
     * 根据传入的游标创建符合条件的联系人json数组.
     *
     * @param requiredField
     *            需要读取的联系人属性集合
     * @param c
     *            游标
     * @param contactsMaxNum
     *            联系人数组的最大人数
     * @return 联系人的 JSON 数组
     */

    private JSONArray populateContactArray(HashSet<String> requiredField,
            Cursor c, int contactsMaxNum) {
        String oldContactId = "";
        boolean newContact = true;

        JSONArray contacts = new JSONArray();
        JSONObject contact = new JSONObject();

        HashMap<String, Object> contactMap = new HashMap<String, Object>();

        // 对一个属性可能存在多份值的数量置零
        mContactAccessor.initQuantityOfFields(contactMap);

        if (c.getCount() > 0) {
            while (c.moveToNext()
                    && (contacts.length() <= (contactsMaxNum - 1))) {
                String contactId = mContactAccessor.getContactId(c);
                String rawId = mContactAccessor.getRawId(c);
                try {
                    // 针对首次查询设置 oldContactId
                    if (c.getPosition() == 0) {
                        oldContactId = contactId;
                    }

                    // 当contact ID变化时
                    // 需要把原有的Contact object放入 contacts数组
                    // 并创建新的 Contact object
                    if (!oldContactId.equals(contactId)) {
                        hashMapToJson(requiredField, contactMap, contact);
                        contacts.put(contact);

                        // 清空对象
                        contact = new JSONObject();
                        contactMap.clear();

                        // 对一个属性可能存在多份值的数量置零
                        mContactAccessor.initQuantityOfFields(contactMap);
                        newContact = true;
                    }

                    // 当检测到有新联系人 时，设置 ID 和 display name
                    // 因为这两个域在返回的集合中的每一行均需要
                    if (newContact) {
                        newContact = false;
                        contact.put(XContactAccessor.LOGIC_FIELD_ID, contactId);
                        contact.put(XContactAccessor.LOGIC_FIELD_RAWID, rawId);
                    }

                    mContactAccessor.cursorToHashMap(contactMap, c);
                } catch (JSONException e) {
                    XLog.e(CLASS_NAME, e.getMessage(), e);
                }

                oldContactId = contactId;
            }

            // Push最后一个联系人到 contacts数组中
            if (contacts.length() < contactsMaxNum) {
                hashMapToJson(requiredField, contactMap, contact);
                contacts.put(contact);
            }
        }
        return contacts;
    }

    /**
     * 将hash表中的联系人数据保存到json对象中.
     *
     * @param requiredFields
     *            需要获取的联系人属性集合
     * @param contactMap
     *            用于存储单个联系人所有属性的 Map
     * @param contactJson
     *            单个联系人的 JSON 数据
     */
    private void hashMapToJson(HashSet<String> requiredFields,
            HashMap<String, Object> contactMap, JSONObject contactJson) {
        try {
            // DisplayName、Nickname、Note、Birthday
            for (String logicField : XContactAccessor.LOGIC_CONTACT_SINGLE_VALUE_FIELDS) {
                if (requiredFields.contains(logicField)) {
                    Object fieldValue = contactMap.get(logicField);
                    if (fieldValue != null) {
                        contactJson.put(logicField, fieldValue);
                    }
                }
            }

            // Addresses、Organization、Phone、Email、IMs、URLs、Photos
            for (String logicParentField : XContactAccessor.LOGIC_CONTACT_MULTIPLE_VALUE_FIELDS) {
                if (requiredFields.contains(logicParentField)) {
                    String quantityField = mContactAccessor
                            .generateQuantityFieldOf(logicParentField);
                    int quantityValue = (Integer) contactMap.get(quantityField);
                    if (quantityValue > 0) {
                        JSONArray jsonArray = new JSONArray();
                        for (int i = 0; i < quantityValue; i++) {
                            JSONObject childJson = new JSONObject();
                            List<String> subFields = mContactAccessor
                                    .getLogicContactSubFieldsOf(logicParentField);

                            for (String subField : subFields) {
                                String key = mContactAccessor
                                        .generateSubFieldKey(logicParentField,
                                                i, subField);
                                childJson.put(subField, contactMap.get(key));
                            }

                            jsonArray.put(childJson);
                        }
                        contactJson.put(logicParentField, jsonArray);
                    }
                }
            }

            String logicParentField = XContactAccessor.LOGIC_FIELD_NAME;
            // Name
            if (requiredFields.contains(logicParentField)) {
                JSONObject fieldJSON = new JSONObject();

                List<String> subFields = mContactAccessor
                        .getLogicContactSubFieldsOf(logicParentField);

                for (String subField : subFields) {
                    String key = mContactAccessor.generateSubFieldKey(
                            logicParentField, 0, subField);
                    fieldJSON.put(subField, contactMap.get(key));
                }

                String key = mContactAccessor.generateSubFieldKey(
                        logicParentField, 0,
                        XContactAccessor.LOGIC_FIELD_NAME_FORMATTED);
                fieldJSON.put(XContactAccessor.LOGIC_FIELD_NAME_FORMATTED,
                        contactMap.get(key));

                contactJson.put(XContactAccessor.LOGIC_FIELD_NAME, fieldJSON);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            XLog.e(CLASS_NAME, e.getMessage(), e);
        }
    }

    /**
     * 获得指定的 property JSON 中的值.
     *
     * @param obj
     *            property JSON 数据
     * @param property
     *            需要获取值的 property 名称
     * @return 此 property 的值
     */
    private String getJSONString(JSONObject obj, String property) {
        String value = null;
        try {
            if (obj != null) {
                value = obj.getString(property);
                if (value.equals("null")) {
                    XLog.i(CLASS_NAME, property + " is 'null'");
                    value = null;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            XLog.d(CLASS_NAME, "Could not get = " + e.getMessage());
        }
        return value;
    }

    /**
     * 存储一个联系人信息. 此函数的主要工作是从传入的 JSONObject 中取出每个 field 值 形成一个 map 后传入 impl 中
     *
     * @param contact
     *            需要存储的联系人的 JSON
     * @return 存储成功返回此联系人 id，失败返回 null
     */
    private String saveContact(JSONObject contact) {
        HashMap<String, Object> propValue = new HashMap<String, Object>();

        String id = getJSONString(contact, XContactAccessor.LOGIC_FIELD_ID);
        propValue.put(XContactAccessor.LOGIC_FIELD_ID, id); // 此处不判空，留给具体实现去负责逻辑

        String rawId = getJSONString(contact,
                XContactAccessor.LOGIC_FIELD_RAWID);
        propValue.put(XContactAccessor.LOGIC_FIELD_RAWID, rawId); // 如果已存在此联系人，则会利用到此

        // 获取 JSON 中的属性数据
        addName(contact, propValue);
        Iterator<String> fieldItor = XContactAccessor.LOGIC_CONTACT_SINGLE_VALUE_FIELDS
                .iterator();
        while (fieldItor.hasNext()) {
            transferSingleValueField(contact, propValue, fieldItor.next());
        }

        // 对一个属性可能存在多份值的数量置零
        mContactAccessor.initQuantityOfFields(propValue);
        fieldItor = XContactAccessor.LOGIC_CONTACT_MULTIPLE_VALUE_FIELDS
                .iterator();
        while (fieldItor.hasNext()) {
            transferMultipleValueFields(contact, propValue, fieldItor.next());
        }

        return mContactAccessor.saveContact(propValue);
    }

    /**
     * 添加 Name.
     *
     * @param contact
     *            联系人的 JSON 数据
     * @param contactPropMap
     *            包含联系人信息的 map
     * @return 更新后的联系人信息的 map
     */
    private void addName(JSONObject contact,
            HashMap<String, Object> contactPropMap) {
        String parentField = XContactAccessor.LOGIC_FIELD_NAME;
        JSONObject nameJson = contact.optJSONObject(parentField);

        if (nameJson != null) {
            Iterator<String> logicSubFields = mContactAccessor
                    .getLogicContactSubFieldsOf(parentField).iterator();

            while (logicSubFields.hasNext()) {
                String childField = logicSubFields.next();
                String key = mContactAccessor.generateSubFieldKey(parentField,
                        0, childField);
                putValueToMap(contactPropMap, key,
                        getJSONString(nameJson, childField));
            }
        }
    }

    /**
     * 将JSONObject中指定属性（没有子属性）的值保存到Hash表中
     *
     * @param jsonObj
     *            存有联系人信息的json对象
     * @param contactData
     *            用于存储联系人信息的hash表
     * @param logicField
     *            要保存的属性名称
     */
    private void transferSingleValueField(JSONObject jsonObj,
            HashMap<String, Object> contactData, String logicField) {
        String fieldValue = getJSONString(jsonObj, logicField);
        if (fieldValue != null) {
            contactData.put(logicField, fieldValue);
        }
    }

    /**
     * 将JSONObject中指定父属性的所有children的子属性数据保存到Hash表中
     *
     * @param jsonObj
     *            存有联系人信息的json对象
     * @param contactData
     *            用于存储联系人信息的hash表
     * @param logicParentField
     *            要保存属性值的父属性
     */
    private void transferMultipleValueFields(JSONObject jsonObj,
            HashMap<String, Object> contactData, String logicParentField) {
        try {
            JSONArray childrenJson = jsonObj.optJSONArray(logicParentField);
            if (null == childrenJson) {
                return;
            }

            int len = childrenJson.length();
            for (int i = 0; i < len; i++) {
                JSONObject childJson = (JSONObject) childrenJson.get(i);

                ListIterator<String> logicSubFieldItor = mContactAccessor
                        .getLogicContactSubFieldsOf(logicParentField)
                        .listIterator();

                while (logicSubFieldItor.hasNext()) {
                    String logicSubField = logicSubFieldItor.next();
                    String key = mContactAccessor.generateSubFieldKey(
                            logicParentField, i, logicSubField);
                    putValueToMap(contactData, key,
                            getJSONString(childJson, logicSubField));
                }

                // 修正photos的value属性值
                if (XContactAccessor.LOGIC_FIELD_PHOTOS
                        .equals(logicParentField)) {
                    byte[] bytes = getPhotoBytes(getJSONString(jsonObj,
                            XContactAccessor.LOGIC_FIELD_COMMON_VALUE));
                    String key = mContactAccessor.generateSubFieldKey(
                            logicParentField, i,
                            XContactAccessor.LOGIC_FIELD_COMMON_VALUE);
                    contactData.put(key, bytes);
                }
            }

            String quantity = mContactAccessor
                    .generateQuantityFieldOf(logicParentField);
            contactData.put(quantity, len);
        } catch (JSONException e) {
            e.printStackTrace();
            XLog.d(CLASS_NAME, "Could not read field [" + logicParentField
                    + "] from json object");
        }
    }

    /**
     * 通过提供的文件名获取照片数据.
     *
     * @param filename
     *            需要获取数据的文件名
     * @return 一个 byte 数组
     * @throws IOException
     */
    private byte[] getPhotoBytes(String filename) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            int bytesRead = 0;
            long totalBytesRead = 0;
            byte[] data = new byte[XConstant.BUFFER_LEN * 4];
            InputStream in = getDataStreamFromUri(filename);

            while ((bytesRead = in.read(data, 0, data.length)) != -1
                    && totalBytesRead <= MAX_PHOTO_SIZE) {
                buffer.write(data, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            in.close();
            buffer.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            XLog.e(CLASS_NAME, e.getMessage(), e);
        } catch (IOException e) {
            e.printStackTrace();
            XLog.e(CLASS_NAME, e.getMessage(), e);
        }
        return buffer.toByteArray();
    }

    /**
     * 基于文件路径或者 URI获取一个输入流 content://, http://, file://.
     *
     * @param path
     *            文件路径
     * @return 输入流
     * @throws IOException
     */
    private InputStream getDataStreamFromUri(String path) throws IOException {
        if (path.startsWith("content:")) {
            Uri uri = Uri.parse(path);
            ContentResolver resolver = getContext().getContentResolver();
            return resolver.openInputStream(uri);
        } else if (path.startsWith("http:") || path.startsWith("file:")) {
            URL url = new URL(path);
            return url.openStream();
        } else {
            return new FileInputStream(path);
        }
    }

    /**
     * 通过 contactId 获取此联系人信息.
     *
     * @param contactId
     *            需要获取信息的联系人
     * @return 此联系人的 JSON 数据
     * @throws JSONException
     */
    private JSONObject getContactById(String contactId) throws JSONException {
        JSONArray fields = new JSONArray();
        fields.put("*");

        HashSet<String> contactPropertySet = getContactFieldSet(fields);
        Cursor cursor = mContactAccessor.getContactById(contactId);
        JSONArray contacts = populateContactArray(contactPropertySet, cursor, 1);
        cursor.close();

        return (contacts.length() == 1) ? contacts.getJSONObject(0) : null;
    }

    /**
     * 将一对键值对加到HashMap中，如果value为null或者key为空串，不作任何操作
     *
     * @param key
     *            键
     * @param value
     *            值
     */
    private void putValueToMap(HashMap<String, Object> map, String key,
            Object value) {
        if (null == map || null == value || XStringUtils.isEmptyString(key)) {
            return;
        }
        if (value instanceof String && XStringUtils.isEmptyString((String) value)) {
            return;
        }
        map.put(key, value);
    }

    /**
     * 调用通讯录界面
     * @param callbackCtx
     */
    private void startContactActivity() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
        mExtensionContext.getSystemContext().startActivityForResult(this, intent, PICK_CONTACT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(PICK_CONTACT == requestCode) {
            if(resultCode == Activity.RESULT_OK) {
                if(!handleResult(intent)) {
                    handleError(CALL_CHOOSE_ERROR);
                }
            }
        }
    }

    /**
     * 获取合法的field字段
     * @param args:要提取的fields字段
     */
    private JSONArray getFields(JSONArray args) {
        JSONObject options = null;
        JSONArray fields = null;
        if(null != args) {
            options = args.optJSONObject(0);
        }
        if(null != options) {
            fields = options.optJSONArray(TAG_FIELDS);;
        }
        if(null == fields) {
            fields = new JSONArray();
            /**如果不传入合法值，则默认获取所有目录*/
            fields.put("*");
        }
        return fields;
    }

    /**
     * 处理activity调用结果
     * @param intent
     */
    private boolean handleResult(Intent intent) {
        Uri contactData = intent.getData();
        Cursor cursor = getContext().getContentResolver().query(contactData, null, null, null, null);
        if (cursor.moveToFirst()) {
            String contactID = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            Cursor cursorContact = mContactAccessor.getContactById(contactID);
            JSONArray result = populateContactArray(getContactFieldSet(mFields),cursorContact, Integer.MAX_VALUE);
            return sendJSCallback(result);
        }
        return false;
    }

    /**
     * 处理成功结果
     * @param contactObject:通讯录JSON对象
     */
    private boolean sendJSCallback(JSONArray contact) {
        if(null != mCallbackCtx) {
            try {
                mCallbackCtx.success(contact.getJSONObject(0));
                return true;
            } catch (JSONException e) {
                XLog.e(CLASS_NAME, e.getMessage());
                handleError(e.getMessage());
            }
        }
        return false;
    }

    /**
     * 处理失败结果
     */
    private void handleError(String errorMsg) {
        new XNotification(mExtensionContext.getSystemContext()).toast(errorMsg);
    }
}
