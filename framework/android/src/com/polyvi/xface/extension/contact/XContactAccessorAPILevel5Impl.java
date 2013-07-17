
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;

import com.polyvi.xface.util.XLog;
import com.polyvi.xface.util.XStringUtils;

/**
 * Contact扩展在android ApiLevel5以上系统上的实现
 * 注：在java变量/方法的命名过程中，使用NATIVE/native前缀时表示数据或名称在android系统内部的表示形式<br/>
 * 而使用LOGIC/logic前缀时则表示数据或名称在js应用中的表示形式，以区分平台与应用之间数据或名称的不同表示形式
 */
public class XContactAccessorAPILevel5Impl extends XContactAccessor {

    protected static final String CLASS_NAME = XContactAccessor.class
            .getSimpleName();

    /** photo type属性值在android系统中对应的值 */
    private static final String NATIVE_PHOTO_TYPE_VALUE = "url";

    /** 此map把JS属性名转化成Android数据库的列名 */
    private static final LinkedHashMap<String, String> LOGIC_FIELD_TO_NATIVE_FIELD_MAP = new LinkedHashMap<String, String>();
    static {
        LOGIC_FIELD_TO_NATIVE_FIELD_MAP.put(LOGIC_CONTACT_FIELDS.get(0),
                ContactsContract.Data.CONTACT_ID); // id
        LOGIC_FIELD_TO_NATIVE_FIELD_MAP.put(LOGIC_CONTACT_FIELDS.get(1),
                StructuredName.DISPLAY_NAME); // name
        LOGIC_FIELD_TO_NATIVE_FIELD_MAP.put(LOGIC_CONTACT_FIELDS.get(2),
                Contacts.DISPLAY_NAME); // displayName
        LOGIC_FIELD_TO_NATIVE_FIELD_MAP.put(LOGIC_CONTACT_FIELDS.get(3),
                Nickname.NAME); // nickname
        LOGIC_FIELD_TO_NATIVE_FIELD_MAP.put(LOGIC_CONTACT_FIELDS.get(4),
                Note.NOTE); // note
        LOGIC_FIELD_TO_NATIVE_FIELD_MAP.put(LOGIC_CONTACT_FIELDS.get(5),
                Event.START_DATE); // birthday
        LOGIC_FIELD_TO_NATIVE_FIELD_MAP.put(LOGIC_CONTACT_FIELDS.get(6),
                StructuredPostal.FORMATTED_ADDRESS); // addresses
        LOGIC_FIELD_TO_NATIVE_FIELD_MAP.put(LOGIC_CONTACT_FIELDS.get(7),
                Organization.COMPANY); // organizations
        LOGIC_FIELD_TO_NATIVE_FIELD_MAP.put(LOGIC_CONTACT_FIELDS.get(8),
                Phone.NUMBER); // phone numbers
        LOGIC_FIELD_TO_NATIVE_FIELD_MAP.put(LOGIC_CONTACT_FIELDS.get(9),
                Email.DATA); // email
        LOGIC_FIELD_TO_NATIVE_FIELD_MAP.put(LOGIC_CONTACT_FIELDS.get(10),
                Im.DATA); // ims
        LOGIC_FIELD_TO_NATIVE_FIELD_MAP.put(LOGIC_CONTACT_FIELDS.get(11),
                Website.URL); // urls
        LOGIC_FIELD_TO_NATIVE_FIELD_MAP.put(LOGIC_CONTACT_FIELDS.get(12),
                Photo.DATA1); // photos
    }

    /** 联系人属性名（js中的表示名称）到对应属性在android系统中的ContentItemType之间的映射 */
    private static final XBiMap<String, String> LOGIC_FIELD_TO_CONTENT_TYPE_MAP = new XBiMap<String, String>();
    static {
        LOGIC_FIELD_TO_CONTENT_TYPE_MAP.put(LOGIC_CONTACT_FIELDS.get(1),
                StructuredName.CONTENT_ITEM_TYPE); // name
        LOGIC_FIELD_TO_CONTENT_TYPE_MAP.put(LOGIC_CONTACT_FIELDS.get(3),
                Nickname.CONTENT_ITEM_TYPE); // nickname
        LOGIC_FIELD_TO_CONTENT_TYPE_MAP.put(LOGIC_CONTACT_FIELDS.get(4),
                Note.CONTENT_ITEM_TYPE); // note
        LOGIC_FIELD_TO_CONTENT_TYPE_MAP.put(LOGIC_CONTACT_FIELDS.get(5),
                Event.CONTENT_ITEM_TYPE); // birthday
        LOGIC_FIELD_TO_CONTENT_TYPE_MAP.put(LOGIC_CONTACT_FIELDS.get(6),
                StructuredPostal.CONTENT_ITEM_TYPE); // addresses
        LOGIC_FIELD_TO_CONTENT_TYPE_MAP.put(LOGIC_CONTACT_FIELDS.get(7),
                Organization.CONTENT_ITEM_TYPE); // organizations
        LOGIC_FIELD_TO_CONTENT_TYPE_MAP.put(LOGIC_CONTACT_FIELDS.get(8),
                Phone.CONTENT_ITEM_TYPE); // phone numbers
        LOGIC_FIELD_TO_CONTENT_TYPE_MAP.put(LOGIC_CONTACT_FIELDS.get(9),
                Email.CONTENT_ITEM_TYPE); // emails
        LOGIC_FIELD_TO_CONTENT_TYPE_MAP.put(LOGIC_CONTACT_FIELDS.get(10),
                Im.CONTENT_ITEM_TYPE); // ims
        LOGIC_FIELD_TO_CONTENT_TYPE_MAP.put(LOGIC_CONTACT_FIELDS.get(11),
                Website.CONTENT_ITEM_TYPE); // urls
        LOGIC_FIELD_TO_CONTENT_TYPE_MAP.put(LOGIC_CONTACT_FIELDS.get(12),
                Photo.CONTENT_ITEM_TYPE); // photos
    }

    // 以下 map 建立 JS 与 Android 内部关于 AddressType 的转换关系
    private static final XBiMap<String, Integer> CONTACT_ADDRESS_TYPE_MAP = new XBiMap<String, Integer>();
    static {
        CONTACT_ADDRESS_TYPE_MAP.put("work", StructuredPostal.TYPE_WORK);
        CONTACT_ADDRESS_TYPE_MAP.put("home", StructuredPostal.TYPE_HOME);
        CONTACT_ADDRESS_TYPE_MAP.put("other", StructuredPostal.TYPE_OTHER);
    }

    // 以下 map 建立 JS 与 Android 内部关于 PhoneType 的转换关系
    private static final XBiMap<String, Integer> CONTACT_PHONE_TYPE_MAP = new XBiMap<String, Integer>();
    static {
        CONTACT_PHONE_TYPE_MAP.put("home", Phone.TYPE_HOME);
        CONTACT_PHONE_TYPE_MAP.put("mobile", Phone.TYPE_MOBILE);
        CONTACT_PHONE_TYPE_MAP.put("work", Phone.TYPE_WORK);
        CONTACT_PHONE_TYPE_MAP.put("work fax", Phone.TYPE_FAX_WORK);
        CONTACT_PHONE_TYPE_MAP.put("home fax", Phone.TYPE_FAX_HOME);
        CONTACT_PHONE_TYPE_MAP.put("fax", Phone.TYPE_FAX_WORK);
        CONTACT_PHONE_TYPE_MAP.put("pager", Phone.TYPE_PAGER);
        CONTACT_PHONE_TYPE_MAP.put("other", Phone.TYPE_OTHER);
        CONTACT_PHONE_TYPE_MAP.put("car", Phone.TYPE_CAR);
        CONTACT_PHONE_TYPE_MAP.put("company main", Phone.TYPE_COMPANY_MAIN);
        CONTACT_PHONE_TYPE_MAP.put("isdn", Phone.TYPE_ISDN);
        CONTACT_PHONE_TYPE_MAP.put("main", Phone.TYPE_MAIN);
        CONTACT_PHONE_TYPE_MAP.put("other fax", Phone.TYPE_OTHER_FAX);
        CONTACT_PHONE_TYPE_MAP.put("radio", Phone.TYPE_RADIO);
        CONTACT_PHONE_TYPE_MAP.put("telex", Phone.TYPE_TELEX);
        CONTACT_PHONE_TYPE_MAP.put("work mobile", Phone.TYPE_WORK_MOBILE);
        CONTACT_PHONE_TYPE_MAP.put("work pager", Phone.TYPE_WORK_PAGER);
        CONTACT_PHONE_TYPE_MAP.put("assistant", Phone.TYPE_ASSISTANT);
        CONTACT_PHONE_TYPE_MAP.put("mms", Phone.TYPE_MMS);
        CONTACT_PHONE_TYPE_MAP.put("callback", Phone.TYPE_CALLBACK);
        CONTACT_PHONE_TYPE_MAP.put("tty ttd", Phone.TYPE_TTY_TDD);
        CONTACT_PHONE_TYPE_MAP.put("custom", Phone.TYPE_CUSTOM);
    }

    // 以下map建立JS与Android内部关于email/im/url/photo类型的转换关系
    private static final XBiMap<String, Integer> CONTACT_BASE_TYPE_MAP = new XBiMap<String, Integer>();
    static {
        CONTACT_BASE_TYPE_MAP.put("custom", Email.TYPE_CUSTOM);
        CONTACT_BASE_TYPE_MAP.put("home", Email.TYPE_HOME);
        CONTACT_BASE_TYPE_MAP.put("work", Email.TYPE_WORK);
        CONTACT_BASE_TYPE_MAP.put("mobile", Email.TYPE_MOBILE);
        CONTACT_BASE_TYPE_MAP.put("other", Email.TYPE_OTHER);
    }

    // 以下map建立JS与Android内部关于Organization类型的转换关系
    private static final XBiMap<String, Integer> CONTACT_ORGANIZATION_TYPE_MAP = new XBiMap<String, Integer>();
    static {
        CONTACT_ORGANIZATION_TYPE_MAP.put("custom", Organization.TYPE_CUSTOM);
        CONTACT_ORGANIZATION_TYPE_MAP.put("work", Organization.TYPE_WORK);
        CONTACT_ORGANIZATION_TYPE_MAP.put("other", Organization.TYPE_OTHER);
    }

    /** Contact Name子属性名称的js表示和android系统表示之间的映射 */
    private static final Map<String, String> CONTACT_NAME_FIELD_MAP = new HashMap<String, String>();
    static {
        CONTACT_NAME_FIELD_MAP.put(LOGIC_CONTACT_NAME_FIELDS.get(0),
                StructuredName.FAMILY_NAME);
        CONTACT_NAME_FIELD_MAP.put(LOGIC_CONTACT_NAME_FIELDS.get(1),
                StructuredName.MIDDLE_NAME);
        CONTACT_NAME_FIELD_MAP.put(LOGIC_CONTACT_NAME_FIELDS.get(2),
                StructuredName.GIVEN_NAME);
        CONTACT_NAME_FIELD_MAP.put(LOGIC_CONTACT_NAME_FIELDS.get(3),
                StructuredName.PREFIX);
        CONTACT_NAME_FIELD_MAP.put(LOGIC_CONTACT_NAME_FIELDS.get(4),
                StructuredName.SUFFIX);
    }

    /** Contact PhoneNumber子属性名称的js表示和android系统表示之间的映射 */
    private static final Map<String, String> CONTACT_PHONE_NUMBER_FIELD_MAP = new HashMap<String, String>();
    static {
        CONTACT_PHONE_NUMBER_FIELD_MAP.put(
                LOGIC_CONTACT_COMMON_SUB_FIELDS.get(0), Phone.NUMBER);
        CONTACT_PHONE_NUMBER_FIELD_MAP.put(
                LOGIC_CONTACT_COMMON_SUB_FIELDS.get(1), Phone.TYPE);
    }

    /** Contact Email子属性名称的js表示和android系统表示之间的映射 */
    private static final Map<String, String> CONTACT_EMAIL_FIELD_MAP = new HashMap<String, String>();
    static {
        CONTACT_EMAIL_FIELD_MAP.put(LOGIC_CONTACT_COMMON_SUB_FIELDS.get(0),
                Email.DATA);
        CONTACT_EMAIL_FIELD_MAP.put(LOGIC_CONTACT_COMMON_SUB_FIELDS.get(1),
                Email.TYPE);
    }

    /** Contact Address子属性名称的js表示和android系统表示之间的映射 */
    private static final Map<String, String> CONTACT_ADDRESS_FIELD_MAP = new HashMap<String, String>();
    static {
        CONTACT_ADDRESS_FIELD_MAP.put(LOGIC_CONTACT_ADDRESS_FIELDS.get(0),
                StructuredPostal.TYPE);
        CONTACT_ADDRESS_FIELD_MAP.put(LOGIC_CONTACT_ADDRESS_FIELDS.get(1),
                StructuredPostal.FORMATTED_ADDRESS);
        CONTACT_ADDRESS_FIELD_MAP.put(LOGIC_CONTACT_ADDRESS_FIELDS.get(2),
                StructuredPostal.STREET);
        CONTACT_ADDRESS_FIELD_MAP.put(LOGIC_CONTACT_ADDRESS_FIELDS.get(3),
                StructuredPostal.CITY);
        CONTACT_ADDRESS_FIELD_MAP.put(LOGIC_CONTACT_ADDRESS_FIELDS.get(4),
                StructuredPostal.REGION);
        CONTACT_ADDRESS_FIELD_MAP.put(LOGIC_CONTACT_ADDRESS_FIELDS.get(5),
                StructuredPostal.POSTCODE);
        CONTACT_ADDRESS_FIELD_MAP.put(LOGIC_CONTACT_ADDRESS_FIELDS.get(6),
                StructuredPostal.COUNTRY);
    }

    /** Contact Organization子属性名称的js表示和android系统表示之间的映射 */
    private static final Map<String, String> CONTACT_ORGANIZATION_FIELD_MAP = new HashMap<String, String>();
    static {
        CONTACT_ORGANIZATION_FIELD_MAP.put(
                LOGIC_CONTACT_ORGANIZATION_FIELDS.get(0), Organization.TYPE);
        CONTACT_ORGANIZATION_FIELD_MAP.put(
                LOGIC_CONTACT_ORGANIZATION_FIELDS.get(1),
                Organization.DEPARTMENT);
        CONTACT_ORGANIZATION_FIELD_MAP.put(
                LOGIC_CONTACT_ORGANIZATION_FIELDS.get(2), Organization.COMPANY);
        CONTACT_ORGANIZATION_FIELD_MAP.put(
                LOGIC_CONTACT_ORGANIZATION_FIELDS.get(3), Organization.TITLE);
    }

    /** Contact IM子属性名称的js表示和android系统表示之间的映射 */
    private static final Map<String, String> CONTACT_IM_FIELD_MAP = new HashMap<String, String>();
    static {
        CONTACT_IM_FIELD_MAP.put(LOGIC_CONTACT_COMMON_SUB_FIELDS.get(0),
                Im.DATA);
        CONTACT_IM_FIELD_MAP.put(LOGIC_CONTACT_COMMON_SUB_FIELDS.get(1),
                Im.TYPE);
    }

    /** Contact URL子属性名称的js表示和android系统表示之间的映射 */
    private static final Map<String, String> CONTACT_URL_FIELD_MAP = new HashMap<String, String>();
    static {
        CONTACT_URL_FIELD_MAP.put(LOGIC_CONTACT_COMMON_SUB_FIELDS.get(0),
                Website.DATA);
        CONTACT_URL_FIELD_MAP.put(LOGIC_CONTACT_COMMON_SUB_FIELDS.get(1),
                Website.TYPE);
    }

    private Context mContext;

    public XContactAccessorAPILevel5Impl(Context context) {
        mContext = context;
    }

    @Override
    public boolean removeContact(String contactId) {
        int result = 0;
        Cursor cursor = queryDB(ContactsContract.Contacts.CONTENT_URI, null,
                ContactsContract.Contacts._ID + " = '" + contactId + "'", null);

        if (cursor == null) {
            return false;
        }

        if (cursor.moveToFirst()) {
            String lookupKey = cursor.getString(cursor
                    .getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
            Uri uri = Uri.withAppendedPath(
                    ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
            result = mContext.getContentResolver().delete(uri, null, null);
        } else {
            XLog.d(CLASS_NAME, "Could not find contact with ID");
        }
        cursor.close();

        return (result > 0) ? true : false;
    }

    /**
     * 判断是否获取所有的联系人信息，即传入的匹配字符串能够匹配所有属性
     *
     * @param searchTerm
     *            进行匹配的字符串，如 Bob 等
     */
    private boolean willAcquireAllContacts(String searchTerm) {
        return "%".equals(searchTerm);
    }

    /**
     * 根据传入的匹配条件，构建针对联系人所有属性域进行匹配的where语句
     *
     * @param searchTerm
     *            查询的匹配条件，如Bob等
     */
    private String buildWhereClauseWithAllField(String searchTerm) {
        // 存放where语句列表
        ArrayList<String> whereList = new ArrayList<String>();
        // 存放where语句中的参数列表
        ArrayList<String> whereArgsList = new ArrayList<String>();

        whereList.add("("
                + LOGIC_FIELD_TO_NATIVE_FIELD_MAP.get(LOGIC_FIELD_DISPLAYNAME)
                + " LIKE ? )");
        whereArgsList.add(searchTerm);

        Iterator<Entry<String, String>> fieldToContentTypeEntries = LOGIC_FIELD_TO_CONTENT_TYPE_MAP
                .entrySet().iterator();
        while (fieldToContentTypeEntries.hasNext()) {
            Entry<String, String> entry = fieldToContentTypeEntries.next();
            String logicField = entry.getKey();
            String contentItemType = entry.getValue();
            String nativeField = LOGIC_FIELD_TO_NATIVE_FIELD_MAP
                    .get(logicField);

            whereList.add("(" + nativeField + " LIKE ? AND "
                    + ContactsContract.Data.MIMETYPE + " = ? )");
            whereArgsList.add(searchTerm);
            whereArgsList.add(contentItemType);
        }

        return mergeSubWhereClause(whereList, whereArgsList);
    }

    /**
     * 根据传入的匹配条件，为需要查询的域构建查询的where语句.
     *
     * @param logicSearchFields
     *            要进行匹配操作的联系人属性集合
     *
     * @param searchTerm
     *            查询的匹配条件，如Bob等
     */
    private String buildWhereClauseWithRequiredField(
            Set<String> logicSearchFields, String searchTerm) {
        Iterator<String> seachFieldItor = logicSearchFields.iterator();
        // 存放where语句列表
        ArrayList<String> whereList = new ArrayList<String>();
        // 存放where语句中的参数列表
        ArrayList<String> whereArgsList = new ArrayList<String>();

        while (seachFieldItor.hasNext()) {
            String searchField = seachFieldItor.next();

            // id和displayName特殊处理
            if (LOGIC_FIELD_ID.equals(searchField)) {
                whereList.add("("
                        + LOGIC_FIELD_TO_NATIVE_FIELD_MAP.get(searchField)
                        + " = ? )");
                // 如果查找 field 的是 ID，则是精确匹配，需取 searchTerm 的子串表示为 id 号，作为查询条件
                whereArgsList.add(searchTerm.substring(1,
                        searchTerm.length() - 1));
            } else if (LOGIC_FIELD_DISPLAYNAME.equals(searchField)) {
                whereList.add("("
                        + LOGIC_FIELD_TO_NATIVE_FIELD_MAP.get(searchField)
                        + " LIKE ? )");
                whereArgsList.add(searchTerm);
            } else {
                String contentItemType = LOGIC_FIELD_TO_CONTENT_TYPE_MAP
                        .get(searchField);
                if (null != contentItemType) {
                    String nativeField = LOGIC_FIELD_TO_NATIVE_FIELD_MAP
                            .get(searchField);
                    whereList.add("(" + nativeField + " LIKE ? AND "
                            + ContactsContract.Data.MIMETYPE + " = ? )");
                    whereArgsList.add(searchTerm);
                    whereArgsList.add(contentItemType);
                }
            }
        }

        return mergeSubWhereClause(whereList, whereArgsList);
    }

    /**
     * 将带通配符'?'的多条where语句和相应的参数组合成一条不带通配符'?'的where语句
     *
     * @param whereClause
     *            存放多条where语句的列表
     * @param whereArgs
     *            存放whereClause中通配符'?'对应参数的列表
     * @return 用于最终查询的where语句
     */
    private String mergeSubWhereClause(List<String> whereClause,
            List<String> whereArgs) {
        StringBuffer where = new StringBuffer();
        int size = whereClause.size();
        for (int i = 0; i < size; i++) {
            where.append(whereClause.get(i));
            if (i != size - 1) {
                where.append(" OR ");
            }
        }

        size = whereArgs.size();
        for (int i = 0; i < size; i++) {
            int index = where.indexOf("?");
            where.replace(index, index + 1, "'" + whereArgs.get(i) + "'");
        }

        return where.toString();
    }

    /**
     * 根据查询条件查询数据库操作.
     *
     * @param projection
     *            需要获取的属性列表
     * @param whereClause
     *            查询用的where语句
     * @return 游标
     */
    private Cursor queryDB(Uri uri, String[] projection, String whereClause,
            String sortOrder) {
        return mContext.getContentResolver().query(uri, projection,
                whereClause, null, sortOrder);
    }

    /**
     * 获取手机上所有联系人的contactId，sim卡和email帐户除外
     */
    private Set<String> getContactIdOnlyPhoneBook() {
        // sumsung sim account type: 'vnd.sec.contact.sim',
        // htc sim account type: 'com.anddroid.contacts.sim'
        // accountType='com.google' represent google account type, we think it
        // is phone account type
        Cursor cursor = queryDB(RawContacts.CONTENT_URI,
                new String[] { RawContacts.CONTACT_ID }, RawContacts.CONTACT_ID
                        + " NOT NULL AND (" + RawContacts.ACCOUNT_TYPE
                        + " NOT LIKE '%sim%' or " + RawContacts.ACCOUNT_TYPE
                        + " IS NULL)", null);
        Set<String> contactIds = new HashSet<String>();
        if (cursor.moveToFirst()) {
            contactIds.add(cursor.getString(0));
            while (cursor.moveToNext()) {
                contactIds.add(cursor.getString(0));
            }
        }
        cursor.close();
        return contactIds;
    }

    @Override
    public Cursor getMatchedContactsCursor(Set<String> logicFieldSet,
            String searchExpression, boolean willMatchAllField,
            String accountType) {
        StringBuffer whereClause = new StringBuffer(); // 综合需要查询域的最终 WHERE子句

        // 首先判断是否获取所有联系人，如果是，则可以不设置匹配条件
        if (!willAcquireAllContacts(searchExpression)) {
            // fields 是否为"*"，即是否匹配所有属性
            if (willMatchAllField) {
                whereClause
                        .append(buildWhereClauseWithAllField(searchExpression));
            } else {
                whereClause.append(buildWhereClauseWithRequiredField(
                        logicFieldSet, searchExpression));
            }
            if (whereClause.length() != 0) {
                whereClause.insert(0, "(");
                whereClause.append(")");
            }
        }

        // 根据accountType参数设置是读手机上，sim卡还是手机和卡上的所有联系人
        if (!CONTACT_ACCOUNT_TYPE_ALL.equals(accountType)) {
            Set<String> phoneBookIds = getContactIdOnlyPhoneBook();
            if (whereClause.length() != 0) {
                whereClause.append(" AND ");
            }
            whereClause.append(ContactsContract.Data.CONTACT_ID);
            if (CONTACT_ACCOUNT_TYPE_PHONE.equals(accountType)) {
                whereClause.append(" IN ");
            } else if (CONTACT_ACCOUNT_TYPE_SIM.equals(accountType)) {
                whereClause.append(" NOT IN ");
            } else {
                whereClause.append(" IN ");
            }
            whereClause.append(buildCollectionWhereClause(phoneBookIds));
        }

        String where = whereClause.length() == 0 ? null : whereClause
                .toString();
        Cursor idCursor = queryDB(ContactsContract.Data.CONTENT_URI,
                new String[] { ContactsContract.Data.CONTACT_ID }, where,
                ContactsContract.Data.CONTACT_ID + " ASC");

        if (idCursor.getCount() <= 0) {
            return idCursor;
        }

        // Create a set of unique ids
        Set<String> contactIds = new HashSet<String>();
        int idColumn = -1;
        while (idCursor.moveToNext()) {
            if (idColumn < 0) {
                idColumn = idCursor
                        .getColumnIndex(ContactsContract.Data.CONTACT_ID);
            }
            contactIds.add(idCursor.getString(idColumn));
        }
        idCursor.close();

        where = ContactsContract.Data.CONTACT_ID + " IN "
                + buildCollectionWhereClause(contactIds);
        Cursor c = queryDB(ContactsContract.Data.CONTENT_URI, null, where,
                ContactsContract.Data.CONTACT_ID + " ASC");
        return c;
    }

    /**
     * 依据传入的数据集合构建一个查询子句，如:"('data1', 'data2', ....)"
     *
     * @param dataSet
     *            查询语句用到的数据集合
     */
    private String buildCollectionWhereClause(Set<String> dataSet) {
        // 计算特定的 id
        Iterator<String> itor = dataSet.iterator();
        StringBuffer buffer = new StringBuffer("(");

        while (itor.hasNext()) {
            buffer.append("'" + itor.next() + "',");
        }
        if (dataSet.size() > 0) { // 去掉最后的‘,’
            buffer.deleteCharAt(buffer.length() - 1);
        }

        buffer.append(")");
        return buffer.toString();
    }

    @Override
    public String getContactId(Cursor c) {
        return c.getString(c.getColumnIndex(ContactsContract.Data.CONTACT_ID));
    }

    @Override
    public String getRawId(Cursor c) {
        return c.getString(c
                .getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
    }

    @Override
    public void cursorToHashMap(Map<String, Object> contactsMap, Cursor c) {
        // 获取当前行的 mimetype用于比较
        String mimetype = c.getString(c
                .getColumnIndex(ContactsContract.Data.MIMETYPE));

        if (StructuredName.CONTENT_ITEM_TYPE.equals(mimetype)) {
            contactsMap.put(LOGIC_FIELD_DISPLAYNAME,
                    c.getString(c.getColumnIndex(StructuredName.DISPLAY_NAME)));
            nameQuery(contactsMap, c);
        } else if (Event.CONTENT_ITEM_TYPE.equals(mimetype)) {
            if (Event.TYPE_BIRTHDAY == c.getInt(c.getColumnIndex(Event.TYPE))) {
                contactsMap.put(LOGIC_FIELD_BIRTHDAY,
                        c.getString(c.getColumnIndex(Event.START_DATE)));
            }
        } else if (Nickname.CONTENT_ITEM_TYPE.equals(mimetype)) {
            contactsMap.put(LOGIC_FIELD_NICKNAME,
                    c.getString(c.getColumnIndex(Nickname.NAME)));
        } else if (Note.CONTENT_ITEM_TYPE.equals(mimetype)) {
            contactsMap.put(LOGIC_FIELD_NOTE,
                    c.getString(c.getColumnIndex(Note.NOTE)));
        } else {
            String logicField = LOGIC_FIELD_TO_CONTENT_TYPE_MAP
                    .getKeyByValue(mimetype);
            if (LOGIC_CONTACT_MULTIPLE_VALUE_FIELDS.contains(logicField)) {
                multipleValueFieldQuery(contactsMap, c, logicField);
            }
        }
    }

    /**
     * 从cursor中读取name相关属性数据
     *
     * @param contactsMap
     *            用于保存联系人信息的hash表
     * @param cursor
     *            数据库中的当前行
     * @return
     */
    private void nameQuery(Map<String, Object> contactsMap, Cursor cursor) {
        StringBuffer formatted = new StringBuffer();

        Iterator<Entry<String, String>> nameFieldEntries = CONTACT_NAME_FIELD_MAP
                .entrySet().iterator();
        while (nameFieldEntries.hasNext()) {
            Entry<String, String> nameFieldEntry = nameFieldEntries.next();
            String logicNameField = nameFieldEntry.getKey();
            String nativeNameField = nameFieldEntry.getValue();
            String fieldValue = cursor.getString(cursor
                    .getColumnIndex(nativeNameField));
            String key = generateSubFieldKey(XContactAccessor.LOGIC_FIELD_NAME,
                    0, logicNameField);

            if (null != fieldValue) {
                contactsMap.put(key, fieldValue);
                formatted.append(fieldValue);
                formatted.append(" ");
            }
        }
        int len = formatted.length();
        if (len != 0) {
            formatted.deleteCharAt(len - 1);
        }
        String key = generateSubFieldKey(XContactAccessor.LOGIC_FIELD_NAME, 0,
                LOGIC_FIELD_NAME_FORMATTED);
        contactsMap.put(key, formatted.toString());
    }

    /**
     * 获取hash表中保存的指定quantityField对应的值，如果不存在返回0
     */
    private int getQuantityValue(Map<String, Object> contactsMap,
            String quantityField) {
        Object quantityObj = contactsMap.get(quantityField);
        if (null != quantityObj) {
            return ((Integer) quantityObj).intValue();
        }
        return 0;
    }

    /**
     * 将cursor中当前指向的item中的联系人信息存放到hash表中
     * 注：仅在cursor中当前指向的item数据为MultipleValueField的子属性数据时使用<br/>
     * （如：addresses， organizations，urls的子属性集）
     */
    private void multipleValueFieldQuery(Map<String, Object> contactsMap,
            Cursor cursor, String logicParentField) {
        String quantityField = generateQuantityFieldOf(logicParentField);
        int quantity = getQuantityValue(contactsMap, quantityField);

        List<String> logicSubFields = getLogicContactSubFieldsOf(logicParentField);
        Map<String, String> subFieldsMap = getContactSubFieldsMapOf(logicParentField);
        for (String subField : logicSubFields) {
            String key = generateSubFieldKey(logicParentField, quantity,
                    subField);
            if (LOGIC_FIELD_COMMON_ID.equals(subField)) {
                contactsMap.put(key, cursor.getString(cursor
                        .getColumnIndex(BaseColumns._ID)));
            } else if (LOGIC_FIELD_COMMON_PREF.equals(subField)) {
                contactsMap.put(key, false); // Android没有存储pref属性
            } else if (LOGIC_FIELD_COMMON_TYPE.equals(subField)) {
                if (LOGIC_FIELD_PHOTOS.equals(logicParentField)) {
                    contactsMap.put(key, NATIVE_PHOTO_TYPE_VALUE);
                } else {
                    int nativeTypeValue = cursor.getInt(cursor
                            .getColumnIndex(subFieldsMap.get(subField)));
                    String logicTypeValue = getLogicTypeValue(logicParentField,
                            nativeTypeValue);
                    contactsMap.put(key, logicTypeValue);
                }
            } else if (LOGIC_FIELD_COMMON_VALUE.equals(subField)
                    && LOGIC_FIELD_PHOTOS.equals(logicParentField)) {
                Uri person = ContentUris.withAppendedId(
                        ContactsContract.Contacts.CONTENT_URI, (new Long(
                                getContactId(cursor))));
                Uri photoUri = Uri.withAppendedPath(person,
                        ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                contactsMap.put(key, photoUri.toString());
            } else {
                Object fieldValue = cursor.getString(cursor
                        .getColumnIndex(subFieldsMap.get(subField)));
                contactsMap.put(key, fieldValue);
            }
        }
        contactsMap.put(quantityField, quantity + 1);
    }

    /**
     * 将Android系统的type属性值转换为JS中定义的type属性值.
     *
     * @param logicParentField
     *            要转换的type属性值的父属性
     * @param nativeTypeValue
     *            android中表示的type属性值
     */
    private String getLogicTypeValue(String logicParentField,
            int nativeTypeValue) {
        XBiMap<String, Integer> typeMap = null;
        if (LOGIC_FIELD_PHONENUMBERS.equals(logicParentField)) {
            typeMap = CONTACT_PHONE_TYPE_MAP;
        } else if (LOGIC_FIELD_ORGANIZATIONS.equals(logicParentField)) {
            typeMap = CONTACT_ORGANIZATION_TYPE_MAP;
        } else if (LOGIC_FIELD_ADDRESSES.equals(logicParentField)) {
            typeMap = CONTACT_ADDRESS_TYPE_MAP;
        } else {
            typeMap = CONTACT_BASE_TYPE_MAP;
        }

        String logicTypeValue = typeMap.getKeyByValue(nativeTypeValue);
        return (null != logicTypeValue) ? logicTypeValue : "other";
    }

    /**
     * 将JS中定义的type属性值转换为Android系统的type属性值.
     *
     * @param logicParentField
     *            要转换的type属性值的父属性
     * @param logicTypeValue
     *            js中表示的type属性值
     */
    private int getRealTypeValue(String logicParentField, String logicTypeValue) {
        Integer valueObj = null;
        logicTypeValue = logicTypeValue.toLowerCase();
        Map<String, Integer> typeMap = null;
        if (LOGIC_FIELD_PHONENUMBERS.equals(logicParentField)) {
            typeMap = CONTACT_PHONE_TYPE_MAP;
        } else if (LOGIC_FIELD_ORGANIZATIONS.equals(logicParentField)) {
            typeMap = CONTACT_ORGANIZATION_TYPE_MAP;
        } else if (LOGIC_FIELD_ADDRESSES.equals(logicParentField)) {
            typeMap = CONTACT_ADDRESS_TYPE_MAP;
        } else {
            typeMap = CONTACT_BASE_TYPE_MAP;
        }

        valueObj = typeMap.get(logicTypeValue);
        if (null == valueObj) {
            valueObj = typeMap.get("other");
        }
        return valueObj.intValue();
    }

    @Override
    public Cursor getContactById(String contactId) {
        return queryDB(ContactsContract.Data.CONTENT_URI, null,
                ContactsContract.Data.CONTACT_ID + " = " + contactId + " ",
                ContactsContract.Data.CONTACT_ID + " ASC");
    }

    @Override
    public String saveContact(Map<String, Object> contactPropMap) {
        AccountManager mgr = AccountManager.get(mContext);
        Account[] accounts = mgr.getAccounts();

        HashMap<String, String> account = getAccountInfo(accounts);
        String accountName = account.get("name");
        String accountType = account.get("type");

        String id = (String) contactPropMap.get(LOGIC_FIELD_ID);

        // 创建新的联系人
        if (id == null) {
            return createNewContact(contactPropMap, accountType, accountName);
        }
        // 修改现有联系人
        else {
            return modifyContact(id, contactPropMap, accountType, accountName);
        }
    }

    /**
     * 获取默认的帐户类型和名称
     *
     * @param accounts
     *            account 数组
     * @return 包含accountName 和 accountType 信息的 map
     */
    private HashMap<String, String> getAccountInfo(Account[] accounts) {
        // 以前通过代码自动获取系统中存在的帐户作为默认帐户使用，比如邮件帐户，google帐户，
        // 可能导致新创建的联系人在系统的联系人程序中看不到，且不能正常删除新建的联系人，
        // 故将默认的帐户名称和类型均设为null，以避免出现上述问题
        HashMap<String, String> account = new HashMap<String, String>();
        account.put("name", null);
        account.put("type", null);
        return account;
    }

    /**
     * 保存一个新的联系人，并将其数据保存到设备联系人数据库中.
     *
     * @param contactPropMap
     *            包含联系人信息的 map
     * @param accountType
     *            账号类型
     * @param accountName
     *            账号名称
     * @return 存储成功返回新联系人的 id，存储失败返回 null
     */
    private String createNewContact(Map<String, Object> contactPropMap,
            String accountType, String accountName) {
        // 创建一个属性列表用于添加将要存储到数据库中的值
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        ops.add(ContentProviderOperation
                .newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE,
                        accountType)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME,
                        accountName).build());

        buildSaveContactNameOperation(contactPropMap, ops, null);

        // 通过迭代处理的 field 包括：nickname、note、birthday
        Iterator<String> logicFieldItor = LOGIC_CONTACT_SINGLE_VALUE_FIELDS
                .iterator();
        logicFieldItor.next(); // 跳过 displayName 域
        while (logicFieldItor.hasNext()) {
            addSingleValueProperty(contactPropMap, logicFieldItor.next(), ops);
        }

        // 通过迭代处理的 field 包括：organizations、ims、urls、phoneNumbers、emails、
        // addresses 和 photo
        logicFieldItor = LOGIC_CONTACT_MULTIPLE_VALUE_FIELDS.iterator();
        while (logicFieldItor.hasNext()) {
            String fieldName = logicFieldItor.next();
            addMultipleValueProperty(contactPropMap, fieldName, ops);
        }

        String newId = null;
        // 添加联系人
        try {
            ContentProviderResult[] cpResults = mContext.getContentResolver()
                    .applyBatch(ContactsContract.AUTHORITY, ops);
            if (cpResults.length >= 0) {
                String rawId = cpResults[0].uri.getLastPathSegment();

                // 通过 rawId 查询 Contact Id
                Cursor c = mContext
                        .getContentResolver()
                        .query(ContactsContract.RawContacts.CONTENT_URI,
                                new String[] { ContactsContract.RawContacts.CONTACT_ID },
                                ContactsContract.RawContacts._ID + "=?",
                                new String[] { String.valueOf(rawId) }, null);

                if (c.moveToFirst()) {
                    newId = c.getString(0);
                }
            }
        } catch (RemoteException e) {
            XLog.e(CLASS_NAME, e.getMessage(), e);
        } catch (OperationApplicationException e) {
            XLog.e(CLASS_NAME, e.getMessage(), e);
        }

        return newId;
    }

    /**
     * 为存在单值的域添加属性
     *
     * @param contactPropMap
     *            包含联系人信息的 map
     * @param logicField
     *            域名称
     * @param ops
     *            ContentProviderOperation
     */
    private void addSingleValueProperty(Map<String, Object> contactPropMap,
            String logicField, ArrayList<ContentProviderOperation> ops) {
        String fieldValue = (String) contactPropMap.get(logicField);
        if (XStringUtils.isEmptyString(fieldValue)) {
            return;
        }

        ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        LOGIC_FIELD_TO_CONTENT_TYPE_MAP.get(logicField));

        if (logicField == LOGIC_FIELD_BIRTHDAY) {
            builder.withValue(ContactsContract.CommonDataKinds.Event.TYPE,
                    ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY);
        }
        builder.withValue(LOGIC_FIELD_TO_NATIVE_FIELD_MAP.get(logicField),
                fieldValue);
        ops.add(builder.build());
    }

    /**
     * 为存在多值的域添加属性
     *
     * @param contactPropMap
     *            包含联系人信息的 map
     * @param logicParentField
     *            域名
     * @param ops
     *            ContentProviderOperation
     */
    private void addMultipleValueProperty(Map<String, Object> contactPropMap,
            String logicParentField, ArrayList<ContentProviderOperation> ops) {
        String quantityField = generateQuantityFieldOf(logicParentField);
        int quantity = (Integer) contactPropMap.get(quantityField);
        if (quantity <= 0) {
            return;
        }

        for (int i = 0; i < quantity; i++) {
            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(
                            ContactsContract.Data.RAW_CONTACT_ID, 0);

            String contentItemType = LOGIC_FIELD_TO_CONTENT_TYPE_MAP
                    .get(logicParentField);
            builder.withValue(ContactsContract.Data.MIMETYPE, contentItemType);
            String commonValueFieldKey = generateSubFieldKey(logicParentField,
                    i, LOGIC_FIELD_COMMON_VALUE);
            Object commonValue = contactPropMap.get(commonValueFieldKey);
            // 如果ContactField中的value属性为null，则不保存该ContactField，不然部分手机在保存的时候会抛异常
            if (isOnlyCommonSubFieldsIncluded(logicParentField)
                    && null == commonValue) {
                continue;
            }
            if (logicParentField == LOGIC_FIELD_PHOTOS) {
                builder.withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                        .withValue(Photo.PHOTO, commonValue);
            } else {
                setSubFieldValueToBuilder(builder, contactPropMap,
                        logicParentField, i);
            }
            ops.add(builder.build());
        }
    }

    /**
     * 将指定父属性域的所有子属性值设置到builder中（photo的子属性除外）
     *
     * @param builder
     *            用于存储子属性的属性名及其值的构造器
     * @param fieldValueMap
     *            源数据（子属性名称->属性值的映射）
     * @param parentField
     *            父属性域的名称（在js中的表示名称）
     * @param childIndex
     *            要保存子属性所在child的索引值
     */
    private void setSubFieldValueToBuilder(
            ContentProviderOperation.Builder builder,
            Map<String, Object> fieldValueMap, String parentField,
            int childIndex) {
        Map<String, String> subFieldMap = getContactSubFieldsMapOf(parentField);
        Iterator<Entry<String, String>> subFieldEntries = subFieldMap
                .entrySet().iterator();
        while (subFieldEntries.hasNext()) {
            Entry<String, String> subFieldEntry = subFieldEntries.next();
            String logicFieldName = subFieldEntry.getKey();
            String nativeFieldName = subFieldEntry.getValue();
            String key = generateSubFieldKey(parentField, childIndex,
                    logicFieldName);
            Object fieldValue = fieldValueMap.get(key);
            if (null == fieldValue) {
                continue;
            }

            if (LOGIC_FIELD_COMMON_TYPE.equals(logicFieldName)) {
                fieldValue = getRealTypeValue(parentField, (String) fieldValue);
            }
            builder.withValue(nativeFieldName, fieldValue);
        }
    }

    /**
     * 修改一个已有联系人的属性.
     *
     * @param id
     *            联系人 id
     * @param contactPropMap
     *            包含需要修改联系人属性值的 map
     * @param accountType
     *            账号类型
     * @param accountName
     *            账号名称
     * @return
     */
    private String modifyContact(String id, Map<String, Object> contactPropMap,
            String accountType, String accountName) {

        // 当更新已有联系人时，一定存在 id 和 rawId
        assert (id != null);
        assert (contactPropMap.get(LOGIC_FIELD_RAWID) != null);

        // 获取联系人的 rawId 是为了在一个已有联系人中加入新值，但更新已有值不需要
        int rawId = Integer.valueOf(
                (String) contactPropMap.get(LOGIC_FIELD_RAWID)).intValue();

        // 创建一个属性列表用于添加将要存储到数据库中的值
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        // 查询根据id查询旧的联系人信息
        Cursor cursor = getContactById(contactPropMap.get(LOGIC_FIELD_ID)
                .toString());
        HashMap<String, Object> oldContactMap = new HashMap<String, Object>();
        initQuantityOfFields(oldContactMap);
        while (cursor.moveToNext()) {
            cursorToHashMap(oldContactMap, cursor);
        }
        cursor.close();

        // 为新联系人改变属性
        buildSaveContactNameOperation(contactPropMap, ops, id);

        // 通过迭代处理的 field 包括：nickname、note、birthday
        // 此处的处理顺序依赖于 mSysAllFieldTypeNameList 的排列顺序
        Iterator<String> typeNameItor = LOGIC_CONTACT_SINGLE_VALUE_FIELDS
                .iterator();
        typeNameItor.next(); // 跳过 displayName 域
        while (typeNameItor.hasNext()) {
            String fieldName = typeNameItor.next();
            modifySingleValueProperty(contactPropMap, oldContactMap, fieldName,
                    id, rawId, ops);
        }

        typeNameItor = LOGIC_CONTACT_MULTIPLE_VALUE_FIELDS.iterator();

        // 通过迭代处理的 field 包括：organizations、ims、urls、phoneNumbers、emails、
        // addresses 和 photo
        while (typeNameItor.hasNext()) {
            String fieldName = typeNameItor.next();
            modifyMultipleValueProperty(contactPropMap, oldContactMap,
                    fieldName, rawId, ops);
        }

        boolean retVal = true;

        // Modify contact
        try {
            mContext.getContentResolver().applyBatch(
                    ContactsContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            XLog.e(CLASS_NAME, e.getMessage(), e);
            retVal = false;
        } catch (OperationApplicationException e) {
            XLog.e(CLASS_NAME, e.getMessage(), e);
            retVal = false;
        }

        // 存储成功返回联系人id，否则返回null
        return retVal ? id : null;
    }

    /**
     * 为存在单值的域修改属性
     *
     * @param newContactMap
     *            包含新的要更新的联系人信息
     * @param oldContactMap
     *            android系统中保持的旧的联系人信息
     * @param fieldName
     *            域名
     * @param id
     *            此联系人的 id
     * @param rawId
     *            此联系人的 rawId
     * @param ops
     *            ContentProviderOperation
     */
    private void modifySingleValueProperty(Map<String, Object> newContactMap,
            Map<String, Object> oldContactMap, String fieldName, String id,
            int rawId, ArrayList<ContentProviderOperation> ops) {
        Object propValue = newContactMap.get(fieldName);

        ContentProviderOperation.Builder builder = null;
        String contentItemType = LOGIC_FIELD_TO_CONTENT_TYPE_MAP.get(fieldName);
        String nativeFieldName = LOGIC_FIELD_TO_NATIVE_FIELD_MAP.get(fieldName);

        Object oldPropValue = oldContactMap.get(fieldName);
        if (null == oldPropValue && null == propValue) {
            return;
        }

        if (null == oldPropValue && null != propValue) {
            // insert a new property
            builder = ContentProviderOperation
                    .newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                    .withValue(ContactsContract.Data.MIMETYPE, contentItemType);

            if (fieldName == LOGIC_FIELD_BIRTHDAY) {
                builder.withValue(Event.TYPE, Event.TYPE_BIRTHDAY);
            }
            builder.withValue(nativeFieldName, propValue);
        } else if (null != oldPropValue && null == propValue) {
            // delete property
            builder = ContentProviderOperation.newDelete(
                    ContactsContract.Data.CONTENT_URI).withSelection(
                    ContactsContract.Data.RAW_CONTACT_ID + "=? AND "
                            + ContactsContract.Data.MIMETYPE + "=?",
                    new String[] { String.valueOf(rawId), contentItemType });
        } else {
            // update an existing property
            builder = ContentProviderOperation
                    .newUpdate(ContactsContract.Data.CONTENT_URI);

            if (fieldName == LOGIC_FIELD_BIRTHDAY) {
                builder = builder.withSelection(
                        ContactsContract.Data.CONTACT_ID + "=? AND "
                                + ContactsContract.Data.MIMETYPE + "=? AND "
                                + Event.TYPE + "=?",
                        new String[] { id, contentItemType,
                                String.valueOf(Event.TYPE_BIRTHDAY) });
                builder.withValue(Event.TYPE, Event.TYPE_BIRTHDAY).withValue(
                        nativeFieldName, propValue);
            } else {
                builder = builder.withSelection(
                        ContactsContract.Data.CONTACT_ID + "=? AND "
                                + ContactsContract.Data.MIMETYPE + "=?",
                        new String[] { id, contentItemType }).withValue(
                        nativeFieldName, propValue);
            }

        }
        ops.add(builder.build());
    }

    /**
     * 为存在多值的域修改属性
     *
     * @param newContactMap
     *            包含新的要更新的联系人信息
     * @param oldContactMap
     *            android系统中保持的旧的联系人信息
     * @param logicParentField
     *            域名
     * @param rawId
     *            此联系人的 rawId
     * @param ops
     *            ContentProviderOperation
     */
    private void modifyMultipleValueProperty(Map<String, Object> newContactMap,
            Map<String, Object> oldContactMap, String logicParentField,
            int rawId, ArrayList<ContentProviderOperation> ops) {
        String quantityField = generateQuantityFieldOf(logicParentField);
        int oldQuantity = (Integer) oldContactMap.get(quantityField);
        int newQuantity = (Integer) newContactMap.get(quantityField);
        if (newQuantity <= 0 && oldQuantity <= 0) {
            return;
        }

        // fetch all old sub id of parent field (i.e. phone id, email id,
        // address id .....)
        Set<String> subFieldIdSet = new HashSet<String>();
        for (int i = 0; i < oldQuantity; i++) {
            String key = generateSubFieldKey(logicParentField, i,
                    LOGIC_FIELD_COMMON_ID);
            subFieldIdSet.add((String) oldContactMap.get(key));
        }

        String contentItemType = LOGIC_FIELD_TO_CONTENT_TYPE_MAP
                .get(logicParentField);
        // update if needed
        for (int i = 0; i < newQuantity; i++) {
            String key = generateSubFieldKey(logicParentField, i,
                    LOGIC_FIELD_COMMON_ID);
            String idValue = (String) newContactMap.get(key);
            ContentProviderOperation.Builder builder = null;

            key = generateSubFieldKey(logicParentField, i,
                    LOGIC_FIELD_COMMON_VALUE);
            Object commonValue = newContactMap.get(key);
            if (isOnlyCommonSubFieldsIncluded(logicParentField)
                    && null == commonValue) {
                continue;
            }

            subFieldIdSet.remove(idValue);

            // insert a new property
            if (idValue == null) {
                builder = ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                contentItemType);
            }
            // update an existing property
            else {
                builder = ContentProviderOperation.newUpdate(
                        ContactsContract.Data.CONTENT_URI).withSelection(
                        "_id=? AND " + ContactsContract.Data.MIMETYPE + "=?",
                        new String[] { idValue, contentItemType });
            }

            if (logicParentField == LOGIC_FIELD_PHOTOS) {
                byte[] bytes = (byte[]) commonValue;
                builder.withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                        .withValue(Photo.PHOTO, bytes);
            } else {
                setSubFieldValueToBuilder(builder, newContactMap,
                        logicParentField, i);
            }

            ops.add(builder.build());
        }
        // delete the other sub fields
        for (String idValue : subFieldIdSet) {
            // 删除原来的数据
            ops.add(ContentProviderOperation
                    .newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                            "_id=? AND " + ContactsContract.Data.MIMETYPE
                                    + "=?",
                            new String[] { "" + idValue, contentItemType })
                    .build());
        }
    }

    /**
     * 构建新建/更新联系人的name属性值（包括displayName）的operation，如果id为null，则将name属性写系统通信录中.
     *
     * @param contactPropMap
     *            包含联系人信息的 map
     * @param ops
     *            属性列表
     * @param id
     *            联系人id
     */
    private void buildSaveContactNameOperation(
            Map<String, Object> contactPropMap,
            ArrayList<ContentProviderOperation> ops, String id) {
        boolean hasValidFieldValue = false; // 是否存在有效的属性值

        ContentProviderOperation.Builder builder = null;
        if (null == id) {
            builder = ContentProviderOperation
                    .newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(
                            ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            StructuredName.CONTENT_ITEM_TYPE);
        } else {
            builder = ContentProviderOperation.newUpdate(
                    ContactsContract.Data.CONTENT_URI).withSelection(
                    ContactsContract.Data.CONTACT_ID + "=? AND "
                            + ContactsContract.Data.MIMETYPE + "=?",
                    new String[] { id, StructuredName.CONTENT_ITEM_TYPE });
        }

        String displayName = (String) contactPropMap
                .get(LOGIC_FIELD_DISPLAYNAME);
        if (displayName != null) {
            builder.withValue(StructuredName.DISPLAY_NAME, displayName);
            hasValidFieldValue = true;
        }

        Iterator<Entry<String, String>> entries = CONTACT_NAME_FIELD_MAP
                .entrySet().iterator();
        while (entries.hasNext()) {
            Entry<String, String> nameEntry = entries.next();
            String logicNameField = nameEntry.getKey();
            String nativeNameField = nameEntry.getValue();
            String key = generateSubFieldKey(LOGIC_FIELD_NAME, 0,
                    logicNameField);
            Object fieldValue = contactPropMap.get(key);
            // if (null != fieldValue) {
            builder.withValue(nativeNameField, fieldValue);
            hasValidFieldValue = true;
            // }
        }

        if (hasValidFieldValue) {
            ops.add(builder.build());
        }
    }

    /**
     * 通过传入的联系人父属性域（js表示，如），获取其子属性的js表示名称到android系统表示名称的映射
     */
    private Map<String, String> getContactSubFieldsMapOf(String logicParentField) {
        if (LOGIC_FIELD_PHONENUMBERS.equals(logicParentField)) {
            return CONTACT_PHONE_NUMBER_FIELD_MAP;
        } else if (LOGIC_FIELD_EMAILS.equals(logicParentField)) {
            return CONTACT_EMAIL_FIELD_MAP;
        } else if (LOGIC_FIELD_ADDRESSES.equals(logicParentField)) {
            return CONTACT_ADDRESS_FIELD_MAP;
        } else if (LOGIC_FIELD_ORGANIZATIONS.equals(logicParentField)) {
            return CONTACT_ORGANIZATION_FIELD_MAP;
        } else if (LOGIC_FIELD_IMS.equals(logicParentField)) {
            return CONTACT_IM_FIELD_MAP;
        } else if (LOGIC_FIELD_URLS.equals(logicParentField)) {
            return CONTACT_URL_FIELD_MAP;
        } else {
            return null;
        }
    }
}
