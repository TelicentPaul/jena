/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.ext.xerces.impl.dv.util;

import java.util.AbstractList;

/**
 * Implementation of <code>org.apache.xerces.xs.datatypes.ByteList</code>.
 *
 * {@literal @xerces.internal} 
 * 
 * @author Ankit Pasricha, IBM
 * 
 * @version $Id: ByteListImpl.java 1024038 2010-10-18 22:06:35Z sandygao $
 */
@SuppressWarnings("all")
public class ByteListImpl extends AbstractList {

    // actually data stored in a byte array
    protected final byte[] data;
    
    // canonical representation of the data
    protected String canonical;
    
    public ByteListImpl(byte[] data) {
        this.data = data;
    }
    
    /*
     * List methods
     */

    public Object get(int index) {
        if (index >= 0 && index < data.length) {
            return Byte.valueOf(data[index]);
        }
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    public int size() {
        return data.length;
    }
}

