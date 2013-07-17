package com.polyvi.xface.extension;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import com.polyvi.xface.plugin.api.XIWebContext;
import com.polyvi.xface.util.XLog;

/**
 * js回调类，该类是用于对js回调进行管理和信息记录，包括js脚本的构建和将扩展发送至主线程队列中
 * 一个JsCallback object对应一条js-native语句
 * */
public class XJsCallback {

    private static final String CLASS_NAME = XJsCallback.class.getSimpleName();

    private String mId;/**< 用于标识每一个回调的id*/

    private String mJsCallback;/**< js传下来的回调id*/

    // 一条扩展命令可能会产生多次回调，如ams install会上报多次progress event，由于所有过程都是异步的，
    // 可能前面的还没有被执行，后面会覆盖前面的script，所以用队列保存
    private Queue<String> mJsScriptQueue;/**< native生成的js脚本串的队列*/

    public XJsCallback(String jsCallback) {
        // 根据XUtils生成的随机数作为标识每一个回调的id
        mId = generateRandomId() + "";
        mJsCallback = jsCallback;
        mJsScriptQueue = new LinkedList<String>();
    }

    public String getId() {
        return mId;
    }

    public synchronized void setJsScript(String jsScript){
        if(!mJsScriptQueue.offer(jsScript)){
            XLog.e(CLASS_NAME, "setJsScript error");
        }
    }

    /**
     * 获取并删除队头的js语句
     *
     * @return  如果队列不为空，返回队头的js语句，否则为空。
     */
    public synchronized String pollJsScript(){
        StringBuffer jsStr = new StringBuffer();
        while( !mJsScriptQueue.isEmpty() )
        {
            jsStr.append( mJsScriptQueue.poll());
        }
        return jsStr.toString();
    }

    /**
     * 根据执行扩展的结果的状态构建js脚本
     *
     * @param extensionResult
     *            执行扩展的结果对象
     * */
    public void genJsScript(XExtensionResult extensionResult) {
        //如果扩展执行结果extensionResult为空，表示该扩展不需要构建js脚本
        //如果状态为OK或者PROGRESS_CHANGING分别回调各自函数,其他状态均回调ErrorCallback函数
        int status = extensionResult.getStatus();
        XExtensionResult.Status DateTypeValue = XExtensionResult.Status.values()[status];
        switch(DateTypeValue)
        {
            case OK:
                setJsScript(extensionResult.toSuccessCallbackString(mJsCallback));
                break;
            case PROGRESS_CHANGING:
                setJsScript(extensionResult.toStatusChangeCallbackString(mJsCallback));
                break;
            case NO_RESULT://this status will not call JsCallback
                break;
            case CLASS_NOT_FOUND_EXCEPTION:
            case ILLEGAL_ACCESS_EXCEPTION:
            case INSTANTIATION_EXCEPTION:
            case MALFORMED_URL_EXCEPTION:
            case IO_EXCEPTION:
            case INVALID_ACTION:
            case JSON_EXCEPTION:
            case ERROR:
                setJsScript(extensionResult.toErrorCallbackString(mJsCallback));
                break;
            default://default may be never get into
                XLog.d("XJsCallback", "Method genJsScript default status shoud not get into!");
                assert false;
                break;
        }
    }

    /**
     * 回调是否有效，在eval扩展的js脚本之前，需要对回调的有效性进行验证，有效地回调才能被执行
     *
     * @param app
     *            xFace的app对象
     * @return 该回调是否有效 true -回调有效 false -回调无效
     * */
    public boolean isValid(XIWebContext app) {
        return false;
    }

    /**
     *  生成一个随机数 作为callback的id
     * @return
     */
    private static int generateRandomId() {
        Random r = new Random();
        return r.nextInt();
    }
}
