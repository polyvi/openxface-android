
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.database.Cursor;

/**
 * 这个接口定义了与 Contacts Provider 通信时依赖于 SDK Level 的 API。 选择的具体实现代码取决于设备上的API Level。
 * 现在不支持 Android 1.x 设备。如果是 Android 2.x 设备, 应该使用 XContactAccessorAPILevel5Impl.
 */

public abstract class XContactAccessor {

    // 在js中使用的联系人所属域名称
    public static final String LOGIC_FIELD_ID = "id";
    public static final String LOGIC_FIELD_RAWID = "rawId";
    public static final String LOGIC_FIELD_DISPLAYNAME = "displayName";
    public static final String LOGIC_FIELD_NAME = "name";
    public static final String LOGIC_FIELD_NICKNAME = "nickname";
    public static final String LOGIC_FIELD_PHONENUMBERS = "phoneNumbers";
    public static final String LOGIC_FIELD_EMAILS = "emails";
    public static final String LOGIC_FIELD_ADDRESSES = "addresses";
    public static final String LOGIC_FIELD_IMS = "ims";
    public static final String LOGIC_FIELD_ORGANIZATIONS = "organizations";
    public static final String LOGIC_FIELD_BIRTHDAY = "birthday";
    public static final String LOGIC_FIELD_NOTE = "note";
    public static final String LOGIC_FIELD_URLS = "urls";
    public static final String LOGIC_FIELD_PHOTOS = "photos";

    // 每个属性域所包含的子属性名
    // ContactName
    public static final String LOGIC_FIELD_NAME_FORMATTED = "formatted";
    public static final String LOGIC_FIELD_NAME_FAMILYNAME = "familyName";
    public static final String LOGIC_FIELD_NAME_GIVENNAME = "givenName";
    public static final String LOGIC_FIELD_NAME_MIDDLENAME = "middleName";
    public static final String LOGIC_FIELD_NAME_PREFIX = "honorificPrefix";
    public static final String LOGIC_FIELD_NAME_SUFFIX = "honorificSuffix";

    // ContactAddress
    public static final String LOGIC_FIELD_ADDRESS_FORMATTED = "formatted";
    public static final String LOGIC_FIELD_ADDRESS_STREETADDRESS = "streetAddress";
    public static final String LOGIC_FIELD_ADDRESS_LOCALITY = "locality";
    public static final String LOGIC_FIELD_ADDRESS_REGION = "region";
    public static final String LOGIC_FIELD_ADDRESS_POSTALCODE = "postalCode";
    public static final String LOGIC_FIELD_ADDRESS_COUNTRY = "country";

    // ContactField（基础子属性名）
    public static final String LOGIC_FIELD_COMMON_ID = "id";
    public static final String LOGIC_FIELD_COMMON_TYPE = "type";
    public static final String LOGIC_FIELD_COMMON_VALUE = "value";
    public static final String LOGIC_FIELD_COMMON_PREF = "pref";

    // ContactOrganization
    public static final String LOGIC_FIELD_ORGANIZATION_NAME = "name";
    public static final String LOGIC_FIELD_ORGANIZATION_DEPARTMENT = "department";
    public static final String LOGIC_FIELD_ORGANIZATION_TITLE = "title";

    // ContactFindOptions
    public static final String FIND_OPTION_NAME_FILTER = "filter";
    public static final String FIND_OPTION_NAME_MULTIPLE = "multiple";
    public static final String FIND_OPTION_NAME_ACCOUNT_TYPE = "accountType";

    // Contact account type
    public static final String CONTACT_ACCOUNT_TYPE_ALL = "All";
    public static final String CONTACT_ACCOUNT_TYPE_PHONE = "Phone";
    public static final String CONTACT_ACCOUNT_TYPE_SIM = "SIM";

    /** 匹配 Email 的正则表达式（如：<anything>@<anything>.<anything>） */
    protected static final String EMAIL_REGEXP = ".+@.+\\.+.+";

    private static final String FIELD_QUANTITY = "quantity";
    private static final String SEPERATOR = "_";

    /** 只包含单个属性值的联系人属性名称集合（js中表示的名称），包含displayname、nickname、note和birthday */
    public static final List<String> LOGIC_CONTACT_SINGLE_VALUE_FIELDS = new ArrayList<String>();
    static {
        LOGIC_CONTACT_SINGLE_VALUE_FIELDS.add(LOGIC_FIELD_DISPLAYNAME);
        LOGIC_CONTACT_SINGLE_VALUE_FIELDS.add(LOGIC_FIELD_NICKNAME);
        LOGIC_CONTACT_SINGLE_VALUE_FIELDS.add(LOGIC_FIELD_NOTE);
        LOGIC_CONTACT_SINGLE_VALUE_FIELDS.add(LOGIC_FIELD_BIRTHDAY);
    }

    /**
     * 包含多个子属性值的联系人属性名称集合（js中表示的名称），包含address、organization、phonenumbers、emails、
     * IMs、URLs、photos
     */
    public static final List<String> LOGIC_CONTACT_MULTIPLE_VALUE_FIELDS = new ArrayList<String>();
    static {
        LOGIC_CONTACT_MULTIPLE_VALUE_FIELDS.add(LOGIC_FIELD_ADDRESSES);
        LOGIC_CONTACT_MULTIPLE_VALUE_FIELDS.add(LOGIC_FIELD_ORGANIZATIONS);
        LOGIC_CONTACT_MULTIPLE_VALUE_FIELDS.add(LOGIC_FIELD_PHONENUMBERS);
        LOGIC_CONTACT_MULTIPLE_VALUE_FIELDS.add(LOGIC_FIELD_EMAILS);
        LOGIC_CONTACT_MULTIPLE_VALUE_FIELDS.add(LOGIC_FIELD_IMS);
        LOGIC_CONTACT_MULTIPLE_VALUE_FIELDS.add(LOGIC_FIELD_URLS);
        LOGIC_CONTACT_MULTIPLE_VALUE_FIELDS.add(LOGIC_FIELD_PHOTOS);
    }

    /** 在js中表示的联系人的属性集合（不包含子属性） */
    public static final List<String> LOGIC_CONTACT_FIELDS = new ArrayList<String>();
    static {
        LOGIC_CONTACT_FIELDS.add(LOGIC_FIELD_ID);
        LOGIC_CONTACT_FIELDS.add(LOGIC_FIELD_NAME);

        LOGIC_CONTACT_FIELDS.addAll(LOGIC_CONTACT_SINGLE_VALUE_FIELDS);
        LOGIC_CONTACT_FIELDS.addAll(LOGIC_CONTACT_MULTIPLE_VALUE_FIELDS);
    }

    /** ContactName在js中表示时的属性名称列表 */
    public static final List<String> LOGIC_CONTACT_NAME_FIELDS = new ArrayList<String>();
    static {
        LOGIC_CONTACT_NAME_FIELDS.add(LOGIC_FIELD_NAME_FAMILYNAME);
        LOGIC_CONTACT_NAME_FIELDS.add(LOGIC_FIELD_NAME_MIDDLENAME);
        LOGIC_CONTACT_NAME_FIELDS.add(LOGIC_FIELD_NAME_GIVENNAME);
        LOGIC_CONTACT_NAME_FIELDS.add(LOGIC_FIELD_NAME_PREFIX);
        LOGIC_CONTACT_NAME_FIELDS.add(LOGIC_FIELD_NAME_SUFFIX);
    }

    /** ContactAddress在js中表示时的属性名称列表 */
    public static final List<String> LOGIC_CONTACT_ADDRESS_FIELDS = new ArrayList<String>();
    static {
        LOGIC_CONTACT_ADDRESS_FIELDS.add(LOGIC_FIELD_COMMON_TYPE);
        LOGIC_CONTACT_ADDRESS_FIELDS.add(LOGIC_FIELD_ADDRESS_FORMATTED);
        LOGIC_CONTACT_ADDRESS_FIELDS.add(LOGIC_FIELD_ADDRESS_STREETADDRESS);
        LOGIC_CONTACT_ADDRESS_FIELDS.add(LOGIC_FIELD_ADDRESS_LOCALITY);
        LOGIC_CONTACT_ADDRESS_FIELDS.add(LOGIC_FIELD_ADDRESS_REGION);
        LOGIC_CONTACT_ADDRESS_FIELDS.add(LOGIC_FIELD_ADDRESS_POSTALCODE);
        LOGIC_CONTACT_ADDRESS_FIELDS.add(LOGIC_FIELD_ADDRESS_COUNTRY);

        // 以下两个属性为生成新联系人准备
        LOGIC_CONTACT_ADDRESS_FIELDS.add(LOGIC_FIELD_COMMON_ID);
        LOGIC_CONTACT_ADDRESS_FIELDS.add(LOGIC_FIELD_COMMON_PREF);
    }

    /** ContactOrganization在js中表示时的属性名称列表 */
    public static final List<String> LOGIC_CONTACT_ORGANIZATION_FIELDS = new ArrayList<String>();
    static {
        LOGIC_CONTACT_ORGANIZATION_FIELDS.add(LOGIC_FIELD_COMMON_TYPE);
        LOGIC_CONTACT_ORGANIZATION_FIELDS
                .add(LOGIC_FIELD_ORGANIZATION_DEPARTMENT);
        LOGIC_CONTACT_ORGANIZATION_FIELDS.add(LOGIC_FIELD_ORGANIZATION_NAME);
        LOGIC_CONTACT_ORGANIZATION_FIELDS.add(LOGIC_FIELD_ORGANIZATION_TITLE);

        // 以下两个属性为生成新联系人准备
        LOGIC_CONTACT_ORGANIZATION_FIELDS.add(LOGIC_FIELD_COMMON_ID);
        LOGIC_CONTACT_ORGANIZATION_FIELDS.add(LOGIC_FIELD_COMMON_PREF);
    }

    /** 公共contact子属性在js中表示时的属性名称列表 */
    public static final List<String> LOGIC_CONTACT_COMMON_SUB_FIELDS = new ArrayList<String>();
    static {
        // 此处排列顺序很重要，不要轻易改变
        LOGIC_CONTACT_COMMON_SUB_FIELDS.add(LOGIC_FIELD_COMMON_VALUE);
        LOGIC_CONTACT_COMMON_SUB_FIELDS.add(LOGIC_FIELD_COMMON_TYPE);
        LOGIC_CONTACT_COMMON_SUB_FIELDS.add(LOGIC_FIELD_COMMON_ID);
        LOGIC_CONTACT_COMMON_SUB_FIELDS.add(LOGIC_FIELD_COMMON_PREF);
    }

    /**
     * 将cursor当前行对应的联系人数据保存到hash表中.
     *
     * @param contactMap
     *            用于存储单个联系人所有属性的 Map
     * @param c
     *            游标
     */
    public abstract void cursorToHashMap(Map<String, Object> contactMap,
            Cursor c);

    /**
     * 根据传入的匹配条件及要匹配的属性集，获得符合匹配条件的contact项
     *
     * @param logicFieldSet
     *            要进行匹配的联系人属性的集合
     * @param searchExpression
     *            查询的匹配条件
     * @param willMatchAllField
     *            是否需要匹配所有的属性
     * @param accountType
     *            联系人的帐户类型（如：Phone, SIM, All）
     * @return 游标
     */
    public abstract Cursor getMatchedContactsCursor(Set<String> logicFieldSet,
            String searchExpression, boolean willMatchAllField,
            String accountType);

    /**
     * 通过游标获得联系人 id.
     *
     * @param c
     *            游标
     * @return 联系人 id
     */
    public abstract String getContactId(Cursor c);

    /**
     * 通过游标获得联系人 rawid.
     *
     * @param c
     *            游标
     * @return 联系人 rawid
     */
    public abstract String getRawId(Cursor c);

    /**
     * 通过指定的 id 删除联系人.
     *
     * @param contactId
     *            需要删除的联系人 id
     * @return 成功返回 true，失败返回false
     */
    public abstract boolean removeContact(String contactId);

    /**
     * 将 JS 传入的联系人数据存储到设备中联系人的数据库中. 如果无此联系人，则创建新联系人； 如果存在此联系人，则修改传入数据中此联系人对应的属性值
     *
     * @param contactPropMap
     *            JS 传入的联系人数据
     * @return 存储成功返回此联系人 id，失败返回 null
     */
    public abstract String saveContact(Map<String, Object> contactPropMap);

    /**
     * 通过联系人 id 获得包含此联系人信息的游标.
     *
     * @param contactId
     *            需要获得联系人信息的 id
     * @return 包含联系人信息的游标
     */
    public abstract Cursor getContactById(String contactId);

    /**
     * 获取在js中表示的某个联系人属性的子属性集
     *
     * @param fieldName
     *            联系人的任一属性名
     * @return 指定field的子属性集
     */
    public List<String> getLogicContactSubFieldsOf(String fieldName) {
        if (isOnlyCommonSubFieldsIncluded(fieldName)) {
            return LOGIC_CONTACT_COMMON_SUB_FIELDS;
        } else if (LOGIC_FIELD_NAME.equals(fieldName)) {
            return LOGIC_CONTACT_NAME_FIELDS;
        } else if (LOGIC_FIELD_ADDRESSES.equals(fieldName)) {
            return LOGIC_CONTACT_ADDRESS_FIELDS;
        } else if (LOGIC_FIELD_ORGANIZATIONS.equals(fieldName)) {
            return LOGIC_CONTACT_ORGANIZATION_FIELDS;
        }
        return null;
    }

    /**
     * 判断传入的属性域是否仅包含普通的子属性，即LOGIC_CONTACT_COMMON_SUB_FIELDS中定义的属性集
     *
     * @param fieldName
     *            父属性名称（js表示时的属性名称）
     * */
    protected boolean isOnlyCommonSubFieldsIncluded(String fieldName) {
        return LOGIC_FIELD_PHONENUMBERS.equals(fieldName)
                || LOGIC_FIELD_EMAILS.equals(fieldName)
                || LOGIC_FIELD_IMS.equals(fieldName)
                || LOGIC_FIELD_URLS.equals(fieldName)
                || LOGIC_FIELD_PHOTOS.equals(fieldName);
    }

    /** 根据传入的父属性名称，生成记录该父属性的children个数的属性名称 */
    public String generateQuantityFieldOf(String logicParentField) {
        return logicParentField + SEPERATOR + FIELD_QUANTITY;
    }

    /**
     * 根据父属性名称、子属性名称及该子属性所在集合在父属性children中的索引生成该子属性在map中保存使用的key<br/>
     * {parent1:{child1:{field1, field2,...}, child2:{field1, field2,...}, ...},
     * parent2:{...}}
     *
     * @param logicParentField
     *            父属性名
     * @param childIndex
     *            子属性所在孩子的索引（从0开始）
     * @param logicChildField
     *            子属性名
     * @return
     */
    public String generateSubFieldKey(String logicParentField, int childIndex,
            String logicChildField) {
        return logicParentField + childIndex + SEPERATOR + logicChildField;
    }

    /**
     * 将联系人信息hash表中存在多值的属性值个数初始化为0.
     *
     * @param contactPropMap
     *            包含联系人信息的 map
     */
    public void initQuantityOfFields(Map<String, Object> contactPropMap) {
        Iterator<String> logicParentFieldItor = XContactAccessor.LOGIC_CONTACT_MULTIPLE_VALUE_FIELDS
                .iterator();
        while (logicParentFieldItor.hasNext()) {
            String quantityName = generateQuantityFieldOf(logicParentFieldItor
                    .next());
            contactPropMap.put(quantityName, 0);
        }
    }
}

/**
 * 双向hash表<br/>
 * 既能通过key查找value，也能通过value查找key，要求key与value必须一一对应
 */
final class XBiMap<K, V> extends HashMap<K, V> {
    /** auto generated */
    private static final long serialVersionUID = 724755341262540357L;

    private Map<V, K> valueToKey;

    public XBiMap() {
        super();
        valueToKey = new HashMap<V, K>();
    }

    @Override
    public V put(K key, V value) {
        valueToKey.put(value, key);
        return super.put(key, value);
    }

    public K getKeyByValue(V value) {
        return valueToKey.get(value);
    }
}
