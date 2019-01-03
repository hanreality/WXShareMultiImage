package com.sch.share;

import android.accessibilityservice.AccessibilityService;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.sch.share.utils.ClipboardUtil;

import java.util.*;

/**
 * Created by StoneHui on 2018/12/12.
 * <p>
 * 微信多图分享服务。
 */
public class WXShareMultiImageService extends AccessibilityService {

    private static final String LAUNCHER_UI = "com.tencent.mm.ui.LauncherUI";
    private static final String SNS_TIME_LINE_UI = "com.tencent.mm.plugin.sns.ui.SnsTimeLineUI";
    private static final String SNS_UPLOAD_UI = "com.tencent.mm.plugin.sns.ui.SnsUploadUI";
    private static final String ALBUM_PREVIEW_UI = "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI";

    private static final String DISCOVER_ZH = "发现";
    private static final String DISCOVER_EN = "Discover";

    private static final String DONE_ZH = "完成";
    private static final String DONE_EN = "Done";

    private AccessibilityNodeInfo prevSource = null;
    private AccessibilityNodeInfo prevListView = null;

    // 当窗口发生的事件是我们配置监听的事件时,会回调此方法.会被调用多次
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        System.out.println("----> " + event);

        if (!ShareInfo.isAuto()) {
            return;
        }

        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                switch (event.getClassName().toString()) {
                    case LAUNCHER_UI:
                        openDiscover(event);
                        break;
                    case SNS_TIME_LINE_UI:
                        processingSnsTimeLineUI(event);
                        break;
                    case SNS_UPLOAD_UI:
                        processingSnsUploadUI(event);
                        break;
                    case ALBUM_PREVIEW_UI:
                        selectImage(event);
                        break;
                    default:
                        break;
                }
                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                if (event.getText().indexOf(DISCOVER_ZH) >= 0 || event.getText().indexOf(DISCOVER_EN) >= 0) {
                    openMoments(event);
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                if (event.getClassName().toString().equals(ListView.class.getName())) {
                    openAlbum(event);
                }
                break;
            default:
                break;
        }

    }

    private void openDiscover(AccessibilityEvent event) {

        if (ShareInfo.getWaitingImageCount() <= 0) {
            return;
        }

        AccessibilityNodeInfo rootNodeInfo = getRootNodeInfo();

        List<AccessibilityNodeInfo> discoverNodeList = rootNodeInfo.findAccessibilityNodeInfosByText(DISCOVER_ZH);
        if (discoverNodeList.isEmpty()) {
            discoverNodeList = rootNodeInfo.findAccessibilityNodeInfosByText(DISCOVER_EN);
        }
        if (!discoverNodeList.isEmpty()) {
            discoverNodeList.get(0).getParent().getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private void openMoments(AccessibilityEvent event) {

        if (ShareInfo.getWaitingImageCount() <= 0) {
            return;
        }

        AccessibilityNodeInfo rootNodeInfo = getRootNodeInfo();

        List<AccessibilityNodeInfo> commentsList = rootNodeInfo.findAccessibilityNodeInfosByText("朋友圈");

        if (commentsList != null && !commentsList.isEmpty()) {
            AccessibilityNodeInfo listView = findParent(commentsList.get(0), ListView.class.getName());
            if (listView != null) {
                listView.getChild(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    private void processingSnsTimeLineUI(AccessibilityEvent event) {
        if (ShareInfo.getWaitingImageCount() <= 0) {
            return;
        }
        // 过滤重复事件。
        if (event.getSource().equals(prevSource)) {
            return;
        }
        prevSource = event.getSource();

        AccessibilityNodeInfo rootNodeInfo = getRootNodeInfo();
        AccessibilityNodeInfo ibtnShare = findNodeInfo(rootNodeInfo, ImageButton.class.getName());
        if (ibtnShare != null) {
            ibtnShare.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    // 处理图文分享界面。
    private void processingSnsUploadUI(AccessibilityEvent event) {
        // 过滤重复事件。
        if (event.getSource().equals(prevSource)) {
            return;
        }
        prevSource = event.getSource();

        AccessibilityNodeInfo rootNodeInfo = getRootNodeInfo();
        if (rootNodeInfo == null) {
            return;
        }

        setTextToUI(rootNodeInfo);

        // 自动点击添加图片的 + 号按钮。
        if (ShareInfo.getWaitingImageCount() > 0) {
            AccessibilityNodeInfo gridView = findNodeInfo(rootNodeInfo, GridView.class.getName());
            if (gridView != null && gridView.getChildCount() > 0) {
                gridView.getChild(gridView.getChildCount() - 1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    // 显示待分享文字。
    private void setTextToUI(AccessibilityNodeInfo rootNodeInfo) {
        if (!ShareInfo.hasText() || !ClipboardUtil.getPrimaryClip(this).equals(ShareInfo.getText())) {
            return;
        }
        // 设置待分享文字。
        AccessibilityNodeInfo editText = findNodeInfo(rootNodeInfo, EditText.class.getName());
        // 粘贴剪切板内容
        if (editText != null) {
            editText.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            editText.performAction(AccessibilityNodeInfo.ACTION_PASTE);
        }
        ShareInfo.setText("");
    }

    // 打开相册。
    private void openAlbum(AccessibilityEvent event) {
        if (ShareInfo.getWaitingImageCount() <= 0) {
            return;
        }
        AccessibilityNodeInfo listView = event.getSource();
        List<AccessibilityNodeInfo> albumNodeInfoList = listView.findAccessibilityNodeInfosByText("从相册选择");
        if (albumNodeInfoList == null || albumNodeInfoList.isEmpty()) {
            return;
        }
        // 过滤重复事件。
        if (listView == prevListView) {
            return;
        }
        prevListView = listView;

        if (listView.getChildCount() > 0) {
            listView.getChild(1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    // 选择图片。
    private void selectImage(AccessibilityEvent event) {
        if (ShareInfo.getWaitingImageCount() <= 0) {
            return;
        }
        AccessibilityNodeInfo rootNodeInfo = getRootNodeInfo();
        if (rootNodeInfo == null) {
            return;
        }
        AccessibilityNodeInfo gridView = findNodeInfo(rootNodeInfo, GridView.class.getName());
        if (gridView == null) {
            return;
        }
        int index = ShareInfo.getSelectedImageCount();
        int maxIndex = ShareInfo.getSelectedImageCount() + ShareInfo.getWaitingImageCount();
        for (; index < maxIndex; index++) {
            AccessibilityNodeInfo viewNode = findNodeInfo(gridView.getChild(index), View.class.getName());
            if (viewNode != null) {
                viewNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }

        List<AccessibilityNodeInfo> doneNodeList = rootNodeInfo.findAccessibilityNodeInfosByText(DONE_ZH);
        if (doneNodeList.isEmpty()) {
            doneNodeList = rootNodeInfo.findAccessibilityNodeInfosByText(DONE_EN);
        }
        if (!doneNodeList.isEmpty()) {
            doneNodeList.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }

        ShareInfo.setImageCount(0, 0);
    }

    private AccessibilityNodeInfo findParent(AccessibilityNodeInfo childNodeInfo, String parentClassName) {
        AccessibilityNodeInfo parentNodeInfo = childNodeInfo.getParent();
        if (parentNodeInfo == null) {
            return null;
        }
        if (parentNodeInfo.getClassName().equals(parentClassName)) {
            return parentNodeInfo;
        }
        return findParent(parentNodeInfo, parentClassName);
    }

    // 查找指定类名的节点。
    @Nullable
    private AccessibilityNodeInfo findNodeInfo(AccessibilityNodeInfo rootNodeInfo, String className) {
        if (rootNodeInfo == null) {
            return null;
        }
        LinkedList<AccessibilityNodeInfo> queue = new LinkedList<>();
        queue.offer(rootNodeInfo);
        AccessibilityNodeInfo info;
        while (!queue.isEmpty()) {
            info = queue.poll();
            if (info == null) {
                continue;
            }
            if (info.getClassName().toString().contains(className)) {
                return info;
            }
            for (int i = 0; i < info.getChildCount(); i++) {
                queue.offer(info.getChild(i));
            }
        }
        return null;
    }

    // 获取 APPLICATION 的 Window 根节点。
    private AccessibilityNodeInfo getRootNodeInfo() {
        AccessibilityNodeInfo rootNodeInfo = null;
        for (AccessibilityWindowInfo windowInfo : getWindows()) {
            if (windowInfo.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                rootNodeInfo = windowInfo.getRoot();
            }
        }
        return rootNodeInfo != null ? rootNodeInfo : getRootInActiveWindow();
    }

    @Override
    public void onInterrupt() {
        //当服务要被中断时调用.会被调用多次
    }

}