/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2017. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.ims;

import android.telephony.Rlog;

import dalvik.system.PathClassLoader;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

/**
 * This class is used to create IMS related AOSP component. It serves IMS apk modules like
 * ImsService. The purpose is to centralize and allow dynamic loading for Mediatek component
 * creation.
 */
public class ImsComponentFactory {
    private static ImsComponentFactory sInstance;
    public static final String LOG_TAG = "ImsComponentFactory";

    public static ImsComponentFactory getInstance() {
        if (sInstance == null) {
            String className = "com.mediatek.ims.MtkImsComponentFactory";
            String classPackage = "/system/framework/mediatek-framework.jar";
            PathClassLoader pathClassLoader = new PathClassLoader(classPackage,
                    ClassLoader.getSystemClassLoader());
            Rlog.d(LOG_TAG , "pathClassLoader = " + pathClassLoader);
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className, false, pathClassLoader);
                Rlog.d(LOG_TAG, "class = " + clazz);
                Constructor<?> clazzConstructfunc = clazz.getConstructor();
                Rlog.d(LOG_TAG, "constructor function = " + clazzConstructfunc);
                sInstance = (ImsComponentFactory) clazzConstructfunc.newInstance();
            } catch (Exception  e) {
                Rlog.e(LOG_TAG, "No MtkImsComponentFactory! Use AOSP for instead!");
                sInstance = new ImsComponentFactory();
            }
        }
        return sInstance;
    }
}
