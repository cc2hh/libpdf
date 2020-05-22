// ActivationService.aidl
package com.jddz.service;

// Declare any non-default types here with import statements

interface UpdateService {
    boolean isForceUpdate(String packageName);

     /**
         * 打开/关闭高级护眼模式（EMUI8.0）
         * <p>
         * 距离护眼：
         * EYES_PROTECT_TYPE_DISTANCE = 0
         * 翻转护眼：
         * EYES_PROTECT_TYPE_FLIP = 1
         * 感光护眼：
         * EYES_PROTECT_TYPE_LIGHT = 2
         *
         * @param eyesProtectType
         * @param on              true打开护眼模式，false关闭护眼模式
         */
        boolean enableEyesProtect(int eyesProtectType, boolean on);
        /**
        *
        * 加密
        **/
        String encrypt(String pwd);
        /**
        *
        *解密
        **/
        String decrypt(String mes);

            /**
             *
             * 直接返回加密后的信息
             *
             **/
            String decryptAndEncrypt(String account,String mes);
}
