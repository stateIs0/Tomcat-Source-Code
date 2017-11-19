/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.coyote;


/**
 * Action hook. Actions represent the callback mechanism used by
 * coyote servlet containers to request operations on the coyote connectors.
 * Some standard actions are defined in ActionCode, however custom
 * actions are permitted.
 *
 * The param object can be used to pass and return informations related with the
 * action.
 * 
 *
 * This interface is typically implemented by ProtocolHandlers, and the param
 * is usually a Request or Response object.
 *
 *   行动钩。动作表示所使用的回调机制
     土狼的servlet容器，要求对土狼的连接器进行操作。
     在ActionCode中定义了一些标准操作行为是允许的。

     param对象可用于传递和返回与此相关的信息
     *行动。
 *
 * 该接口通常由协议处理程序和param实现。通常是一个请求或响应对象。
 *
 * @author Remy Maucherat
 */
public interface ActionHook {


    /**
     * Send an action to the connector.
     * 
     * @param actionCode Type of the action
     * @param param Action parameter
     */
    public void action(ActionCode actionCode, Object param);


}
