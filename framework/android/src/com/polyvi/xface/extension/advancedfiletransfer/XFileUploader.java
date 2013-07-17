
/*
 Copyright 2012-2013, Polyvi Inc. (http://polyvi.github.io/openxface)
 This program is distributed under the terms of the GNU General Public License.

 This file is part of xFace.

 xFace is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 xFace is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with xFace.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.polyvi.xface.extension.advancedfiletransfer;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;

import com.polyvi.xface.extension.XCallbackContext;
import com.polyvi.xface.extension.XExtensionContext;
import com.polyvi.xface.extension.XExtensionResult;
import com.polyvi.xface.extension.XExtensionResult.Status;
import com.polyvi.xface.plugin.api.XIWebContext;
import com.polyvi.xface.util.XLog;
import com.polyvi.xface.util.XPathResolver;
import com.polyvi.xface.util.XStringUtils;

//上传请求发起后，组装头部信息发给服务器（数据格式为：Content-Length=?;filename=?;sourceid= ?如果用户初次上传文件，sourceid的值为空).
// 服务器根据头部信息判断要上传的文件是否有上传记录，然后返回相应的信息(格式为： sourceid=?;position=?),从而根据position实现断点上传。
public class XFileUploader implements XIFileTransfer, XIFileTransferListener {

    private static final String CLASS_NAME = XFileUploader.class.getSimpleName();

    /**握手阶段的头文字名*/
    private static final String ACTION_NAME_HAND = "HAND";

    /**上传阶段的头文字名*/
    private static final String ACTION_NAME_UPLOAD = "UPLOAD";

    /** http头 */
    private static final String HTTP_HEAD = "http://";

    /** 连接超时时间 */
    private static final int mTimeout = 5000;

    /**读取数据的*/

    private static final int FILE_NOT_FOUND_ERR = 1;
    private static final int INVALID_URL_ERR = 2;
    private static final int CONNECTION_ERR = 3;

    /** 定义三种上传的状态：初始化状态，正在上传状态，暂停状态 */
    private static final int INIT = 1;
    private static final int UPLOADING = 2;
    private static final int PAUSE = 3;
    private int mState = INIT;

    /**定义上传文件的划分倍数和单位文件大小*/
    private static final int DIVIDE_SIZE_TWO   = 2;
    private static final int DIVIDE_SIZE_TEN    = 10;
    private static final int DIVIDE_SIZE_TWENTY = 20;
    private static final int SIZE_KB = 1024;

    /**标示读取文件结束*/
    private static final int READ_FILE_END = -1;

    /**标示返回值错误*/
    private static final int RESULT_CODE_ERROR = -1;

    /**标示服务器返回的结果码,1表示服务器成功接收到文件一个分块，0表示服务器成功接收到整个文件*/
    private static final int RESULT_CODE_CHUNK_RECEIVED = 1;
    private static final int RESULT_CODE_FILE_RECEIVED = 0;

    /**要上传文件的大小*/
    private long mUploadFileSize;

    /**每次上传文件块的大小*/
    private int mUploadFileSizePerTime;

    /** 要上传的文件路径 */
    private String mFilePath;

    /**要上传的文件*/
    private File mUploadFile;

    /**要上传文件的服务器响应id*/
    private String mResponseId;

    /**要上传文件的开始位置*/
    private int mStartedPosition;

    /**要上传文件的已经上传的大小*/
    private int mAlreadyUploadLength;

    /** 服务器地址 */
    private String mServer;

    /** native端js回调的上下文环境 */
    private XCallbackContext mCallbackCtx;

    /** 当前的应用 */
    private XIWebContext mWebContext;

    /** 操作配置文件的对象 */
    private XFileTransferRecorder mFileTransferRecorder;

    /** 上传管理器 */
    private XFileTransferManager mFileTransferManager;

    public XFileUploader(String filePath, String server,
            XExtensionContext extensionContext, XIWebContext webContext,
            XFileTransferRecorder recorder, XFileTransferManager manager) {
        init(filePath, server, extensionContext, webContext, recorder, manager);
    }

    /** 初始化方法 */
    private void init(String filePath, String server,
            XExtensionContext extensionContext, XIWebContext webContext,
            XFileTransferRecorder recorder, XFileTransferManager manager) {
        mFilePath = filePath;
        mServer = server;
        mWebContext = webContext;
        mFileTransferRecorder = recorder;
        mFileTransferManager = manager;
    }

    @Override
    public void onSuccess() {
        mFileTransferRecorder.deleteUploadInfo(mFilePath);
        setState(INIT);
        mFileTransferManager.removeFileTranferTask(mWebContext.getApplication().getAppId(), mFilePath);
        mCallbackCtx.success();
    }

    @Override
    public void onError(int errorCode) {
        setState(INIT);
        JSONObject error = new JSONObject();
        Status status = Status.ERROR;
        try {
            error.put("code", errorCode);
            error.put("source", mFilePath);
            error.put("target", mServer);
        } catch (JSONException e) {
            status = Status.JSON_EXCEPTION;
            XLog.e(CLASS_NAME, e.getMessage());
        }
        XExtensionResult result = new XExtensionResult(status, error);
        mCallbackCtx.sendExtensionResult(result);
    }

    // TODO:下面代码以后会调整
    @Override
    public void onProgressUpdated(int completeSize, long totalSize) {
        JSONObject jsonObj = new JSONObject();
        Status status = Status.PROGRESS_CHANGING;
        try {
            jsonObj.put("loaded", completeSize);
            jsonObj.put("total", totalSize);
        } catch (JSONException e) {
            status = Status.JSON_EXCEPTION;
            XLog.e(CLASS_NAME, e.getMessage());
        }
        XExtensionResult result = new XExtensionResult(status, jsonObj);
        result.setKeepCallback(true);
        mCallbackCtx.sendExtensionResult(result);
    }

	/** 上传线程执行的上传函数 */
	private void upload() {
		// 检查路径和URL错误
		if (!initUploadFileInfo()) {
			return;
		}
		byte[] buffer = new byte[mUploadFileSizePerTime];
		//分多次连接是因为一次向流写入太多数据会导致程序崩溃
		while ((mAlreadyUploadLength < mUploadFileSize)) {
			//握手过程
			if (!handleShake()) {
				break;
			}
			// 从文件中读取数据
			int len = readFileData(buffer);
			//上传文件
			if (!uploadData(buffer,len)) {
				break;
			}
		}
		if (mState != PAUSE && mAlreadyUploadLength != mUploadFileSize) {
			onError(CONNECTION_ERR);
		}
	}

	/**
	 * 握手过程，交换头文字信息，并从服务器获取资源id和上传的开始位置
	 *
	 * @return true:握手过程成功，false:握手过程失败。
	 */
	private boolean handleShake() {
		HttpURLConnection httpConnection = null;
		DataInputStream dataInputStream = null;
		String souceid = mFileTransferRecorder.getSourceId(mFilePath,"" + mUploadFileSize);
		try {
			httpConnection = getHttpConnection(mServer);
			// 设置握手阶段的头文字
			httpConnection.setRequestProperty("Charset", "UTF-8");
			httpConnection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			httpConnection.setRequestProperty("ACTIONNAME", ACTION_NAME_HAND);
			httpConnection.setRequestProperty("RESOURCEID", souceid);
			httpConnection.setRequestProperty("FILENAME", getUploadFileName());
			httpConnection.setRequestProperty("FILESIZE", "" + mUploadFileSize);
			if(HttpURLConnection.HTTP_OK == httpConnection.getResponseCode()){
				// 获取服务器返回过来的信息，格式为： RESOURCEID=?;BFFORE=?
				dataInputStream = new DataInputStream(
						httpConnection.getInputStream());
				// 处理服务器传过来的response信息
				handleResponse(dataInputStream.readLine());
				// 如果souceid为空则表示服务器上没有存在该上传，需设置。
				setSourceId(souceid);
			} else {
				onError(INVALID_URL_ERR);
			}
		} catch (Exception e) {
			onError(INVALID_URL_ERR);
			e.printStackTrace();
			return false;
		} finally {
			if (null != httpConnection) {
				httpConnection.disconnect();
				httpConnection = null;
			}
			// 握手过程结束，关闭连接。
			try {
				if(null != dataInputStream) {
					dataInputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	/**
	 * 文件上传过程
	 *
	 * @return true：本次上传操作成功,false：本次上传操作失败。
	 */
	private boolean uploadData(byte[] buffer, int len) {
		HttpURLConnection httpConnection = null;
		InputStream inStream = null;
		try {
			// 设置上传阶段的头文字
			httpConnection = getHttpConnection(mServer);
			httpConnection.setRequestProperty("Charset", "UTF-8");
			httpConnection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			httpConnection.setRequestProperty("ACTIONNAME", ACTION_NAME_UPLOAD);
			httpConnection.setRequestProperty("RESOURCEID", mResponseId);
			httpConnection.setRequestProperty("BEFORE", "" + mStartedPosition);
			// 向服务器写入buffer中数据
			writeBufferData(httpConnection.getOutputStream(), buffer, len);
			// 获取从服务器传来的结果码
			int resultCode = getResultCode(httpConnection);
			doProcessUpdate(resultCode, len);
			if ((RESULT_CODE_FILE_RECEIVED == resultCode)
					&& (mAlreadyUploadLength == mUploadFileSize)) {
				onSuccess();
			}
			if (mState == PAUSE) {
				return false;
			}
		} catch (Exception e) {
			onError(CONNECTION_ERR);
			XLog.e(CLASS_NAME, "connection error");
			return false;
		} finally {
			if (null != inStream) {
				try {
					inStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (null != httpConnection) {
				httpConnection.disconnect();
				httpConnection = null;
			}
		}
		return true;
	}

	/**
	 * 初始化上传文件信息
	 *
	 * @return true:初始化上传文件信息成功,false：始化上传文件信息失败。
	 * */
	private boolean initUploadFileInfo() {
		// 检查路径和URL错误
		if (null == (mUploadFile = getFile())) {
			onError(FILE_NOT_FOUND_ERR);
			return false;
		}
		if (!mServer.startsWith(HTTP_HEAD)) {
			onError(INVALID_URL_ERR);
			return false;
		}
		mUploadFileSize = getUploadFileSize();
		mUploadFileSizePerTime = getSingleUploadLength();
		return true;
	}

	/**
	 * 处理服务器的返回的头文件信息
	 *
	 * @param response
	 *            ：待处理的response
	 * @return true:response结果正确，false:response结果错误。
	 * */
	private void handleResponse(String response) throws Exception {
		if (XStringUtils.isEmptyString(response)) {
			throw new Exception("response is null");
		}
		String items[] = response.split(";");
		int first_index = items[0].indexOf(":");
		int second_index = items[1].indexOf(":");
		if ((RESULT_CODE_ERROR == first_index) || (RESULT_CODE_ERROR == second_index)) {
			throw new Exception("response error");
		}
		mResponseId = items[0].substring(items[0].indexOf(":") + 1);
		mStartedPosition = Integer.parseInt(items[1].substring(items[1].indexOf(":") + 1));
		if(mStartedPosition > mUploadFileSize)
		{
			throw new Exception("file StartedPosition is bigger than  fileSize error");
		}
	}

	/**
	 * 如果souceid为空则表示服务器上没有存在该上传记录
	 *
	 * @param souceid
	 *            ：获取到得sourceid
	 */
	private void setSourceId(String souceid) {
		if (null == souceid) {
			mFileTransferRecorder.saveUploadInfo(mResponseId, mFilePath ,"" + mUploadFileSize);
		}

	}

	/**
	 * 读取buffer数据,并返回读取的大小
	 *
	 * @param buffer
	 *            :读取数据的缓冲区
	 */
	private int readFileData(byte[] buffer) {
		int len = READ_FILE_END;
		RandomAccessFile accessFile = null;
		try {
			accessFile = new RandomAccessFile(mUploadFile, "r");
			accessFile.seek(mStartedPosition);
			len = accessFile.read(buffer);
			if (mStartedPosition != mAlreadyUploadLength) {
				mAlreadyUploadLength = mStartedPosition;
			}
			accessFile.close();
		} catch (FileNotFoundException e) {
			len = READ_FILE_END;
			onError(FILE_NOT_FOUND_ERR);
		} catch (IOException e) {
			len = READ_FILE_END;
			onError(FILE_NOT_FOUND_ERR);
		}
		return len;
	}

	/**
	 * 将指定buff的数据写入到目标输出流
	 *
	 * @param outStream
	 *            ：输入流
	 * @param buffer
	 *            :要写入的数据
	 * @param len
	 *            :要写入数据的长度
	 * @return 实际写入数据大小
	 */
	private int writeBufferData(OutputStream outStream, byte[] buffer, int len)
			throws IOException {
		outStream.write(buffer, 0, len);
		outStream.flush();
		outStream.close();
		return len;
	}

	/**
	 * 获取从服务端传来的结果码
	 *
	 * @param httpConnection
	 *            :http连接
	 * @return 1：成功接收到文件分块。 0：成功接收到整个文件。 -1：获取结果码出现错误。
	 */
	private int getResultCode(HttpURLConnection httpConnection) {
		DataInputStream dataInputStream = null;
		try {
			if (HttpURLConnection.HTTP_OK != httpConnection.getResponseCode()) {
				return RESULT_CODE_ERROR;
			}
			// 服务端如果接收成功，会返回RETURN_CODE:1
			dataInputStream = new DataInputStream(
					httpConnection.getInputStream());
			// 处理服务器传过来的response信息
			String data = dataInputStream.readLine();
			int resultCode = Integer
					.valueOf(data.substring(data.indexOf(":") + 1));
			return (resultCode == RESULT_CODE_CHUNK_RECEIVED
					|| resultCode == RESULT_CODE_FILE_RECEIVED) ? resultCode
					: RESULT_CODE_ERROR;
		} catch (Exception e) {
			e.printStackTrace();
			return RESULT_CODE_ERROR;
		} finally {
			try {
				if (null != dataInputStream) {
					dataInputStream.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 修改进度
	 *
	 * @param len
	 *            :本次上传的文件大小
	 */
	private void doProcessUpdate(int resultCode,int len) {
		if(RESULT_CODE_ERROR != resultCode){
			mAlreadyUploadLength += len;
			XLog.d("xface", "" + mAlreadyUploadLength);
			onProgressUpdated(mAlreadyUploadLength, mUploadFileSize);
		}
	}

	/** 获取要上传文件的大小 */
	private long getUploadFileSize() {
		return mUploadFile.length();
	}

	/** 获取要上传文件的名字 */
	private String getUploadFileName() {
		return mUploadFile.getName();
	}

	/**
	 * 把文件分成几份上传有2个原因：<br/>
	 * 1.一次性向流写入大量数据会导致程序崩溃。<br/>
	 * 2.分成几份上传会使进度条更新更平滑。<br/>
	 * 获取每次要上传文件块的大小 。<br/>
	 * 如果文件大小不超过1k，则分成2份上传。<br/>
	 * 如果文件大小在1k-1M之间，则分成10份上传。<br/>
	 * 如果文件大小在1M-10M之间，则分成20份上传。<br/>
	 * 如果文件大小超过10M，则每次上传2M。<br/>
	 * */
	private int getSingleUploadLength() {
		// 文件总大小
		int totalLength = (int) mUploadFileSize;
		//如果文件小于100字节则直接一次上传
		if (totalLength < SIZE_KB / DIVIDE_SIZE_TEN) {
			return SIZE_KB / DIVIDE_SIZE_TEN;
		} else if (totalLength < SIZE_KB) {
			return totalLength / DIVIDE_SIZE_TWO;
		} else if (totalLength < SIZE_KB * SIZE_KB) {
			return totalLength / DIVIDE_SIZE_TEN;
		} else if (totalLength < DIVIDE_SIZE_TEN * SIZE_KB * SIZE_KB) {
			return totalLength / DIVIDE_SIZE_TWENTY;
		} else {
			return DIVIDE_SIZE_TWO * SIZE_KB * SIZE_KB;
		}
	}

	/**
	 * 获取指定url的http连接
	 *
	 * @param url
	 *            :url地址
	 * @return url对应的http连接
	 * */
	private HttpURLConnection getHttpConnection(String url) throws IOException,
			MalformedURLException, ProtocolException {
		HttpURLConnection httpConnection;
		httpConnection = ((HttpURLConnection) new URL(url).openConnection());
		httpConnection.setConnectTimeout(mTimeout);
		httpConnection.setRequestMethod("POST");
		httpConnection.setDoInput(true);
		httpConnection.setDoOutput(true);
		return httpConnection;
	}

	@Override
	public void transfer(XCallbackContext callbackCtx) {
		if (mState == UPLOADING) {
			return;
		}
		mCallbackCtx = callbackCtx;
		setState(UPLOADING);
		new Thread(new Runnable() {
			@Override
			public void run() {
				upload();
			}
		}).start();
	}

    private synchronized void setState(int state) {
        mState = state;
    }

	/**
	 * 获取指定文件地址对应的文件对象
	 */
	private File getFile() {
		XPathResolver pathResolver = new XPathResolver(mFilePath,
		        mWebContext.getWorkSpace());
		String absoluteFilePath = pathResolver.resolve();
		File uploadFile = new File(absoluteFilePath);
		if (null != absoluteFilePath) {
			uploadFile = new File(absoluteFilePath);
			if (uploadFile.exists()) {
				return uploadFile;
			}
		}
		return null;
	}

    @Override
    public void pause() {
        setState(PAUSE);
    }

}
