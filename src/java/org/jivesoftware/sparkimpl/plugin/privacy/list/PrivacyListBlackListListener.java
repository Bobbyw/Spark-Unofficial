/**
 * $RCSfile: ,v $
 * $Revision: $
 * $Date: $
 * 
 * Copyright (C) 2004-2010 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.sparkimpl.plugin.privacy.list;

/**
 *
 * @author Zolotarev Konstantin
 */
public interface PrivacyListBlackListListener {

    /**
     * New Item added into blockList
     * @param jid Users jid
     */
    public void addedBlockedItem(String jid);

    /**
     * User removed from BlockList
     * @param jid users jid
     */
    public void removedBlockedItem(String jid);

}
